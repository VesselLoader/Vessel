package net.minecraftforge.fml.event.lifecycle;

import java.util.stream.Stream;

/**
 * Lifecycle event fired when mods can process received InterModComms (IMC) messages.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public class InterModProcessEvent extends ParallelDispatchEvent {

    public InterModProcessEvent() {
        super("minecraft");
    }

    public InterModProcessEvent(String modId) {
        super(modId);
    }

    public Stream<?> getIMCStream() {
        return Stream.empty();
    }
}
