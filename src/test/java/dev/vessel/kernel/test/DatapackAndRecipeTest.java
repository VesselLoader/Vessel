package dev.vessel.kernel.test;

import dev.vessel.kernel.datapack.RecipeMatchCache;
import dev.vessel.kernel.datapack.TagRegistryIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit verification suite for TagRegistryIndex and RecipeMatchCache.
 *
 * @version 1.0.0
 */
public class DatapackAndRecipeTest {

    @Test
    @DisplayName("TagRegistryIndex registers tags and evaluates membership in binary search")
    void testTagMembership() {
        TagRegistryIndex index = new TagRegistryIndex();

        index.registerTag("c:iron_ores", new int[]{15, 108, 42});
        assertThat(index.getTagCount()).isEqualTo(1);
        assertThat(index.getMemberCount("c:iron_ores")).isEqualTo(3);

        // Members are sorted internally [15, 42, 108]
        assertThat(index.isMember("c:iron_ores", 15)).isTrue();
        assertThat(index.isMember("c:iron_ores", 42)).isTrue();
        assertThat(index.isMember("c:iron_ores", 108)).isTrue();

        // Non-members return false
        assertThat(index.isMember("c:iron_ores", 99)).isFalse();
        assertThat(index.isMember("unknown:tag", 15)).isFalse();
    }

    @Test
    @DisplayName("RecipeMatchCache computes grid layout hashes and caches match results")
    void testRecipeCache() {
        RecipeMatchCache cache = new RecipeMatchCache(64);

        int[] craftingGrid2x2 = new int[]{1, 1, 1, 1}; // 4 oak planks -> 1 crafting table
        long hash = RecipeMatchCache.computeGridHash(craftingGrid2x2, 2, 2);

        // First query: cache miss
        assertThat(cache.get(hash)).isEqualTo(-1);
        assertThat(cache.getMissCount()).isEqualTo(1L);

        // Store recipe result: crafting table (id=58)
        cache.put(hash, 58);

        // Second query: cache hit
        assertThat(cache.get(hash)).isEqualTo(58);
        assertThat(cache.getHitCount()).isEqualTo(1L);
        assertThat(cache.getHitRatio()).isEqualTo(0.5);
    }
}
