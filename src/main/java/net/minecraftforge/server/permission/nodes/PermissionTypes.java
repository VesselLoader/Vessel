package net.minecraftforge.server.permission.nodes;

/**
 * Standard factory constants for Forge permission node value types.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PermissionTypes {

    public static final PermissionType<Boolean> BOOLEAN = new BooleanPermissionType();
    public static final PermissionType<Integer> INTEGER = new IntegerPermissionType();
    public static final PermissionType<String> STRING = new StringPermissionType();

    private PermissionTypes() {
    }
}
