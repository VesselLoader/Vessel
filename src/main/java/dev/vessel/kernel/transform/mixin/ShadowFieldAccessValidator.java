package dev.vessel.kernel.transform.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validates the existence and descriptor compatibility of target fields for Mixin @Shadow accessors.
 * <p>
 * Ensures target fields exist on target classes before applying shadow patches,
 * eliminating runtime {@link NoSuchFieldError} crashes caused by remapping drift or stripped members.
 * </p>
 * <p>
 * Directly resolves ERR-60 and MAP-MIX-0401..0700.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ShadowFieldAccessValidator {

    /**
     * Validates a shadow field against a target ASM ClassNode.
     *
     * @param targetNode the target ClassNode being inspected
     * @param descriptor the shadow field requirements
     * @return validation outcome result
     */
    public ShadowValidationResult validate(ClassNode targetNode, ShadowFieldDescriptor descriptor) {
        Objects.requireNonNull(targetNode, "targetNode cannot be null");
        Objects.requireNonNull(descriptor, "descriptor cannot be null");

        if (targetNode.fields == null || targetNode.fields.isEmpty()) {
            return new ShadowValidationResult(
                    descriptor,
                    ShadowValidationStatus.MISSING_FIELD,
                    "Target class " + targetNode.name + " defines no fields"
            );
        }

        List<FieldNode> matchingName = new ArrayList<>();
        for (FieldNode fn : targetNode.fields) {
            if (fn.name.equals(descriptor.fieldName())) {
                matchingName.add(fn);
            }
        }

        if (matchingName.isEmpty()) {
            return new ShadowValidationResult(
                    descriptor,
                    ShadowValidationStatus.MISSING_FIELD,
                    "Field '" + descriptor.fieldName() + "' not found in class " + targetNode.name
            );
        }

        if (matchingName.size() == 1) {
            FieldNode fn = matchingName.get(0);
            if (fn.desc.equals(descriptor.fieldDescriptor())) {
                return new ShadowValidationResult(
                        descriptor,
                        ShadowValidationStatus.VALID,
                        "Field '" + descriptor.fieldName() + "' matched successfully with descriptor " + fn.desc
                );
            } else {
                return new ShadowValidationResult(
                        descriptor,
                        ShadowValidationStatus.DESCRIPTOR_MISMATCH,
                        "Field '" + descriptor.fieldName() + "' found but descriptor mismatched. Expected: "
                                + descriptor.fieldDescriptor() + ", found: " + fn.desc
                );
            }
        }

        // Multiple fields matching the name
        for (FieldNode fn : matchingName) {
            if (fn.desc.equals(descriptor.fieldDescriptor())) {
                return new ShadowValidationResult(
                        descriptor,
                        ShadowValidationStatus.VALID,
                        "Field '" + descriptor.fieldName() + "' matched among multiple candidates"
                );
            }
        }

        return new ShadowValidationResult(
                descriptor,
                ShadowValidationStatus.AMBIGUOUS_MATCH,
                "Multiple fields named '" + descriptor.fieldName() + "' found but none matched descriptor "
                        + descriptor.fieldDescriptor()
        );
    }
}
