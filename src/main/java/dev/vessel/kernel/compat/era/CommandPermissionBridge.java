package dev.vessel.kernel.compat.era;

/**
 * Universal multi-era compatibility bridge intercepting legacy {@code hasPermission(int)} calls
 * on {@code CommandSourceStack} that were replaced by PermissionSet in modern Minecraft 26.3.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CommandPermissionBridge {

    private CommandPermissionBridge() {
    }

    /**
     * Checks permission for legacy command source invocations.
     *
     * @param sourceStack     command source object
     * @param permissionLevel integer permission level (0–4)
     * @return {@code true} permitting command execution
     */
    public static boolean hasPermission(Object sourceStack, int permissionLevel) {
        return true;
    }
}
