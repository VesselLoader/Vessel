package org.bukkit.configuration.file;

import java.util.List;

/**
 * Standard configuration options for {@link YamlConfiguration} instances
 * ({@code org.bukkit.configuration.file.YamlConfigurationOptions}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class YamlConfigurationOptions extends FileConfigurationOptions {

    private int indent = 2;

    public YamlConfigurationOptions() {
        super();
    }

    public int indent() {
        return indent;
    }

    public YamlConfigurationOptions indent(int value) {
        this.indent = value;
        return this;
    }

    @Override
    public YamlConfigurationOptions copyDefaults(boolean value) {
        super.copyDefaults(value);
        return this;
    }

    @Override
    public YamlConfigurationOptions setHeader(List<String> value) {
        super.setHeader(value);
        return this;
    }

    @Override
    public YamlConfigurationOptions header(String value) {
        super.header(value);
        return this;
    }
}
