package dev.vessel.kernel.adapter.bukkit;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable metadata descriptor representing a parsed Bukkit or Paper plugin manifest.
 *
 * @param name canonical plugin name
 * @param version plugin version string
 * @param mainClass entrypoint class name
 * @param bootstrapperClass optional Paper bootstrapper class
 * @param loaderClass optional Paper plugin loader class
 * @param apiVersion targeted Bukkit/Paper API version
 * @param kind descriptor specification format
 * @param requiredDependencies list of hard dependency plugin names
 * @param softDependencies list of soft dependency plugin names
 * @param loadBefore list of plugin names that should be loaded after this plugin
 * @param authors list of authors
 * @param description plugin description
 * @param website plugin website URL
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record PaperPluginDescriptor(
        String name,
        String version,
        String mainClass,
        String bootstrapperClass,
        String loaderClass,
        String apiVersion,
        PluginDescriptorKind kind,
        List<String> requiredDependencies,
        List<String> softDependencies,
        List<String> loadBefore,
        List<String> authors,
        String description,
        String website
) {

    /**
     * Compact constructor guaranteeing non-null unmodifiable collections.
     */
    public PaperPluginDescriptor {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(version, "version cannot be null");
        Objects.requireNonNull(kind, "kind cannot be null");

        mainClass = mainClass != null ? mainClass : "";
        bootstrapperClass = bootstrapperClass != null ? bootstrapperClass : "";
        loaderClass = loaderClass != null ? loaderClass : "";
        apiVersion = apiVersion != null ? apiVersion : "";
        description = description != null ? description : "";
        website = website != null ? website : "";

        requiredDependencies = requiredDependencies != null
                ? Collections.unmodifiableList(requiredDependencies)
                : Collections.emptyList();
        softDependencies = softDependencies != null
                ? Collections.unmodifiableList(softDependencies)
                : Collections.emptyList();
        loadBefore = loadBefore != null
                ? Collections.unmodifiableList(loadBefore)
                : Collections.emptyList();
        authors = authors != null
                ? Collections.unmodifiableList(authors)
                : Collections.emptyList();
    }
}
