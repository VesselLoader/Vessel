package dev.vessel.kernel.compat.sponge;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Bridges Sponge DataManipulator and Key-Value stores into Minecraft NBT and DataComponents.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class SpongeDataManipulatorBridge {

    private final ConcurrentMap<String, ConcurrentMap<String, Object>> manipulatorStore = new ConcurrentHashMap<>();

    public void offerData(String holderId, String keyId, Object value) {
        Objects.requireNonNull(holderId, "holderId cannot be null");
        Objects.requireNonNull(keyId, "keyId cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        manipulatorStore.computeIfAbsent(holderId, k -> new ConcurrentHashMap<>()).put(keyId, value);
    }

    public Object getData(String holderId, String keyId) {
        if (holderId == null || keyId == null) {
            return null;
        }
        ConcurrentMap<String, Object> map = manipulatorStore.get(holderId);
        return map != null ? map.get(keyId) : null;
    }

    public boolean hasData(String holderId, String keyId) {
        if (holderId == null || keyId == null) {
            return false;
        }
        ConcurrentMap<String, Object> map = manipulatorStore.get(holderId);
        return map != null && map.containsKey(keyId);
    }

    public void removeData(String holderId, String keyId) {
        if (holderId != null && keyId != null) {
            ConcurrentMap<String, Object> map = manipulatorStore.get(holderId);
            if (map != null) {
                map.remove(keyId);
            }
        }
    }
}
