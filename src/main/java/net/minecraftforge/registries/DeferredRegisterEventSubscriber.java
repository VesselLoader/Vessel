package net.minecraftforge.registries;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraftforge.fml.event.lifecycle.ModLifecycleEvent;

/**
 * Top-level event subscriber ({@code net.minecraftforge.registries.DeferredRegisterEventSubscriber})
 * that triggers {@link DeferredRegister#resolveAllEntries()} when a {@link RegisterEvent} or
 * {@link ModLifecycleEvent} is posted on a Forge {@code IEventBus}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class DeferredRegisterEventSubscriber implements Consumer<Object> {

    private final DeferredRegister<?> owner;

    /**
     * Constructs a new subscriber bound to the given {@link DeferredRegister}.
     *
     * @param owner non-null owning deferred register
     */
    public DeferredRegisterEventSubscriber(DeferredRegister<?> owner) {
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
    }

    /**
     * Resolves all queued entries on the owning {@link DeferredRegister} when a {@link RegisterEvent}
     * or {@link ModLifecycleEvent} arrives.
     *
     * @param event dispatched event payload
     */
    @Override
    public void accept(Object event) {
        if (event instanceof ModLifecycleEvent || event instanceof RegisterEvent) {
            owner.resolveAllEntries();
        }
    }
}
