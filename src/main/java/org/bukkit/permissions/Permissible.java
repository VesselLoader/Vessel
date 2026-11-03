package org.bukkit.permissions;

/**
 * Contract for any subject capable of holding permissions and operator status
 * ({@code org.bukkit.permissions.Permissible}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface Permissible {

    /**
     * Checks whether an explicit permission assignment exists for the specified permission name.
     *
     * @param name permission node name
     * @return {@code true} if explicitly configured
     */
    default boolean isPermissionSet(String name) {
        return false;
    }

    /**
     * Evaluates whether this subject is granted the specified permission node.
     *
     * @param name permission node name
     * @return {@code true} if granted
     */
    boolean hasPermission(String name);

    /**
     * Evaluates whether this subject is granted the specified {@link Permission}.
     *
     * @param perm permission descriptor
     * @return {@code true} if granted
     */
    default boolean hasPermission(Permission perm) {
        return perm != null
                && (isPermissionSet(perm.getName())
                        ? hasPermission(perm.getName())
                        : perm.getDefault().getValue(isOp()));
    }

    /**
     * Checks whether this subject has server operator status.
     *
     * @return {@code true} if operator
     */
    boolean isOp();

    /**
     * Updates the server operator status of this subject.
     *
     * @param value {@code true} to grant operator status
     */
    default void setOp(boolean value) {
    }
}
