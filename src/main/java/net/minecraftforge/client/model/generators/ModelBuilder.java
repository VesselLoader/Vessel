package net.minecraftforge.client.model.generators;

/**
 * Universal headless stub matching ModelBuilder to satisfy Create Registrate datagen references.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public class ModelBuilder<T extends ModelBuilder<T>> extends ModelFile {

    public ModelBuilder() {
        super(null);
    }

    public ModelBuilder(Object location) {
        super(location);
    }
}
