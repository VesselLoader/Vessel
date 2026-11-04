package net.minecraftforge.fluids;

/**
 * Universal multi-era properties builder for {@link FluidType}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FluidType$Properties {

    private String descriptionId;
    private int density = 1000;
    private int temperature = 300;
    private int viscosity = 1000;
    private int lightLevel = 0;

    /**
     * Constructs a default fluid properties instance.
     */
    public FluidType$Properties() {
    }

    /**
     * Creates a new fluid properties builder.
     *
     * @return fresh properties instance
     */
    public static FluidType$Properties create() {
        return new FluidType$Properties();
    }

    /**
     * Sets the description translation key ID.
     *
     * @param descriptionId description key
     * @return this properties instance
     */
    public FluidType$Properties descriptionId(String descriptionId) {
        this.descriptionId = descriptionId;
        return this;
    }

    /**
     * Sets the fluid density in kg/m3.
     *
     * @param density density value
     * @return this properties instance
     */
    public FluidType$Properties density(int density) {
        this.density = density;
        return this;
    }

    /**
     * Sets the temperature in Kelvin.
     *
     * @param temperature temperature
     * @return this properties instance
     */
    public FluidType$Properties temperature(int temperature) {
        this.temperature = temperature;
        return this;
    }

    /**
     * Sets the viscosity in mPa.s.
     *
     * @param viscosity viscosity
     * @return this properties instance
     */
    public FluidType$Properties viscosity(int viscosity) {
        this.viscosity = viscosity;
        return this;
    }

    /**
     * Sets the light emission level.
     *
     * @param lightLevel light level [0, 15]
     * @return this properties instance
     */
    public FluidType$Properties lightLevel(int lightLevel) {
        this.lightLevel = lightLevel;
        return this;
    }

    public String getDescriptionId() {
        return descriptionId;
    }

    public int getDensity() {
        return density;
    }

    public int getTemperature() {
        return temperature;
    }

    public int getViscosity() {
        return viscosity;
    }

    public int getLightLevel() {
        return lightLevel;
    }
}
