package dev.vessel.kernel.identity;

import dev.vessel.kernel.bootstrap.ScannedClassSymbols;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Ultra-fast bytecode-level ecosystem classifier.
 * Inspects raw bytecode instructions, superclasses, interfaces, and annotation descriptors
 * to definitively identify the originating source loader (FABRIC, QUILT, FORGE, NEOFORGE, BUKKIT, VESSEL_NATIVE)
 * without loading classes into the JVM, preventing classloader contamination and initialization crashes (ERR-03, ERR-04).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ModuleSourceClassifier {

    private static final int PARSE_FLAGS = ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES;

    // Cache enum values to eliminate hot-path array cloning (ERR-04)
    private static final SourceLoaderKind[] CACHED_LOADER_KINDS = SourceLoaderKind.values();

    /**
     * Constructs a ModuleSourceClassifier instance.
     */
    public ModuleSourceClassifier() {
    }

    /**
     * Classifies a single class bytecode array into its source loader kind.
     *
     * @param classBytes raw compiled class bytes
     * @return identified SourceLoaderKind
     */
    public SourceLoaderKind classify(byte[] classBytes) {
        return classifyDetailed(classBytes).detectedKind();
    }

    /**
     * Performs detailed classification on a single class bytecode array, returning confidence metrics.
     *
     * @param classBytes raw compiled class bytes
     * @return classification result record
     */
    public ModuleSourceClassificationResult classifyDetailed(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes cannot be null");
        SourceClassifierContext context = new SourceClassifierContext();
        ClassReader reader = new ClassReader(classBytes);
        SourceClassifierBytecodeVisitor visitor = new SourceClassifierBytecodeVisitor(context);
        reader.accept(visitor, PARSE_FLAGS);

        return new ModuleSourceClassificationResult(
                context.resolveWinningKind(),
                context.maxScore(),
                context.primarySignature()
        );
    }

    /**
     * Classifies pre-scanned class symbols without re-parsing bytecode.
     *
     * @param symbols immutable scanned class symbols
     * @return identified SourceLoaderKind
     */
    public SourceLoaderKind classify(ScannedClassSymbols symbols) {
        return classifyDetailed(symbols).detectedKind();
    }

    /**
     * Performs detailed classification on pre-scanned class symbols.
     *
     * @param symbols immutable scanned class symbols
     * @return classification result record
     */
    public ModuleSourceClassificationResult classifyDetailed(ScannedClassSymbols symbols) {
        Objects.requireNonNull(symbols, "symbols cannot be null");
        SourceClassifierContext context = new SourceClassifierContext();

        // Check superclass
        if (symbols.superName() != null) {
            context.recordTypeReference(symbols.superName());
        }

        // Check interfaces with indexed loop (ERR-03)
        List<String> ifaces = symbols.interfaces();
        int ifaceSize = ifaces.size();
        for (int i = 0; i < ifaceSize; i++) {
            context.recordTypeReference(ifaces.get(i));
        }

        // Check annotations with for-each / iterator
        for (String annotation : symbols.annotations()) {
            context.recordAnnotation(annotation);
        }

        return new ModuleSourceClassificationResult(
                context.resolveWinningKind(),
                context.maxScore(),
                context.primarySignature()
        );
    }

    /**
     * Classifies an entire JAR archive stream by scanning its class entries.
     *
     * @param jarInputStream stream of the JAR archive
     * @return dominant SourceLoaderKind
     * @throws IOException if reading the archive fails
     */
    public SourceLoaderKind classifyJar(InputStream jarInputStream) throws IOException {
        Objects.requireNonNull(jarInputStream, "jarInputStream cannot be null");
        SourceClassifierContext aggregatedContext = new SourceClassifierContext();

        try (ZipInputStream zis = new ZipInputStream(jarInputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class") && !entry.getName().startsWith("META-INF/")) {
                    byte[] classBytes = zis.readAllBytes();
                    ClassReader reader = new ClassReader(classBytes);
                    SourceClassifierBytecodeVisitor visitor = new SourceClassifierBytecodeVisitor(aggregatedContext);
                    reader.accept(visitor, PARSE_FLAGS);
                }
                zis.closeEntry();
            }
        }

        return aggregatedContext.resolveWinningKind();
    }

    /**
     * Classifies a collection of class bytecode arrays.
     *
     * @param classesBytecode collection of class bytecode arrays
     * @return dominant SourceLoaderKind
     */
    public SourceLoaderKind classifyAll(Collection<byte[]> classesBytecode) {
        Objects.requireNonNull(classesBytecode, "classesBytecode cannot be null");
        SourceClassifierContext aggregatedContext = new SourceClassifierContext();

        for (byte[] classBytes : classesBytecode) {
            ClassReader reader = new ClassReader(classBytes);
            SourceClassifierBytecodeVisitor visitor = new SourceClassifierBytecodeVisitor(aggregatedContext);
            reader.accept(visitor, PARSE_FLAGS);
        }

        return aggregatedContext.resolveWinningKind();
    }

    /**
     * Returns the cached array of known loader kinds without allocation overhead.
     *
     * @return array of SourceLoaderKind
     */
    public static SourceLoaderKind[] cachedLoaderKinds() {
        return CACHED_LOADER_KINDS;
    }
}
