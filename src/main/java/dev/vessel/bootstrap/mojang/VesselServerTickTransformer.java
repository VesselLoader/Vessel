package dev.vessel.bootstrap.mojang;

import dev.vessel.kernel.compat.bytecode.AsmClassReaderBytecodeSanitizer;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Bytecode transformer instrumenting {@code net.minecraft.server.MinecraftServer.tickServer}
 * to link runtime server ticks directly to {@link VesselServerTickClassVisitor}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselServerTickTransformer {

    public static final String TARGET_INTERNAL_NAME = "net/minecraft/server/MinecraftServer";
    public static final String TARGET_CLASS_NAME = "net.minecraft.server.MinecraftServer";

    private final AsmClassReaderBytecodeSanitizer sanitizer = new AsmClassReaderBytecodeSanitizer();

    public VesselServerTickTransformer() {
    }

    /**
     * Determines whether the specified class name targets MinecraftServer.
     *
     * @param className binary or internal class name
     * @return {@code true} if matching
     */
    public static boolean isServerClass(String className) {
        if (className == null) {
            return false;
        }
        return TARGET_CLASS_NAME.equals(className)
                || TARGET_INTERNAL_NAME.equals(className)
                || className.endsWith("server/MinecraftServer")
                || className.endsWith("server.MinecraftServer");
    }

    /**
     * Transforms MinecraftServer bytecode to instrument the tick loop.
     *
     * @param originalBytes raw class bytecode
     * @return transformed bytecode
     */
    public byte[] transformServerTick(byte[] originalBytes) {
        Objects.requireNonNull(originalBytes, "originalBytes must not be null");
        try {
            int originalMajor = sanitizer.extractMajorVersion(originalBytes);
            ClassReader reader = sanitizer.createSafeClassReader(originalBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            VesselServerTickClassVisitor visitor = new VesselServerTickClassVisitor(writer);
            reader.accept(visitor, 0);
            byte[] transformed = writer.toByteArray();
            return sanitizer.restoreOriginalMajorVersion(transformed, originalMajor);
        } catch (Exception e) {
            return originalBytes;
        }
    }
}
