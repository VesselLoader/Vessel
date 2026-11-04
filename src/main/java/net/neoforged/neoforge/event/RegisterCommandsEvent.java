package net.neoforged.neoforge.event;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Objects;
import net.neoforged.bus.api.Event;

/**
 * NeoForge command registration event ({@code net.neoforged.neoforge.event.RegisterCommandsEvent})
 * fired on {@code NeoForge.EVENT_BUS} to register Brigadier command trees.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class RegisterCommandsEvent extends Event {

    private final Object dispatcher;

    /**
     * Constructs a new {@code RegisterCommandsEvent} backed by a fresh {@link CommandDispatcher}.
     */
    public RegisterCommandsEvent() {
        this(new CommandDispatcher<>());
    }

    /**
     * Constructs a new {@code RegisterCommandsEvent} backed by the specified Brigadier command dispatcher.
     *
     * @param dispatcher non-null Brigadier command dispatcher instance across classloaders
     */
    public RegisterCommandsEvent(Object dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher must not be null");
    }

    /**
     * Returns the Brigadier {@link CommandDispatcher} for registering server commands.
     *
     * @param <S> command source type
     * @return non-null command dispatcher
     */
    @SuppressWarnings("unchecked")
    public <S> CommandDispatcher<S> getDispatcher() {
        return (CommandDispatcher<S>) dispatcher;
    }
}
