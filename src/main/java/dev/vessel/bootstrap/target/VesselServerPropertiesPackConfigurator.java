package dev.vessel.bootstrap.target;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Updates {@code server.properties} with resource pack configuration adhering to the exact
 * era-specific property keys supported by the running Minecraft version:
 * <ul>
 *   <li>{@code resource-pack}: all versions (1.6+)</li>
 *   <li>{@code resource-pack-sha1}: Minecraft 1.10+</li>
 *   <li>{@code resource-pack-prompt}: Minecraft 1.17+</li>
 *   <li>{@code resource-pack-id}: Minecraft 1.20.3+</li>
 * </ul>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class VesselServerPropertiesPackConfigurator {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    public static final String DEFAULT_PROMPT_JSON =
            "{\"text\":\"VESSEL Sovereign Universal Mod Presentation Pack\"}";
    public static final String DEFAULT_PACK_UUID = "fcce4567-99de-3e01-949b-9799d6489542";

    public VesselServerPropertiesPackConfigurator() {
    }

    /**
     * Injects the resource pack download URL, SHA-1 checksum, prompt, and pack ID into
     * {@code server.properties} according to the capabilities of {@code mcVersion}.
     *
     * @param serverRoot root directory of the server
     * @param packUrl    local HTTP URL serving the resource pack
     * @param packSha1   40-character SHA-1 hexadecimal hash
     * @param mcVersion  active Minecraft version string
     */
    public void configureServerProperties(
            Path serverRoot,
            String packUrl,
            String packSha1,
            String mcVersion) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Objects.requireNonNull(packUrl, "packUrl must not be null");
        Objects.requireNonNull(packSha1, "packSha1 must not be null");

        Path propsPath = serverRoot.resolve("server.properties");
        if (!Files.isRegularFile(propsPath)) {
            return;
        }

        boolean supportsSha1 = isSupportedSha1(mcVersion);
        boolean supportsPrompt = isSupportedPrompt(mcVersion);
        boolean supportsPackId = isSupportedPackId(mcVersion);

        try {
            String content = Files.readString(propsPath, StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder(content.length() + 256);
            boolean hasPack = false;
            boolean hasSha1 = false;
            boolean hasPrompt = false;
            boolean hasPackId = false;

            for (String rawLine : content.split("\\R")) {
                String line = rawLine.trim();
                if (line.startsWith("resource-pack=")) {
                    sb.append("resource-pack=").append(packUrl).append('\n');
                    hasPack = true;
                } else if (line.startsWith("resource-pack-sha1=")) {
                    if (supportsSha1) {
                        sb.append("resource-pack-sha1=").append(packSha1).append('\n');
                        hasSha1 = true;
                    }
                } else if (line.startsWith("resource-pack-id=")) {
                    if (supportsPackId) {
                        sb.append("resource-pack-id=").append(DEFAULT_PACK_UUID).append('\n');
                        hasPackId = true;
                    }
                } else if (line.startsWith("resource-pack-prompt=")) {
                    if (supportsPrompt) {
                        sb.append("resource-pack-prompt=").append(DEFAULT_PROMPT_JSON).append('\n');
                        hasPrompt = true;
                    }
                } else {
                    sb.append(rawLine).append('\n');
                }
            }

            if (!hasPack) {
                sb.append("resource-pack=").append(packUrl).append('\n');
            }
            if (supportsSha1 && !hasSha1) {
                sb.append("resource-pack-sha1=").append(packSha1).append('\n');
            }
            if (supportsPrompt && !hasPrompt) {
                sb.append("resource-pack-prompt=").append(DEFAULT_PROMPT_JSON).append('\n');
            }
            if (supportsPackId && !hasPackId) {
                sb.append("resource-pack-id=").append(DEFAULT_PACK_UUID).append('\n');
            }

            Files.writeString(propsPath, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public boolean isSupportedSha1(String mcVersion) {
        int[] v = parseVersion(mcVersion);
        if (v[0] >= 26) {
            return true;
        }
        return v[0] == 1 && v[1] >= 10;
    }

    public boolean isSupportedPrompt(String mcVersion) {
        int[] v = parseVersion(mcVersion);
        if (v[0] >= 26) {
            return true;
        }
        return v[0] == 1 && v[1] >= 17;
    }

    public boolean isSupportedPackId(String mcVersion) {
        int[] v = parseVersion(mcVersion);
        if (v[0] >= 26) {
            return true;
        }
        if (v[0] != 1) {
            return true;
        }
        if (v[1] > 20) {
            return true;
        }
        return v[1] == 20 && v[2] >= 3;
    }

    private static int[] parseVersion(String mcVersion) {
        if (mcVersion == null || mcVersion.isBlank()) {
            return new int[]{1, 21, 4};
        }
        String trimmed = mcVersion.trim();
        if (trimmed.startsWith("26.") || trimmed.equals("26")) {
            return new int[]{26, 3, 0};
        }
        Matcher matcher = VERSION_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return new int[]{1, 21, 4};
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        return new int[]{major, minor, patch};
    }
}
