package dev.vessel.kernel.testmatrix;

import dev.vessel.kernel.identity.SourceLoaderKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Automated test harness executing concurrent initialization and simulated runtime verification
 * across heterogeneous loaders (Forge, Fabric, Paper, Quilt, NeoForge) without classpath collisions.
 *
 * <p>Remediates ERR-10 and ERR-18 by verifying package boundary isolation and class namespace integrity.</p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MultiLoaderConcurrentTestHarness {

    public static final long DEFAULT_TIMEOUT_MILLIS = 10_000L;
    public static final String CONFLICT_PACKAGE_MSG = "Cross-loader package collision detected for package: ";
    public static final String CONFLICT_CLASS_MSG = "Cross-loader entrypoint collision detected for class: ";

    private final List<MultiLoaderTestTarget> registeredTargets = new ArrayList<>();
    private final Map<String, SourceLoaderKind> claimedPackages = new ConcurrentHashMap<>();
    private final Map<String, SourceLoaderKind> claimedClasses = new ConcurrentHashMap<>();

    public MultiLoaderConcurrentTestHarness() {
    }

    /**
     * Registers a module target into the test harness suite.
     *
     * @param target module test target descriptor
     */
    public void registerTarget(MultiLoaderTestTarget target) {
        Objects.requireNonNull(target, "target must not be null");
        registeredTargets.add(target);
    }

    /**
     * Returns an unmodifiable snapshot of currently registered targets.
     *
     * @return list of registered targets
     */
    public List<MultiLoaderTestTarget> getRegisteredTargets() {
        return Collections.unmodifiableList(registeredTargets);
    }

    /**
     * Executes concurrent initialization across all registered targets, evaluating classpath
     * boundaries and package namespace separation.
     *
     * @param timeoutMillis maximum test execution timeout in milliseconds
     * @param strictMode if true, throws {@link ClasspathCollisionException} on collision
     * @return multi-loader test execution summary
     * @throws ClasspathCollisionException if strictMode is true and a collision occurs
     */
    public MultiLoaderTestResult executeConcurrentTest(long timeoutMillis, boolean strictMode)
            throws ClasspathCollisionException {
        long startNanos = System.nanoTime();
        claimedPackages.clear();
        claimedClasses.clear();

        List<String> conflicts = Collections.synchronizedList(new ArrayList<>());
        int targetCount = registeredTargets.size();

        if (targetCount == 0) {
            return new MultiLoaderTestResult(true, 0, 0, List.of(), System.nanoTime() - startNanos);
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(targetCount, 8));
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(targetCount);

        for (int i = 0; i < targetCount; i++) {
            MultiLoaderTestTarget target = registeredTargets.get(i);
            executor.execute(new TargetVerificationTask(target, startGate, endGate, claimedPackages, claimedClasses, conflicts));
        }

        startGate.countDown();

        try {
            boolean completed = endGate.await(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!completed) {
                conflicts.add("Concurrent test timed out after " + timeoutMillis + " ms");
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            conflicts.add("Concurrent test thread interrupted: " + interruptedException.getMessage());
        } finally {
            executor.shutdownNow();
        }

        long durationNanos = System.nanoTime() - startNanos;
        boolean success = conflicts.isEmpty();

        if (strictMode && !success) {
            throw new ClasspathCollisionException("Classpath collision detected during multi-loader execution: " + conflicts);
        }

        return new MultiLoaderTestResult(success, targetCount, conflicts.size(), conflicts, durationNanos);
    }
}
