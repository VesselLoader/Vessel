package dev.vessel.kernel.adapter.forge;

import java.util.Objects;

/**
 * Exception thrown when Inter-Mod Communications (IMC) violates lifecycle gates or delivery contracts.
 * <p>
 * Eliminates ECO-005 (Inter-Mod Comms deadlocks and out-of-order dispatch failures).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class ForgeInterModCommsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String ERROR_CODE = "ECO-005";

    private final String senderModId;
    private final String targetModId;
    private final InterModCommsLifecycleStage currentStage;

    /**
     * Constructs an IMC exception with diagnostic context.
     *
     * @param senderModId  sender mod identifier
     * @param targetModId  target mod identifier
     * @param currentStage current stage of the router
     * @param message      explanatory message
     */
    public ForgeInterModCommsException(
            String senderModId,
            String targetModId,
            InterModCommsLifecycleStage currentStage,
            String message
    ) {
        super(String.format("[%s] IMC dispatch from '%s' to '%s' failed in stage %s: %s",
                ERROR_CODE, senderModId, targetModId, currentStage, message));
        this.senderModId = Objects.requireNonNull(senderModId, "senderModId must not be null");
        this.targetModId = Objects.requireNonNull(targetModId, "targetModId must not be null");
        this.currentStage = Objects.requireNonNull(currentStage, "currentStage must not be null");
    }

    /**
     * Returns the sender mod identifier.
     *
     * @return sender mod ID
     */
    public String getSenderModId() {
        return senderModId;
    }

    /**
     * Returns the target mod identifier.
     *
     * @return target mod ID
     */
    public String getTargetModId() {
        return targetModId;
    }

    /**
     * Returns the lifecycle stage.
     *
     * @return lifecycle stage
     */
    public InterModCommsLifecycleStage getCurrentStage() {
        return currentStage;
    }
}
