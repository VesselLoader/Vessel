package dev.vessel.kernel.event.orchestration;

import dev.vessel.kernel.event.EventExecutionPhase;
import dev.vessel.kernel.identity.SourceLoaderKind;

/**
 * Deterministic multi-loader event priority and sub-phase topological order mapper
 * implementing {@code ADR-0003} ({@code VES-EVENT-001}).
 *
 * <p>Reconciles inverted priority conventions across Forge/NeoForge ({@code HIGHEST} runs first)
 * and Bukkit/Spigot/Paper/Folia ({@code LOWEST} runs first) into VESSEL's 6-tier
 * {@link EventExecutionPhase} pipeline, and enforces deterministic intra-phase topological
 * ordering ({@code Fabric/Quilt -> Forge/NeoForge -> Bukkit/Paper/Folia}).</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MultiLoaderPriorityPhaseMapper {

    /**
     * Constructs a new multi-loader priority phase mapper.
     */
    public MultiLoaderPriorityPhaseMapper() {
    }

    /**
     * Maps a Minecraft Forge {@link net.minecraftforge.eventbus.api.EventPriority} to the
     * corresponding universal {@link EventExecutionPhase}.
     *
     * @param priority Forge event priority (nullable, defaults to {@link EventExecutionPhase#NORMAL})
     * @return mapped universal execution phase
     */
    public EventExecutionPhase mapForgePriority(net.minecraftforge.eventbus.api.EventPriority priority) {
        if (priority == null) {
            return EventExecutionPhase.NORMAL;
        }
        return switch (priority) {
            case HIGHEST -> EventExecutionPhase.EARLIEST;
            case HIGH -> EventExecutionPhase.EARLY;
            case NORMAL -> EventExecutionPhase.NORMAL;
            case LOW -> EventExecutionPhase.LATE;
            case LOWEST -> EventExecutionPhase.LATEST;
        };
    }

    /**
     * Maps a NeoForge {@link net.neoforged.bus.api.EventPriority} to the corresponding
     * universal {@link EventExecutionPhase}.
     *
     * @param priority NeoForge event priority (nullable, defaults to {@link EventExecutionPhase#NORMAL})
     * @return mapped universal execution phase
     */
    public EventExecutionPhase mapNeoForgePriority(net.neoforged.bus.api.EventPriority priority) {
        if (priority == null) {
            return EventExecutionPhase.NORMAL;
        }
        return switch (priority) {
            case HIGHEST -> EventExecutionPhase.EARLIEST;
            case HIGH -> EventExecutionPhase.EARLY;
            case NORMAL -> EventExecutionPhase.NORMAL;
            case LOW -> EventExecutionPhase.LATE;
            case LOWEST -> EventExecutionPhase.LATEST;
        };
    }

    /**
     * Maps a Bukkit/Spigot/Paper/Folia {@link org.bukkit.event.EventPriority} to the corresponding
     * universal {@link EventExecutionPhase}.
     *
     * @param priority Bukkit event priority (nullable, defaults to {@link EventExecutionPhase#NORMAL})
     * @return mapped universal execution phase
     */
    public EventExecutionPhase mapBukkitPriority(org.bukkit.event.EventPriority priority) {
        if (priority == null) {
            return EventExecutionPhase.NORMAL;
        }
        return switch (priority) {
            case LOWEST -> EventExecutionPhase.EARLIEST;
            case LOW -> EventExecutionPhase.EARLY;
            case NORMAL -> EventExecutionPhase.NORMAL;
            case HIGH -> EventExecutionPhase.LATE;
            case HIGHEST -> EventExecutionPhase.LATEST;
            case MONITOR -> EventExecutionPhase.MONITOR;
        };
    }

    /**
     * Resolves the intra-phase loader topological sub-order defined by {@code ADR-0003}
     * ({@code VES-EVENT-001}).
     *
     * <ul>
     *   <li>{@code 0}: {@link SourceLoaderKind#FABRIC}, {@link SourceLoaderKind#QUILT},
     *       and {@link SourceLoaderKind#VESSEL_NATIVE}</li>
     *   <li>{@code 1}: {@link SourceLoaderKind#FORGE} and {@link SourceLoaderKind#NEOFORGE}</li>
     *   <li>{@code 2}: {@link SourceLoaderKind#BUKKIT}, {@link SourceLoaderKind#SPIGOT},
     *       {@link SourceLoaderKind#PAPER}, and {@link SourceLoaderKind#FOLIA}</li>
     *   <li>{@code 3}: {@code null} or unrecognized loader kind</li>
     * </ul>
     *
     * @param loaderKind originating loader ecosystem kind
     * @return deterministic numeric sub-phase index ({@code 0..3})
     */
    public int resolveLoaderTopologicalOrder(SourceLoaderKind loaderKind) {
        if (loaderKind == null) {
            return 3;
        }
        return switch (loaderKind) {
            case FABRIC, QUILT, VESSEL_NATIVE -> 0;
            case FORGE, NEOFORGE -> 1;
            case BUKKIT, SPIGOT, PAPER, FOLIA -> 2;
        };
    }

    /**
     * Compares two loader kinds according to their {@code ADR-0003} intra-phase topological sub-order.
     *
     * @param firstLoader  first loader ecosystem kind
     * @param secondLoader second loader ecosystem kind
     * @return negative if {@code firstLoader} executes before {@code secondLoader}, zero if equal,
     *         or positive if {@code firstLoader} executes after {@code secondLoader}
     */
    public int compareSubPhaseOrder(SourceLoaderKind firstLoader, SourceLoaderKind secondLoader) {
        return Integer.compare(
                resolveLoaderTopologicalOrder(firstLoader),
                resolveLoaderTopologicalOrder(secondLoader));
    }
}
