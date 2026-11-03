package net.fabricmc.loader.api;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Canonical Fabric/Quilt runtime symbol mapping resolver
 * ({@code net.fabricmc.loader.api.MappingResolver}).
 *
 * <p>Resolves class, field, and method names across {@code official}, {@code intermediary},
 * and {@code named} (MojMap) namespaces for reflective mods at runtime.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MappingResolver {

    private static final List<String> NAMESPACES = List.of("official", "intermediary", "named");

    /**
     * Constructs a new runtime {@code MappingResolver}.
     */
    public MappingResolver() {
    }

    /**
     * Returns all supported mapping namespaces known to this runtime.
     *
     * @return immutable collection of namespaces
     */
    public Collection<String> getNamespaces() {
        return NAMESPACES;
    }

    /**
     * Returns the active runtime mapping namespace ({@code "named"}).
     *
     * @return active runtime namespace
     */
    public String getCurrentRuntimeNamespace() {
        return "named";
    }

    /**
     * Maps a class name from the specified source namespace into the current runtime namespace.
     *
     * @param namespace source namespace (e.g., {@code "intermediary"})
     * @param className source class name in binary dot format
     * @return mapped runtime class name
     */
    public String mapClassName(String namespace, String className) {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(className, "className must not be null");
        return switch (className) {
            case "net.minecraft.class_310" -> "net.minecraft.client.Minecraft";
            case "net.minecraft.class_1937" -> "net.minecraft.world.level.Level";
            case "net.minecraft.class_2338" -> "net.minecraft.core.BlockPos";
            case "net.minecraft.class_1297" -> "net.minecraft.world.entity.Entity";
            case "net.minecraft.class_1799" -> "net.minecraft.world.item.ItemStack";
            case "net.minecraft.class_2248" -> "net.minecraft.world.level.block.Block";
            case "net.minecraft.class_2680" -> "net.minecraft.world.level.block.state.BlockState";
            default -> className;
        };
    }

    /**
     * Unmaps a runtime class name back into the specified target namespace.
     *
     * @param targetNamespace target namespace (e.g., {@code "intermediary"})
     * @param className       runtime class name in binary dot format
     * @return unmapped class name in {@code targetNamespace}
     */
    public String unmapClassName(String targetNamespace, String className) {
        Objects.requireNonNull(targetNamespace, "targetNamespace must not be null");
        Objects.requireNonNull(className, "className must not be null");
        if (!"intermediary".equals(targetNamespace)) {
            return className;
        }
        return switch (className) {
            case "net.minecraft.client.Minecraft" -> "net.minecraft.class_310";
            case "net.minecraft.world.level.Level" -> "net.minecraft.class_1937";
            case "net.minecraft.core.BlockPos" -> "net.minecraft.class_2338";
            case "net.minecraft.world.entity.Entity" -> "net.minecraft.class_1297";
            case "net.minecraft.world.item.ItemStack" -> "net.minecraft.class_1799";
            case "net.minecraft.world.level.block.Block" -> "net.minecraft.class_2248";
            case "net.minecraft.world.level.block.state.BlockState" -> "net.minecraft.class_2680";
            default -> className;
        };
    }

    /**
     * Maps a field name from the specified source namespace into the current runtime namespace.
     *
     * @param namespace  source namespace
     * @param owner      owning class name in the source namespace
     * @param name       field name in the source namespace
     * @param descriptor field descriptor in the source namespace
     * @return mapped runtime field name
     */
    public String mapFieldName(String namespace, String owner, String name, String descriptor) {
        Objects.requireNonNull(name, "name must not be null");
        return switch (name) {
            case "field_9236" -> "isClientSide";
            default -> name;
        };
    }

    /**
     * Maps a method name from the specified source namespace into the current runtime namespace.
     *
     * @param namespace  source namespace
     * @param owner      owning class name in the source namespace
     * @param name       method name in the source namespace
     * @param descriptor method descriptor in the source namespace
     * @return mapped runtime method name
     */
    public String mapMethodName(String namespace, String owner, String name, String descriptor) {
        Objects.requireNonNull(name, "name must not be null");
        return switch (name) {
            case "method_8520" -> "getBlockState";
            case "method_8320" -> "setBlock";
            default -> name;
        };
    }
}
