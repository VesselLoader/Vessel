package dev.vessel.kernel.test;

import dev.vessel.kernel.optimization.leak.LifecycleDisposalRegistry;
import dev.vessel.kernel.optimization.leak.LifecycleDisposalScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit verification suite for Memory Leak Fix-inspired lifecycle disposal registry.
 *
 * @version 1.0.1
 */
public class LifecycleDisposalTest {

    @Test
    @DisplayName("LifecycleDisposalRegistry deterministically disposes player scoped resources upon disconnect")
    void testPlayerDisconnectDisposal() {
        LifecycleDisposalRegistry registry = new LifecycleDisposalRegistry();

        AtomicBoolean player1Cleaned = new AtomicBoolean(false);
        AtomicBoolean player2Cleaned = new AtomicBoolean(false);

        String player1Uuid = "uuid-player-1";
        String player2Uuid = "uuid-player-2";

        registry.registerAction(LifecycleDisposalScope.PLAYER_DISCONNECT, player1Uuid, () -> player1Cleaned.set(true));
        registry.registerAction(LifecycleDisposalScope.PLAYER_DISCONNECT, player2Uuid, () -> player2Cleaned.set(true));

        assertThat(registry.getRegisteredCount()).isEqualTo(2L);
        assertThat(registry.getActiveCount()).isEqualTo(2);

        // Disconnect player 1
        int disposed = registry.dispose(LifecycleDisposalScope.PLAYER_DISCONNECT, player1Uuid);
        assertThat(disposed).isEqualTo(1);
        assertThat(player1Cleaned.get()).isTrue();
        assertThat(player2Cleaned.get()).isFalse();
        assertThat(registry.getActiveCount()).isEqualTo(1);
        assertThat(registry.getDisposedCount()).isEqualTo(1L);

        // Disconnect player 2
        registry.dispose(LifecycleDisposalScope.PLAYER_DISCONNECT, player2Uuid);
        assertThat(player2Cleaned.get()).isTrue();
        assertThat(registry.getActiveCount()).isEqualTo(0);
        assertThat(registry.getDisposedCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("LifecycleDisposalRegistry disposes all resources under a scope or all scopes")
    void testScopeBulkDisposal() {
        LifecycleDisposalRegistry registry = new LifecycleDisposalRegistry();

        AtomicBoolean chunk1Disposed = new AtomicBoolean(false);
        AtomicBoolean chunk2Disposed = new AtomicBoolean(false);
        AtomicBoolean worldDisposed = new AtomicBoolean(false);

        registry.registerAction(LifecycleDisposalScope.CHUNK_UNLOAD, "chunk_0_0", () -> chunk1Disposed.set(true));
        registry.registerAction(LifecycleDisposalScope.CHUNK_UNLOAD, "chunk_0_1", () -> chunk2Disposed.set(true));
        registry.registerAction(LifecycleDisposalScope.WORLD_UNLOAD, "overworld", () -> worldDisposed.set(true));

        assertThat(registry.getActiveCount()).isEqualTo(3);

        // Dispose all chunks
        int chunkCleaned = registry.disposeAll(LifecycleDisposalScope.CHUNK_UNLOAD);
        assertThat(chunkCleaned).isEqualTo(2);
        assertThat(chunk1Disposed.get()).isTrue();
        assertThat(chunk2Disposed.get()).isTrue();
        assertThat(worldDisposed.get()).isFalse();
        assertThat(registry.getActiveCount()).isEqualTo(1);

        // Dispose everything remaining
        int allCleaned = registry.disposeAll();
        assertThat(allCleaned).isEqualTo(1);
        assertThat(worldDisposed.get()).isTrue();
        assertThat(registry.getActiveCount()).isEqualTo(0);
    }
}
