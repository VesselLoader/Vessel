package dev.vessel.kernel.network.handshake;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates and renders rich-text diagnostic disconnection payloads when a connecting client
 * fails the modpack manifest audit in {@link ClientPresentationMode#STRICT_CLIENT_MODDED}.
 *
 * <p>Produces human-readable, color-formatted terminal/GUI text and standardized Minecraft JSON
 * disconnection components specifying missing mods, version conflicts, and modpack download URLs.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MissingModDisconnectionPayload {

    /**
     * Default kick screen title formatted with Minecraft color codes.
     */
    public static final String DEFAULT_KICK_SCREEN_TITLE = "§c§lCONNECTION REJECTED: MISSING REQUIRED MODS";

    /**
     * Canonical header banner border.
     */
    public static final String HEADER_BANNER = "======================= VESSEL MODPACK HANDSHAKE =======================";

    /**
     * Canonical footer banner border.
     */
    public static final String FOOTER_BANNER = "========================================================================";

    /**
     * Subtitle indicating client mismatch.
     */
    public static final String SUBTITLE_MISMATCH = "Connection rejected: Client environment does not match server modpack.";

    /**
     * Section header for missing mods.
     */
    public static final String SECTION_MISSING_MODS = "[MISSING MODS]";

    /**
     * Section header for version conflicts.
     */
    public static final String SECTION_VERSION_CONFLICTS = "[VERSION CONFLICTS]";

    /**
     * Default URL for modpack download instructions.
     */
    public static final String DEFAULT_DOWNLOAD_URL = "https://github.com/VesselLoader/Vessel";

    private final String title;
    private final List<String> missingMods;
    private final Map<String, String> versionConflicts;
    private final String downloadUrl;
    private final String formattedRichText;
    private final String jsonPayload;
    private final byte[] wirePayload;

    /**
     * Constructs an immutable MissingModDisconnectionPayload.
     *
     * @param title            screen title
     * @param missingMods      list of missing mod identifiers
     * @param versionConflicts map of mod IDs to version conflict descriptions
     * @param downloadUrl      URL for the client to retrieve the required modpack
     */
    public MissingModDisconnectionPayload(
            String title,
            List<String> missingMods,
            Map<String, String> versionConflicts,
            String downloadUrl
    ) {
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.missingMods = Collections.unmodifiableList(
                List.copyOf(Objects.requireNonNull(missingMods, "missingMods must not be null"))
        );
        this.versionConflicts = Collections.unmodifiableMap(
                Map.copyOf(Objects.requireNonNull(versionConflicts, "versionConflicts must not be null"))
        );
        this.downloadUrl = Objects.requireNonNull(downloadUrl, "downloadUrl must not be null");
        this.formattedRichText = renderRichText(this.title, this.missingMods, this.versionConflicts, this.downloadUrl);
        this.jsonPayload = renderJsonPayload(this.title, this.formattedRichText);
        this.wirePayload = this.jsonPayload.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Creates a disconnection payload with default title.
     *
     * @param missingMods      list of missing mod identifiers
     * @param versionConflicts map of mod IDs to version conflict descriptions
     * @param downloadUrl      modpack download URL
     * @return constructed payload
     */
    public static MissingModDisconnectionPayload create(
            List<String> missingMods,
            Map<String, String> versionConflicts,
            String downloadUrl
    ) {
        return new MissingModDisconnectionPayload(
                DEFAULT_KICK_SCREEN_TITLE,
                missingMods,
                versionConflicts,
                downloadUrl
        );
    }

    /**
     * Creates a disconnection payload with custom title.
     *
     * @param title            custom screen title
     * @param missingMods      list of missing mod identifiers
     * @param versionConflicts map of mod IDs to version conflict descriptions
     * @param downloadUrl      modpack download URL
     * @return constructed payload
     */
    public static MissingModDisconnectionPayload create(
            String title,
            List<String> missingMods,
            Map<String, String> versionConflicts,
            String downloadUrl
    ) {
        return new MissingModDisconnectionPayload(
                title,
                missingMods,
                versionConflicts,
                downloadUrl
        );
    }

    /**
     * Returns the title of the disconnection screen.
     *
     * @return title string
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns an unmodifiable list of missing mod identifiers.
     *
     * @return missing mods list
     */
    public List<String> getMissingMods() {
        return missingMods;
    }

    /**
     * Returns an unmodifiable map of version conflicts.
     *
     * @return version conflicts map
     */
    public Map<String, String> getVersionConflicts() {
        return versionConflicts;
    }

    /**
     * Returns the download URL provided to the player.
     *
     * @return download URL
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Returns the full formatted human-readable rich-text representation.
     *
     * @return formatted rich text
     */
    public String getFormattedRichText() {
        return formattedRichText;
    }

    /**
     * Returns the serialized JSON text component representation for network disconnect packets.
     *
     * @return JSON payload string
     */
    public String getJsonPayload() {
        return jsonPayload;
    }

    /**
     * Returns the wire binary payload in UTF-8.
     *
     * @return wire payload bytes
     */
    public byte[] getWirePayload() {
        return wirePayload.clone();
    }

    /**
     * Returns whether this payload contains any missing mods or version conflicts.
     *
     * @return {@code true} if violations exist
     */
    public boolean hasViolations() {
        return !missingMods.isEmpty() || !versionConflicts.isEmpty();
    }

    private static String renderRichText(
            String title,
            List<String> missing,
            Map<String, String> conflicts,
            String url
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER_BANNER).append('\n');
        sb.append(SUBTITLE_MISMATCH).append('\n').append('\n');

        if (!missing.isEmpty()) {
            sb.append(SECTION_MISSING_MODS).append('\n');
            for (String modId : missing) {
                sb.append("  - ").append(modId).append('\n');
            }
            sb.append('\n');
        }

        if (!conflicts.isEmpty()) {
            sb.append(SECTION_VERSION_CONFLICTS).append('\n');
            for (Map.Entry<String, String> entry : conflicts.entrySet()) {
                sb.append("  - ").append(entry.getKey()).append(" (").append(entry.getValue()).append(')').append('\n');
            }
            sb.append('\n');
        }

        sb.append("Please download the verified modpack update at:\n");
        sb.append(url).append('\n');
        sb.append(FOOTER_BANNER);
        return sb.toString();
    }

    private static String renderJsonPayload(String title, String richText) {
        String escapedText = richText.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
        String escapedTitle = title.replace("\\", "\\\\")
                .replace("\"", "\\\"");

        return "{\"text\":\"" + escapedTitle + "\\n\\n" + escapedText + "\",\"color\":\"red\"}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MissingModDisconnectionPayload other)) {
            return false;
        }
        return Objects.equals(title, other.title)
                && Objects.equals(missingMods, other.missingMods)
                && Objects.equals(versionConflicts, other.versionConflicts)
                && Objects.equals(downloadUrl, other.downloadUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, missingMods, versionConflicts, downloadUrl);
    }

    @Override
    public String toString() {
        return "MissingModDisconnectionPayload{"
                + "title='" + title + '\''
                + ", missingCount=" + missingMods.size()
                + ", conflictsCount=" + versionConflicts.size()
                + ", downloadUrl='" + downloadUrl + '\''
                + '}';
    }
}
