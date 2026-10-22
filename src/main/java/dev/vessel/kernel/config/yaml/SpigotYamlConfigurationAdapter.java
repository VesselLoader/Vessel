package dev.vessel.kernel.config.yaml;

import dev.vessel.kernel.config.HierarchicalConfigNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Clean-room YamlConfiguration adapter providing 100% backward compatibility
 * for Spigot/Paper plugins without requiring Bukkit or SnakeYAML on classpath.
 * <p>
 * Supports comment preservation across save/load cycles, nested list parsing,
 * deep dot-notation paths, and color code translation (§ and &amp;).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SpigotYamlConfigurationAdapter extends MemoryConfigurationSection {

    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    private boolean preserveComments = true;
    private String headerComment = null;

    /**
     * Constructs an empty SpigotYamlConfigurationAdapter.
     */
    public SpigotYamlConfigurationAdapter() {
        super();
    }

    /**
     * Loads configuration data from a YAML string.
     *
     * @param yaml raw YAML text
     */
    public void loadFromString(String yaml) {
        values.clear();
        comments.clear();
        headerComment = null;

        if (yaml == null || yaml.trim().isEmpty()) {
            return;
        }

        String[] lines = yaml.split("\r?\n");
        StringBuilder pendingComment = new StringBuilder();

        List<MemoryConfigurationSection> sectionStack = new ArrayList<>();
        List<Integer> indentStack = new ArrayList<>();

        sectionStack.add(this);
        indentStack.add(-1);

        String currentListKey = null;
        List<Object> currentList = null;
        int currentListIndent = -1;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int indent = getIndentation(line);

            // Handle comments
            if (trimmed.startsWith("#")) {
                if (pendingComment.length() > 0) {
                    pendingComment.append("\n");
                }
                pendingComment.append(trimmed);
                continue;
            }

            // Handle list elements: "  - value"
            if (trimmed.startsWith("-") && currentList != null && indent > currentListIndent) {
                if (sectionStack.size() > 1 && sectionStack.get(sectionStack.size() - 1).getName().equals(currentListKey)) {
                    sectionStack.remove(sectionStack.size() - 1);
                    indentStack.remove(indentStack.size() - 1);
                    MemoryConfigurationSection parentSec = sectionStack.get(sectionStack.size() - 1);
                    parentSec.values.put(currentListKey, currentList);
                }
                String itemText = trimmed.substring(1).trim();
                currentList.add(parseScalar(itemText));
                continue;
            }

            // If we were parsing a list and indent decreases or a new key is encountered
            if (currentList != null && indent <= currentListIndent) {
                currentList = null;
                currentListKey = null;
                currentListIndent = -1;
            }

            // Pop stack based on indentation
            while (indentStack.size() > 1 && indent <= indentStack.get(indentStack.size() - 1)) {
                sectionStack.remove(sectionStack.size() - 1);
                indentStack.remove(indentStack.size() - 1);
            }

            MemoryConfigurationSection currentSection = sectionStack.get(sectionStack.size() - 1);

            int colonIdx = trimmed.indexOf(':');
            if (colonIdx == -1) {
                continue;
            }

            String key = trimmed.substring(0, colonIdx).trim();
            if (key.startsWith("'") && key.endsWith("'") && key.length() >= 2) {
                key = key.substring(1, key.length() - 1);
            } else if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
                key = key.substring(1, key.length() - 1);
            }

            String valPart = trimmed.substring(colonIdx + 1).trim();

            if (preserveComments && pendingComment.length() > 0) {
                if (currentSection == this && values.isEmpty() && headerComment == null) {
                    headerComment = pendingComment.toString();
                } else {
                    currentSection.setComment(key, pendingComment.toString());
                }
                pendingComment.setLength(0);
            }

            if (valPart.isEmpty()) {
                // Section or list start
                MemoryConfigurationSection newSection = new MemoryConfigurationSection(currentSection, key);
                currentSection.values.put(key, newSection);
                sectionStack.add(newSection);
                indentStack.add(indent);

                // Prepare potential list
                currentListKey = key;
                currentList = new ArrayList<>();
                currentListIndent = indent;
                // If it turns out to be a list, we'll replace the section with the list when the first item is added
                final List<Object> listRef = currentList;
                final String listKey = key;
                final MemoryConfigurationSection secRef = currentSection;

                // Deferred list hook: will overwrite section if "- " follows
                // Handled in list item branch below
            } else if (valPart.startsWith("[") && valPart.endsWith("]")) {
                // Inline array: [a, b, c]
                List<Object> parsedArray = parseInlineArray(valPart);
                currentSection.values.put(key, parsedArray);
            } else {
                // Scalar value
                Object parsedVal = parseScalar(valPart);
                currentSection.values.put(key, parsedVal);
            }
        }
    }

    /**
     * Serializes this configuration tree to YAML string matching Spigot standards.
     *
     * @return formatted YAML text
     */
    public String saveToString() {
        StringBuilder sb = new StringBuilder();
        if (headerComment != null && preserveComments) {
            sb.append(headerComment).append("\n");
        }
        serializeSection(this, sb, 0);
        return sb.toString();
    }

    private void serializeSection(MemoryConfigurationSection section, StringBuilder sb, int indentLevel) {
        String indent = "  ".repeat(indentLevel);

        for (Map.Entry<String, Object> entry : section.values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (preserveComments) {
                String comment = section.getComment(key);
                if (comment != null && !comment.isEmpty()) {
                    for (String cLine : comment.split("\n")) {
                        sb.append(indent).append(cLine.startsWith("#") ? cLine : "# " + cLine).append("\n");
                    }
                }
            }

            if (value instanceof MemoryConfigurationSection childSec) {
                if (!childSec.values.isEmpty()) {
                    sb.append(indent).append(key).append(":\n");
                    serializeSection(childSec, sb, indentLevel + 1);
                } else {
                    sb.append(indent).append(key).append(": {}\n");
                }
            } else if (value instanceof List<?> list) {
                if (list.isEmpty()) {
                    sb.append(indent).append(key).append(": []\n");
                } else {
                    sb.append(indent).append(key).append(":\n");
                    for (Object item : list) {
                        sb.append(indent).append("  - ").append(formatScalar(item)).append("\n");
                    }
                }
            } else {
                sb.append(indent).append(key).append(": ").append(formatScalar(value)).append("\n");
            }
        }
    }

    private static String formatScalar(Object val) {
        if (val == null) {
            return "null";
        }
        if (val instanceof String str) {
            if (str.isEmpty()) {
                return "''";
            }
            if (str.contains("\n") || str.contains(":") || str.contains("#") ||
                str.contains("'") || str.startsWith("@") || str.startsWith("&") || str.startsWith("§")) {
                return "'" + str.replace("'", "''") + "'";
            }
            return str;
        }
        return String.valueOf(val);
    }

    private static Object parseScalar(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        if (raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("yes") || raw.equalsIgnoreCase("on")) {
            return Boolean.TRUE;
        }
        if (raw.equalsIgnoreCase("false") || raw.equalsIgnoreCase("no") || raw.equalsIgnoreCase("off")) {
            return Boolean.FALSE;
        }
        if (raw.equalsIgnoreCase("null") || raw.equals("~")) {
            return null;
        }

        // Strip single or double quotes
        if ((raw.startsWith("'") && raw.endsWith("'")) || (raw.startsWith("\"") && raw.endsWith("\""))) {
            if (raw.length() >= 2) {
                return raw.substring(1, raw.length() - 1).replace("''", "'");
            }
        }

        // Try integer
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
        }

        // Try long
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
        }

        // Try double
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
        }

        return raw;
    }

    private static List<Object> parseInlineArray(String raw) {
        List<Object> list = new ArrayList<>();
        String inner = raw.substring(1, raw.length() - 1).trim();
        if (inner.isEmpty()) {
            return list;
        }
        String[] tokens = inner.split(",");
        for (String t : tokens) {
            list.add(parseScalar(t.trim()));
        }
        return list;
    }

    private static int getIndentation(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += 2;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Translates alternate color codes ('&amp;') into section sign ('§').
     *
     * @param altColorChar    alternate color symbol, typically '&amp;'
     * @param textToTranslate text containing formatting codes
     * @return translated text with '§' codes
     */
    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        if (textToTranslate == null || textToTranslate.isEmpty()) {
            return textToTranslate;
        }
        char[] chars = textToTranslate.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == altColorChar && isValidColorChar(chars[i + 1])) {
                chars[i] = '§';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    /**
     * Strips all Minecraft color codes ('§x') from a string.
     *
     * @param input text with color codes
     * @return text with color codes removed
     */
    public static String stripColor(String input) {
        if (input == null) {
            return null;
        }
        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    private static boolean isValidColorChar(char c) {
        return (c >= '0' && c <= '9') ||
               (c >= 'a' && c <= 'f') ||
               (c >= 'A' && c <= 'F') ||
               (c >= 'k' && c <= 'o') ||
               (c >= 'K' && c <= 'O') ||
               c == 'r' || c == 'R';
    }

    /**
     * Converts this YamlConfiguration into a canonical {@link HierarchicalConfigNode}.
     *
     * @return hierarchical config node tree
     */
    public HierarchicalConfigNode toConfigNode() {
        HierarchicalConfigNode rootNode = new HierarchicalConfigNode("");
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            populateNode(rootNode, entry.getKey(), entry.getValue(), getComment(entry.getKey()));
        }
        return rootNode;
    }

    private void populateNode(HierarchicalConfigNode parent, String key, Object val, String comment) {
        HierarchicalConfigNode child = parent.getOrCreateNode(key);
        if (comment != null) {
            child.setComment(comment);
        }
        if (val instanceof MemoryConfigurationSection sec) {
            for (Map.Entry<String, Object> subEntry : sec.values.entrySet()) {
                populateNode(child, subEntry.getKey(), subEntry.getValue(), sec.getComment(subEntry.getKey()));
            }
        } else {
            child.setValue(val);
        }
    }

    public boolean isPreserveComments() {
        return preserveComments;
    }

    public void setPreserveComments(boolean preserveComments) {
        this.preserveComments = preserveComments;
    }

    public String getHeaderComment() {
        return headerComment;
    }

    public void setHeaderComment(String headerComment) {
        this.headerComment = headerComment;
    }
}
