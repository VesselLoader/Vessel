package dev.vessel.kernel.transform.mixin;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * High-performance parser extracting Mixin and MixinExtras injection metadata
 * directly from ASM MethodNode bytecode annotations.
 *
 * <p>Supports:
 * <ul>
 *   <li>{@code @Inject}</li>
 *   <li>{@code @Redirect}</li>
 *   <li>{@code @ModifyVariable}</li>
 *   <li>{@code @WrapOperation}</li>
 * </ul>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MixinAnnotationParser {

    public static final String INJECT_DESC         = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    public static final String REDIRECT_DESC       = "Lorg/spongepowered/asm/mixin/injection/Redirect;";
    public static final String MODIFY_VAR_DESC     = "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;";
    public static final String WRAP_OPERATION_DESC = "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";

    /**
     * Parses the primary Mixin injection annotation from a method node if present.
     *
     * @param methodNode the ASM method node to inspect
     * @return parsed metadata, or empty if no recognized Mixin annotation is present
     */
    public Optional<ParsedMixinAnnotation> parseMethodAnnotation(MethodNode methodNode) {
        Objects.requireNonNull(methodNode, "methodNode cannot be null");

        Optional<ParsedMixinAnnotation> result = findInAnnotations(methodNode.visibleAnnotations);
        if (result.isPresent()) {
            return result;
        }
        return findInAnnotations(methodNode.invisibleAnnotations);
    }

    private Optional<ParsedMixinAnnotation> findInAnnotations(List<AnnotationNode> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return Optional.empty();
        }

        for (AnnotationNode ann : annotations) {
            if (isSupportedMixinAnnotation(ann.desc)) {
                return Optional.of(extractAnnotationData(ann));
            }
        }
        return Optional.empty();
    }

    private boolean isSupportedMixinAnnotation(String desc) {
        return INJECT_DESC.equals(desc)
                || REDIRECT_DESC.equals(desc)
                || MODIFY_VAR_DESC.equals(desc)
                || WRAP_OPERATION_DESC.equals(desc);
    }

    private ParsedMixinAnnotation extractAnnotationData(AnnotationNode ann) {
        String targetMethod = "";
        String targetDesc = "";
        String injectionPointAt = "HEAD";
        String targetOwner = "";
        int ordinal = -1;
        boolean cancellable = false;

        if (ann.values != null) {
            for (int i = 0; i < ann.values.size(); i += 2) {
                String key = (String) ann.values.get(i);
                Object val = ann.values.get(i + 1);

                if ("method".equals(key)) {
                    if (val instanceof List<?> list && !list.isEmpty()) {
                        targetMethod = String.valueOf(list.get(0));
                    } else if (val instanceof String s) {
                        targetMethod = s;
                    }
                } else if ("at".equals(key)) {
                    if (val instanceof AnnotationNode atNode && atNode.values != null) {
                        for (int j = 0; j < atNode.values.size(); j += 2) {
                            String atKey = (String) atNode.values.get(j);
                            Object atVal = atNode.values.get(j + 1);
                            if ("value".equals(atKey) && atVal instanceof String atStr) {
                                injectionPointAt = atStr;
                            } else if ("target".equals(atKey) && atVal instanceof String targetStr) {
                                targetDesc = targetStr;
                            }
                        }
                    }
                } else if ("ordinal".equals(key) && val instanceof Number n) {
                    ordinal = n.intValue();
                } else if ("cancellable".equals(key) && val instanceof Boolean b) {
                    cancellable = b;
                }
            }
        }

        // Clean target method name and descriptor if combined like "tick()V"
        int parenIndex = targetMethod.indexOf('(');
        if (parenIndex != -1) {
            targetDesc = targetMethod.substring(parenIndex);
            targetMethod = targetMethod.substring(0, parenIndex);
        }

        return new ParsedMixinAnnotation(
                ann.desc,
                targetMethod,
                targetDesc,
                injectionPointAt,
                targetOwner,
                ordinal,
                cancellable
        );
    }
}
