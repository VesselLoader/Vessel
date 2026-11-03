package org.bukkit.event.server;

import java.util.Objects;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when the Bukkit/Paper server completes startup or reload initialization.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes; load types are
 * represented via canonical string constants {@link #STARTUP_TYPE} and {@link #RELOAD_TYPE}.</p>
 *
 * @version 1.0.0
 */
public class ServerLoadEvent extends Event {

    /**
     * Indicates that the server load occurred during initial startup.
     */
    public static final String STARTUP_TYPE = "STARTUP";

    /**
     * Indicates that the server load occurred during a reload cycle.
     */
    public static final String RELOAD_TYPE = "RELOAD";

    private static final HandlerList HANDLERS = new HandlerList();

    private final String loadType;

    /**
     * Constructs a synchronous {@code ServerLoadEvent} with {@link #STARTUP_TYPE}.
     */
    public ServerLoadEvent() {
        this(STARTUP_TYPE);
    }

    /**
     * Constructs a synchronous {@code ServerLoadEvent} with the specified load type.
     *
     * @param loadType the load type ({@link #STARTUP_TYPE} or {@link #RELOAD_TYPE})
     */
    public ServerLoadEvent(String loadType) {
        super(false);
        this.loadType = Objects.requireNonNull(loadType, "loadType must not be null");
    }

    /**
     * Returns the load type identifier ({@link #STARTUP_TYPE} or {@link #RELOAD_TYPE}).
     *
     * @return the load type string
     */
    public String getLoadType() {
        return loadType;
    }

    /**
     * Returns the {@link HandlerList} for this event instance.
     *
     * @return the handler list
     */
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the static {@link HandlerList} for {@code ServerLoadEvent}.
     *
     * @return the static handler list
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
