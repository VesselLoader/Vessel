package org.bukkit.configuration.file;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard configuration options for {@link FileConfiguration} instances
 * ({@code org.bukkit.configuration.file.FileConfigurationOptions}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FileConfigurationOptions {

    private boolean copyDefaults = false;
    private List<String> header = new ArrayList<>();

    public FileConfigurationOptions() {
    }

    public boolean copyDefaults() {
        return copyDefaults;
    }

    public FileConfigurationOptions copyDefaults(boolean value) {
        this.copyDefaults = value;
        return this;
    }

    public List<String> getHeader() {
        return header;
    }

    public FileConfigurationOptions setHeader(List<String> value) {
        this.header = value != null ? new ArrayList<>(value) : new ArrayList<>();
        return this;
    }

    public FileConfigurationOptions header(String value) {
        if (value != null) {
            this.header = List.of(value.split("\n"));
        } else {
            this.header = new ArrayList<>();
        }
        return this;
    }

    public String header() {
        return String.join("\n", header);
    }
}
