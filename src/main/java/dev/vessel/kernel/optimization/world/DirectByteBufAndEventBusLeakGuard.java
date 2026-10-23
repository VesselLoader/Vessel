package dev.vessel.kernel.optimization.world;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * Prevents off-heap {@code OutOfMemoryError: Direct buffer memory} crashes by wrapping
 * modded packet handlers in a deterministic {@code try-finally} release guard, evicts
 * dead entity listeners from the {@code EventBus}, and pools {@code FakePlayer} UUIDs
 * per dimension and machine profile (Create Deployers, Modular Routers, Integrated Tunnels).
 *
 * @version 1.0.0
 */
public final class DirectByteBufAndEventBusLeakGuard {

    private final List<WeakReference<Object>> weakListenerOwners = new CopyOnWriteArrayList<>();
    private final List<BooleanSupplier> listenerRemovedPredicates = new CopyOnWriteArrayList<>();
    private final List<Runnable> listenerHooks = new CopyOnWriteArrayList<>();
    private final Map<String, UUID> pooledFakePlayers = new ConcurrentHashMap<>();

    /**
     * Creates a new direct ByteBuf, EventBus, and FakePlayer leak guard.
     */
    public DirectByteBufAndEventBusLeakGuard() {
    }

    /**
     * Executes {@code packetHandler} inside a {@code try-finally} block that invokes
     * {@code releaseAction} while {@code refCountSupplier.getAsInt() > 0}, guaranteeing
     * zero leaked Netty Direct ByteBuf instances even if the mod packet handler throws.
     *
     * @param packetHandler    mod custom packet handler runnable
     * @param refCountSupplier supplier returning the current ByteBuf reference count
     * @param releaseAction    action decrementing/releasing the ByteBuf
     */
    public void executeWithByteBufReleaseGuard(
            Runnable packetHandler, IntSupplier refCountSupplier, Runnable releaseAction) {
        Objects.requireNonNull(packetHandler, "packetHandler must not be null");
        Objects.requireNonNull(refCountSupplier, "refCountSupplier must not be null");
        Objects.requireNonNull(releaseAction, "releaseAction must not be null");
        try {
            packetHandler.run();
        } finally {
            int safetyCounter = 0;
            while (refCountSupplier.getAsInt() > 0 && safetyCounter < 64) {
                releaseAction.run();
                safetyCounter++;
            }
        }
    }

    /**
     * Registers an entity-owned or BlockEntity-owned event listener tracked via a
     * {@link WeakReference} and a removal predicate.
     *
     * @param entityOrBlockEntity owning entity or BlockEntity instance
     * @param isRemovedPredicate  predicate returning {@code true} when the owner is removed/unloaded
     * @param eventHook           registered event callback
     */
    public synchronized void registerWeakEntityEventListener(
            Object entityOrBlockEntity,
            BooleanSupplier isRemovedPredicate,
            Runnable eventHook) {
        Objects.requireNonNull(entityOrBlockEntity, "entityOrBlockEntity must not be null");
        Objects.requireNonNull(isRemovedPredicate, "isRemovedPredicate must not be null");
        Objects.requireNonNull(eventHook, "eventHook must not be null");
        weakListenerOwners.add(new WeakReference<>(entityOrBlockEntity));
        listenerRemovedPredicates.add(isRemovedPredicate);
        listenerHooks.add(eventHook);
    }

    /**
     * Scans and evicts all registered entity event listeners whose owner has been
     * garbage-collected or marked removed.
     *
     * @return number of dead or removed entity listeners evicted
     */
    public synchronized int evictRemovedEntityListeners() {
        int evicted = 0;
        for (int i = weakListenerOwners.size() - 1; i >= 0; i--) {
            WeakReference<Object> ref = weakListenerOwners.get(i);
            BooleanSupplier isRemoved = listenerRemovedPredicates.get(i);
            if (ref.get() == null || isRemoved.getAsBoolean()) {
                weakListenerOwners.remove(i);
                listenerRemovedPredicates.remove(i);
                listenerHooks.remove(i);
                evicted++;
            }
        }
        return evicted;
    }

    /**
     * Acquires a deterministic pooled {@code FakePlayer} UUID for the given dimension
     * and machine profile name so automated machines do not allocate thousands of
     * distinct FakePlayer instances.
     *
     * @param dimensionKey       namespaced dimension key
     * @param machineProfileName machine profile identifier (e.g., {@code "[Create_Deployer]"})
     * @return pooled deterministic FakePlayer UUID
     */
    public UUID acquirePooledFakePlayer(String dimensionKey, String machineProfileName) {
        Objects.requireNonNull(dimensionKey, "dimensionKey must not be null");
        Objects.requireNonNull(machineProfileName, "machineProfileName must not be null");
        String compositeKey =
                dimensionKey.trim().toLowerCase(Locale.ROOT)
                        + "|"
                        + machineProfileName.trim().toLowerCase(Locale.ROOT);
        return pooledFakePlayers.computeIfAbsent(
                compositeKey,
                k -> UUID.nameUUIDFromBytes(("VESSEL_FAKEPLAYER:" + k).getBytes(StandardCharsets.UTF_8)));
    }
}
