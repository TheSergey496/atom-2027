package org.prolegacy.atom2027.services;

import org.prolegacy.atom2027.entities.Employee;
import org.prolegacy.atom2027.entities.ids.EmployeeId;
import org.prolegacy.atom2027.entities.ids.EventId;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * speed(brigade, type) = sum of member qualifications (missing -> 0)
 * time(event, brigade) = 5 * Tbase / speed  (T = Tbase / (R1/5 + ... + R5/5))
 * <p>
 * Rules enforced:
 * - brigade size 2..5, every brigade gets >= 1 event, employee in <= 1 brigade
 * - a brigade may take an event only if >= 1 member is qualified for its type
 * - same-type transitive precedence (nearest-ancestor blockers -> full order)
 * <p>
 * Persistent brigades + HEFT list scheduling with gap insertion (no waves).
 */
public class BrigadeSolver {

    public record TaskInput(EventId event, String type, double baseSeconds, Set<EventId> blockers) {
    }

    public record Job(EventId event, String type, Duration start, Duration duration) {
    }

    public record BrigadeSolution(List<EmployeeId> members, List<Job> jobs, Duration finishesAt) {
    }

    public record Solution(List<BrigadeSolution> brigades, Duration makespan, List<EmployeeId> reserve) {
    }

    private static final int MIN = 2, MAX = 5;
    private static final double EPS = 1e-9;
    private static final int LS_BUDGET = 150;

    private final List<EmployeeId> ids;
    private final Map<EmployeeId, Map<String, Double>> proff;

    private final Map<EventId, Task> tasks = new LinkedHashMap<>();
    private List<EventId> topo;
    private final Map<String, Double> totalProff = new HashMap<>();
    private final Map<String, Double> totalWork = new HashMap<>();
    private double lowerBound;

    public BrigadeSolver(Collection<Employee> employees, Map<EmployeeId, Map<String, Double>> proff) {
        this.ids = employees.stream().map(Employee::getId).toList();
        this.proff = proff;
    }

    // --- public API ---------------------------------------------------------

    public Solution solve(List<TaskInput> inputs) {
        if (inputs.isEmpty()) return new Solution(List.of(), Duration.ZERO, List.copyOf(ids));
        if (ids.size() < MIN) throw new IllegalStateException("At least 2 employees are required");

        buildGraph(inputs);
        totalProff.clear();
        totalWork.clear();
        for (EmployeeId id : ids)
            proff.getOrDefault(id, Map.of()).forEach((t, v) -> totalProff.merge(t, v, Double::sum));
        for (Task t : tasks.values()) totalWork.merge(t.type, t.work, Double::sum);
        for (String t : totalWork.keySet())
            if (totalProff.getOrDefault(t, 0.0) <= EPS)
                throw new IllegalStateException("No qualified employees for resource type " + t);
        computeLowerBound();

        int kMax = Math.min(tasks.size(), ids.size() / MIN);
        TreeSet<Integer> ks = new TreeSet<>(Comparator.reverseOrder());
        for (int k = 1; k <= Math.min(kMax, 16); k++) ks.add(k);
        ks.add(kMax);
        ks.add(Math.max(1, kMax - 1));
        ks.add(Math.max(1, kMax / 2));
        ks.add(Math.max(1, kMax / 4));
        ks.add(Math.max(1, Math.min(kMax, (ids.size() + MAX - 1) / MAX)));

        List<List<EmployeeId>> bestPart = null;
        double bestMake = Double.POSITIVE_INFINITY;
        for (int k : ks) {
            for (boolean snake : new boolean[]{false, true}) {
                List<Brigade> bs = form(k, snake);
                if (bs == null) continue;
                double mk = repairAndSchedule(bs);
                if (mk < bestMake - EPS) {
                    bestMake = mk;
                    bestPart = snapshot(bs);
                }
            }
            if (bestMake <= lowerBound + EPS) break; // provably optimal
        }
        if (bestPart == null) throw new IllegalStateException("No feasible brigade layout (2..5, type coverage)");

        bestMake = localSearch(bestPart, bestMake);
        List<Brigade> bs = fromPartition(bestPart);
        schedule(bs);
        return materialize(bs);
    }

    // --- event DAG ------------------------------------------------------------

    private static final class Task {
        final EventId id;
        final String type;
        final double work; // work = 5 * Tbase
        final List<Task> preds = new ArrayList<>();
        final List<Task> succs = new ArrayList<>();
        double rank;

