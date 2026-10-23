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
 * @version 1.0.0
 */
public final class VesselCommandsTransformer {

    public static final String TARGET_INTERNAL_NAME = "net/minecraft/commands/Commands";
    public static final String TARGET_CLASS_NAME = "net.minecraft.commands.Commands";

    public VesselCommandsTransformer() {
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
