package net.minecraftforge.client.model.generators;

/**
 * Universal cross-era stub for Forge ConfiguredModel data generation.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class ConfiguredModel {

    public final ModelFile model;
    public final int rotationX;
    public final int rotationY;
    public final boolean uvLock;
    public final int weight;

    public ConfiguredModel(ModelFile model) {
        this(model, 0, 0, false, 1);
    }

    public ConfiguredModel(ModelFile model, int rotationX, int rotationY, boolean uvLock) {
        this(model, rotationX, rotationY, uvLock, 1);
    }

    public ConfiguredModel(ModelFile model, int rotationX, int rotationY, boolean uvLock, int weight) {
        this.model = model;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.uvLock = uvLock;
        this.weight = weight;
    }

    public static ConfiguredModel[] allYRotations(ModelFile model, int rotX, boolean uvlock) {
        ConfiguredModel[] array = new ConfiguredModel[4];
        array[0] = new ConfiguredModel(model, rotX, 0, uvlock);
        array[1] = new ConfiguredModel(model, rotX, 90, uvlock);
        array[2] = new ConfiguredModel(model, rotX, 180, uvlock);
        array[3] = new ConfiguredModel(model, rotX, 270, uvlock);
        return array;
    }
}
