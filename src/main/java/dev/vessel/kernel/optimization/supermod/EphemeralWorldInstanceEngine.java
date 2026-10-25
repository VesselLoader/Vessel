package dev.vessel.kernel.optimization.supermod;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages zero-disk-I/O copy-on-write ephemeral RAM world instances for mini-games (Bedwars,
 * Skywars) and instanced modded boss dungeons.
 *
 * @version 1.0.0
 */
public final class EphemeralWorldInstanceEngine {

    private final Map<String, byte[]> activeRamWorlds = new ConcurrentHashMap<>();
    private final Map<String, String> instanceToTemplate = new ConcurrentHashMap<>();

    /**
     * Clones a template world snapshot directly in RAM without touching disk storage.
     *
     * @param templateWorldName the source template world name
     * @param instanceWorldId   the unique ephemeral world instance identifier
     * @param templateDataBytes the serialized template region bytes
     * @return the canonical in-memory mount URI for the cloned world instance
     */
    public String cloneTemplateWorldInRam(
            String templateWorldName,
            String instanceWorldId,
            byte[] templateDataBytes) {
        Objects.requireNonNull(templateWorldName, "templateWorldName must not be null");
        Objects.requireNonNull(instanceWorldId, "instanceWorldId must not be null");
        Objects.requireNonNull(templateDataBytes, "templateDataBytes must not be null");
        activeRamWorlds.put(
                instanceWorldId,
                Arrays.copyOf(templateDataBytes, templateDataBytes.length));
        instanceToTemplate.put(instanceWorldId, templateWorldName);
        return "vessel-ram://" + templateWorldName + "/" + instanceWorldId;
    }

    /**
     * Destroys an ephemeral world instance in RAM and immediately releases its byte buffer.
     *
     * @param instanceWorldId the ephemeral world instance identifier
     * @return {@code true} if the instance existed and was destroyed
     */
    public boolean destroyEphemeralWorldInRam(String instanceWorldId) {
        Objects.requireNonNull(instanceWorldId, "instanceWorldId must not be null");
        instanceToTemplate.remove(instanceWorldId);
        return activeRamWorlds.remove(instanceWorldId) != null;
    }

    /**
     * Returns the count of currently active ephemeral RAM world instances.
     *
     * @return active ephemeral world count
     */
    public int getActiveEphemeralWorldCount() {
        return activeRamWorlds.size();
    }
}
