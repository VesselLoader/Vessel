package dev.vessel.kernel.runtime;

import dev.vessel.kernel.identity.VesselModuleIdentifier;
import dev.vessel.kernel.ir.BytecodeToIRParser;
import dev.vessel.kernel.vfs.InMemoryVirtualFileSystemMount;
import dev.vessel.kernel.vfs.ZeroCopyByteBuffer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Isolated modular ClassLoader for VESSEL modules and plugins.
 * Reads bytecode directly from in-memory Virtual File System mounts (ZeroCopyByteBuffer),
 * applies synthetic interface injection on the fly via ASM, and prevents cross-module
 * classpath contamination.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class VesselModuleClassLoader extends ClassLoader {

    private final VesselModuleIdentifier moduleIdentifier;
    private final InMemoryVirtualFileSystemMount vfsMount;
    private final InterfaceInjectionRegistry interfaceRegistry;
    private final BytecodeToIRParser irParser;
    private final SyntheticInterfaceBytecodeApplier interfaceApplier;
    private final AtomicInteger loadedClassCount = new AtomicInteger(0);

    /**
     * Constructs a new VesselModuleClassLoader.
     *
     * @param moduleIdentifier the identity of the module owning this loader
     * @param vfsMount the in-memory VFS mount containing the module's compiled classes
     * @param parent the parent class loader (typically the platform or application loader)
     * @param interfaceRegistry the synthetic interface injection registry
     * @param irParser the bytecode-to-IR parser
     * @param interfaceApplier the synthetic interface applier
     */
    public VesselModuleClassLoader(
            VesselModuleIdentifier moduleIdentifier,
            InMemoryVirtualFileSystemMount vfsMount,
            ClassLoader parent,
            InterfaceInjectionRegistry interfaceRegistry,
            BytecodeToIRParser irParser,
            SyntheticInterfaceBytecodeApplier interfaceApplier) {
        super(parent);
        this.moduleIdentifier = Objects.requireNonNull(moduleIdentifier, "moduleIdentifier cannot be null");
        this.vfsMount = Objects.requireNonNull(vfsMount, "vfsMount cannot be null");
        this.interfaceRegistry = Objects.requireNonNull(interfaceRegistry, "interfaceRegistry cannot be null");
        this.irParser = Objects.requireNonNull(irParser, "irParser cannot be null");
        this.interfaceApplier = Objects.requireNonNull(interfaceApplier, "interfaceApplier cannot be null");
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Enforce system and microkernel package delegation boundaries
        if (isProtectedSystemPackage(name)) {
            return super.findClass(name);
        }

        String resourcePath = name.replace('.', '/') + ".class";
        Optional<ZeroCopyByteBuffer> optBuffer = vfsMount.readResourceZeroCopy(resourcePath);

        if (optBuffer.isEmpty()) {
            throw new ClassNotFoundException("Class " + name + " not found in VFS mount for " + moduleIdentifier);
        }

        ZeroCopyByteBuffer buffer = optBuffer.get();
        byte[] classBytes = buffer.toByteArrayCopy();

        // Check if synthetic interface injections apply to this class
        if (interfaceRegistry.hasInjections(name)) {
            Set<String> injections = interfaceRegistry.getInjectedInterfaces(name);
            ClassNode classNode = irParser.parse(classBytes);
            int injectedCount = interfaceApplier.apply(classNode, injections);

            if (injectedCount > 0) {
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                classNode.accept(writer);
                classBytes = writer.toByteArray();
            }
        }

        Class<?> defined = defineClass(name, classBytes, 0, classBytes.length);
        loadedClassCount.incrementAndGet();
        return defined;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        Optional<ZeroCopyByteBuffer> optBuffer = vfsMount.readResourceZeroCopy(name);
        if (optBuffer.isPresent()) {
            return optBuffer.get().asInputStream();
        }
        return super.getResourceAsStream(name);
    }

    /**
     * Returns the module identifier associated with this classloader.
     *
     * @return module identifier
     */
    public VesselModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    /**
     * Returns the total count of classes defined and loaded by this module loader.
     *
     * @return loaded class count
     */
    public int getLoadedClassCount() {
        return loadedClassCount.get();
    }

    private static boolean isProtectedSystemPackage(String name) {
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("jdk.")
                || name.startsWith("sun.")
                || name.startsWith("dev.vessel.kernel.");
    }
}
