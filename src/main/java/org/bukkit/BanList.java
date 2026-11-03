package org.bukkit;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a server ban list categorized by {@link BanListType} ({@code org.bukkit.BanList}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BanList {

    private final BanListType type;
    private final Map<String, BanEntry> entries = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code BanList} for the specified {@link BanListType}.
     *
     * @param type non-null ban list type
     */
    public BanList(BanListType type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public BanListType getType() {
        return type;
    }

    /**
     * Adds a ban entry to this list.
     *
     * @param target non-null target identifier
     * @param reason ban reason (nullable)
     * @param source ban issuer source (nullable)
     * @return created {@link BanEntry}
     */
    public BanEntry addBan(String target, String reason, String source) {
        Objects.requireNonNull(target, "target must not be null");
        BanEntry entry = new BanEntry(target, reason, source);
        entries.put(target.trim().toLowerCase(Locale.ROOT), entry);
        return entry;
    }

    /**
     * Retrieves the {@link BanEntry} for the specified target, or {@code null} if not banned.
     *
     * @param target target identifier
     * @return ban entry or {@code null}
     */
    public BanEntry getBanEntry(String target) {
        if (target == null) {
            return null;
        }
        return entries.get(target.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Checks whether the specified target is currently banned on this list.
     *
     * @param target target identifier
     * @return {@code true} if banned
     */
    public boolean isBanned(String target) {
        return target != null && entries.containsKey(target.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Pardons (removes) any active ban for the specified target.
     *
     * @param target target identifier
     */
    public void pardon(String target) {
        if (target != null) {
            entries.remove(target.trim().toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Returns an unmodifiable snapshot of all active {@link BanEntry}s in this list.
     *
     * @return set of ban entries
     */
    public Set<BanEntry> getBanEntries() {
        return Collections.unmodifiableSet(new HashSet<>(entries.values()));
    }
}
