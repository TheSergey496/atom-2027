package org.prolegacy.atom2027.services;

import org.prolegacy.atom2027.entities.*;
import org.prolegacy.atom2027.entities.ids.*;
import org.prolegacy.atom2027.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Service
public class BrigadeOptimizerService {
    private static final int MIN_BRIGADE = 2;
    private static final int MAX_BRIGADE = 5;
    private static final Duration DEFAULT_RESOLVE = Duration.ofHours(100000);

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EdificeRepository edificeRepository;
    @Autowired
    private EdificeDependenciesRepository edificeDependenciesRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private EmployeeQualificationRepository employeeQualificationRepository;
    @Autowired
    private TimeResolveRepository timeResolveRepository;
    @Autowired
    private BrigadeRepository brigadeRepository;
    @Autowired
    private BrigadeMemberRepository brigadeMemberRepository;
    @Autowired
    private BrigadeEventRepository brigadeEventRepository;

    @Transactional
    public void optimize() {
        brigadeEventRepository.deleteAll();
        brigadeMemberRepository.deleteAll();
        brigadeRepository.deleteAll();

        final Map<EdificeId, Edifice> edifices = edificeRepository.findAll().stream()
                .collect(Collectors.toMap(Edifice::getId, Function.identity()));
        final Map<EdificeId, List<Event>> eventsByEdificeId = eventRepository.findAll().stream()
                .collect(Collectors.groupingBy(Event::getEdificeId));
        final Map<EdificeDependencyId, EdificeDependency> edificeDependencies =
                edificeDependenciesRepository.findAll().stream()
                        .collect(Collectors.toMap(EdificeDependency::getId, Function.identity()));
        final Map<EmployeeId, Employee> employees = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        final Map<EmployeeId, List<EmployeeQualification>> qualifications = employeeQualificationRepository.findAll().stream()
                .collect(Collectors.groupingBy(EmployeeQualification::getEmployeeId));
        final Map<EdificeId, Duration> waterResolveTime = new HashMap<>();
        final Map<EdificeId, Duration> energyResolveTime = new HashMap<>();
        timeResolveRepository.findAll().forEach(timeResolve -> {
            if (timeResolve.getResourceType().contains("water")) {
                waterResolveTime.put(timeResolve.getEdificeId(), timeResolve.getAverageResolutionTime());
            } else if (timeResolve.getResourceType().contains("energy")) {
                energyResolveTime.put(timeResolve.getEdificeId(), timeResolve.getAverageResolutionTime());
            }
        });

        // ---------- build a flat qualification lookup ----------
        final Map<EmployeeId, Map<String, Double>> qualMap = new HashMap<>();
        qualifications.forEach((empId, qs) -> {
            Map<String, Double> m = new HashMap<>();
            for (EmployeeQualification q : qs) {
                m.merge(q.getResourceType(), q.getProff().doubleValue(), Math::max);
            }
            qualMap.put(empId, m);
        });

        // --- 1. Directed adjacency (source -> target) ---------------------
        Map<EdificeId, Set<EdificeId>> outgoing = new HashMap<>();
        Map<EdificeId, Set<EdificeId>> incoming = new HashMap<>();
        for (EdificeDependency dep : edificeDependencies.values()) {
            EdificeId src = dep.getSourceEdificeId();
            EdificeId dst = dep.getTargetEdificeId();
            if (src.equals(dst)) continue;
            if (!edifices.containsKey(src) || !edifices.containsKey(dst)) continue;
            outgoing.computeIfAbsent(src, k -> new HashSet<>()).add(dst);
            incoming.computeIfAbsent(dst, k -> new HashSet<>()).add(src);
        }

        // --- 2. Connected components (undirected) ------------------------
        Map<EdificeId, Integer> componentOf = new HashMap<>();
        int nextComponent = 0;
        for (EdificeId start : edifices.keySet()) {
            if (componentOf.containsKey(start)) continue;
            Deque<EdificeId> queue = new ArrayDeque<>();
            queue.add(start);
            componentOf.put(start, nextComponent);
            while (!queue.isEmpty()) {
                EdificeId cur = queue.poll();
                for (EdificeId nb : outgoing.getOrDefault(cur, Set.of())) {
                    if (componentOf.putIfAbsent(nb, nextComponent) == null) queue.add(nb);
                }
                for (EdificeId nb : incoming.getOrDefault(cur, Set.of())) {
                    if (componentOf.putIfAbsent(nb, nextComponent) == null) queue.add(nb);
                }
            }
            nextComponent++;
        }

        // --- 3. Bucket events by (component, resourceType) ----------------
        Map<Integer, Map<String, List<Event>>> groups = new TreeMap<>();
        for (Map.Entry<EdificeId, List<Event>> entry : eventsByEdificeId.entrySet()) {
            Integer comp = componentOf.get(entry.getKey());
            if (comp == null) continue; // unknown edifice → handled below
            for (Event event : entry.getValue()) {
                groups.computeIfAbsent(comp, k -> new TreeMap<>())
                        .computeIfAbsent(event.getResourceType(), k -> new ArrayList<>())
                        .add(event);
            }
        }

        // --- 4. One deque per (component, type) ---------------------------
        List<ArrayDeque<ScheduledEvent>> result = new ArrayList<>();
        for (Map<String, List<Event>> byType : groups.values()) {
            for (List<Event> events : byType.values()) {
                result.add(buildDeque(events, outgoing));
            }
        }

        // --- 5. Events of unknown edifices → one deque per event ----------
        for (Map.Entry<EdificeId, List<Event>> entry : eventsByEdificeId.entrySet()) {
            if (edifices.containsKey(entry.getKey())) continue;
            for (Event event : entry.getValue()) {
                ArrayDeque<ScheduledEvent> dq = new ArrayDeque<>();
                dq.add(new ScheduledEvent(event, List.of()));
                result.add(dq);
            }
        }

        Map<EmployeeId, Map<String, Double>> proff = new HashMap<>();
        qualifications.forEach((id, qs) -> {
            Map<String, Double> byType = new HashMap<>();
            for (EmployeeQualification q : qs)
                byType.merge(q.getResourceType(), q.getProff().doubleValue(), Double::sum);
            proff.put(id, byType);
        });
        BrigadePlanner planner = new BrigadePlanner(employees.values(), proff);

        final Duration fallbackBase = Duration.ofHours(1); // when TimeResolve is missing (e.g. unknown edifice)
        ToDoubleFunction<Event> baseSeconds = e -> {
            Map<EdificeId, Duration> m = e.getResourceType().contains("water") ? waterResolveTime : energyResolveTime;
            return m.getOrDefault(e.getEdificeId(), fallbackBase).toMillis() / 1000.0;
        };

        List<List<Event>> waves = executeAll(result, null);

        List<BrigadePlanner.BrigadePlan> plans = planner.planAll(waves, baseSeconds);
        persistPlans(plans);

        Duration total = plans.stream().map(BrigadePlanner.BrigadePlan::finishesAt)
                .max(Comparator.naturalOrder()).orElse(Duration.ZERO);
        System.out.printf("%d brigades, %d events, total resolution time %s%n",
                plans.size(), waves.stream().mapToInt(List::size).sum(), total);
    }

