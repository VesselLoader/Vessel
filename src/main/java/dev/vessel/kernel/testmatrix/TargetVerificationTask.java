package dev.vessel.kernel.testmatrix;

import dev.vessel.kernel.identity.SourceLoaderKind;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * Task executing isolated namespace verification for a single {@link MultiLoaderTestTarget} in concurrent harness testing.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TargetVerificationTask implements Runnable {

    private final MultiLoaderTestTarget target;
    private final CountDownLatch startGate;
    private final CountDownLatch endGate;
    private final Map<String, SourceLoaderKind> claimedPackages;
    private final Map<String, SourceLoaderKind> claimedClasses;
    private final List<String> conflicts;

    public TargetVerificationTask(
            MultiLoaderTestTarget target,
            CountDownLatch startGate,
            CountDownLatch endGate,
            Map<String, SourceLoaderKind> claimedPackages,
            Map<String, SourceLoaderKind> claimedClasses,
            List<String> conflicts
    ) {
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.startGate = Objects.requireNonNull(startGate, "startGate must not be null");
        this.endGate = Objects.requireNonNull(endGate, "endGate must not be null");
        this.claimedPackages = Objects.requireNonNull(claimedPackages, "claimedPackages must not be null");
        this.claimedClasses = Objects.requireNonNull(claimedClasses, "claimedClasses must not be null");
        this.conflicts = Objects.requireNonNull(conflicts, "conflicts must not be null");
    }

    @Override
    public void run() {
        try {
            startGate.await();

            for (String pkg : target.exportedPackages()) {
                SourceLoaderKind existing = claimedPackages.putIfAbsent(pkg, target.loaderKind());
                if (existing != null && existing != target.loaderKind()) {
                    conflicts.add(MultiLoaderConcurrentTestHarness.CONFLICT_PACKAGE_MSG + pkg
                            + " between " + existing + " and " + target.loaderKind());
                }
            }

            for (String clazz : target.entrypointClasses()) {
                SourceLoaderKind existing = claimedClasses.putIfAbsent(clazz, target.loaderKind());
                if (existing != null && existing != target.loaderKind()) {
                    conflicts.add(MultiLoaderConcurrentTestHarness.CONFLICT_CLASS_MSG + clazz
                            + " between " + existing + " and " + target.loaderKind());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            conflicts.add("Task interrupted for target " + target.modId());
        } finally {
            endGate.countDown();
        }
    }
}
