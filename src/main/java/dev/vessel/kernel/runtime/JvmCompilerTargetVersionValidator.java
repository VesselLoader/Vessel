package dev.vessel.kernel.runtime;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Validates bytecode target version compliance across compiled modules and mod JARs.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class JvmCompilerTargetVersionValidator {

    public static final int CLASS_MAGIC = 0xCAFEBABE;
    public static final int JAVA_21_CLASS_VERSION = 65;
    public static final int JAVA_25_CLASS_VERSION = 69;

    public int readClassFileMajorVersion(byte[] bytecode) {
        if (bytecode == null || bytecode.length < 8) {
            return -1;
        }
        int magic = ((bytecode[0] & 0xFF) << 24)
                | ((bytecode[1] & 0xFF) << 16)
                | ((bytecode[2] & 0xFF) << 8)
                | (bytecode[3] & 0xFF);
        if (magic != CLASS_MAGIC) {
            return -1;
        }
        return ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
    }

    public int inspectJarRootEntryClassVersion(Path jarPath) {
        Objects.requireNonNull(jarPath, "jarPath cannot be null");
        if (!Files.isRegularFile(jarPath)) {
            return -1;
        }
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class") && !entry.getName().contains("module-info")) {
                    try (InputStream is = jar.getInputStream(entry);
                         DataInputStream dis = new DataInputStream(is)) {
                        int magic = dis.readInt();
                        if (magic == CLASS_MAGIC) {
                            dis.readUnsignedShort(); // minor
                            return dis.readUnsignedShort(); // major
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            return -1;
        }
        return -1;
    }

    public boolean isBytecodeExecutableOnJvm(int classMajorVersion, int hostJvmFeatureVersion) {
        if (classMajorVersion <= 0) {
            return true;
        }
        int requiredJvmFeature = classMajorVersion - 44;
        return hostJvmFeatureVersion >= requiredJvmFeature;
    }
}