    private void persistPlans(List<BrigadePlanner.BrigadePlan> plans) {
        for (BrigadePlanner.BrigadePlan plan : plans) {
            // id is null -> filled by @LongIdGenerator on persist
            Brigade brigade = brigadeRepository.save(new Brigade(null));
            BrigadeId brigadeId = brigade.getId();

            List<BrigadeMember> members = plan.members().stream()
                    .map(employeeId -> new BrigadeMember(null, brigadeId, employeeId))
                    .toList();
            brigadeMemberRepository.saveAll(members);

            List<BrigadeEvent> events = new ArrayList<>();
            long order = 1; // sequential execution order within the brigade
            for (BrigadePlanner.Assignment a : plan.assignments()) {
                events.add(new BrigadeEvent(null, brigadeId, a.event().getId(), order++));
            }
            brigadeEventRepository.saveAll(events);
        }
    }

    // =====================================================================
    //  Brigade planning
    // =====================================================================

    /**
     * Heuristic planner for a single parallel batch.
     *
     * <ol>
     *   <li>Compute Tbase for every event.</li>
     *   <li>Decide how many brigades we can form (limited by number of events and
     *       by the minimum brigade size of two).</li>
     *   <li>Distribute the most qualified employees into brigades using a
     *       snake (boustrophedon) pass so that total qualification is balanced.</li>
     *   <li>Guarantee every brigade gets at least one event: the first
     *       {@code B} (largest) events are distributed one per brigade,
     *       choosing the best‑qualified brigade for each event.</li>
     *   <li>Remaining events are list‑scheduled onto the brigade that finishes
     *       them earliest.</li>
     * </ol>
     */
    private List<BrigadePlan> planBatch(
            List<Event> batch,
            Map<EmployeeId, Employee> employees,
            Map<EmployeeId, Map<String, Double>> qualMap,
            Map<EdificeId, Duration> waterResolveTime,
            Map<EdificeId, Duration> energyResolveTime,
            Map<EdificeId, Edifice> edifices) {

        if (batch.isEmpty()) return List.of();

        // 1. Tbase per event
        Map<EventId, Duration> tbaseById = new HashMap<>();
        for (Event e : batch) {
            tbaseById.put(e.getId(), resolveTbase(e, waterResolveTime, energyResolveTime));
        }

        // 2. How many brigades can we form?
        List<Employee> allEmployees = new ArrayList<>(employees.values());
        int totalEmp = allEmployees.size();
        int brigadeCount = Math.min(batch.size(), totalEmp / MIN_BRIGADE);
        if (brigadeCount == 0) {
            if (totalEmp >= MIN_BRIGADE) {
                brigadeCount = 1;
            } else {
                throw new IllegalStateException(
                        "Cannot form a brigade: only " + totalEmp + " employee(s) available");
            }
        }

        // 3. Pick and distribute employees
        Map<EmployeeId, Double> totalQual = new HashMap<>();
        for (Employee emp : allEmployees) {
            double s = qualMap.getOrDefault(emp.getId(), Map.of())
                    .values().stream().mapToDouble(Double::doubleValue).sum();
            totalQual.put(emp.getId(), s);
        }
        allEmployees.sort(Comparator
                .comparingDouble((Employee e) -> totalQual.getOrDefault(e.getId(), 0.0))
                .reversed());

        int useCount = Math.min(allEmployees.size(), brigadeCount * MAX_BRIGADE);
        List<Employee> selected = allEmployees.subList(0, useCount);

        List<List<Employee>> members = new ArrayList<>(brigadeCount);
        for (int i = 0; i < brigadeCount; i++) members.add(new ArrayList<>());

        if (brigadeCount == 1) {
            members.get(0).addAll(selected);
        } else {
            int cur = 0;
            int dir = 1;
            for (Employee emp : selected) {
                members.get(cur).add(emp);
                if (cur + dir < 0 || cur + dir >= brigadeCount) {
                    dir = -dir;
                    cur += dir;
                } else {
                    cur += dir;
                }
            }
        }

        // Build mutable plans
        List<BrigadePlan> plans = new ArrayList<>(brigadeCount);
        for (List<Employee> team : members) {
            plans.add(new BrigadePlan(team));
        }

        // 4. Assign events – sorted largest first
        List<Event> sorted = new ArrayList<>(batch);
        sorted.sort(Comparator.comparing((Event e) -> tbaseById.get(e.getId())).reversed());

        // First pass: one event per brigade (ensures every brigade gets ≥1 event)
        int assignedFirst = Math.min(brigadeCount, sorted.size());
        for (int i = 0; i < assignedFirst; i++) {
            Event ev = sorted.get(i);
            BrigadePlan best = null;
            double bestQual = -1;
            for (BrigadePlan p : plans) {
                if (!p.events.isEmpty()) continue; // already has its first event
                double q = sumQual(p.members, ev.getResourceType(), qualMap);
                if (q > bestQual) {
                    bestQual = q;
                    best = p;
                }
            }
            if (best != null) {
                appendEvent(best, ev, tbaseById.get(ev.getId()), qualMap);
            }
        }

        // Second pass: list scheduling for the rest
        for (int i = assignedFirst; i < sorted.size(); i++) {
            Event ev = sorted.get(i);
            Duration tbase = tbaseById.get(ev.getId());
            BrigadePlan best = null;
            Duration bestFinish = null;
            for (BrigadePlan p : plans) {
                Duration t = computeTime(p.members, ev.getResourceType(), tbase, qualMap);
                Duration finish = p.totalTime.plus(t);
                if (bestFinish == null || finish.compareTo(bestFinish) < 0) {
                    bestFinish = finish;
                    best = p;
                }
            }
            appendEvent(best, ev, tbase, qualMap);
        }

        return plans;
    }

