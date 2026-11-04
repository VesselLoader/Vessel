package dev.vessel.kernel.resourcepack.bedrock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Sanitizer for Bedrock Edition resource pack {@code manifest.json} payloads.
 * <p>
 * Scans and strips phantom {@code "dependencies"} declarations (such as references to server-side
 * virtual Behavior Packs) before distributing resource packs to connecting Bedrock clients.
 * In sovereign server architectures, server-side simulation handles behavior logic; distributing
 * resource packs with dangling behavior pack dependencies triggers fatal {@code "Missing Dependencies"}
 * warning dialogs or silent asset rejection on Bedrock clients.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class VesselBedrockManifestDependencyCleaner {

    private static final String DEPENDENCIES_KEY = "dependencies";
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson GSON_COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    /**
     * Constructs a new Bedrock manifest dependency cleaner.
     */
    public VesselBedrockManifestDependencyCleaner() {
    }

    /**
     * Inspects whether the provided {@code manifest.json} text declares a {@code "dependencies"} block.
     *
     * @param manifestJson raw manifest JSON string
     * @return true if dependencies declaration exists
     */
    public boolean hasDependencies(String manifestJson) {
        Objects.requireNonNull(manifestJson, "manifestJson cannot be null");
        String json = stripBom(manifestJson);
        if (json.isBlank()) {
            return false;
        }

        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                return false;
            }
            JsonObject obj = root.getAsJsonObject();
            return obj.has(DEPENDENCIES_KEY);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Counts the total number of dependency declarations inside {@code manifest.json}.
     *
     * @param manifestJson raw manifest JSON string
     * @return number of dependencies declared, or 0 if none
     */
    public int countDependencies(String manifestJson) {
        Objects.requireNonNull(manifestJson, "manifestJson cannot be null");
        String json = stripBom(manifestJson);
        if (json.isBlank()) {
            return 0;
        }

        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                return 0;
            }
            JsonObject obj = root.getAsJsonObject();
            if (!obj.has(DEPENDENCIES_KEY)) {
                return 0;
            }
            JsonElement depElement = obj.get(DEPENDENCIES_KEY);
            if (depElement.isJsonArray()) {
                return depElement.getAsJsonArray().size();
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Strips all {@code "dependencies"} declarations completely from {@code manifest.json} with default pretty-printing.
     *
     * @param manifestJson raw manifest JSON text
     * @return cleaned manifest JSON text
     */
    public String clean(String manifestJson) {
        return clean(manifestJson, true);
    }

    /**
     * Strips all {@code "dependencies"} declarations completely from {@code manifest.json} with specified formatting.
     *
     * @param manifestJson raw manifest JSON text
     * @param prettyPrint whether to pretty-print or emit high-performance compact JSON
     * @return cleaned manifest JSON text
     */
    public String clean(String manifestJson, boolean prettyPrint) {
        return cleanWithAudit(manifestJson, prettyPrint).cleanedManifestJson();
    }

    /**
     * Strips all {@code "dependencies"} declarations from raw manifest byte buffer.
     *
     * @param manifestBytes raw UTF-8 encoded manifest bytes
     * @return cleaned UTF-8 encoded manifest bytes
     */
    public byte[] clean(byte[] manifestBytes) {
        return clean(manifestBytes, true);
    }

    /**
     * Strips all {@code "dependencies"} declarations from raw manifest byte buffer with specified formatting.
     *
     * @param manifestBytes raw UTF-8 encoded manifest bytes
     * @param prettyPrint whether to pretty-print JSON
     * @return cleaned UTF-8 encoded manifest bytes
     */
    public byte[] clean(byte[] manifestBytes, boolean prettyPrint) {
        Objects.requireNonNull(manifestBytes, "manifestBytes cannot be null");
        byte[] bytesToParse = stripBomBytes(manifestBytes);
        String original = new String(bytesToParse, StandardCharsets.UTF_8);
        String cleaned = clean(original, prettyPrint);
        return cleaned.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Sanitizes {@code manifest.json} and returns a detailed audit result with default pretty printing.
     *
     * @param manifestJson raw manifest JSON string
     * @return cleaning audit result
     * @throws IllegalArgumentException if the manifest is invalid or malformed JSON
     */
    public VesselBedrockManifestCleaningResult cleanWithAudit(String manifestJson) {
        return cleanWithAudit(manifestJson, true);
    }

    /**
     * Sanitizes {@code manifest.json} and returns a detailed audit result.
     *
     * @param manifestJson raw manifest JSON string
     * @param prettyPrint whether to pretty-print JSON
     * @return cleaning audit result
     * @throws IllegalArgumentException if the manifest is invalid or malformed JSON
     */
    public VesselBedrockManifestCleaningResult cleanWithAudit(String manifestJson, boolean prettyPrint) {
        Objects.requireNonNull(manifestJson, "manifestJson cannot be null");
        String json = stripBom(manifestJson);
        if (json.isBlank()) {
            throw new IllegalArgumentException("manifestJson cannot be blank");
        }

        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed manifest JSON: " + e.getMessage(), e);
        }

        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("Root manifest JSON element must be an object");
        }

        JsonObject obj = root.getAsJsonObject();
        if (!obj.has(DEPENDENCIES_KEY)) {
            return VesselBedrockManifestCleaningResult.unmodified(manifestJson);
        }

        JsonElement depElement = obj.get(DEPENDENCIES_KEY);
        List<String> removedIdentifiers = new ArrayList<>();
        int count = 0;

        if (depElement.isJsonArray()) {
            JsonArray arr = depElement.getAsJsonArray();
            count = arr.size();
            for (JsonElement item : arr) {
                if (item.isJsonObject()) {
                    JsonObject itemObj = item.getAsJsonObject();
                    if (itemObj.has("uuid")) {
                        removedIdentifiers.add(itemObj.get("uuid").getAsString());
                    } else {
                        removedIdentifiers.add(itemObj.toString());
                    }
                } else if (item.isJsonPrimitive()) {
                    removedIdentifiers.add(item.getAsString());
                }
            }
        } else {
            count = 1;
            removedIdentifiers.add(depElement.toString());
        }

        // Remove the dependencies field entirely
        obj.remove(DEPENDENCIES_KEY);

        Gson serializer = prettyPrint ? GSON_PRETTY : GSON_COMPACT;
        String cleanedJson = serializer.toJson(obj);
        return new VesselBedrockManifestCleaningResult(true, count, cleanedJson, removedIdentifiers);
    }

    /**
     * Selectively strips targeted virtual Behavior Pack UUID dependencies while preserving
     * legitimate resource pack dependencies.
     *
     * @param manifestJson raw manifest JSON string
     * @param targetVirtualUuids set of virtual UUIDs to strip (matched case-insensitively)
     * @return cleaning audit result
     */
    public VesselBedrockManifestCleaningResult cleanTargetedDependencies(
            String manifestJson,
            Set<String> targetVirtualUuids
    ) {
        return cleanTargetedDependencies(manifestJson, targetVirtualUuids, true);
    }

    /**
     * Selectively strips targeted virtual Behavior Pack UUID dependencies while preserving
     * legitimate resource pack dependencies.
     *
     * @param manifestJson raw manifest JSON string
     * @param targetVirtualUuids set of virtual UUIDs to strip (matched case-insensitively)
     * @param prettyPrint whether to pretty-print JSON
     * @return cleaning audit result
     */
    public VesselBedrockManifestCleaningResult cleanTargetedDependencies(
            String manifestJson,
            Set<String> targetVirtualUuids,
            boolean prettyPrint
    ) {
        Objects.requireNonNull(manifestJson, "manifestJson cannot be null");
        Objects.requireNonNull(targetVirtualUuids, "targetVirtualUuids cannot be null");

        String json = stripBom(manifestJson);
        if (json.isBlank() || targetVirtualUuids.isEmpty()) {
            return VesselBedrockManifestCleaningResult.unmodified(manifestJson);
        }

        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed manifest JSON: " + e.getMessage(), e);
        }

        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("Root manifest JSON element must be an object");
        }

        JsonObject obj = root.getAsJsonObject();
        if (!obj.has(DEPENDENCIES_KEY)) {
            return VesselBedrockManifestCleaningResult.unmodified(manifestJson);
        }

        JsonElement depElement = obj.get(DEPENDENCIES_KEY);
        if (!depElement.isJsonArray()) {
            return cleanWithAudit(manifestJson, prettyPrint);
        }

        // Build canonical lowercase set of target UUIDs for case-insensitive matching
        Set<String> normalizedTargets = new java.util.HashSet<>(targetVirtualUuids.size());
        for (String target : targetVirtualUuids) {
            if (target != null) {
                normalizedTargets.add(target.trim().toLowerCase(java.util.Locale.ROOT));
            }
        }

        JsonArray arr = depElement.getAsJsonArray();
        JsonArray remaining = new JsonArray();
        List<String> removedUuids = new ArrayList<>();

        for (JsonElement item : arr) {
            boolean shouldRemove = false;
            if (item.isJsonObject()) {
                JsonObject itemObj = item.getAsJsonObject();
                if (itemObj.has("uuid")) {
                    String uuid = itemObj.get("uuid").getAsString();
                    if (normalizedTargets.contains(uuid.trim().toLowerCase(java.util.Locale.ROOT))) {
                        shouldRemove = true;
                        removedUuids.add(uuid);
                    }
                }
            } else if (item.isJsonPrimitive()) {
                String val = item.getAsString();
                if (normalizedTargets.contains(val.trim().toLowerCase(java.util.Locale.ROOT))) {
                    shouldRemove = true;
                    removedUuids.add(val);
                }
            }

            if (!shouldRemove) {
                remaining.add(item);
            }
        }

        if (removedUuids.isEmpty()) {
            return VesselBedrockManifestCleaningResult.unmodified(manifestJson);
        }

        if (remaining.isEmpty()) {
            obj.remove(DEPENDENCIES_KEY);
        } else {
            obj.add(DEPENDENCIES_KEY, remaining);
        }

        Gson serializer = prettyPrint ? GSON_PRETTY : GSON_COMPACT;
        String cleanedJson = serializer.toJson(obj);
        return new VesselBedrockManifestCleaningResult(true, removedUuids.size(), cleanedJson, removedUuids);
    }

    /**
     * Selectively strips targeted virtual Behavior Pack UUID dependencies from byte buffer.
     *
     * @param manifestBytes raw manifest bytes
     * @param targetVirtualUuids set of virtual UUIDs to strip
     * @return cleaned manifest byte array
     */
    public byte[] cleanTargetedDependencies(byte[] manifestBytes, Set<String> targetVirtualUuids) {
        return cleanTargetedDependencies(manifestBytes, targetVirtualUuids, true);
    }

    /**
     * Selectively strips targeted virtual Behavior Pack UUID dependencies from byte buffer with formatting option.
     *
     * @param manifestBytes raw manifest bytes
     * @param targetVirtualUuids set of virtual UUIDs to strip
     * @param prettyPrint whether to pretty-print JSON
     * @return cleaned manifest byte array
     */
    public byte[] cleanTargetedDependencies(byte[] manifestBytes, Set<String> targetVirtualUuids, boolean prettyPrint) {
        Objects.requireNonNull(manifestBytes, "manifestBytes cannot be null");
        Objects.requireNonNull(targetVirtualUuids, "targetVirtualUuids cannot be null");
        byte[] bytesToParse = stripBomBytes(manifestBytes);
        String original = new String(bytesToParse, StandardCharsets.UTF_8);
        VesselBedrockManifestCleaningResult result = cleanTargetedDependencies(original, targetVirtualUuids, prettyPrint);
        return result.cleanedManifestJson().getBytes(StandardCharsets.UTF_8);
    }

    private static String stripBom(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        if (trimmed.startsWith("\uFEFF")) {
            return trimmed.substring(1).trim();
        }
        return trimmed;
    }

    private static byte[] stripBomBytes(byte[] bytes) {
        if (bytes != null && bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return java.util.Arrays.copyOfRange(bytes, 3, bytes.length);
        }
        return bytes;
    }
}
