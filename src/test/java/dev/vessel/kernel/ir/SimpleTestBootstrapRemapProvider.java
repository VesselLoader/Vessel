package dev.vessel.kernel.ir;

import java.util.HashMap;
import java.util.Map;

/**
 * Test implementation of {@link BootstrapRemapProvider} for unit testing dynamic call site linking.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SimpleTestBootstrapRemapProvider implements BootstrapRemapProvider {

    private final Map<String, String> classMappings = new HashMap<>();
    private final Map<String, String> methodMappings = new HashMap<>();
    private final Map<String, String> descriptorMappings = new HashMap<>();

    public void addClassMapping(String from, String to) {
        classMappings.put(from, to);
    }

    public void addMethodMapping(String from, String to) {
        methodMappings.put(from, to);
    }

    public void addDescriptorMapping(String from, String to) {
        descriptorMappings.put(from, to);
    }

    @Override
    public String mapClassName(String internalName) {
        return classMappings.getOrDefault(internalName, internalName);
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        return methodMappings.getOrDefault(name, name);
    }

    @Override
    public String mapMethodDescriptor(String descriptor) {
        return descriptorMappings.getOrDefault(descriptor, descriptor);
    }
}
