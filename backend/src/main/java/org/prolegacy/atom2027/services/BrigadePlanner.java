package org.prolegacy.atom2027.services;

import org.prolegacy.atom2027.entities.Employee;
import org.prolegacy.atom2027.entities.Event;
import org.prolegacy.atom2027.entities.ids.EmployeeId;

import java.time.Duration;
import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * speed(brigade, type) = sum of member qualifications (missing -> 0)
 * time(event, brigade) = 5 * Tbase / speed.  Brigades run in parallel,
 * events inside a brigade sequentially. Minimizes wave makespan.
 */
public class BrigadePlanner {

    public record Task(Event event, String type, double work) {
    } // work = 5 * Tbase, seconds

    public record Assignment(Event event, Duration duration, Duration startsAt) {
    }

    public record BrigadePlan(List<EmployeeId> members, List<Assignment> assignments, Duration finishesAt) {
    }

    private static final int MIN = 2, MAX = 5;
    private static final double EPS = 1e-9;

    private final List<EmployeeId> ids;
    private final Map<EmployeeId, Map<String, Double>> proff; // employee -> type -> qualification

    public BrigadePlanner(Collection<Employee> employees, Map<EmployeeId, Map<String, Double>> proff) {
        this.ids = employees.stream().map(Employee::getId).toList();
        this.proff = proff;
    }

    public List<BrigadePlan> plan(List<Event> batch, ToDoubleFunction<Event> baseSeconds) {
        if (batch.isEmpty()) return List.of();
        if (ids.size() < MIN) throw new IllegalStateException("At least 2 employees are required");

        List<Task> tasks = batch.stream()
                .map(e -> new Task(e, e.getResourceType(), 5.0 * baseSeconds.applyAsDouble(e)))
                .sorted(Comparator.comparingDouble(Task::work).reversed())
                .toList();

        Map<String, Double> p = new HashMap<>(), w = new HashMap<>();
        for (EmployeeId id : ids) proff.getOrDefault(id, Map.of()).forEach((t, v) -> p.merge(t, v, Double::sum));
        for (Task t : tasks) w.merge(t.type(), t.work(), Double::sum);
        for (String t : w.keySet())
            if (p.getOrDefault(t, 0.0) <= EPS)
                throw new IllegalStateException("No qualified employees for resource type " + t);

        double lowerBound = w.entrySet().stream()
                .mapToDouble(e -> e.getValue() / p.get(e.getKey())).max().orElse(0);

        List<Brigade> best = null;
        double bestMake = Double.POSITIVE_INFINITY, bestTotal = Double.POSITIVE_INFINITY;
        int kMax = Math.min(tasks.size(), ids.size() / MIN);

        for (int k = kMax; k >= 1; k--) {
            for (boolean snake : new boolean[]{false, true}) {
                List<Brigade> bs = construct(k, tasks, snake);
                if (bs == null) continue;
                improve(bs);
                double make = makespan(bs), total = totalTime(bs);
                if (better(make, total, bestMake, bestTotal)) {
                    best = bs;
                    bestMake = make;
                    bestTotal = total;
                }
            }
            if (bestMake <= lowerBound + EPS) break; // provably can't do better
        }
        if (best == null || Double.isInfinite(bestMake))
            throw new IllegalStateException("Cannot cover all resource types with brigades of size 2..5");
        return materialize(best);
    }

    // --- construction -----------------------------------------------------

    private List<Brigade> construct(int k, List<Task> tasks, boolean snake) {
        List<EmployeeId> pool = new ArrayList<>(ids);
        if (pool.size() > k * MAX) { // keep only the strongest 5k, rest = reserve
            pool.sort(Comparator.comparingDouble((EmployeeId id) -> total(id)).reversed());
            pool = new ArrayList<>(pool.subList(0, k * MAX));
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
        } else { // cluster by qualification profile -> specialized brigades
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

        // biggest tasks first -> brigade that finishes earliest;
        // reserve the last tasks so every brigade gets at least one event
        int empties = k;
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            boolean mustFill = tasks.size() - i == empties;
            Brigade target = null;
            double bestFinish = Double.POSITIVE_INFINITY;
            for (Brigade b : bs) {
                if (mustFill && !b.tasks.isEmpty()) continue;
                b.add(t);
                double f = b.time();
                b.drop(t);
                if (f < bestFinish) {
                    bestFinish = f;
                    target = b;
                }
            }
            if (target == null)
                for (Brigade b : bs)
                    if (!mustFill || b.tasks.isEmpty()) {
                        target = b;
                        break;
                    }
            if (target.tasks.isEmpty()) empties--;
            target.add(t);
        }
        return bs;
    }

    // --- local search (first-improvement hill climbing) --------------------

    private void improve(List<Brigade> bs) {
        for (int pass = 0; pass < 50; pass++) {
            boolean improved = false;
            for (int i = 0; i < bs.size(); i++)
                for (int j = i + 1; j < bs.size(); j++) {
                    improved |= swapMembers(bs, i, j);
                    improved |= moveMembers(bs, i, j);
                    improved |= moveMembers(bs, j, i);
                    improved |= swapTasks(bs, i, j);
                    improved |= moveTasks(bs, i, j);
                    improved |= moveTasks(bs, j, i);
                }
            if (!improved) return;
        }
    }

