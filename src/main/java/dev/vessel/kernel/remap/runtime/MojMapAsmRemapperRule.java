package dev.vessel.kernel.remap.runtime;

import org.objectweb.asm.commons.Remapper;

/**
 * ASM {@link Remapper} rule translating Fabric/Quilt Intermediary ({@code class_*},
 * {@code method_*}, {@code field_*}) and Forge/NeoForge SRG ({@code m_*}, {@code f_*},
 * {@code func_*}, {@code field_*}) descriptors into canonical Mojang Official Mappings (MojMap).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MojMapAsmRemapperRule extends Remapper {

    @Override
    public String map(String internalName) {
        if (internalName == null) {
            return null;
        }
        return switch (internalName) {
            case "net/minecraft/class_310" -> "net/minecraft/client/Minecraft";
            case "net/minecraft/class_1937" -> "net/minecraft/world/level/Level";
            case "net/minecraft/class_2338" -> "net/minecraft/core/BlockPos";
            case "net/minecraft/class_1297" -> "net/minecraft/world/entity/Entity";
            case "net/minecraft/class_1657" -> "net/minecraft/world/entity/player/Player";
            case "net/minecraft/class_3222" -> "net/minecraft/server/level/ServerPlayer";
            case "net/minecraft/class_1799" -> "net/minecraft/world/item/ItemStack";
            case "net/minecraft/class_2248" -> "net/minecraft/world/level/block/Block";
            case "net/minecraft/class_2680" -> "net/minecraft/world/level/block/state/BlockState";
            default -> super.map(internalName);
        };
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        if (name == null) {
            return null;
        }
        return switch (name) {
            case "method_8520", "m_46865_", "func_180495_p" -> "getBlockState";
            case "method_8320", "m_7470_" -> "setBlock";
            case "method_5776", "m_5830_" -> "getName";
            default -> super.mapMethodName(owner, name, descriptor);
        };
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        if (name == null) {
            return null;
        }
        return switch (name) {
            case "field_9236", "f_46443_", "field_72995_K" -> "isClientSide";
            default -> super.mapFieldName(owner, name, descriptor);
        };
    }
}
