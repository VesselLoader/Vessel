package net.minecraftforge.fml.event.lifecycle;

/**
 * Lifecycle event fired when mods can send InterModComms (IMC) messages to other mods.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public class InterModEnqueueEvent extends ParallelDispatchEvent {

    public InterModEnqueueEvent() {
        super("minecraft");
    }

    public InterModEnqueueEvent(String modId) {
        super(modId);
    }
}
