package dev.vessel.kernel.resourcepack.bedrock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * High-performance compiler for Bedrock Edition texture atlas dictionaries
 * ({@code textures/item_texture.json} and {@code textures/terrain_texture.json}).
 * <p>
 * Implements strict namespace isolation for texture shortnames, preventing silent name collisions
 * and texture overwriting between multiple mods sharing identical asset identifiers (e.g. {@code techmod:wrench}
 * vs {@code magicmod:wrench}). Automatically creates safe namespaced aliases (colon-delimited, underscore-delimited,
 * and dot-delimited) while preserving vanilla Mojang shortname conventions for {@code minecraft} namespace assets.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class VesselBedrockTextureDictionaryCompiler {

    /** Default Bedrock resource pack name identifier declared in atlas headers. */
    public static final String DEFAULT_PACK_NAME = "vessel";

    /** Default mipmap levels for terrain block textures in modern Bedrock clients. */
    public static final int DEFAULT_NUM_MIP_LEVELS = 4;

    /** Default pixel padding for terrain block atlas tiles. */
    public static final int DEFAULT_TERRAIN_PADDING = 8;

    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson GSON_COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    private final NavigableMap<String, VesselBedrockTextureEntry> itemTextures = new ConcurrentSkipListMap<>();
    private final NavigableMap<String, VesselBedrockTextureEntry> terrainTextures = new ConcurrentSkipListMap<>();

    /**
     * Constructs a new empty Bedrock texture dictionary compiler.
     */
    public VesselBedrockTextureDictionaryCompiler() {
    }

    /**
     * Registers an item texture under the specified namespace and identifier, establishing
     * isolated namespace keys to eliminate mod texture collisions.
     *
     * @param namespace owning mod or system namespace (e.g. "techmod", "minecraft")
     * @param id unqualified item identifier (e.g. "wrench", "copper_ingot")
     * @param bedrockPath relative Bedrock texture path (e.g. "textures/techmod/items/wrench")
     */
    public void registerItem(String namespace, String id, String bedrockPath) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(bedrockPath, "bedrockPath cannot be null");

        String cleanedNamespace = namespace.trim().toLowerCase(java.util.Locale.ROOT);
        String cleanedId = id.trim().toLowerCase(java.util.Locale.ROOT);

        // Strip redundant namespace prefixes if present in identifier
        if (cleanedId.startsWith(cleanedNamespace + ":")) {
            cleanedId = cleanedId.substring(cleanedNamespace.length() + 1);
        } else if (cleanedId.startsWith(cleanedNamespace + "_")) {
            cleanedId = cleanedId.substring(cleanedNamespace.length() + 1);
        } else if (cleanedId.contains(":")) {
            int colon = cleanedId.indexOf(':');
            cleanedNamespace = cleanedId.substring(0, colon);
            cleanedId = cleanedId.substring(colon + 1);
        }

        VesselBedrockTextureEntry canonicalEntry = VesselBedrockTextureEntry.of(
                cleanedNamespace,
                cleanedId,
                bedrockPath,
                VesselBedrockTextureType.ITEM
        );

        if (cleanedNamespace.equals("minecraft")) {
            itemTextures.put(cleanedId, canonicalEntry);
            itemTextures.put("minecraft:" + cleanedId, canonicalEntry);
            itemTextures.put("minecraft_" + cleanedId, canonicalEntry);
        } else {
            // Isolate namespace under colon, underscore, and dot keys
            itemTextures.put(cleanedNamespace + ":" + cleanedId, canonicalEntry);
            itemTextures.put(cleanedNamespace + "_" + cleanedId, canonicalEntry);
            itemTextures.put(cleanedNamespace + "." + cleanedId, canonicalEntry);
        }
    }

    /**
     * Registers an item texture by qualified identifier string.
     * <p>
     * If {@code identifier} contains a colon {@code :}, it is split into namespace and id.
     * Otherwise, it is registered under the {@code minecraft} namespace.
     * </p>
     *
     * @param identifier qualified or unqualified identifier string
     * @param bedrockPath relative Bedrock texture path
     */
    public void registerItem(String identifier, String bedrockPath) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(bedrockPath, "bedrockPath cannot be null");

        int colonIdx = identifier.indexOf(':');
        if (colonIdx >= 0) {
            String ns = identifier.substring(0, colonIdx).trim();
            String raw = identifier.substring(colonIdx + 1).trim();
            if (ns.isEmpty()) {
                ns = "minecraft";
            }
            registerItem(ns, raw, bedrockPath);
        } else {
            registerItem("minecraft", identifier.trim(), bedrockPath);
        }
    }

    /**
     * Registers a terrain (block) texture under the specified namespace and identifier, establishing
     * isolated namespace keys to eliminate block face collisions.
     *
     * @param namespace owning mod or system namespace (e.g. "techmod", "minecraft")
     * @param id unqualified block identifier (e.g. "crusher", "copper_block")
     * @param bedrockPath relative Bedrock texture path (e.g. "textures/techmod/blocks/crusher")
     */
    public void registerTerrain(String namespace, String id, String bedrockPath) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(bedrockPath, "bedrockPath cannot be null");

        String cleanedNamespace = namespace.trim().toLowerCase(java.util.Locale.ROOT);
        String cleanedId = id.trim().toLowerCase(java.util.Locale.ROOT);

        // Strip redundant namespace prefixes if present in identifier
        if (cleanedId.startsWith(cleanedNamespace + ":")) {
            cleanedId = cleanedId.substring(cleanedNamespace.length() + 1);
        } else if (cleanedId.startsWith(cleanedNamespace + "_")) {
            cleanedId = cleanedId.substring(cleanedNamespace.length() + 1);
        } else if (cleanedId.contains(":")) {
            int colon = cleanedId.indexOf(':');
            cleanedNamespace = cleanedId.substring(0, colon);
            cleanedId = cleanedId.substring(colon + 1);
        }

        VesselBedrockTextureEntry canonicalEntry = VesselBedrockTextureEntry.of(
                cleanedNamespace,
                cleanedId,
                bedrockPath,
                VesselBedrockTextureType.TERRAIN
        );

        if (cleanedNamespace.equals("minecraft")) {
            terrainTextures.put(cleanedId, canonicalEntry);
            terrainTextures.put("minecraft:" + cleanedId, canonicalEntry);
            terrainTextures.put("minecraft_" + cleanedId, canonicalEntry);
        } else {
            // Isolate namespace under colon, underscore, and dot keys
            terrainTextures.put(cleanedNamespace + ":" + cleanedId, canonicalEntry);
            terrainTextures.put(cleanedNamespace + "_" + cleanedId, canonicalEntry);
            terrainTextures.put(cleanedNamespace + "." + cleanedId, canonicalEntry);
        }
    }

    /**
     * Registers a terrain (block) texture by qualified identifier string.
     * <p>
     * If {@code identifier} contains a colon {@code :}, it is split into namespace and id.
     * Otherwise, it is registered under the {@code minecraft} namespace.
     * </p>
     *
     * @param identifier qualified or unqualified identifier string
     * @param bedrockPath relative Bedrock texture path
     */
    public void registerTerrain(String identifier, String bedrockPath) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(bedrockPath, "bedrockPath cannot be null");

        int colonIdx = identifier.indexOf(':');
        if (colonIdx >= 0) {
            String ns = identifier.substring(0, colonIdx).trim();
            String raw = identifier.substring(colonIdx + 1).trim();
            if (ns.isEmpty()) {
                ns = "minecraft";
            }
            registerTerrain(ns, raw, bedrockPath);
        } else {
            registerTerrain("minecraft", identifier.trim(), bedrockPath);
        }
    }

    /**
     * Registers a pre-built texture entry into the appropriate dictionary map.
     *
     * @param entry non-null texture entry
     */
    public void registerEntry(VesselBedrockTextureEntry entry) {
        Objects.requireNonNull(entry, "entry cannot be null");
        if (entry.type() == VesselBedrockTextureType.ITEM) {
            registerItem(entry.namespace(), entry.rawName(), entry.bedrockPath());
        } else {
            registerTerrain(entry.namespace(), entry.rawName(), entry.bedrockPath());
        }
    }

    /**
     * Compiles the {@code textures/item_texture.json} payload with default pack name.
     *
     * @return pretty-printed JSON string
     */
    public String compileItemTextureJson() {
        return compileItemTextureJson(DEFAULT_PACK_NAME, true);
    }

    /**
     * Compiles the {@code textures/item_texture.json} payload with specified resource pack name.
     *
     * @param resourcePackName resource pack name identifier
     * @return pretty-printed JSON string
     */
    public String compileItemTextureJson(String resourcePackName) {
        return compileItemTextureJson(resourcePackName, true);
    }

    /**
     * Compiles the {@code textures/item_texture.json} payload with specified resource pack name and formatting.
     *
     * @param resourcePackName resource pack name identifier
     * @param prettyPrint whether to pretty-print or emit high-speed compact JSON
     * @return JSON string
     */
    public String compileItemTextureJson(String resourcePackName, boolean prettyPrint) {
        Objects.requireNonNull(resourcePackName, "resourcePackName cannot be null");

        JsonObject root = new JsonObject();
        root.addProperty("resource_pack_name", resourcePackName);
        root.addProperty("texture_name", VesselBedrockTextureType.ITEM.atlasName());

        JsonObject textureData = new JsonObject();
        for (Map.Entry<String, VesselBedrockTextureEntry> entry : itemTextures.entrySet()) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("textures", entry.getValue().bedrockPath());
            textureData.add(entry.getKey(), itemObj);
        }
        root.add("texture_data", textureData);

        Gson serializer = prettyPrint ? GSON_PRETTY : GSON_COMPACT;
        return serializer.toJson(root);
    }

    /**
     * Compiles the {@code textures/terrain_texture.json} payload with default configuration.
     *
     * @return pretty-printed JSON string
     */
    public String compileTerrainTextureJson() {
        return compileTerrainTextureJson(DEFAULT_PACK_NAME, DEFAULT_NUM_MIP_LEVELS, DEFAULT_TERRAIN_PADDING, true);
    }

    /**
     * Compiles the {@code textures/terrain_texture.json} payload with specified parameters.
     *
     * @param resourcePackName resource pack name identifier
     * @param numMipLevels number of mipmap levels (typically 1 to 4)
     * @param padding pixel padding around tiles (typically 4 or 8)
     * @return pretty-printed JSON string
     */
    public String compileTerrainTextureJson(String resourcePackName, int numMipLevels, int padding) {
        return compileTerrainTextureJson(resourcePackName, numMipLevels, padding, true);
    }

    /**
     * Compiles the {@code textures/terrain_texture.json} payload with specified parameters and formatting.
     *
     * @param resourcePackName resource pack name identifier
     * @param numMipLevels number of mipmap levels (typically 1 to 4)
     * @param padding pixel padding around tiles (typically 4 or 8)
     * @param prettyPrint whether to pretty-print or emit high-speed compact JSON
     * @return JSON string
     */
    public String compileTerrainTextureJson(String resourcePackName, int numMipLevels, int padding, boolean prettyPrint) {
        Objects.requireNonNull(resourcePackName, "resourcePackName cannot be null");
        if (numMipLevels < 0) {
            throw new IllegalArgumentException("numMipLevels must be non-negative: " + numMipLevels);
        }
        if (padding < 0) {
            throw new IllegalArgumentException("padding must be non-negative: " + padding);
        }

        JsonObject root = new JsonObject();
        root.addProperty("resource_pack_name", resourcePackName);
        root.addProperty("texture_name", VesselBedrockTextureType.TERRAIN.atlasName());
        root.addProperty("padding", padding);
        root.addProperty("num_mip_levels", numMipLevels);

        JsonObject textureData = new JsonObject();
        for (Map.Entry<String, VesselBedrockTextureEntry> entry : terrainTextures.entrySet()) {
            JsonObject terrainObj = new JsonObject();
            terrainObj.addProperty("textures", entry.getValue().bedrockPath());
            textureData.add(entry.getKey(), terrainObj);
        }
        root.add("texture_data", textureData);

        Gson serializer = prettyPrint ? GSON_PRETTY : GSON_COMPACT;
        return serializer.toJson(root);
    }

    /**
     * Compiles both item and terrain texture dictionaries into a map keyed by relative pack paths.
     *
     * @return map of pack path to JSON string
     */
    public Map<String, String> compileAll() {
        return compileAll(DEFAULT_PACK_NAME, true);
    }

    /**
     * Compiles both item and terrain texture dictionaries into a map keyed by relative pack paths with custom pack name.
     *
     * @param resourcePackName resource pack name identifier
     * @return map of pack path to JSON string
     */
    public Map<String, String> compileAll(String resourcePackName) {
        return compileAll(resourcePackName, true);
    }

    /**
     * Compiles both item and terrain texture dictionaries with specified pack name and formatting.
     *
     * @param resourcePackName resource pack name identifier
     * @param prettyPrint whether to pretty-print JSON
     * @return map of pack path to JSON string
     */
    public Map<String, String> compileAll(String resourcePackName, boolean prettyPrint) {
        return Map.of(
                VesselBedrockTextureType.ITEM.dictionaryPath(), compileItemTextureJson(resourcePackName, prettyPrint),
                VesselBedrockTextureType.TERRAIN.dictionaryPath(), compileTerrainTextureJson(resourcePackName, DEFAULT_NUM_MIP_LEVELS, DEFAULT_TERRAIN_PADDING, prettyPrint)
        );
    }

    /**
     * Returns the count of registered item texture keys.
     *
     * @return item key count
     */
    public int itemCount() {
        return itemTextures.size();
    }

    /**
     * Returns the count of registered terrain texture keys.
     *
     * @return terrain key count
     */
    public int terrainCount() {
        return terrainTextures.size();
    }

    /**
     * Checks whether an item texture key is registered.
     *
     * @param key dictionary lookup key
     * @return true if key exists
     */
    public boolean hasItem(String key) {
        return key != null && itemTextures.containsKey(key.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Checks whether a terrain texture key is registered.
     *
     * @param key dictionary lookup key
     * @return true if key exists
     */
    public boolean hasTerrain(String key) {
        return key != null && terrainTextures.containsKey(key.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Retrieves the texture entry for a given item key.
     *
     * @param key dictionary lookup key
     * @return texture entry or null if absent
     */
    public VesselBedrockTextureEntry getItemEntry(String key) {
        if (key == null) {
            return null;
        }
        return itemTextures.get(key.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Retrieves the texture entry for a given terrain key.
     *
     * @param key dictionary lookup key
     * @return texture entry or null if absent
     */
    public VesselBedrockTextureEntry getTerrainEntry(String key) {
        if (key == null) {
            return null;
        }
        return terrainTextures.get(key.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Returns an unmodifiable view of all registered item texture entries.
     *
     * @return unmodifiable item textures map
     */
    public Map<String, VesselBedrockTextureEntry> itemTextures() {
        return Collections.unmodifiableMap(itemTextures);
    }

    /**
     * Returns an unmodifiable view of all registered terrain texture entries.
     *
     * @return unmodifiable terrain textures map
     */
    public Map<String, VesselBedrockTextureEntry> terrainTextures() {
        return Collections.unmodifiableMap(terrainTextures);
    }

    /**
     * Clears all registered item and terrain textures.
     */
    public void clear() {
        itemTextures.clear();
        terrainTextures.clear();
    }
}
