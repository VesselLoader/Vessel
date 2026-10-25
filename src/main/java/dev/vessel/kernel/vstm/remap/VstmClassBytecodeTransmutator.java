package dev.vessel.kernel.vstm.remap;

import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;

/**
 * Single-responsibility bytecode transformation engine that accepts a raw JVM {@code .class}
 * byte array and re-emits a fully transmuted class byte array using ASM.
 *
 * <p>Employs {@link VstmClassRemapper} with {@code COMPUTE_MAXS} stack computation,
 * eliminating unnecessary class-hierarchy reflection during offline or loader transformation (Catalog ref: USML-0021).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 3)
 */
public final class VstmClassBytecodeTransmutator {

    private static final int DEFAULT_READER_FLAGS = ClassReader.EXPAND_FRAMES;
    private static final int DEFAULT_WRITER_FLAGS = ClassWriter.COMPUTE_MAXS;

    /**
     * Constructs a class bytecode transmutator instance.
     */
    public VstmClassBytecodeTransmutator() {
        // Stateless transformation engine
    }

    /**
     * Transmutes raw class bytecode using the provided remapper and default ASM flags.
     *
     * @param classBytecode raw input class bytes
     * @param remapper      active bytecode remapper
     * @return transmuted class bytecode
     * @throws VstmRemapException if bytecode parsing or generation fails
     */
    public byte[] transmute(byte[] classBytecode, Remapper remapper) {
        return transmute(classBytecode, remapper, DEFAULT_READER_FLAGS, DEFAULT_WRITER_FLAGS);
    }

    /**
     * Transmutes raw class bytecode using the provided remapper and explicit ASM flags.
     *
     * @param classBytecode raw input class bytes
     * @param remapper      active bytecode remapper
     * @param readerFlags   ASM ClassReader parsing options (e.g. EXPAND_FRAMES)
     * @param writerFlags   ASM ClassWriter generation flags (e.g. COMPUTE_MAXS)
     * @return transmuted class bytecode
     * @throws VstmRemapException if bytecode parsing or generation fails
     */
    public byte[] transmute(byte[] classBytecode, Remapper remapper, int readerFlags, int writerFlags) {
        Objects.requireNonNull(classBytecode, "classBytecode must not be null");
        Objects.requireNonNull(remapper, "remapper must not be null");

        if (classBytecode.length == 0) {
            throw new VstmRemapException("Cannot transmute empty class bytecode");
        }

        try {
            ClassReader reader = new ClassReader(classBytecode);
            ClassWriter writer = new ClassWriter(reader, writerFlags);
            VstmClassRemapper classRemapper = new VstmClassRemapper(writer, remapper);

            reader.accept(classRemapper, readerFlags);
            return writer.toByteArray();
        } catch (Exception ex) {
            throw new VstmRemapException("Failed to transmute class bytecode: " + ex.getMessage(), ex);
        }
    }
}
