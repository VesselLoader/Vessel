package dev.vessel.bootstrap.mojang;

import dev.vessel.kernel.compat.bytecode.AsmClassReaderBytecodeSanitizer;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Transforms {@code net.minecraft.world.item.crafting.Ingredient} bytecode to strip {@code ACC_FINAL},
 * enabling legacy Forge 1.20.1 mods like Create (Registrate DataIngredient) to subclass it safely on Minecraft 26.3.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselIngredientTransformer {

    public static final String TARGET_INTERNAL_NAME = "net/minecraft/world/item/crafting/Ingredient";
    public static final String TARGET_CLASS_NAME = "net.minecraft.world.item.crafting.Ingredient";

    private final AsmClassReaderBytecodeSanitizer sanitizer = new AsmClassReaderBytecodeSanitizer();

    public VesselIngredientTransformer() {
    }

    /**
     * Checks if the given class name matches {@code net.minecraft.world.item.crafting.Ingredient}.
     *
     * @param className class name with dots or slashes
     * @return {@code true} if matching
     */
    public static boolean isIngredientClass(String className) {
        if (className == null) {
            return false;
        }
        String norm = className.replace('/', '.');
        return TARGET_CLASS_NAME.equals(norm);
    }

    /**
     * Transforms {@code net.minecraft.world.item.crafting.Ingredient} bytecode to strip {@code ACC_FINAL}.
     *
     * @param originalBytes raw class bytecode
     * @return transformed class bytecode
     */
    public byte[] transformIngredient(byte[] originalBytes) {
        Objects.requireNonNull(originalBytes, "originalBytes must not be null");
        try {
            int originalMajor = sanitizer.extractMajorVersion(originalBytes);
            ClassReader reader = sanitizer.createSafeClassReader(originalBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            VesselIngredientClassVisitor visitor = new VesselIngredientClassVisitor(writer);
            reader.accept(visitor, 0);
            byte[] transformed = writer.toByteArray();
            return sanitizer.restoreOriginalMajorVersion(transformed, originalMajor);
        } catch (Exception e) {
            return originalBytes;
        }
    }
}
