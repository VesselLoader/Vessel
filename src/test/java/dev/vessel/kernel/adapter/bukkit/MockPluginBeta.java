package dev.vessel.kernel.adapter.bukkit;

/**
 * Sample mock plugin class representing an export from Plugin Beta.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MockPluginBeta {

    public static final String IDENTIFIER = "BETA_SERVICES";

    public MockPluginBeta() {
    }

    public String getService() {
        return IDENTIFIER;
    }
}
