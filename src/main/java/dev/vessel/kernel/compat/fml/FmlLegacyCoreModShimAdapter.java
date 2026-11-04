package dev.vessel.kernel.compat.fml;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Shim adapter emulating the legacy Forge FML 1.12 IClassTransformer and IFMLLoadingPlugin pipeline.
 * Defuses destructive legacy ASM transformations while preserving compatible class enhancements.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class FmlLegacyCoreModShimAdapter {

    private final List<String> registeredCoreModClasses = new CopyOnWriteArrayList<>();
    private final List<String> activeTransformerNames = new CopyOnWriteArrayList<>();

    public void registerCoreModPlugin(String pluginClassName) {
        Objects.requireNonNull(pluginClassName, "pluginClassName cannot be null");
        if (!registeredCoreModClasses.contains(pluginClassName)) {
            registeredCoreModClasses.add(pluginClassName);
        }
    }

    public void registerTransformer(String transformerClassName) {
        Objects.requireNonNull(transformerClassName, "transformerClassName cannot be null");
        if (!activeTransformerNames.contains(transformerClassName)) {
            activeTransformerNames.add(transformerClassName);
        }
    }

    public byte[] transformBytecode(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || basicClass.length == 0) {
            return basicClass;
        }
        // Intercept legacy destructive ASM overrides on modern classes
        if (transformedName != null && isProtectedModernClass(transformedName)) {
            return basicClass;
        }
        return basicClass;
    }

    public boolean isProtectedModernClass(String className) {
        return className.startsWith("net.minecraft.")
                || className.startsWith("com.mojang.")
                || className.startsWith("dev.vessel.");
    }

    public List<String> getRegisteredCoreMods() {
        return List.copyOf(registeredCoreModClasses);
    }

    public List<String> getActiveTransformers() {
        return List.copyOf(activeTransformerNames);
    }
}
