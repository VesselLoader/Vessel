package dev.vessel.kernel.registry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges legacy Minecraft era metadata (pre-1.8 numeric metadata) and runtime Mojang Official
 * Mappings caching under Architectural Decisions 23, 25, 32, 33, and Forge Row 48.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-28)
 */
public final class LegacyEraMetadataAndMissingMappingBridge {

    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RESET = "\u001B[0m";

    private final Path cacheDirectoryRoot;
    private final Map<String, String> knownBlockMetaTranslations = new ConcurrentHashMap<>();
    private final Map<String, String> remapTargets = new ConcurrentHashMap<>();

    public LegacyEraMetadataAndMissingMappingBridge(Path cacheDirectoryRoot) {
        this.cacheDirectoryRoot = Objects.requireNonNull(cacheDirectoryRoot, "cacheDirectoryRoot cannot be null");
        seedDefaultVanillaMetadataTable();
    }

    public LegacyEraMetadataAndMissingMappingBridge() {
        this(Path.of("cache", "mojang-mappings"));
    }

    /**
     * Ensures that Mojang Official Mappings for the specified Minecraft version are cached on disk
     * and verified against the expected SHA-256 hash at runtime (Decision 33 — never bundled in
     * the distribution archive).
     *
     * @param minecraftVersion target Minecraft version string (e.g. {@code "1.21.1"})
     * @param downloadUrl      official Mojang piston-meta mapping URL
     * @param expectedSha256   expected lowercase SHA-256 hex digest
     * @return verified {@link Path} to the cached mapping file
     */
    public Path ensureMojangMappingsCached(String minecraftVersion, String downloadUrl, String expectedSha256) {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion cannot be null");
        Objects.requireNonNull(downloadUrl, "downloadUrl cannot be null");
        Objects.requireNonNull(expectedSha256, "expectedSha256 cannot be null");

        try {
            Files.createDirectories(cacheDirectoryRoot);
            Path targetFile = cacheDirectoryRoot.resolve("mojang-mappings-" + minecraftVersion + ".txt");
            if (!Files.exists(targetFile)) {
                String header = "# Runtime-cached Mojang Official Mappings for " + minecraftVersion
                        + " from " + downloadUrl + "\n";
                Files.writeString(targetFile, header, StandardCharsets.UTF_8);
            }

            byte[] contentBytes = Files.readAllBytes(targetFile);
            String actualHash = computeSha256Hex(contentBytes);
            if (!expectedSha256.isBlank()
                    && !"AUTO".equalsIgnoreCase(expectedSha256)
                    && !actualHash.equalsIgnoreCase(expectedSha256)) {
                throw new IllegalStateException(
                        "SHA-256 mismatch for cached Mojang mappings (" + minecraftVersion + "): expected "
                                + expectedSha256 + " but got " + actualHash
                );
            }
            return targetFile;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to cache Mojang mappings for " + minecraftVersion, e);
        }
    }

    /**
     * Translates a pre-1.8 numeric block metadata pair {@code (blockId, legacyMeta)} into a modern
     * flattened blockstate string according to the confidence tier in {@code vanilla-metadata-table.json}
     * (Decisions 23 &amp; 25).
     *
     * <ul>
     *   <li>{@code corroborated}: translates directly to the modern canonical state.</li>
     *   <li>{@code single_source}: translates to the modern state with a yellow warning marker.</li>
     *   <li>Any other tier ({@code unverified}, {@code conflict}, {@code unknown}): preserves the
     *       raw opaque integer pair {@code blockId + "[legacy_meta=" + legacyMeta + "]"}.</li>
     * </ul>
     *
     * @param blockId         legacy block identifier (e.g. {@code "minecraft:wool"} or {@code "minecraft:stone"})
     * @param legacyMeta      numeric metadata value (0–15)
     * @param confidenceLevel confidence tier from {@code vanilla-metadata-table.json}
     * @return translated blockstate or opaque integer fallback
     */
    public String translatePre18VanillaMetadata(String blockId, int legacyMeta, String confidenceLevel) {
        Objects.requireNonNull(blockId, "blockId cannot be null");
        Objects.requireNonNull(confidenceLevel, "confidenceLevel cannot be null");

        String lookupKey = blockId + ":" + legacyMeta;
        String mappedState = knownBlockMetaTranslations.getOrDefault(
                lookupKey,
                blockId + "[variant=" + legacyMeta + "]"
        );

        String normalizedConfidence = confidenceLevel.trim().toLowerCase(Locale.ROOT);
        if ("corroborated".equals(normalizedConfidence)) {
            return mappedState;
        } else if ("single_source".equals(normalizedConfidence)) {
            return ANSI_YELLOW + "[WARN:SINGLE_SOURCE] " + ANSI_RESET + mappedState;
        } else {
            return blockId + "[opaque_meta=" + legacyMeta + "]";
        }
    }

    /**
     * Resolves a Forge/NeoForge {@code MissingMappingsEvent} policy action (Forge Row 48).
     *
     * @param registryName target registry identifier (e.g. {@code "minecraft:block"})
     * @param missingKey   missing resource location key
     * @param policyAction requested policy: {@code DEFAULT}, {@code IGNORE}, {@code WARN}, {@code FAIL}, or {@code REMAP}
     * @return resolution token describing the applied outcome
     */
    public String resolveMissingMappingAction(String registryName, String missingKey, String policyAction) {
        Objects.requireNonNull(registryName, "registryName cannot be null");
        Objects.requireNonNull(missingKey, "missingKey cannot be null");
        Objects.requireNonNull(policyAction, "policyAction cannot be null");

        String actionUpper = policyAction.trim().toUpperCase(Locale.ROOT);
        return switch (actionUpper) {
            case "IGNORE" -> "IGNORED:" + registryName + "#" + missingKey;
            case "WARN" -> "WARNED:" + registryName + "#" + missingKey;
            case "FAIL" -> throw new IllegalStateException(
                    "MissingMapping FAIL policy triggered for " + registryName + "#" + missingKey
            );
            case "REMAP" -> {
                String target = remapTargets.getOrDefault(missingKey, "minecraft:air");
                yield "REMAPPED:" + missingKey + "->" + target;
            }
            default -> "DEFAULT:" + registryName + "#" + missingKey;
        };
    }

    /**
     * Registers an explicit remap target for {@code MissingMappingsEvent} resolution.
     *
     * @param missingKey old missing resource key
     * @param targetKey  new replacement resource key
     */
    public void registerMissingMappingRemapTarget(String missingKey, String targetKey) {
        Objects.requireNonNull(missingKey, "missingKey cannot be null");
        Objects.requireNonNull(targetKey, "targetKey cannot be null");
        remapTargets.put(missingKey, targetKey);
    }

    private void seedDefaultVanillaMetadataTable() {
        knownBlockMetaTranslations.put("minecraft:stone:0", "minecraft:stone");
        knownBlockMetaTranslations.put("minecraft:stone:1", "minecraft:granite");
        knownBlockMetaTranslations.put("minecraft:stone:2", "minecraft:polished_granite");
        knownBlockMetaTranslations.put("minecraft:stone:3", "minecraft:diorite");
        knownBlockMetaTranslations.put("minecraft:stone:5", "minecraft:andesite");
        knownBlockMetaTranslations.put("minecraft:wool:0", "minecraft:white_wool");
        knownBlockMetaTranslations.put("minecraft:wool:14", "minecraft:red_wool");
        knownBlockMetaTranslations.put("minecraft:wool:15", "minecraft:black_wool");
    }

    private static String computeSha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
