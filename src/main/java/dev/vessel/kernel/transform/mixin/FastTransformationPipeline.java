package dev.vessel.kernel.transform.mixin;

import dev.vessel.kernel.ir.BytecodeToIRParser;
import dev.vessel.kernel.ir.VesselAccessWidenerApplier;
import dev.vessel.kernel.ir.VesselAccessWideningTable;
import dev.vessel.kernel.ir.VesselSemanticHookRecord;
import dev.vessel.kernel.runtime.InterfaceInjectionRegistry;
import dev.vessel.kernel.runtime.SyntheticInterfaceBytecodeApplier;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-throughput unified bytecode transformation pipeline operating at classloading time.
 * Coordinates Access Widening, Synthetic Interface Injection, and Semantic Mixin Transformation
 * in a single, cache-friendly ASM pass.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FastTransformationPipeline {

    private final BytecodeToIRParser irParser;
    private final VesselAccessWideningTable wideningTable;
    private final VesselAccessWidenerApplier widenerApplier;
    private final InterfaceInjectionRegistry interfaceRegistry;
    private final SyntheticInterfaceBytecodeApplier interfaceApplier;
    private final SemanticHookRegistry hookRegistry;
    private final SemanticBytecodeTransformer hookTransformer;

    private final AtomicInteger transformedClassCount = new AtomicInteger(0);
    private final AtomicLong totalTransformNanos = new AtomicLong(0L);

    /**
     * Constructs a unified FastTransformationPipeline with all subsystem workers.
     */
    public FastTransformationPipeline(
            BytecodeToIRParser irParser,
            VesselAccessWideningTable wideningTable,
            VesselAccessWidenerApplier widenerApplier,
            InterfaceInjectionRegistry interfaceRegistry,
            SyntheticInterfaceBytecodeApplier interfaceApplier,
            SemanticHookRegistry hookRegistry,
            SemanticBytecodeTransformer hookTransformer) {
        this.irParser = Objects.requireNonNull(irParser, "irParser cannot be null");
        this.wideningTable = Objects.requireNonNull(wideningTable, "wideningTable cannot be null");
        this.widenerApplier = Objects.requireNonNull(widenerApplier, "widenerApplier cannot be null");
        this.interfaceRegistry = Objects.requireNonNull(interfaceRegistry, "interfaceRegistry cannot be null");
        this.interfaceApplier = Objects.requireNonNull(interfaceApplier, "interfaceApplier cannot be null");
        this.hookRegistry = Objects.requireNonNull(hookRegistry, "hookRegistry cannot be null");
        this.hookTransformer = Objects.requireNonNull(hookTransformer, "hookTransformer cannot be null");
    }

    /**
     * Processes class bytecode through the unified transformation pipeline.
     * Bypasses parsing completely if the target class requires no transformations.
     *
     * @param className internal or binary class name (e.g. "net.minecraft.world.entity.Entity")
     * @param originalBytes original class bytecode
     * @return transformed bytecode, or original array if unmodified
     */
    public byte[] transformBytecode(String className, byte[] originalBytes) {
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(originalBytes, "originalBytes cannot be null");

        String normalizedName = className.replace('.', '/');

        boolean needsWidening = wideningTable.hasWidening(normalizedName);
        boolean needsInterfaces = interfaceRegistry.hasInjections(normalizedName);
        boolean needsHooks = hookRegistry.hasHooksFor(normalizedName);

        // Fast-path bypass: Zero parsing overhead when no modifications are required
        if (!needsWidening && !needsInterfaces && !needsHooks) {
            return originalBytes;
        }

        long startNanos = System.nanoTime();

        ClassNode classNode = irParser.parse(originalBytes);
        boolean modified = false;

        // 1. Access Widening
        if (needsWidening) {
            widenerApplier.apply(classNode, wideningTable);
            modified = true;
        }

        // 2. Synthetic Interface Injection
        if (needsInterfaces) {
            Set<String> interfaces = interfaceRegistry.getInjectedInterfaces(normalizedName);
            int added = interfaceApplier.apply(classNode, interfaces);
            if (added > 0) {
                modified = true;
            }
        }

        // 3. Semantic Hook Transformation
        if (needsHooks) {
            List<VesselSemanticHookRecord> hooks = hookRegistry.getHooksFor(normalizedName);
            int applied = hookTransformer.transform(classNode, hooks);
            if (applied > 0) {
                modified = true;
            }
        }

        if (!modified) {
            return originalBytes;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        byte[] transformed = writer.toByteArray();

        long duration = System.nanoTime() - startNanos;
        totalTransformNanos.addAndGet(duration);
        transformedClassCount.incrementAndGet();

        return transformed;
    }

    public int getTransformedClassCount() {
        return transformedClassCount.get();
    }

    public long getTotalTransformNanos() {
        return totalTransformNanos.get();
    }
}
