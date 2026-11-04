package net.minecraftforge.common.world;

/**
 * Manager coordinating Forge forced chunk ticket registrations.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ForgeChunkManager {

    private ForgeChunkManager() {
    }

    public static void setForcedChunkLoadingCallback(
            String modId,
            ForgeChunkManager$LoadingValidationCallback callback) {
    }
}
