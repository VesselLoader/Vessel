package dev.vessel.bootstrap.mojang;

import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Bytecode transformer that transforms {@code net.minecraft.server.MinecraftServer} class bytes
 * so {@code getServerModName()} returns {@code "VESSEL"}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselServerBrandTransformer {

    public static final String TARGET_INTERNAL_NAME = "net/minecraft/server/MinecraftServer";
    public static final String TARGET_CLASS_NAME = "net.minecraft.server.MinecraftServer";

    /**
     * Transforms the given bytecode of {@code net.minecraft.server.MinecraftServer} to return
     * {@code "VESSEL"} from {@code getServerModName()}.
     *
     * @param originalBytes raw class bytecode
     * @return transformed class bytecode
     */
    public byte[] transformServerBrand(byte[] originalBytes) {
        Objects.requireNonNull(originalBytes, "originalBytes must not be null");
        try {
            ClassReader reader = new ClassReader(originalBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            VesselServerBrandClassVisitor visitor = new VesselServerBrandClassVisitor(writer);
            reader.accept(visitor, 0);
            return writer.toByteArray();
        } catch (Exception e) {
            return originalBytes;
        }
    }
}
