package net.fabricmc.loader.api;

import dev.vessel.adapter.fabric.FabricModContainerShim;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical Fabric/Quilt runtime {@code ModContainer} ({@code net.fabricmc.loader.api.ModContainer})
 * bridged directly to VESSEL's {@link FabricModContainerShim}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ModContainer {

    private final FabricModContainerShim underlyingShim;

    /**
     * Constructs a live Fabric {@code ModContainer} wrapping the specified VESSEL container shim.
     *
     * @param underlyingShim non-null {@link FabricModContainerShim}
     */
    public ModContainer(FabricModContainerShim underlyingShim) {
        this.underlyingShim = Objects.requireNonNull(underlyingShim, "underlyingShim must not be null");
    }

    /**
     * Returns the canonical lowercase mod ID (e.g., {@code "lithium"}, {@code "carpet"}).
     *
     * @return mod ID string
     */
    public String getModId() {
        return underlyingShim.getIdentifier().namespace();
    }

    /**
     * Returns the human-readable mod name or fallback namespace.
     *
     * @return mod display name
     */
    public String getName() {
        String customName = underlyingShim.getOpaqueCustomMetadata().get("name");
        if (customName != null && !customName.isBlank()) {
            return customName;
        }
        return underlyingShim.getIdentifier().namespace();
    }

    /**
     * Returns the semantic version string of the mod.
     *
     * @return version string
     */
    public String getVersion() {
        return underlyingShim.getIdentifier().version();
    }

    /**
     * Returns the physical or virtual filesystem root path of the mod archive.
     *
     * @return root path
     */
    public Path getRootPath() {
        return underlyingShim.getRootPath();
    }

    /**
     * Returns the unmodifiable map of opaque custom metadata entries from {@code fabric.mod.json}.
     *
     * @return custom metadata map
     */
    public Map<String, String> getCustomMetadata() {
        return underlyingShim.getOpaqueCustomMetadata();
    }

    /**
     * Returns the underlying VESSEL {@link FabricModContainerShim}.
     *
     * @return underlying container shim
     */
    public FabricModContainerShim getUnderlyingShim() {
        return underlyingShim;
    }
}
