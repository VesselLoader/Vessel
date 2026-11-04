package dev.vessel.bootstrap.realm;

import java.util.Objects;
import java.util.Set;

/**
 * Filter policy determining package visibility and export boundaries for a module realm.
 *
 * <p>Prevents dependency leakage by isolating internal shaded libraries (e.g. shaded protobuf,
 * guava, gson) within a mod's private realm while exporting public mod APIs.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ModuleRealmPackageExportFilter {

    private static final Set<String> PRIVATE_PREFIXES = Set.of(
            "META-INF.",
            "dev.vessel.shadow.",
            "com.google.protobuf.",
            "org.apache.commons.",
            "com.electronwill.nightconfig."
    );

    /**
     * Constructs a package export filter.
     */
    public ModuleRealmPackageExportFilter() {
    }

    /**
     * Checks if a class is allowed to be exported to other dependent module realms.
     *
     * @param className binary class name (e.g. {@code "com.example.mod.MyApi"})
     * @return true if class is public/exported; false if internal shaded
     */
    public boolean isExported(String className) {
        Objects.requireNonNull(className, "className cannot be null");
        for (String prefix : PRIVATE_PREFIXES) {
            if (className.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }
}
