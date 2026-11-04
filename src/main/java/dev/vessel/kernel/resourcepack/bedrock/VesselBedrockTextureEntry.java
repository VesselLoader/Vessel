package dev.vessel.kernel.resourcepack.bedrock;

import java.util.Objects;

/**
 * Immutable descriptor representing a registered texture entry in Bedrock texture dictionaries.
 * <p>
 * Stores the isolated shortname, namespace, raw identifier name, normalized Bedrock relative path,
 * and target atlas type. Automatically normalizes texture paths by removing trailing {@code .png}
 * extensions and converting file separators to forward slashes.
 * </p>
 *
 * @param shortname isolated dictionary key identifier (e.g. "techmod:wrench" or "apple")
 * @param namespace owning namespace (e.g. "techmod" or "minecraft")
 * @param rawName raw unqualified asset name (e.g. "wrench" or "apple")
 * @param bedrockPath normalized texture path without {@code .png} extension
 * @param type target dictionary type (ITEM or TERRAIN)
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public record VesselBedrockTextureEntry(
        String shortname,
        String namespace,
        String rawName,
        String bedrockPath,
        VesselBedrockTextureType type
) {

    public VesselBedrockTextureEntry {
        Objects.requireNonNull(shortname, "shortname cannot be null");
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(rawName, "rawName cannot be null");
        Objects.requireNonNull(bedrockPath, "bedrockPath cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        // Normalize path: replace backslashes, trim leading slashes
        String normalized = bedrockPath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        // Handle Java resource pack assets/ convention
        if (normalized.toLowerCase(java.util.Locale.ROOT).startsWith("assets/")) {
            String[] segments = normalized.split("/");
            if (segments.length >= 4 && segments[2].equalsIgnoreCase("textures")) {
                String ns = segments[1];
                String cat = switch (segments[3].toLowerCase(java.util.Locale.ROOT)) {
                    case "block" -> "blocks";
                    case "item" -> "items";
                    case "particle" -> "particles";
                    default -> segments[3];
                };
                StringBuilder sb = new StringBuilder();
                for (int i = 4; i < segments.length; i++) {
                    if (i > 4) {
                        sb.append('/');
                    }
                    sb.append(segments[i]);
                }
                String filePart = sb.toString();
                if (ns.equalsIgnoreCase("minecraft")) {
                    normalized = filePart.isEmpty() ? "textures/" + cat : "textures/" + cat + "/" + filePart;
                } else {
                    normalized = filePart.isEmpty() ? "textures/" + ns + "/" + cat : "textures/" + ns + "/" + cat + "/" + filePart;
                }
            }
        }

        // Strip common image file extensions
        String lower = normalized.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".png")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        } else if (lower.endsWith(".tga")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        } else if (lower.endsWith(".jpg")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        } else if (lower.endsWith(".jpeg")) {
            normalized = normalized.substring(0, normalized.length() - 5);
        }

        // Ensure Bedrock texture path is rooted under textures/
        if (!normalized.toLowerCase(java.util.Locale.ROOT).startsWith("textures/")) {
            normalized = "textures/" + normalized;
        }

        bedrockPath = normalized;
    }

    /**
     * Factory method creating a texture entry from namespace, raw name, texture path, and type.
     *
     * @param namespace owning namespace
     * @param rawName unqualified identifier
     * @param bedrockPath relative Bedrock texture path
     * @param type atlas type
     * @return new texture entry
     */
    public static VesselBedrockTextureEntry of(
            String namespace,
            String rawName,
            String bedrockPath,
            VesselBedrockTextureType type
    ) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(rawName, "rawName cannot be null");
        String shortname = namespace.equalsIgnoreCase("minecraft")
                ? rawName
                : namespace + ":" + rawName;
        return new VesselBedrockTextureEntry(shortname, namespace, rawName, bedrockPath, type);
    }
}
