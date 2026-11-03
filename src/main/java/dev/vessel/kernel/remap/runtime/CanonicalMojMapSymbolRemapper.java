package dev.vessel.kernel.remap.runtime;

import dev.vessel.kernel.remap.MappingNamespaceKind;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

/**
 * Runtime symbol remapper that translates Intermediary, Yarn, Searge SRG, and Spigot-obfuscated
 * classfiles into canonical Mojang Official Mappings (MojMap) on the fly via ASM
 * {@link ClassRemapper}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CanonicalMojMapSymbolRemapper {

    private final BytecodeNamespaceClassifier classifier = new BytecodeNamespaceClassifier();
    private final MojMapAsmRemapperRule remapperRule = new MojMapAsmRemapperRule();

    /**
     * Remaps the provided JVM class bytecode into canonical MojMap symbols if the class uses a
     * non-MojMap namespace.
     *
     * @param classBytes raw JVM classfile bytes (must not be null)
     * @return MojMap-harmonized classfile bytes, or the original bytes if already MojMap or invalid
     */
    public byte[] remapToMojMapIfNecessary(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes must not be null");
        if (classBytes.length < 8) {
            return classBytes;
        }
        if ((classBytes[0] & 0xFF) != 0xCA
                || (classBytes[1] & 0xFF) != 0xFE
                || (classBytes[2] & 0xFF) != 0xBA
                || (classBytes[3] & 0xFF) != 0xBE) {
            return classBytes;
        }
        MappingNamespaceKind kind = classifier.classifyClassBytecode(classBytes);
        if (kind == MappingNamespaceKind.MOJMAP) {
            return classBytes;
        }
        try {
            ClassReader cr = new ClassReader(classBytes);
            ClassWriter cw = new ClassWriter(0);
            ClassRemapper classRemapper = new ClassRemapper(cw, remapperRule);
            cr.accept(classRemapper, 0);
            return cw.toByteArray();
        } catch (Exception e) {
            return classBytes;
        }
    }

    /**
     * Maps a class internal name (e.g., {@code "net/minecraft/class_1937"}) to its canonical MojMap
     * internal name.
     *
     * @param internalName source internal class name
     * @return mapped MojMap internal class name
     */
    public String mapClassInternalName(String internalName) {
        return remapperRule.map(internalName);
    }

    /**
     * Maps a method name from Intermediary or SRG into its canonical MojMap method name.
     *
     * @param owner      owning class internal name
     * @param name       source method name
     * @param descriptor method descriptor
     * @return mapped MojMap method name
     */
    public String mapMethodSymbol(String owner, String name, String descriptor) {
        return remapperRule.mapMethodName(owner, name, descriptor);
    }

    /**
     * Maps a field name from Intermediary or SRG into its canonical MojMap field name.
     *
     * @param owner      owning class internal name
     * @param name       source field name
     * @param descriptor field descriptor
     * @return mapped MojMap field name
     */
    public String mapFieldSymbol(String owner, String name, String descriptor) {
        return remapperRule.mapFieldName(owner, name, descriptor);
    }
}
