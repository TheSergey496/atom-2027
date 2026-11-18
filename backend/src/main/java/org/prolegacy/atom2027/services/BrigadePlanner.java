package org.prolegacy.atom2027.services;

import org.prolegacy.atom2027.entities.Employee;
import org.prolegacy.atom2027.entities.Event;
import org.prolegacy.atom2027.entities.ids.EmployeeId;

import java.time.Duration;
import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Forms brigades ONCE (2..5 people, each person in exactly one brigade),
 * assigns all events to them across dependency waves, every brigade gets >= 1 event.
 * speed(brigade,type) = sum of member qualifications; time = 5*Tbase/speed.
 * Objective: minimize total schedule time (sum of wave makespans).
 */
public class BrigadePlanner {

    public record Task(Event event, String type, double work, int wave) {}
    public record Assignment(Event event, Duration duration, Duration startsAt) {}
    public record BrigadePlan(List<EmployeeId> members, List<Assignment> assignments, Duration finishesAt) {}

    private static final int MIN = 2, MAX = 5;
    private static final double EPS = 1e-9;

    private final List<EmployeeId> ids;
    private final Map<EmployeeId, Map<String, Double>> proff;

    public BrigadePlanner(Collection<Employee> employees, Map<EmployeeId, Map<String, Double>> proff) {
        this.ids = employees.stream().map(Employee::getId).toList();
        this.proff = proff;
    }

    public List<BrigadePlan> planAll(List<List<Event>> waves, ToDoubleFunction<Event> baseSeconds) {
        List<Task> tasks = new ArrayList<>();
        for (int w = 0; w < waves.size(); w++)
            for (Event e : waves.get(w))
                tasks.add(new Task(e, e.getResourceType(), 5.0 * baseSeconds.applyAsDouble(e), w));
        if (tasks.isEmpty()) return List.of();
        if (ids.size() < MIN) throw new IllegalStateException("At least 2 employees are required");

        Set<String> types = new HashSet<>();
        Map<String, Double> p = new HashMap<>();
        for (Task t : tasks) types.add(t.type());
        for (EmployeeId id : ids) proff.getOrDefault(id, Map.of()).forEach((t, v) -> p.merge(t, v, Double::sum));
        for (String t : types)
            if (p.getOrDefault(t, 0.0) <= EPS)
                throw new IllegalStateException("No qualified employees for resource type " + t);

        long useful = ids.stream().filter(id -> rel(id, types) > EPS).count();
        int kMax = (int) Math.min(tasks.size(), Math.min(ids.size() / MIN, useful));

        double lowerBound = 0;
        for (List<Event> wave : waves) {
            Map<String, Double> w = new HashMap<>();
            for (Event e : wave) w.merge(e.getResourceType(), 5.0 * baseSeconds.applyAsDouble(e), Double::sum);
            double mx = 0;
            for (var e : w.entrySet()) mx = Math.max(mx, e.getValue() / p.get(e.getKey()));
            lowerBound += mx;
        }

        List<Brigade> best = null;
        double bestTime = Double.POSITIVE_INFINITY;
        for (int k = kMax; k >= 1; k--) {
            List<Brigade> bs = formBrigades(k, types);
            assignTasks(bs, tasks, waves.size());
            repairEmpty(bs);
            improve(bs, waves.size());
            double t = total(rows(bs, waves.size()));
            if (t < bestTime - EPS) { bestTime = t; best = bs; }
            if (bestTime <= lowerBound + EPS) break;
        }
        if (best == null || Double.isInfinite(bestTime))
            throw new IllegalStateException("Cannot build a feasible brigade plan");
        return materialize(best, waves.size());
    }

    // --- formation: one partition of employees, sizes 2..5 ------------------

    private List<Brigade> formBrigades(int k, Set<String> types) {
        List<EmployeeId> pool = new ArrayList<>(ids);
        if (pool.size() > k * MAX) {
            pool.sort(Comparator.comparingDouble((EmployeeId id) -> rel(id, types)).reversed());
            pool = new ArrayList<>(pool.subList(0, k * MAX));
        }
        pool.sort(Comparator.comparing((EmployeeId id) -> dominant(id, types))
                .thenComparing(Comparator.comparingDouble((EmployeeId id) -> share(id, types)).reversed())
                .thenComparing(Comparator.comparingDouble((EmployeeId id) -> rel(id, types)).reversed()));

        int[] size = new int[k];
        Arrays.fill(size, MIN);
        int left = pool.size() - k * MIN;
        for (int i = 0; left > 0; left--, i = (i + 1) % k) size[i]++;

        List<Brigade> bs = new ArrayList<>();
        int pos = 0;
        for (int j = 0; j < k; j++) {
            Brigade b = new Brigade();
            for (int s = 0; s < size[j]; s++) b.add(pool.get(pos++));
            bs.add(b);
        }
        // every brigade needs >= 1 member qualified for a present type
        for (Brigade b : bs) {
            if (b.members.stream().anyMatch(id -> rel(id, types) > EPS)) continue;
            Brigade donor = bs.stream()
                    .filter(d -> d != b && d.members.stream().filter(id -> rel(id, types) > EPS).count() >= 2)
                    .findFirst().orElseThrow();
            EmployeeId give = b.members.get(b.members.size() - 1);
            EmployeeId take = donor.members.stream().filter(id -> rel(id, types) > EPS)
                    .reduce((a, c) -> c).orElseThrow();
            b.drop(give); donor.drop(take); b.add(take); donor.add(give);
        }
        return bs;
    }

