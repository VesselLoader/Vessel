package dev.vessel.kernel.vstm.remap;

import dev.vessel.kernel.vstm.builder.VstmTransmutationContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.objectweb.asm.commons.Remapper;

/**
 * End-to-end streaming transmutator for mod JAR files and archive bundles.
 *
 * <p>Streams each {@link JarEntry} from an input source, dynamically identifies JVM bytecode
 * classes to pass through {@link VstmClassBytecodeTransmutator}, retargets entry paths via
 * {@link VstmJarEntryNameResolver}, and preserves 100% of resource assets, configurations, and manifests intact
 * without unzipping to temporary disk storage (Catalog ref: USML-0021, FOL-0040).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 3)
 */
public final class VstmJarStreamTransmutator {

    private static final String MANIFEST_ENTRY_NAME = "META-INF/MANIFEST.MF";

    private final VstmClassBytecodeTransmutator classTransmutator;
    private final VstmJarEntryNameResolver nameResolver;

    /**
     * Constructs a JAR stream transmutator using default collaborator instances.
     */
    public VstmJarStreamTransmutator() {
        this(new VstmClassBytecodeTransmutator(), new VstmJarEntryNameResolver());
    }

    /**
     * Constructs a JAR stream transmutator with explicit collaborator components.
     *
     * @param classTransmutator the bytecode transmutation engine
     * @param nameResolver      the archive entry path resolver
     */
    public VstmJarStreamTransmutator(
            VstmClassBytecodeTransmutator classTransmutator,
            VstmJarEntryNameResolver nameResolver
    ) {
        this.classTransmutator = Objects.requireNonNull(classTransmutator, "classTransmutator must not be null");
        this.nameResolver = Objects.requireNonNull(nameResolver, "nameResolver must not be null");
    }

    /**
     * Transmutes an input JAR stream into an output stream using the active transmutation context.
     *
     * @param inputStream  source input stream containing the raw JAR archive
     * @param outputStream target output stream where transmuted JAR will be written
     * @param context      active module transmutation context
     * @return telemetry statistics detailing rewritten classes and preserved assets
     * @throws VstmRemapException if an I/O or bytecode transformation error occurs
     */
    public VstmRemapStatistics transmuteJar(
            InputStream inputStream,
            OutputStream outputStream,
            VstmTransmutationContext context
    ) {
        Objects.requireNonNull(context, "context must not be null");
        VstmAsmRemapperBridge bridge = new VstmAsmRemapperBridge(context);
        return transmuteJar(inputStream, outputStream, bridge);
    }

    /**
     * Transmutes an input JAR stream into an output stream using an ASM remapper.
     *
     * @param inputStream  source input stream containing the raw JAR archive
     * @param outputStream target output stream where transmuted JAR will be written
     * @param remapper     active bytecode remapper
     * @return telemetry statistics detailing rewritten classes and preserved assets
     * @throws VstmRemapException if an I/O or bytecode transformation error occurs
     */
    public VstmRemapStatistics transmuteJar(
            InputStream inputStream,
            OutputStream outputStream,
            Remapper remapper
    ) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        Objects.requireNonNull(outputStream, "outputStream must not be null");
        Objects.requireNonNull(remapper, "remapper must not be null");

        int classesTransmuted = 0;
        int resourcesPreserved = 0;
        long totalBytesProcessed = 0L;

        try {
            JarInputStream jarIn = new JarInputStream(inputStream);
            Manifest manifest = jarIn.getManifest();
            JarOutputStream jarOut;
            boolean manifestEmitted = false;

            if (manifest != null) {
                jarOut = new JarOutputStream(outputStream, manifest);
                manifestEmitted = true;
                resourcesPreserved++;
            } else {
                jarOut = new JarOutputStream(outputStream);
            }

            JarEntry entry;
            while ((entry = jarIn.getNextJarEntry()) != null) {
                String entryName = entry.getName();

                // Skip manifest if already emitted via JarOutputStream header
                if (manifestEmitted && MANIFEST_ENTRY_NAME.equalsIgnoreCase(entryName)) {
                    jarIn.closeEntry();
                    continue;
                }

                if (entry.isDirectory()) {
                    JarEntry dirEntry = new JarEntry(entryName);
                    if (entry.getTime() > 0) {
                        dirEntry.setTime(entry.getTime());
                    }
                    jarOut.putNextEntry(dirEntry);
                    jarOut.closeEntry();
                    jarIn.closeEntry();
                    continue;
                }

                byte[] entryBytes = jarIn.readAllBytes();
                totalBytesProcessed += entryBytes.length;

                if (nameResolver.isClassEntry(entryName)) {
                    byte[] transmutedBytes = classTransmutator.transmute(entryBytes, remapper);
                    String remappedName = nameResolver.resolveEntryName(entryName, remapper);

                    JarEntry classEntry = new JarEntry(remappedName);
                    if (entry.getTime() > 0) {
                        classEntry.setTime(entry.getTime());
                    }
                    jarOut.putNextEntry(classEntry);
                    jarOut.write(transmutedBytes);
                    jarOut.closeEntry();

                    classesTransmuted++;
                } else {
                    JarEntry resourceEntry = new JarEntry(entryName);
                    if (entry.getTime() > 0) {
                        resourceEntry.setTime(entry.getTime());
                    }
                    jarOut.putNextEntry(resourceEntry);
                    jarOut.write(entryBytes);
                    jarOut.closeEntry();

                    resourcesPreserved++;
                }

                jarIn.closeEntry();
            }

            jarOut.finish();
            jarOut.flush();

            return new VstmRemapStatistics(classesTransmuted, resourcesPreserved, totalBytesProcessed);
        } catch (IOException ex) {
            throw new VstmRemapException("I/O error during JAR stream transmutation: " + ex.getMessage(), ex);
        }
    }

    /**
     * Retrieves the underlying class bytecode transmutator.
     *
     * @return the class bytecode transmutator
     */
    public VstmClassBytecodeTransmutator getClassTransmutator() {
        return classTransmutator;
    }

    /**
     * Retrieves the entry name resolver.
     *
     * @return the entry name resolver
     */
    public VstmJarEntryNameResolver getNameResolver() {
        return nameResolver;
    }
}
