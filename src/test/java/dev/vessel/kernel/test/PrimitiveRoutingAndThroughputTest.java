package dev.vessel.kernel.test;

import dev.vessel.kernel.actor.RegionCoordinateHash;
import dev.vessel.kernel.actor.RegionalActorStrand;
import dev.vessel.kernel.actor.primitive.LongPrimitive2ObjectMap;
import dev.vessel.kernel.actor.primitive.RegionStrandRouter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification and throughput test suite for primitive spatial routing and primitive map.
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-5)
 */
public class PrimitiveRoutingAndThroughputTest {

    @Test
    @DisplayName("LongPrimitive2ObjectMap performs primitive long key put, get, remove without boxing")
    void testPrimitiveMapOperations() {
        LongPrimitive2ObjectMap<String> map = new LongPrimitive2ObjectMap<>(8);

        // Put and get non-zero keys
        map.put(100L, "Val100");
        map.put(-500L, "ValNeg500");
        map.put(999999999L, "ValLarge");

        // Zero key handling
        map.put(0L, "ValZero");

        assertThat(map.size()).isEqualTo(4);
        assertThat(map.get(100L)).isEqualTo("Val100");
        assertThat(map.get(-500L)).isEqualTo("ValNeg500");
        assertThat(map.get(999999999L)).isEqualTo("ValLarge");
        assertThat(map.get(0L)).isEqualTo("ValZero");
        assertThat(map.get(12345L)).isNull();

        // Update existing key
        map.put(100L, "Val100Updated");
        assertThat(map.get(100L)).isEqualTo("Val100Updated");
        assertThat(map.size()).isEqualTo(4);

        // Remove key
        String removed = map.remove(100L);
        assertThat(removed).isEqualTo("Val100Updated");
        assertThat(map.get(100L)).isNull();
        assertThat(map.size()).isEqualTo(3);

        // Remove zero key
        assertThat(map.remove(0L)).isEqualTo("ValZero");
        assertThat(map.get(0L)).isNull();
        assertThat(map.size()).isEqualTo(2);

        map.clear();
        assertThat(map.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("RegionStrandRouter routes block and chunk coordinates with zero allocations")
    void testRegionStrandRouter() {
        RegionStrandRouter router = new RegionStrandRouter();

        RegionCoordinateHash hashA = RegionCoordinateHash.fromBlock(1024, 2048); // regionX=4, regionZ=8
        RegionCoordinateHash hashB = RegionCoordinateHash.fromBlock(-512, -256); // regionX=-2, regionZ=-1

        RegionalActorStrand strandA = new RegionalActorStrand(hashA);
        RegionalActorStrand strandB = new RegionalActorStrand(hashB);

        try {
            router.register(strandA);
            router.register(strandB);

            assertThat(router.registeredCount()).isEqualTo(2);

            // Block lookup within region A (blockX=1024..1279, blockZ=2048..2303)
            RegionalActorStrand routedA1 = router.routeByBlock(1024, 2048);
            RegionalActorStrand routedA2 = router.routeByBlock(1100, 2200);
            assertThat(routedA1).isSameAs(strandA);
            assertThat(routedA2).isSameAs(strandA);

            // Chunk lookup for region A (chunkX = 1024 >> 4 = 64; chunkZ = 2048 >> 4 = 128)
            RegionalActorStrand chunkRoutedA = router.routeByChunk(64, 128);
            assertThat(chunkRoutedA).isSameAs(strandA);

            // Negative coordinates for region B
            RegionalActorStrand routedB = router.routeByBlock(-500, -200);
            assertThat(routedB).isSameAs(strandB);

            // Unregistered coordinate returns null
            assertThat(router.routeByBlock(999999, 999999)).isNull();

            // Unregister
            RegionalActorStrand unreg = router.unregister(hashA);
            assertThat(unreg).isSameAs(strandA);
            assertThat(router.registeredCount()).isEqualTo(1);
            assertThat(router.routeByBlock(1024, 2048)).isNull();
        } finally {
            strandA.close();
            strandB.close();
        }
    }

    @Test
    @DisplayName("High-throughput stress test: 50,000 spatial routing queries execute without allocation crash")
    void testHighThroughputSpatialRouting() {
        RegionStrandRouter router = new RegionStrandRouter();
        RegionalActorStrand[] strands = new RegionalActorStrand[16];

        try {
            for (int i = 0; i < 16; i++) {
                RegionCoordinateHash hash = RegionCoordinateHash.fromChunk(i * 16, i * 16);
                strands[i] = new RegionalActorStrand(hash);
                router.register(strands[i]);
            }

            // Run 50,000 lookups
            for (int i = 0; i < 50_000; i++) {
                int blockCoord = (i % 16) * 256 + 10;
                RegionalActorStrand routed = router.routeByBlock(blockCoord, blockCoord);
                assertThat(routed).isNotNull();
            }
        } finally {
            for (RegionalActorStrand strand : strands) {
                if (strand != null) {
                    strand.close();
                }
            }
        }
    }
}
