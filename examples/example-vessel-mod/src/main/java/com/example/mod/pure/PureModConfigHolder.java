package com.example.mod.pure;

import dev.vessel.api.config.VesselConfigurationTree;
import dev.vessel.api.config.YamlToVesselParser;

import java.util.List;
import java.util.Objects;

/**
 * Demonstrates flat, zero-allocation configuration management using VESSEL's Native Config Substrate.
 *
 * <p>Unlike SnakeYAML or NightConfig which allocate thousands of intermediate hashmap nodes,
 * VESSEL stores configuration keys and values in flat, memory-efficient token arrays.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class PureModConfigHolder {

    private final YamlToVesselParser parser;
    private VesselConfigurationTree configTree;

    /**
     * Constructs a new configuration holder.
     */
    public PureModConfigHolder() {
        this.parser = new YamlToVesselParser();
        this.configTree = new VesselConfigurationTree();
    }

    /**
     * Loads and parses raw YAML configuration content into the flat tree.
     *
     * @param rawYaml raw YAML configuration string
     */
    public void loadConfiguration(String rawYaml) {
        Objects.requireNonNull(rawYaml, "rawYaml cannot be null");
        this.configTree = parser.parse(rawYaml);
    }

    /**
     * Retrieves the configured maximum transfer rate for energy conduits.
     *
     * @return energy max transfer rate in RF/FE/E
     */
    public long getMaxEnergyTransferRate() {
        return configTree.getLong("energy.conduit.max-transfer", 50000L);
    }

    /**
     * Retrieves the configured battery capacity.
     *
     * @return battery capacity in RF/FE/E
     */
    public long getBatteryCapacity() {
        return configTree.getLong("energy.battery.capacity", 10000000L);
    }

    /**
     * Retrieves list of dimension IDs where teleportation is disabled.
     *
     * @return unmodifiable list of blacklisted dimension identifiers
     */
    public List<String> getBlacklistedDimensions() {
        return configTree.getStringList("teleport.blacklisted-dimensions");
    }

    /**
     * Serializes the current configuration state back into clean YAML format.
     *
     * @return formatted YAML text string
     */
    public String saveToYaml() {
        return parser.toYaml(configTree);
    }
}
