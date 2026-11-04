package net.minecraftforge.registries;

import dev.vessel.adapter.forge.DeferredRegisterBridge;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Forge deferred registration queue ({@code net.minecraftforge.registries.DeferredRegister})
 * bridged directly to VESSEL's {@link DeferredRegisterBridge}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> base registry type
 * @version 1.0.1
 */
public final class DeferredRegister<T> {

    private final String registryName;
    private final String modid;
    private final DeferredRegisterBridge underlyingBridge = new DeferredRegisterBridge();
    private final List<RegistryObject<T>> entries = new CopyOnWriteArrayList<>();

    private DeferredRegister(String registryName, String modid) {
        this.registryName = Objects.requireNonNull(registryName, "registryName must not be null").trim();
        this.modid = Objects.requireNonNull(modid, "modid must not be null").trim();
    }

    /**
     * Creates a new {@code DeferredRegister} for the given registry identifier and mod namespace.
     *
     * @param registryName canonical registry identifier (e.g., {@code "minecraft:block"})
     * @param modid        owning mod identifier
     * @param <B>          base registry entry type
     * @return a new {@code DeferredRegister} instance
     */
    public static <B> DeferredRegister<B> create(String registryName, String modid) {
        return new DeferredRegister<>(registryName, modid);
    }

    /**
     * Queues a new entry supplier under the specified path name and returns its {@link RegistryObject} holder.
     *
     * @param name entry path name within {@link #getModId()}
     * @param sup  lazy entry factory supplier
     * @param <I>  specific entry subtype
     * @return a {@link RegistryObject} holder for the queued entry
     */
    @SuppressWarnings("unchecked")
    public <I extends T> RegistryObject<I> register(String name, Supplier<? extends I> sup) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(sup, "sup must not be null");
        String fullId = modid.trim().toLowerCase(Locale.ROOT)
                + ":"
                + name.trim().toLowerCase(Locale.ROOT);
        RegistryObject<I> obj = new RegistryObject<>(fullId, sup);
        entries.add((RegistryObject<T>) obj);
        return obj;
    }

    /**
     * Subscribes this {@code DeferredRegister} to the given Forge mod {@link IEventBus} so that
     * queued entries are resolved during lifecycle/registry dispatch.
     *
     * @param bus target Forge mod event bus
     */
    public void register(IEventBus bus) {
        Objects.requireNonNull(bus, "bus must not be null");
        bus.addListener(new DeferredRegisterEventSubscriber(this));
    }

    /**
     * Resolves all queued {@link RegistryObject} entries and records them in VESSEL's
     * {@link DeferredRegisterBridge} and {@link dev.vessel.kernel.registry.VesselModRegistryHolder}.
     *
     * @return the number of entries resolved
     */
    public int resolveAllEntries() {
        int resolvedCount = 0;
        boolean isItem = registryName.toLowerCase(Locale.ROOT).contains("item");
        for (RegistryObject<T> entry : entries) {
            T resolved = entry.resolveNow();
            String id = entry.getId();
            int colonIndex = id.indexOf(':');
            String path = colonIndex >= 0 ? id.substring(colonIndex + 1) : id;
            if (isItem) {
                underlyingBridge.registerItemEntry(modid, path, Collections.emptyMap());
                if (resolved != null) {
                    dev.vessel.kernel.registry.VesselModRegistryHolder.registerModItem(id, resolved);
                }
            } else {
                underlyingBridge.registerBlockEntry(modid, path, 1);
                if (resolved != null) {
                    dev.vessel.kernel.registry.VesselModRegistryHolder.registerModBlock(id, resolved);
                }
            }
            resolvedCount++;
        }
        return resolvedCount;
    }

    /**
     * Returns an unmodifiable view of all queued {@link RegistryObject} entries.
     *
     * @return unmodifiable collection of entries
     */
    public Collection<RegistryObject<T>> getEntries() {
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
     * Returns the mod identifier owning this deferred register.
     *
     * @return mod identifier
     */
    public String getModId() {
        return modid;
    }

    /**
     * Returns VESSEL's underlying {@link DeferredRegisterBridge}.
     *
     * @return underlying deferred register bridge
     */
    public DeferredRegisterBridge getUnderlyingBridge() {
        return underlyingBridge;
    }
}
