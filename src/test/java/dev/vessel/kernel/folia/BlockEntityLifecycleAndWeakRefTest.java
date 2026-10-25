package dev.vessel.kernel.folia;

import dev.vessel.kernel.folia.blockentity.BlockEntityLifecycleSentinel;
import dev.vessel.kernel.folia.blockentity.WeakReferenceCapabilityBridge;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BlockEntityLifecycleSentinel} and {@link WeakReferenceCapabilityBridge}.
 * <p>
 * Validates ghost-ticking suppression on chunk unloads and GC-safe capability bridging.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class BlockEntityLifecycleAndWeakRefTest {

    @Test
    public void testBlockEntityLifecycleAndGhostTickPrevention() {
        BlockEntityLifecycleSentinel sentinel = new BlockEntityLifecycleSentinel();
        TestBlockEntityLifecycleCallback callback = new TestBlockEntityLifecycleCallback();

        long chunkKey = 12345L;
        boolean registered = sentinel.registerBlockEntity(
                chunkKey, 10, 64, 10, "vessel:fusion_reactor", callback
        );
        assertThat(registered).isTrue();
        assertThat(sentinel.isChunkActive(chunkKey)).isTrue();
        assertThat(sentinel.registeredCount(chunkKey)).isEqualTo(1);

        // Active ticking is allowed
        assertThat(sentinel.isTickingAllowed(chunkKey, 10, 64, 10)).isTrue();

        // Chunk unloads
        int detached = sentinel.onChunkUnload(chunkKey);
        assertThat(detached).isEqualTo(1);
        assertThat(callback.detachCount()).isEqualTo(1);
        assertThat(callback.isChunkUnloaded()).isTrue();
        assertThat(sentinel.isChunkActive(chunkKey)).isFalse();

        // Subsequent ticking is blocked (ghost tick suppressed)
        assertThat(sentinel.isTickingAllowed(chunkKey, 10, 64, 10)).isFalse();
        assertThat(sentinel.ghostTicksPreventedCount()).isEqualTo(1L);
    }

    @Test
    public void testWeakReferenceCapabilityResolutionAndUnload() {
        WeakReferenceCapabilityBridge bridge = new WeakReferenceCapabilityBridge();

        long chunkKey = 999L;
        String capabilityId = "forge:energy";
        String dummyEnergyStorage = "EnergyStorage_100kFE";

        bridge.registerCapability(chunkKey, 5, 60, 5, capabilityId, dummyEnergyStorage);

        assertThat(bridge.hasCapability(chunkKey, 5, 60, 5, capabilityId)).isTrue();

        Optional<String> resolved = bridge.getCapability(chunkKey, 5, 60, 5, capabilityId, String.class);
        assertThat(resolved).isPresent().contains("EnergyStorage_100kFE");

        // Chunk unload cleans up capabilities immediately
        int removed = bridge.onChunkUnload(chunkKey);
        assertThat(removed).isEqualTo(1);
        assertThat(bridge.hasCapability(chunkKey, 5, 60, 5, capabilityId)).isFalse();
    }
}
