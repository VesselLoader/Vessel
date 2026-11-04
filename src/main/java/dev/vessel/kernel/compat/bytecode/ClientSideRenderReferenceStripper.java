package dev.vessel.kernel.compat.bytecode;

import java.util.Set;

/**
 * Strips client-only rendering references (e.g. RenderType, ItemRenderer, Dist.CLIENT)
 * from server-bound mod bytecode to prevent NoClassDefFoundError on headless dedicated servers.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class ClientSideRenderReferenceStripper {

    private static final Set<String> CLIENT_ONLY_CLASS_PREFIXES = Set.of(
            "net/minecraft/client/",
            "com/mojang/blaze3d/",
            "net/minecraftforge/client/",
            "net/fabricmc/fabric/api/client/",
            "net/neoforged/neoforge/client/"
    );

    public boolean isClientOnlyClass(String internalClassName) {
        if (internalClassName == null) {
            return false;
        }
        for (String prefix : CLIENT_ONLY_CLASS_PREFIXES) {
            if (internalClassName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public boolean isClientOnlyMethodAnnotation(String annotationDescriptor) {
        if (annotationDescriptor == null) {
            return false;
        }
        return annotationDescriptor.contains("OnlyIn")
                || annotationDescriptor.contains("Environment")
                || annotationDescriptor.contains("SideOnly");
    }

    public String sanitizeClassNameForServer(String internalClassName) {
        if (isClientOnlyClass(internalClassName)) {
            return "java/lang/Object";
        }
        return internalClassName;
    }
}
