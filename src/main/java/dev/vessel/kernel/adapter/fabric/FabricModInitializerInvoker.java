package dev.vessel.kernel.adapter.fabric;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Safely invokes Fabric mod entrypoints ({@code ModInitializer}, {@code DedicatedServerModInitializer})
 * across lifecycle phases in dedicated server environments.
 *
 * <p>Quells: ERR-35, ECO-051..060.</p>
 * <p>Strictly enforces Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FabricModInitializerInvoker {

    /**
     * Canonical interface name for common Fabric initialization.
     */
    public static final String FABRIC_MOD_INITIALIZER_INTERFACE = "net.fabricmc.api.ModInitializer";

    /**
     * Canonical interface name for dedicated server Fabric initialization.
     */
    public static final String FABRIC_SERVER_INITIALIZER_INTERFACE = "net.fabricmc.api.DedicatedServerModInitializer";

    /**
     * Canonical interface name for client-side Fabric initialization.
     */
    public static final String FABRIC_CLIENT_INITIALIZER_INTERFACE = "net.fabricmc.api.ClientModInitializer";

    /**
     * Method name for common initialization.
     */
    public static final String METHOD_ON_INITIALIZE = "onInitialize";

    /**
     * Method name for dedicated server initialization.
     */
    public static final String METHOD_ON_INITIALIZE_SERVER = "onInitializeServer";

    /**
     * Method name for client initialization.
     */
    public static final String METHOD_ON_INITIALIZE_CLIENT = "onInitializeClient";

    /**
     * Canonical phase name for main initialization.
     */
    public static final String PHASE_MAIN = "main";

    /**
     * Canonical phase name for server initialization.
     */
    public static final String PHASE_SERVER = "server";

    /**
     * Canonical phase name for client initialization.
     */
    public static final String PHASE_CLIENT = "client";

    private final List<FabricInvocationResult> invocationHistory = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new FabricModInitializerInvoker.
     */
    public FabricModInitializerInvoker() {
    }

    /**
     * Invokes the common {@code ModInitializer#onInitialize()} entrypoint for a given mod.
     *
     * @param modId the mod identifier
     * @param className the entrypoint class name
     * @param classLoader the classloader containing the mod classes
     * @return invocation result record
     */
    public FabricInvocationResult invokeMainEntrypoint(String modId, String className, ClassLoader classLoader) {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(classLoader, "classLoader cannot be null");

        FabricInvocationResult result = invokeReflective(modId, className, METHOD_ON_INITIALIZE, PHASE_MAIN, classLoader);
        invocationHistory.add(result);
        return result;
    }

    /**
     * Invokes the dedicated server {@code DedicatedServerModInitializer#onInitializeServer()} entrypoint.
     *
     * @param modId the mod identifier
     * @param className the entrypoint class name
     * @param classLoader the classloader containing the mod classes
     * @return invocation result record
     */
    public FabricInvocationResult invokeServerEntrypoint(String modId, String className, ClassLoader classLoader) {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(classLoader, "classLoader cannot be null");

        FabricInvocationResult result = invokeReflective(modId, className, METHOD_ON_INITIALIZE_SERVER, PHASE_SERVER, classLoader);
        invocationHistory.add(result);
        return result;
    }

    /**
     * Skips client-only entrypoints safely on dedicated server architectures.
     *
     * @param modId the mod identifier
     * @param className the entrypoint class name
     * @return skipped invocation result
     */
    public FabricInvocationResult invokeClientEntrypoint(String modId, String className) {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(className, "className cannot be null");

        FabricInvocationResult result = FabricInvocationResult.skipped(modId, className, PHASE_CLIENT);
        invocationHistory.add(result);
        return result;
    }

    /**
     * Directly invokes an already-instantiated entrypoint object.
     *
     * @param modId the mod identifier
     * @param instance the instantiated entrypoint object
     * @param phase the lifecycle phase name
     * @return invocation result record
     */
    public FabricInvocationResult invokeInstance(String modId, Object instance, String phase) {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(instance, "instance cannot be null");
        Objects.requireNonNull(phase, "phase cannot be null");

        String className = instance.getClass().getName();
        String targetMethod = resolveMethodForPhase(phase);

        try {
            Method method = instance.getClass().getMethod(targetMethod);
            method.setAccessible(true);
            method.invoke(instance);
            FabricInvocationResult result = FabricInvocationResult.success(modId, className, phase);
            invocationHistory.add(result);
            return result;
        } catch (NoSuchMethodException e) {
            FabricInvocationResult result = FabricInvocationResult.notFound(modId, className, phase, e);
            invocationHistory.add(result);
            return result;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            FabricInvocationResult result = FabricInvocationResult.failed(modId, className, phase, cause);
            invocationHistory.add(result);
            return result;
        } catch (ReflectiveOperationException | SecurityException e) {
            FabricInvocationResult result = FabricInvocationResult.failed(modId, className, phase, e);
            invocationHistory.add(result);
            return result;
        }
    }

    /**
     * Reflectively instantiates and invokes an entrypoint class.
     *
     * @param modId the mod identifier
     * @param className the entrypoint class name
     * @param methodName the method name to invoke
     * @param phase the phase name
     * @param classLoader the classloader to resolve the class
     * @return invocation result record
     */
    public FabricInvocationResult invokeReflective(
            String modId,
            String className,
            String methodName,
            String phase,
            ClassLoader classLoader) {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(methodName, "methodName cannot be null");
        Objects.requireNonNull(phase, "phase cannot be null");
        Objects.requireNonNull(classLoader, "classLoader cannot be null");

        try {
            Class<?> clazz = Class.forName(className, true, classLoader);
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();

            Method method = clazz.getMethod(methodName);
            method.setAccessible(true);
            method.invoke(instance);

            return FabricInvocationResult.success(modId, className, phase);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return FabricInvocationResult.notFound(modId, className, phase, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return FabricInvocationResult.failed(modId, className, phase, cause);
        } catch (ReflectiveOperationException | SecurityException e) {
            return FabricInvocationResult.failed(modId, className, phase, e);
        }
    }

    /**
     * Resolves the canonical method name for a given lifecycle phase.
     *
     * @param phase the phase name
     * @return target method name
     */
    private static String resolveMethodForPhase(String phase) {
        if (PHASE_SERVER.equalsIgnoreCase(phase)) {
            return METHOD_ON_INITIALIZE_SERVER;
        } else if (PHASE_CLIENT.equalsIgnoreCase(phase)) {
            return METHOD_ON_INITIALIZE_CLIENT;
        } else {
            return METHOD_ON_INITIALIZE;
        }
    }

    /**
     * Retrieves an unmodifiable view of all completed invocations.
     *
     * @return unmodifiable list of results
     */
    public List<FabricInvocationResult> getInvocationHistory() {
        return Collections.unmodifiableList(invocationHistory);
    }

    /**
     * Clears all recorded invocation history.
     */
    public void clearHistory() {
        invocationHistory.clear();
    }
}
