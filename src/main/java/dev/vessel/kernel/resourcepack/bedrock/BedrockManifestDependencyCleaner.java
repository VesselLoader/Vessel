package dev.vessel.kernel.resourcepack.bedrock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Objects;

/**
 * Sanitizer for Bedrock {@code manifest.json} files removing phantom behavior pack dependencies.
 * <p>
 * Third-party tools often generate Bedrock resource packs declaring dummy or virtual
 * {@code "dependencies"} to non-existent behavior packs. When connecting to servers with such packs,
 * mobile and console Bedrock clients abort pack download with the fatal error
 * <em>"Some dependencies are missing"</em>. This cleaner strips the {@code "dependencies"} array
 * entirely, ensuring flawless client-side pack acceptance.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BedrockManifestDependencyCleaner {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * Cleanses a Bedrock {@code manifest.json} document by stripping any {@code "dependencies"} property.
     *
     * @param manifestJson raw JSON string of manifest.json
     * @return cleaned manifest JSON without behavior pack dependencies
     */
    public String cleanDependencies(String manifestJson) {
        Objects.requireNonNull(manifestJson, "manifestJson cannot be null");
        if (manifestJson.isBlank()) {
            return manifestJson;
        }

        JsonElement element;
        try {
            element = JsonParser.parseString(manifestJson);
        } catch (Exception e) {
            return manifestJson; // Retain original if unparseable
        }

        if (!element.isJsonObject()) {
            return manifestJson;
        }

        JsonObject obj = element.getAsJsonObject();
        boolean modified = false;

        if (obj.has("dependencies")) {
            obj.remove("dependencies");
            modified = true;
        }

        if (!modified) {
            return manifestJson;
        }

        return gson.toJson(obj);
    }
}
