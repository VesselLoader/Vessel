package dev.vessel.kernel.datapack;

/**
 * Standard Minecraft Named Binary Tag (NBT) type identifiers.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum NbtTagType {
    TAG_END(0),
    TAG_BYTE(1),
    TAG_SHORT(2),
    TAG_INT(3),
    TAG_LONG(4),
    TAG_FLOAT(5),
    TAG_DOUBLE(6),
    TAG_BYTE_ARRAY(7),
    TAG_STRING(8),
    TAG_LIST(9),
    TAG_COMPOUND(10),
    TAG_INT_ARRAY(11),
    TAG_LONG_ARRAY(12);

    private final byte id;

    NbtTagType(int id) {
        this.id = (byte) id;
    }

    /**
     * Obtains the byte ID of the tag.
     *
     * @return byte ID
     */
    public byte getId() {
        return id;
    }

    /**
     * Resolves an NbtTagType by primitive byte ID.
     *
     * @param id byte ID
     * @return matching type or TAG_END if unknown
     */
    public static NbtTagType fromId(byte id) {
        return switch (id) {
            case 1 -> TAG_BYTE;
            case 2 -> TAG_SHORT;
            case 3 -> TAG_INT;
            case 4 -> TAG_LONG;
            case 5 -> TAG_FLOAT;
            case 6 -> TAG_DOUBLE;
            case 7 -> TAG_BYTE_ARRAY;
            case 8 -> TAG_STRING;
            case 9 -> TAG_LIST;
            case 10 -> TAG_COMPOUND;
            case 11 -> TAG_INT_ARRAY;
            case 12 -> TAG_LONG_ARRAY;
            default -> TAG_END;
        };
    }
}
