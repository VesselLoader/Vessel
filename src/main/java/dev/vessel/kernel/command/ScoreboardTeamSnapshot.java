package dev.vessel.kernel.command;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable snapshot record representing a Scoreboard Team.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @param name                   unique team identifier
 * @param displayName            display title
 * @param prefix                 team member prefix
 * @param suffix                 team member suffix
 * @param color                  formatting color name
 * @param allowFriendlyFire      friendly fire flag
 * @param seeFriendlyInvisibles  whether teammates can see invisible teammates
 * @param members                unmodifiable set of member player names
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record ScoreboardTeamSnapshot(
        String name,
        String displayName,
        String prefix,
        String suffix,
        String color,
        boolean allowFriendlyFire,
        boolean seeFriendlyInvisibles,
        Set<String> members
) {

    /**
     * Compact constructor creating an unmodifiable copy of member names.
     */
    public ScoreboardTeamSnapshot {
        Objects.requireNonNull(name, "name cannot be null");
        displayName = (displayName != null) ? displayName : name;
        prefix = (prefix != null) ? prefix : "";
        suffix = (suffix != null) ? suffix : "";
        color = (color != null) ? color : "white";
        members = (members != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(members)) : Collections.emptySet();
    }
}
