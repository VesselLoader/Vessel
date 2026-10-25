package dev.vessel.kernel.item.capability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Snapshot holding all capability attachments of an ItemStack prior to Bukkit
 * {@code setItemMeta()} operations.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @param itemIdentifier canonical identifier of the item type (e.g. "mekanism:atomic_disassembler")
 * @param attachments    unmodifiable map of capability attachments keyed by capability ID
 * @param captureNanos   high-precision capture timestamp
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record PreservedCapabilitySnapshot(
        String itemIdentifier,
        Map<String, CapabilityTagAttachment> attachments,
        long captureNanos
) {

    /**
     * Compact constructor creating defensive unmodifiable copy of capability attachments.
     */
    public PreservedCapabilitySnapshot {
        Objects.requireNonNull(itemIdentifier, "itemIdentifier cannot be null");
        Objects.requireNonNull(attachments, "attachments cannot be null");
        attachments = Collections.unmodifiableMap(new LinkedHashMap<>(attachments));
    }

    /**
     * Retrieves an attachment for a specific capability key.
     *
     * @param capabilityKey target key
     * @return matching attachment or null
     */
    public CapabilityTagAttachment getAttachment(String capabilityKey) {
        return attachments.get(capabilityKey);
    }

    /**
     * Checks if this snapshot contains attachments.
     *
     * @return {@code true} if at least one capability is preserved
     */
    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }
}