    // --- assignment: wave by wave, biggest first, earliest-finishing brigade --

    private void assignTasks(List<Brigade> bs, List<Task> tasks, int waveCount) {
        int assigned = 0;
        for (int w = 0; w < waveCount; w++) {
            List<Map<String, Double>> load = new ArrayList<>();
            for (Brigade b : bs) load.add(new HashMap<>());
            int wi = w;
            List<Task> waveTasks = tasks.stream().filter(t -> t.wave() == wi)
                    .sorted(Comparator.comparingDouble(Task::work).reversed()).toList();
            for (Task t : waveTasks) {
                long empties = bs.stream().filter(b -> b.tasks.isEmpty()).count();
                boolean mustFill = tasks.size() - assigned == empties;
                int bestIdx = -1;
                double bestFinish = Double.POSITIVE_INFINITY;
                for (int relax = 0; relax < 2 && bestIdx < 0; relax++) {
                    for (int j = 0; j < bs.size(); j++) {
                        Brigade b = bs.get(j);
                        double s = b.speed.getOrDefault(t.type(), 0.0);
                        if (s <= EPS) continue;
                        if (mustFill && relax == 0 && !b.tasks.isEmpty()) continue;
                        double f = (load.get(j).getOrDefault(t.type(), 0.0) + t.work()) / s;
                        for (var e : load.get(j).entrySet())
                            if (!e.getKey().equals(t.type())) f += e.getValue() / b.speed.get(e.getKey());
                        if (f < bestFinish) { bestFinish = f; bestIdx = j; }
                    }
                }
                if (bestIdx < 0) throw new IllegalStateException("No brigade can serve type " + t.type());
                bs.get(bestIdx).add(t);
                load.get(bestIdx).merge(t.type(), t.work(), Double::sum);
                assigned++;
            }
        }
    }

    // --- guarantee rule 2: steal a compatible event, else dissolve -----------

    private void repairEmpty(List<Brigade> bs) {
        for (Iterator<Brigade> it = bs.iterator(); it.hasNext(); ) {
            Brigade b = it.next();
            if (!b.tasks.isEmpty()) continue;
            Brigade donor = null;
            Task steal = null;
            List<Brigade> donors = new ArrayList<>(bs);
            donors.sort(Comparator.comparingInt((Brigade d) -> d.tasks.size()).reversed());
            for (Brigade d : donors) {
                if (d == b || d.tasks.size() < 2) continue;
                for (Task t : d.tasks)
                    if (b.speed.getOrDefault(t.type(), 0.0) > EPS) { donor = d; steal = t; break; }
                if (donor != null) break;
            }
            if (donor != null) {
                donor.drop(steal);
                b.add(steal);
            } else { // cannot serve any event -> dissolve, reseat members where there is room
                for (EmployeeId m : new ArrayList<>(b.members)) {
                    b.drop(m);
                    for (Brigade d : bs)
                        if (d != b && d.members.size() < MAX) { d.add(m); break; }
                }
                it.remove();
            }
        }
    }

    // --- local search on the whole schedule (member swaps / moves) -----------

    private void improve(List<Brigade> bs, int W) {
        double[][] wt = rows(bs, W);
        for (int pass = 0; pass < 20; pass++) {
            boolean improved = false;
            for (int i = 0; i < bs.size(); i++)
                for (int j = i + 1; j < bs.size(); j++) {
                    improved |= swapMembers(bs, wt, i, j, W);
                    improved |= moveMembers(bs, wt, i, j, W);
                    improved |= moveMembers(bs, wt, j, i, W);
                }
            if (!improved) return;
        }
    }

    private boolean swapMembers(List<Brigade> bs, double[][] wt, int i, int j, int W) {
        Brigade a = bs.get(i), b = bs.get(j);
        double t0 = total(wt);
        for (EmployeeId x : new ArrayList<>(a.members))
            for (EmployeeId y : new ArrayList<>(b.members)) {
                a.drop(x); b.drop(y); a.add(y); b.add(x);
                double[] ri = a.row(W), rj = b.row(W);
                if (totalWith(wt, i, ri, j, rj) < t0 - EPS) { wt[i] = ri; wt[j] = rj; return true; }
                a.drop(y); b.drop(x); a.add(x); b.add(y);
            }
        return false;
    }

