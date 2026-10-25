package dev.vessel.kernel.architectury;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test mock tracking GeckoLib animation packets dispatched by the runtime adapter.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SampleTrackingPacketListener implements GeckoAnimationPacketListener {

    private final List<String> triggeredAnimations = new ArrayList<>();

    public SampleTrackingPacketListener() {
    }

    @Override
    public void onAnimationTriggered(
            String animatableId,
            String controllerName,
            String animationName,
            boolean loop,
            double transitionSpeed) {
        triggeredAnimations.add(animatableId + "::" + controllerName + "::" + animationName + "::" + loop);
    }

    public List<String> getTriggeredAnimations() {
        return Collections.unmodifiableList(triggeredAnimations);
    }

    public void clear() {
        triggeredAnimations.clear();
    }
}
