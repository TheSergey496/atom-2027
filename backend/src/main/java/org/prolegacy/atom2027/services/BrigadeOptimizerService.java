package org.prolegacy.atom2027.services;

import org.prolegacy.atom2027.entities.*;
import org.prolegacy.atom2027.entities.ids.BrigadeId;
import org.prolegacy.atom2027.entities.ids.EdificeId;
import org.prolegacy.atom2027.entities.ids.EmployeeId;
import org.prolegacy.atom2027.entities.ids.EventId;
import org.prolegacy.atom2027.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BrigadeOptimizerService {

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

    private static final Duration FALLBACK_BASE = Duration.ofHours(1); // missing TimeResolve / unknown edifice

    @Transactional
    public void optimize() {
        // ---- load ----------------------------------------------------------
        Set<EdificeId> edifices = edificeRepository.findAll().stream()
                .map(Edifice::getId).collect(Collectors.toSet());
        List<Event> events = eventRepository.findAll();
        Map<EmployeeId, Employee> employees = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<EmployeeId, Map<String, Double>> proff = new HashMap<>();
        for (EmployeeQualification q : employeeQualificationRepository.findAll())
            proff.computeIfAbsent(q.getEmployeeId(), k -> new HashMap<>())
                    .merge(q.getResourceType(), q.getProff().doubleValue(), Double::sum);
        Map<EdificeId, Duration> waterBase = new HashMap<>(), energyBase = new HashMap<>();
        timeResolveRepository.findAll().forEach(tr -> {
            if (tr.getResourceType().contains("water")) waterBase.put(tr.getEdificeId(), tr.getAverageResolutionTime());
            else if (tr.getResourceType().contains("energy"))
                energyBase.put(tr.getEdificeId(), tr.getAverageResolutionTime());
        });

        // ---- directed adjacency (same filters as before) ---------------------
        Map<EdificeId, Set<EdificeId>> outgoing = new HashMap<>();
        for (EdificeDependency dep : edificeDependenciesRepository.findAll()) {
            EdificeId src = dep.getSourceEdificeId(), dst = dep.getTargetEdificeId();
            if (src.equals(dst) || !edifices.contains(src) || !edifices.contains(dst)) continue;
            outgoing.computeIfAbsent(src, k -> new HashSet<>()).add(dst);
        }

        // ---- transitive same-type blockers: nearest same-type ancestors -------
        Map<String, Map<EdificeId, List<Event>>> byType = events.stream()
                .collect(Collectors.groupingBy(Event::getResourceType,
                        Collectors.groupingBy(Event::getEdificeId)));
        List<BrigadeSolver.TaskInput> inputs = new ArrayList<>();
        for (var typeEntry : byType.entrySet()) {
            Map<EdificeId, List<Event>> sameType = typeEntry.getValue();
            Map<EdificeId, Set<EdificeId>> nearestPred = nearestSameTypePreds(sameType.keySet(), outgoing);
            for (var edEntry : sameType.entrySet()) {
                Set<EventId> blockers = new HashSet<>();
                for (EdificeId pred : nearestPred.getOrDefault(edEntry.getKey(), Set.of()))
                    for (Event pe : sameType.get(pred)) blockers.add(pe.getId());
                for (Event e : edEntry.getValue())
                    inputs.add(new BrigadeSolver.TaskInput(e.getId(), e.getResourceType(),
                            baseSeconds(e, waterBase, energyBase), blockers));
            }
        }

        // ---- solve ------------------------------------------------------------
        BrigadeSolver solver = new BrigadeSolver(employees.values(), proff);
        BrigadeSolver.Solution solution = solver.solve(inputs);

        // ---- persist (idempotent re-runs) --------------------------------------
        brigadeEventRepository.deleteAllInBatch();
        brigadeMemberRepository.deleteAllInBatch();
        brigadeRepository.deleteAllInBatch();
        List<BrigadeSolver.BrigadeSolution> brigades = solution.brigades();
        for (int i = 0; i < brigades.size(); i++) {
            BrigadeSolver.BrigadeSolution plan = brigades.get(i);
            Brigade brigade = brigadeRepository.save(new Brigade("Бригада %s".formatted(i + 1))); // id via @LongIdGenerator
            BrigadeId brigadeId = brigade.getId();
            brigadeMemberRepository.saveAll(plan.members().stream()
                    .map(id -> new BrigadeMember(null, brigadeId, id)).toList());
            List<BrigadeEvent> links = new ArrayList<>();
            long order = 1;
            for (BrigadeSolver.Job job : plan.jobs()) // jobs already in execution order
                links.add(new BrigadeEvent(null, brigadeId, job.event(), order++));
            brigadeEventRepository.saveAll(links);
        }

        System.out.printf("Scheduled %d events on %d brigades, total time %s (%d in reserve)%n",
                inputs.size(), solution.brigades().size(), solution.makespan(), solution.reserve().size());
        for (var b : solution.brigades())
            System.out.printf("  %s -> %s (%s)%n", b.members(), b.jobs(), b.finishesAt());
    }

    /**
     * Your original BFS semantics, flattened: stop at same-type edifices, don't expand past them.
     */
    private Map<EdificeId, Set<EdificeId>> nearestSameTypePreds(
            Set<EdificeId> sources, Map<EdificeId, Set<EdificeId>> outgoing) {
        Map<EdificeId, Set<EdificeId>> nearestPred = new HashMap<>();
        for (EdificeId source : sources) {
            Set<EdificeId> seen = new HashSet<>();
            Deque<EdificeId> queue = new ArrayDeque<>();
            for (EdificeId next : outgoing.getOrDefault(source, Set.of()))
                if (seen.add(next)) queue.add(next);
            while (!queue.isEmpty()) {
                EdificeId cur = queue.poll();
                if (cur.equals(source)) continue;
                if (sources.contains(cur)) {
                    nearestPred.computeIfAbsent(cur, k -> new HashSet<>()).add(source);
                    continue;
                }
                for (EdificeId next : outgoing.getOrDefault(cur, Set.of()))
                    if (seen.add(next)) queue.add(next);
            }
        }
        return nearestPred;
    }

    private double baseSeconds(Event e, Map<EdificeId, Duration> waterBase, Map<EdificeId, Duration> energyBase) {
        Map<EdificeId, Duration> m = e.getResourceType().contains("water") ? waterBase : energyBase;
        return m.getOrDefault(e.getEdificeId(), FALLBACK_BASE).toMillis() / 1000.0;
    }
}