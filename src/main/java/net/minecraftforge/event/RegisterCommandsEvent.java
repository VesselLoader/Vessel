package net.minecraftforge.event;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/**
 * Forge command registration event ({@code net.minecraftforge.event.RegisterCommandsEvent})
 * fired on {@code MinecraftForge.EVENT_BUS} to register Brigadier command trees.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class RegisterCommandsEvent extends Event {

    private final CommandDispatcher<Object> dispatcher;

    /**
     * Constructs a new {@code RegisterCommandsEvent} backed by a fresh {@link CommandDispatcher}.
     */
    public RegisterCommandsEvent() {
        this(new CommandDispatcher<>());
    }

    /**
     * Constructs a new {@code RegisterCommandsEvent} backed by the specified {@link CommandDispatcher}.
     *
     * @param dispatcher non-null Brigadier command dispatcher
     */
    public RegisterCommandsEvent(CommandDispatcher<Object> dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher must not be null");
    }

    /**
     * Returns the Brigadier {@link CommandDispatcher} for registering server commands.
     *
     * @return non-null command dispatcher
     */
    public CommandDispatcher<Object> getDispatcher() {
        return dispatcher;
    }
}
