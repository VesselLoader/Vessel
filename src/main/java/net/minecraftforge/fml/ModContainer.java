package net.minecraftforge.fml;

/**
 * Universal multi-era container wrapper for Forge mod modules.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public class ModContainer {

    private final String modId;

    public ModContainer() {
        this("minecraft");
    }

    public ModContainer(String modId) {
        this.modId = (modId == null || modId.isBlank()) ? "minecraft" : modId.trim();
    }

    public String getModId() {
        return modId;
    }
}
