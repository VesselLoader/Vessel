package dev.vessel.kernel.optimization.crosschunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.kernel.actor.RegionCoordinateHash;
import dev.vessel.kernel.actor.RegionalActorStrand;
import dev.vessel.kernel.actor.primitive.RegionStrandRouter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Sub-Stage 6.9: all 10 cross-chunk and pipe optimization engines in
 * {@code dev.vessel.kernel.optimization.crosschunk}.
 *
 * @version 1.0.0
 */
class CrossChunkAndPipeOptimizationTest {

    private static long packBlockPos(int x, int y, int z) {
        return (((long) (x & 0x3FFFFFF)) << 38)
                | (((long) (z & 0x3FFFFFF)) << 12)
                | (y & 0xFFFL);
    }

    @Test
    void testProjectileGovernorTrainSplineCoalescerAndRedstoneGatekeeper() {
        CrossChunkProjectileTrajectoryGovernor projectileGov =
                new CrossChunkProjectileTrajectoryGovernor();
        assertTrue(projectileGov.shouldAllowProjectileStepIntoChunk(1L, 4, 4, true, false));
        assertFalse(projectileGov.shouldAllowProjectileStepIntoChunk(2L, 10, 10, false, false));
        assertFalse(projectileGov.shouldAllowProjectileStepIntoChunk(3L, 20, 20, false, true));

        double[] ballisticPos = projectileGov.computeVirtualBallisticPosition(
                0.0, 100.0, 0.0, 10.0, 5.0, 0.0, 1.0, 4);
        assertEquals(40.0, ballisticPos[0], 0.001);
        assertEquals(112.0, ballisticPos[1], 0.001);

        RegionStrandRouter router = new RegionStrandRouter();
        RegionalActorStrand strand = new RegionalActorStrand(RegionCoordinateHash.fromChunk(32, 32));
        router.register(strand);
        assertTrue(projectileGov.handoffProjectileAcrossFoliaRegions(3L, 0, 0, 32, 32, router));

        TrainAndContraptionChunkTicketCoalescer trainCoalescer =
                new TrainAndContraptionChunkTicketCoalescer();
        assertEquals(25.0, trainCoalescer.advanceUnloadedTrainOnVirtualSpline(50L, 95.0, 30.0, 100.0), 0.001);
        assertTrue(trainCoalescer.shouldMaterializeTrainEntity(100.0, 100.0, 120.0, 120.0, 64.0));
        assertFalse(trainCoalescer.shouldMaterializeTrainEntity(100.0, 100.0, 500.0, 500.0, 64.0));

        trainCoalescer.recordTrainChunkTicket(900L, 1000L);
        assertTrue(trainCoalescer.releaseExpiredTicketsWithHysteresis(1100L, 300L).isEmpty());
        assertEquals(List.of(900L), trainCoalescer.releaseExpiredTicketsWithHysteresis(1305L, 300L));

        CrossChunkRedstoneAndCableGatekeeper gatekeeper =
                new CrossChunkRedstoneAndCableGatekeeper();
        assertFalse(gatekeeper.interceptBorderSignal(1L, 2L, 5, 15, false));
        assertFalse(gatekeeper.interceptBorderSignal(1L, 2L, 12, 15, false));
        assertTrue(gatekeeper.getLatchedBorderMask(2L) != 0L);
        assertEquals(2, gatekeeper.flushLatchedSignalsOnChunkLoad(2L));
        assertEquals(0L, gatekeeper.getLatchedBorderMask(2L));
    }

