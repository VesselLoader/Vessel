package dev.vessel.kernel.jvm.reflection;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Recalculates and dynamically synchronizes memory offsets via Unsafe after bytecode injection.
 * <p>
 * When Mixin patches inject new synthetic fields into Minecraft classes, JVM memory layouts shift.
 * This recalculator accurately recomputes {@link Unsafe#objectFieldOffset} and {@link Unsafe#staticFieldOffset},
 * updating cached mod offset references to prevent fatal JVM memory faults (SIGSEGV).
 * </p>
 * <p>
 * Directly resolves ERR-73 and MAP-REF-0351..0700.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class UnsafeFieldOffsetRecalculator {

    private static final Unsafe THE_UNSAFE;

    static {
        Unsafe unsafe = null;
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Throwable t) {
            // Unsafe unavailable
        }
        THE_UNSAFE = unsafe;
    }

    private final ConcurrentMap<String, FieldOffsetEntry> offsetCache = new ConcurrentHashMap<>();

    /**
     * Calculates the exact memory offset for a specified field on a class.
     *
     * @param clazz the class defining the field
     * @param fieldName the name of the field
     * @return FieldOffsetEntry containing the calculated memory offset
     * @throws NoSuchFieldException if the field is not present
     */
    public FieldOffsetEntry recalculate(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Objects.requireNonNull(clazz, "clazz cannot be null");
        Objects.requireNonNull(fieldName, "fieldName cannot be null");

        if (THE_UNSAFE == null) {
            throw new IllegalStateException("sun.misc.Unsafe is not available in the current JVM runtime");
        }

        Field field = findField(clazz, fieldName);
        field.setAccessible(true);

        boolean isStatic = Modifier.isStatic(field.getModifiers());
        long offset = isStatic
                ? THE_UNSAFE.staticFieldOffset(field)
                : THE_UNSAFE.objectFieldOffset(field);

        FieldOffsetEntry entry = new FieldOffsetEntry(clazz.getName(), fieldName, offset, isStatic);
        offsetCache.put(cacheKey(clazz.getName(), fieldName), entry);
        return entry;
    }

    /**
     * Retrieves the cached offset or recalculates it if not yet registered.
     *
     * @param clazz the declaring class
     * @param fieldName the field name
     * @return memory byte offset
     */
    public long getOffset(Class<?> clazz, String fieldName) {
        String key = cacheKey(clazz.getName(), fieldName);
        FieldOffsetEntry entry = offsetCache.get(key);
        if (entry != null) {
            return entry.offset();
        }

        try {
            return recalculate(clazz, fieldName).offset();
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Cannot locate field " + fieldName + " on " + clazz.getName(), e);
        }
    }

    /**
     * Directly updates a static long field in a caching class with the freshly recalculated offset.
     *
     * @param cacheHolderClass the class holding the static offset field
     * @param cacheFieldName the static long field name to update
     * @param targetClass the class whose field layout changed
     * @param targetFieldName the shifted field name
     */
    public void updateStaticCacheHolder(
            Class<?> cacheHolderClass,
            String cacheFieldName,
            Class<?> targetClass,
            String targetFieldName) throws NoSuchFieldException, IllegalAccessException {
        Objects.requireNonNull(cacheHolderClass, "cacheHolderClass cannot be null");
        Objects.requireNonNull(cacheFieldName, "cacheFieldName cannot be null");
        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        Objects.requireNonNull(targetFieldName, "targetFieldName cannot be null");

        long offset = getOffset(targetClass, targetFieldName);

        Field cacheField = cacheHolderClass.getDeclaredField(cacheFieldName);
        cacheField.setAccessible(true);

        try {
            cacheField.setLong(null, offset);
        } catch (Throwable t) {
            if (THE_UNSAFE != null) {
                long staticOffset = THE_UNSAFE.staticFieldOffset(cacheField);
                Object base = THE_UNSAFE.staticFieldBase(cacheField);
                THE_UNSAFE.putLong(base, staticOffset, offset);
            }
        }
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + fieldName + " was not found in hierarchy of " + clazz.getName());
    }

    private String cacheKey(String className, String fieldName) {
        return className + "#" + fieldName;
    }
}
