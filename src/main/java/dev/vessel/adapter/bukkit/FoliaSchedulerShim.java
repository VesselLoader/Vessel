package dev.vessel.adapter.bukkit;

import dev.vessel.kernel.actor.GlobalRegionActorStrand;
import dev.vessel.kernel.actor.RegionalActorStrand;
import dev.vessel.kernel.actor.primitive.RegionStrandRouter;
import java.util.Objects;
import java.util.UUID;

/**
 * Bridges Folia's {@code RegionizedServer}, {@code RegionScheduler}, {@code GlobalRegionScheduler},
 * and {@code EntityScheduler} directly onto VESSEL's {@link RegionalActorStrand} and
 * {@link GlobalRegionActorStrand} infrastructure.
 *
 * @version 1.0.0
 */
public final class FoliaSchedulerShim {

    /**
     * Schedules a task on the {@link RegionalActorStrand} owning the specified chunk coordinates.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @param task   the task to execute on the region strand
     * @param router the spatial region strand router
     * @return {@code true} if the target region strand is active and accepted the task
     */
    public boolean scheduleRegionTask(
            int chunkX,
            int chunkZ,
            Runnable task,
            RegionStrandRouter router) {
        Objects.requireNonNull(task, "task must not be null");
        Objects.requireNonNull(router, "router must not be null");
        RegionalActorStrand strand = router.routeByChunk(chunkX, chunkZ);
        if (strand == null || strand.isShutdown()) {
            return false;
        }
        strand.post(task);
        return true;
    }

    /**
     * Schedules a server-wide task (weather, scoreboard, daytime, console command) on the
     * {@link GlobalRegionActorStrand}.
     *
     * @param task         the task to execute on the global region strand
     * @param globalStrand the pinned global region actor strand
     * @return {@code true} if the global strand is active and accepted the task
     */
    public boolean scheduleGlobalRegionTask(
            Runnable task,
            GlobalRegionActorStrand globalStrand) {
        Objects.requireNonNull(task, "task must not be null");
        Objects.requireNonNull(globalStrand, "globalStrand must not be null");
        if (globalStrand.isShutdown()) {
            return false;
        }
        globalStrand.post(task);
        return true;
    }

    /**
     * Schedules an entity-bound task on the {@link RegionalActorStrand} currently owning the
     * entity's chunk coordinates.
     *
     * @param entityUuid    the target entity's UUID
     * @param currentChunkX the entity's current chunk X coordinate
     * @param currentChunkZ the entity's current chunk Z coordinate
     * @param task          the entity task to execute
     * @param router        the spatial region strand router
     * @return {@code true} if the owning region strand accepted the entity task
     */
    public boolean scheduleEntityTask(
            UUID entityUuid,
            int currentChunkX,
            int currentChunkZ,
            Runnable task,
            RegionStrandRouter router) {
        Objects.requireNonNull(entityUuid, "entityUuid must not be null");
        return scheduleRegionTask(currentChunkX, currentChunkZ, task, router);
    }
}
