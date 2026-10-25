package dev.vessel.kernel.physics;

import dev.vessel.kernel.optimization.physics.ExplosionBlockEntry;
import dev.vessel.kernel.optimization.physics.ExplosionUpdateConsumer;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete test collector for batched explosion block changes.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TestExplosionBlockCollector implements ExplosionUpdateConsumer {

    private final List<ExplosionBlockEntry> recordedChanges = new ArrayList<>();

    @Override
    public void applyBlockChange(int x, int y, int z, int newBlockStateId) {
        recordedChanges.add(new ExplosionBlockEntry(x, y, z, newBlockStateId));
    }

    public List<ExplosionBlockEntry> getRecordedChanges() {
        return recordedChanges;
    }

    public int getCount() {
        return recordedChanges.size();
    }
}
