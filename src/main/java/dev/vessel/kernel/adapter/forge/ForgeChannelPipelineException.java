package dev.vessel.kernel.adapter.forge;

import java.util.Objects;

/**
 * Typed exception thrown upon Forge custom channel payload routing or protocol violations.
 * <p>
 * Eliminates ERR-91 and mitigates ecosystem mismatch codes ECO-011 through ECO-020.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class ForgeChannelPipelineException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String channelName;

    /**
     * Constructs a channel pipeline exception with code, channel name, and explanatory message.
     *
     * @param errorCode   error code identifier
     * @param channelName namespaced channel identifier
     * @param message     description of the violation
     */
    public ForgeChannelPipelineException(String errorCode, String channelName, String message) {
        super(String.format("[%s] Channel '%s' error: %s", errorCode, channelName, message));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.channelName = Objects.requireNonNull(channelName, "channelName must not be null");
    }

    /**
     * Constructs a channel pipeline exception with code, channel name, explanatory message, and root cause.
     *
     * @param errorCode   error code identifier
     * @param channelName namespaced channel identifier
     * @param message     description of the violation
     * @param cause       underlying cause
     */
    public ForgeChannelPipelineException(String errorCode, String channelName, String message, Throwable cause) {
        super(String.format("[%s] Channel '%s' error: %s", errorCode, channelName, message), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.channelName = Objects.requireNonNull(channelName, "channelName must not be null");
    }

    /**
     * Returns the error code.
     *
     * @return error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the channel identifier.
     *
     * @return channel identifier
     */
    public String getChannelName() {
        return channelName;
    }
}
