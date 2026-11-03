package net.md_5.bungee.api.plugin;

import java.util.logging.Logger;

/**
 * BungeeCord base plugin class ({@code net.md_5.bungee.api.plugin.Plugin}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class Plugin {

    private final Logger logger = Logger.getLogger("BungeePlugin");

    public Plugin() {
    }

    public void onLoad() {
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public Logger getLogger() {
        return logger;
    }
}
