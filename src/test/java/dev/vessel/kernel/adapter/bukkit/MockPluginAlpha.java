package dev.vessel.kernel.adapter.bukkit;

/**
 * Sample mock plugin class representing an export from Plugin Alpha.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MockPluginAlpha {

    public static final String IDENTIFIER = "ALPHA_SERVICES";

    public MockPluginAlpha() {
    }

    public String getService() {
        return IDENTIFIER;
    }
}
