package net.minecraftforge.event.entity.living;

/**
 * Universal cross-era shim for Forge LivingEvent$LivingTickEvent.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class LivingEvent$LivingTickEvent extends LivingEvent {

    public LivingEvent$LivingTickEvent() {
        super();
    }

    public LivingEvent$LivingTickEvent(Object entity) {
        super(entity);
    }
}
