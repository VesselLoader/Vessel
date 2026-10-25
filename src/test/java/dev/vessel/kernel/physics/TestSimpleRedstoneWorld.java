package dev.vessel.kernel.physics;

import dev.vessel.kernel.optimization.physics.RedstonePowerSourceProvider;
import dev.vessel.kernel.optimization.physics.RedstoneWireNetworkAccessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Clean mock world grid implementation for testing AlternateCurrentBfsRedstoneSolver.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TestSimpleRedstoneWorld implements RedstoneWireNetworkAccessor, RedstonePowerSourceProvider {

    private final Set<Long> wireBlocks = new HashSet<>();
    private final Map<Long, Integer> wirePowers = new HashMap<>();
    private final Map<Long, Integer> externalPowers = new HashMap<>();

    public void addWire(int x, int y, int z, int initialPower) {
        long key = packKey(x, y, z);
        wireBlocks.add(key);
        wirePowers.put(key, initialPower);
    }

    public void setExternalPowerSource(int x, int y, int z, int power) {
        externalPowers.put(packKey(x, y, z), power);
    }

    @Override
    public boolean isWire(int x, int y, int z) {
        return wireBlocks.contains(packKey(x, y, z));
    }

    @Override
    public int getWirePower(int x, int y, int z) {
        return wirePowers.getOrDefault(packKey(x, y, z), 0);
    }

    @Override
    public void setWirePower(int x, int y, int z, int power) {
        wirePowers.put(packKey(x, y, z), power);
    }

    @Override
    public int getExternalPower(int x, int y, int z) {
        return externalPowers.getOrDefault(packKey(x, y, z), 0);
    }

    private static long packKey(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) y & 0xFFFL) << 26) | ((long) z & 0x3FFFFFFL);
    }
}
