package net.minecraftforge.server.permission.nodes;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Concrete Forge permission node representation ({@code net.minecraftforge.server.permission.nodes.PermissionNode}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <T> permission value type
 */
public class PermissionNode<T> {

    private final String modId;
    private final String nodeName;
    private final PermissionType<T> type;
    private final PermissionNode$PermissionResolver<T> defaultResolver;

    public PermissionNode(
            String modId,
            String nodeName,
            PermissionType<T> type,
            PermissionNode$PermissionResolver<T> defaultResolver,
            PermissionDynamicContextKey<?>... dynamics) {
        this.modId = Objects.requireNonNull(modId, "modId must not be null");
        this.nodeName = Objects.requireNonNull(nodeName, "nodeName must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.defaultResolver = defaultResolver;
    }

    public PermissionNode(
            ResourceLocation nodeLocation,
            PermissionType<T> type,
            PermissionNode$PermissionResolver<T> defaultResolver,
            PermissionDynamicContextKey<?>... dynamics) {
        this(
                Objects.requireNonNull(nodeLocation, "nodeLocation must not be null").getNamespace(),
                nodeLocation.getPath(),
                type,
                defaultResolver,
                dynamics);
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getModId() {
        return modId;
    }

    public PermissionType<T> getType() {
        return type;
    }

    public PermissionNode$PermissionResolver<T> getDefaultResolver() {
        return defaultResolver;
    }
}
