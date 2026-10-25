package dev.vessel.kernel.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Hierarchical configuration tree node supporting dot-notation navigation and comment preservation.
 * <p>
 * Bridges Bukkit's {@code MemorySection}/{@code YamlConfiguration}, Forge's {@code CommentedConfig},
 * and Fabric's config models into a single unified in-memory tree with zero third-party dependencies.
 *
 * @version 1.0.0
 */
public final class HierarchicalConfigNode {

    private final String key;
    private Object value;
    private String comment;
    private final Map<String, HierarchicalConfigNode> children;

    /**
     * Constructs a root or child configuration node.
     *
     * @param key the node key name (empty string for root)
     */
    public HierarchicalConfigNode(String key) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.children = new LinkedHashMap<>();
    }

    /**
     * Retrieves a child or nested node by dot-separated path (e.g. "database.mysql.port").
     *
     * @param path dot-separated key path
     * @return the node, or null if not found
     */
    public HierarchicalConfigNode getNode(String path) {
        if (path == null || path.isEmpty()) {
            return this;
        }

        String[] parts = path.split("\\.");
        HierarchicalConfigNode current = this;

        for (String part : parts) {
            current = current.children.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * Retrieves or creates child nodes along a dot-separated path.
     *
     * @param path dot-separated key path
     * @return target node
     */
    public HierarchicalConfigNode getOrCreateNode(String path) {
        if (path == null || path.isEmpty()) {
            return this;
        }

        String[] parts = path.split("\\.");
        HierarchicalConfigNode current = this;

        for (String part : parts) {
            current = current.children.computeIfAbsent(part, HierarchicalConfigNode::new);
        }
        return current;
    }

    /**
     * Sets a value at the specified dot-separated path.
     *
     * @param path  dot-separated key path
     * @param value value to set
     */
    public void set(String path, Object value) {
        HierarchicalConfigNode target = getOrCreateNode(path);
        target.setValue(value);
    }

    /**
     * Sets a comment for the node at the specified dot-separated path.
     *
     * @param path    dot-separated key path
     * @param comment comment string
     */
    public void setComment(String path, String comment) {
        HierarchicalConfigNode target = getOrCreateNode(path);
        target.comment = comment;
    }

    /**
     * Checks if a path exists.
     *
     * @param path dot-separated path
     * @return true if path exists
     */
    public boolean contains(String path) {
        return getNode(path) != null;
    }

    /**
     * Retrieves a string value, returning default if absent or not a string.
     *
     * @param path         dot-separated path
     * @param defaultValue fallback default
     * @return string value
     */
    public String getString(String path, String defaultValue) {
        HierarchicalConfigNode node = getNode(path);
        if (node != null && node.value != null) {
            return String.valueOf(node.value);
        }
        return defaultValue;
    }

    /**
     * Retrieves an integer value.
     *
     * @param path         dot-separated path
     * @param defaultValue fallback default
     * @return int value
     */
    public int getInt(String path, int defaultValue) {
        HierarchicalConfigNode node = getNode(path);
        if (node != null && node.value != null) {
            if (node.value instanceof Number num) {
                return num.intValue();
            }
            try {
                return Integer.parseInt(node.value.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Retrieves a long value.
     *
     * @param path         dot-separated path
     * @param defaultValue fallback default
     * @return long value
     */
    public long getLong(String path, long defaultValue) {
        HierarchicalConfigNode node = getNode(path);
        if (node != null && node.value != null) {
            if (node.value instanceof Number num) {
                return num.longValue();
            }
            try {
                return Long.parseLong(node.value.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Retrieves a double value.
     *
     * @param path         dot-separated path
     * @param defaultValue fallback default
     * @return double value
     */
    public double getDouble(String path, double defaultValue) {
        HierarchicalConfigNode node = getNode(path);
        if (node != null && node.value != null) {
            if (node.value instanceof Number num) {
                return num.doubleValue();
            }
            try {
                return Double.parseDouble(node.value.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Retrieves a boolean value.
     *
     * @param path         dot-separated path
     * @param defaultValue fallback default
     * @return boolean value
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        HierarchicalConfigNode node = getNode(path);
        if (node != null && node.value != null) {
            if (node.value instanceof Boolean b) {
                return b;
            }
            String s = node.value.toString().trim().toLowerCase();
            if (s.equals("true") || s.equals("yes") || s.equals("1")) {
                return true;
            }
            if (s.equals("false") || s.equals("no") || s.equals("0")) {
                return false;
            }
        }
        return defaultValue;
    }

    /**
     * Retrieves a list of strings.
     *
     * @param path dot-separated path
     * @return list of strings
     */
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path) {
        HierarchicalConfigNode node = getNode(path);
        if (node != null && node.value instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        return Collections.emptyList();
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Map<String, HierarchicalConfigNode> getChildren() {
        return children;
    }

    public boolean isSection() {
        return !children.isEmpty();
    }
}
