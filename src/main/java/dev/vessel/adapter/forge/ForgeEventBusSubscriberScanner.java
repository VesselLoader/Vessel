package dev.vessel.adapter.forge;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import net.minecraftforge.common.MinecraftForge;

/**
 * Scans candidate classes within Forge mod JARs for {@code @Mod.EventBusSubscriber} annotations
 * or static methods annotated with {@code @SubscribeEvent}, automatically registering qualifying
 * dedicated-server classes to {@link MinecraftForge#EVENT_BUS}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class ForgeEventBusSubscriberScanner {

    public static final String SUBSCRIBER_ANNOTATION_NAME =
            "net.minecraftforge.fml.common.Mod$EventBusSubscriber";
    public static final String SUBSCRIBE_EVENT_ANNOTATION_NAME =
            "net.minecraftforge.eventbus.api.SubscribeEvent";

    /**
     * Scans and registers any {@code @Mod.EventBusSubscriber} or static subscriber classes
     * from {@code candidateClassNames} loaded by {@code classLoader}.
     *
     * @param candidateClassNames candidate class names found in the mod JAR
     * @param classLoader         live transforming classloader
     * @return count of subscriber classes successfully registered to {@link MinecraftForge#EVENT_BUS}
     */
    public int scanAndRegisterSubscribers(
            List<String> candidateClassNames,
            ClassLoader classLoader) {
        Objects.requireNonNull(candidateClassNames, "candidateClassNames must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");

        int registeredCount = 0;
        for (String className : candidateClassNames) {
            if (className == null || className.isBlank() || className.startsWith("java.")) {
                continue;
            }
            try {
                Class<?> clazz = Class.forName(className, false, classLoader);
                if (isEventBusSubscriber(clazz)) {
                    MinecraftForge.EVENT_BUS.register(clazz);
                    registeredCount++;
                }
            } catch (Throwable ignored) {
            }
        }
        return registeredCount;
    }

    /**
     * Checks whether a class is marked as an event bus subscriber via annotation or static handler methods.
     * Client-only subscribers (targeting only {@code Dist.CLIENT}) are skipped on dedicated server.
     *
     * @param clazz candidate class
     * @return {@code true} if qualifying as an event bus subscriber
     */
    public boolean isEventBusSubscriber(Class<?> clazz) {
        if (clazz == null || clazz.isInterface() || clazz.isAnnotation() || clazz.isEnum()) {
            return false;
        }

        try {
            // Check 1: Declared annotations matching @Mod.EventBusSubscriber
            for (Annotation annotation : clazz.getAnnotations()) {
                String annoName = annotation.annotationType().getName();
                if (SUBSCRIBER_ANNOTATION_NAME.equals(annoName)
                        || annoName.endsWith("EventBusSubscriber")) {
                    if (isClientOnlySubscriber(annotation)) {
                        return false;
                    }
                    return true;
                }
            }

            // Check 2: Contains public static methods annotated with @SubscribeEvent
            for (Method m : clazz.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1) {
                    for (Annotation a : m.getAnnotations()) {
                        String aName = a.annotationType().getName();
                        if (SUBSCRIBE_EVENT_ANNOTATION_NAME.equals(aName)
                                || aName.endsWith("SubscribeEvent")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }

    private static boolean isClientOnlySubscriber(Annotation annotation) {
        try {
            Method busMethod = annotation.annotationType().getMethod("bus");
            Object busVal = busMethod.invoke(annotation);
            if (busVal != null && "MOD".equals(busVal.toString())) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        try {
            Method valMethod = null;
            try {
                valMethod = annotation.annotationType().getMethod("value");
            } catch (NoSuchMethodException e) {
                try {
                    valMethod = annotation.annotationType().getMethod("dist");
                } catch (NoSuchMethodException ignored) {
                }
            }
            if (valMethod != null) {
                Object distArray = valMethod.invoke(annotation);
                if (distArray instanceof Object[] dists && dists.length > 0) {
                    boolean hasServer = false;
                    for (Object d : dists) {
                        if (d != null) {
                            String s = d.toString();
                            if ("DEDICATED_SERVER".equals(s) || "SERVER".equals(s)) {
                                hasServer = true;
                                break;
                            }
                        }
                    }
                    if (!hasServer) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
