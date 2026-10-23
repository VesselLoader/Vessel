package dev.vessel.adapter.neoforge;

import dev.vessel.kernel.identity.ForgeModsTomlParser;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import dev.vessel.kernel.identity.dependency.DependencyConstraintKind;
import dev.vessel.kernel.identity.dependency.ModuleDependencyRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses {@code META-INF/neoforge.mods.toml} descriptors, extracting the canonical
 * {@link VesselModuleIdentifier} and {@code [[dependencies.<modId>]]} constraint rules.
 *
 * @version 1.0.0
 */
public final class NeoForgeMetadataAdapter {

    private static final Pattern DEPENDENCY_SECTION_HEADER =
            Pattern.compile("^\\s*\\[\\[dependencies\\.([^\\]]+)]]\\s*$", Pattern.MULTILINE);
    private static final Pattern MOD_ID_FIELD =
            Pattern.compile("^\\s*modId\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.MULTILINE);
    private static final Pattern VERSION_RANGE_FIELD =
            Pattern.compile("^\\s*versionRange\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.MULTILINE);
    private static final Pattern TYPE_FIELD =
            Pattern.compile("^\\s*type\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.MULTILINE);
    private static final Pattern MANDATORY_FIELD =
            Pattern.compile("^\\s*mandatory\\s*=\\s*(true|false)", Pattern.MULTILINE);
    private static final Pattern ORDERING_FIELD =
            Pattern.compile("^\\s*ordering\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.MULTILINE);

    private final ForgeModsTomlParser baseParser = new ForgeModsTomlParser();

    /**
     * Creates a new NeoForge TOML metadata adapter.
     */
    public NeoForgeMetadataAdapter() {
    }

    /**
     * Parses the {@code [[mods]]} block of a {@code neoforge.mods.toml} file into a
     * canonical {@link VesselModuleIdentifier}.
     *
     * @param tomlContent raw TOML string content
     * @return canonical NeoForge module identifier
     */
    public VesselModuleIdentifier parseNeoForgeToml(String tomlContent) {
        Objects.requireNonNull(tomlContent, "tomlContent must not be null");
        return baseParser.parseContent(tomlContent, true);
    }

    /**
     * Extracts all {@code [[dependencies.<modId>]]} blocks from a {@code neoforge.mods.toml}
     * descriptor into canonical {@link ModuleDependencyRule} instances.
     *
     * @param tomlContent raw TOML string content
     * @return unmodifiable list of dependency constraint rules
     */
    public List<ModuleDependencyRule> extractNeoForgeDependencies(String tomlContent) {
        Objects.requireNonNull(tomlContent, "tomlContent must not be null");
        VesselModuleIdentifier sourceModule = parseNeoForgeToml(tomlContent);
        List<ModuleDependencyRule> rules = new ArrayList<>();

        Matcher headerMatcher = DEPENDENCY_SECTION_HEADER.matcher(tomlContent);
        List<Integer> blockStarts = new ArrayList<>();
        while (headerMatcher.find()) {
            blockStarts.add(headerMatcher.start());
        }
        for (int i = 0; i < blockStarts.size(); i++) {
            int start = blockStarts.get(i);
            int end = (i + 1 < blockStarts.size()) ? blockStarts.get(i + 1) : tomlContent.length();
            String block = tomlContent.substring(start, end);
            Matcher modIdMatcher = MOD_ID_FIELD.matcher(block);
            if (!modIdMatcher.find()) {
                continue;
            }
            String targetModId = modIdMatcher.group(1).trim();
            String versionRange = "*";
            Matcher rangeMatcher = VERSION_RANGE_FIELD.matcher(block);
            if (rangeMatcher.find()) {
                versionRange = rangeMatcher.group(1).trim();
            }
            DependencyConstraintKind kind = resolveConstraintKind(block);
            rules.add(new ModuleDependencyRule(sourceModule, targetModId, versionRange, kind));
        }
        return Collections.unmodifiableList(rules);
    }

    private static DependencyConstraintKind resolveConstraintKind(String block) {
        Matcher typeMatcher = TYPE_FIELD.matcher(block);
        if (typeMatcher.find()) {
            String rawType = typeMatcher.group(1).trim().toLowerCase(Locale.ROOT);
            if ("incompatible".equals(rawType) || "discouraged".equals(rawType)) {
                return DependencyConstraintKind.CONFLICTS;
            }
            if ("optional".equals(rawType)) {
                return DependencyConstraintKind.OPTIONAL;
            }
        }
        Matcher orderingMatcher = ORDERING_FIELD.matcher(block);
        if (orderingMatcher.find()) {
            String ordering = orderingMatcher.group(1).trim().toUpperCase(Locale.ROOT);
            if ("BEFORE".equals(ordering)) {
                return DependencyConstraintKind.LOAD_BEFORE;
            }
            if ("AFTER".equals(ordering)) {
                return DependencyConstraintKind.LOAD_AFTER;
            }
        }
        Matcher mandatoryMatcher = MANDATORY_FIELD.matcher(block);
        if (mandatoryMatcher.find() && "false".equalsIgnoreCase(mandatoryMatcher.group(1))) {
            return DependencyConstraintKind.OPTIONAL;
        }
        return DependencyConstraintKind.MANDATORY;
    }
}
