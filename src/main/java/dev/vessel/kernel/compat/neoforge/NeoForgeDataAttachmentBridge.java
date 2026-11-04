package dev.vessel.kernel.compat.neoforge;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Bridge implementing NeoForge Data Attachments storage and retrieval for entities, block entities, and items.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class NeoForgeDataAttachmentBridge {

    private final ConcurrentMap<String, ConcurrentMap<String, Object>> holderAttachments = new ConcurrentHashMap<>();

    public void setData(Object holderInstance, String attachmentKey, Object data) {
        Objects.requireNonNull(holderInstance, "holderInstance cannot be null");
        Objects.requireNonNull(attachmentKey, "attachmentKey cannot be null");
        String holderKey = computeHolderIdentifier(holderInstance);
        holderAttachments.computeIfAbsent(holderKey, k -> new ConcurrentHashMap<>()).put(attachmentKey, data);
    }

    public Object getData(Object holderInstance, String attachmentKey) {
        if (holderInstance == null || attachmentKey == null) {
            return null;
        }
        String holderKey = computeHolderIdentifier(holderInstance);
        ConcurrentMap<String, Object> map = holderAttachments.get(holderKey);
        return map != null ? map.get(attachmentKey) : null;
    }

    public boolean hasData(Object holderInstance, String attachmentKey) {
        if (holderInstance == null || attachmentKey == null) {
            return false;
        }
        String holderKey = computeHolderIdentifier(holderInstance);
        ConcurrentMap<String, Object> map = holderAttachments.get(holderKey);
        return map != null && map.containsKey(attachmentKey);
    }

    public Object removeData(Object holderInstance, String attachmentKey) {
        if (holderInstance == null || attachmentKey == null) {
            return null;
        }
        String holderKey = computeHolderIdentifier(holderInstance);
        ConcurrentMap<String, Object> map = holderAttachments.get(holderKey);
        return map != null ? map.remove(attachmentKey) : null;
    }

    private String computeHolderIdentifier(Object holderInstance) {
        return holderInstance.getClass().getName() + "@" + System.identityHashCode(holderInstance);
    }
}
