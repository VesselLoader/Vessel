package net.minecraftforge.fml.loading;

import java.nio.file.Path;
import net.minecraftforge.api.distmarker.Dist;

/**
 * Compatibility stub for Forge FML loader environment ({@code net.minecraftforge.fml.loading.FMLLoader}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class FMLLoader {

    private FMLLoader() {
    }

    /**
     * Returns whether the server is running in a production environment.
     *
     * @return {@code true}
     */
    public static boolean isProduction() {
        return true;
    }

    /**
     * Returns the physical distribution side of the server.
     *
     * @return {@link Dist#DEDICATED_SERVER}
     */
    public static Dist getDist() {
        return Dist.DEDICATED_SERVER;
    }

    /**
     * Returns the working directory game path.
     *
     * @return current root directory path
     */
    public static Path getGamePath() {
        return Path.of(".");
    }
}
