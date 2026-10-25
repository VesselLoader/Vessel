package dev.vessel.kernel.registry;

/**
 * Data types supported by Data Components in Minecraft 1.20.5+ for direct O(1) serialization.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum DataComponentValueType {
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN,
    STRING,
    LIST,
    COMPOUND_BLOB
}
