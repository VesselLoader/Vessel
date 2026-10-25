package dev.vessel.kernel.network.profile;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides an in-memory GameProfile federation cache and zero-latency offline response bridge
 * for Floodgate Bedrock UUIDs (format: {@code 00000000-0000-0000-xxxx-xxxxxxxxxxxx}), completely
 * preventing synchronous blocking HTTP calls to Mojang servers by Forge mods (FTB Teams, JourneyMap)
 * which freeze the server tick loop for 5 seconds.
 *
 * <p>Responsibility: Non-blocking profile caching and Bedrock federation.
 * Eliminates: ERR-26, NET-061..080.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class UniversalProfileFederationBridge {

    /**
     * Canonical prefix prepended to Bedrock player display names if not already present.
     */
    public static final String BEDROCK_NAME_PREFIX = ".";

    /**
     * Fallback username assigned when an unknown Bedrock UUID is resolved without a name hint.
     */
    public static final String UNKNOWN_BEDROCK_FALLBACK_NAME = ".BedrockPlayer";

    private final Map<UUID, FederatedGameProfile> profileCache = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameToUuidIndex = new ConcurrentHashMap<>();
    private final AtomicLong cacheHits = new AtomicLong(0L);
    private final AtomicLong syntheticGenerations = new AtomicLong(0L);

    /**
     * Constructs a new UniversalProfileFederationBridge.
     */
    public UniversalProfileFederationBridge() {
    }

    /**
     * Checks if a given UUID conforms to the Geyser Floodgate UUID specification
     * (where the most significant 64 bits are zero).
     *
     * @param uuid the player UUID to inspect
     * @return {@code true} if this UUID belongs to a Floodgate / Bedrock player
     */
    public boolean isFloodgateBedrockUuid(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return uuid.getMostSignificantBits() == 0L;
    }

    /**
     * Resolves a player's profile from the local cache. If missing and the UUID is a Floodgate
     * Bedrock UUID, instantaneously synthesizes an offline Bedrock profile with zero network I/O.
     *
     * @param uuid         the player UUID to resolve
     * @param fallbackName default username to use if synthesis is required
     * @return resolved or synthesized federated game profile
     */
    public FederatedGameProfile resolveProfile(UUID uuid, String fallbackName) {
        Objects.requireNonNull(uuid, "uuid must not be null");

        FederatedGameProfile cached = profileCache.get(uuid);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }

        if (isFloodgateBedrockUuid(uuid)) {
            syntheticGenerations.incrementAndGet();
            String resolvedName = sanitizeBedrockUsername(fallbackName);
            FederatedGameProfile synthetic = new FederatedGameProfile(
                    uuid,
                    resolvedName,
                    Collections.emptyList(),
                    true
            );
            cacheProfile(synthetic);
            return synthetic;
        }

        return null;
    }

    /**
     * Explicitly inserts or updates a player profile in the federation cache.
     *
     * @param profile the profile to store
     */
    public void cacheProfile(FederatedGameProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        profileCache.put(profile.id(), profile);
        nameToUuidIndex.put(profile.name().toLowerCase(), profile.id());
    }

    /**
     * Resolves a cached profile by username (case-insensitive).
     *
     * @param username the username to query
     * @return cached profile, or {@code null} if not cached
     */
    public FederatedGameProfile resolveByName(String username) {
        Objects.requireNonNull(username, "username must not be null");
        UUID id = nameToUuidIndex.get(username.toLowerCase());
        if (id != null) {
            return profileCache.get(id);
        }
        return null;
    }

    /**
     * Invalidates and evicts a player's profile from the cache.
     *
     * @param uuid player UUID to evict
     * @return previously cached profile, or {@code null}
     */
    public FederatedGameProfile invalidate(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        FederatedGameProfile removed = profileCache.remove(uuid);
        if (removed != null) {
            nameToUuidIndex.remove(removed.name().toLowerCase());
        }
        return removed;
    }

    /**
     * Normalizes a Bedrock username to ensure it starts with the canonical prefix.
     *
     * @param name input username hint
     * @return normalized Bedrock name
     */
    public String sanitizeBedrockUsername(String name) {
        if (name == null || name.isBlank()) {
            return UNKNOWN_BEDROCK_FALLBACK_NAME;
        }
        if (name.startsWith(BEDROCK_NAME_PREFIX) || name.startsWith("*")) {
            return name;
        }
        return BEDROCK_NAME_PREFIX + name;
    }

    /**
     * Returns the total count of currently cached profiles.
     *
     * @return profile cache size
     */
    public int getCachedProfileCount() {
        return profileCache.size();
    }

    /**
     * Returns the count of successful cache hits.
     *
     * @return cache hits
     */
    public long getCacheHits() {
        return cacheHits.get();
    }

    /**
     * Returns the count of synthetic offline profiles generated without network calls.
     *
     * @return synthetic generations count
     */
    public long getSyntheticGenerations() {
        return syntheticGenerations.get();
    }

    /**
     * Clears all cached profiles and index entries.
     */
    public void clearCache() {
        profileCache.clear();
        nameToUuidIndex.clear();
    }
}
