package dev.vessel.kernel.identity;

import dev.vessel.kernel.identity.dependency.DependencyConstraintKind;
import dev.vessel.kernel.identity.dependency.ModuleDependencyRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Quilt mod metadata descriptors ({@code quilt.mod.json}), extracting the
 * canonical {@link VesselModuleIdentifier}, dependency constraints, and nested
 * Jar-in-Jar archive paths.
 *
 * @version 1.0.0
 */
public final class QuiltModJsonParser {

    private static final Pattern ID_FIELD_PATTERN =
            Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VERSION_FIELD_PATTERN =
            Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VERSIONS_FIELD_PATTERN =
            Pattern.compile("\"versions\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern OPTIONAL_FIELD_PATTERN =
            Pattern.compile("\"optional\"\\s*:\\s*(true|false)");
    private static final Pattern DEPENDS_ARRAY_PATTERN =
            Pattern.compile("\"depends\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern BREAKS_ARRAY_PATTERN =
            Pattern.compile("\"breaks\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern JARS_ARRAY_PATTERN =
            Pattern.compile("\"jars\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern OBJECT_ITEM_PATTERN =
            Pattern.compile("\\{([^}]+)\\}", Pattern.DOTALL);
    private static final Pattern STRING_LITERAL_PATTERN =
            Pattern.compile("\"([^\"]+)\"");

    /**
     * Creates a new Quilt metadata parser.
     */
    public QuiltModJsonParser() {
    }

    /**
     * Parses the {@code quilt_loader} descriptor from a {@code quilt.mod.json} payload.
     *
     * @param jsonContent raw {@code quilt.mod.json} string content
     * @return canonical {@link VesselModuleIdentifier} under {@link SourceLoaderKind#QUILT}
     */
    public VesselModuleIdentifier parseQuiltModJson(String jsonContent) {
        Objects.requireNonNull(jsonContent, "jsonContent must not be null");
        Matcher idMatcher = ID_FIELD_PATTERN.matcher(jsonContent);
        if (!idMatcher.find()) {
            throw new InvalidMetadataException("Missing 'id' field in quilt.mod.json");
        }
        String modId = idMatcher.group(1).trim();

        String version = "0.0.0";
        Matcher versionMatcher = VERSION_FIELD_PATTERN.matcher(jsonContent);
        if (versionMatcher.find()) {
            version = versionMatcher.group(1).trim();
        }
        return VesselModuleIdentifier.of(SourceLoaderKind.QUILT, modId, version);
    }

    /**
     * Extracts {@code depends} and {@code breaks} dependency rules from {@code quilt.mod.json}.
     *
     * @param jsonContent raw {@code quilt.mod.json} string content
     * @return immutable list of {@link ModuleDependencyRule} entries
     */
    public List<ModuleDependencyRule> extractQuiltDependencies(String jsonContent) {
        Objects.requireNonNull(jsonContent, "jsonContent must not be null");
        VesselModuleIdentifier sourceModule = parseQuiltModJson(jsonContent);
        List<ModuleDependencyRule> rules = new ArrayList<>();

        Matcher dependsMatcher = DEPENDS_ARRAY_PATTERN.matcher(jsonContent);
        if (dependsMatcher.find()) {
            parseDependencyEntries(
                    sourceModule,
                    dependsMatcher.group(1),
                    DependencyConstraintKind.MANDATORY,
                    rules);
        }

        Matcher breaksMatcher = BREAKS_ARRAY_PATTERN.matcher(jsonContent);
        if (breaksMatcher.find()) {
            parseDependencyEntries(
                    sourceModule,
                    breaksMatcher.group(1),
                    DependencyConstraintKind.CONFLICTS,
                    rules);
        }
        return Collections.unmodifiableList(rules);
    }

    /**
     * Extracts nested Jar-in-Jar file paths from the {@code jars} array in {@code quilt.mod.json}.
     *
     * @param jsonContent raw {@code quilt.mod.json} string content
     * @return immutable list of nested JAR relative paths
     */
    public List<String> extractNestedJarPaths(String jsonContent) {
        Objects.requireNonNull(jsonContent, "jsonContent must not be null");
        List<String> jarPaths = new ArrayList<>();
        Matcher jarsMatcher = JARS_ARRAY_PATTERN.matcher(jsonContent);
        if (!jarsMatcher.find()) {
            return Collections.emptyList();
        }
        String jarsBlock = jarsMatcher.group(1);
        Matcher stringMatcher = STRING_LITERAL_PATTERN.matcher(jarsBlock);
        while (stringMatcher.find()) {
            String candidate = stringMatcher.group(1).trim();
            if (candidate.endsWith(".jar")) {
                jarPaths.add(candidate);
            }
        }
        return Collections.unmodifiableList(jarPaths);
    }

    private static void parseDependencyEntries(
            VesselModuleIdentifier sourceModule,
            String arrayBody,
            DependencyConstraintKind defaultKind,
            List<ModuleDependencyRule> targetList) {
        Matcher objMatcher = OBJECT_ITEM_PATTERN.matcher(arrayBody);
        boolean matchedObjects = false;
        while (objMatcher.find()) {
            matchedObjects = true;
            String itemBody = objMatcher.group(1);
            Matcher idMatcher = ID_FIELD_PATTERN.matcher(itemBody);
            if (!idMatcher.find()) {
                continue;
            }
            String depId = idMatcher.group(1).trim();
            String verConstraint = "*";
            Matcher verMatcher = VERSIONS_FIELD_PATTERN.matcher(itemBody);
            if (verMatcher.find()) {
                verConstraint = verMatcher.group(1).trim();
            } else {
                Matcher singleVer = VERSION_FIELD_PATTERN.matcher(itemBody);
                if (singleVer.find()) {
                    verConstraint = singleVer.group(1).trim();
                }
            }
            DependencyConstraintKind kind = defaultKind;
            if (defaultKind == DependencyConstraintKind.MANDATORY) {
                Matcher optMatcher = OPTIONAL_FIELD_PATTERN.matcher(itemBody);
                if (optMatcher.find() && "true".equalsIgnoreCase(optMatcher.group(1))) {
                    kind = DependencyConstraintKind.OPTIONAL;
                }
            }
            targetList.add(new ModuleDependencyRule(sourceModule, depId, verConstraint, kind));
        }

        if (!matchedObjects) {
            Matcher strMatcher = STRING_LITERAL_PATTERN.matcher(arrayBody);
            while (strMatcher.find()) {
                String depId = strMatcher.group(1).trim();
                if (!depId.isEmpty()) {
                    targetList.add(new ModuleDependencyRule(sourceModule, depId, "*", defaultKind));
                }
            }
        }
    }
}
