package net.minecraftforge.fluids;

/**
 * Universal multi-era compatibility representation of a Forge {@code FluidType}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FluidType {

    private final FluidType$Properties properties;

    /**
     * Constructs a fluid type with the given configured properties.
     *
     * @param properties fluid properties
     */
    public FluidType(FluidType$Properties properties) {
        this.properties = properties != null ? properties : FluidType$Properties.create();
    }

    /**
     * Returns the description translation key of this fluid type.
     *
     * @return translation key
     */
    public String getDescriptionId() {
        return properties.getDescriptionId();
    }

    /**
     * Returns the density of this fluid type.
     *
     * @return density
     */
    public int getDensity() {
        return properties.getDensity();
    }

    /**
     * Returns the temperature of this fluid type.
     *
     * @return temperature
     */
    public int getTemperature() {
        return properties.getTemperature();
    }

    /**
     * Returns the viscosity of this fluid type.
     *
     * @return viscosity
     */
    public int getViscosity() {
        return properties.getViscosity();
    }

    /**
     * Returns the light level of this fluid type.
     *
     * @return light emission
     */
    public int getLightLevel() {
        return properties.getLightLevel();
    }
}
