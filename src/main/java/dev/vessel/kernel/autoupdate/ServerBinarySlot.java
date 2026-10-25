package dev.vessel.kernel.autoupdate;

import java.util.Locale;
import java.util.Objects;

/**
 * Enumeration representing the dual binary execution slots ({@code SLOT_A} and {@code SLOT_B})
 * for zero-downtime and Windows file-lock safe server updates.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public enum ServerBinarySlot {

    /**
     * Binary Slot A, corresponding to executable artifact {@code server-slot-a.jar}.
     */
    SLOT_A("server-slot-a.jar"),

    /**
     * Binary Slot B, corresponding to executable artifact {@code server-slot-b.jar}.
     */
    SLOT_B("server-slot-b.jar");

    private final String binaryFileName;

    ServerBinarySlot(String binaryFileName) {
        this.binaryFileName = Objects.requireNonNull(binaryFileName, "binaryFileName must not be null");
    }

    /**
     * Returns the relative file name of the binary associated with this slot.
     *
     * @return binary file name (e.g. {@code server-slot-a.jar})
     */
    public String binaryFileName() {
        return this.binaryFileName;
    }

    /**
     * Resolves the opposite/standby slot for an alternate-side staged update.
     *
     * @return the opposite {@link ServerBinarySlot}
     */
    public ServerBinarySlot alternateSlot() {
        return this == SLOT_A ? SLOT_B : SLOT_A;
    }

    /**
     * Parses a string representation into a valid {@link ServerBinarySlot}.
     *
     * @param rawName raw name, slot identifier, or file name
     * @return resolved {@link ServerBinarySlot}
     * @throws IllegalArgumentException if the slot name cannot be recognized
     */
    public static ServerBinarySlot fromString(String rawName) {
        Objects.requireNonNull(rawName, "rawName must not be null");
        String normalized = rawName.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("SLOT_A") || normalized.equals("A") || normalized.contains("SLOT-A")) {
            return SLOT_A;
        }
        if (normalized.equals("SLOT_B") || normalized.equals("B") || normalized.contains("SLOT-B")) {
            return SLOT_B;
        }
        throw new IllegalArgumentException("Unknown server binary slot descriptor: " + rawName);
    }
}
