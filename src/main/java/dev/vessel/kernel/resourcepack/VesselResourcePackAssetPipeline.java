package dev.vessel.kernel.resourcepack;

import dev.vessel.kernel.asset.stream.ItemModelSourceDefinition;
import dev.vessel.kernel.asset.stream.VesselDualEraModelGenerator;
import dev.vessel.kernel.resourcepack.bedrock.VesselBedrockMetadataSynthesizer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * End-to-end asset sanitization and deterministic packaging pipeline for VESSEL.
 * <p>
 * Unifies all asset transformation stages into a single cohesive, high-performance pipeline:
 * <ol>
 *   <li>POSIX lowercasing and directory traversal prevention via {@link VesselAssetPathNormalizer}</li>
 *   <li>Shader safety filtering via {@link VesselCoreShaderSafetyFilter}</li>
 *   <li>Texture atlas seam protection and PoT verification via {@link VesselTextureAtlasSanitizer}</li>
 *   <li>Audio heap explosion prevention via {@link VesselSoundStreamingMetadataGenerator}</li>
 *   <li>Bedrock metadata and localization synthesis via {@link VesselBedrockMetadataSynthesizer}</li>
 *   <li>Dual-era model transpilation via {@link VesselDualEraModelGenerator}</li>
 *   <li>Bit-identical ZIP archiving via {@link VesselDeterministicZipPacker}</li>
 * </ol>
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselResourcePackAssetPipeline {

    private final VesselAssetPathNormalizer pathNormalizer = new VesselAssetPathNormalizer();
    private final VesselCoreShaderSafetyFilter shaderFilter = new VesselCoreShaderSafetyFilter();
    private final VesselTextureAtlasSanitizer textureSanitizer = new VesselTextureAtlasSanitizer();
    private final VesselSoundStreamingMetadataGenerator soundOptimizer = new VesselSoundStreamingMetadataGenerator();
    private final VesselBedrockMetadataSynthesizer bedrockSynthesizer = new VesselBedrockMetadataSynthesizer();
    private final VesselDualEraModelGenerator modelGenerator = new VesselDualEraModelGenerator();
    private final VesselDeterministicZipPacker zipPacker = new VesselDeterministicZipPacker();

    /**
     * Executes the complete asset sanitization pipeline on raw input entries and returns
     * sanitized entries ready for packing.
     *
     * @param rawEntries raw map of relative path to file bytes
     * @param modelDefinitions optional list of item model source definitions (may be empty)
     * @param mcVersion target active Minecraft version (e.g. {@code "1.21.4"}, {@code "26.3"})
     * @return fully sanitized and augmented entry map
     */
    public Map<String, byte[]> sanitizeAll(
            Map<String, byte[]> rawEntries,
            List<ItemModelSourceDefinition> modelDefinitions,
            String mcVersion
    ) {
        Objects.requireNonNull(rawEntries, "rawEntries cannot be null");
        String activeVersion = mcVersion != null ? mcVersion : "1.21.4";

        // Stage 1: Lowercase and POSIX normalize all entry paths
        Map<String, byte[]> normalizedEntries = new LinkedHashMap<>(rawEntries.size() + 16);
        for (Map.Entry<String, byte[]> entry : rawEntries.entrySet()) {
            String normPath = pathNormalizer.normalize(entry.getKey());
            normalizedEntries.putIfAbsent(normPath, entry.getValue());
        }

        // Stage 2: Purge unsafe core shaders (assets/minecraft/shaders/core/**)
        VesselCoreShaderFilterResult shaderResult = shaderFilter.filter(normalizedEntries);
        Map<String, byte[]> safeEntries = new LinkedHashMap<>(shaderResult.safeEntries());

        // Stage 3: Texture Atlas Protection (PoT alignment & 1-pixel border padding)
        for (Map.Entry<String, byte[]> entry : new ArrayList<>(safeEntries.entrySet())) {
            String path = entry.getKey();
            if (path.endsWith(".png") && path.startsWith("assets/") && path.contains("/textures/")) {
                try {
                    VesselSanitizedTextureResult sanitized = textureSanitizer.sanitize(entry.getValue());
                    safeEntries.put(path, sanitized.sanitizedPngData());
                } catch (Exception ignored) {
                    // Non-standard or special PNG preserved as-is
                }
            }
        }

        // Stage 4: Audio Streaming Optimization (OGG > 500KB or > 5s in sounds.json)
        Map<String, byte[]> soundFiles = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : safeEntries.entrySet()) {
            if (entry.getKey().endsWith(".ogg")) {
                soundFiles.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, byte[]> entry : new ArrayList<>(safeEntries.entrySet())) {
            String path = entry.getKey();
            if (path.endsWith("sounds.json")) {
                try {
                    String rawJson = new String(entry.getValue(), StandardCharsets.UTF_8);
                    String updated = soundOptimizer.updateSoundsJson(rawJson, soundFiles);
                    safeEntries.put(path, updated.getBytes(StandardCharsets.UTF_8));
                } catch (Exception ignored) {
                }
            }
        }

        // Stage 5: Dual-era item model generation
        if (modelDefinitions != null && !modelDefinitions.isEmpty()) {
            Map<String, String> generatedModels = modelGenerator.generateForVersion(modelDefinitions, activeVersion);
            for (Map.Entry<String, String> modelEntry : generatedModels.entrySet()) {
                String normPath = pathNormalizer.normalize(modelEntry.getKey());
                safeEntries.put(normPath, modelEntry.getValue().getBytes(StandardCharsets.UTF_8));
            }
        }

        // Stage 6: Bedrock metadata synthesis (item_texture.json, terrain_texture.json, texts/*.lang, clean manifest)
        Map<String, byte[]> bedrockMetadata = bedrockSynthesizer.synthesizeBedrockMetadata(safeEntries);
        for (Map.Entry<String, byte[]> bm : bedrockMetadata.entrySet()) {
            safeEntries.putIfAbsent(bm.getKey(), bm.getValue());
        }

        return Collections.unmodifiableMap(safeEntries);
    }

    /**
     * Executes the sanitization pipeline and packs the result into a byte array with deterministic SHA-1.
     *
     * @param rawEntries raw map of relative path to file bytes
     * @param modelDefinitions optional model definitions
     * @param mcVersion target Minecraft version
     * @return bit-identical packed ZIP byte array
     */
    public byte[] sanitizeAndPack(
            Map<String, byte[]> rawEntries,
            List<ItemModelSourceDefinition> modelDefinitions,
            String mcVersion
    ) {
        Map<String, byte[]> sanitized = sanitizeAll(rawEntries, modelDefinitions, mcVersion);
        return zipPacker.pack(sanitized);
    }

    /**
     * Executes the sanitization pipeline and packs directly into a target output stream.
     *
     * @param rawEntries raw map of relative path to file bytes
     * @param modelDefinitions optional model definitions
     * @param mcVersion target Minecraft version
     * @param outputStream target destination stream
     * @throws IOException on I/O error
     */
    public void sanitizeAndPackToStream(
            Map<String, byte[]> rawEntries,
            List<ItemModelSourceDefinition> modelDefinitions,
            String mcVersion,
            OutputStream outputStream
    ) throws IOException {
        Map<String, byte[]> sanitized = sanitizeAll(rawEntries, modelDefinitions, mcVersion);
        List<VesselDeterministicZipEntry> entries = new ArrayList<>(sanitized.size());
        for (Map.Entry<String, byte[]> e : sanitized.entrySet()) {
            entries.add(new VesselDeterministicZipEntry(e.getKey(), e.getValue()));
        }
        zipPacker.packToStream(entries, outputStream);
    }

    /**
     * Returns the underlying deterministic ZIP packer.
     *
     * @return zip packer instance
     */
    public VesselDeterministicZipPacker getZipPacker() {
        return zipPacker;
    }
}
