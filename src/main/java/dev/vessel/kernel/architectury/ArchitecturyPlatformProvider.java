package dev.vessel.kernel.architectury;

import java.nio.file.Path;

/**
 * Service provider interface supplying platform environment details for Architectury API hooks.
 *
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface ArchitecturyPlatformProvider {

    /**
     * Retrieves the active runtime platform kind.
     *
     * @return platform kind
     */
    ArchitecturyPlatformKind getPlatformKind();

    /**
     * Checks if a mod with the specified ID is currently loaded.
     *
     * @param modId the mod identifier
     * @return true if loaded, false otherwise
     */
    boolean isModLoaded(String modId);

    /**
     * Retrieves the configuration directory path.
     *
     * @return path to config folder
     */
    Path getConfigDirectory();

    /**
     * Retrieves the root game directory path.
     *
     * @return path to game folder
     */
    Path getGameDirectory();
}
