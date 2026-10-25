package dev.vessel.bootstrap.ecosystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Maintains official open-source attribution, copyright notices, licenses, and upstream project
 * links for third-party ecosystem integrations and lag-reduction / optimization algorithms bundled
 * or adapted in VESSEL — including GeyserMC, Floodgate, spark, Sodium, Lithium, FerriteCore,
 * ModernFix, C2ME, Krypton, LazyDFU, FastSuite, Ksyxis, PaperMC/Folia, Valkyrien Skies, and Mojang Studios.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public final class ThirdPartyCreditsAndAttributionRegistry {

    public static final String CREDITS_FILE_NAME = "CREDITS.md";

    // 1. GeyserMC
    public static final String GEYSER_PROJECT_NAME = "GeyserMC (Geyser & Floodgate)";
    public static final String GEYSER_AUTHORS = "GeyserMC Team & Contributors";
    public static final String GEYSER_LICENSE = "MIT License";
    public static final String GEYSER_UPSTREAM_URL = "https://geysermc.org / https://github.com/GeyserMC/Geyser";
    public static final String GEYSER_ROLE =
            "Enables seamless Minecraft: Bedrock Edition client connections (UDP 19132) to VESSEL Java Edition servers "
                    + "with protocol translation, Xbox Live / Floodgate authentication, and automatic custom block/item resource pack mapping.";

    // 2. Floodgate
    public static final String FLOODGATE_PROJECT_NAME = "Floodgate (Bedrock Xbox Live Auth Bridge)";
    public static final String FLOODGATE_AUTHORS = "GeyserMC Team & Contributors";
    public static final String FLOODGATE_LICENSE = "MIT License";
    public static final String FLOODGATE_UPSTREAM_URL = "https://geysermc.org / https://github.com/GeyserMC/Floodgate";
    public static final String FLOODGATE_ROLE =
            "Enables seamless Bedrock Edition authentication without requiring players to own a Java Edition account.";

    // 3. spark
    public static final String SPARK_PROJECT_NAME = "spark (Performance Profiler)";
    public static final String SPARK_AUTHORS = "Lucko (lucko) & Contributors";
    public static final String SPARK_LICENSE = "GPL-3.0 (Standalone Profiler Agent/Plugin Integration)";
    public static final String SPARK_UPSTREAM_URL = "https://spark.lucko.me / https://github.com/lucko/spark";
    public static final String SPARK_ROLE =
            "Provides production-ready async CPU sampling, MSPT tick duration histograms, GC pressure monitoring, and heap summary diagnostics alongside VESSEL's internal microkernel telemetry.";

    // 4. Sodium / Embeddium
    public static final String SODIUM_PROJECT_NAME = "Sodium / Embeddium";
    public static final String SODIUM_AUTHORS = "CaffeineMC (JellySquid / IMS21) & Contributors";
    public static final String SODIUM_LICENSE = "LGPL-3.0";
    public static final String SODIUM_UPSTREAM_URL = "https://github.com/CaffeineMC/sodium-fabric";
    public static final String SODIUM_ROLE =
            "Pioneered high-performance chunk meshing, compact 16-bit block neighbor index table (CompactBlockNeighborTable), "
                    + "fast chunk section palette serialization (FastChunkSectionPalette), and BFS fluid propagation algorithms adapted in VESSEL's ultra-optimize kernel pipeline.";

    // 5. Lithium / Radium
    public static final String LITHIUM_PROJECT_NAME = "Lithium / Radium";
    public static final String LITHIUM_AUTHORS = "CaffeineMC (JellySquid / 2No2Name) & Contributors";
    public static final String LITHIUM_LICENSE = "LGPL-3.0";
    public static final String LITHIUM_UPSTREAM_URL = "https://github.com/CaffeineMC/lithium-fabric";
    public static final String LITHIUM_ROLE =
            "Architected spatial entity hashing (SpatialEntityHashGrid), priority block tick scheduling (FastBlockTickPriorityQueue), "
                    + "entity activation range throttling (EntityActivationRangeGovernor), dropped item/XP orb batch merging, and light suppression techniques foundational to VESSEL's tick engine.";

    // 6. FerriteCore
    public static final String FERRITECORE_PROJECT_NAME = "FerriteCore";
    public static final String FERRITECORE_AUTHORS = "malte0811 & Contributors";
    public static final String FERRITECORE_LICENSE = "MIT License";
    public static final String FERRITECORE_UPSTREAM_URL = "https://github.com/malte0811/FerriteCore";
    public static final String FERRITECORE_ROLE =
            "Innovated block state property deduplication (CompactPropertyStorage), compact integer sets (FastFastutilIntSet), "
                    + "and block model geometry memory compaction, powering VESSEL's sub-gigabyte memory footprint.";

    // 7. ModernFix
    public static final String MODERNFIX_PROJECT_NAME = "ModernFix";
    public static final String MODERNFIX_AUTHORS = "embeddedt & Contributors";
    public static final String MODERNFIX_LICENSE = "LGPL-3.0";
    public static final String MODERNFIX_UPSTREAM_URL = "https://github.com/embeddedt/ModernFix";
    public static final String MODERNFIX_ROLE =
            "Conceived on-demand DFU bootstrap (LazyDfuAndDynamicResourceOptimizer), direct ByteBuf and event listener leak guards "
                    + "(DirectByteBufAndEventBusLeakGuard), and structure location adaptive backoff algorithms incorporated into VESSEL.";

    // 8. C2ME
    public static final String C2ME_PROJECT_NAME = "C2ME (Concurrent Chunk Management Engine)";
    public static final String C2ME_AUTHORS = "RelativityMC (ishland) & Contributors";
    public static final String C2ME_LICENSE = "MIT License";
    public static final String C2ME_UPSTREAM_URL = "https://github.com/RelativityMC/C2ME-fabric";
    public static final String C2ME_ROLE =
            "Advanced asynchronous multi-threaded chunk loading, linear region file zero-allocation I/O (LinearRegionFileFastPath), "
                    + "and non-blocking chunk ticket scheduler (ChunkTicketAdaptiveThrottle).";

    // 9. Krypton
    public static final String KRYPTON_PROJECT_NAME = "Krypton";
    public static final String KRYPTON_AUTHORS = "Andrew Steinborn (TuxDidier) & Contributors";
    public static final String KRYPTON_LICENSE = "LGPL-3.0";
    public static final String KRYPTON_UPSTREAM_URL = "https://github.com/astei/krypton";
    public static final String KRYPTON_ROLE =
            "Pioneered zero-copy packet framing (ZeroCopyPacketFrame), Netty channel write consolidation ring buffer "
                    + "(PacketCoalescingRingBuffer), and microkernel network pipeline bypass (VesselNettyChannelInterceptor).";

    // 10. LazyDFU
    public static final String LAZYDFU_PROJECT_NAME = "LazyDFU";
    public static final String LAZYDFU_AUTHORS = "astei & Contributors";
    public static final String LAZYDFU_LICENSE = "MIT License";
    public static final String LAZYDFU_UPSTREAM_URL = "https://github.com/astei/lazydfu";
    public static final String LAZYDFU_ROLE =
            "Eliminated server startup freeze by deferring Mojang DataFixerUpper (DFU) rules and compound schema compilation until required.";

    // 11. FastSuite
    public static final String FASTSUITE_PROJECT_NAME = "FastSuite";
    public static final String FASTSUITE_AUTHORS = "Shadows-of-Fire / Placebo & Contributors";
    public static final String FASTSUITE_LICENSE = "MIT License";
    public static final String FASTSUITE_UPSTREAM_URL = "https://github.com/Shadows-of-Fire/FastSuite";
    public static final String FASTSUITE_ROLE =
            "Provided indexed recipe lookup and item transfer backoff algorithms utilized in VESSEL cross-chunk hopper and pipe networks.";

    // 12. Ksyxis
    public static final String KSYXIS_PROJECT_NAME = "Ksyxis";
    public static final String KSYXIS_AUTHORS = "VidTu & Contributors";
    public static final String KSYXIS_LICENSE = "MIT License";
    public static final String KSYXIS_UPSTREAM_URL = "https://github.com/VidTu/Ksyxis";
    public static final String KSYXIS_ROLE =
            "Inspired zero-lag instant server launch by deferring synchronous spawn chunk generation until first player login.";

    // 13. PaperMC & Folia
    public static final String PAPER_FOLIA_PROJECT_NAME = "PaperMC & Folia";
    public static final String PAPER_FOLIA_AUTHORS = "PaperMC Team & Contributors";
    public static final String PAPER_FOLIA_LICENSE = "GPL-3.0";
    public static final String PAPER_FOLIA_UPSTREAM_URL = "https://papermc.io / https://github.com/PaperMC/Paper / https://github.com/PaperMC/Folia";
    public static final String PAPER_FOLIA_ROLE =
            "Architected regionized multithreaded tick strands (RegionalActorStrand, GlobalRegionActorStrand), "
                    + "watchdog freeze avoidance (max-tick-time=-1), and production-grade restart automation.";

    // 14. Valkyrien Skies
    public static final String VALKYRIEN_SKIES_PROJECT_NAME = "Valkyrien Skies";
    public static final String VALKYRIEN_SKIES_AUTHORS = "Valkyrien Skies Team & Contributors";
    public static final String VALKYRIEN_SKIES_LICENSE = "MIT License";
    public static final String VALKYRIEN_SKIES_UPSTREAM_URL = "https://valkyrienskies.org / https://github.com/ValkyrienSkies/Valkyrien-Skies-2";
    public static final String VALKYRIEN_SKIES_ROLE =
            "Conceived rigid-body ship physics strand boundaries (ValkyrienPhysicsStrandBarrier) and transform collision "
                    + "governors (ValkyrienPhysicsAndVoxelShipCollisionGovernor) integrated into VESSEL's dynamic physics engine.";

    // 15. Minecraft Dedicated Server
    public static final String MINECRAFT_PROJECT_NAME = "Minecraft Dedicated Server (Mojang Studios)";
    public static final String MINECRAFT_AUTHORS = "Mojang AB / Microsoft Corporation";
    public static final String MINECRAFT_LICENSE = "Mojang EULA (https://www.minecraft.net/en-us/eula)";
    public static final String MINECRAFT_UPSTREAM_URL = "https://www.minecraft.net";
    public static final String MINECRAFT_ROLE =
            "Core vanilla game mechanics, world generation, and official server protocol foundation.";

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
     * Returns standard attribution notice for Sodium / Embeddium.
     *
     * @return attribution notice string
     */
    public String getSodiumAttributionNotice() {
        return SODIUM_PROJECT_NAME + " — Copyright (c) 2020-2026 " + SODIUM_AUTHORS
                + " (" + SODIUM_LICENSE + ") — " + SODIUM_UPSTREAM_URL;
    }

    /**
     * Returns standard attribution notice for Lithium / Radium.
     *
     * @return attribution notice string
     */
    public String getLithiumAttributionNotice() {
        return LITHIUM_PROJECT_NAME + " — Copyright (c) 2020-2026 " + LITHIUM_AUTHORS
                + " (" + LITHIUM_LICENSE + ") — " + LITHIUM_UPSTREAM_URL;
    }

    /**
     * Returns standard attribution notice for FerriteCore.
     *
     * @return attribution notice string
     */
    public String getFerriteCoreAttributionNotice() {
        return FERRITECORE_PROJECT_NAME + " — Copyright (c) 2020-2026 " + FERRITECORE_AUTHORS
                + " (" + FERRITECORE_LICENSE + ") — " + FERRITECORE_UPSTREAM_URL;
    }

    /**
     * Returns standard attribution notice for ModernFix.
     *
     * @return attribution notice string
     */
    public String getModernFixAttributionNotice() {
        return MODERNFIX_PROJECT_NAME + " — Copyright (c) 2022-2026 " + MODERNFIX_AUTHORS
                + " (" + MODERNFIX_LICENSE + ") — " + MODERNFIX_UPSTREAM_URL;
    }

    /**
     * Returns standard attribution notice for C2ME.
     *
     * @return attribution notice string
     */
    public String getC2meAttributionNotice() {
        return C2ME_PROJECT_NAME + " — Copyright (c) 2021-2026 " + C2ME_AUTHORS
                + " (" + C2ME_LICENSE + ") — " + C2ME_UPSTREAM_URL;
    }

    /**
     * Returns standard attribution notice for Krypton.
     *
     * @return attribution notice string
     */
    public String getKryptonAttributionNotice() {
        return KRYPTON_PROJECT_NAME + " — Copyright (c) 2020-2026 " + KRYPTON_AUTHORS
                + " (" + KRYPTON_LICENSE + ") — " + KRYPTON_UPSTREAM_URL;
    }

    /**
     * Returns standard attribution notice for LazyDFU.
     *
     * @return attribution notice string
     */
    public String getLazyDfuAttributionNotice() {
        return LAZYDFU_PROJECT_NAME + " — Copyright (c) 2021-2026 " + LAZYDFU_AUTHORS
                + " (" + LAZYDFU_LICENSE + ") — " + LAZYDFU_UPSTREAM_URL;
    }

    /**
     * Returns standard attribution notice for FastSuite.
     *
     * @return attribution notice string
     */
    public String getFastSuiteAttributionNotice() {
        return FASTSUITE_PROJECT_NAME + " — Copyright (c) 2020-2026 " + FASTSUITE_AUTHORS
                + " (" + FASTSUITE_LICENSE + ") — " + FASTSUITE_UPSTREAM_URL;
    }

    /**
     * Returns standard attribution notice for Ksyxis.
     *
     * @return attribution notice string
     */
    public String getKsyxisAttributionNotice() {
        return KSYXIS_PROJECT_NAME + " — Copyright (c) 2022-2026 " + KSYXIS_AUTHORS
                + " (" + KSYXIS_LICENSE + ") — " + KSYXIS_UPSTREAM_URL;
    }

    /**
     * Returns standard attribution notice for PaperMC &amp; Folia.
     *
     * @return attribution notice string
     */
    public String getPaperFoliaAttributionNotice() {
        return PAPER_FOLIA_PROJECT_NAME + " — Copyright (c) 2016-2026 " + PAPER_FOLIA_AUTHORS
                + " (" + PAPER_FOLIA_LICENSE + ") — " + PAPER_FOLIA_UPSTREAM_URL;
    }

    /**
     * Returns standard attribution notice for Valkyrien Skies.
     *
     * @return attribution notice string
     */
    public String getValkyrienSkiesAttributionNotice() {
        return VALKYRIEN_SKIES_PROJECT_NAME + " — Copyright (c) 2016-2026 " + VALKYRIEN_SKIES_AUTHORS
                + " (" + VALKYRIEN_SKIES_LICENSE + ") — " + VALKYRIEN_SKIES_UPSTREAM_URL;
    }

    /**
     * Generates the complete {@code CREDITS.md} markdown document crediting GeyserMC, Floodgate,
     * Spark, Mojang Studios, and all 11 community performance and optimization projects.
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
     * Spark, Mojang Studios, and all 11 community performance and optimization projects.
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

                VESSEL is built upon the collective wisdom, breakthroughs, and architectural patterns of the open-source Minecraft server, modding, and performance communities.

                This document provides full, explicit attribution, copyright notices, upstream repositories, and licenses for all third-party ecosystems and lag-reduction / optimization algorithms bundled or adapted in VESSEL for **Minecraft %s**.

                ---

                ## Section I: Bundled Ecosystem Integrations (Auto-Provisioned)

                VESSEL automatically fetches and maintains the latest releases of these companion projects via official APIs:

                ### 1. GeyserMC (`Geyser` & `Floodgate`)
                - **Integrated Geyser Version:** `%s` (Auto-updated to latest release on every VESSEL update)
                - **Integrated Floodgate Version:** `%s` (Auto-updated to latest release on every VESSEL update)
                - **Authors:** %s
                - **License:** %s
                - **Official Website & Repositories:** %s | %s
                - **Role in VESSEL:** %s

                ### 2. spark (`lucko/spark` Performance Profiler)
                - **Integrated Version:** `%s` (Auto-updated to latest release on every VESSEL update)
                - **Authors:** %s
                - **License:** %s
                - **Official Website & Repository:** %s
                - **Role in VESSEL:** %s

                ---

                ## Section II: Server Engine, Multi-Threading & Lag-Reduction Architectures

                VESSEL's 40 microkernel optimization engines incorporate, adapt, and build upon algorithms, data structures, and optimization paradigms pioneered by the following upstream projects:

                ### 3. Sodium / Embeddium
                - **Project:** %s
                - **Authors:** %s
                - **License:** %s
                - **Upstream Repository:** %s
                - **Role in VESSEL:** %s

                ### 4. Lithium / Radium
                - **Project:** %s
                - **Authors:** %s
                - **License:** %s
                - **Upstream Repository:** %s
                - **Role in VESSEL:** %s

                ### 5. FerriteCore
                - **Project:** %s
                - **Authors:** %s
                - **License:** %s
                - **Upstream Repository:** %s
                - **Role in VESSEL:** %s

                ### 6. ModernFix
                - **Project:** %s
                - **Authors:** %s
                - **License:** %s
                - **Upstream Repository:** %s
                - **Role in VESSEL:** %s

                ### 7. C2ME (Concurrent Chunk Management Engine)
                - **Project:** %s
                - **Authors:** %s
                - **License:** %s
                - **Upstream Repository:** %s
                - **Role in VESSEL:** %s

                ### 8. Krypton
                - **Project:** %s
                - **Authors:** %s
                - **License:** %s
                - **Upstream Repository:** %s
                - **Role in VESSEL:** %s

                ### 9. LazyDFU
                - **Project:** %s
                - **Authors:** %s
                - **License:** %s
                - **Upstream Repository:** %s
                - **Role in VESSEL:** %s

                ### 10. FastSuite
                - **Project:** %s
                - **Authors:** %s
                - **License:** %s
                - **Upstream Repository:** %s
                - **Role in VESSEL:** %s

                ### 11. Ksyxis
                - **Project:** %s
                - **Authors:** %s
                - **License:** %s
                - **Upstream Repository:** %s
                - **Role in VESSEL:** %s

                ### 12. PaperMC & Folia
                - **Project:** %s
                - **Authors:** %s
                - **License:** %s
                - **Upstream Repositories:** %s
                - **Role in VESSEL:** %s

                ### 13. Valkyrien Skies
                - **Project:** %s
                - **Authors:** %s
                - **License:** %s
                - **Upstream Repository:** %s
                - **Role in VESSEL:** %s

                ---

                ## Section III: Vanilla Game Platform

                ### 14. Minecraft Dedicated Server (`Mojang Studios`)
                - **Target Default Release:** `%s` (`latest.release` via Mojang Piston Meta API)
                - **Authors:** %s
                - **License:** %s
                - **Official Website:** %s
                - **Role in VESSEL:** %s
                """.stripIndent().formatted(
                minecraftVersion,
                geyserVersion,
                floodgateVersion,
                GEYSER_AUTHORS,
                GEYSER_LICENSE,
                GEYSER_UPSTREAM_URL,
                FLOODGATE_UPSTREAM_URL,
                GEYSER_ROLE,
                sparkVersion,
                SPARK_AUTHORS,
                SPARK_LICENSE,
                SPARK_UPSTREAM_URL,
                SPARK_ROLE,
                SODIUM_PROJECT_NAME,
                SODIUM_AUTHORS,
                SODIUM_LICENSE,
                SODIUM_UPSTREAM_URL,
                SODIUM_ROLE,
                LITHIUM_PROJECT_NAME,
                LITHIUM_AUTHORS,
                LITHIUM_LICENSE,
                LITHIUM_UPSTREAM_URL,
                LITHIUM_ROLE,
                FERRITECORE_PROJECT_NAME,
                FERRITECORE_AUTHORS,
                FERRITECORE_LICENSE,
                FERRITECORE_UPSTREAM_URL,
                FERRITECORE_ROLE,
                MODERNFIX_PROJECT_NAME,
                MODERNFIX_AUTHORS,
                MODERNFIX_LICENSE,
                MODERNFIX_UPSTREAM_URL,
                MODERNFIX_ROLE,
                C2ME_PROJECT_NAME,
                C2ME_AUTHORS,
                C2ME_LICENSE,
                C2ME_UPSTREAM_URL,
                C2ME_ROLE,
                KRYPTON_PROJECT_NAME,
                KRYPTON_AUTHORS,
                KRYPTON_LICENSE,
                KRYPTON_UPSTREAM_URL,
                KRYPTON_ROLE,
                LAZYDFU_PROJECT_NAME,
                LAZYDFU_AUTHORS,
                LAZYDFU_LICENSE,
                LAZYDFU_UPSTREAM_URL,
                LAZYDFU_ROLE,
                FASTSUITE_PROJECT_NAME,
                FASTSUITE_AUTHORS,
                FASTSUITE_LICENSE,
                FASTSUITE_UPSTREAM_URL,
                FASTSUITE_ROLE,
                KSYXIS_PROJECT_NAME,
                KSYXIS_AUTHORS,
                KSYXIS_LICENSE,
                KSYXIS_UPSTREAM_URL,
                KSYXIS_ROLE,
                PAPER_FOLIA_PROJECT_NAME,
                PAPER_FOLIA_AUTHORS,
                PAPER_FOLIA_LICENSE,
                PAPER_FOLIA_UPSTREAM_URL,
                PAPER_FOLIA_ROLE,
                VALKYRIEN_SKIES_PROJECT_NAME,
                VALKYRIEN_SKIES_AUTHORS,
                VALKYRIEN_SKIES_LICENSE,
                VALKYRIEN_SKIES_UPSTREAM_URL,
                VALKYRIEN_SKIES_ROLE,
                minecraftVersion,
                MINECRAFT_AUTHORS,
                MINECRAFT_LICENSE,
                MINECRAFT_UPSTREAM_URL,
                MINECRAFT_ROLE);
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