    private boolean swapMembers(List<Brigade> bs, int i, int j) {
        Brigade a = bs.get(i), b = bs.get(j);
        for (EmployeeId x : new ArrayList<>(a.members))
            for (EmployeeId y : new ArrayList<>(b.members)) {
                double m0 = makespan(bs), t0 = totalTime(bs);
                a.drop(x);
                b.drop(y);
                a.add(y);
                b.add(x);
                if (better(makespan(bs), totalTime(bs), m0, t0)) return true;
                a.drop(y);
                b.drop(x);
                a.add(x);
                b.add(y);
            }
        return false;
    }

    private boolean moveMembers(List<Brigade> bs, int from, int to) {
        Brigade a = bs.get(from), b = bs.get(to);
        if (a.members.size() <= MIN || b.members.size() >= MAX) return false;
        for (EmployeeId x : new ArrayList<>(a.members)) {
            double m0 = makespan(bs), t0 = totalTime(bs);
            a.drop(x);
            b.add(x);
            if (better(makespan(bs), totalTime(bs), m0, t0)) return true;
            b.drop(x);
            a.add(x);
        }
        return false;
    }

    private boolean swapTasks(List<Brigade> bs, int i, int j) {
        Brigade a = bs.get(i), b = bs.get(j);
        for (Task x : new ArrayList<>(a.tasks))
            for (Task y : new ArrayList<>(b.tasks)) {
                double m0 = makespan(bs), t0 = totalTime(bs);
                a.drop(x);
                b.drop(y);
                a.add(y);
                b.add(x);
                if (better(makespan(bs), totalTime(bs), m0, t0)) return true;
                a.drop(y);
                b.drop(x);
                a.add(x);
                b.add(y);
            }
        return false;
    }

    private boolean moveTasks(List<Brigade> bs, int from, int to) {
        Brigade a = bs.get(from), b = bs.get(to);
        if (a.tasks.size() <= 1) return false; // every brigade keeps >= 1 event
        for (Task x : new ArrayList<>(a.tasks)) {
            double m0 = makespan(bs), t0 = totalTime(bs);
            a.drop(x);
            b.add(x);
            if (better(makespan(bs), totalTime(bs), m0, t0)) return true;
            b.drop(x);
            a.add(x);
        }
        return false;
    }

    // --- model -------------------------------------------------------------

    private final class Brigade {
        final List<EmployeeId> members = new ArrayList<>();
        final List<Task> tasks = new ArrayList<>();
        final Map<String, Double> speed = new HashMap<>();
        final Map<String, Double> work = new HashMap<>();

        void add(EmployeeId id) {
            members.add(id);
            proff.getOrDefault(id, Map.of()).forEach((t, v) -> speed.merge(t, v, Double::sum));
        }

        void drop(EmployeeId id) {
            members.remove(id);
            proff.getOrDefault(id, Map.of()).forEach((t, v) -> speed.merge(t, -v, Double::sum));
        }

        void add(Task t) {
            tasks.add(t);
            work.merge(t.type(), t.work(), Double::sum);
        }

        void drop(Task t) {
            tasks.remove(t);
            work.merge(t.type(), -t.work(), Double::sum);
        }

        double time() { // seconds; +inf if some needed type has zero qualification
            double c = 0;
            for (var e : work.entrySet()) {
                if (e.getValue() <= EPS) continue;
                double s = speed.getOrDefault(e.getKey(), 0.0);
                if (s <= EPS) return Double.POSITIVE_INFINITY;
                c += e.getValue() / s;
            }
            return c;
        }
    }

    private static double makespan(List<Brigade> bs) {
        double m = 0;
        for (Brigade b : bs) m = Math.max(m, b.time());
        return m;
    }

    private static double totalTime(List<Brigade> bs) {
        double s = 0;
        for (Brigade b : bs) s += b.time();
        return s;
    }

    private static boolean better(double make, double total, double make0, double total0) {
        return make < make0 - EPS || (Math.abs(make - make0) <= EPS && total < total0 - EPS);
    }

    private double total(EmployeeId id) {
        return proff.getOrDefault(id, Map.of()).values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private String dominant(EmployeeId id) {
        return proff.getOrDefault(id, Map.of()).entrySet().stream()
                .filter(e -> e.getValue() > EPS)
                .max(Comparator.comparingDouble((Map.Entry<String, Double> e) -> e.getValue())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey).orElse("\uffff"); // unqualified go last
    }

    private double share(EmployeeId id) {
        double t = total(id);
        return t <= EPS ? 0 : proff.getOrDefault(id, Map.of()).getOrDefault(dominant(id), 0.0) / t;
    }

    private List<BrigadePlan> materialize(List<Brigade> bs) {
        List<BrigadePlan> plans = new ArrayList<>();
        for (Brigade b : bs) {
            Duration cursor = Duration.ZERO;
            List<Assignment> assignments = new ArrayList<>();
            List<Task> ordered = b.tasks.stream()
                    .sorted(Comparator.comparingDouble(Task::work).reversed()).toList();
            for (Task t : ordered) {
                Duration d = Duration.ofMillis(Math.round(1000.0 * t.work() / b.speed.get(t.type())));
                assignments.add(new Assignment(t.event(), d, cursor));
                cursor = cursor.plus(d);
            }
            plans.add(new BrigadePlan(List.copyOf(b.members), List.copyOf(assignments), cursor));
        }
        plans.sort(Comparator.comparing(BrigadePlan::finishesAt).reversed());
        return plans;
    }
}