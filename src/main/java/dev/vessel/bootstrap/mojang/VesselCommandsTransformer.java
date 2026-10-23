package dev.vessel.bootstrap.mojang;

import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Bytecode transformer that instruments {@code net.minecraft.commands.Commands} constructor to
 * register the active dispatcher holder into {@code VesselServerBridgeRegistry}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class VesselCommandsTransformer {

    public static final String TARGET_INTERNAL_NAME = "net/minecraft/commands/Commands";
    public static final String TARGET_CLASS_NAME = "net.minecraft.commands.Commands";

    public VesselCommandsTransformer() {
    }

    /**
     * Checks if the given class name matches any Minecraft commands dispatcher or manager class across eras.
     *
     * @param className class name with dots or slashes
     * @return {@code true} if this class represents a commands dispatcher or manager
     */
    public static boolean isCommandsClass(String className) {
        if (className == null) {
            return false;
        }
        String norm = className.replace('/', '.');
        return "net.minecraft.commands.Commands".equals(norm)
                || "net.minecraft.command.Commands".equals(norm)
                || "net.minecraft.command.ServerCommandManager".equals(norm)
                || "net.minecraft.command.CommandHandler".equals(norm);
    }

    /**
     * Transforms the given bytecode of {@code net.minecraft.commands.Commands}.
     *
     * @param originalBytes raw class bytecode
     * @return transformed class bytecode
     */
    public byte[] transformCommands(byte[] originalBytes) {
        Objects.requireNonNull(originalBytes, "originalBytes must not be null");
        try {
            ClassReader reader = new ClassReader(originalBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            VesselCommandsClassVisitor visitor = new VesselCommandsClassVisitor(writer);
            reader.accept(visitor, 0);
            return writer.toByteArray();
        } catch (Exception e) {
            return originalBytes;
        }
    }
}
