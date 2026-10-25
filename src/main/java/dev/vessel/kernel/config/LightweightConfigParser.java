package dev.vessel.kernel.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lightweight, zero-dependency configuration parser and serializer for YAML and TOML.
 * <p>
 * Preserves comments across reloads and builds hierarchical trees without external libraries,
 * keeping VESSEL's standalone footprint compact.
 *
 * @version 1.0.0
 */
public final class LightweightConfigParser {

    private LightweightConfigParser() {
        // Utility constructor
    }

    /**
     * Parses YAML/TOML-style indented text into a hierarchical configuration tree.
     *
     * @param content raw configuration string
     * @return root configuration node
     */
    public static HierarchicalConfigNode parse(String content) {
        HierarchicalConfigNode root = new HierarchicalConfigNode("");
        if (content == null || content.isEmpty()) {
            return root;
        }

        String[] lines = content.split("\r?\n");
        StringBuilder pendingComment = new StringBuilder();

        List<HierarchicalConfigNode> nodeStack = new ArrayList<>();
        List<Integer> indentStack = new ArrayList<>();
        nodeStack.add(root);
        indentStack.add(-1);

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // Capture comments
            if (trimmed.startsWith("#")) {
                if (pendingComment.length() > 0) {
                    pendingComment.append("\n");
                }
                pendingComment.append(trimmed.substring(1).trim());
                continue;
            }

            // Calculate indentation level
            int indent = 0;
            while (indent < line.length() && (line.charAt(indent) == ' ' || line.charAt(indent) == '\t')) {
                indent++;
            }

            // Pop stack for shallower indentation
            while (indentStack.size() > 1 && indent <= indentStack.get(indentStack.size() - 1)) {
                nodeStack.remove(nodeStack.size() - 1);
                indentStack.remove(indentStack.size() - 1);
            }

            HierarchicalConfigNode parent = nodeStack.get(nodeStack.size() - 1);

            int colonIdx = trimmed.indexOf(':');
            int equalsIdx = trimmed.indexOf('=');
            int delimiterIdx = (colonIdx != -1) ? colonIdx : equalsIdx;

            if (delimiterIdx != -1) {
                String key = trimmed.substring(0, delimiterIdx).trim();
                String rawVal = trimmed.substring(delimiterIdx + 1).trim();

                HierarchicalConfigNode node = parent.getOrCreateNode(key);
                if (pendingComment.length() > 0) {
                    node.setComment(pendingComment.toString());
                    pendingComment.setLength(0);
                }

                if (rawVal.isEmpty()) {
                    // This is a section header, push to stack
                    nodeStack.add(node);
                    indentStack.add(indent);
                } else {
                    node.setValue(parseScalarValue(rawVal));
                }
            }
        }

        return root;
    }

    /**
     * Serializes a hierarchical configuration tree into formatted YAML text with preserved comments.
     *
     * @param root root configuration node
     * @return YAML string
     */
    public static String toYamlString(HierarchicalConfigNode root) {
        StringBuilder sb = new StringBuilder();
        serializeChildren(root, sb, 0);
        return sb.toString();
    }

    private static void serializeChildren(HierarchicalConfigNode parent, StringBuilder sb, int indentLevel) {
        String indent = "  ".repeat(indentLevel);

        for (Map.Entry<String, HierarchicalConfigNode> entry : parent.getChildren().entrySet()) {
            HierarchicalConfigNode node = entry.getValue();

            if (node.getComment() != null && !node.getComment().isEmpty()) {
                for (String cLine : node.getComment().split("\n")) {
                    sb.append(indent).append("# ").append(cLine).append("\n");
                }
            }

            sb.append(indent).append(node.getKey()).append(":");

            if (node.isSection()) {
                sb.append("\n");
                serializeChildren(node, sb, indentLevel + 1);
            } else {
                sb.append(" ").append(formatValue(node.getValue())).append("\n");
            }
        }
    }

    private static Object parseScalarValue(String raw) {
        // Strip quotes
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            return raw.substring(1, raw.length() - 1);
        }
        if (raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("yes")) {
            return Boolean.TRUE;
        }
        if (raw.equalsIgnoreCase("false") || raw.equalsIgnoreCase("no")) {
            return Boolean.FALSE;
        }
        try {
            if (raw.contains(".")) {
                return Double.parseDouble(raw);
            }
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
        }
        return raw;
    }

    private static String formatValue(Object val) {
        if (val == null) {
            return "";
        }
        if (val instanceof String s) {
            if (s.contains(" ") || s.contains(":") || s.contains("#")) {
                return "\"" + s + "\"";
            }
            return s;
        }
        return val.toString();
    }
}
