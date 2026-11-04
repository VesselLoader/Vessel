package dev.vessel.adapter.bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clean-room parser that extracts the {@code commands:} section from a Bukkit {@code plugin.yml}
 * or {@code paper-plugin.yml} descriptor into structured {@link PluginYmlCommandDescriptor} records
 * without external library dependencies.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class PluginYmlCommandParser {

    private static final Pattern INLINE_ALIASES_PATTERN =
            Pattern.compile("\\[(.*)\\]");

    /**
     * Parses all command definitions from the given YAML {@link InputStream}.
     *
     * @param inputStream YAML descriptor input stream
     * @return list of parsed command descriptors
     * @throws IOException if stream reading fails
     */
    public List<PluginYmlCommandDescriptor> parseCommands(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return parseCommandsFromLines(lines);
        }
    }

    /**
     * Parses command descriptors from an in-memory list of YAML lines.
     *
     * @param lines raw lines from plugin descriptor
     * @return list of parsed command descriptors
     */
    public List<PluginYmlCommandDescriptor> parseCommandsFromLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }

        List<PluginYmlCommandDescriptor> results = new ArrayList<>();
        boolean insideCommands = false;
        int commandsIndent = -1;

        String currentCmdName = null;
        String currentDesc = "";
        String currentUsage = "";
        String currentPerm = "";
        List<String> currentAliases = new ArrayList<>();
        boolean readingBlockAliases = false;

        for (String rawLine : lines) {
            String lineWithoutComments = stripComments(rawLine);
            if (lineWithoutComments.trim().isEmpty()) {
                continue;
            }

            int indent = getLeadingWhitespaceCount(lineWithoutComments);
            String trimmed = lineWithoutComments.trim();

            if (!insideCommands) {
                if (trimmed.equals("commands:") || trimmed.startsWith("commands:")) {
                    insideCommands = true;
                    commandsIndent = indent;
                }
                continue;
            }

            // If indentation drops back to root or below commands:, commands section ended
            if (indent <= commandsIndent && !trimmed.isEmpty()) {
                // Flush last command if any
                if (currentCmdName != null) {
                    results.add(new PluginYmlCommandDescriptor(
                            currentCmdName, currentDesc, currentUsage, currentPerm, currentAliases));
                    currentCmdName = null;
                }
                break;
            }

            // Command definition (e.g. "  chunky:")
            if (indent > commandsIndent && trimmed.endsWith(":") && !trimmed.contains(" ")
                    && !trimmed.equals("aliases:") && !trimmed.equals("permissions:") && !trimmed.equals("children:")) {
                if (currentCmdName != null) {
                    results.add(new PluginYmlCommandDescriptor(
                            currentCmdName, currentDesc, currentUsage, currentPerm, currentAliases));
                    currentDesc = "";
                    currentUsage = "";
                    currentPerm = "";
                    currentAliases = new ArrayList<>();
                    readingBlockAliases = false;
                }
                currentCmdName = trimmed.substring(0, trimmed.length() - 1).trim();
                continue;
            }

            // Sub-properties under command
            if (currentCmdName != null) {
                if (trimmed.startsWith("description:")) {
                    currentDesc = extractValue(trimmed, "description:");
                    readingBlockAliases = false;
                } else if (trimmed.startsWith("usage:")) {
                    currentUsage = extractValue(trimmed, "usage:");
                    readingBlockAliases = false;
                } else if (trimmed.startsWith("permission:")) {
                    currentPerm = extractValue(trimmed, "permission:");
                    readingBlockAliases = false;
                } else if (trimmed.startsWith("aliases:")) {
                    String val = extractValue(trimmed, "aliases:");
                    Matcher m = INLINE_ALIASES_PATTERN.matcher(val);
                    if (m.find()) {
                        String[] parts = m.group(1).split(",");
                        for (String p : parts) {
                            String alias = p.trim().replaceAll("['\"]", "");
                            if (!alias.isEmpty()) {
                                currentAliases.add(alias);
                            }
                        }
                        readingBlockAliases = false;
                    } else if (!val.isBlank()) {
                        currentAliases.add(val.replaceAll("['\"]", "").trim());
                        readingBlockAliases = false;
                    } else {
                        readingBlockAliases = true;
                    }
                } else if (trimmed.startsWith("- ") && (readingBlockAliases || !currentAliases.isEmpty())) {
                    String extraAlias = trimmed.substring(2).trim().replaceAll("['\"]", "");
                    if (!extraAlias.isEmpty()) {
                        currentAliases.add(extraAlias);
                    }
                }
            }
        }

        if (currentCmdName != null) {
            results.add(new PluginYmlCommandDescriptor(
                    currentCmdName, currentDesc, currentUsage, currentPerm, currentAliases));
        }

        return Collections.unmodifiableList(results);
    }

    private static String stripComments(String line) {
        int hashIdx = line.indexOf('#');
        if (hashIdx >= 0) {
            return line.substring(0, hashIdx);
        }
        return line;
    }

    private static int getLeadingWhitespaceCount(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ' || c == '\t') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private static String extractValue(String line, String prefix) {
        String sub = line.substring(prefix.length()).trim();
        if (sub.startsWith("\"") && sub.endsWith("\"") && sub.length() >= 2) {
            return sub.substring(1, sub.length() - 1);
        }
        if (sub.startsWith("'") && sub.endsWith("'") && sub.length() >= 2) {
            return sub.substring(1, sub.length() - 1);
        }
        return sub;
    }
}
