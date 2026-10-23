package dev.vessel.bootstrap.ecosystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Maintains official open-source attribution, copyright notices, licenses, and upstream project
 * links for third-party ecosystem integrations bundled with VESSEL — specifically
 * <b>GeyserMC (Geyser &amp; Floodgate)</b> and <b>Spark Profiler (lucko/spark)</b>.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class ThirdPartyCreditsAndAttributionRegistry {

    public static final String CREDITS_FILE_NAME = "CREDITS.md";

    public static final String GEYSER_PROJECT_NAME = "GeyserMC (Geyser & Floodgate)";
    public static final String GEYSER_AUTHORS = "GeyserMC Team & Contributors";
    public static final String GEYSER_LICENSE = "MIT License";
    public static final String GEYSER_UPSTREAM_URL = "https://geysermc.org / https://github.com/GeyserMC/Geyser";

    public static final String FLOODGATE_PROJECT_NAME = "Floodgate (Bedrock Xbox Live Auth Bridge)";
    public static final String FLOODGATE_AUTHORS = "GeyserMC Team & Contributors";
    public static final String FLOODGATE_LICENSE = "MIT License";
    public static final String FLOODGATE_UPSTREAM_URL = "https://geysermc.org / https://github.com/GeyserMC/Floodgate";

    public static final String SPARK_PROJECT_NAME = "spark (Performance Profiler)";
    public static final String SPARK_AUTHORS = "Lucko (lucko) & Contributors";
    public static final String SPARK_LICENSE = "GPL-3.0 (Standalone Profiler Agent/Plugin Integration)";
    public static final String SPARK_UPSTREAM_URL = "https://spark.lucko.me / https://github.com/lucko/spark";

    /**
     * Returns the formatted attribution line for GeyserMC.
     *
     * @param activeGeyserVersion active Geyser build/version string
     * @return attribution notice string
     */
    public String getGeyserAttributionNotice(String activeGeyserVersion) {
        Objects.requireNonNull(activeGeyserVersion, "activeGeyserVersion must not be null");
        return GEYSER_PROJECT_NAME + " v" + activeGeyserVersion
                + " — Copyright (c) 2019-2026 " + GEYSER_AUTHORS
                + " (" + GEYSER_LICENSE + ") — " + GEYSER_UPSTREAM_URL;
    }

    /**
     * Returns the formatted attribution line for Spark Profiler.
     *
     * @param activeSparkVersion active Spark release version string
     * @return attribution notice string
     */
    public String getSparkAttributionNotice(String activeSparkVersion) {
        Objects.requireNonNull(activeSparkVersion, "activeSparkVersion must not be null");
        return SPARK_PROJECT_NAME + " v" + activeSparkVersion
                + " — Copyright (c) 2018-2026 " + SPARK_AUTHORS
                + " (" + SPARK_LICENSE + ") — " + SPARK_UPSTREAM_URL;
    }

    /**
     * Returns an ordered list of all third-party attribution notices for console startup logging.
     *
     * @param geyserVersion active Geyser version
     * @param sparkVersion  active Spark version
     * @return immutable list of attribution lines
     */
    public List<String> getStartupAttributionBannerLines(
            String geyserVersion,
            String sparkVersion) {
        return List.of(
                "[VESSEL Credits] Built-in Bedrock Crossplay powered by "
                        + getGeyserAttributionNotice(geyserVersion),
                "[VESSEL Credits] Built-in Tick & Heap Profiling powered by "
                        + getSparkAttributionNotice(sparkVersion));
    }

    /**
     * Returns the formatted attribution line for Floodgate.
     *
     * @param activeFloodgateVersion active Floodgate release version string
     * @return attribution notice string
     */
    public String getFloodgateAttributionNotice(String activeFloodgateVersion) {
        Objects.requireNonNull(activeFloodgateVersion, "activeFloodgateVersion must not be null");
        return FLOODGATE_PROJECT_NAME + " v" + activeFloodgateVersion
                + " - Copyright (c) 2019-2026 " + FLOODGATE_AUTHORS
                + " (" + FLOODGATE_LICENSE + ") - " + FLOODGATE_UPSTREAM_URL;
    }

    /**
     * Generates the complete {@code CREDITS.md} markdown document crediting GeyserMC, Floodgate,
     * Spark, and Mojang Studios.
     *
     * @param geyserVersion    active Geyser version
     * @param sparkVersion     active Spark version
     * @param minecraftVersion active Minecraft release version
     * @return markdown content string
     */
    public String buildFullCreditsMarkdown(
            String geyserVersion,
            String sparkVersion,
            String minecraftVersion) {
        return buildFullCreditsMarkdown(
                geyserVersion,
                BuiltInGeyserAndSparkProvisioner.DEFAULT_LATEST_FLOODGATE_VERSION,
                sparkVersion,
                minecraftVersion);
    }

    /**
     * Generates the complete {@code CREDITS.md} markdown document crediting GeyserMC, Floodgate,
     * Spark, and Mojang Studios.
     *
     * @param geyserVersion    active Geyser version
     * @param floodgateVersion active Floodgate version
     * @param sparkVersion     active Spark version
     * @param minecraftVersion active Minecraft release version
     * @return markdown content string
     */
    public String buildFullCreditsMarkdown(
            String geyserVersion,
            String floodgateVersion,
            String sparkVersion,
            String minecraftVersion) {
        Objects.requireNonNull(geyserVersion, "geyserVersion must not be null");
        Objects.requireNonNull(floodgateVersion, "floodgateVersion must not be null");
        Objects.requireNonNull(sparkVersion, "sparkVersion must not be null");
        Objects.requireNonNull(minecraftVersion, "minecraftVersion must not be null");

        return """
                # VESSEL Third-Party Open-Source Credits & Attributions

                VESSEL integrates and auto-provisions the latest official releases of **GeyserMC**, **Floodgate**, and **spark** via Modrinth & GeyserMC APIs to provide out-of-the-box Bedrock Edition crossplay and low-overhead server performance profiling on **Minecraft %s**. Full credit and gratitude belong to their respective authors and contributors:

                ---

                ## 1. GeyserMC (`Geyser` & `Floodgate`)
                - **Integrated Geyser Version:** `%s` (Auto-updated to latest release on every VESSEL update)
                - **Integrated Floodgate Version:** `%s` (Auto-updated to latest release on every VESSEL update)
                - **Authors:** %s
                - **License:** %s
                - **Official Website & Repositories:** %s | %s
                - **Role in VESSEL:** Enables seamless Minecraft: Bedrock Edition client connections (UDP 19132) to VESSEL Java Edition servers with protocol translation, Xbox Live / Floodgate authentication, and automatic custom block/item resource pack mapping.

                ---

                ## 2. spark (`lucko/spark` Performance Profiler)
                - **Integrated Version:** `%s` (Auto-updated to latest release on every VESSEL update)
                - **Authors:** %s
                - **License:** %s
                - **Official Website & Repository:** %s
                - **Role in VESSEL:** Provides production-ready async CPU sampling, MSPT tick duration histograms, GC pressure monitoring, and heap summary diagnostics alongside VESSEL's internal microkernel telemetry.

                ---

                ## 3. Minecraft Dedicated Server (`Mojang Studios`)
                - **Target Default Release:** `%s` (`latest.release` via Mojang Piston Meta API)
                - **Copyright:** Copyright (c) Mojang AB. Do not distribute proprietary Mojang assets outside official EULA terms (`eula=true`).
                """.stripIndent().formatted(
                minecraftVersion,
                geyserVersion,
                floodgateVersion,
                GEYSER_AUTHORS,
                GEYSER_LICENSE,
                GEYSER_UPSTREAM_URL,
                FLOODGATE_UPSTREAM_URL,
                sparkVersion,
                SPARK_AUTHORS,
                SPARK_LICENSE,
                SPARK_UPSTREAM_URL,
                minecraftVersion);
    }

    /**
     * Writes or updates {@code CREDITS.md} inside the specified server root directory.
     *
     * @param serverRoot       server root directory
     * @param geyserVersion    active Geyser version
     * @param sparkVersion     active Spark version
     * @param minecraftVersion active Minecraft release version
     * @return path to the written {@code CREDITS.md} file
     */
    public Path writeCreditsFileToServerRoot(
            Path serverRoot,
            String geyserVersion,
            String sparkVersion,
            String minecraftVersion) {
        return writeCreditsFileToServerRoot(
                serverRoot,
                geyserVersion,
                BuiltInGeyserAndSparkProvisioner.DEFAULT_LATEST_FLOODGATE_VERSION,
                sparkVersion,
                minecraftVersion);
    }

    /**
     * Writes or updates {@code CREDITS.md} inside the specified server root directory.
     *
     * @param serverRoot       server root directory
     * @param geyserVersion    active Geyser version
     * @param floodgateVersion active Floodgate version
     * @param sparkVersion     active Spark version
     * @param minecraftVersion active Minecraft release version
     * @return path to the written {@code CREDITS.md} file
     */
    public Path writeCreditsFileToServerRoot(
            Path serverRoot,
            String geyserVersion,
            String floodgateVersion,
            String sparkVersion,
            String minecraftVersion) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Path creditsFile = serverRoot.resolve(CREDITS_FILE_NAME);
        String markdown = buildFullCreditsMarkdown(
                geyserVersion, floodgateVersion, sparkVersion, minecraftVersion);
        try {
            Files.createDirectories(serverRoot);
            Files.writeString(creditsFile, markdown, StandardCharsets.UTF_8);
            return creditsFile;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write CREDITS.md to " + creditsFile, e);
        }
    }
}
