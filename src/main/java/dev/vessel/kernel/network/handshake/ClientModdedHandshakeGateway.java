package dev.vessel.kernel.network.handshake;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway coordinator governing multi-ecosystem login handshakes across FML (Forge), NeoForge,
 * and Fabric/Quilt network protocols.
 *
 * <p>In {@link ClientPresentationMode#SOVEREIGN_VANILLA}, permits zero-client-mod connections.
 * In {@link ClientPresentationMode#STRICT_CLIENT_MODDED}, enforces mod manifest verification
 * and authorizes direct packet pass-through upon successful audit.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ClientModdedHandshakeGateway {

    /**
     * Canonical channel identifier for FML handshake protocol.
     */
    public static final String FML_HANDSHAKE_CHANNEL = "fml:handshake";

    /**
     * Canonical channel identifier for FML login wrapper.
     */
    public static final String FML_LOGIN_WRAPPER_CHANNEL = "fml:loginwrapper";

    /**
     * Canonical channel identifier for NeoForge handshake protocol.
     */
    public static final String NEOFORGE_HANDSHAKE_CHANNEL = "neoforge:handshake";

    /**
     * Canonical channel identifier for Fabric networking API v1.
     */
    public static final String FABRIC_NETWORKING_CHANNEL = "fabric-networking-api-v1";

    /**
     * Canonical channel identifier for Fabric registry sync.
     */
    public static final String FABRIC_REGISTRY_SYNC_CHANNEL = "fabric:registry/sync";

    /**
     * Universal VESSEL mod audit channel for direct hash and manifest queries.
     */
    public static final String VESSEL_MOD_AUDIT_CHANNEL = "vessel:mod_audit";

    /**
     * Handshake state: initiated connection.
     */
    public static final String STATE_INITIATED = "INITIATED";

    /**
     * Handshake state: mod manifest query dispatched to client.
     */
    public static final String STATE_AUDIT_PENDING = "AUDIT_PENDING";

    /**
     * Handshake state: client verified and authorized for play.
     */
    public static final String STATE_VERIFIED = "VERIFIED";

    /**
     * Handshake state: connection rejected due to manifest discrepancy.
     */
    public static final String STATE_DISCONNECTED = "DISCONNECTED";

    private static final Set<String> SUPPORTED_HANDSHAKE_CHANNELS = Set.of(
            FML_HANDSHAKE_CHANNEL,
            FML_LOGIN_WRAPPER_CHANNEL,
            NEOFORGE_HANDSHAKE_CHANNEL,
            FABRIC_NETWORKING_CHANNEL,
            FABRIC_REGISTRY_SYNC_CHANNEL,
            VESSEL_MOD_AUDIT_CHANNEL
    );

    private volatile ClientPresentationMode presentationMode;
    private final ModManifestAuditEvaluator auditEvaluator;
    private final DirectModdedPacketRouter packetRouter;

    private final ConcurrentHashMap<String, String> serverActiveMods = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> connectionStates = new ConcurrentHashMap<>();

    private volatile boolean enforceExactVersion = false;
    private volatile String kickScreenTitle = MissingModDisconnectionPayload.DEFAULT_KICK_SCREEN_TITLE;
    private volatile String downloadUrl = MissingModDisconnectionPayload.DEFAULT_DOWNLOAD_URL;

    /**
     * Constructs a new ClientModdedHandshakeGateway.
     *
     * @param presentationMode initial client presentation mode
     * @param auditEvaluator   audit evaluator engine
     * @param packetRouter     direct packet router
     */
    public ClientModdedHandshakeGateway(
            ClientPresentationMode presentationMode,
            ModManifestAuditEvaluator auditEvaluator,
            DirectModdedPacketRouter packetRouter
    ) {
        this.presentationMode = Objects.requireNonNull(presentationMode, "presentationMode must not be null");
        this.auditEvaluator = Objects.requireNonNull(auditEvaluator, "auditEvaluator must not be null");
        this.packetRouter = Objects.requireNonNull(packetRouter, "packetRouter must not be null");
    }

    /**
     * Determines whether the specified channel is a recognized modded handshake channel.
     *
     * @param channel the channel identifier to test
     * @return {@code true} if supported handshake channel
     */
    public boolean isModdedHandshakeChannel(String channel) {
        if (channel == null) {
            return false;
        }
        return SUPPORTED_HANDSHAKE_CHANNELS.contains(channel)
                || channel.startsWith("fml:")
                || channel.startsWith("neoforge:")
                || channel.startsWith("fabric:");
    }

    /**
     * Initiates the handshake lifecycle for a connecting client.
     *
     * @param connectionId unique connection identifier
     * @param channel      inbound handshake channel
     * @return the outbound query channel identifier, or {@code null} if unhandled
     */
    public String initiateHandshake(String connectionId, String channel) {
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        Objects.requireNonNull(channel, "channel must not be null");

        if (!isModdedHandshakeChannel(channel)) {
            return null;
        }

        packetRouter.registerConnection(connectionId, presentationMode);

        if (presentationMode == ClientPresentationMode.SOVEREIGN_VANILLA) {
            connectionStates.put(connectionId, STATE_VERIFIED);
            return channel;
        }

        connectionStates.put(connectionId, STATE_AUDIT_PENDING);
        return VESSEL_MOD_AUDIT_CHANNEL;
    }

    /**
     * Processes client-submitted mod manifest response and returns disconnection payload upon mismatch.
     *
     * @param connectionId client connection identifier
     * @param clientMods   map of client mod identifiers to installed versions
     * @return {@link MissingModDisconnectionPayload} if mismatch occurs, or {@code null} if verified
     */
    public MissingModDisconnectionPayload processModAuditResponse(
            String connectionId,
            Map<String, String> clientMods
    ) {
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        Objects.requireNonNull(clientMods, "clientMods must not be null");

        if (presentationMode == ClientPresentationMode.SOVEREIGN_VANILLA) {
            connectionStates.put(connectionId, STATE_VERIFIED);
            return null;
        }

        MissingModDisconnectionPayload payload = auditEvaluator.evaluateAndBuildPayload(
                clientMods,
                serverActiveMods,
                enforceExactVersion,
                kickScreenTitle,
                downloadUrl
        );

        if (payload != null && payload.hasViolations()) {
            connectionStates.put(connectionId, STATE_DISCONNECTED);
            packetRouter.unregisterConnection(connectionId);
            return payload;
        }

        connectionStates.put(connectionId, STATE_VERIFIED);
        packetRouter.markConnectionVerified(connectionId);
        return null;
    }

    /**
     * Processes fast-path SHA-256 registry hash audit from client.
     *
     * @param connectionId unique connection identifier
     * @param clientHash   SHA-256 hex string computed by client
     * @return {@code true} if hash matched and connection verified immediately
     */
    public boolean processModAuditHashResponse(String connectionId, String clientHash) {
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        if (clientHash == null || clientHash.isBlank()) {
            return false;
        }

        if (presentationMode == ClientPresentationMode.SOVEREIGN_VANILLA) {
            connectionStates.put(connectionId, STATE_VERIFIED);
            return true;
        }

        boolean matched = auditEvaluator.verifyRegistryHash(clientHash, serverActiveMods);
        if (matched) {
            connectionStates.put(connectionId, STATE_VERIFIED);
            packetRouter.markConnectionVerified(connectionId);
            return true;
        }

        return false;
    }

    /**
     * Checks if the connection session has verified status and is cleared for PLAY state.
     *
     * @param connectionId unique connection identifier
     * @return {@code true} if cleared for gameplay
     */
    public boolean isConnectionReadyForPlay(String connectionId) {
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        return STATE_VERIFIED.equals(connectionStates.get(connectionId));
    }

    /**
     * Terminates and cleans up state for a disconnecting client.
     *
     * @param connectionId connection identifier
     */
    public void terminateConnection(String connectionId) {
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        connectionStates.remove(connectionId);
        packetRouter.unregisterConnection(connectionId);
    }

    /**
     * Adds an active server mod requirement.
     *
     * @param modId              mod identifier
     * @param versionRequirement version or semver requirement
     */
    public void addServerActiveMod(String modId, String versionRequirement) {
        Objects.requireNonNull(modId, "modId must not be null");
        Objects.requireNonNull(versionRequirement, "versionRequirement must not be null");
        serverActiveMods.put(modId, versionRequirement);
    }

    /**
     * Sets the full map of active server mods.
     *
     * @param activeMods map of mod IDs to required versions
     */
    public void setServerActiveMods(Map<String, String> activeMods) {
        Objects.requireNonNull(activeMods, "activeMods must not be null");
        serverActiveMods.clear();
        serverActiveMods.putAll(activeMods);
    }

    /**
     * Returns an unmodifiable view of active server mods.
     *
     * @return active server mods map
     */
    public Map<String, String> getServerActiveMods() {
        return Collections.unmodifiableMap(serverActiveMods);
    }

    /**
     * Returns the current client presentation mode.
     *
     * @return presentation mode
     */
    public ClientPresentationMode getPresentationMode() {
        return presentationMode;
    }

    /**
     * Updates the client presentation mode.
     *
     * @param presentationMode new presentation mode
     */
    public void setPresentationMode(ClientPresentationMode presentationMode) {
        this.presentationMode = Objects.requireNonNull(presentationMode, "presentationMode must not be null");
    }

    /**
     * Returns whether exact version matching is enforced.
     *
     * @return {@code true} if exact version enforced
     */
    public boolean isEnforceExactVersion() {
        return enforceExactVersion;
    }

    /**
     * Configures exact semantic version enforcement.
     *
     * @param enforceExactVersion {@code true} to require exact version match
     */
    public void setEnforceExactVersion(boolean enforceExactVersion) {
        this.enforceExactVersion = enforceExactVersion;
    }

    /**
     * Returns the current kick screen title.
     *
     * @return kick screen title
     */
    public String getKickScreenTitle() {
        return kickScreenTitle;
    }

    /**
     * Sets a custom kick screen title.
     *
     * @param kickScreenTitle title string
     */
    public void setKickScreenTitle(String kickScreenTitle) {
        this.kickScreenTitle = Objects.requireNonNull(kickScreenTitle, "kickScreenTitle must not be null");
    }

    /**
     * Returns the modpack download URL.
     *
     * @return download URL
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Sets the modpack download URL.
     *
     * @param downloadUrl download URL string
     */
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = Objects.requireNonNull(downloadUrl, "downloadUrl must not be null");
    }

    /**
     * Returns the number of connections currently pending audit evaluation.
     *
     * @return pending audit count
     */
    public int getActivePendingCount() {
        int count = 0;
        for (String state : connectionStates.values()) {
            if (STATE_AUDIT_PENDING.equals(state)) {
                count++;
            }
        }
        return count;
    }
}
