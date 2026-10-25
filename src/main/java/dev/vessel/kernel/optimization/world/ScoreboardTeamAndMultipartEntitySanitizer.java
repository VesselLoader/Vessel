package dev.vessel.kernel.optimization.world;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Purges orphaned {@code PlayerTeam} entries left behind in {@code scoreboard.dat}
 * by dead modded boss/affix mobs (Apotheosis, Cataclysm, Champions, MythicMobs) and
 * cleans up lingering multipart sub-entity hitboxes ({@code PartEntity} / {@code dragonParts})
 * when chunks unload (Ice &amp; Fire dragons, Twilight Forest Hydra/Naga, Mowzie's Mobs).
 *
 * @version 1.0.0
 */
public final class ScoreboardTeamAndMultipartEntitySanitizer {

    private final Map<String, UUID> mobTeamToEntityUuid = new ConcurrentHashMap<>();
    private final Map<Long, Integer> multipartCountByChunk = new ConcurrentHashMap<>();

    /**
     * Creates a new scoreboard team and multipart entity sanitizer.
     */
    public ScoreboardTeamAndMultipartEntitySanitizer() {
    }

    /**
     * Associates a temporary scoreboard team name with a modded mob entity UUID.
     *
     * @param teamName      scoreboard team identifier
     * @param mobEntityUuid UUID of the modded mob
     */
    public void registerMobScoreboardTeam(String teamName, UUID mobEntityUuid) {
        Objects.requireNonNull(teamName, "teamName must not be null");
        Objects.requireNonNull(mobEntityUuid, "mobEntityUuid must not be null");
        mobTeamToEntityUuid.put(teamName.trim(), mobEntityUuid);
    }

    /**
     * Purges all registered mob scoreboard teams whose associated entity no longer
     * satisfies {@code isEntityStillAlive}, keeping {@code scoreboard.dat} compact.
     *
     * @param isEntityStillAlive predicate returning {@code true} if the entity is still alive
     * @return number of orphaned mob scoreboard teams purged
     */
    public int purgeOrphanedMobTeams(Predicate<UUID> isEntityStillAlive) {
        Objects.requireNonNull(isEntityStillAlive, "isEntityStillAlive must not be null");
        int purged = 0;
        Iterator<Map.Entry<String, UUID>> iterator = mobTeamToEntityUuid.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, UUID> entry = iterator.next();
            if (!isEntityStillAlive.test(entry.getValue())) {
                iterator.remove();
                purged++;
            }
        }
        return purged;
    }

    /**
     * Registers multipart sub-entity hitbox IDs ({@code PartEntity}) belonging to a
     * multi-part boss inside {@code chunkCoord}.
     *
     * @param parentBossUuid UUID of the parent boss entity
     * @param chunkCoord     64-bit packed chunk coordinate
     * @param partEntityIds  array of numeric sub-entity hitbox IDs
     */
    public void registerMultipartSubEntities(
            UUID parentBossUuid, long chunkCoord, int[] partEntityIds) {
        Objects.requireNonNull(parentBossUuid, "parentBossUuid must not be null");
        Objects.requireNonNull(partEntityIds, "partEntityIds must not be null");
        if (partEntityIds.length > 0) {
            multipartCountByChunk.merge(chunkCoord, partEntityIds.length, Integer::sum);
        }
    }

    /**
     * Cleans up and removes all lingering multipart sub-entity hitboxes associated with
     * {@code unloadedChunkCoord}.
     *
     * @param unloadedChunkCoord 64-bit packed chunk coordinate being unloaded
     * @return number of multipart sub-entity hitboxes cleaned up
     */
    public int cleanupUnloadedMultipartHitboxes(long unloadedChunkCoord) {
        Integer removed = multipartCountByChunk.remove(unloadedChunkCoord);
        return removed != null ? removed : 0;
    }
}
