package net.minecraftforge.client.model.generators;

/**
 * Headless server-safe stub for Forge ModelFile to prevent NoClassDefFoundError during mod datagen registration.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public class ModelFile {

    protected Object location;

    public ModelFile() {
    }

    public ModelFile(Object location) {
        this.location = location;
    }

    public Object getLocation() {
        return location;
    }
}
