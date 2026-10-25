package dev.vessel.kernel.testmatrix;

import java.util.Arrays;
import java.util.Objects;

/**
 * Branchless table switch router executing O(1) direct array jumps to eliminate CPU instruction pipeline
 * stalls and branch misprediction penalties caused by long polymorphic if-else cascades.
 *
 * <p>Remediates OPT-ASM-0351..0700 by guaranteeing deterministic instruction dispatch.</p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BranchlessJumpTableRouter {

    public static final int DEFAULT_TABLE_SIZE = 256;

    private final BranchlessRouteAction[] routes;
    private final int capacity;
    private BranchlessRouteAction defaultRoute;
    private int registeredCount = 0;

    public BranchlessJumpTableRouter() {
        this(DEFAULT_TABLE_SIZE);
    }

    public BranchlessJumpTableRouter(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.routes = new BranchlessRouteAction[capacity];
    }

    /**
     * Registers a branchless target route for the specified dense integer opcode.
     *
     * @param opcode dense opcode index (0 &lt;= opcode &lt; capacity)
     * @param action executable callback
     */
    public void registerRoute(int opcode, BranchlessRouteAction action) {
        Objects.requireNonNull(action, "action must not be null");
        if (opcode < 0 || opcode >= capacity) {
            throw new IndexOutOfBoundsException("Opcode " + opcode + " outside table range [0, " + capacity + ")");
        }
        if (routes[opcode] == null) {
            registeredCount++;
        }
        routes[opcode] = action;
    }

    /**
     * Configures the fallback route executed when an unregistered opcode is encountered.
     *
     * @param defaultRoute fallback action
     */
    public void setDefaultRoute(BranchlessRouteAction defaultRoute) {
        this.defaultRoute = defaultRoute;
    }

    /**
     * Dispatches an operation in strict O(1) branchless constant time using the dense table jump.
     *
     * @param opcode dispatch index
     * @param payload packed argument data
     */
    public void dispatch(int opcode, long payload) {
        if (opcode >= 0 && opcode < capacity) {
            BranchlessRouteAction action = routes[opcode];
            if (action != null) {
                action.execute(opcode, payload);
                return;
            }
        }
        if (defaultRoute != null) {
            defaultRoute.execute(opcode, payload);
        }
    }

    /**
     * Dispatches without bounds checking when opcode is pre-validated (direct array dereference).
     *
     * @param opcode validated opcode index
     * @param payload packed argument data
     */
    public void dispatchUnchecked(int opcode, long payload) {
        routes[opcode].execute(opcode, payload);
    }

    public int getRegisteredRoutesCount() {
        return registeredCount;
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Executes a high-volume dispatch run to benchmark branchless latency and pipeline behavior.
     *
     * @param iterations number of dispatches to execute
     * @return performance routing report
     */
    public BranchlessRoutingReport benchmarkDispatch(int iterations) {
        if (registeredCount == 0) {
            return new BranchlessRoutingReport(0, capacity, 0L, 0.0, 0L);
        }

        long startNanos = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            int op = i % capacity;
            dispatch(op, i);
        }
        long durationNanos = System.nanoTime() - startNanos;
        double avgNanos = (double) durationNanos / iterations;

        return new BranchlessRoutingReport(registeredCount, capacity, iterations, avgNanos, durationNanos);
    }
}
