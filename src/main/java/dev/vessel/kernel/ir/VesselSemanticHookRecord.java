package dev.vessel.kernel.ir;

import java.util.Objects;

/**
 * Immutable canonical record representing a semantic bytecode interception intent.
 * <p>
 * Decouples the hook intent from ecosystem-specific annotations (@Inject, @Redirect,
 * @WrapOperation, @SubscribeEvent) into a normalized Ahead-Of-Time representation.
 * </p>
 *
 * @param intent type of semantic interception
 * @param targetOwner internal name of target class (e.g. "net/minecraft/server/MinecraftServer")
 * @param targetName target method name
 * @param targetDesc target method descriptor
 * @param sourceClass internal name of injecting module class
 * @param sourceMethod injecting handler method name
 * @param priority execution priority order (higher priority executes first)
 * @version 1.0.1
 * @since 1.0.0 (Pass K-7)
 */
public record VesselSemanticHookRecord(
        VesselSemanticHookIntent intent,
        String targetOwner,
        String targetName,
        String targetDesc,
        String sourceClass,
        String sourceMethod,
        int priority
) {

    public VesselSemanticHookRecord {
        Objects.requireNonNull(intent, "intent cannot be null");
        Objects.requireNonNull(targetOwner, "targetOwner cannot be null");
        Objects.requireNonNull(targetName, "targetName cannot be null");
        Objects.requireNonNull(targetDesc, "targetDesc cannot be null");
        Objects.requireNonNull(sourceClass, "sourceClass cannot be null");
        Objects.requireNonNull(sourceMethod, "sourceMethod cannot be null");

        targetOwner = targetOwner.replace('.', '/');
        sourceClass = sourceClass.replace('.', '/');
    }
}
