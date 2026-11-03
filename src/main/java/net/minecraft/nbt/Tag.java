package net.minecraft.nbt;

/**
 * Base contract for Minecraft Named Binary Tag (NBT) data elements
 * ({@code net.minecraft.nbt.Tag}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface Tag {

    byte TAG_END = 0;
    byte TAG_BYTE = 1;
    byte TAG_SHORT = 2;
    byte TAG_INT = 3;
    byte TAG_LONG = 4;
    byte TAG_FLOAT = 5;
    byte TAG_DOUBLE = 6;
    byte TAG_BYTE_ARRAY = 7;
    byte TAG_STRING = 8;
    byte TAG_LIST = 9;
    byte TAG_COMPOUND = 10;

    /**
     * Returns the binary NBT type ID of this tag.
     *
     * @return NBT tag type ID
     */
    byte getId();

    /**
     * Returns the string representation of this tag's value.
     *
     * @return string representation
     */
    String getAsString();

    /**
     * Creates a deep copy of this NBT tag.
     *
     * @return deep copy of this tag
     */
    Tag copy();
}
