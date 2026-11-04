package dev.vessel.kernel.runtime;

import java.lang.Runtime.Version;
import java.util.Objects;

/**
 * Asserts host JVM runtime compatibility against required Java LTS baselines.
 * Ensures the running JVM feature version satisfies minimum requirements for Minecraft modern servers.
 *
 * @version 1.0.0
 */
public final class JavaLtsRuntimeCompatibilityAsserter {

    public static final int MINIMUM_LTS_FEATURE_VERSION = 21;
    public static final int TARGET_FUTURE_LTS_VERSION = 25;

    public boolean isLtsRuntimeCompatible(int featureVersion) {
        return featureVersion >= MINIMUM_LTS_FEATURE_VERSION;
    }

    public int detectHostJvmFeatureVersion() {
        Version version = Runtime.version();
        return version.feature();
    }

    public boolean verifyHostJvmMeetsBaseline() {
        int feature = detectHostJvmFeatureVersion();
        return isLtsRuntimeCompatible(feature);
    }

    public String generateCompatibilityReport(String targetServerVersion) {
        Objects.requireNonNull(targetServerVersion, "targetServerVersion cannot be null");
        int hostFeature = detectHostJvmFeatureVersion();
        boolean compliant = isLtsRuntimeCompatible(hostFeature);
        return "[VESSEL/JVM-LTS] Host Java: " + hostFeature
                + " | Minimum Required: Java " + MINIMUM_LTS_FEATURE_VERSION + " LTS"
                + " | Target Server: " + targetServerVersion
                + " | Status: " + (compliant ? "COMPLIANT" : "INCOMPATIBLE");
    }
}
