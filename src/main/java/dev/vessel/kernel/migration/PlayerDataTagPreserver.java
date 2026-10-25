package dev.vessel.kernel.migration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * High-integrity engine preserving 100% player inventory data, experience metrics,
 * and modded NBT capability structures during cross-platform server migrations.
 * <p>
 * Mitigates: <b>ERR-92</b>, <b>OPS-0001..0200</b>.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class PlayerDataTagPreserver {

    private final PlayerDataUuidSanitizer uuidSanitizer;
    private final ModdedInventoryNbtShield modShield;

    /**
     * Constructs a new preserver initializing internal sanitizers and shields.
     */
    public PlayerDataTagPreserver() {
        this.uuidSanitizer = new PlayerDataUuidSanitizer();
        this.modShield = new ModdedInventoryNbtShield();
    }

    /**
     * Creates a verified, sanitized player inventory snapshot from incoming raw attributes.
     *
     * @param rawUuid      the un-sanitized player UUID
     * @param xpLevel      experience level
     * @param xpProgress   progress to next level
     * @param xpTotal      total accumulated experience
     * @param xpSeed       enchantment seed
     * @param standardTags standard player data tags
     * @param moddedTags   raw modded inventory or capability tags
     * @return immutable validated snapshot
     */
    public PlayerInventorySnapshot createSnapshot(
            String rawUuid,
            int xpLevel,
            float xpProgress,
            int xpTotal,
            int xpSeed,
            Map<String, String> standardTags,
            Map<String, byte[]> moddedTags
    ) {
        String canonicalUuid = uuidSanitizer.sanitizeUuid(rawUuid);
        Map<String, byte[]> protectedModTags = modShield.extractProtectedTags(moddedTags);

        return new PlayerInventorySnapshot(
                canonicalUuid,
                xpLevel,
                xpProgress,
                xpTotal,
                xpSeed,
                standardTags != null ? standardTags : Collections.emptyMap(),
                protectedModTags,
                System.currentTimeMillis()
        );
    }

    /**
     * Non-destructively merges an incoming player snapshot with an existing saved snapshot.
     * <p>
     * Ensures experience metrics are monotonically preserved (never unintentionally downgraded),
     * standard attributes are updated, and all third-party mod capabilities from both sessions
     * are merged without data loss.
     * </p>
     *
     * @param existing the existing saved snapshot (may be null if first migration)
     * @param incoming the freshly loaded incoming snapshot
     * @return reconciled immutable snapshot
     */
    public PlayerInventorySnapshot mergeSnapshots(
            PlayerInventorySnapshot existing,
            PlayerInventorySnapshot incoming
    ) {
        Objects.requireNonNull(incoming, "incoming snapshot cannot be null");
        if (existing == null) {
            return incoming;
        }

        if (!existing.playerUuid().equalsIgnoreCase(incoming.playerUuid())) {
            throw new PlayerDataPreservationException(
                    "Cannot merge snapshots of different players: existing=" + existing.playerUuid()
                            + ", incoming=" + incoming.playerUuid()
            );
        }

        // Monotonic experience preservation
        int preservedXpTotal = Math.max(existing.xpTotal(), incoming.xpTotal());
        int preservedXpLevel = Math.max(existing.xpLevel(), incoming.xpLevel());
        float preservedXpProgress = (preservedXpLevel == incoming.xpLevel())
                ? incoming.xpProgress()
                : existing.xpProgress();
        int preservedXpSeed = incoming.xpSeed() != 0 ? incoming.xpSeed() : existing.xpSeed();

        // Standard tags union (incoming takes precedence)
        Map<String, String> mergedStandardTags = new HashMap<>(existing.standardTags());
        mergedStandardTags.putAll(incoming.standardTags());

        // Modded capability tags union
        Map<String, byte[]> mergedModTags = new HashMap<>(existing.preservedModTags());
        for (Map.Entry<String, byte[]> entry : incoming.preservedModTags().entrySet()) {
            if (entry.getValue() != null && entry.getValue().length > 0) {
                mergedModTags.put(entry.getKey(), entry.getValue().clone());
            }
        }

        return new PlayerInventorySnapshot(
                incoming.playerUuid(),
                preservedXpLevel,
                preservedXpProgress,
                preservedXpTotal,
                preservedXpSeed,
                mergedStandardTags,
                mergedModTags,
                System.currentTimeMillis()
        );
    }
}