    private boolean moveMembers(List<Brigade> bs, double[][] wt, int i, int j, int W) {
        Brigade a = bs.get(i), b = bs.get(j);
        if (a.members.size() <= MIN || b.members.size() >= MAX) return false;
        double t0 = total(wt);
        for (EmployeeId x : new ArrayList<>(a.members)) {
            a.drop(x); b.add(x);
            double[] ri = a.row(W), rj = b.row(W);
            if (totalWith(wt, i, ri, j, rj) < t0 - EPS) { wt[i] = ri; wt[j] = rj; return true; }
            b.drop(x); a.add(x);
        }
        return false;
    }

    // --- schedule materialization -------------------------------------------

    private List<BrigadePlan> materialize(List<Brigade> bs, int W) {
        for (Brigade b : bs) b.tasks.sort(Comparator.comparingInt(Task::wave)); // stable
        double[][] wt = rows(bs, W);
        double[] waveStart = new double[W];
        double acc = 0;
        for (int w = 0; w < W; w++) {
            waveStart[w] = acc;
            double mx = 0;
            for (double[] r : wt) mx = Math.max(mx, r[w]);
            acc += mx;
        }
        List<BrigadePlan> plans = new ArrayList<>();
        for (Brigade b : bs) {
            double cursor = 0;
            List<Assignment> as = new ArrayList<>();
            for (Task t : b.tasks) {
                double start = Math.max(cursor, waveStart[t.wave()]);
                double dur = t.work() / b.speed.get(t.type());
                as.add(new Assignment(t.event(), durOf(dur), durOf(start)));
                cursor = start + dur;
            }
            plans.add(new BrigadePlan(List.copyOf(b.members), List.copyOf(as), durOf(cursor)));
        }
        plans.sort(Comparator.comparing(BrigadePlan::finishesAt).reversed());
        return plans;
    }

    // --- model & scoring -----------------------------------------------------

    private final class Brigade {
        final List<EmployeeId> members = new ArrayList<>();
        final List<Task> tasks = new ArrayList<>();
        final Map<String, Double> speed = new HashMap<>();

        void add(EmployeeId id) {
            members.add(id);
            proff.getOrDefault(id, Map.of()).forEach((t, v) -> speed.merge(t, v, Double::sum));
        }
        void drop(EmployeeId id) {
            members.remove(id);
            proff.getOrDefault(id, Map.of()).forEach((t, v) -> speed.merge(t, -v, Double::sum));
        }
        void add(Task t) { tasks.add(t); }
        void drop(Task t) { tasks.remove(t); }

        double[] row(int W) { // completion time per wave
            Map<Integer, Map<String, Double>> byWave = new HashMap<>();
            for (Task t : tasks)
                byWave.computeIfAbsent(t.wave(), k -> new HashMap<>()).merge(t.type(), t.work(), Double::sum);
            double[] r = new double[W];
            for (var e : byWave.entrySet()) {
                double c = 0;
                for (var te : e.getValue().entrySet()) {
                    double s = speed.getOrDefault(te.getKey(), 0.0);
                    if (s <= EPS) { c = Double.POSITIVE_INFINITY; break; }
                    c += te.getValue() / s;
                }
                r[e.getKey()] = c;
            }
            return r;
        }
    }

    private static double[][] rows(List<Brigade> bs, int W) {
        double[][] wt = new double[bs.size()][];
        for (int b = 0; b < bs.size(); b++) wt[b] = bs.get(b).row(W);
        return wt;
    }
    private static double total(double[][] wt) {
        double sum = 0;
        for (int w = 0; w < wt[0].length; w++) {
            double mx = 0;
            for (double[] r : wt) mx = Math.max(mx, r[w]);
            sum += mx;
        }
        return sum;
    }
    private static double totalWith(double[][] wt, int i, double[] ri, int j, double[] rj) {
        double sum = 0;
        for (int w = 0; w < wt[i].length; w++) {
            double mx = Math.max(ri[w], rj[w]);
            for (int b = 0; b < wt.length; b++)
                if (b != i && b != j) mx = Math.max(mx, wt[b][w]);
            sum += mx;
        }
        return sum;
    }

    private double rel(EmployeeId id, Set<String> types) {
        double s = 0;
        for (String t : types) s += proff.getOrDefault(id, Map.of()).getOrDefault(t, 0.0);
        return s;
    }
    private String dominant(EmployeeId id, Set<String> types) {
        String best = "\uffff";
        double bv = EPS;
        for (String t : types) {
            double v = proff.getOrDefault(id, Map.of()).getOrDefault(t, 0.0);
            if (v > bv || (v == bv && best.compareTo(t) < 0 && v > EPS)) { bv = v; best = t; }
        }
        return best;
    }
    private double share(EmployeeId id, Set<String> types) {
        double r = rel(id, types);
        return r <= EPS ? 0 : proff.getOrDefault(id, Map.of()).getOrDefault(dominant(id, types), 0.0) / r;
    }
    private static Duration durOf(double seconds) { return Duration.ofMillis(Math.round(seconds * 1000)); }
}