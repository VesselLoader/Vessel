package dev.vessel.kernel.architectury;

/**
 * Listener interface for intercepting server-side GeckoLib animation triggers and broadcasting
 * synchronization packets to client networking channels.
 *
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface GeckoAnimationPacketListener {

    /**
     * Invoked when an animation is triggered on the server and must be dispatched to clients.
     *
     * @param animatableId target entity or block entity identifier
     * @param controllerName animation controller name
     * @param animationName animation clip name
     * @param loop whether the animation is looping
     * @param transitionSpeed animation playback speed
     */
    void onAnimationTriggered(
            String animatableId,
            String controllerName,
            String animationName,
            boolean loop,
            double transitionSpeed
    );
}
