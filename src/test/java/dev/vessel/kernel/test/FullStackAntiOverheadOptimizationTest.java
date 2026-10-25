package dev.vessel.kernel.test;

import dev.vessel.kernel.bootstrap.BytecodeSymbolScannerVisitor;
import dev.vessel.kernel.bootstrap.ScannedClassSymbols;
import dev.vessel.kernel.bootstrap.ZeroCopyBytecodeSymbolScanner;
import dev.vessel.kernel.datapack.OptimizedLootTableRule;
import dev.vessel.kernel.datapack.OptimizedRecipeCandidate;
import dev.vessel.kernel.datapack.RuntimeRecipeLootTableOptimizer;
import dev.vessel.kernel.display.model.AnimatedModelLODManager;
import dev.vessel.kernel.display.model.ModelLODProfile;
import dev.vessel.kernel.network.ParticleBroadcastThrottle;
import dev.vessel.kernel.network.ThrottledParticleEffect;
import dev.vessel.kernel.network.bedrock.BedrockRakNetDirectChannelEncoder;
import dev.vessel.kernel.network.bedrock.BedrockRakNetFrameHeader;
import dev.vessel.kernel.optimization.block.BreadthFirstFluidSimulator;
import dev.vessel.kernel.optimization.block.FastBlockTickPriorityQueue;
import dev.vessel.kernel.optimization.block.FluidFlowAction;
import dev.vessel.kernel.optimization.block.FluidSimulationType;
import dev.vessel.kernel.optimization.chunk.ChunkTicketAdaptiveThrottle;
import dev.vessel.kernel.optimization.chunk.ChunkTicketEntry;
import dev.vessel.kernel.optimization.chunk.ChunkTicketPriority;
import dev.vessel.kernel.optimization.chunk.LinearRegionFileFastPath;
import dev.vessel.kernel.optimization.chunk.RegionChunkLocation;
import dev.vessel.kernel.optimization.chunk.RegionCompressionFormat;
import dev.vessel.kernel.optimization.entity.EntityActivationRangeLOD;
import dev.vessel.kernel.optimization.entity.EntityActivationTier;
import dev.vessel.kernel.optimization.leak.LeakSanitizeResult;
import dev.vessel.kernel.optimization.leak.NativeMemoryPoolRecycler;
import dev.vessel.kernel.optimization.leak.PooledBufferStatistics;
import dev.vessel.kernel.optimization.leak.ThreadLocalLeakSanitizer;
import dev.vessel.kernel.optimization.lighting.ChunkSectionLightData;
import dev.vessel.kernel.optimization.lighting.LightPropagationType;
import dev.vessel.kernel.optimization.lighting.StarlightZeroCopyLightEngine;
import dev.vessel.kernel.optimization.physics.FastRayTraceCollisionPath;
import dev.vessel.kernel.optimization.physics.FastRayTraceHit;
import dev.vessel.kernel.vfs.ArchiveMountLayer;
import dev.vessel.kernel.vfs.LayeredMemoryMappedVfsMount;
import dev.vessel.kernel.vfs.VfsFileResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive verification test suite for Full-Stack Zero-Lag Anti-Overhead Engine
 * across all 6 optimization domains (Pass K-21).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class FullStackAntiOverheadOptimizationTest {

    @Test
    @DisplayName("Group 1: ZeroCopyBytecodeSymbolScanner extracts symbols and annotations with zero classloading")
    void testZeroCopyBytecodeSymbolScanner() {
        // Synthesize a dummy class bytecode with ASM
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/example/TestModule", null, "java/lang/Object",
                new String[]{"java/lang/Runnable"});
        cw.visitAnnotation("Lnet/neoforged/fml/common/Mod;", true).visitEnd();
        cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null).visitEnd();
        cw.visitEnd();
        byte[] bytecode = cw.toByteArray();

        ZeroCopyBytecodeSymbolScanner scanner = new ZeroCopyBytecodeSymbolScanner();
        ScannedClassSymbols symbols = scanner.scan(bytecode);

        assertThat(symbols.className()).isEqualTo("com/example/TestModule");
        assertThat(symbols.superName()).isEqualTo("java/lang/Object");
        assertThat(symbols.interfaces()).contains("java/lang/Runnable");
        assertThat(symbols.hasAnnotation("Lnet/neoforged/fml/common/Mod;")).isTrue();
        assertThat(symbols.hasAnnotationMatching("Mod")).isTrue();
        assertThat(symbols.entrypoints()).contains("com/example/TestModule#main");
        assertThat(scanner.getScannedClassesCount()).isEqualTo(1);
        assertThat(scanner.getTotalScanDurationNanos()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Group 1: LayeredMemoryMappedVfsMount priority resolution & zero-copy resource retrieval")
    void testLayeredMemoryMappedVfsMount() throws IOException {
        // Create Layer 1 (priority 10)
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        try (ZipOutputStream zos1 = new ZipOutputStream(baos1)) {
            zos1.putNextEntry(new ZipEntry("assets/mod/base.txt"));
            zos1.write("Layer 1 Base".getBytes(StandardCharsets.UTF_8));
            zos1.closeEntry();
            zos1.putNextEntry(new ZipEntry("assets/mod/override.txt"));
            zos1.write("Layer 1 Override".getBytes(StandardCharsets.UTF_8));
            zos1.closeEntry();
        }
        ArchiveMountLayer layer1 = new ArchiveMountLayer("mod:base", 10, baos1.toByteArray());

        // Create Layer 2 (priority 50, higher priority)
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        try (ZipOutputStream zos2 = new ZipOutputStream(baos2)) {
            zos2.putNextEntry(new ZipEntry("assets/mod/override.txt"));
            zos2.write("Layer 2 Wins".getBytes(StandardCharsets.UTF_8));
            zos2.closeEntry();
        }
        ArchiveMountLayer layer2 = new ArchiveMountLayer("mod:addon", 50, baos2.toByteArray());

        try (LayeredMemoryMappedVfsMount vfs = new LayeredMemoryMappedVfsMount("/mods")) {
            vfs.addLayer(layer1);
            vfs.addLayer(layer2);

            assertThat(vfs.layerCount()).isEqualTo(2);
            assertThat(vfs.hasResource("assets/mod/base.txt")).isTrue();
            assertThat(vfs.hasResource("assets/mod/override.txt")).isTrue();
            assertThat(vfs.hasResource("assets/mod/nonexistent.txt")).isFalse();

            // Layer 2 should override Layer 1 for override.txt
            Optional<VfsFileResource> overrideRes = vfs.findResource("assets/mod/override.txt");
            assertThat(overrideRes).isPresent();
            assertThat(overrideRes.get().sourceLayerIdentifier()).isEqualTo("mod:addon");
            assertThat(new String(overrideRes.get().toByteArray(), StandardCharsets.UTF_8)).isEqualTo("Layer 2 Wins");

            // Base resource from Layer 1
            Optional<byte[]> baseBytes = vfs.readResource("assets/mod/base.txt");
            assertThat(baseBytes).isPresent();
            assertThat(new String(baseBytes.get(), StandardCharsets.UTF_8)).isEqualTo("Layer 1 Base");

            // Listing
            assertThat(vfs.listResources("assets/mod")).contains("assets/mod/base.txt", "assets/mod/override.txt");
        }
    }

    @Test
    @DisplayName("Group 1: RuntimeRecipeLootTableOptimizer single-cycle 64-bit bitmask lookups")
    void testRuntimeRecipeLootTableOptimizer() {
        RuntimeRecipeLootTableOptimizer optimizer = new RuntimeRecipeLootTableOptimizer();

        long maskIron = optimizer.getOrAssignItemMask("minecraft:iron_ingot");
        long maskStick = optimizer.getOrAssignItemMask("minecraft:stick");
        long maskDiamond = optimizer.getOrAssignItemMask("minecraft:diamond");

        // Sword recipe: 2 iron + 1 stick -> iron_sword
        long swordIngredientMask = maskIron | maskStick;
        optimizer.registerRecipe(new OptimizedRecipeCandidate("minecraft:iron_sword", "minecraft:iron_sword", 1, swordIngredientMask, true));

        // Pickaxe recipe: 3 diamond + 2 stick -> diamond_pickaxe
        long pickaxeIngredientMask = maskDiamond | maskStick;
        optimizer.registerRecipe(new OptimizedRecipeCandidate("minecraft:diamond_pickaxe", "minecraft:diamond_pickaxe", 1, pickaxeIngredientMask, true));

        // Crafting grid has iron and stick
        long playerGridMask = maskIron | maskStick;
        List<OptimizedRecipeCandidate> matches = optimizer.findMatchingRecipes(playerGridMask);
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).outputItemId()).isEqualTo("minecraft:iron_sword");

        // Loot table test
        long zombieLootCondition = RuntimeRecipeLootTableOptimizer.CONDITION_KILLED_BY_PLAYER;
        optimizer.registerLootRule(new OptimizedLootTableRule("zombie_flesh", "minecraft:entities/zombie", 0L, "minecraft:rotten_flesh", 1, 2, 1.0f));
        optimizer.registerLootRule(new OptimizedLootTableRule("zombie_iron", "minecraft:entities/zombie", zombieLootCondition, "minecraft:iron_ingot", 0, 1, 0.025f));

        // Mob died by environmental damage (not player)
        List<OptimizedLootTableRule> dropsNatural = optimizer.evaluateLootTable("minecraft:entities/zombie", 0L);
        assertThat(dropsNatural).hasSize(1);
        assertThat(dropsNatural.get(0).dropItemId()).isEqualTo("minecraft:rotten_flesh");

        // Mob killed by player
        List<OptimizedLootTableRule> dropsPlayer = optimizer.evaluateLootTable("minecraft:entities/zombie", RuntimeRecipeLootTableOptimizer.CONDITION_KILLED_BY_PLAYER);
        assertThat(dropsPlayer).hasSize(2);
    }

    @Test
    @DisplayName("Group 2: LinearRegionFileFastPath direct buffer compression & sector streaming")
    void testLinearRegionFileFastPath() throws IOException {
        try (LinearRegionFileFastPath region = new LinearRegionFileFastPath(0, 0, 16384)) {
            byte[] chunkData = "VESSEL Chunk 1.21.1 NBT Payload Test".repeat(50).getBytes(StandardCharsets.UTF_8);

            // Write chunk at (3, 7) with DEFLATE
            region.writeChunk(3, 7, chunkData, RegionCompressionFormat.DEFLATE);

            assertThat(region.hasChunk(3, 7)).isTrue();
            assertThat(region.hasChunk(0, 0)).isFalse();

            RegionChunkLocation loc = region.getChunkLocation(3, 7);
            assertThat(loc.isEmpty()).isFalse();
            assertThat(loc.sectorOffset()).isGreaterThanOrEqualTo(2);
            assertThat(loc.sectorCount()).isGreaterThan(0);

            // Read back and compare
            byte[] readBack = region.readChunk(3, 7);
            assertThat(readBack).isEqualTo(chunkData);

            // Write chunk with GZIP at (15, 20)
            byte[] chunkDataGzip = "Another Chunk Payload".getBytes(StandardCharsets.UTF_8);
            region.writeChunk(15, 20, chunkDataGzip, RegionCompressionFormat.GZIP);
            assertThat(region.readChunk(15, 20)).isEqualTo(chunkDataGzip);
        }
    }

    @Test
    @DisplayName("Group 2: ChunkTicketAdaptiveThrottle tier prioritization & instant eviction sweep")
    void testChunkTicketAdaptiveThrottle() {
        ChunkTicketAdaptiveThrottle throttle = new ChunkTicketAdaptiveThrottle();

        long startTick = 100L;
        // Chunk (5, 5): Player ticket for 20 ticks
        throttle.addTicket(5, 5, ChunkTicketPriority.PLAYER, 20, "player_uuid", startTick);
        // Chunk (5, 5): Entity ticket for 5 ticks
        throttle.addTicket(5, 5, ChunkTicketPriority.ENTITY, 5, "entity_123", startTick);
        // Chunk (10, 10): WorldGen ticket for 10 ticks
        throttle.addTicket(10, 10, ChunkTicketPriority.WORLD_GEN, 10, "generator", startTick);

        assertThat(throttle.getActiveChunkCount()).isEqualTo(2);
        assertThat(throttle.getTotalTicketsCount()).isEqualTo(3);
        assertThat(throttle.getHighestPriority(5, 5)).contains(ChunkTicketPriority.PLAYER);

        // Advance 12 ticks: tick 112 -> WorldGen ticket on (10, 10) expires (100 + 10 = 110)
        List<Long> evicted = throttle.sweepExpiredTickets(112L);
        assertThat(evicted).hasSize(1);
        assertThat(evicted.get(0)).isEqualTo(ChunkTicketEntry.packKey(10, 10));
        assertThat(throttle.isChunkActive(10, 10)).isFalse();
        assertThat(throttle.isChunkActive(5, 5)).isTrue();

        // Advance past 125 ticks: Player ticket expires too
        List<Long> evictedRemaining = throttle.sweepExpiredTickets(125L);
        assertThat(evictedRemaining).contains(ChunkTicketEntry.packKey(5, 5));
        assertThat(throttle.getActiveChunkCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Group 3: StarlightZeroCopyLightEngine non-recursive BFS light propagation & removal")
    void testStarlightZeroCopyLightEngine() {
        StarlightZeroCopyLightEngine engine = new StarlightZeroCopyLightEngine(LightPropagationType.BLOCK);
        ChunkSectionLightData section = new ChunkSectionLightData();

        // Place light source level 15 at center (8, 8, 8)
        engine.propagate(section, 8, 8, 8, 15);

        assertThat(section.get(8, 8, 8)).isEqualTo(15);
        // 1 block away (Manhattan distance 1) -> level 14
        assertThat(section.get(7, 8, 8)).isEqualTo(14);
        assertThat(section.get(9, 8, 8)).isEqualTo(14);
        assertThat(section.get(8, 9, 8)).isEqualTo(14);
        assertThat(section.get(8, 7, 8)).isEqualTo(14);
        // 2 blocks away -> level 13
        assertThat(section.get(6, 8, 8)).isEqualTo(13);

        // Remove light source
        engine.removeLight(section, 8, 8, 8);
        assertThat(section.get(8, 8, 8)).isEqualTo(0);
        assertThat(section.get(7, 8, 8)).isEqualTo(0);
    }

    @Test
    @DisplayName("Group 3: BreadthFirstFluidSimulator free-fall column fast-path and horizontal spread")
    void testBreadthFirstFluidSimulator() {
        BreadthFirstFluidSimulator simulator = new BreadthFirstFluidSimulator(FluidSimulationType.WATER);

        // Space is passable when Y >= 5
        List<FluidFlowAction> actions = simulator.simulateSpread(8, 10, 8, true, (x, y, z) -> y >= 5);

        assertThat(actions).isNotEmpty();
        // Should contain free-fall vertical actions for y=9, y=8, y=7, y=6, y=5
        long verticalCount = actions.stream().filter(FluidFlowAction::isFalling).count();
        assertThat(verticalCount).isGreaterThanOrEqualTo(5);

        // Should contain horizontal flow actions
        long horizontalCount = actions.stream().filter(a -> !a.isFalling()).count();
        assertThat(horizontalCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("Group 3: FastBlockTickPriorityQueue zero-allocation binary min-heap ordering")
    void testFastBlockTickPriorityQueue() {
        FastBlockTickPriorityQueue queue = new FastBlockTickPriorityQueue(64);

        // Enqueue out of order
        queue.enqueue(105L, 10, 60, 10, 1);
        queue.enqueue(101L, 20, 60, 20, 2);
        queue.enqueue(101L, 30, 60, 30, 0); // Earlier priority for same tick 101
        queue.enqueue(110L, 40, 60, 40, 1);

        assertThat(queue.size()).isEqualTo(4);
        assertThat(queue.peekScheduledTick()).isEqualTo(101L);

        List<String> polled = new ArrayList<>();
        // Poll ready ticks up to tick 105
        int count = queue.pollReadyTicks(105L, (tick, x, y, z, priority) -> {
            polled.add("tick=" + tick + ",x=" + x + ",p=" + priority);
        });

        assertThat(count).isEqualTo(3);
        assertThat(polled).containsExactly(
                "tick=101,x=30,p=0", // priority 0 before priority 2
                "tick=101,x=20,p=2",
                "tick=105,x=10,p=1"
        );
        assertThat(queue.size()).isEqualTo(1);
        assertThat(queue.peekScheduledTick()).isEqualTo(110L);
    }

    @Test
    @DisplayName("Group 4: EntityActivationRangeLOD radial euclidean tiers and boss exemption rules")
    void testEntityActivationRangeLOD() {
        EntityActivationRangeLOD lod = new EntityActivationRangeLOD();

        // Register active player at (0, 0, 0)
        lod.updatePlayerPosition(1, 0.0, 64.0, 0.0);

        // Zombie 10m away -> ACTIVE (tick interval 1)
        assertThat(lod.resolveTier("minecraft:zombie", 10.0, 64.0, 0.0)).isEqualTo(EntityActivationTier.ACTIVE);
        assertThat(lod.shouldTickEntity(101, "minecraft:zombie", 10.0, 64.0, 0.0, 100L)).isTrue();
        assertThat(lod.shouldTickEntity(101, "minecraft:zombie", 10.0, 64.0, 0.0, 101L)).isTrue();

        // Zombie 25m away -> NEAR (tick interval 4)
        assertThat(lod.resolveTier("minecraft:zombie", 25.0, 64.0, 0.0)).isEqualTo(EntityActivationTier.NEAR);

        // Zombie 50m away -> FAR (tick interval 16)
        assertThat(lod.resolveTier("minecraft:zombie", 50.0, 64.0, 0.0)).isEqualTo(EntityActivationTier.FAR);

        // Zombie 80m away -> DORMANT (no ticks)
        assertThat(lod.resolveTier("minecraft:zombie", 80.0, 64.0, 0.0)).isEqualTo(EntityActivationTier.DORMANT);
        assertThat(lod.shouldTickEntity(102, "minecraft:zombie", 80.0, 64.0, 0.0, 100L)).isFalse();

        // Register Ender Dragon exemption rule -> always ACTIVE even 100m away
        lod.registerRule(entityType -> "minecraft:ender_dragon".equals(entityType) ? EntityActivationTier.ACTIVE : null);
        assertThat(lod.resolveTier("minecraft:ender_dragon", 100.0, 64.0, 0.0)).isEqualTo(EntityActivationTier.ACTIVE);
    }

    @Test
    @DisplayName("Group 4: FastRayTraceCollisionPath 3D DDA voxel traversal raycasting")
    void testFastRayTraceCollisionPath() {
        FastRayTraceCollisionPath rayTracer = new FastRayTraceCollisionPath();

        // Solid block placed at (5, 0, 0)
        FastRayTraceHit hit = rayTracer.rayTraceLine(
                0.5, 0.5, 0.5,
                10.5, 0.5, 0.5,
                (x, y, z) -> x == 5 && y == 0 && z == 0
        );

        assertThat(hit.isHit()).isTrue();
        assertThat(hit.blockX()).isEqualTo(5);
        assertThat(hit.blockY()).isEqualTo(0);
        assertThat(hit.blockZ()).isEqualTo(0);
        assertThat(hit.hitFace()).isEqualTo(FastRayTraceCollisionPath.FACE_WEST);
        assertThat(hit.distance()).isBetween(4.0, 5.0);

        // Line-of-sight without obstacles
        FastRayTraceHit miss = rayTracer.rayTraceLine(
                0.5, 0.5, 0.5,
                10.5, 0.5, 0.5,
                (x, y, z) -> false
        );
        assertThat(miss.isHit()).isFalse();
    }

    @Test
    @DisplayName("Group 5: NativeMemoryPoolRecycler tiered pooling & cache hit statistics")
    void testNativeMemoryPoolRecycler() {
        NativeMemoryPoolRecycler recycler = new NativeMemoryPoolRecycler();

        // Acquire 4KB buffer (cache miss initial)
        byte[] buf1 = recycler.acquireByteArray(4096);
        assertThat(buf1.length).isEqualTo(4096);

        // Release back to pool
        recycler.releaseByteArray(buf1);
        assertThat(recycler.getActivePooledBuffers()).isEqualTo(1);

        // Re-acquire -> should be cache hit
        byte[] buf2 = recycler.acquireByteArray(4096);
        assertThat(buf2).isSameAs(buf1);

        // Float array tier
        float[] f1 = recycler.acquireFloatArray(1024);
        assertThat(f1.length).isEqualTo(1024);
        recycler.releaseFloatArray(f1);
        float[] f2 = recycler.acquireFloatArray(1024);
        assertThat(f2).isSameAs(f1);

        PooledBufferStatistics stats = recycler.getStatistics();
        assertThat(stats.totalAcquires()).isEqualTo(4L);
        assertThat(stats.totalReleases()).isEqualTo(2L);
        assertThat(stats.cacheHits()).isEqualTo(2L);
        assertThat(stats.hitRate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Group 5: ThreadLocalLeakSanitizer static hooks and thread sweep")
    void testThreadLocalLeakSanitizer() {
        ThreadLocalLeakSanitizer sanitizer = new ThreadLocalLeakSanitizer();
        AtomicBoolean staticCleaned = new AtomicBoolean(false);

        sanitizer.registerStaticCleanup(() -> staticCleaned.set(true));

        LeakSanitizeResult result = sanitizer.sanitize(getClass().getClassLoader());

        assertThat(staticCleaned.get()).isTrue();
        assertThat(result.scannedThreadsCount()).isGreaterThan(0);
        assertThat(result.clearedStaticReferencesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Group 6: AnimatedModelLODManager dynamic bone transform broadcast throttling")
    void testAnimatedModelLODManager() {
        AnimatedModelLODManager lodManager = new AnimatedModelLODManager();

        // < 16m: LOD 0
        ModelLODProfile p0 = lodManager.resolveLOD(10.0 * 10.0);
        assertThat(p0.lodLevel()).isEqualTo(0);
        assertThat(p0.updateIntervalTicks()).isEqualTo(1);
        assertThat(lodManager.shouldBroadcastAnimation(1, 100.0, 10L)).isTrue();

        // 16..32m: LOD 1 (interval 2)
        ModelLODProfile p1 = lodManager.resolveLOD(20.0 * 20.0);
        assertThat(p1.lodLevel()).isEqualTo(1);
        assertThat(p1.updateIntervalTicks()).isEqualTo(2);

        // 32..64m: LOD 2 (interval 4)
        ModelLODProfile p2 = lodManager.resolveLOD(40.0 * 40.0);
        assertThat(p2.lodLevel()).isEqualTo(2);
        assertThat(p2.updateIntervalTicks()).isEqualTo(4);

        // > 64m: LOD 3 (interval 20)
        ModelLODProfile p3 = lodManager.resolveLOD(80.0 * 80.0);
        assertThat(p3.lodLevel()).isEqualTo(3);
        assertThat(p3.updateIntervalTicks()).isEqualTo(20);

        assertThat(lodManager.getInterpolationDurationSeconds(p1)).isEqualTo(0.1f);
    }

    @Test
    @DisplayName("Group 6: ParticleBroadcastThrottle spatial clustering, coalescing & drop ceiling")
    void testParticleBroadcastThrottle() {
        ParticleBroadcastThrottle throttle = new ParticleBroadcastThrottle(50);

        // Add 20 flame particles at (10.0, 64.0, 10.0)
        boolean ok1 = throttle.addParticle("minecraft:flame", 10.0, 64.0, 10.0, 0.1f, 0.1f, 0.1f, 0.05f, 20);
        // Add 20 more flame particles in the same 4x4x4 cluster (10 >> 2 == 11 >> 2 == 2)
        boolean ok2 = throttle.addParticle("minecraft:flame", 11.0, 64.0, 11.0, 0.1f, 0.1f, 0.1f, 0.05f, 20);

        assertThat(ok1).isTrue();
        assertThat(ok2).isTrue();

        // Add 30 more particles in the same cluster (8..11) -> total exceeds limit of 50, so 10 should be accepted and 20 dropped
        throttle.addParticle("minecraft:flame", 10.5, 64.0, 10.5, 0.1f, 0.1f, 0.1f, 0.05f, 30);
        assertThat(throttle.getDroppedParticlesCount()).isEqualTo(20L);

        // Flush batch -> coalesced into 1 ThrottledParticleEffect with count = 50
        List<ThrottledParticleEffect> batch = throttle.flushBatch();
        assertThat(batch).hasSize(1);
        assertThat(batch.get(0).count()).isEqualTo(50);
        assertThat(batch.get(0).particleType()).isEqualTo("minecraft:flame");
    }

    @Test
    @DisplayName("Group 6: BedrockRakNetDirectChannelEncoder direct off-heap frame encoding and decoding")
    void testBedrockRakNetDirectChannelEncoder() {
        BedrockRakNetDirectChannelEncoder encoder = new BedrockRakNetDirectChannelEncoder();

        BedrockRakNetFrameHeader header = new BedrockRakNetFrameHeader(
                (byte) (BedrockRakNetFrameHeader.FLAG_RELIABLE | BedrockRakNetFrameHeader.FLAG_ORDERED),
                12345,
                (byte) 3,
                6789
        );

        byte[] payload = "Bedrock MovePlayerPacket Payload".getBytes(StandardCharsets.UTF_8);
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024);

        int writtenBytes = encoder.encodeFrame(directBuffer, header, payload);
        assertThat(writtenBytes).isGreaterThan(payload.length);

        // Decode back
        directBuffer.flip();
        BedrockRakNetFrameHeader decoded = encoder.decodeFrameHeader(directBuffer);

        assertThat(decoded.sequenceNumber()).isEqualTo(12345);
        assertThat(decoded.orderChannel()).isEqualTo((byte) 3);
        assertThat(decoded.orderIndex()).isEqualTo(6789);
        assertThat(decoded.isReliable()).isTrue();
        assertThat(decoded.isOrdered()).isTrue();
        assertThat(decoded.isSplit()).isFalse();
    }
}
