package dev.vessel.kernel.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test suite for {@link VesselBootstrapCoordinator}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class VesselBootstrapCoordinatorTest {

    @Test
    @DisplayName("Successfully coordinates default microkernel bootstrap sequence to completion")
    void testSuccessfulSequentialBootstrap() {
        VesselBootstrapCoordinator coordinator = new VesselBootstrapCoordinator();

        assertThat(coordinator.isBootstrapped()).isFalse();
        assertThat(coordinator.currentPhase()).isEqualTo(BootstrapPhase.PREFLIGHT);

        coordinator.coordinateBootstrap();

        assertThat(coordinator.isBootstrapped()).isTrue();
        assertThat(coordinator.currentPhase()).isEqualTo(BootstrapPhase.COMPLETED);

        List<BootstrapExecutionResult> history = coordinator.executionHistory();
        assertThat(history).hasSize(5);
        assertThat(history.get(0).phase()).isEqualTo(BootstrapPhase.PREFLIGHT);
        assertThat(history.get(1).phase()).isEqualTo(BootstrapPhase.SCANNER);
        assertThat(history.get(2).phase()).isEqualTo(BootstrapPhase.VFS_MOUNT);
        assertThat(history.get(3).phase()).isEqualTo(BootstrapPhase.NATIVE_GUARD);
        assertThat(history.get(4).phase()).isEqualTo(BootstrapPhase.REGISTRY);

        for (BootstrapExecutionResult result : history) {
            assertThat(result.success()).isTrue();
            assertThat(result.errorMessage()).isEmpty();
        }
    }

    @Test
    @DisplayName("Bootstrap coordinate execution is idempotent")
    void testBootstrapIdempotency() {
        VesselBootstrapCoordinator coordinator = new VesselBootstrapCoordinator();
        coordinator.coordinateBootstrap();
        assertThat(coordinator.isBootstrapped()).isTrue();

        int historySizeAfterFirstRun = coordinator.executionHistory().size();

        // Second invocation should do nothing
        coordinator.coordinateBootstrap();
        assertThat(coordinator.isBootstrapped()).isTrue();
        assertThat(coordinator.executionHistory()).hasSize(historySizeAfterFirstRun);
    }

    @Test
    @DisplayName("Executes custom phase step overrides properly")
    void testCustomStepOverride() {
        VesselBootstrapCoordinator coordinator = new VesselBootstrapCoordinator();
        AtomicInteger customStepCount = new AtomicInteger(0);

        coordinator.registerCustomStep(BootstrapPhase.SCANNER, customStepCount::incrementAndGet);
        coordinator.coordinateBootstrap();

        assertThat(customStepCount.get()).isEqualTo(1);
        assertThat(coordinator.isBootstrapped()).isTrue();
    }

    @Test
    @DisplayName("Transitions to FAILED state and throws VesselBootstrapException on step error")
    void testBootstrapFailureHandling() {
        VesselBootstrapCoordinator coordinator = new VesselBootstrapCoordinator();
        coordinator.registerCustomStep(BootstrapPhase.VFS_MOUNT, () -> {
            throw new RuntimeException("Simulated disk error");
        });

        assertThatThrownBy(coordinator::coordinateBootstrap)
                .isInstanceOf(VesselBootstrapException.class)
                .hasMessageContaining("Bootstrap failed during phase: VFS_MOUNT")
                .matches(e -> ((VesselBootstrapException) e).failedPhase() == BootstrapPhase.VFS_MOUNT);

        assertThat(coordinator.isBootstrapped()).isFalse();
        assertThat(coordinator.currentPhase()).isEqualTo(BootstrapPhase.FAILED);

        List<BootstrapExecutionResult> history = coordinator.executionHistory();
        assertThat(history).isNotEmpty();
        BootstrapExecutionResult lastResult = history.get(history.size() - 1);
        assertThat(lastResult.phase()).isEqualTo(BootstrapPhase.VFS_MOUNT);
        assertThat(lastResult.success()).isFalse();
        assertThat(lastResult.errorMessage()).contains("Simulated disk error");
    }

    @Test
    @DisplayName("Handles concurrent coordinateBootstrap calls safely without race conditions")
    void testConcurrentBootstrapCalls() throws InterruptedException {
        VesselBootstrapCoordinator coordinator = new VesselBootstrapCoordinator();
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successes = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    coordinator.coordinateBootstrap();
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successes.get()).isEqualTo(threadCount);
        assertThat(coordinator.isBootstrapped()).isTrue();
        assertThat(coordinator.executionHistory()).hasSize(5);
    }
}
