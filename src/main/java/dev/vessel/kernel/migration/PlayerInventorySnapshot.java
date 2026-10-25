package dev.vessel.kernel.migration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot encapsulating player inventory, experience metrics,
 * and modded capability data for zero-loss server migration.
 *
 * @param playerUuid       canonical lowercase UUID string of the player
 * @param xpLevel          current level of experience
 * @param xpProgress       progress towards the next level (0.0 to 1.0)
 * @param xpTotal          lifetime or total accumulated experience points
 * @param xpSeed           randomization seed for enchantment table
 * @param standardTags     immutable mapping of standard player attributes and tags
 * @param preservedModTags immutable mapping of protected third-party capability binary payloads
 * @param timestamp        creation timestamp in milliseconds
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record PlayerInventorySnapshot(
        String playerUuid,
        int xpLevel,
        float xpProgress,
        int xpTotal,
        int xpSeed,
        Map<String, String> standardTags,
        Map<String, byte[]> preservedModTags,
        long timestamp
) {

    /**
     * Compact constructor validating invariants and creating deep copies.
     */
    public PlayerInventorySnapshot {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        Objects.requireNonNull(standardTags, "standardTags cannot be null");
        Objects.requireNonNull(preservedModTags, "preservedModTags cannot be null");

        if (xpLevel < 0) {
            throw new IllegalArgumentException("xpLevel cannot be negative: " + xpLevel);
        }
        if (xpTotal < 0) {
            throw new IllegalArgumentException("xpTotal cannot be negative: " + xpTotal);
        }

        standardTags = Map.copyOf(standardTags);

        Map<String, byte[]> clonedModTags = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : preservedModTags.entrySet()) {
            clonedModTags.put(entry.getKey(), entry.getValue() != null ? entry.getValue().clone() : new byte[0]);
        }
        preservedModTags = Collections.unmodifiableMap(clonedModTags);
    }
}
