package net.minecraftforge.client.model.generators;

/**
 * Universal cross-era stub for Forge BlockStateProvider data generation.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public abstract class BlockStateProvider {

    private final BlockModelProvider blockModels;
    private final ItemModelProvider itemModels;

    public BlockStateProvider() {
        this.blockModels = new BlockModelProvider();
        this.itemModels = new ItemModelProvider();
    }

    public BlockModelProvider models() {
        return this.blockModels;
    }

    public ItemModelProvider itemModels() {
        return this.itemModels;
    }

    protected abstract void registerStatesAndModels();
}
