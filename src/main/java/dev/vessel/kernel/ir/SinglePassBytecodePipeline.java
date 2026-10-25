package dev.vessel.kernel.ir;

import dev.vessel.kernel.runtime.InterfaceInjectionRegistry;
import dev.vessel.kernel.runtime.SyntheticInterfaceBytecodeApplier;
import dev.vessel.kernel.transform.mixin.SemanticBytecodeTransformer;
import dev.vessel.kernel.transform.mixin.SemanticHookRegistry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance single-pass bytecode transformation pipeline.
 * <p>
 * Parses raw JVM bytecode once into memory, applies access widening, synthetic interface injection,
 * semantic mixin interception hooks, generic signature reconciliation, and covariant bridge deduplication
 * before writing out the resulting bytecode in a single pass without redundant intermediate re-parsing.
 * </p>
 * <p>
 * Directly resolves ERR-06 and MAP-001..MAP-010.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SinglePassBytecodePipeline {

    private final BytecodeToIRParser parser;
    private final VesselAccessWideningTable wideningTable;
    private final VesselAccessWidenerApplier widenerApplier;
    private final InterfaceInjectionRegistry interfaceRegistry;
    private final SyntheticInterfaceBytecodeApplier interfaceApplier;
    private final SemanticHookRegistry hookRegistry;
    private final SemanticBytecodeTransformer hookTransformer;
    private final GenericSignatureTypeErasureReconciler signatureReconciler;
    private final CovariantBridgeMethodDeduplicator bridgeDeduplicator;
    private final SinglePassPipelineConfig config;

    private final AtomicInteger transformedClassCount = new AtomicInteger(0);
    private final AtomicLong totalExecutionNanos = new AtomicLong(0L);

    /**
     * Constructs a full single-pass pipeline with all subsystem collaborators and default config.
     */
    public SinglePassBytecodePipeline(
            BytecodeToIRParser parser,
            VesselAccessWideningTable wideningTable,
            VesselAccessWidenerApplier widenerApplier,
            InterfaceInjectionRegistry interfaceRegistry,
            SyntheticInterfaceBytecodeApplier interfaceApplier,
            SemanticHookRegistry hookRegistry,
            SemanticBytecodeTransformer hookTransformer,
            GenericSignatureTypeErasureReconciler signatureReconciler,
            CovariantBridgeMethodDeduplicator bridgeDeduplicator) {
        this(parser, wideningTable, widenerApplier, interfaceRegistry, interfaceApplier,
                hookRegistry, hookTransformer, signatureReconciler, bridgeDeduplicator,
                SinglePassPipelineConfig.DEFAULT);
    }

    /**
     * Constructs a full single-pass pipeline with explicit configuration.
     */
    public SinglePassBytecodePipeline(
            BytecodeToIRParser parser,
            VesselAccessWideningTable wideningTable,
            VesselAccessWidenerApplier widenerApplier,
            InterfaceInjectionRegistry interfaceRegistry,
            SyntheticInterfaceBytecodeApplier interfaceApplier,
            SemanticHookRegistry hookRegistry,
            SemanticBytecodeTransformer hookTransformer,
            GenericSignatureTypeErasureReconciler signatureReconciler,
            CovariantBridgeMethodDeduplicator bridgeDeduplicator,
            SinglePassPipelineConfig config) {
        this.parser = Objects.requireNonNull(parser, "parser cannot be null");
        this.wideningTable = Objects.requireNonNull(wideningTable, "wideningTable cannot be null");
        this.widenerApplier = Objects.requireNonNull(widenerApplier, "widenerApplier cannot be null");
        this.interfaceRegistry = Objects.requireNonNull(interfaceRegistry, "interfaceRegistry cannot be null");
        this.interfaceApplier = Objects.requireNonNull(interfaceApplier, "interfaceApplier cannot be null");
        this.hookRegistry = Objects.requireNonNull(hookRegistry, "hookRegistry cannot be null");
        this.hookTransformer = Objects.requireNonNull(hookTransformer, "hookTransformer cannot be null");
        this.signatureReconciler = Objects.requireNonNull(signatureReconciler, "signatureReconciler cannot be null");
        this.bridgeDeduplicator = Objects.requireNonNull(bridgeDeduplicator, "bridgeDeduplicator cannot be null");
        this.config = Objects.requireNonNull(config, "config cannot be null");
    }

    /**
     * Transforms class bytecode in a single pass.
     *
     * @param className internal or fully-qualified name of the class
     * @param originalBytes original class bytecode
     * @return transformed bytecode or original array if unmanipulated
     */
    public byte[] transform(String className, byte[] originalBytes) {
        return process(className, originalBytes).bytecode();
    }

    /**
     * Executes the pipeline with a detailed execution report.
     *
     * @param className internal or fully-qualified name of the class
     * @param originalBytes original class bytecode
     * @return result report containing bytecode and modification statistics
     */
    public SinglePassPipelineResult process(String className, byte[] originalBytes) {
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(originalBytes, "originalBytes cannot be null");

        String normalizedName = className.replace('.', '/');
        boolean needsWidening = wideningTable.hasWidening(normalizedName);
        boolean needsInterfaces = interfaceRegistry.hasInjections(normalizedName);
        boolean needsHooks = hookRegistry.hasHooksFor(normalizedName);

        // Fast-path bypass when class has no active widening, interfaces, or semantic hooks
        if (!needsWidening && !needsInterfaces && !needsHooks) {
            return new SinglePassPipelineResult(originalBytes, false, 0, 0L);
        }

        long startNanos = System.nanoTime();
        int modificationsCount = 0;

        try {
            ClassNode classNode = parser.parse(originalBytes);

            // 1. Access Widening pass
            if (needsWidening) {
                widenerApplier.apply(classNode, wideningTable);
                modificationsCount++;
            }

            // 2. Synthetic Interface Injection pass
            if (needsInterfaces) {
                Set<String> interfaces = interfaceRegistry.getInjectedInterfaces(normalizedName);
                int added = interfaceApplier.apply(classNode, interfaces);
                if (added > 0) {
                    modificationsCount += added;
                }
            }

            // 3. Semantic Hook Transformation pass
            if (needsHooks) {
                List<VesselSemanticHookRecord> hooks = hookRegistry.getHooksFor(normalizedName);
                int hooksApplied = hookTransformer.transform(classNode, hooks);
                if (hooksApplied > 0) {
                    modificationsCount += hooksApplied;
                }
            }

            // 4. Generic Signature Reconcile pass
            if (config.reconcileSignatures()) {
                SignatureReconcileResult reconcileResult = signatureReconciler.reconcile(classNode);
                if (reconcileResult.modifiedSignaturesCount() > 0) {
                    modificationsCount += reconcileResult.modifiedSignaturesCount();
                }
            }

            // 5. Covariant Bridge Method Deduplication pass
            if (config.deduplicateBridges()) {
                BridgeDeduplicationReport bridgeReport = bridgeDeduplicator.deduplicate(classNode);
                if (bridgeReport.removedBridgeCount() > 0) {
                    modificationsCount += bridgeReport.removedBridgeCount();
                }
            }

            if (modificationsCount == 0) {
                long duration = System.nanoTime() - startNanos;
                return new SinglePassPipelineResult(originalBytes, false, 0, duration);
            }

            int writerFlags = 0;
            if (config.computeFrames()) {
                writerFlags |= ClassWriter.COMPUTE_FRAMES;
            } else if (config.computeMaxs()) {
                writerFlags |= ClassWriter.COMPUTE_MAXS;
            }

            ClassWriter writer = new ClassWriter(writerFlags);
            classNode.accept(writer);
            byte[] transformedBytes = writer.toByteArray();

            long duration = System.nanoTime() - startNanos;
            transformedClassCount.incrementAndGet();
            totalExecutionNanos.addAndGet(duration);

            return new SinglePassPipelineResult(transformedBytes, true, modificationsCount, duration);
        } catch (Exception e) {
            throw new SinglePassPipelineException("Failed to execute single-pass pipeline for " + className, e);
        }
    }

    public int getTransformedClassCount() {
        return transformedClassCount.get();
    }

    public long getTotalExecutionNanos() {
        return totalExecutionNanos.get();
    }

    public SinglePassPipelineConfig getConfig() {
        return config;
    }
}
