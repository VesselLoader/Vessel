package dev.vessel.kernel.vstm.remap;

import java.util.Objects;
import org.objectweb.asm.commons.Remapper;

/**
 * Single-responsibility resolver that computes the output archive entry path
 * for classes and resource files during JAR transmutation.
 *
 * <p>Translates {@code .class} paths according to class remapping tables while preserving
 * 100% of non-class resource assets, configurations, and manifests intact (Catalog ref: USML-0021).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 3)
 */
public final class VstmJarEntryNameResolver {

    private static final String CLASS_EXTENSION = ".class";

    /**
     * Constructs a jar entry name resolver.
     */
    public VstmJarEntryNameResolver() {
        // Stateless path resolution helper
    }

    /**
     * Resolves the target archive path for a given entry name.
     *
     * @param originalEntryName original JAR entry path (e.g. "net/minecraft/class_1937.class")
     * @param remapper          active bytecode remapper
     * @return resolved output entry path
     */
    public String resolveEntryName(String originalEntryName, Remapper remapper) {
        Objects.requireNonNull(originalEntryName, "originalEntryName must not be null");
        Objects.requireNonNull(remapper, "remapper must not be null");

        if (originalEntryName.endsWith(CLASS_EXTENSION)) {
            String internalName = originalEntryName.substring(
                    0,
                    originalEntryName.length() - CLASS_EXTENSION.length()
            );
            String mapped = remapper.map(internalName);
            if (mapped != null && !mapped.isEmpty() && !mapped.equals(internalName)) {
                return mapped + CLASS_EXTENSION;
            }
        }

        return originalEntryName;
    }

    /**
     * Checks if a given entry path corresponds to a JVM bytecode class file.
     *
     * @param entryName the archive entry path
     * @return true if the path ends with {@code .class}
     */
    public boolean isClassEntry(String entryName) {
        return entryName != null && entryName.endsWith(CLASS_EXTENSION);
    }
}
