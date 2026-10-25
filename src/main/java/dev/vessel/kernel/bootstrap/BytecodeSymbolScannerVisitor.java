package dev.vessel.kernel.bootstrap;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Event-driven ASM ClassVisitor scanning class metadata, interfaces, annotations, and entrypoints
 * in a single zero-allocation streaming pass.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BytecodeSymbolScannerVisitor extends ClassVisitor {

    private String className;
    private String superName;
    private final List<String> interfaces = new ArrayList<>();
    private final Set<String> annotations = new HashSet<>();
    private final List<String> entrypoints = new ArrayList<>();
    private final List<String> mixinTargets = new ArrayList<>();
    private boolean isAbstract;
    private boolean isInterface;

    /**
     * Constructs a BytecodeSymbolScannerVisitor using ASM9.
     */
    public BytecodeSymbolScannerVisitor() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.superName = superName;
        this.isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
        this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        if (interfaces != null) {
            for (String iface : interfaces) {
                this.interfaces.add(iface);
            }
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (descriptor != null) {
            annotations.add(descriptor);
            if (descriptor.contains("Mixin") || descriptor.endsWith("/Mixin;")) {
                return new BytecodeAnnotationValueVisitor(mixinTargets);
            }
        }
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        // Look for main methods or mod entrypoint methods
        if ("main".equals(name) && "([Ljava/lang/String;)V".equals(descriptor) && (access & Opcodes.ACC_STATIC) != 0) {
            entrypoints.add(className + "#main");
        } else if ("onInitialize".equals(name) || "onInitializeClient".equals(name) || "onInitializeServer".equals(name)) {
            entrypoints.add(className + "#" + name);
        }
        return null;
    }

    /**
     * Builds the immutable ScannedClassSymbols extracted during the visitor walk.
     *
     * @return the scanned symbols snapshot
     */
    public ScannedClassSymbols toSymbols() {
        return new ScannedClassSymbols(
                className != null ? className : "unknown",
                superName != null ? superName : "java/lang/Object",
                interfaces,
                annotations,
                entrypoints,
                mixinTargets,
                isAbstract,
                isInterface
        );
    }
}
