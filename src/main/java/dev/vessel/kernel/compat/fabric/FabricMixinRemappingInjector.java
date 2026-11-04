package dev.vessel.kernel.compat.fabric;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Dynamically remaps Fabric Mixin target classes and method injection points
 * from Intermediary names into MojMap / modern server runtime names.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class FabricMixinRemappingInjector {

    private final ConcurrentMap<String, String> intermediaryToMojMapClasses = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> intermediaryToMojMapMethods = new ConcurrentHashMap<>();

    public void registerClassRemap(String intermediaryClassName, String mojMapClassName) {
        Objects.requireNonNull(intermediaryClassName, "intermediaryClassName cannot be null");
        Objects.requireNonNull(mojMapClassName, "mojMapClassName cannot be null");
        intermediaryToMojMapClasses.put(intermediaryClassName, mojMapClassName);
    }

    public void registerMethodRemap(String intermediaryMethodName, String mojMapMethodName) {
        Objects.requireNonNull(intermediaryMethodName, "intermediaryMethodName cannot be null");
        Objects.requireNonNull(mojMapMethodName, "mojMapMethodName cannot be null");
        intermediaryToMojMapMethods.put(intermediaryMethodName, mojMapMethodName);
    }

    public String remapTargetClass(String targetClassName) {
        if (targetClassName == null) {
            return null;
        }
        return intermediaryToMojMapClasses.getOrDefault(targetClassName, targetClassName);
    }

    public String remapTargetMethod(String targetMethodName) {
        if (targetMethodName == null) {
            return null;
        }
        return intermediaryToMojMapMethods.getOrDefault(targetMethodName, targetMethodName);
    }

    public boolean hasMappingForClass(String intermediaryClassName) {
        return intermediaryClassName != null && intermediaryToMojMapClasses.containsKey(intermediaryClassName);
    }
}
