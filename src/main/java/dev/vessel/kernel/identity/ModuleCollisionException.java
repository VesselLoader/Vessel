package dev.vessel.kernel.identity;

import java.util.Objects;

/**
 * Domain exception thrown when two modules share the same namespace within the same loader ecosystem.
 * <p>
 * Under Decision 8, this collision triggers an immediate Hard Stop on server startup,
 * identifying both the conflicting identifiers and their originating JAR paths.
 * </p>
 *
 * @version 1.0.2
 * @since 1.0.0 (Pass K-4)
 */
public class ModuleCollisionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final VesselModuleIdentifier existingModule;
    private final VesselModuleIdentifier collidingModule;
    private final String existingJarPath;
    private final String collidingJarPath;

    public ModuleCollisionException(
            VesselModuleIdentifier existingModule,
            VesselModuleIdentifier collidingModule,
            String existingJarPath,
            String collidingJarPath
    ) {
        super(formatMessage(existingModule, collidingModule, existingJarPath, collidingJarPath));
        this.existingModule = Objects.requireNonNull(existingModule, "existingModule cannot be null");
        this.collidingModule = Objects.requireNonNull(collidingModule, "collidingModule cannot be null");
        this.existingJarPath = Objects.requireNonNull(existingJarPath, "existingJarPath cannot be null");
        this.collidingJarPath = Objects.requireNonNull(collidingJarPath, "collidingJarPath cannot be null");
    }

    private static String formatMessage(
            VesselModuleIdentifier existingModule,
            VesselModuleIdentifier collidingModule,
            String existingJarPath,
            String collidingJarPath
    ) {
        return String.format(
                "Module namespace collision detected (Decision 8 Hard Stop): " +
                "Loader [%s], Namespace [%s] is already claimed by JAR [%s] (version %s), " +
                "but conflicting JAR [%s] declares version %s.",
                existingModule.loader(),
                existingModule.namespace(),
                existingJarPath,
                existingModule.version(),
                collidingJarPath,
                collidingModule.version()
        );
    }

    public VesselModuleIdentifier getExistingModule() {
        return existingModule;
    }

    public VesselModuleIdentifier getCollidingModule() {
        return collidingModule;
    }

    public String getExistingJarPath() {
        return existingJarPath;
    }

    public String getCollidingJarPath() {
        return collidingJarPath;
    }
}
