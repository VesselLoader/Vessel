package dev.vessel.kernel.resourcepack.bedrock;

import java.util.Objects;

/**
 * Immutable descriptor for an individual Bedrock language translation entry.
 * <p>
 * Stores key-value translation mappings normalized for Bedrock Edition, along with
 * source namespace lineage for auditability and debugging.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @param key Bedrock localization key (e.g. "tile.techmod.crusher.name", "item.techmod.wrench.name")
 * @param value localized display text
 * @param sourceNamespace origin namespace from Java resource pack (e.g. "techmod", "minecraft")
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public record VesselBedrockLanguageEntry(
        String key,
        String value,
        String sourceNamespace
) {

    public VesselBedrockLanguageEntry {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(sourceNamespace, "sourceNamespace cannot be null");
    }

    /**
     * Formats this entry as a standard Bedrock {@code .lang} line ({@code <key>=<value>}).
     * <p>
     * Ensures any raw newline characters are escaped as literal {@code \n} so that each entry
     * occupies exactly one line in the output file.
     * </p>
     *
     * @return formatted line string
     */
    public String toLangLine() {
        String escapedValue = value.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
        return key + "=" + escapedValue;
    }
}
