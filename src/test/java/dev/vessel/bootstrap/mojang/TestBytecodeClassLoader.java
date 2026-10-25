package dev.vessel.bootstrap.mojang;

import java.util.Objects;

/**
 * Test helper {@link ClassLoader} that defines a given class from raw bytecode.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class TestBytecodeClassLoader extends ClassLoader {

    private final String targetClassName;
    private final byte[] classBytes;

    public TestBytecodeClassLoader(ClassLoader parent, String targetClassName, byte[] classBytes) {
        super(parent);
        this.targetClassName = Objects.requireNonNull(targetClassName, "targetClassName must not be null");
        this.classBytes = Objects.requireNonNull(classBytes, "classBytes must not be null");
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (targetClassName.equals(name)) {
            return defineClass(name, classBytes, 0, classBytes.length);
        }
        return super.findClass(name);
    }
}
