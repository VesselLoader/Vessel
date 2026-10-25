package dev.vessel.adapter.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Sub-Stage 6.8: {@link UnifiedDataAttachmentBridge},
 * {@link TransactionalInventoryTransferCoordinator}, {@link ChunkLoaderGovernor},
 * {@link BlockUpdateRecursionGuard}, {@link P2PLoopRecursionGuard},
 * {@link ToolModifierEventAggregator}, and {@link DrawerInventoryIndexAccelerator}.
 *
 * @version 1.0.0
 */
class HybridDataSyncAndRecursionGuardTest {

    @Test
    void testUnifiedDataAttachmentBridgeAndTwoPhaseInventoryTransfer() {
        UnifiedDataAttachmentBridge dataBridge = new UnifiedDataAttachmentBridge();
        UUID holderId = UUID.randomUUID();
        dataBridge.putSynchronizedData(holderId, "ars_nouveau:max_mana", "1500");

        assertEquals("1500", dataBridge.getFromBukkitPdc(holderId, "ars_nouveau:max_mana"));
        assertEquals("1500", dataBridge.getFromNeoForgeAttachment(holderId, "ars_nouveau:max_mana"));
        assertEquals("1500", dataBridge.getFromFabricCardinalComponent(holderId, "ars_nouveau:max_mana"));

        TransactionalInventoryTransferCoordinator transferCoordinator =
                new TransactionalInventoryTransferCoordinator();
        UUID seller = UUID.randomUUID();
        UUID buyer = UUID.randomUUID();
        assertTrue(transferCoordinator.executeTwoPhaseItemTransfer(
                seller, buyer, "minecraft:diamond_block", 16, true, true));
        assertFalse(transferCoordinator.executeTwoPhaseItemTransfer(
                seller, buyer, "minecraft:nether_star", 1, true, false));
        assertEquals(1, transferCoordinator.getAbortedTransferCount());
    }

    @Test
    void testChunkLoaderGovernorQuotasOfflineSleepAndLoginWake() {
        ChunkLoaderGovernor governor = new ChunkLoaderGovernor();
        UUID playerUuid = UUID.randomUUID();

        assertTrue(governor.requestForcedChunkTicket(playerUuid, 1001L, 2));
        assertTrue(governor.requestForcedChunkTicket(playerUuid, 1002L, 2));
        assertFalse(governor.requestForcedChunkTicket(playerUuid, 1003L, 2));

        assertTrue(governor.isChunkActivelyForced(1001L));
        assertEquals(0, governor.suspendChunksForOfflinePlayer(playerUuid, 10L, 30L));
        assertEquals(2, governor.suspendChunksForOfflinePlayer(playerUuid, 45L, 30L));
        assertFalse(governor.isChunkActivelyForced(1001L));

        assertEquals(2, governor.wakeChunksOnPlayerLogin(playerUuid));
        assertTrue(governor.isChunkActivelyForced(1002L));
    }

    @Test
    void testRecursionGuardsToolModifierAggregatorAndDrawerIndexO1() {
        BlockUpdateRecursionGuard blockGuard = new BlockUpdateRecursionGuard();
        assertTrue(blockGuard.enterBlockUpdate(500L, 2));
        assertTrue(blockGuard.enterBlockUpdate(501L, 2));
        assertFalse(blockGuard.enterBlockUpdate(502L, 2));
        assertEquals(2, blockGuard.getCurrentDepth());
        blockGuard.exitBlockUpdate(501L);
        blockGuard.exitBlockUpdate(500L);
        assertEquals(0, blockGuard.getCurrentDepth());

        P2PLoopRecursionGuard p2pGuard = new P2PLoopRecursionGuard();
        assertTrue(p2pGuard.tryTraverseTunnel(1L, 2L));
        assertTrue(p2pGuard.tryTraverseTunnel(2L, 3L));
        assertFalse(p2pGuard.tryTraverseTunnel(3L, 1L));
        p2pGuard.resetTickTraversalCycle();
        assertTrue(p2pGuard.tryTraverseTunnel(3L, 1L));

        ToolModifierEventAggregator toolAggregator = new ToolModifierEventAggregator();
        toolAggregator.queueModifierDelta(99L, 4.5f, 1);
        toolAggregator.queueModifierDelta(99L, 2.0f, 2);
        assertEquals(6.5f, toolAggregator.flushAggregatedDamageBonus(99L), 0.001f);
        assertEquals(3, toolAggregator.flushAggregatedDurabilityCost(99L));
        assertEquals(0.0f, toolAggregator.flushAggregatedDamageBonus(99L), 0.001f);

        DrawerInventoryIndexAccelerator drawerIndex = new DrawerInventoryIndexAccelerator();
        drawerIndex.bindItemToDrawerSlot(7000L, "minecraft:iron_ingot", 14);
        assertEquals(14, drawerIndex.lookupDrawerSlotO1(7000L, "minecraft:iron_ingot"));
        drawerIndex.invalidateControllerIndex(7000L);
        assertEquals(-1, drawerIndex.lookupDrawerSlotO1(7000L, "minecraft:iron_ingot"));
    }
}
