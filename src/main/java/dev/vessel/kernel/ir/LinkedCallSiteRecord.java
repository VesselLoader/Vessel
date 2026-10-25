package dev.vessel.kernel.ir;

import java.util.Objects;

/**
 * Immutable carrier recording a re-linked bootstrap method argument or target call site.
 *
 * @param bsmOwner owner of the bootstrap method
 * @param bsmName name of the bootstrap method
 * @param bsmDescriptor descriptor of the bootstrap method
 * @param argumentIndex index of the updated argument in BSM arguments array
 * @param originalTarget original target handle or descriptor string
 * @param remappedTarget remapped target handle or descriptor string
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record LinkedCallSiteRecord(
        String bsmOwner,
        String bsmName,
        String bsmDescriptor,
        int argumentIndex,
        String originalTarget,
        String remappedTarget
) {
    public LinkedCallSiteRecord {
        Objects.requireNonNull(bsmOwner, "bsmOwner cannot be null");
        Objects.requireNonNull(bsmName, "bsmName cannot be null");
        Objects.requireNonNull(originalTarget, "originalTarget cannot be null");
        Objects.requireNonNull(remappedTarget, "remappedTarget cannot be null");
    }
}
