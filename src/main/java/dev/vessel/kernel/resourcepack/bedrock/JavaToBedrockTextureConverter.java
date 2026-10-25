package dev.vessel.kernel.resourcepack.bedrock;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance converter transforming Java Edition texture paths, formats, and animation
 * metadata to Bedrock Edition texture and flipbook specifications.
 * <p>
 * Handles namespace remapping (e.g. {@code assets/minecraft/textures/block/} to {@code textures/blocks/}),
 * PNG binary header validation, texture dimension extraction, and {@code .png.mcmeta} animation parsing.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class JavaToBedrockTextureConverter {

    private static final byte[] PNG_MAGIC = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private static final Pattern FRAMETIME_PATTERN = Pattern.compile("\"frametime\"\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERPOLATE_PATTERN = Pattern.compile("\"interpolate\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FRAMES_ARRAY_PATTERN = Pattern.compile("\"frames\"\\s*:\\s*\\[([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern FRAME_TOKEN_PATTERN = Pattern.compile("\\{[^}]*\\}|\\d+");
    private static final Pattern INDEX_IN_OBJECT_PATTERN = Pattern.compile("\"index\"\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FRAME_HEIGHT_PATTERN = Pattern.compile("\"height\"\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    /**
     * Converts a Java Edition resource pack texture path to a Bedrock Edition texture path.
     * <p>
     * Java convention: {@code assets/<namespace>/textures/<category>/<file>}
     * Bedrock convention:
     * <ul>
     *   <li>Vanilla {@code block} &rarr; {@code textures/blocks/}</li>
     *   <li>Vanilla {@code item} &rarr; {@code textures/items/}</li>
     *   <li>Vanilla {@code particle} &rarr; {@code textures/particles/}</li>
     *   <li>Custom namespace {@code assets/<mod>/textures/<category>/<file>} &rarr; {@code textures/<mod>/<category>/<file>}</li>
     * </ul>
     *
     * @param javaPath Java texture path
     * @return normalized Bedrock texture path
     * @throws IllegalArgumentException if the path does not conform to Java resource pack conventions
     */
    public String convertPath(String javaPath) {
        Objects.requireNonNull(javaPath, "javaPath cannot be null");
        String normalized = javaPath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (!normalized.toLowerCase(java.util.Locale.ROOT).startsWith("assets/")) {
            throw new IllegalArgumentException("Java resource pack path must start with 'assets/': " + javaPath);
        }

        String[] parts = normalized.split("/");
        if (parts.length < 4 || !parts[2].equalsIgnoreCase("textures")) {
            throw new IllegalArgumentException("Path does not match 'assets/<namespace>/textures/...': " + javaPath);
        }

        String namespace = parts[1];
        String category = parts[3];

        // Bedrock uses plural 'blocks', 'items', and 'particles'
        String mappedCategory = switch (category.toLowerCase(java.util.Locale.ROOT)) {
            case "block" -> "blocks";
            case "item" -> "items";
            case "particle" -> "particles";
            default -> category;
        };

        // Remaining subpath
        StringBuilder subpath = new StringBuilder();
        for (int i = 4; i < parts.length; i++) {
            if (i > 4) {
                subpath.append('/');
            }
            subpath.append(parts[i]);
        }

        String fileName = subpath.toString();
        if (namespace.equalsIgnoreCase("minecraft")) {
            return fileName.isEmpty()
                    ? "textures/" + mappedCategory
                    : "textures/" + mappedCategory + "/" + fileName;
        } else {
            return fileName.isEmpty()
                    ? "textures/" + namespace + "/" + mappedCategory
                    : "textures/" + namespace + "/" + mappedCategory + "/" + fileName;
        }
    }

    /**
     * Validates whether raw byte buffer contains a valid PNG image signature.
     *
     * @param data raw file bytes
     * @return true if byte stream matches standard PNG magic header
     */
    public boolean isValidPng(byte[] data) {
        if (data == null || data.length < PNG_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (data[i] != PNG_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extracts dimensions [width, height] from a PNG byte array header without allocating BufferedImage.
     *
     * @param data raw PNG bytes
     * @return int array where [0] is width and [1] is height in pixels
     * @throws IllegalArgumentException if data is not a valid PNG, is truncated, or lacks IHDR chunk
     */
    public int[] extractPngDimensions(byte[] data) {
        if (!isValidPng(data) || data.length < 24) {
            throw new IllegalArgumentException("Invalid or truncated PNG byte array");
        }

        // Validate IHDR chunk type at bytes 12..15
        if (data[12] != 'I' || data[13] != 'H' || data[14] != 'D' || data[15] != 'R') {
            throw new IllegalArgumentException("PNG missing mandatory IHDR chunk header");
        }

        int width = ((data[16] & 0xFF) << 24)
                | ((data[17] & 0xFF) << 16)
                | ((data[18] & 0xFF) << 8)
                | (data[19] & 0xFF);

        int height = ((data[20] & 0xFF) << 24)
                | ((data[21] & 0xFF) << 16)
                | ((data[22] & 0xFF) << 8)
                | (data[23] & 0xFF);

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Extracted non-positive PNG dimensions: " + width + "x" + height);
        }

        return new int[]{width, height};
    }

    /**
     * Converts a Java texture and optional {@code .mcmeta} animation specification into Bedrock texture metadata.
     *
     * @param javaPath original Java resource path
     * @param pngData raw PNG bytes for dimension inspection
     * @param mcmetaJson optional JSON content from companion {@code .mcmeta} file, or null
     * @return converted {@link BedrockTextureMetadata}
     */
    public BedrockTextureMetadata convertTexture(String javaPath, byte[] pngData, String mcmetaJson) {
        String bedrockPath = convertPath(javaPath);
        int[] dimensions = extractPngDimensions(pngData);
        int width = dimensions[0];
        int height = dimensions[1];

        int frameHeight = width;
        int ticksPerFrame = 1;
        boolean interpolate = false;
        int[] customFrames = null;

        if (mcmetaJson != null && !mcmetaJson.isBlank()) {
            Matcher frameHeightMatcher = FRAME_HEIGHT_PATTERN.matcher(mcmetaJson);
            if (frameHeightMatcher.find()) {
                int parsedHeight = Integer.parseInt(frameHeightMatcher.group(1));
                if (parsedHeight > 0 && parsedHeight <= height) {
                    frameHeight = parsedHeight;
                }
            }

            Matcher frameTimeMatcher = FRAMETIME_PATTERN.matcher(mcmetaJson);
            if (frameTimeMatcher.find()) {
                ticksPerFrame = Math.max(1, Integer.parseInt(frameTimeMatcher.group(1)));
            }
            Matcher interpolateMatcher = INTERPOLATE_PATTERN.matcher(mcmetaJson);
            if (interpolateMatcher.find()) {
                interpolate = Boolean.parseBoolean(interpolateMatcher.group(1));
            }

            Matcher framesArrayMatcher = FRAMES_ARRAY_PATTERN.matcher(mcmetaJson);
            if (framesArrayMatcher.find()) {
                String framesContent = framesArrayMatcher.group(1);
                Matcher tokenMatcher = FRAME_TOKEN_PATTERN.matcher(framesContent);
                java.util.List<Integer> parsedFrames = new java.util.ArrayList<>();
                while (tokenMatcher.find()) {
                    String token = tokenMatcher.group();
                    if (token.startsWith("{")) {
                        Matcher objMatcher = INDEX_IN_OBJECT_PATTERN.matcher(token);
                        if (objMatcher.find()) {
                            parsedFrames.add(Integer.parseInt(objMatcher.group(1)));
                        }
                    } else {
                        parsedFrames.add(Integer.parseInt(token));
                    }
                }
                if (!parsedFrames.isEmpty()) {
                    customFrames = new int[parsedFrames.size()];
                    for (int i = 0; i < parsedFrames.size(); i++) {
                        customFrames[i] = parsedFrames.get(i);
                    }
                }
            }
        }

        int frameCount = 1;
        if (height > frameHeight && height % frameHeight == 0) {
            frameCount = height / frameHeight;
        }

        boolean animated = frameCount > 1
                || (customFrames != null && customFrames.length > 1)
                || (mcmetaJson != null && mcmetaJson.contains("\"animation\""));

        if (customFrames != null) {
            return BedrockTextureMetadata.ofAnimated(bedrockPath, javaPath, ticksPerFrame, frameCount, interpolate, customFrames);
        } else if (animated) {
            return BedrockTextureMetadata.ofAnimated(bedrockPath, javaPath, ticksPerFrame, frameCount, interpolate);
        } else {
            return BedrockTextureMetadata.ofStatic(bedrockPath, javaPath);
        }
    }

    /**
     * Compiles a collection of converted texture metadata into standard Bedrock {@code textures/flipbook_textures.json}.
     *
     * @param textures list of texture metadata records
     * @return formatted JSON array string for flipbook animations
     */
    public String generateFlipbookTexturesJson(List<BedrockTextureMetadata> textures) {
        Objects.requireNonNull(textures, "textures cannot be null");
        StringBuilder sb = new StringBuilder("[\n");
        boolean first = true;

        for (BedrockTextureMetadata meta : textures) {
            if (!meta.animated()) {
                continue;
            }
            if (!first) {
                sb.append(",\n");
            }
            first = false;

            // Generate atlas tile key from file name
            String fileName = meta.bedrockPath();
            int lastSlash = fileName.lastIndexOf('/');
            if (lastSlash >= 0) {
                fileName = fileName.substring(lastSlash + 1);
            }
            if (fileName.endsWith(".png")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }
            if (fileName.endsWith(".tga")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }

            String entry = meta.toFlipbookJsonEntry(fileName);
            // Indent entry
            String[] lines = entry.split("\n");
            for (int i = 0; i < lines.length; i++) {
                sb.append("  ").append(lines[i]);
                if (i < lines.length - 1) {
                    sb.append("\n");
                }
            }
        }

        sb.append("\n]");
        return sb.toString();
    }
}