    @Test
    void testAsyncLocateCapabilityBackoffRecipeIndexPoiAndFramedFlyweight() throws Exception {
        AsyncStructureLocateAndJigsawGovernor locateGov =
                new AsyncStructureLocateAndJigsawGovernor();
        long[] coords = locateGov.locateStructureAsync("dungeons_arise:foundry", 10, 20, 32).get();
        assertEquals(2, coords.length);
        assertTrue(locateGov.isJigsawPieceWithinSafeBounds(12, 22, 10, 20, 5, 16));
        assertFalse(locateGov.isJigsawPieceWithinSafeBounds(12, 22, 10, 20, 20, 16));

        CapabilityCacheAndPipeBackoffGovernor pipeGov =
                new CapabilityCacheAndPipeBackoffGovernor();
        Object dummyHandler = "IItemHandler#01";
        pipeGov.cacheBlockCapability(400L, 2, dummyHandler, 10);
        assertEquals(dummyHandler, pipeGov.getCachedCapabilityIfValid(400L, 2, 10));
        assertNull(pipeGov.getCachedCapabilityIfValid(400L, 2, 11));

        pipeGov.markDestinationContainerFull(500L, 77L);
        assertTrue(pipeGov.shouldPipeSkipFullDestination(500L, 77L));
        assertFalse(pipeGov.shouldPipeSkipFullDestination(500L, 78L));

        IndexedRecipeLookupAccelerator recipeIndex = new IndexedRecipeLookupAccelerator();
        recipeIndex.indexRecipe("create:crushing/iron_ore", "create:crushing", 42, 0b101L);
        assertEquals(
                "create:crushing/iron_ore",
                recipeIndex.lookupRecipeFast(888L, "create:crushing", 42, 0b101L));
        assertNull(recipeIndex.lookupRecipeFast(888L, "create:crushing", 99, 0L));
        assertTrue(recipeIndex.isKnownNegativeRecipeInput("create:crushing", 99, 0L));
        assertTrue(recipeIndex.compressRecipeBookSyncPayload(50000, true)
                < recipeIndex.compressRecipeBookSyncPayload(50000, false));

        PoiSpatialIndexAndVillagerLobotomizer poiEngine =
                new PoiSpatialIndexAndVillagerLobotomizer();
        long poiPos = packBlockPos(10, 64, 10);
        poiEngine.registerPoiNode(poiPos, 3, 1);
        assertEquals(poiPos, poiEngine.findNearestAvailablePoiO1(12, 64, 12, 3, 16));
        assertTrue(poiEngine.shouldLobotomizeTrappedVillager(true, true, true, true, 1205L, 1200));
        assertFalse(poiEngine.shouldLobotomizeTrappedVillager(true, true, true, true, 1200L, 1200));

        FramedBlockEntityFlyweightCompressor flyweight =
                new FramedBlockEntityFlyweightCompressor();
        int id1 = flyweight.internCamoBlockEntityNbt("minecraft:oak_planks[waterlogged=false]");
        int id2 = flyweight.internCamoBlockEntityNbt("minecraft:oak_planks[waterlogged=false]");
        assertEquals(id1, id2);
        assertNotNull(flyweight.resolveFlyweightCamoState(id1));
        flyweight.bindBlockToTagBitmask(300, 7);
        assertTrue(flyweight.hasBlockTagFastO1(300, 7));
        assertFalse(flyweight.hasBlockTagFastO1(300, 8));
    }

    @Test
    void testFluidFlowFallingColumnCollapseAndTreeCanopyDecayScheduler() {
        FluidFlowAndFallingBlockPhysicsGovernor fluidGov =
                new FluidFlowAndFallingBlockPhysicsGovernor();
        fluidGov.cacheSlopeFlowDirection(100L, 15, (byte) 0b0101);
        assertEquals((byte) 0b0101, fluidGov.lookupCachedFlowDirection(100L, 15));
        assertTrue(fluidGov.shouldCollapseColumnInstantly(100L, 4, 4, 40));

        int[] columnStates = new int[16];
        columnStates[5] = 12;
        columnStates[6] = 12;
        columnStates[7] = 12;
        int shifted = fluidGov.collapseFallingColumnDirectly(columnStates, 5, 0, 3);
        assertEquals(3, shifted);
        assertEquals(12, columnStates[0]);
        assertEquals(12, columnStates[2]);
        assertEquals(0, columnStates[5]);
        assertEquals(2, fluidGov.coalesceFluidPumpUpdates(100L, new long[]{10L, 20L, 10L}));

        TreeCanopyDecayAndVeinBreakScheduler canopyScheduler =
                new TreeCanopyDecayAndVeinBreakScheduler();
        long trunk = packBlockPos(0, 64, 0);
        long nearLeaf = packBlockPos(2, 65, 1);
        long farLeaf = packBlockPos(10, 70, 10);
        long[] unsupported = canopyScheduler.computeLeafDistanceBitmaskBfs(
                new long[]{trunk}, new long[]{nearLeaf, farLeaf}, 7);
        assertEquals(1, unsupported.length);
        assertEquals(farLeaf, unsupported[0]);

        assertEquals(3, canopyScheduler.enqueueStaggeredCanopyDecay(
                55L, new long[]{1L, 2L, 3L}, 2));
        assertEquals(2, canopyScheduler.pollNextDecayBatch(55L, 1).length);
        assertEquals(1, canopyScheduler.pollNextDecayBatch(55L, 2).length);

        Map<String, Integer> drops = canopyScheduler.coalesceCanopyLootDrops(
                trunk,
                new String[]{"minecraft:oak_sapling", "minecraft:apple", "minecraft:oak_sapling"},
                new int[]{2, 1, 3});
        assertEquals(5, drops.get("minecraft:oak_sapling"));
        assertEquals(1, drops.get("minecraft:apple"));
    }
}
