package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ClassVisitor stripping ACC_FINAL from {@code net.minecraft.world.item.crafting.Ingredient}
 * to allow cross-era subclassing by legacy mods (e.g., Registrate DataIngredient).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselIngredientClassVisitor extends ClassVisitor {

    public VesselIngredientClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        int nonFinalAccess = access & ~Opcodes.ACC_FINAL;
        super.visit(version, nonFinalAccess, name, signature, superName, interfaces);
    }
}