        Task(EventId id, String type, double work) {
            this.id = id;
            this.type = type;
            this.work = work;
        }
    }

    private void buildGraph(List<TaskInput> inputs) {
        tasks.clear();
        for (TaskInput in : inputs) tasks.put(in.event(), new Task(in.event(), in.type(), 5.0 * in.baseSeconds()));
        for (TaskInput in : inputs) {
            Task t = tasks.get(in.event());
            for (EventId b : in.blockers()) {
                Task p = tasks.get(b);
                if (p == null) continue;
                t.preds.add(p);
                p.succs.add(t);
            }
        }
        Map<EventId, Integer> indeg = new HashMap<>();
        tasks.values().forEach(t -> indeg.put(t.id, t.preds.size()));
        Deque<EventId> q = new ArrayDeque<>();
        tasks.keySet().forEach(id -> {
            if (indeg.get(id) == 0) q.add(id);
        });
        topo = new ArrayList<>();
        while (!q.isEmpty()) {
            EventId id = q.poll();
            topo.add(id);
            for (Task s : tasks.get(id).succs) {
                int d = indeg.get(s.id) - 1;
                indeg.put(s.id, d);
                if (d == 0) q.add(s.id);
            }
        }
        if (topo.size() < tasks.size()) {
            List<EventId> stuck = tasks.keySet().stream().filter(id -> indeg.get(id) > 0).toList();
            throw new IllegalStateException("Cyclic same-type dependencies between events: " + stuck);
        }
    }

    private void computeLowerBound() {
        double lb = 0;
        for (var e : totalWork.entrySet()) lb = Math.max(lb, e.getValue() / totalProff.get(e.getKey()));
        Map<String, Double> top5 = new HashMap<>();
        for (String type : totalWork.keySet()) {
            List<Double> vals = ids.stream().map(id -> p(id, type)).sorted(Comparator.reverseOrder()).toList();
            top5.put(type, vals.stream().limit(MAX).mapToDouble(Double::doubleValue).sum());
        }
        Map<EventId, Double> bl = new HashMap<>();
        for (int i = topo.size() - 1; i >= 0; i--) {
            Task t = tasks.get(topo.get(i));
            double best = 0;
            for (Task s : t.succs) best = Math.max(best, bl.get(s.id));
            bl.put(t.id, t.work / top5.get(t.type) + best);
        }
        for (double v : bl.values()) lb = Math.max(lb, v);
        lowerBound = lb;
    }

    /**
     * Critical-path priority; strictly decreasing along edges => valid topological order.
     */
    private List<Task> rankOrder(int k) {
        for (int i = topo.size() - 1; i >= 0; i--) {
            Task t = tasks.get(topo.get(i));
            double best = 0;
            for (Task s : t.succs) best = Math.max(best, s.rank);
            t.rank = t.work / (totalProff.get(t.type) / k) + best;
        }
        return tasks.values().stream()
                .sorted(Comparator.comparingDouble((Task t) -> t.rank).reversed()).toList();
    }

    // --- brigades ---------------------------------------------------------------

    private record Scheduled(Task task, double start, double finish) {
    }

    private final class Brigade {
        final List<EmployeeId> members = new ArrayList<>();
        final Map<String, Double> speed = new HashMap<>();
        final List<Scheduled> jobs = new ArrayList<>(); // sorted by start, non-overlapping

        void add(EmployeeId id) {
            members.add(id);
            proff.getOrDefault(id, Map.of()).forEach((t, v) -> speed.merge(t, v, Double::sum));
        }

        void drop(EmployeeId id) {
            members.remove(id);
            proff.getOrDefault(id, Map.of()).forEach((t, v) -> speed.merge(t, -v, Double::sum));
        }
    }

