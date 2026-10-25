package dev.vessel.kernel.config.yaml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * In-memory hierarchical configuration section implementing Spigot/Bukkit contract.
 * <p>
 * Handles dot-notation path navigation (e.g. {@code "database.mysql.port"}), recursive
 * deep value collection, robust type coercion, and per-key comment retention.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class MemoryConfigurationSection implements ConfigurationSection {

    private final String name;
    private final ConfigurationSection parent;
    private final ConfigurationSection root;
    protected final Map<String, Object> values = new LinkedHashMap<>();
    protected final Map<String, String> comments = new LinkedHashMap<>();

    /**
     * Root section constructor.
     */
    public MemoryConfigurationSection() {
        this.name = "";
        this.parent = null;
        this.root = this;
    }

    /**
     * Child section constructor.
     *
     * @param parent parent section
     * @param name   section relative name
     */
    public MemoryConfigurationSection(ConfigurationSection parent, String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.parent = Objects.requireNonNull(parent, "parent cannot be null");
        this.root = parent.getRoot();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCurrentPath() {
        if (parent == null || parent == this) {
            return name;
        }
        String parentPath = parent.getCurrentPath();
        if (parentPath == null || parentPath.isEmpty()) {
            return name;
        }
        return parentPath + "." + name;
    }

    @Override
    public ConfigurationSection getParent() {
        return parent;
    }

    @Override
    public ConfigurationSection getRoot() {
        return root;
    }

    @Override
    public Object get(String path) {
        return get(path, null);
    }

    @Override
    public Object get(String path, Object def) {
        if (path == null || path.isEmpty()) {
            return this;
        }

        int dotIdx = path.indexOf('.');
        if (dotIdx != -1) {
            String subKey = path.substring(0, dotIdx);
            String remaining = path.substring(dotIdx + 1);
            Object child = values.get(subKey);
            if (child instanceof ConfigurationSection sec) {
                return sec.get(remaining, def);
            }
            return def;
        }

        Object val = values.get(path);
        return (val != null) ? val : def;
    }

    @Override
    public void set(String path, Object value) {
        Objects.requireNonNull(path, "path cannot be null");
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Cannot set value on empty path");
        }

        int dotIdx = path.indexOf('.');
        if (dotIdx != -1) {
            String subKey = path.substring(0, dotIdx);
            String remaining = path.substring(dotIdx + 1);
            Object child = values.get(subKey);
            ConfigurationSection sec;
            if (child instanceof ConfigurationSection existingSec) {
                sec = existingSec;
            } else {
                sec = new MemoryConfigurationSection(this, subKey);
                values.put(subKey, sec);
            }
            sec.set(remaining, value);
            return;
        }

        if (value == null) {
            values.remove(path);
            comments.remove(path);
        } else {
            values.put(path, value);
        }
    }

    @Override
    public boolean contains(String path) {
        return get(path) != null;
    }

    @Override
    public boolean isSet(String path) {
        return contains(path);
    }

    @Override
    public String getString(String path) {
        return getString(path, null);
    }

    @Override
    public String getString(String path, String def) {
        Object val = get(path, def);
        return (val != null) ? String.valueOf(val) : def;
    }

    @Override
    public int getInt(String path) {
        return getInt(path, 0);
    }

    @Override
    public int getInt(String path, int def) {
        Object val = get(path);
        if (val instanceof Number num) {
            return num.intValue();
        }
        if (val instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    @Override
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        Object val = get(path);
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String str) {
            if ("true".equalsIgnoreCase(str) || "yes".equalsIgnoreCase(str) || "on".equalsIgnoreCase(str)) {
                return true;
            }
            if ("false".equalsIgnoreCase(str) || "no".equalsIgnoreCase(str) || "off".equalsIgnoreCase(str)) {
                return false;
            }
        }
        return def;
    }

    @Override
    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }

    @Override
    public double getDouble(String path, double def) {
        Object val = get(path);
        if (val instanceof Number num) {
            return num.doubleValue();
        }
        if (val instanceof String str) {
            try {
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    @Override
    public long getLong(String path) {
        return getLong(path, 0L);
    }

    @Override
    public long getLong(String path, long def) {
        Object val = get(path);
        if (val instanceof Number num) {
            return num.longValue();
        }
        if (val instanceof String str) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    @Override
    public List<?> getList(String path) {
        return getList(path, Collections.emptyList());
    }

    @Override
    public List<?> getList(String path, List<?> def) {
        Object val = get(path);
        if (val instanceof List<?> list) {
            return list;
        }
        return def;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path) {
        Object val = get(path);
        if (val instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object obj : list) {
                result.add(obj != null ? String.valueOf(obj) : "");
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public List<Integer> getIntegerList(String path) {
        Object val = get(path);
        if (val instanceof List<?> list) {
            List<Integer> result = new ArrayList<>(list.size());
            for (Object obj : list) {
                if (obj instanceof Number num) {
                    result.add(num.intValue());
                } else if (obj instanceof String str) {
                    try {
                        result.add(Integer.parseInt(str.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public ConfigurationSection getConfigurationSection(String path) {
        Object val = get(path);
        if (val instanceof ConfigurationSection sec) {
            return sec;
        }
        return null;
    }

    @Override
    public ConfigurationSection createSection(String path) {
        Objects.requireNonNull(path, "path cannot be null");
        int dotIdx = path.indexOf('.');
        if (dotIdx != -1) {
            String subKey = path.substring(0, dotIdx);
            String remaining = path.substring(dotIdx + 1);
            ConfigurationSection sec = getConfigurationSection(subKey);
            if (sec == null) {
                sec = new MemoryConfigurationSection(this, subKey);
                values.put(subKey, sec);
            }
            return sec.createSection(remaining);
        }

        ConfigurationSection sec = new MemoryConfigurationSection(this, path);
        values.put(path, sec);
        return sec;
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        Set<String> keys = new LinkedHashSet<>();
        collectKeys(this, "", keys, deep);
        return keys;
    }

    private void collectKeys(ConfigurationSection section, String prefix, Set<String> outKeys, boolean deep) {
        if (section instanceof MemoryConfigurationSection memSec) {
            for (Map.Entry<String, Object> entry : memSec.values.entrySet()) {
                String fullKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                outKeys.add(fullKey);
                if (deep && entry.getValue() instanceof ConfigurationSection childSec) {
                    collectKeys(childSec, fullKey, outKeys, true);
                }
            }
        }
    }

    @Override
    public Map<String, Object> getValues(boolean deep) {
        Map<String, Object> map = new LinkedHashMap<>();
        collectValues(this, "", map, deep);
        return map;
    }

    private void collectValues(ConfigurationSection section, String prefix, Map<String, Object> outMap, boolean deep) {
        if (section instanceof MemoryConfigurationSection memSec) {
            for (Map.Entry<String, Object> entry : memSec.values.entrySet()) {
                String fullKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                outMap.put(fullKey, entry.getValue());
                if (deep && entry.getValue() instanceof ConfigurationSection childSec) {
                    collectValues(childSec, fullKey, outMap, true);
                }
            }
        }
    }

    public void setComment(String path, String comment) {
        if (comment == null) {
            comments.remove(path);
        } else {
            comments.put(path, comment);
        }
    }

    public String getComment(String path) {
        return comments.get(path);
    }
}
