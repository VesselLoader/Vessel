package dev.vessel.kernel.tick;

import java.util.HashSet;
import java.util.Set;

/**
 * Test implementation of {@link PathPassabilityPredicate} supporting obstacle definitions.
 * <p>
 * Ensures zero anonymous classes in tests (Rule 17).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TestPathPassabilityPredicate implements PathPassabilityPredicate {

    private final Set<Long> obstacles = new HashSet<>();

    /**
     * Adds an impassable obstacle at (x, y, z).
     *
     * @param x world X
     * @param y world Y
     * @param z world Z
     */
    public void addObstacle(int x, int y, int z) {
        obstacles.add(packKey(x, y, z));
    }

    @Override
    public boolean isPassable(int x, int y, int z) {
        return !obstacles.contains(packKey(x, y, z));
    }

    private static long packKey(int x, int y, int z) {
        return (((long) (x & 0x3FFFFFF)) << 38)
                | (((long) (z & 0x3FFFFFF)) << 12)
                | ((long) (y & 0xFFF));
    }
}
