package dev.vessel.kernel.adapter.fabric;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Invokes Quilt-native mod entrypoints ({@code org.quiltmc.loader.api.ModInitializer}) with support
 * for {@code ModContainer} parameter passing and fallback zero-arg invocation.
 *
 * <p>Quells: ECO-061..070.</p>
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class QuiltModInitializerInvoker {

    /**
     * Canonical interface name for Quilt mod initialization.
     */
    public static final String QUILT_MOD_INITIALIZER_INTERFACE = "org.quiltmc.loader.api.ModInitializer";

    /**
     * Canonical interface name for Quilt dedicated server initialization.
     */
    public static final String QUILT_SERVER_INITIALIZER_INTERFACE = "org.quiltmc.loader.api.entrypoint.server.DedicatedServerModInitializer";

    /**
     * Common initialization method name.
     */
    public static final String METHOD_ON_INITIALIZE = "onInitialize";

    /**
     * Dedicated server initialization method name.
     */
    public static final String METHOD_ON_INITIALIZE_SERVER = "onInitializeServer";

    private final List<QuiltInvocationResult> invocationHistory = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new QuiltModInitializerInvoker.
     */
    public QuiltModInitializerInvoker() {
    }

    /**
     * Invokes the Quilt {@code onInitialize} entrypoint with an optional mod container argument.
     *
     * @param modId the unique identifier of the Quilt mod
     * @param className the fully qualified entrypoint class name
     * @param modContainer the mod container object to pass to the initializer, or null if zero-arg
     * @param classLoader the classloader containing the entrypoint class
     * @return invocation result record
     */
    public QuiltInvocationResult invokeQuiltModInitializer(
            String modId,
            String className,
            Object modContainer,
            ClassLoader classLoader) {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(classLoader, "classLoader cannot be null");

        QuiltInvocationResult result = invokeReflectiveWithFallback(
                modId, className, METHOD_ON_INITIALIZE, modContainer, classLoader);
        invocationHistory.add(result);
        return result;
    }

    /**
     * Invokes the Quilt dedicated server entrypoint with an optional mod container argument.
     *
     * @param modId the unique identifier of the Quilt mod
     * @param className the fully qualified entrypoint class name
     * @param modContainer the mod container object to pass to the initializer, or null if zero-arg
     * @param classLoader the classloader containing the entrypoint class
     * @return invocation result record
     */
    public QuiltInvocationResult invokeQuiltServerInitializer(
            String modId,
            String className,
            Object modContainer,
            ClassLoader classLoader) {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(classLoader, "classLoader cannot be null");

        QuiltInvocationResult result = invokeReflectiveWithFallback(
                modId, className, METHOD_ON_INITIALIZE_SERVER, modContainer, classLoader);
        invocationHistory.add(result);
        return result;
    }

    /**
     * Directly invokes a pre-instantiated Quilt entrypoint instance.
     *
     * @param modId the unique identifier of the Quilt mod
     * @param instance the entrypoint instance
     * @param methodName the target method name ({@value #METHOD_ON_INITIALIZE} or {@value #METHOD_ON_INITIALIZE_SERVER})
     * @param modContainer optional mod container argument
     * @return invocation result record
     */
    public QuiltInvocationResult invokeInstance(
            String modId,
            Object instance,
            String methodName,
            Object modContainer) {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(instance, "instance cannot be null");
        Objects.requireNonNull(methodName, "methodName cannot be null");

        String className = instance.getClass().getName();
        try {
            boolean invoked = tryInvokeOneArgMethod(instance, methodName, modContainer);
            if (!invoked) {
                Method zeroArgMethod = instance.getClass().getMethod(methodName);
                zeroArgMethod.setAccessible(true);
                zeroArgMethod.invoke(instance);
            }
            QuiltInvocationResult result = QuiltInvocationResult.success(modId, className);
            invocationHistory.add(result);
            return result;
        } catch (NoSuchMethodException e) {
            QuiltInvocationResult result = QuiltInvocationResult.notFound(modId, className, e);
            invocationHistory.add(result);
            return result;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            QuiltInvocationResult result = QuiltInvocationResult.failed(modId, className, cause);
            invocationHistory.add(result);
            return result;
        } catch (ReflectiveOperationException | SecurityException e) {
            QuiltInvocationResult result = QuiltInvocationResult.failed(modId, className, e);
            invocationHistory.add(result);
            return result;
        }
    }

    private QuiltInvocationResult invokeReflectiveWithFallback(
            String modId,
            String className,
            String methodName,
            Object modContainer,
            ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(className, true, classLoader);
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();

            boolean invoked = tryInvokeOneArgMethod(instance, methodName, modContainer);
            if (!invoked) {
                Method zeroArgMethod = clazz.getMethod(methodName);
                zeroArgMethod.setAccessible(true);
                zeroArgMethod.invoke(instance);
            }

            return QuiltInvocationResult.success(modId, className);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return QuiltInvocationResult.notFound(modId, className, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return QuiltInvocationResult.failed(modId, className, cause);
        } catch (ReflectiveOperationException | SecurityException e) {
            return QuiltInvocationResult.failed(modId, className, e);
        }
    }

    private boolean tryInvokeOneArgMethod(Object instance, String methodName, Object modContainer)
            throws ReflectiveOperationException {
        if (modContainer == null) {
            return false;
        }
        Method[] methods = instance.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m.getName().equals(methodName) && m.getParameterCount() == 1) {
                Class<?> paramType = m.getParameterTypes()[0];
                if (paramType.isInstance(modContainer) || paramType.isAssignableFrom(modContainer.getClass())) {
                    m.setAccessible(true);
                    m.invoke(instance, modContainer);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns an unmodifiable list of Quilt invocation results.
     *
     * @return unmodifiable list
     */
    public List<QuiltInvocationResult> getInvocationHistory() {
        return Collections.unmodifiableList(invocationHistory);
    }

    /**
     * Clears invocation history.
     */
    public void clearHistory() {
        invocationHistory.clear();
    }
}
