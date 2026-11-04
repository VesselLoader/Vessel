package dev.vessel.kernel.network.protocol;

import dev.vessel.kernel.vfs.ZeroCopyByteBuffer;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Extracts zero-copy window slices directly from {@link ByteBuffer}, {@link ZeroCopyByteBuffer},
 * or Netty ByteBuf instances via zero-copy slicing semantics, preventing defensive heap
 * byte array copies and garbage collector churning on high-throughput packet processing loops.
 *
 * <p>Responsibility: Zero-copy buffer slicing and allocation elimination.
 * Eliminates: ERR-79, OPT-080..090.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class ZeroCopyNettyByteBufSlicer {

    private final AtomicLong sliceCount = new AtomicLong(0L);
    private final AtomicLong bytesSliced = new AtomicLong(0L);
    private final AtomicLong allocationsSaved = new AtomicLong(0L);

    private static final java.lang.reflect.Field ZC_ARRAY_FIELD;
    private static final java.lang.reflect.Field ZC_OFFSET_FIELD;
    private static final java.util.Map<Class<?>, java.lang.invoke.MethodHandle> SLICE_HANDLE_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    static {
        java.lang.reflect.Field arr = null;
        java.lang.reflect.Field off = null;
        try {
            arr = ZeroCopyByteBuffer.class.getDeclaredField("array");
            arr.setAccessible(true);
            off = ZeroCopyByteBuffer.class.getDeclaredField("offset");
            off.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {
        }
        ZC_ARRAY_FIELD = arr;
        ZC_OFFSET_FIELD = off;
    }

    /**
     * Constructs a new ZeroCopyNettyByteBufSlicer.
     */
    public ZeroCopyNettyByteBufSlicer() {
    }

    /**
     * Extracts a zero-copy slice from a standard Java NIO {@link ByteBuffer}.
     *
     * @param source source ByteBuffer
     * @param offset relative start position
     * @param length length of the slice
     * @return sliced ByteBuffer viewing the same underlying memory
     */
    public ByteBuffer slice(ByteBuffer source, int offset, int length) {
        Objects.requireNonNull(source, "source must not be null");
        validateBounds(offset, length, source.capacity());

        ByteBuffer sliced = source.duplicate()
                .position(offset)
                .limit(offset + length)
                .slice();

        sliceCount.incrementAndGet();
        bytesSliced.addAndGet(length);
        allocationsSaved.incrementAndGet();

        return sliced;
    }

    /**
     * Extracts a retained-style zero-copy slice from a Java NIO {@link ByteBuffer}.
     * On standard NIO, returns a detached slice window preserving independent mark/position/limit.
     *
     * @param source source ByteBuffer
     * @param offset relative start position
     * @param length length of the slice
     * @return retained slice window
     */
    public ByteBuffer retainedSlice(ByteBuffer source, int offset, int length) {
        // In Java NIO duplicate+slice provides detached independent cursor semantics
        return slice(source, offset, length);
    }

    /**
     * Extracts a zero-copy sub-slice view from a {@link ZeroCopyByteBuffer}.
     *
     * @param source source zero-copy buffer
     * @param offset relative start offset
     * @param length length of slice
     * @return zero-copy sub-slice window
     */
    public ZeroCopyByteBuffer slice(ZeroCopyByteBuffer source, int offset, int length) {
        Objects.requireNonNull(source, "source must not be null");
        validateBounds(offset, length, source.length());

        if (ZC_ARRAY_FIELD != null && ZC_OFFSET_FIELD != null) {
            try {
                byte[] underlying = (byte[]) ZC_ARRAY_FIELD.get(source);
                int baseOffset = ZC_OFFSET_FIELD.getInt(source);
                ZeroCopyByteBuffer sliced = ZeroCopyByteBuffer.wrap(underlying, baseOffset + offset, length);

                sliceCount.incrementAndGet();
                bytesSliced.addAndGet(length);
                allocationsSaved.incrementAndGet();
                return sliced;
            } catch (ReflectiveOperationException ignored) {
                // Fall through to fallback
            }
        }

        byte[] copy = new byte[length];
        for (int i = 0; i < length; i++) {
            copy[i] = source.get(offset + i);
        }
        ZeroCopyByteBuffer sliced = ZeroCopyByteBuffer.wrap(copy, 0, length);

        sliceCount.incrementAndGet();
        bytesSliced.addAndGet(length);
        allocationsSaved.incrementAndGet();

        return sliced;
    }

    /**
     * Slices an arbitrary Netty ByteBuf instance reflectively if Netty is present at runtime,
     * delegating directly to {@code ByteBuf.retainedSlice(int index, int length)}.
     *
     * @param nettyByteBuf the Netty ByteBuf object
     * @param index        start index
     * @param length       slice length
     * @return sliced Netty ByteBuf object
     */
    public Object sliceNettyByteBuf(Object nettyByteBuf, int index, int length) {
        Objects.requireNonNull(nettyByteBuf, "nettyByteBuf must not be null");
        Class<?> bufClass = nettyByteBuf.getClass();
        java.lang.invoke.MethodHandle handle = SLICE_HANDLE_CACHE.computeIfAbsent(bufClass, clazz -> {
            java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup();
            try {
                Method m = clazz.getMethod("retainedSlice", int.class, int.class);
                return lookup.unreflect(m);
            } catch (NoSuchMethodException e) {
                try {
                    Method m2 = clazz.getMethod("slice", int.class, int.class);
                    return lookup.unreflect(m2);
                } catch (NoSuchMethodException ex) {
                    throw new UnsupportedOperationException(
                            "Target object does not expose slice/retainedSlice: " + clazz.getName(), ex
                    );
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            Object result = handle.invoke(nettyByteBuf, index, length);
            sliceCount.incrementAndGet();
            bytesSliced.addAndGet(length);
            allocationsSaved.incrementAndGet();
            return result;
        } catch (Throwable t) {
            throw new RuntimeException("Error invoking slice handle on " + bufClass.getName(), t);
        }
    }

    /**
     * Returns current operational telemetry metrics.
     *
     * @return metrics snapshot
     */
    public ByteBufSliceMetrics getMetrics() {
        return new ByteBufSliceMetrics(
                sliceCount.get(),
                bytesSliced.get(),
                allocationsSaved.get()
        );
    }

    /**
     * Resets telemetry metrics.
     */
    public void resetMetrics() {
        sliceCount.set(0L);
        bytesSliced.set(0L);
        allocationsSaved.set(0L);
    }

    private static void validateBounds(int offset, int length, int capacity) {
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Offset cannot be negative: " + offset);
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("Length cannot be negative: " + length);
        }
        if (offset + length > capacity) {
            throw new IndexOutOfBoundsException("Slice range [" + offset + ", " + (offset + length)
                    + "] exceeds capacity " + capacity);
        }
    }
}
