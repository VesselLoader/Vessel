package net.minecraftforge.api.distmarker;

/**
 * Clean-room enumeration of Minecraft physical distribution environments
 * ({@code net.minecraftforge.api.distmarker.Dist}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum Dist {

    CLIENT,
    DEDICATED_SERVER;

    public boolean isClient() {
        return this == CLIENT;
    }

    public boolean isDedicatedServer() {
        return this == DEDICATED_SERVER;
    }
}
