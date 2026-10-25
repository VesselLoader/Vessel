package dev.vessel.kernel.ir;

/**
 * Strategy interface providing lookup services for remapping bootstrap method arguments and handles.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface BootstrapRemapProvider {

    /**
     * Remaps an internal class name.
     *
     * @param internalName source class internal name
     * @return remapped class internal name, or original if unchanged
     */
    String mapClassName(String internalName);

    /**
     * Remaps a method name given its owner and descriptor.
     *
     * @param owner owner class internal name
     * @param name method name
     * @param descriptor method descriptor
     * @return remapped method name, or original if unchanged
     */
    String mapMethodName(String owner, String name, String descriptor);

    /**
     * Remaps a method descriptor string.
     *
     * @param descriptor method descriptor
     * @return remapped method descriptor, or original if unchanged
     */
    String mapMethodDescriptor(String descriptor);
}
