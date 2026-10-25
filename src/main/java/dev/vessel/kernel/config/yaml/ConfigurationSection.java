package dev.vessel.kernel.config.yaml;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clean-room contract matching Spigot/Bukkit {@code ConfigurationSection}.
 * <p>
 * Provides complete backward compatibility for plugins expecting the standard Bukkit
 * hierarchical configuration API without depending on Bukkit or Spigot server JARs.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface ConfigurationSection {

    String getName();

    String getCurrentPath();

    ConfigurationSection getParent();

    ConfigurationSection getRoot();

    Object get(String path);

    Object get(String path, Object def);

    void set(String path, Object value);

    boolean contains(String path);

    boolean isSet(String path);

    String getString(String path);

    String getString(String path, String def);

    int getInt(String path);

    int getInt(String path, int def);

    boolean getBoolean(String path);

    boolean getBoolean(String path, boolean def);

    double getDouble(String path);

    double getDouble(String path, double def);

    long getLong(String path);

    long getLong(String path, long def);

    List<?> getList(String path);

    List<?> getList(String path, List<?> def);

    List<String> getStringList(String path);

    List<Integer> getIntegerList(String path);

    ConfigurationSection getConfigurationSection(String path);

    ConfigurationSection createSection(String path);

    Set<String> getKeys(boolean deep);

    Map<String, Object> getValues(boolean deep);
}
