package dev.vessel.bootstrap.mojang;

import java.util.Objects;

/**
 * Test helper classloader that defines classes from in-memory byte arrays.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class TestBytecodeClassDefiner extends ClassLoader {

    public TestBytecodeClassDefiner(ClassLoader parent) {
        super(parent);
    }

    public Class<?> defineFromBytes(String name, byte[] bytecode) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(bytecode, "bytecode must not be null");
        return defineClass(name, bytecode, 0, bytecode.length);
    }
}