    private void appendEvent(BrigadePlan plan, Event ev, Duration tbase,
                             Map<EmployeeId, Map<String, Double>> qualMap) {
        Duration t = computeTime(plan.members, ev.getResourceType(), tbase, qualMap);
        plan.events.add(ev);
        plan.perEventTime.put(ev.getId(), t);
        plan.totalTime = plan.totalTime.plus(t);
    }

    private Duration computeTime(List<Employee> members, String resourceType,
                                 Duration tbase,
                                 Map<EmployeeId, Map<String, Double>> qualMap) {
        double sum = sumQual(members, resourceType, qualMap);
        if (sum <= 0.0) {
            // Nobody is qualified – effectively an infinite resolution time.
            return Duration.ofDays(365);
        }
        double denom = sum / 5.0;                      // T = Tbase / (Σ Ri / 5)
        return Duration.ofMillis((long) (tbase.toMillis() / denom));
    }

    private double sumQual(List<Employee> members, String resourceType,
                           Map<EmployeeId, Map<String, Double>> qualMap) {
        return members.stream()
                .mapToDouble(e -> qualMap.getOrDefault(e.getId(), Map.of())
                        .getOrDefault(resourceType, 0.0))
                .sum();
    }

    private Duration resolveTbase(Event e,
                                  Map<EdificeId, Duration> waterResolveTime,
                                  Map<EdificeId, Duration> energyResolveTime) {
        String rt = e.getResourceType();
        if (rt != null) {
            String lower = rt.toLowerCase();
            if (lower.contains("water")) {
                Duration d = waterResolveTime.get(e.getEdificeId());
                return d != null ? d : DEFAULT_RESOLVE;
            }
            if (lower.contains("energy")) {
                Duration d = energyResolveTime.get(e.getEdificeId());
                return d != null ? d : DEFAULT_RESOLVE;
            }
        }
        return DEFAULT_RESOLVE;
    }

