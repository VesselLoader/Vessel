package dev.vessel.kernel.registry;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sentinel guard managing Minecraft dynamic registry freeze cycles across heterogeneous mod loaders.
 * <p>
 * Eliminates ERR-98 (Premature Registry.freeze() collisions when mods register late items or blocks).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DynamicRegistryFreezeSentinel {

    public static final String REGISTRY_BLOCK = "minecraft:block";
    public static final String REGISTRY_ITEM = "minecraft:item";
    public static final String REGISTRY_ENTITY_TYPE = "minecraft:entity_type";
    public static final String REGISTRY_BIOME = "minecraft:biome";
    public static final String REGISTRY_SOUND_EVENT = "minecraft:sound_event";
    public static final String REGISTRY_ENCHANTMENT = "minecraft:enchantment";
    public static final String REGISTRY_DATA_COMPONENT_TYPE = "minecraft:data_component_type";

    private final Map<String, RegistryFreezeState> states = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> registryLocks = new ConcurrentHashMap<>();

    /**
     * Constructs a sentinel pre-populated with standard registries in BOOT_PENDING state.
     */
    public DynamicRegistryFreezeSentinel() {
        registerDefaultRegistries();
    }

    /**
     * Registers a new dynamic registry identifier under sentinel surveillance.
     *
     * @param registryName namespaced registry identifier
     */
    public void monitorRegistry(String registryName) {
        Objects.requireNonNull(registryName, "registryName cannot be null");
        states.putIfAbsent(registryName, RegistryFreezeState.BOOT_PENDING);
        registryLocks.putIfAbsent(registryName, new ReentrantLock());
    }

    /**
     * Retrieves the current freeze state for a registry.
     *
     * @param registryName registry identifier
     * @return current freeze state, or UNLOCKED if unmonitored
     */
    public RegistryFreezeState getState(String registryName) {
        Objects.requireNonNull(registryName, "registryName cannot be null");
        return states.getOrDefault(registryName, RegistryFreezeState.UNLOCKED);
    }

    /**
     * Checks if new elements can currently be registered.
     *
     * @param registryName registry identifier
     * @return true if registrations are allowed
     */
    public boolean canRegister(String registryName) {
        RegistryFreezeState state = getState(registryName);
        return state == RegistryFreezeState.UNLOCKED || state == RegistryFreezeState.BOOT_PENDING;
    }

    /**
     * Checks if registry is in sealed or immutable state.
     *
     * @param registryName registry identifier
     * @return true if frozen
     */
    public boolean isFrozen(String registryName) {
        RegistryFreezeState state = getState(registryName);
        return state == RegistryFreezeState.SEALED || state == RegistryFreezeState.IMMUTABLE;
    }

    /**
     * Transitions a registry to SEALED state.
     *
     * @param registryName registry identifier
     */
    public void seal(String registryName) {
        monitorRegistry(registryName);
        states.put(registryName, RegistryFreezeState.SEALED);
    }

    /**
     * Issues an authorized lock token for scoped unfreezing.
     *
     * @param registryName target registry
     * @param requesterName name of loader or module
     * @return security token
     */
    public RegistryLockToken issueToken(String registryName, String requesterName) {
        monitorRegistry(registryName);
        return new RegistryLockToken(UUID.randomUUID().toString(), requesterName, System.currentTimeMillis());
    }

    /**
     * Safely executes an action with a temporarily unfrozen registry, re-locking on completion.
     *
     * @param registryName target registry
     * @param token authorization token
     * @param action callback action to execute
     */
    public void executeScopedUnfreeze(String registryName, RegistryLockToken token, Runnable action) {
        Objects.requireNonNull(registryName, "registryName cannot be null");
        Objects.requireNonNull(token, "token cannot be null");
        Objects.requireNonNull(action, "action cannot be null");

        monitorRegistry(registryName);
        ReentrantLock lock = registryLocks.get(registryName);
        lock.lock();

        RegistryFreezeState priorState = states.get(registryName);
        try {
            if (priorState == RegistryFreezeState.IMMUTABLE) {
                throw new IllegalStateException("Cannot unfreeze IMMUTABLE registry: " + registryName);
            }
            states.put(registryName, RegistryFreezeState.UNLOCKED);
            action.run();
        } finally {
            states.put(registryName, priorState);
            lock.unlock();
        }
    }

    /**
     * Transitions all monitored registries to a specified state.
     *
     * @param targetState state to apply
     */
    public void setAllStates(RegistryFreezeState targetState) {
        Objects.requireNonNull(targetState, "targetState cannot be null");
        for (String reg : states.keySet()) {
            states.put(reg, targetState);
        }
    }

    private void registerDefaultRegistries() {
        monitorRegistry(REGISTRY_BLOCK);
        monitorRegistry(REGISTRY_ITEM);
        monitorRegistry(REGISTRY_ENTITY_TYPE);
        monitorRegistry(REGISTRY_BIOME);
        monitorRegistry(REGISTRY_SOUND_EVENT);
        monitorRegistry(REGISTRY_ENCHANTMENT);
        monitorRegistry(REGISTRY_DATA_COMPONENT_TYPE);
    }
}
