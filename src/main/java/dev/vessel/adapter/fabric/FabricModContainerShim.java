package dev.vessel.adapter.fabric;

import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Clean-room shim representing a Fabric or Quilt {@code ModContainer}, preserving
 * opaque {@code custom} metadata values per Lesson L24.
 *
 * @version 1.0.0
 */
public final class FabricModContainerShim {

    private final VesselModuleIdentifier identifier;
    private final Map<String, String> opaqueCustomMetadata;
    private final Path rootPath;

    /**
     * Creates a new Fabric/Quilt mod container shim.
     *
     * @param identifier           canonical module identifier
     * @param opaqueCustomMetadata custom metadata key-value map from {@code fabric.mod.json}
     * @param rootPath             root path of the mod archive or directory
     */
    public FabricModContainerShim(
            VesselModuleIdentifier identifier,
            Map<String, String> opaqueCustomMetadata,
            Path rootPath) {
        this.identifier = Objects.requireNonNull(identifier, "identifier must not be null");
        Objects.requireNonNull(opaqueCustomMetadata, "opaqueCustomMetadata must not be null");
        this.opaqueCustomMetadata = Collections.unmodifiableMap(new LinkedHashMap<>(opaqueCustomMetadata));
        this.rootPath = Objects.requireNonNull(rootPath, "rootPath must not be null");
    }

    /**
     * Returns the canonical VESSEL module identifier for this container.
     *
     * @return the module identifier
     */
    public VesselModuleIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * Returns the immutable map of opaque custom metadata entries preserved from the
     * mod descriptor (Lesson L24).
     *
     * @return unmodifiable map of custom metadata entries
     */
    public Map<String, String> getOpaqueCustomMetadata() {
        return opaqueCustomMetadata;
    }

    /**
     * Returns the root filesystem or VFS path of the mod container.
     *
     * @return root path
     */
    public Path getRootPath() {
        return rootPath;
    }
}
