package org.bukkit;

import java.util.Objects;

/**
 * Single ban entry stored inside a {@link BanList} ({@code org.bukkit.BanEntry}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BanEntry {

    private final String target;
    private volatile String source;
    private volatile String reason;

    /**
     * Constructs a new {@code BanEntry}.
     *
     * @param target non-null banned target identifier (username or IP)
     * @param reason ban reason (nullable)
     * @param source ban issuer source (defaults to {@code "Server"} if null)
     */
    public BanEntry(String target, String reason, String source) {
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.reason = reason;
        this.source = source != null ? source : "Server";
    }

    public String getTarget() {
        return target;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source != null ? source : "Server";
    }
}
