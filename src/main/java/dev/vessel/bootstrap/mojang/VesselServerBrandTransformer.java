package dev.vessel.bootstrap.mojang;

import dev.vessel.kernel.compat.bytecode.AsmClassReaderBytecodeSanitizer;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Bytecode transformer that transforms {@code net.minecraft.server.MinecraftServer} class bytes
 * so {@code getServerModName()} returns {@code "VESSEL"}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.4
 */
public final class VesselServerBrandTransformer {

    public static final String TARGET_INTERNAL_NAME = "net/minecraft/server/MinecraftServer";
    public static final String TARGET_CLASS_NAME = "net.minecraft.server.MinecraftServer";

    private final AsmClassReaderBytecodeSanitizer sanitizer = new AsmClassReaderBytecodeSanitizer();

    public VesselServerBrandTransformer() {
    }

    /**
     * Checks if the given class name matches a MinecraftServer target across eras.
     *
     * @param className class name with dots or slashes
     * @return {@code true} if this class represents a MinecraftServer
     */
    public static boolean isServerClass(String className) {
        if (className == null) {
            return false;
        }
        String norm = className.replace('/', '.');
        return "net.minecraft.server.MinecraftServer".equals(norm)
                || "net.minecraft.server.dedicated.DedicatedServer".equals(norm)
                || (norm.startsWith("net.minecraft.server.")
                        && (norm.endsWith(".MinecraftServer") || norm.endsWith(".DedicatedServer")));
    }

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
            int originalMajor = sanitizer.extractMajorVersion(originalBytes);
            ClassReader reader = sanitizer.createSafeClassReader(originalBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            VesselServerBrandClassVisitor visitor = new VesselServerBrandClassVisitor(writer);
            reader.accept(visitor, 0);
            byte[] transformed = writer.toByteArray();
            return sanitizer.restoreOriginalMajorVersion(transformed, originalMajor);
        } catch (Exception e) {
            return originalBytes;
        }
    }
}
