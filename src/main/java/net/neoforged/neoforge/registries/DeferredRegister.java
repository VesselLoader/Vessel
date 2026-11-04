package net.neoforged.neoforge.registries;

import dev.vessel.adapter.forge.DeferredRegisterBridge;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;

/**
 * Canonical NeoForge {@code DeferredRegister} implementation
 * ({@code net.neoforged.neoforge.registries.DeferredRegister}) backed by VESSEL's
 * {@link DeferredRegisterBridge}.
 *
 * <p>Queues {@link DeferredHolder} entries during mod construction and binds them when attached to
 * a mod's {@link IEventBus} via {@link #register(IEventBus)}.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @param <T> base registry type
 */
public final class DeferredRegister<T> {

    private final String registryName;
    private final String modid;
    private final DeferredRegisterBridge underlyingBridge = new DeferredRegisterBridge();
    private final List<DeferredHolder<T, ? extends T>> entries = new CopyOnWriteArrayList<>();

    private DeferredRegister(String registryName, String modid) {
        this.registryName = Objects.requireNonNull(registryName, "registryName must not be null");
        this.modid = Objects.requireNonNull(modid, "modid must not be null");
    }

    /**
     * Creates a new NeoForge {@code DeferredRegister} for the given registry key and mod namespace.
     *
     * @param registryName target registry name (e.g. {@code "minecraft:block"})
     * @param modid        owning mod identifier
     * @param <B>          base registry entry type
     * @return new {@code DeferredRegister} instance
     */
    public static <B> DeferredRegister<B> create(String registryName, String modid) {
        return new DeferredRegister<>(registryName, modid);
    }

    /**
     * Queues a new entry supplier under the specified path name and returns its {@link DeferredHolder}.
     *
     * @param name entry path name within {@link #getModId()}
     * @param sup  lazy supplier creating the entry instance
     * @param <I>  concrete entry subtype
     * @return {@link DeferredHolder} handle for the queued entry
     */
    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> sup) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(sup, "sup must not be null");
        String fullId = modid.trim().toLowerCase(Locale.ROOT)
                + ":"
                + name.trim().toLowerCase(Locale.ROOT);
        DeferredHolder<T, I> holder = new DeferredHolder<>(fullId, sup);
        entries.add(holder);
        return holder;
    }

    /**
     * Subscribes this {@code DeferredRegister} to the given NeoForge mod {@link IEventBus} so all
     * queued entries are resolved when lifecycle or registry events fire.
     *
     * @param bus target mod event bus
     */
    public void register(IEventBus bus) {
        Objects.requireNonNull(bus, "bus must not be null");
        bus.addListener(new NeoForgeDeferredRegisterSubscriber(this));
    }

    /**
     * Resolves and binds all queued {@link DeferredHolder} entries into the underlying VESSEL
     * {@link DeferredRegisterBridge} and {@link dev.vessel.kernel.registry.VesselModRegistryHolder}.
     *
     * @return number of resolved entries
     */
    public int resolveAllEntries() {
        int count = 0;
        boolean isItem = registryName.toLowerCase(Locale.ROOT).contains("item");
        for (DeferredHolder<T, ? extends T> holder : entries) {
            T resolved = holder.resolveNow();
            String id = holder.getId();
            String entryPath = id.substring(id.indexOf(':') + 1);
            if (isItem) {
                underlyingBridge.registerItemEntry(modid, entryPath, Collections.emptyMap());
                if (resolved != null) {
                    dev.vessel.kernel.registry.VesselModRegistryHolder.registerModItem(id, resolved);
                }
            } else {
                underlyingBridge.registerBlockEntry(modid, entryPath, 1);
                if (resolved != null) {
                    dev.vessel.kernel.registry.VesselModRegistryHolder.registerModBlock(id, resolved);
                }
            }
            count++;
        }
        return count;
    }

    /**
     * Returns an unmodifiable view of all queued {@link DeferredHolder} entries.
     *
     * @return unmodifiable collection of entries
     */
    public Collection<DeferredHolder<T, ? extends T>> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Returns the canonical registry name targeted by this deferred register.
     *
     * @return registry name
     */
    public String getRegistryName() {
        return registryName;
    }

    /**
     * Returns the mod identifier namespace owning this deferred register.
     *
     * @return mod identifier
     */
    public String getModId() {
        return modid;
    }

    /**
     * Returns the underlying VESSEL {@link DeferredRegisterBridge}.
     *
     * @return underlying bridge
     */
    public DeferredRegisterBridge getUnderlyingBridge() {
        return underlyingBridge;
    }
}
