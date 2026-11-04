package net.minecraftforge.server.permission.events;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.server.permission.nodes.PermissionNode;

/**
 * Event fired to collect custom permission nodes declared by active Forge mods.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PermissionGatherEvent$Nodes extends Event {

    private final List<PermissionNode<?>> nodes = new ArrayList<>();

    public PermissionGatherEvent$Nodes() {
    }

    public void addNodes(Iterable<PermissionNode<?>> toAdd) {
        if (toAdd != null) {
            for (PermissionNode<?> n : toAdd) {
                if (n != null) {
                    nodes.add(n);
                }
            }
        }
    }

    public List<PermissionNode<?>> getNodes() {
        return nodes;
    }
}
