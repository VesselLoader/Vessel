package dev.vessel.kernel.diagnostic;

import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.util.Map;
import java.util.Objects;

/**
 * Generates Smart Blame crash reports (Decision 8 &amp; Lesson L39) that open with a complete table
 * of loaded modules and pinpoint the exact culprit {@code (source loader, module id)} and JAR file
 * path by walking the exception stack trace.
 *
 * @version 1.0.0
 */
public final class VesselCrashReportGenerator {

    /**
     * Generates a structured Smart Blame crash report from an unhandled exception.
     *
     * @param exception              the unhandled throwable
     * @param classPrefixToModuleMap map from package/class prefix to owning
     *                               {@link VesselModuleIdentifier}
     * @param moduleJarPaths         map from {@link VesselModuleIdentifier} to physical JAR path
     * @return the formatted Smart Blame crash report string
     */
    public String generateSmartBlameCrashReport(
            Throwable exception,
            Map<String, VesselModuleIdentifier> classPrefixToModuleMap,
            Map<VesselModuleIdentifier, String> moduleJarPaths) {
        Objects.requireNonNull(exception, "exception must not be null");
        Objects.requireNonNull(
                classPrefixToModuleMap, "classPrefixToModuleMap must not be null");
        Objects.requireNonNull(moduleJarPaths, "moduleJarPaths must not be null");

        VesselModuleIdentifier blamedModule = findBlamedModule(exception, classPrefixToModuleMap);
        String blamedJar = blamedModule != null
                ? moduleJarPaths.getOrDefault(blamedModule, "unknown.jar")
                : "kernel/vanilla";

        StringBuilder report = new StringBuilder(1024);
        report.append("================================================================\n");
        report.append("           VESSEL SOVEREIGN MICROKERNEL CRASH REPORT            \n");
        report.append("================================================================\n");
        if (blamedModule != null) {
            report.append("SMART BLAME CULPRIT : (")
                    .append(blamedModule.loader().getId())
                    .append(", ")
                    .append(blamedModule.namespace())
                    .append("@")
                    .append(blamedModule.version())
                    .append(")\n");
            report.append("CULPRIT JAR PATH    : ").append(blamedJar).append("\n");
        } else {
            report.append("SMART BLAME CULPRIT : (unattributed / core runtime)\n");
        }
        report.append("EXCEPTION           : ")
                .append(exception.getClass().getName())
                .append(": ")
                .append(exception.getMessage())
                .append("\n");
        report.append("----------------------------------------------------------------\n");
        report.append("LOADED MODULE TABLE :\n");
        for (Map.Entry<VesselModuleIdentifier, String> entry : moduleJarPaths.entrySet()) {
            VesselModuleIdentifier id = entry.getKey();
            report.append("  - [")
                    .append(id.loader().getId())
                    .append("] ")
                    .append(id.namespace())
                    .append("@")
                    .append(id.version())
                    .append(" -> ")
                    .append(entry.getValue())
                    .append("\n");
        }
        report.append("================================================================\n");
        return report.toString();
    }

    private static VesselModuleIdentifier findBlamedModule(
            Throwable current,
            Map<String, VesselModuleIdentifier> classPrefixToModuleMap) {
        Throwable cursor = current;
        while (cursor != null) {
            for (StackTraceElement frame : cursor.getStackTrace()) {
                String className = frame.getClassName();
                for (Map.Entry<String, VesselModuleIdentifier> entry
                        : classPrefixToModuleMap.entrySet()) {
                    if (className.startsWith(entry.getKey())) {
                        return entry.getValue();
                    }
                }
            }
            cursor = cursor.getCause();
        }
        return null;
    }
}
