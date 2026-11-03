package dev.vessel.kernel.network.handshake;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Audits and verifies client-submitted mod manifests against active server registries.
 *
 * <p>Supports SHA-256 fast-path hash verification, semantic version matching, exact version
 * enforcement, and generates structured {@link MissingModDisconnectionPayload} upon mismatch.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ModManifestAuditEvaluator {

    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String HEX_FORMAT = "%02x";

    /**
     * Constructs a new ModManifestAuditEvaluator.
     */
    public ModManifestAuditEvaluator() {
    }

    /**
     * Computes a canonical SHA-256 hexadecimal hash representing the server's active mod registry.
     *
     * @param serverMods map of mod identifiers to required version strings
     * @return hex-encoded SHA-256 digest
     */
    public String computeModRegistryHash(Map<String, String> serverMods) {
        Objects.requireNonNull(serverMods, "serverMods must not be null");
        TreeMap<String, String> sortedMods = new TreeMap<>(serverMods);
        StringBuilder canonicalString = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedMods.entrySet()) {
            canonicalString.append(entry.getKey()).append(':').append(entry.getValue()).append(';');
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            byte[] hashBytes = digest.digest(canonicalString.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format(HEX_FORMAT, b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM missing SHA-256 message digest algorithm", e);
        }
    }

    /**
     * Performs constant-time verification of a client-provided modpack registry hash.
     *
     * @param clientProvidedHash hex hash supplied by connecting client
     * @param serverMods         map of active server mods
     * @return {@code true} if hashes match identically
     */
    public boolean verifyRegistryHash(String clientProvidedHash, Map<String, String> serverMods) {
        if (clientProvidedHash == null || clientProvidedHash.isBlank()) {
            return false;
        }
        String serverHash = computeModRegistryHash(serverMods);
        byte[] a = clientProvidedHash.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] b = serverHash.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    /**
     * Determines if a client-installed mod version satisfies the server requirement.
     *
     * @param clientVersion       version string installed on client
     * @param serverRequirement   version or version constraint required by server
     * @param enforceExactVersion when {@code true}, strings must match identically
     * @return {@code true} if compatible
     */
    public boolean isVersionCompatible(String clientVersion, String serverRequirement, boolean enforceExactVersion) {
        if (clientVersion == null || serverRequirement == null) {
            return false;
        }
        String clientTrimmed = clientVersion.trim();
        String serverTrimmed = serverRequirement.trim();

        if (clientTrimmed.equals(serverTrimmed)) {
            return true;
        }

        if (enforceExactVersion) {
            return false;
        }

        // Handle ">=" constraint syntax in server requirement (e.g. ">= 6.0.1")
        if (serverTrimmed.startsWith(">=")) {
            String targetVer = serverTrimmed.substring(2).trim();
            return compareSemVer(clientTrimmed, targetVer) >= 0;
        }

        // Permissive patch discrepancy: same major and minor version allows differing patch
        int[] clientParts = parseVersionParts(clientTrimmed);
        int[] serverParts = parseVersionParts(serverTrimmed);

        if (clientParts != null && serverParts != null) {
            if (clientParts[0] == serverParts[0] && clientParts[1] == serverParts[1]) {
                return clientParts[2] >= serverParts[2];
            }
        }

        return false;
    }

    /**
     * Discovers all mod identifiers required by the server that are absent from the client.
     *
     * @param clientMods map of client mod IDs to installed versions
     * @param serverMods map of server mod IDs to required versions
     * @return list of missing mod identifiers
     */
    public List<String> findMissingMods(Map<String, String> clientMods, Map<String, String> serverMods) {
        Objects.requireNonNull(clientMods, "clientMods must not be null");
        Objects.requireNonNull(serverMods, "serverMods must not be null");

        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> serverEntry : serverMods.entrySet()) {
            String modId = serverEntry.getKey();
            if (!clientMods.containsKey(modId)) {
                missing.add(modId + " (Required: " + serverEntry.getValue() + " | Client: Not Installed)");
            }
        }
        return Collections.unmodifiableList(missing);
    }

    /**
     * Discovers all mod version conflicts where a mod is present on both sides but incompatible.
     *
     * @param clientMods           map of client mod IDs to installed versions
     * @param serverMods           map of server mod IDs to required versions
     * @param enforceExactVersion  whether exact semantic versions are required
     * @return map of mod IDs to conflict descriptions
     */
    public Map<String, String> findVersionConflicts(
            Map<String, String> clientMods,
            Map<String, String> serverMods,
            boolean enforceExactVersion
    ) {
        Objects.requireNonNull(clientMods, "clientMods must not be null");
        Objects.requireNonNull(serverMods, "serverMods must not be null");

        Map<String, String> conflicts = new LinkedHashMap<>();
        for (Map.Entry<String, String> serverEntry : serverMods.entrySet()) {
            String modId = serverEntry.getKey();
            if (clientMods.containsKey(modId)) {
                String clientVer = clientMods.get(modId);
                String serverVer = serverEntry.getValue();
                if (!isVersionCompatible(clientVer, serverVer, enforceExactVersion)) {
                    conflicts.put(modId, "Server: " + serverVer + " | Client: " + clientVer + " - Outdated");
                }
            }
        }
        return Collections.unmodifiableMap(conflicts);
    }

    /**
     * Evaluates whether a client's installed mods fully satisfy server requirements.
     *
     * @param clientMods          map of client mods
     * @param serverMods          map of server mods
     * @param enforceExactVersion whether exact version matching is enforced
     * @return {@code true} if all required mods are present and compatible
     */
    public boolean evaluate(
            Map<String, String> clientMods,
            Map<String, String> serverMods,
            boolean enforceExactVersion
    ) {
        List<String> missing = findMissingMods(clientMods, serverMods);
        if (!missing.isEmpty()) {
            return false;
        }
        Map<String, String> conflicts = findVersionConflicts(clientMods, serverMods, enforceExactVersion);
        return conflicts.isEmpty();
    }

    /**
     * Evaluates client mods and produces a {@link MissingModDisconnectionPayload} if mismatch exists.
     *
     * @param clientMods          map of client mods
     * @param serverMods          map of server mods
     * @param enforceExactVersion whether exact version matching is enforced
     * @param customTitle         title for the kick screen
     * @param downloadUrl         URL for downloading the modpack
     * @return constructed payload if mismatch exists, or {@code null} if verified
     */
    public MissingModDisconnectionPayload evaluateAndBuildPayload(
            Map<String, String> clientMods,
            Map<String, String> serverMods,
            boolean enforceExactVersion,
            String customTitle,
            String downloadUrl
    ) {
        List<String> missing = findMissingMods(clientMods, serverMods);
        Map<String, String> conflicts = findVersionConflicts(clientMods, serverMods, enforceExactVersion);

        if (missing.isEmpty() && conflicts.isEmpty()) {
            return null;
        }

        String title = (customTitle != null && !customTitle.isBlank())
                ? customTitle
                : MissingModDisconnectionPayload.DEFAULT_KICK_SCREEN_TITLE;
        String url = (downloadUrl != null && !downloadUrl.isBlank())
                ? downloadUrl
                : MissingModDisconnectionPayload.DEFAULT_DOWNLOAD_URL;

        return MissingModDisconnectionPayload.create(title, missing, conflicts, url);
    }

    private static int[] parseVersionParts(String version) {
        if (version == null) {
            return null;
        }
        String[] tokens = version.split("[.\\-_+]");
        if (tokens.length < 2) {
            return null;
        }
        try {
            int major = Integer.parseInt(tokens[0]);
            int minor = Integer.parseInt(tokens[1]);
            int patch = tokens.length >= 3 ? Integer.parseInt(tokens[2]) : 0;
            return new int[]{major, minor, patch};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int compareSemVer(String ver1, String ver2) {
        int[] p1 = parseVersionParts(ver1);
        int[] p2 = parseVersionParts(ver2);
        if (p1 == null || p2 == null) {
            return ver1.compareTo(ver2);
        }
        for (int i = 0; i < 3; i++) {
            if (p1[i] != p2[i]) {
                return Integer.compare(p1[i], p2[i]);
            }
        }
        return 0;
    }
}