    private List<Brigade> form(int k, boolean snake) {
        List<EmployeeId> pool = new ArrayList<>(ids);
        if (pool.size() > k * MAX) {
            // keep the best expert per needed type, then fill with strongest employees
            List<EmployeeId> keep = new ArrayList<>();
            for (String type : totalWork.keySet()) {
                EmployeeId best = null;
                double bv = EPS;
                for (EmployeeId id : pool) {
                    if (keep.contains(id)) continue;
                    double v = p(id, type);
                    if (v > bv) {
                        bv = v;
                        best = id;
                    }
                }
                if (best != null) keep.add(best);
            }
            List<EmployeeId> rest = pool.stream().filter(id -> !keep.contains(id))
                    .sorted(Comparator.comparingDouble((EmployeeId id) -> total(id)).reversed()).toList();
            pool = new ArrayList<>(keep);
            pool.addAll(rest.subList(0, Math.min(rest.size(), k * MAX - keep.size())));
        }
        List<Brigade> bs = new ArrayList<>();
        for (int i = 0; i < k; i++) bs.add(new Brigade());

        if (snake) { // balanced draft by strength
            pool.sort(Comparator.comparingDouble((EmployeeId id) -> total(id)).reversed());
            int placed = 0, step = 0;
            while (placed < pool.size()) {
                int r = step++ % (2 * k);
                int j = r < k ? r : 2 * k - 1 - r;
                if (bs.get(j).members.size() >= MAX) continue;
                bs.get(j).add(pool.get(placed++));
            }
            for (Brigade b : bs) if (b.members.size() < MIN) return null;
        } else { // cluster by qualification profile -> specialization
            pool.sort(Comparator.comparing((EmployeeId id) -> dominant(id))
                    .thenComparing(Comparator.comparingDouble((EmployeeId id) -> share(id)).reversed())
                    .thenComparing(Comparator.comparingDouble((EmployeeId id) -> total(id)).reversed()));
            int[] size = new int[k];
            Arrays.fill(size, MIN);
            int left = pool.size() - k * MIN;
            for (int i = 0; left > 0; left--, i = (i + 1) % k) size[i]++;
            int pos = 0;
            for (int j = 0; j < k; j++)
                for (int s = 0; s < size[j]; s++) bs.get(j).add(pool.get(pos++));
        }
        return bs;
    }

    // --- HEFT schedule with gap insertion ----------------------------------------

    private double schedule(List<Brigade> bs) {
        for (Brigade b : bs) b.jobs.clear();
        Map<EventId, Double> finish = new HashMap<>();
        for (Task t : rankOrder(bs.size())) {
            double ready = 0;
            for (Task p : t.preds) ready = Math.max(ready, finish.get(p.id));
            Brigade best = null;
            double bestStart = 0, bestFinish = Double.POSITIVE_INFINITY, bestSpeed = Double.POSITIVE_INFINITY;
            for (Brigade b : bs) {
                double s = b.speed.getOrDefault(t.type, 0.0);
                if (s <= EPS) continue; // rule: >= 1 member qualified for this type
                double p = t.work / s;
                double start = earliestSlot(b.jobs, ready, p);
                double fin = start + p;
                if (fin < bestFinish - EPS || (Math.abs(fin - bestFinish) <= EPS && s < bestSpeed - EPS)) {
                    best = b;
                    bestStart = start;
                    bestFinish = fin;
                    bestSpeed = s;
                }
            }
            if (best == null) return Double.POSITIVE_INFINITY; // type not covered by any brigade
            best.jobs.add(new Scheduled(t, bestStart, bestFinish));
            best.jobs.sort(Comparator.comparingDouble(Scheduled::start));
            finish.put(t.id, bestFinish);
        }
        double mk = 0;
        for (Brigade b : bs) for (Scheduled j : b.jobs) mk = Math.max(mk, j.finish());
        return mk;
    }

    private static double earliestSlot(List<Scheduled> jobs, double ready, double p) {
        double cursor = ready;
        for (Scheduled j : jobs) {
            if (j.start() - cursor >= p - EPS) return cursor; // fits in the gap before j
            cursor = Math.max(cursor, j.finish());
        }
        return cursor;
    }

    /**
     * Dissolve brigades that got no events; reattach members where they help most.
     */
    private double repairAndSchedule(List<Brigade> bs) {
        while (true) {
            double mk = schedule(bs);
            if (Double.isInfinite(mk)) return mk;
            List<Brigade> unused = bs.stream().filter(b -> b.jobs.isEmpty()).toList();
            if (unused.isEmpty()) return mk;
            List<EmployeeId> pool = new ArrayList<>();
            for (Brigade u : unused) pool.addAll(u.members);
            bs.removeAll(unused);
            pool.sort(Comparator.comparingDouble((EmployeeId id) -> total(id)).reversed());
            for (EmployeeId id : pool) {
                Brigade target = null;
                double bestGain = -1;
                for (Brigade b : bs) {
                    if (b.members.size() >= MAX) continue;
                    double gain = gain(b, id);
                    if (gain > bestGain) {
                        bestGain = gain;
                        target = b;
                    }
                }
                if (target != null) target.add(id); // else stays in reserve
            }
        }
    }

