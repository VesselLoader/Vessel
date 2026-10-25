package dev.vessel.kernel.adapter.neoforge;

import java.util.Objects;

/**
 * Exception thrown when NeoForge Data Attachment operations fail or encounter type/memory leaks.
 * <p>
 * Eliminates ERR-99 (AttachmentType leak across chunk unloads / concurrent access corruptions)
 * and addresses ECO-041 through ECO-050.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class NeoForgeAttachmentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String holderId;
    private final String attachmentKey;

    /**
     * Constructs an attachment exception with diagnostic details.
     *
     * @param errorCode     canonical error code
     * @param holderId      the target holder ID
     * @param attachmentKey key involved in violation
     * @param message       descriptive message
     */
    public NeoForgeAttachmentException(
            String errorCode,
            String holderId,
            String attachmentKey,
            String message
    ) {
        super(String.format("[%s] Holder '%s' attachment '%s': %s",
                errorCode, holderId, attachmentKey, message));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.holderId = Objects.requireNonNull(holderId, "holderId must not be null");
        this.attachmentKey = Objects.requireNonNull(attachmentKey, "attachmentKey must not be null");
    }

    /**
     * Constructs an attachment exception with diagnostic details and root cause.
     *
     * @param errorCode     canonical error code
     * @param holderId      the target holder ID
     * @param attachmentKey key involved in violation
     * @param message       descriptive message
     * @param cause         root cause throwable
     */
    public NeoForgeAttachmentException(
            String errorCode,
            String holderId,
            String attachmentKey,
            String message,
            Throwable cause
    ) {
        super(String.format("[%s] Holder '%s' attachment '%s': %s",
                errorCode, holderId, attachmentKey, message), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.holderId = Objects.requireNonNull(holderId, "holderId must not be null");
        this.attachmentKey = Objects.requireNonNull(attachmentKey, "attachmentKey must not be null");
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
     * Returns the target holder ID.
     *
     * @return holder ID
     */
    public String getHolderId() {
        return holderId;
    }

    /**
     * Returns the attachment key.
     *
     * @return attachment key
     */
    public String getAttachmentKey() {
        return attachmentKey;
    }
}
