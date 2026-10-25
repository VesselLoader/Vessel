package dev.vessel.kernel.ir;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Ahead-Of-Time parser converting raw JVM bytecode into normalized ASM {@link ClassNode} representations.
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-7)
 */
public class BytecodeToIRParser {

    /**
     * Parses raw class byte array into an ASM ClassNode.
     *
     * @param classBytes raw .class bytecode
     * @return populated ClassNode
     */
    public ClassNode parse(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes cannot be null");
        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }

    /**
     * Parses class bytecode from an input stream.
     *
     * @param stream input stream containing .class file
     * @return populated ClassNode
     * @throws IOException if stream reading fails
     */
    public ClassNode parse(InputStream stream) throws IOException {
        Objects.requireNonNull(stream, "stream cannot be null");
        ClassReader reader = new ClassReader(stream);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }
}