    private static String format(Duration d) {
        long s = d.getSeconds();
        return String.format("%d.%03ds", s, d.toMillisPart());
    }

    /**
     * Mutable plan for one brigade.
     */
    private static final class BrigadePlan {
        final List<Employee> members;
        final List<Event> events = new ArrayList<>();
        final Map<EventId, Duration> perEventTime = new HashMap<>();
        Duration totalTime = Duration.ZERO;

        BrigadePlan(List<Employee> members) {
            this.members = members;
        }
    }

    // =====================================================================
    //  Original scheduling helpers (unchanged)
    // =====================================================================

    private ArrayDeque<ScheduledEvent> buildDeque(
            List<Event> events,
            Map<EdificeId, Set<EdificeId>> outgoing) {
        // ... (unchanged – copy from the original file) ...
        Map<EdificeId, List<Event>> sameTypeByEdifice = new HashMap<>();
        for (Event e : events) {
            sameTypeByEdifice.computeIfAbsent(e.getEdificeId(), k -> new ArrayList<>()).add(e);
        }

        Map<EdificeId, Set<EdificeId>> nearestPred = new HashMap<>();
        for (EdificeId source : sameTypeByEdifice.keySet()) {
            Set<EdificeId> seen = new HashSet<>();
            Deque<EdificeId> queue = new ArrayDeque<>();
            for (EdificeId next : outgoing.getOrDefault(source, Set.of())) {
                if (seen.add(next)) queue.add(next);
            }
            while (!queue.isEmpty()) {
                EdificeId cur = queue.poll();
                if (cur.equals(source)) continue;
                if (sameTypeByEdifice.containsKey(cur)) {
                    nearestPred.computeIfAbsent(cur, k -> new HashSet<>()).add(source);
                    continue;
                }
                for (EdificeId next : outgoing.getOrDefault(cur, Set.of())) {
                    if (seen.add(next)) queue.add(next);
                }
            }
        }

        Map<EventId, Event> byId = events.stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        Map<EventId, List<EventId>> blockerIds = new LinkedHashMap<>();
        for (Event e : events) {
            List<EventId> blockers = new ArrayList<>();
            for (EdificeId pred : nearestPred.getOrDefault(e.getEdificeId(), Set.of())) {
                for (Event pe : sameTypeByEdifice.get(pred)) {
                    blockers.add(pe.getId());
                }
            }
            blockerIds.put(e.getId(), blockers);
        }

        Map<EventId, Integer> inDegree = new HashMap<>();
        Map<EventId, List<EventId>> dependents = new HashMap<>();
        for (Event e : events) {
            List<EventId> bl = blockerIds.get(e.getId());
            inDegree.put(e.getId(), bl.size());
            for (EventId f : bl) {
                dependents.computeIfAbsent(f, k -> new ArrayList<>()).add(e.getId());
            }
        }

        Deque<EventId> ready = new ArrayDeque<>();
        for (Event e : events) {
            if (inDegree.get(e.getId()) == 0) ready.add(e.getId());
        }

        List<EventId> sorted = new ArrayList<>();
        while (!ready.isEmpty()) {
            EventId id = ready.poll();
            sorted.add(id);
            for (EventId dep : dependents.getOrDefault(id, List.of())) {
                int d = inDegree.get(dep) - 1;
                inDegree.put(dep, d);
                if (d == 0) ready.add(dep);
            }
        }

        if (sorted.size() < events.size()) {
            Set<EventId> placed = new HashSet<>(sorted);
            for (Event e : events) {
                if (placed.add(e.getId())) sorted.add(e.getId());
            }
        }

        ArrayDeque<ScheduledEvent> deque = new ArrayDeque<>();
        Map<EventId, ScheduledEvent> built = new HashMap<>();
        for (EventId id : sorted) {
            Event e = byId.get(id);
            List<ScheduledEvent> blockers = new ArrayList<>();
            for (EventId bid : blockerIds.getOrDefault(id, List.of())) {
                ScheduledEvent sb = built.get(bid);
                if (sb != null) blockers.add(sb);
            }
            ScheduledEvent se = new ScheduledEvent(e, blockers);
            built.put(id, se);
            deque.addLast(se);
        }
        return deque;
    }

