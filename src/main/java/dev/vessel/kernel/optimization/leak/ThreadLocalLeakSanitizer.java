package dev.vessel.kernel.optimization.leak;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ThreadLocal and ClassLoader metadata leak sanitizer.
 * Scans active threads across the JVM ThreadGroup, identifying and unhooking dangling ThreadLocal
 * instances and static cache bindings associated with unloading or reloading ClassLoaders,
 * systematically preventing Metaspace OutOfMemoryError crashes on servers with frequent plugin reloads.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ThreadLocalLeakSanitizer {

    private final List<Runnable> staticCleanupHooks = new CopyOnWriteArrayList<>();

    /**
     * Constructs a ThreadLocalLeakSanitizer.
     */
    public ThreadLocalLeakSanitizer() {
    }

    /**
     * Registers a static cache or listener cleanup callback to be executed during sanitization.
     *
     * @param cleanupHook cleanup task
     */
    public void registerStaticCleanup(Runnable cleanupHook) {
        Objects.requireNonNull(cleanupHook, "cleanupHook cannot be null");
        staticCleanupHooks.add(cleanupHook);
    }

    /**
     * Executes a full sanitization pass targeting the specified classloader being unloaded.
     *
     * @param targetClassLoader classloader being unloaded or reloaded
     * @return sanitization report
     */
    public LeakSanitizeResult sanitize(ClassLoader targetClassLoader) {
        long start = System.nanoTime();
        int clearedThreadLocals = 0;
        int clearedStaticRefs = 0;

        // 1. Run static cleanup hooks
        for (Runnable hook : staticCleanupHooks) {
            try {
                hook.run();
                clearedStaticRefs++;
            } catch (Throwable ignored) {
                // Ignore errors in custom cleanup hooks to maintain stability
            }
        }

        // 2. Enumerate all active threads
        Thread[] threads = getAllActiveThreads();
        int threadCount = threads.length;

        // 3. Inspect ThreadLocals on active threads
        if (targetClassLoader != null) {
            for (Thread thread : threads) {
                if (thread != null) {
                    clearedThreadLocals += sanitizeThread(thread, targetClassLoader, "threadLocals");
                    clearedThreadLocals += sanitizeThread(thread, targetClassLoader, "inheritableThreadLocals");
                }
            }
        }

        long elapsed = System.nanoTime() - start;
        return new LeakSanitizeResult(threadCount, clearedThreadLocals, clearedStaticRefs, elapsed);
    }

    private int sanitizeThread(Thread thread, ClassLoader targetClassLoader, String fieldName) {
        int cleared = 0;
        try {
            Field threadLocalsField = Thread.class.getDeclaredField(fieldName);
            threadLocalsField.setAccessible(true);
            Object map = threadLocalsField.get(thread);
            if (map == null) {
                return 0;
            }

            Field tableField = map.getClass().getDeclaredField("table");
            tableField.setAccessible(true);
            Object[] table = (Object[]) tableField.get(map);
            if (table == null) {
                return 0;
            }

            for (Object entry : table) {
                if (entry != null) {
                    Field valueField = entry.getClass().getDeclaredField("value");
                    valueField.setAccessible(true);
                    Object value = valueField.get(entry);

                    if (value != null && isLoadedBy(value.getClass(), targetClassLoader)) {
                        valueField.set(entry, null);
                        cleared++;
                    }
                }
            }
        } catch (Throwable ignored) {
            // JVM security manager or strong encapsulation fallback
        }
        return cleared;
    }

    private boolean isLoadedBy(Class<?> clazz, ClassLoader targetClassLoader) {
        ClassLoader cl = clazz.getClassLoader();
        while (cl != null) {
            if (cl == targetClassLoader) {
                return true;
            }
            cl = cl.getParent();
        }
        return false;
    }

    private Thread[] getAllActiveThreads() {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        int count = rootGroup.activeCount();
        Thread[] threads = new Thread[count + 32];
        int actual = rootGroup.enumerate(threads, true);
        Thread[] result = new Thread[actual];
        System.arraycopy(threads, 0, result, 0, actual);
        return result;
    }
}
