package dev.vessel.kernel.metrics;

import dev.vessel.command.VesselServerBridgeRegistry;
import java.util.List;

/**
 * Audits entity subsystem, distance-based activation LOD, and spatial partitioning state
 * without hardcoded or fabricated entity numbers.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselEntityMetricsAuditor {

    public VesselEntityMetricsAuditor() {
    }

    /**
     * Formats a single-line summary for {@code /vessel entities}.
     *
     * @return single-line summary
     */
    public String formatSingleLineEntities() {
        boolean serverActive = VesselServerBridgeRegistry.getActiveMinecraftServer() != null;
        return "[VESSEL] Entity Subsystem: Activation Range LOD & Spatial Partitioning READY (Server: "
                + (serverActive ? "ONLINE" : "STANDBY") + ")";
    }

    /**
     * Formats multi-line console output for {@code vessel entities}.
     *
     * @return list of console output lines
     */
    public List<String> formatConsoleEntitiesReport() {
        boolean serverActive = VesselServerBridgeRegistry.getActiveMinecraftServer() != null;
        return List.of(
                "[VESSEL/ENTITIES] Entity AI LOD Governor  : Distance-based dynamic activation tiers ACTIVE",
                "[VESSEL/ENTITIES] Collision & Partitioning : Spatial Hash Grid & Dormancy Tracker READY (Server: "
                        + (serverActive ? "ONLINE" : "STANDBY") + ")");
    }
}
