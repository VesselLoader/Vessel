package net.neoforged.neoforge.registries;

import java.util.Objects;
import java.util.function.Consumer;
import net.neoforged.fml.event.lifecycle.ModLifecycleEvent;

/**
 * Standalone NeoForge mod event bus subscriber that resolves all queued entries on a
 * {@link DeferredRegister} when a {@link RegisterEvent} or {@link ModLifecycleEvent} is posted.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class NeoForgeDeferredRegisterSubscriber implements Consumer<Object> {

    private final DeferredRegister<?> owner;

    /**
     * Constructs a subscriber bound to the specified {@link DeferredRegister}.
     *
     * @param owner the owning deferred register
     */
    public NeoForgeDeferredRegisterSubscriber(DeferredRegister<?> owner) {
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
    }

    @Override
    public void accept(Object event) {
        if (event instanceof ModLifecycleEvent || event instanceof RegisterEvent) {
            owner.resolveAllEntries();
        }
    }
}