    private List<Event> nextReadyEvents(List<ArrayDeque<ScheduledEvent>> deques,
                                        Set<EventId> completedEventIds) {
        List<Event> ready = new ArrayList<>();
        for (ArrayDeque<ScheduledEvent> deque : deques) {
            for (ScheduledEvent se : deque) {
                EventId id = se.event().getId();
                if (completedEventIds.contains(id)) continue;
                if (se.isReady(completedEventIds)) ready.add(se.event());
            }
        }
        return ready;
    }

    private List<List<Event>> executeAll(List<ArrayDeque<ScheduledEvent>> deques,
                                         Consumer<List<Event>> executor) {
        int total = deques.stream().mapToInt(ArrayDeque::size).sum();
        Set<EventId> completed = new HashSet<>();
        List<List<Event>> batches = new ArrayList<>();

        while (completed.size() < total) {
            List<Event> batch = nextReadyEvents(deques, completed);
            if (batch.isEmpty()) {
                List<String> stuck = new ArrayList<>();
                for (ArrayDeque<ScheduledEvent> deque : deques) {
                    for (ScheduledEvent se : deque) {
                        if (!completed.contains(se.event().getId())) {
                            stuck.add(se.event().getId().toString());
                        }
                    }
                }
                throw new IllegalStateException(
                        "Dependency deadlock: " + stuck.size()
                                + " event(s) can never be scheduled: " + stuck);
            }
            batches.add(batch);
            if (executor != null) executor.accept(batch);
            for (Event e : batch) completed.add(e.getId());
        }
        return batches;
    }
}

record ScheduledEvent(Event event, List<ScheduledEvent> directBlockers) {
    public boolean isReady(Collection<EventId> completedEventIds) {
        return directBlockers.stream()
                .allMatch(b -> completedEventIds.contains(b.event().getId()));
    }
}