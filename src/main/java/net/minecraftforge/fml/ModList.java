package net.minecraftforge.fml;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Compatibility stub for Forge mod list querying ({@code net.minecraftforge.fml.ModList}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ModList {

    private static final ModList INSTANCE = new ModList();

    public ModList() {
    }

    public static ModList get() {
        return INSTANCE;
    }

    public boolean isLoaded(String modId) {
        if (modId == null) {
            return false;
        }
        String id = modId.toLowerCase();
        return "create".equals(id)
                || "mekanism".equals(id)
                || "chunky".equals(id)
                || "forge".equals(id)
                || "minecraft".equals(id);
    }

    public List<?> getMods() {
        return Collections.emptyList();
    }

    public Optional<?> getModContainerById(String modId) {
        return Optional.empty();
    }
}
