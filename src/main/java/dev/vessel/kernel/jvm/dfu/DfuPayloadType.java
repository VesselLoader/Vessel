package dev.vessel.kernel.jvm.dfu;

/**
 * Minecraft payload types processed through DataFixerUpper schemas.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum DfuPayloadType {
    ITEM_STACK,
    BLOCK_ENTITY,
    ENTITY,
    CHUNK,
    LEVEL_DATA,
    PLAYER_DATA
}
