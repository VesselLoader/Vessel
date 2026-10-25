package dev.vessel.kernel.access;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Virtualizes and unlocks Java Platform Module System (JPMS) package boundaries in Java 21 LTS
 * without requiring explicit '--add-opens' CLI launch parameters.
 * <p>
 * Leverages trusted MethodHandles lookup unlocked via Unsafe to invoke private Module#implAddOpens,
 * allowing legacy multi-loader reflection calls to sun.misc, java.lang, and internal JVM machinery.
 * </p>
 * <p>
 * Directly resolves ERR-71, ERR-72, and MAP-REF-0001..0350.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class JpmsModuleBoundaryVirtualizer {

    public static final String ALL_UNNAMED_TARGET = "ALL-UNNAMED";
    public static final String JAVA_BASE_NAME = "java.base";

    private static final String[] CRITICAL_BASE_PACKAGES = {
            "java.lang",
            "java.lang.reflect",
            "java.lang.invoke",
            "java.io",
            "java.nio",
            "java.nio.channels",
            "java.util",
            "java.util.concurrent",
            "sun.misc",
            "sun.reflect",
            "jdk.internal.misc",
            "jdk.internal.loader",
            "jdk.internal.reflect"
    };

    private static final Unsafe THE_UNSAFE;
    private static final MethodHandles.Lookup TRUSTED_LOOKUP;
    private static final MethodHandle IMPL_ADD_OPENS_HANDLE;

    static {
        Unsafe unsafe = null;
        MethodHandles.Lookup lookup = null;
        MethodHandle addOpensHandle = null;

        try {
            Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            unsafe = (Unsafe) theUnsafeField.get(null);

            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            long offset = unsafe.staticFieldOffset(implLookupField);
            lookup = (MethodHandles.Lookup) unsafe.getObject(MethodHandles.Lookup.class, offset);

            Method implAddOpens = Module.class.getDeclaredMethod("implAddOpens", String.class, Module.class);
            addOpensHandle = lookup.unreflect(implAddOpens);
        } catch (Throwable t) {
            // Fallback for restricted security profiles: lookup remains null and will report soft failure
        }

        THE_UNSAFE = unsafe;
        TRUSTED_LOOKUP = lookup;
        IMPL_ADD_OPENS_HANDLE = addOpensHandle;
    }

    /**
     * Opens a specific package from a source module to a target module.
     *
     * @param sourceModule the declaring module
     * @param packageName package to open (dot-separated)
     * @param targetModule target module receiving access
     * @return outcome result of the virtualization
     */
    public JpmsVirtualizationResult openModule(Module sourceModule, String packageName, Module targetModule) {
        Objects.requireNonNull(sourceModule, "sourceModule cannot be null");
        Objects.requireNonNull(packageName, "packageName cannot be null");
        Objects.requireNonNull(targetModule, "targetModule cannot be null");

        String targetName = targetModule.isNamed() ? targetModule.getName() : ALL_UNNAMED_TARGET;

        if (!sourceModule.getPackages().contains(packageName)) {
            return new JpmsVirtualizationResult(
                    sourceModule.getName(),
                    packageName,
                    targetName,
                    false,
                    "Package '" + packageName + "' does not exist in module " + sourceModule.getName()
            );
        }

        if (sourceModule.isOpen(packageName, targetModule)) {
            return new JpmsVirtualizationResult(
                    sourceModule.getName(),
                    packageName,
                    targetName,
                    true,
                    "Package was already open"
            );
        }

        if (IMPL_ADD_OPENS_HANDLE == null) {
            return new JpmsVirtualizationResult(
                    sourceModule.getName(),
                    packageName,
                    targetName,
                    false,
                    "Trusted lookup is unavailable in current JVM environment"
            );
        }

        try {
            IMPL_ADD_OPENS_HANDLE.invoke(sourceModule, packageName, targetModule);
            return new JpmsVirtualizationResult(
                    sourceModule.getName(),
                    packageName,
                    targetName,
                    true,
                    "Successfully opened via trusted MethodHandle"
            );
        } catch (Throwable t) {
            return new JpmsVirtualizationResult(
                    sourceModule.getName(),
                    packageName,
                    targetName,
                    false,
                    "Failed to open package: " + t.getMessage()
            );
        }
    }

    /**
     * Opens a java.base package to the caller's unnamed module.
     *
     * @param packageName package name within java.base
     * @return outcome result
     */
    public JpmsVirtualizationResult openBasePackageToAllUnnamed(String packageName) {
        Module baseModule = Object.class.getModule();
        Module unnamedModule = getClass().getClassLoader().getUnnamedModule();
        return openModule(baseModule, packageName, unnamedModule);
    }

    /**
     * Opens all critical java.base packages to ALL-UNNAMED.
     *
     * @return list of virtualization results
     */
    public List<JpmsVirtualizationResult> openAllBaseInternalPackages() {
        Module baseModule = Object.class.getModule();
        Module unnamedModule = getClass().getClassLoader().getUnnamedModule();
        java.util.Set<String> existingPackages = baseModule.getPackages();

        List<JpmsVirtualizationResult> results = new ArrayList<>();
        for (String pkg : CRITICAL_BASE_PACKAGES) {
            if (existingPackages.contains(pkg)) {
                results.add(openModule(baseModule, pkg, unnamedModule));
            }
        }
        return results;
    }

    /**
     * Checks whether trusted native access is operational in the current JVM process.
     *
     * @return true if trusted lookup handle is initialized
     */
    public boolean isTrustedAccessAvailable() {
        return IMPL_ADD_OPENS_HANDLE != null && THE_UNSAFE != null;
    }
}
