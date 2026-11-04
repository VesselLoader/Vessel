package net.minecraftforge.event.level;

import net.minecraftforge.common.util.BlockSnapshot;

/**
 * Event fired when a block is placed by an entity ({@code net.minecraftforge.event.level.BlockEvent$EntityPlaceEvent}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public class BlockEvent$EntityPlaceEvent extends BlockEvent {

    private final Object blockSnapshot;
    private final Object placedBlock;
    private final Object entity;

    public BlockEvent$EntityPlaceEvent() {
        super();
        this.blockSnapshot = null;
        this.placedBlock = null;
        this.entity = null;
    }

    public BlockEvent$EntityPlaceEvent(BlockSnapshot blockSnapshot, Object placedBlock, Object entity) {
        super();
        this.blockSnapshot = blockSnapshot;
        this.placedBlock = placedBlock;
        this.entity = entity;
    }

    public BlockEvent$EntityPlaceEvent(Object blockSnapshot, Object placedBlock, Object entity) {
        super();
        this.blockSnapshot = blockSnapshot;
        this.placedBlock = placedBlock;
        this.entity = entity;
    }

    public Object getEntity() {
        return entity;
    }

    public Object getPlacedBlock() {
        return placedBlock;
    }

    public Object getBlockSnapshot() {
        return blockSnapshot;
    }
}
