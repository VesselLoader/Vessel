package dev.vessel.kernel.resourcepack.bedrock;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Bedrock edition metadata and addon synthesizer.
 * <p>
 * Automatically analyzes aggregated Java Edition asset entries and generates canonical Bedrock Edition
 * metadata structures:
 * <ul>
 *   <li>{@code textures/item_texture.json} and {@code textures/terrain_texture.json} with isolated namespace shortnames</li>
 *   <li>{@code texts/*.lang} localization dictionaries with normalized {@code tile.} and {@code item.} keys</li>
 *   <li>Sanitized {@code manifest.json} stripped of any phantom behavior pack dependencies</li>
 * </ul>
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class VesselBedrockMetadataSynthesizer {

    private final VesselBedrockTextureDictionaryCompiler textureCompiler = new VesselBedrockTextureDictionaryCompiler();
    private final VesselBedrockLanguageFileGenerator langGenerator = new VesselBedrockLanguageFileGenerator();
    private final BedrockManifestGenerator manifestGenerator = new BedrockManifestGenerator();
    private final BedrockManifestDependencyCleaner dependencyCleaner = new BedrockManifestDependencyCleaner();
    private final JavaToBedrockTextureConverter textureConverter = new JavaToBedrockTextureConverter();

    /**
     * Synthesizes Bedrock metadata files from an aggregated map of Java Edition resource pack assets.
     *
     * @param javaAssets map of relative normalized paths to raw asset byte arrays
     * @return map of synthesized Bedrock file paths to byte arrays
     */
    public Map<String, byte[]> synthesizeBedrockMetadata(Map<String, byte[]> javaAssets) {
        Objects.requireNonNull(javaAssets, "javaAssets cannot be null");
        Map<String, byte[]> bedrockFiles = new LinkedHashMap<>();

        // 1. Scan textures, convert paths to Bedrock layout, and populate Bedrock texture dictionary
        textureCompiler.clear();
        for (Map.Entry<String, byte[]> assetEntry : javaAssets.entrySet()) {
            String path = assetEntry.getKey();
            byte[] bytes = assetEntry.getValue();
            if (!path.endsWith(".png")) {
                continue;
            }

            if (path.startsWith("assets/")) {
                try {
                    String convertedPath = textureConverter.convertPath(path);
                    bedrockFiles.putIfAbsent(convertedPath, bytes);
                } catch (Exception ignored) {
                }
                String[] parts = path.split("/");
                if (parts.length >= 5 && "textures".equals(parts[2])) {
                    String namespace = parts[1];
                    String category = parts[3];
                    String filename = parts[parts.length - 1];
                    String id = filename.substring(0, filename.length() - 4); // strip .png

                    if ("item".equals(category) || "items".equals(category)) {
                        String bedrockPath = "textures/" + namespace + "/items/" + id;
                        textureCompiler.registerItem(namespace, id, bedrockPath);
                    } else if ("block".equals(category) || "blocks".equals(category)) {
                        String bedrockPath = "textures/" + namespace + "/blocks/" + id;
                        textureCompiler.registerTerrain(namespace, id, bedrockPath);
                    }
                }
            }
        }

        // Generate textures/item_texture.json and textures/terrain_texture.json
        if (textureCompiler.itemCount() > 0) {
            String itemJson = textureCompiler.compileItemTextureJson();
            bedrockFiles.put("textures/item_texture.json", itemJson.getBytes(StandardCharsets.UTF_8));
        }
        if (textureCompiler.terrainCount() > 0) {
            String terrainJson = textureCompiler.compileTerrainTextureJson();
            bedrockFiles.put("textures/terrain_texture.json", terrainJson.getBytes(StandardCharsets.UTF_8));
        }

        // 2. Scan language files: assets/<namespace>/lang/*.json -> texts/<locale>.lang
        Map<String, Map<String, String>> localeTranslations = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : javaAssets.entrySet()) {
            String path = entry.getKey();
            if (path.startsWith("assets/") && path.contains("/lang/") && path.endsWith(".json")) {
                String[] parts = path.split("/");
                if (parts.length >= 4) {
                    String namespace = parts[1];
                    String filename = parts[parts.length - 1];
                    String bedrockPath = VesselBedrockLocaleMapper.toBedrockFilePath(filename);

                    try {
                        String json = new String(entry.getValue(), StandardCharsets.UTF_8);
                        var parsed = langGenerator.parseTranslations(namespace, json);
                        Map<String, String> targetMap = localeTranslations.computeIfAbsent(bedrockPath, k -> new LinkedHashMap<>());
                        for (VesselBedrockLanguageEntry le : parsed.values()) {
                            targetMap.put(le.key(), le.value());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        for (Map.Entry<String, Map<String, String>> locEntry : localeTranslations.entrySet()) {
            String langFilePath = locEntry.getKey();
            String langContent = langGenerator.generateLangFromMap("vessel", locEntry.getValue());
            bedrockFiles.put(langFilePath, langContent.getBytes(StandardCharsets.UTF_8));
        }

        // 3. Manifest processing: if manifest.json exists, clean dependencies; otherwise generate canonical manifest
        byte[] existingManifestBytes = javaAssets.get("manifest.json");
        if (existingManifestBytes != null) {
            String existingManifest = new String(existingManifestBytes, StandardCharsets.UTF_8);
            String cleaned = dependencyCleaner.cleanDependencies(existingManifest);
            bedrockFiles.put("manifest.json", cleaned.getBytes(StandardCharsets.UTF_8));
        } else {
            String generated = manifestGenerator.generateManifest(
                    "vessel-bedrock-addon",
                    "VESSEL Sovereign Universal Bedrock Addon",
                    "Unified Resource and Addon Pack for Bedrock Clients"
            );
            bedrockFiles.put("manifest.json", generated.getBytes(StandardCharsets.UTF_8));
        }

        return bedrockFiles;
    }
}
