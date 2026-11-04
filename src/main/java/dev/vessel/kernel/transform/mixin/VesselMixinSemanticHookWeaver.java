package dev.vessel.kernel.transform.mixin;

import dev.vessel.kernel.ir.VesselSemanticHookRecord;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Weaves raw parsed Mixin bytecode annotations into canonical VESSEL Semantic Hook Records
 * and registers them into the active {@link SemanticHookRegistry}.
 * <p>
 * Directly resolves orphaned status of {@link MixinToSemanticHookConverter} (Issue #18)
 * and bridges Fabric/Forge mixin bytecode annotations into the sovereign IR pipeline.
 * </p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class VesselMixinSemanticHookWeaver {

    /** Default priority for Mixin injectors according to SpongePowered specifications. */
    public static final int DEFAULT_MIXIN_PRIORITY = 1000;

    private final MixinToSemanticHookConverter hookConverter;
    private final MixinAnnotationParser annotationParser;

    /**
     * Constructs a new mixin semantic hook weaver.
     */
    public VesselMixinSemanticHookWeaver() {
        this(new MixinToSemanticHookConverter(), new MixinAnnotationParser());
    }

    /**
     * Constructs a mixin semantic hook weaver with explicit collaborators.
     *
     * @param hookConverter    semantic hook converter
     * @param annotationParser mixin annotation parser
     */
    public VesselMixinSemanticHookWeaver(
            MixinToSemanticHookConverter hookConverter,
            MixinAnnotationParser annotationParser) {
        this.hookConverter = Objects.requireNonNull(hookConverter, "hookConverter cannot be null");
        this.annotationParser = Objects.requireNonNull(annotationParser, "annotationParser cannot be null");
    }

    /**
     * Parses all injection annotations from a candidate mixin classfile and registers corresponding
     * {@link VesselSemanticHookRecord} entries for each specified target class into the registry.
     *
     * @param mixinBytes    raw bytecode of the mixin class
     * @param targetClasses list of normalized or binary target class names
     * @param registry      target semantic hook registry
     * @return total count of registered semantic hooks
     */
    public int weaveMixinIntoRegistry(
            byte[] mixinBytes,
            List<String> targetClasses,
            SemanticHookRegistry registry) {
        Objects.requireNonNull(mixinBytes, "mixinBytes cannot be null");
        Objects.requireNonNull(targetClasses, "targetClasses cannot be null");
        Objects.requireNonNull(registry, "registry cannot be null");

        if (targetClasses.isEmpty()) {
            return 0;
        }

        ClassReader reader = new ClassReader(mixinBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        int registeredCount = 0;
        for (MethodNode methodNode : classNode.methods) {
            Optional<ParsedMixinAnnotation> parsedOpt = annotationParser.parseMethodAnnotation(methodNode);
            if (parsedOpt.isPresent()) {
                ParsedMixinAnnotation parsed = parsedOpt.get();
                int priority = DEFAULT_MIXIN_PRIORITY;
                for (String targetClass : targetClasses) {
                    if (targetClass != null && !targetClass.isBlank()) {
                        VesselSemanticHookRecord hookRecord = hookConverter.convert(
                                targetClass,
                                classNode.name,
                                methodNode.name,
                                parsed,
                                priority
                        );
                        registry.register(hookRecord);
                        registeredCount++;
                    }
                }
            }
        }
        return registeredCount;
    }

    public MixinToSemanticHookConverter getHookConverter() {
        return hookConverter;
    }

    public MixinAnnotationParser getAnnotationParser() {
        return annotationParser;
    }
}
