package dev.vessel.kernel.optimization;

import dev.vessel.bootstrap.target.ServerTickLoopInjector;
import dev.vessel.kernel.gpu.GpuComputeDispatcher;
import dev.vessel.kernel.optimization.entity.EntityActivationRangeLOD;
import dev.vessel.kernel.optimization.physics.EntityDormancyTracker;
import dev.vessel.kernel.optimization.supermod.VesselExplosionFieldAccessor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sovereign runtime bytecode hook bridge invoked by ASM-instrumented Minecraft server classes.
 * <p>
 * Connects Vanilla {@code MinecraftServer.tickServer()}, {@code Entity.tick()}, {@code Explosion.explode()},
 * and POI queries directly to active VESSEL optimization engines and the GPU/SIMD accelerator with zero reflection.
 * </p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 * @author VESSEL Engine Team
 */
public final class VesselOptimizationRuntimeHooks {

    private static final EntityDormancyTracker DORMANCY_TRACKER = new EntityDormancyTracker();
    private static final EntityActivationRangeLOD ENTITY_LOD =
            VesselOptimizationCoordinator.getActiveCoordinator().getEntityLod();
    private static final ServerTickLoopInjector TICK_INJECTOR = new ServerTickLoopInjector();
    private static final AtomicLong SERVER_TICK_COUNTER = new AtomicLong(0L);

    private VesselOptimizationRuntimeHooks() {
    }

    /**
     * Hook invoked at the start of a server tick.
     */
    public static void onServerTickStart() {
        long tick = SERVER_TICK_COUNTER.incrementAndGet();
        TICK_INJECTOR.onServerTickStart(tick);
    }

    /**
     * Hook invoked at the start of a server tick with an explicit tick counter.
     *
     * @param tickNumber monotonic tick index
     */
    public static void onServerTickStart(long tickNumber) {
        long tick = tickNumber > 0L ? tickNumber : SERVER_TICK_COUNTER.incrementAndGet();
        TICK_INJECTOR.onServerTickStart(tick);
    }

    /**
     * Hook invoked at the completion of a server tick.
     */
    public static void onServerTickEnd() {
        long tick = SERVER_TICK_COUNTER.get();
        TICK_INJECTOR.onServerTickEnd(tick);
    }

    /**
     * Hook invoked at the completion of a server tick with an explicit tick counter.
     *
     * @param tickNumber monotonic tick index
     */
    public static void onServerTickEnd(long tickNumber) {
        long tick = tickNumber > 0L ? tickNumber : SERVER_TICK_COUNTER.get();
        TICK_INJECTOR.onServerTickEnd(tick);
    }

    /**
     * Hook invoked at the very beginning of {@code Entity.tick()}.
     *
     * @param entityId unique entity ID
     * @param posX     current coordinate X
     * @param posY     current coordinate Y
     * @param posZ     current coordinate Z
     * @return true if entity should proceed with heavy physics and brain calculations
     */
    public static boolean onEntityTick(int entityId, double posX, double posY, double posZ) {
        return onEntityTick(entityId, posX, posY, posZ, false);
    }

    /**
     * Hook invoked at the very beginning of {@code Entity.tick()} with player discrimination.
     *
     * @param entityId unique entity ID
     * @param posX     current coordinate X
     * @param posY     current coordinate Y
     * @param posZ     current coordinate Z
     * @param isPlayer whether this entity is an active player
     * @return true if entity should proceed with heavy physics and brain calculations
     */
    public static boolean onEntityTick(int entityId, double posX, double posY, double posZ, boolean isPlayer) {
        if (isPlayer) {
            ENTITY_LOD.updatePlayerPosition(entityId, posX, posY, posZ);
            return true;
        }
        DORMANCY_TRACKER.recordTickPosition(entityId, posX, posY, posZ);
        long tick = SERVER_TICK_COUNTER.get();
        return ENTITY_LOD.shouldTickEntity(entityId, "minecraft:generic", posX, posY, posZ, tick);
    }

    /**
     * Hook invoked when an explosion triggers spherical raycasting.
     *
     * @param originX explosion epicenter X
     * @param originY explosion epicenter Y
     * @param originZ explosion epicenter Z
     * @param power   explosion power
     * @param rays    ray count to compute
     * @return array of packed affected block coordinates
     */
    public static long[] onExplosionRaycast(double originX, double originY, double originZ, float power, int rays) {
        return GpuComputeDispatcher.getActiveDispatcher().dispatchExplosionRaycasts(
                originX, originY, originZ, power, rays);
    }

    /**
     * Hook invoked at the entry of {@code Explosion.explode()}.
     *
     * @param explosion instance of Minecraft Explosion
     */
    public static void onExplosionStart(Object explosion) {
        if (explosion == null) {
            return;
        }
        VesselExplosionFieldAccessor.dispatchAcceleratedExplosion(explosion);
    }

    /**
     * Returns the global runtime dormancy tracker instance.
     *
     * @return active dormancy tracker
     */
    public static EntityDormancyTracker getDormancyTracker() {
        return DORMANCY_TRACKER;
    }

    /**
     * Returns the global runtime entity LOD manager.
     *
     * @return active entity LOD manager
     */
    public static EntityActivationRangeLOD getEntityLod() {
        return ENTITY_LOD;
    }

    /**
     * Returns the global runtime tick loop injector instance.
     *
     * @return active tick loop injector
     */
    public static ServerTickLoopInjector getTickInjector() {
        return TICK_INJECTOR;
    }
}
