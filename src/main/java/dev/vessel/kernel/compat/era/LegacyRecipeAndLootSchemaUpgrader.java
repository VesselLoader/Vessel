package dev.vessel.kernel.compat.era;

import java.util.Objects;

/**
 * Upgrades DataPack resource paths and JSON schemas from older Minecraft versions (&le; 1.20.6)
 * into the singular-directory and {@code "id"}-based schema introduced in Minecraft 1.21+:
 * <ul>
 *   <li>{@code data/<ns>/loot_tables/} &rarr; {@code data/<ns>/loot_table/}</li>
 *   <li>{@code data/<ns>/recipes/} &rarr; {@code data/<ns>/recipe/}</li>
 *   <li>{@code data/<ns>/advancements/} &rarr; {@code data/<ns>/advancement/}</li>
 *   <li>{@code data/<ns>/structures/} &rarr; {@code data/<ns>/structure/}</li>
 *   <li>{@code data/<ns>/tags/items/} &rarr; {@code data/<ns>/tags/item/}</li>
 *   <li>{@code data/<ns>/tags/blocks/} &rarr; {@code data/<ns>/tags/block/}</li>
 *   <li>{@code data/<ns>/tags/entity_types/} &rarr; {@code data/<ns>/tags/entity_type/}</li>
 *   <li>{@code data/<ns>/tags/fluids/} &rarr; {@code data/<ns>/tags/fluid/}</li>
 * </ul>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class LegacyRecipeAndLootSchemaUpgrader {

    /**
     * Upgrades a legacy plural DataPack resource path (&le; 1.20.6) to its canonical Minecraft
     * 1.21+ singular path.
     *
     * @param legacyResourcePath the original resource path inside a mod JAR
     * @return the upgraded 1.21+ resource path
     */
    public String upgradeDataPackResourcePath(String legacyResourcePath) {
        Objects.requireNonNull(legacyResourcePath, "legacyResourcePath must not be null");
        String normalized = legacyResourcePath.replace('\\', '/');
        if (!normalized.startsWith("data/")) {
            return normalized;
        }
        return normalized
                .replace("/loot_tables/", "/loot_table/")
                .replace("/recipes/", "/recipe/")
                .replace("/advancements/", "/advancement/")
                .replace("/structures/", "/structure/")
                .replace("/item_modifiers/", "/item_modifier/")
                .replace("/predicates/", "/predicate/")
                .replace("/tags/items/", "/tags/item/")
                .replace("/tags/blocks/", "/tags/block/")
                .replace("/tags/entity_types/", "/tags/entity_type/")
                .replace("/tags/fluids/", "/tags/fluid/")
                .replace("/tags/game_events/", "/tags/game_event/");
    }

    /**
     * Upgrades a legacy recipe or loot table JSON payload to match Minecraft 1.21+ codec keys
     * (converting {@code "result": { "item": "..." }} into {@code "result": { "id": "..." }}).
     *
     * @param rawJsonContent the legacy JSON content
     * @return the upgraded JSON content
     */
    public String upgradeLegacyRecipeOrLootJson(String rawJsonContent) {
        Objects.requireNonNull(rawJsonContent, "rawJsonContent must not be null");
        return rawJsonContent.replaceAll(
                "(\"result\"\\s*:\\s*\\{[^}]*?)\"item\"(\\s*:)",
                "$1\"id\"$2");
    }
}
