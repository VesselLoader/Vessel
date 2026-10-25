package dev.vessel.kernel.physics;

import dev.vessel.kernel.optimization.physics.ExplosionLightUpdateConsumer;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete test collector for batched chunk section lighting update triggers.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TestExplosionLightCollector implements ExplosionLightUpdateConsumer {

    private final List<Long> relitSections = new ArrayList<>();

    @Override
    public void scheduleSectionRelight(int sectionX, int sectionY, int sectionZ) {
        long key = (((long) sectionX & 0x3FFFFFL) << 42) | (((long) sectionY & 0xFFFFF) << 22) | ((long) sectionZ & 0x3FFFFFL);
        relitSections.add(key);
    }

    public List<Long> getRelitSections() {
        return relitSections;
    }

    public int getCount() {
        return relitSections.size();
    }
}