    private double gain(Brigade b, EmployeeId id) {
        Map<String, Double> work = new HashMap<>();
        for (Scheduled j : b.jobs) work.merge(j.task().type, j.task().work, Double::sum);
        double g = 0;
        for (var e : work.entrySet()) {
            double s = b.speed.getOrDefault(e.getKey(), 0.0), pi = p(id, e.getKey());
            if (pi > EPS && s > EPS) g += e.getValue() * (1.0 / s - 1.0 / (s + pi));
        }
        return g;
    }

    // --- local search on the best partition ---------------------------------------

    private double localSearch(List<List<EmployeeId>> part, double bestMake) {
        int evals = 0;
        boolean improved = true;
        while (improved && evals < LS_BUDGET) {
            improved = false;
            outer:
            for (int a = 0; a < part.size(); a++)
                for (int b = a + 1; b < part.size(); b++) {
                    List<EmployeeId> A = part.get(a), B = part.get(b);
                    for (EmployeeId x : new ArrayList<>(A))
                        for (EmployeeId y : new ArrayList<>(B)) {
                            if (evals >= LS_BUDGET) break outer;
                            A.remove(x);
                            B.remove(y);
                            A.add(y);
                            B.add(x);
                            evals++;
                            double mk = evaluate(part);
                            if (mk < bestMake - EPS) {
                                bestMake = mk;
                                improved = true;
                                break outer;
                            }
                            A.remove(y);
                            B.remove(x);
                            A.add(x);
                            B.add(y);
                        }
                }
        }
        return bestMake;
    }

    private double evaluate(List<List<EmployeeId>> part) {
        List<Brigade> bs = fromPartition(part);
        double mk = schedule(bs);
        for (Brigade b : bs) if (b.jobs.isEmpty()) return Double.POSITIVE_INFINITY; // every brigade >= 1 event
        return mk;
    }

    private List<Brigade> fromPartition(List<List<EmployeeId>> part) {
        List<Brigade> bs = new ArrayList<>();
        for (List<EmployeeId> mem : part) {
            Brigade b = new Brigade();
            for (EmployeeId id : mem) b.add(id);
            bs.add(b);
        }
        return bs;
    }

    private static List<List<EmployeeId>> snapshot(List<Brigade> bs) {
        return bs.stream().map(b -> new ArrayList<>(b.members)).collect(Collectors.toList());
    }

    // --- helpers / output -----------------------------------------------------------

    private double p(EmployeeId id, String type) {
        return proff.getOrDefault(id, Map.of()).getOrDefault(type, 0.0);
    }

    private double total(EmployeeId id) {
        return proff.getOrDefault(id, Map.of()).values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private String dominant(EmployeeId id) {
        return proff.getOrDefault(id, Map.of()).entrySet().stream()
                .filter(e -> e.getValue() > EPS)
                .max(Comparator.comparingDouble((Map.Entry<String, Double> e) -> e.getValue())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey).orElse("\uffff");
    }

    private double share(EmployeeId id) {
        double t = total(id);
        return t <= EPS ? 0 : p(id, dominant(id)) / t;
    }

    private Solution materialize(List<Brigade> bs) {
        List<BrigadeSolution> out = new ArrayList<>();
        Set<EmployeeId> used = new HashSet<>();
        double mk = 0;
        for (Brigade b : bs) {
            List<Job> jobs = new ArrayList<>();
            double last = 0;
            for (Scheduled j : b.jobs) {
                jobs.add(new Job(j.task().id, j.task().type,
                        Duration.ofMillis(Math.round(j.start() * 1000)),
                        Duration.ofMillis(Math.round((j.finish() - j.start()) * 1000))));
                last = j.finish();
            }
            mk = Math.max(mk, last);
            used.addAll(b.members);
            out.add(new BrigadeSolution(List.copyOf(b.members), List.copyOf(jobs),
                    Duration.ofMillis(Math.round(last * 1000))));
        }
        out.sort(Comparator.comparing(BrigadeSolution::finishesAt).reversed());
        List<EmployeeId> reserve = ids.stream().filter(id -> !used.contains(id)).toList();
        return new Solution(List.copyOf(out), Duration.ofMillis(Math.round(mk * 1000)), reserve);
    }
}