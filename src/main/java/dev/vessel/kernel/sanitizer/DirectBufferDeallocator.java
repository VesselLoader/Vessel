package dev.vessel.kernel.sanitizer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Objects;
import sun.misc.Unsafe;

/**
 * Low-level utility safely deallocating direct off-heap byte buffers across Java 21 LTS runtimes.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DirectBufferDeallocator {

    private static final Unsafe THE_UNSAFE;

    static {
        Unsafe unsafe = null;
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Throwable ignored) {
        }
        THE_UNSAFE = unsafe;
    }

    private DirectBufferDeallocator() {
    }

    /**
     * Immediately deallocates the native memory backed by a direct {@link ByteBuffer}.
     *
     * @param buffer direct byte buffer to deallocate
     * @return {@code true} if deallocation succeeded
     */
    public static boolean deallocate(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        if (!buffer.isDirect()) {
            return false;
        }

        // 1. Try Unsafe.invokeCleaner (Standard JDK 9+)
        if (THE_UNSAFE != null) {
            try {
                THE_UNSAFE.invokeCleaner(buffer);
                return true;
            } catch (Throwable ignored) {
            }
        }

        // 2. Fallback via reflection on DirectBuffer.cleaner()
        try {
            Method cleanerMethod = buffer.getClass().getMethod("cleaner");
            cleanerMethod.setAccessible(true);
            Object cleaner = cleanerMethod.invoke(buffer);
            if (cleaner != null) {
                Method cleanMethod = cleaner.getClass().getMethod("clean");
                cleanMethod.setAccessible(true);
                cleanMethod.invoke(cleaner);
                return true;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }
}
