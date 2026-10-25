package dev.vessel.kernel.sanitizer;

import java.beans.Introspector;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Proactive Metaspace leak sanitizer detecting and purging abandoned ClassLoader references,
 * unhooking persistent ThreadLocal bindings, and flushing JVM static caches upon plugin or mod reload.
 *
 * <p>Remediates ERR-28 and JVM-0001..0200 by preventing Metaspace OutOfMemoryError crashes.</p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MetaspaceMemoryLeakSanitizer {

    public static final long ESTIMATED_LOADER_METASPACE_BYTES = 512L * 1024L; // 512KB per unhooked loader estimate
    private static final String THREAD_LOCALS_FIELD = "threadLocals";
    private static final String INHERITABLE_THREAD_LOCALS_FIELD = "inheritableThreadLocals";
    private static final String TABLE_FIELD = "table";

    private final List<TrackedClassLoaderEntry> trackedLoaders = new CopyOnWriteArrayList<>();
    private final Map<String, Boolean> knownLeakSignatures = new ConcurrentHashMap<>();

    public MetaspaceMemoryLeakSanitizer() {
    }

    /**
     * Registers a ClassLoader to be monitored for unreleased references during lifecycle transitions.
     *
     * @param loaderName diagnostic name of the loader
     * @param classLoader instance to monitor
     */
    public void trackClassLoader(String loaderName, ClassLoader classLoader) {
        Objects.requireNonNull(loaderName, "loaderName must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        trackedLoaders.add(new TrackedClassLoaderEntry(loaderName, new WeakReference<>(classLoader), System.currentTimeMillis()));
    }

    /**
     * Returns the count of actively tracked classloader entries.
     *
     * @return count of tracked entries
     */
    public int getTrackedCount() {
        return trackedLoaders.size();
    }

    /**
     * Executes a full sanitization pass over tracked classloaders and active threads.
     *
     * @return sanitization report
     */
    public MetaspaceSanitizeReport sanitize() {
        long startNanos = System.nanoTime();
        int leakedCount = 0;
        int clearedThreadLocals = 0;
        int clearedStaticCaches = 0;

        List<ClassLoader> activeTargets = new ArrayList<>();
        for (TrackedClassLoaderEntry entry : trackedLoaders) {
            ClassLoader loader = entry.reference().get();
            if (loader != null) {
                activeTargets.add(loader);
                leakedCount++;
            }
        }

        // 1. Flush JVM Introspector caches
        try {
            Introspector.flushCaches();
            clearedStaticCaches++;
        } catch (Throwable ignored) {
        }

        // 2. Scan and unhook ThreadLocal bindings for all active threads
        if (!activeTargets.isEmpty()) {
            Thread[] activeThreads = getAllActiveThreads();
            for (Thread thread : activeThreads) {
                if (thread != null) {
                    clearedThreadLocals += sanitizeThread(thread, activeTargets);
                }
            }
        }

        // 3. Request GC to accelerate Metaspace reclamation
        System.gc();

        // 4. Recalculate remaining leaked loaders
        int remainingLeaked = 0;
        for (TrackedClassLoaderEntry entry : trackedLoaders) {
            if (!entry.isCollected()) {
                remainingLeaked++;
            }
        }

        int resolvedLoaders = leakedCount - remainingLeaked;
        long estimatedReclaimed = resolvedLoaders * ESTIMATED_LOADER_METASPACE_BYTES;
        long elapsedNanos = System.nanoTime() - startNanos;

        return new MetaspaceSanitizeReport(
                trackedLoaders.size(),
                remainingLeaked,
                clearedThreadLocals,
                clearedStaticCaches,
                estimatedReclaimed,
                elapsedNanos
        );
    }

    private int sanitizeThread(Thread thread, List<ClassLoader> targets) {
        int purged = 0;
        purged += purgeThreadLocalMap(thread, THREAD_LOCALS_FIELD, targets);
        purged += purgeThreadLocalMap(thread, INHERITABLE_THREAD_LOCALS_FIELD, targets);
        return purged;
    }

    private int purgeThreadLocalMap(Thread thread, String fieldName, List<ClassLoader> targets) {
        int cleared = 0;
        try {
            Field threadLocalsField = Thread.class.getDeclaredField(fieldName);
            threadLocalsField.setAccessible(true);
            Object threadLocalMap = threadLocalsField.get(thread);
            if (threadLocalMap == null) {
                return 0;
            }

            Field tableField = threadLocalMap.getClass().getDeclaredField(TABLE_FIELD);
            tableField.setAccessible(true);
            Object table = tableField.get(threadLocalMap);
            if (table == null) {
                return 0;
            }

            int length = Array.getLength(table);
            for (int i = 0; i < length; i++) {
                Object entry = Array.get(table, i);
                if (entry != null) {
                    if (isEntryAssociatedWithLoaders(entry, targets)) {
                        Array.set(table, i, null);
                        cleared++;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return cleared;
    }

    private boolean isEntryAssociatedWithLoaders(Object entry, List<ClassLoader> targets) {
        try {
            Field valueField = entry.getClass().getDeclaredField("value");
            valueField.setAccessible(true);
            Object value = valueField.get(entry);
            if (value != null) {
                ClassLoader valueLoader = value.getClass().getClassLoader();
                for (ClassLoader target : targets) {
                    if (valueLoader == target) {
                        return true;
                    }
                }
            }

            if (entry instanceof Reference<?> ref) {
                Object key = ref.get();
                if (key != null) {
                    ClassLoader keyLoader = key.getClass().getClassLoader();
                    for (ClassLoader target : targets) {
                        if (keyLoader == target) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static Thread[] getAllActiveThreads() {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        Thread[] threads = new Thread[rootGroup.activeCount() * 2 + 16];
        int count = rootGroup.enumerate(threads, true);
        Thread[] result = new Thread[count];
        System.arraycopy(threads, 0, result, 0, count);
        return result;
    }
}
