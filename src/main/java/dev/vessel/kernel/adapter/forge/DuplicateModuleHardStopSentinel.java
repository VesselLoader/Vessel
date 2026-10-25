package dev.vessel.kernel.adapter.forge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-boot sentinel enforcing strict isolation against duplicate module namespace collisions.
 * <p>
 * Under Architectural Decision 8 and Decision 17, duplicate namespaces on the same loader type
 * represent fatal ambiguities that must trigger an immediate Hard Stop before bytecode initialization.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DuplicateModuleHardStopSentinel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateModuleHardStopSentinel.class);

    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";

    /**
     * Default constructor for sentinel validator.
     */
    public DuplicateModuleHardStopSentinel() {
    }

    /**
     * Scans an inventory of candidate modules and immediately triggers a hard stop if duplicates exist.
     *
     * @param candidates list of candidate modules to evaluate
     * @return clean scan report if no duplicates were discovered
     * @throws DuplicateModuleHardStopException if one or more duplicate namespaces are detected
     */
    public DuplicateScanReport scanAndEnforce(List<ModuleNamespaceDescriptor> candidates) {
        DuplicateScanReport report = scanOnly(candidates);
        if (report.hasDuplicates()) {
            emitRedCollisionBanner(report);
            Map.Entry<String, List<ModuleNamespaceDescriptor>> firstConflict =
                    report.duplicatesByNamespace().entrySet().iterator().next();
            String ns = firstConflict.getKey();
            List<ModuleNamespaceDescriptor> list = firstConflict.getValue();
            String loader = list.get(0).loaderType();
            String message = String.format("Found %d collisions for namespace '%s'. See log output above.",
                    list.size(), ns);
            throw new DuplicateModuleHardStopException(ns, loader, message);
        }
        return report;
    }

    /**
     * Inspects candidate module inventory without triggering immediate hard stop termination.
     *
     * @param candidates list of candidate modules
     * @return diagnostic scan report
     */
    public DuplicateScanReport scanOnly(List<ModuleNamespaceDescriptor> candidates) {
        Objects.requireNonNull(candidates, "candidates must not be null");

        // Map key format: loader:namespace
        Map<String, List<ModuleNamespaceDescriptor>> grouped = new LinkedHashMap<>();
        for (ModuleNamespaceDescriptor descriptor : candidates) {
            String compositeKey = descriptor.loaderType().trim().toUpperCase(Locale.ROOT)
                    + ":" + descriptor.namespaceId().trim().toLowerCase(Locale.ROOT);
            List<ModuleNamespaceDescriptor> bucket = grouped.computeIfAbsent(compositeKey, k -> new ArrayList<>());
            bucket.add(descriptor);
        }

        Map<String, List<ModuleNamespaceDescriptor>> duplicates = new LinkedHashMap<>();
        int totalCollisions = 0;

        for (Map.Entry<String, List<ModuleNamespaceDescriptor>> entry : grouped.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
                totalCollisions += entry.getValue().size();
            }
        }

        return new DuplicateScanReport(
                !duplicates.isEmpty(),
                totalCollisions,
                duplicates
        );
    }

    private void emitRedCollisionBanner(DuplicateScanReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(ANSI_RED);
        sb.append("\n================================================================================\n");
        sb.append(" [VESSEL HARD STOP] FATAL DUPLICATE MODULE NAMESPACE COLLISION (DECISION 8 & 17)\n");
        sb.append("================================================================================\n");
        for (Map.Entry<String, List<ModuleNamespaceDescriptor>> entry : report.duplicatesByNamespace().entrySet()) {
            sb.append(" Colliding Namespace: ").append(entry.getKey()).append("\n");
            for (ModuleNamespaceDescriptor desc : entry.getValue()) {
                sb.append("   - Loader: ").append(desc.loaderType())
                        .append(" | Version: ").append(desc.version())
                        .append(" | Jar: ").append(desc.jarOrigin()).append("\n");
            }
        }
        sb.append("================================================================================\n");
        sb.append(ANSI_RESET);

        String banner = sb.toString();
        LOGGER.error("{}", banner);
        System.err.println(banner);
    }
}
