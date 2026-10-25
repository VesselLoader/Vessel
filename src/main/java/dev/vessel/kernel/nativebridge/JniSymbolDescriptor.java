package dev.vessel.kernel.nativebridge;

import java.util.Objects;

/**
 * Immutable descriptor for an exported native JNI function symbol.
 *
 * @param symbolName native C/JNI exported symbol name (e.g. Java_org_example_Native_call)
 * @param memoryAddress virtual memory pointer address of the exported function symbol
 * @param librarySource name or path of the originating native shared library
 * @param versionTag semantic version or hash signature of the native library
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record JniSymbolDescriptor(
        String symbolName,
        long memoryAddress,
        String librarySource,
        String versionTag
) {
    public JniSymbolDescriptor {
        Objects.requireNonNull(symbolName, "symbolName cannot be null");
        librarySource = (librarySource != null) ? librarySource : "unknown";
        versionTag = (versionTag != null) ? versionTag : "1.0.0";
    }
}
