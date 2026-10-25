package dev.vessel.kernel.item.capability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sovereign capability preservation bridge preventing data loss of Forge/NeoForge capabilities
 * when Bukkit plugins mutate items via {@code ItemStack.setItemMeta()} under Decision 55 and CBT-0001..0400.
 *
 * <p>Standard Spigot/Paper {@code CraftItemStack.setItemMeta()} reconstructs NBT purely from Bukkit
 * {@code ItemMeta}, discarding unmapped Forge capability tags (e.g. {@code forge:caps}, Mekanism energy,
 * Curios slots). This bridge captures capabilities prior to mutation and restores them losslessly.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class ItemMetaCapabilityPreservationBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemMetaCapabilityPreservationBridge.class);

    public static final String FORGE_CAPS_TAG_KEY = "ForgeCaps";
    public static final String DATA_ATTACHMENTS_TAG_KEY = "neoforge:attachments";

    private final Map<String, PreservedCapabilitySnapshot> temporarySnapshots = new ConcurrentHashMap<>();

    /**
     * Constructs a new capability preservation bridge.
     */
    public ItemMetaCapabilityPreservationBridge() {
    }

    /**
     * Captures a capability snapshot for the given item identifier and raw capability attachments.
     *
     * @param itemIdentifier canonical item type ID
     * @param attachments    map of capability attachments
     * @return constructed snapshot
     */
    public PreservedCapabilitySnapshot capture(String itemIdentifier, Map<String, CapabilityTagAttachment> attachments) {
        Objects.requireNonNull(itemIdentifier, "itemIdentifier cannot be null");
        Objects.requireNonNull(attachments, "attachments cannot be null");

        PreservedCapabilitySnapshot snapshot = new PreservedCapabilitySnapshot(
                itemIdentifier,
                attachments,
                System.nanoTime()
        );
        temporarySnapshots.put(itemIdentifier, snapshot);
        LOGGER.debug("Captured {} capability attachments for item '{}'", attachments.size(), itemIdentifier);
        return snapshot;
    }

    /**
     * Restores preserved capabilities onto the target component or NBT map post-Bukkit mutation.
     *
     * @param targetNbtOrComponents destination map (e.g. Minecraft DataComponent or NBT compound)
     * @param snapshot               previously captured snapshot
     * @return unmodifiable merged map containing both Bukkit changes and Forge capabilities
     */
    public Map<String, Object> restoreCapabilities(
            Map<String, Object> targetNbtOrComponents,
            PreservedCapabilitySnapshot snapshot
    ) {
        Objects.requireNonNull(targetNbtOrComponents, "targetNbtOrComponents cannot be null");
        if (snapshot == null || !snapshot.hasAttachments()) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(targetNbtOrComponents));
        }

        Map<String, Object> merged = new LinkedHashMap<>(targetNbtOrComponents);

        // Re-inject ForgeCaps compound if preserved
        Map<String, Object> capsCompound = new LinkedHashMap<>();
        for (Map.Entry<String, CapabilityTagAttachment> entry : snapshot.attachments().entrySet()) {
            capsCompound.put(entry.getKey(), entry.getValue().rawPayload());
        }

        merged.put(FORGE_CAPS_TAG_KEY, Collections.unmodifiableMap(capsCompound));
        LOGGER.debug("Restored {} capabilities into item '{}'", snapshot.attachments().size(), snapshot.itemIdentifier());
        return Collections.unmodifiableMap(merged);
    }

    /**
     * Clears temporary cached snapshot for an item.
     *
     * @param itemIdentifier item ID
     */
    public void discardSnapshot(String itemIdentifier) {
        if (itemIdentifier != null) {
            temporarySnapshots.remove(itemIdentifier);
        }
    }

    /**
     * Returns the number of snapshots currently staged in the bridge cache.
     *
     * @return cached count
     */
    public int getCachedSnapshotCount() {
        return temporarySnapshots.size();
    }
}
