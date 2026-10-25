package dev.vessel.kernel.adapter.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Robust, zero-dependency parser for Spigot {@code plugin.yml} and Paper {@code paper-plugin.yml}
 * manifest descriptors.
 *
 * <p>Accurately extracts plugin identity, main classes, bootstrapping hooks, and dependency
 * relationships across single-line arrays, YAML multiline blocks, and nested dependency tables.</p>
 *
 * <p>Quells: ECO-091..100.</p>
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class PaperPluginYmlDescriptorParser {

    /**
     * Constructs a new descriptor parser.
     */
    public PaperPluginYmlDescriptorParser() {
    }

    /**
     * Automatically detects format kind and parses the raw YAML descriptor text.
     *
     * @param yamlContent raw descriptor content
     * @return parsed metadata descriptor
     */
    public PaperPluginDescriptor parse(String yamlContent) {
        Objects.requireNonNull(yamlContent, "yamlContent cannot be null");
        PluginDescriptorKind detectedKind = detectKind(yamlContent);
        return parse(yamlContent, detectedKind);
    }

    /**
     * Parses the YAML text under a specific descriptor kind.
     *
     * @param yamlContent raw descriptor content
     * @param kind targeted descriptor kind
     * @return parsed metadata descriptor
     */
    public PaperPluginDescriptor parse(String yamlContent, PluginDescriptorKind kind) {
        Objects.requireNonNull(yamlContent, "yamlContent cannot be null");
        Objects.requireNonNull(kind, "kind cannot be null");

        String name = "unknown";
        String version = "1.0.0";
        String mainClass = "";
        String bootstrapperClass = "";
        String loaderClass = "";
        String apiVersion = "";
        String description = "";
        String website = "";

        List<String> requiredDeps = new ArrayList<>();
        List<String> softDeps = new ArrayList<>();
        List<String> loadBefore = new ArrayList<>();
        List<String> authors = new ArrayList<>();

        String[] lines = yamlContent.split("\\R");
        String currentSection = "";

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            String trimmed = rawLine.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int indent = getLeadingWhitespaceCount(rawLine);

            if (indent == 0) {
                currentSection = "";
            }

            int colonIndex = trimmed.indexOf(':');
            if (colonIndex <= 0) {
                // Check if this is a list item under a section
                if (trimmed.startsWith("-") && !currentSection.isEmpty()) {
                    String item = stripQuotes(trimmed.substring(1).trim());
                    if (!item.isEmpty()) {
                        addToListForSection(currentSection, item, requiredDeps, softDeps, loadBefore, authors);
                    }
                }
                continue;
            }

            String key = trimmed.substring(0, colonIndex).trim();
            String value = trimmed.substring(colonIndex + 1).trim();
            value = stripComment(value);
            value = stripQuotes(value);

            if (indent == 0) {
                if ("name".equalsIgnoreCase(key)) {
                    name = value;
                } else if ("version".equalsIgnoreCase(key)) {
                    version = value;
                } else if ("main".equalsIgnoreCase(key)) {
                    mainClass = value;
                } else if ("bootstrapper".equalsIgnoreCase(key)) {
                    bootstrapperClass = value;
                } else if ("loader".equalsIgnoreCase(key)) {
                    loaderClass = value;
                } else if ("api-version".equalsIgnoreCase(key)) {
                    apiVersion = value;
                } else if ("description".equalsIgnoreCase(key)) {
                    description = value;
                } else if ("website".equalsIgnoreCase(key)) {
                    website = value;
                } else if ("author".equalsIgnoreCase(key) && !value.isEmpty()) {
                    authors.add(value);
                } else if ("authors".equalsIgnoreCase(key)) {
                    parseInlineOrStartSection(key, value, authors, "authors");
                    currentSection = "authors";
                } else if ("depend".equalsIgnoreCase(key)) {
                    parseInlineOrStartSection(key, value, requiredDeps, "depend");
                    currentSection = "depend";
                } else if ("softdepend".equalsIgnoreCase(key)) {
                    parseInlineOrStartSection(key, value, softDeps, "softdepend");
                    currentSection = "softdepend";
                } else if ("loadbefore".equalsIgnoreCase(key) || "load-before".equalsIgnoreCase(key)) {
                    parseInlineOrStartSection(key, value, loadBefore, "loadbefore");
                    currentSection = "loadbefore";
                } else if ("dependencies".equalsIgnoreCase(key)) {
                    currentSection = "dependencies";
                }
            } else {
                // Nested lines (e.g. inside dependencies or server)
                if ("dependencies".equalsIgnoreCase(currentSection) || currentSection.startsWith("dependencies.")) {
                    if ("server".equalsIgnoreCase(key) || "bootstrap".equalsIgnoreCase(key)) {
                        currentSection = "dependencies." + key.toLowerCase();
                    } else if ("required".equalsIgnoreCase(key)) {
                        parseInlineOrStartSection(key, value, requiredDeps, "dependencies.required");
                        currentSection = "dependencies.required";
                    } else if ("load-after".equalsIgnoreCase(key)) {
                        parseInlineOrStartSection(key, value, softDeps, "dependencies.load-after");
                        currentSection = "dependencies.load-after";
                    } else if ("load-before".equalsIgnoreCase(key)) {
                        parseInlineOrStartSection(key, value, loadBefore, "dependencies.load-before");
                        currentSection = "dependencies.load-before";
                    }
                } else if (trimmed.startsWith("-")) {
                    String item = stripQuotes(trimmed.substring(1).trim());
                    if (!item.isEmpty()) {
                        addToListForSection(currentSection, item, requiredDeps, softDeps, loadBefore, authors);
                    }
                }
            }
        }

        return new PaperPluginDescriptor(
                name,
                version,
                mainClass,
                bootstrapperClass,
                loaderClass,
                apiVersion,
                kind,
                requiredDeps,
                softDeps,
                loadBefore,
                authors,
                description,
                website
        );
    }

    private static PluginDescriptorKind detectKind(String yamlContent) {
        if (yamlContent.contains("paper-plugin.yml")
                || yamlContent.contains("dependencies:")
                || yamlContent.contains("bootstrapper:")
                || yamlContent.contains("loader:")) {
            return PluginDescriptorKind.PAPER_PLUGIN_YML;
        }
        return PluginDescriptorKind.BUKKIT_PLUGIN_YML;
    }

    private static void parseInlineOrStartSection(String key, String value, List<String> targetList, String sectionName) {
        if (value.startsWith("[") && value.endsWith("]")) {
            String inner = value.substring(1, value.length() - 1).trim();
            if (!inner.isEmpty()) {
                String[] tokens = inner.split(",");
                for (int i = 0; i < tokens.length; i++) {
                    String token = stripQuotes(tokens[i].trim());
                    if (!token.isEmpty()) {
                        targetList.add(token);
                    }
                }
            }
        } else if (!value.isEmpty()) {
            targetList.add(value);
        }
    }

    private static void addToListForSection(
            String section,
            String item,
            List<String> required,
            List<String> soft,
            List<String> loadBefore,
            List<String> authors) {
        if ("depend".equalsIgnoreCase(section) || "dependencies.required".equalsIgnoreCase(section)) {
            required.add(item);
        } else if ("softdepend".equalsIgnoreCase(section) || "dependencies.load-after".equalsIgnoreCase(section)) {
            soft.add(item);
        } else if ("loadbefore".equalsIgnoreCase(section) || "dependencies.load-before".equalsIgnoreCase(section)) {
            loadBefore.add(item);
        } else if ("authors".equalsIgnoreCase(section)) {
            authors.add(item);
        }
    }

    private static int getLeadingWhitespaceCount(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += 4;
            } else {
                break;
            }
        }
        return count;
    }

    private static String stripQuotes(String s) {
        if (s == null) {
            return "";
        }
        String res = s.trim();
        if (res.length() >= 2) {
            if ((res.startsWith("\"") && res.endsWith("\"")) || (res.startsWith("'") && res.endsWith("'"))) {
                return res.substring(1, res.length() - 1).trim();
            }
        }
        return res;
    }

    private static String stripComment(String s) {
        if (s == null) {
            return "";
        }
        int hash = s.indexOf('#');
        if (hash >= 0) {
            return s.substring(0, hash).trim();
        }
        return s.trim();
    }
}
