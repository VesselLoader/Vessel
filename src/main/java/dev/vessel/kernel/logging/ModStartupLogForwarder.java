package dev.vessel.kernel.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Ensures that mod and plugin startup logs (banners, ASCII headers, and initialization notices)
 * are forwarded directly to the main server console rather than being swallowed or routed to hidden log files.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ModStartupLogForwarder {

    public ModStartupLogForwarder() {
    }

    /**
     * Initializes global logging forwarders for JUL (java.util.logging) and Log4j2 properties.
     */
    public void initializeGlobalLogForwarding() {
        try {
            System.setProperty("log4j2.level", "INFO");
            System.setProperty("log4j.configurationFile", "log4j2.xml");
            System.setProperty("terminal.jline", "true");
            System.setProperty("terminal.ansi", "true");

            Logger rootLogger = LogManager.getLogManager().getLogger("");
            if (rootLogger != null) {
                rootLogger.setLevel(Level.INFO);
                boolean hasConsole = false;
                for (Handler handler : rootLogger.getHandlers()) {
                    if (handler instanceof ConsoleHandler) {
                        handler.setLevel(Level.INFO);
                        hasConsole = true;
                    }
                }
                if (!hasConsole) {
                    ConsoleHandler consoleHandler = new ConsoleHandler();
                    consoleHandler.setLevel(Level.INFO);
                    rootLogger.addHandler(consoleHandler);
                }
            }
        } catch (Throwable ignored) {
            // Best effort initialization
        }
    }
}
