package dev.vessel.bootstrap.target;

import dev.vessel.bootstrap.ecosystem.BuiltInGeyserAndSparkProvisioner;
import dev.vessel.bootstrap.ecosystem.BuiltInGeyserBedrockBridgeService;
import dev.vessel.bootstrap.mojang.JvmVersionCompatibilityGuard;
import dev.vessel.bootstrap.mojang.MojangBundlerExtractor;
import dev.vessel.bootstrap.mojang.MojangServerJarDownloader;
import dev.vessel.bootstrap.mojang.MojangVersionManifestResolver;
import dev.vessel.bootstrap.mojang.RealDirectoryModAndPluginScanner;
import dev.vessel.bootstrap.mojang.ServerEulaAndPropertiesBootstrap;
import dev.vessel.bootstrap.mojang.SinglePassModAndPluginLiveActivator;
import dev.vessel.bootstrap.mojang.TierOneVanillaGameLauncher;
import dev.vessel.bootstrap.mojang.VesselCustomFlagsProfile;
import dev.vessel.bootstrap.updater.StagedKernelPatchManager;
import dev.vessel.bootstrap.updater.VesselBuildVersionCode;
import dev.vessel.command.VesselBrigadierCommandBridge;
import dev.vessel.command.VesselRootCommand;
import dev.vessel.command.VesselServerRestartService;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Sovereign standalone server entrypoint ({@code Main-Class}) orchestrating the canonical 16-step
 * VESSEL boot sequence in a single continuous pass (zero restart required on first run), emitting
 * structured installer/runtime/debug console logs, verifying host JVM compatibility against both
 * VESSEL's minimum requirement (Java 21 LTS) and the target Minecraft server's bytecode version,
 * provisioning built-in GeyserMC, Floodgate &amp; spark with full credits, upgrading lower-version
 * mods and plugins in memory, activating all entrypoints, intercepting live {@code /vessel},
 * {@code /geyser}, {@code /floodgate}, and {@code /spark} console commands, and dynamically
 * launching any Minecraft server release (from legacy monolithic 1.12.2 / 1.16.5 JARs to modern
 * 1.18–1.21.11 / 26.3+ bundler JARs).
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.13
 * @since 1.0.0 (Pass K-29)
 */
public final class VesselServerLauncher {

    private final Path serverRoot;
    private final dev.vessel.command.VesselBukkitCommandBridge bukkitCommandBridge =
            new dev.vessel.command.VesselBukkitCommandBridge();
    private volatile int completedBootSteps;
    private volatile String resolvedActiveMcVersion = MojangVersionManifestResolver.FALLBACK_LATEST_RELEASE;
    private volatile Map<Path, VesselModuleIdentifier> discoveredModules = Map.of();
    private volatile int activatedEntrypointsCount;
    private volatile boolean vanillaMainInvoked;
    private volatile boolean stagedPatchAppliedOnBoot;
    private volatile boolean jvmVersionMismatchHalted;
    private volatile URLClassLoader activeTierOneClassLoader;
    private volatile VesselStartupConsoleLogger activeConsoleLogger;
    private volatile VesselCustomFlagsProfile activeCustomFlags;
    private volatile BuiltInGeyserBedrockBridgeService activeGeyserBridgeService;
    private volatile VesselResourcePackDeliveryService activeResourcePackService;
    private volatile VesselBrigadierCommandBridge activeBrigadierBridge;
    private volatile List<String> lastBootConsoleLogs = List.of();
    private volatile String activeVesselVersionCode =
            VesselBuildVersionCode.CURRENT_EMBEDDED_VERSION_CODE;

    public VesselServerLauncher(Path serverRoot) {
        this.serverRoot = Objects.requireNonNull(serverRoot, "serverRoot cannot be null");
        this.bukkitCommandBridge.initialize(this.serverRoot);
    }

    public VesselServerLauncher() {
        this(Path.of("."));
    }

    /**
     * Returns the server root path.
     *
     * @return server root path
     */
    public Path getServerRoot() {
        return serverRoot;
    }

    /**
     * JVM main entrypoint for {@code server.jar}. Automatically enables live Mojang Piston Meta API
     * resolution if not overridden via CLI flags.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        // Force IPv4 stack before any JVM networking class initializes so Mojang's authlib
        // (https://api.minecraftservices.com/publickeys) never stalls on broken IPv6 routes.
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
        String[] safeArgs = args != null ? args : new String[0];
        boolean hasManifestFlag = false;
        for (String arg : safeArgs) {
            if (arg != null && arg.startsWith("--vessel-manifest-url")) {
                hasManifestFlag = true;
                break;
            }
        }
        String[] effectiveArgs = safeArgs;
        if (!hasManifestFlag) {
            effectiveArgs = new String[safeArgs.length + 1];
            System.arraycopy(safeArgs, 0, effectiveArgs, 0, safeArgs.length);
            effectiveArgs[safeArgs.length] =
                    "--vessel-manifest-url=" + MojangVersionManifestResolver.DEFAULT_MANIFEST_URL;
        }
        VesselServerLauncher launcher = new VesselServerLauncher();
        launcher.executeBootSequence(effectiveArgs);
    }

    /**
     * Executes the 16-step startup boot sequence. By default ({@code --vessel-single-pass-boot}),
     * first-run scaffolding and full server boot complete in a single invocation without requiring
     * a restart.
     *
     * @param args command-line arguments
     * @return process exit/status code ({@code 0} on clean startup, {@code 1} on JVM version mismatch)
     */
    public int executeBootSequence(String[] args) {
        Objects.requireNonNull(args, "args cannot be null");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
        long bootStartMillis = System.currentTimeMillis();
        completedBootSteps = 0;
        jvmVersionMismatchHalted = false;

        // Step 0: Apply any pending staged kernel patches from a previous live hourly auto-update
        StagedKernelPatchManager patchManager = new StagedKernelPatchManager();
        stagedPatchAppliedOnBoot = patchManager.applyPendingStagedPatchesOnStartup(serverRoot);
        activeVesselVersionCode = patchManager.readActiveVersionCode(serverRoot);

        // Step 1: Parse CLI flags (--vessel-version, --nogui, --vessel-deep-debug, --vessel-ultra-optimize)
        VesselCliFlagsParser flagsParser = new VesselCliFlagsParser();
        flagsParser.parseArguments(args);
        VesselCustomFlagsProfile customFlags = flagsParser.getCustomFlagsProfile();
        activeCustomFlags = customFlags;
        VesselStartupConsoleLogger consoleLogger =
                VesselStartupConsoleLogger.fromFlagsProfile(customFlags);
        activeConsoleLogger = consoleLogger;
        JvmVersionCompatibilityGuard jvmGuard = new JvmVersionCompatibilityGuard();
        int runningJavaVer = jvmGuard.detectRunningJavaFeatureVersion();
        boolean isFirstRunInstall = !Files.isRegularFile(
                serverRoot.resolve(StartupBuildStageOrchestrator.FIRST_RUN_SENTINEL_FILE));

        consoleLogger.logStartupBanner(
                activeVesselVersionCode,
                runningJavaVer,
                isFirstRunInstall,
                customFlags.isUltraOptimizationEnabled());
        if (stagedPatchAppliedOnBoot) {
            consoleLogger.logInfo("Applied pending staged kernel patch -> active build: " + activeVesselVersionCode);
        }
        consoleLogger.logDebug("CLI Flags Profile: singlePassBoot=" + customFlags.isSinglePassBootEnabled()
                + ", universalCompat=" + customFlags.isUniversalCompatEnabled()
                + ", autoLatest=" + customFlags.isAutoLatestVersionEnabled()
                + ", ultraOptimize=" + customFlags.isUltraOptimizationEnabled()
                + ", deepDebug=" + customFlags.isDeepDebugEnabled());
        completedBootSteps = 1;

        // Step 2: Scaffold directories & check first-run sentinel (Single-Pass by default!)
        if (isFirstRunInstall) {
            consoleLogger.logInstaller("First-run detected. Scaffolding VESSEL server directories in: "
                    + serverRoot.toAbsolutePath().normalize());
        }
        VesselFirstRunScaffolder scaffolder = new VesselFirstRunScaffolder();
        scaffolder.scaffoldServerDirectories(serverRoot);
        if (isFirstRunInstall) {
            consoleLogger.logInstaller("Created directory hierarchy: mods/, plugins/, vessel-config/, cache/");
        } else {
            consoleLogger.logInfo("Verified server workspace directories (mods/, plugins/, vessel-config/, cache/)");
        }

        StartupBuildStageOrchestrator buildOrchestrator = new StartupBuildStageOrchestrator();
        List<String> scopedVersions = flagsParser.getScopedVesselVersions();
        boolean sentinelProceed = buildOrchestrator.checkFirstRunSentinelOrScheduleExit(serverRoot, scopedVersions);
        completedBootSteps = 2;
        if (!sentinelProceed && !customFlags.isSinglePassBootEnabled()) {
            consoleLogger.logInstaller("First-run scaffolding finished (single-pass boot disabled via flag).");
            lastBootConsoleLogs = consoleLogger.getRecordedLines();
            return 0;
        }

        // Step 3: Resolve target Minecraft version (always latest.release if not pinned)
        Path cacheDir = serverRoot.resolve("cache");
        MojangVersionManifestResolver manifestResolver = new MojangVersionManifestResolver();
        String manifestUrl = flagsParser.getFlagValueOrDefault(
                "vessel-manifest-url",
                "offline://" + MojangVersionManifestResolver.CACHED_MANIFEST_FILE);
        if (isFirstRunInstall) {
            consoleLogger.logInstaller("Querying Mojang version manifest (" + manifestUrl + ")...");
        }
        String manifestJson = manifestResolver.fetchOrLoadCachedVersionManifest(cacheDir, manifestUrl);
        if (!scopedVersions.isEmpty()) {
            resolvedActiveMcVersion = scopedVersions.get(0);
            consoleLogger.logInfo("Target Minecraft version pinned via CLI flag: " + resolvedActiveMcVersion);
        } else {
            resolvedActiveMcVersion = manifestResolver.resolveLatestReleaseVersion(manifestJson);
            consoleLogger.logInfo("Resolved latest official Minecraft release: " + resolvedActiveMcVersion);
        }
        System.setProperty("vessel.active.mc.version", resolvedActiveMcVersion);

        // Step 4–10: Bootstrap EULA/server.properties, provision GeyserMC, Floodgate & spark + CREDITS.md, scan mods/ & plugins/
        ServerEulaAndPropertiesBootstrap eulaBootstrap = new ServerEulaAndPropertiesBootstrap();
        int serverPort = Integer.parseInt(flagsParser.getFlagValueOrDefault("port", "25565"));
        eulaBootstrap.ensureEulaAccepted(
                serverRoot,
                customFlags.isAcceptEulaEnabled() || customFlags.isSinglePassBootEnabled());
        eulaBootstrap.ensureServerProperties(serverRoot, serverPort);
        if (isFirstRunInstall) {
            consoleLogger.logInstaller("Generated eula.txt (eula=true) and server.properties (server-port=" + serverPort + ")");
            consoleLogger.logInstaller("Provisioning built-in GeyserMC (Bedrock Bridge), Floodgate & spark (Profiler) into cache/ecosystem/...");
        }

        boolean isOnlineManifest = manifestUrl.startsWith("http://") || manifestUrl.startsWith("https://");
        String geyserApiEndpoint = flagsParser.getFlagValueOrDefault(
                "vessel-geyser-api-url",
                isOnlineManifest ? BuiltInGeyserAndSparkProvisioner.GEYSER_MODRINTH_API_URL : "offline://geyser");
        String floodgateApiEndpoint = flagsParser.getFlagValueOrDefault(
                "vessel-floodgate-api-url",
                isOnlineManifest ? BuiltInGeyserAndSparkProvisioner.FLOODGATE_MODRINTH_API_URL : "offline://floodgate");
        String sparkApiEndpoint = flagsParser.getFlagValueOrDefault(
                "vessel-spark-api-url",
                isOnlineManifest ? BuiltInGeyserAndSparkProvisioner.SPARK_LATEST_API_URL : "offline://spark");

        BuiltInGeyserAndSparkProvisioner ecosystemProvisioner =
                new BuiltInGeyserAndSparkProvisioner();
        ecosystemProvisioner.provisionAndSyncLatestEcosystem(
                serverRoot,
                resolvedActiveMcVersion,
                geyserApiEndpoint,
                floodgateApiEndpoint,
                sparkApiEndpoint);
        System.setProperty("vessel.active.geyser.version", ecosystemProvisioner.getActiveGeyserVersion());
        System.setProperty("vessel.active.floodgate.version", ecosystemProvisioner.getActiveFloodgateVersion());
        System.setProperty("vessel.active.spark.version", ecosystemProvisioner.getActiveSparkVersion());
        consoleLogger.logInfo("Built-in Ecosystem ready: GeyserMC ("
                + ecosystemProvisioner.getActiveGeyserVersion()
                + ") + Floodgate (" + ecosystemProvisioner.getActiveFloodgateVersion()
                + ") + spark (" + ecosystemProvisioner.getActiveSparkVersion()
                + ") | Credits written to CREDITS.md");

        buildOrchestrator.purgeInterruptedPartialBuilds(cacheDir.resolve("builds"));
        LazyDfuAndDynamicResourceOptimizer dfuOptimizer = new LazyDfuAndDynamicResourceOptimizer();
        dfuOptimizer.deferDfuSchemaCompilation();

        RealDirectoryModAndPluginScanner scanner = new RealDirectoryModAndPluginScanner();
        discoveredModules = scanner.scanAllModAndPluginJars(serverRoot);
        if (isFirstRunInstall || !discoveredModules.isEmpty()) {
            consoleLogger.logInfo("Scanning mods/ and plugins/ -> Discovered "
                    + discoveredModules.size() + " mod/plugin JAR(s)");
        }
        for (Map.Entry<Path, VesselModuleIdentifier> entry : discoveredModules.entrySet()) {
            consoleLogger.logDebug("  -> Module JAR: " + entry.getKey().getFileName()
                    + " | Identity: " + entry.getValue().namespace()
                    + " v" + entry.getValue().version()
                    + " [" + entry.getValue().loader() + "]");
        }

        SinglePassModAndPluginLiveActivator liveActivator = new SinglePassModAndPluginLiveActivator();
        activatedEntrypointsCount = liveActivator.activateAllDiscoveredModsAndPlugins(
                serverRoot,
                resolvedActiveMcVersion,
                discoveredModules,
                customFlags,
                VesselServerLauncher.class.getClassLoader());
        consoleLogger.logInfo("Single-pass module activation complete: "
                + activatedEntrypointsCount + " entrypoint(s) activated across "
                + discoveredModules.size() + " module(s)");
        completedBootSteps = 10;

        // Step 11: Startup Build Stage — eager render bundle compilation & HTTP streaming before world load
        buildOrchestrator.buildRenderBundleForCombination(
                resolvedActiveMcVersion,
                SourceLoaderKind.FABRIC,
                "VESSEL_DEFAULT_BUNDLE".getBytes(StandardCharsets.UTF_8)
        );
        consoleLogger.logDebug("Compiled default Universal Client Presentation render bundle for Minecraft "
                + resolvedActiveMcVersion);

        VesselResourcePackDeliveryService packService = new VesselResourcePackDeliveryService();
        activeResourcePackService = packService;
        String packUrl = packService.startDeliveryServerAndConfigureProperties(serverRoot, serverPort);
        if (!packUrl.isBlank()) {
            consoleLogger.logInfo("Universal Presentation HTTP Server active on " + packUrl
                    + " (SHA-1: " + packService.getActivePackSha1() + ")");
        }
        completedBootSteps = 11;

        // Steps 12–16: Download & unpack Mojang server JAR dynamically, verify JVM version, and start server
        vanillaMainInvoked = false;
        String downloadUrl = flagsParser.getFlagValueOrDefault("vessel-mojang-jar-url", "");
        String expectedSha1 = "";
        Path bundlerJar = cacheDir.resolve("mojang_" + resolvedActiveMcVersion + ".jar");
        boolean bundlerAlreadyCached = Files.isRegularFile(bundlerJar);
        MojangServerJarDownloader downloader = new MojangServerJarDownloader();
        if (downloadUrl.isBlank()
                && !bundlerAlreadyCached
                && (manifestUrl.startsWith("http://") || manifestUrl.startsWith("https://"))) {
            consoleLogger.logInstaller("Resolving official Mojang server download URL for Minecraft "
                    + resolvedActiveMcVersion + "...");
            String versionMetaUrl =
                    manifestResolver.extractVersionMetadataUrl(manifestJson, resolvedActiveMcVersion);
            if (!versionMetaUrl.isBlank()) {
                String versionMetaJson = manifestResolver.fetchOrLoadVersionDetailJson(
                        cacheDir.resolve("meta"), resolvedActiveMcVersion, versionMetaUrl);
                downloadUrl = downloader.extractServerDownloadUrl(versionMetaJson);
                expectedSha1 = downloader.extractServerDownloadSha1(versionMetaJson);
            } else {
                consoleLogger.logWarn("Minecraft version '" + resolvedActiveMcVersion
                        + "' was not found in Mojang version_manifest_v2.json.");
            }
        }
        if (!downloadUrl.isBlank()) {
            if (!bundlerAlreadyCached) {
                consoleLogger.logInstaller("Downloading official Mojang server JAR ("
                        + downloadUrl + ") -> cache/mojang_" + resolvedActiveMcVersion + ".jar ...");
            }
            bundlerJar = downloader.ensureVanillaServerJarCached(
                    cacheDir, resolvedActiveMcVersion, downloadUrl, expectedSha1);
            if (!bundlerAlreadyCached && Files.isRegularFile(bundlerJar)) {
                consoleLogger.logInstaller("Successfully downloaded and verified cache/mojang_"
                        + resolvedActiveMcVersion + ".jar");
            }
        }
        if (Files.isRegularFile(bundlerJar)) {
            MojangBundlerExtractor extractor = new MojangBundlerExtractor();
            boolean isModernBundler = extractor.isBundlerArchive(bundlerJar);
            Path bundlerExtractDir = cacheDir.resolve("bundler").resolve(resolvedActiveMcVersion);
            boolean alreadyUnpacked = !isModernBundler
                    || Files.isDirectory(bundlerExtractDir.resolve("versions"));
            if (!alreadyUnpacked) {
                consoleLogger.logInstaller("Unpacking Mojang bundler JAR (libraries & server-"
                        + resolvedActiveMcVersion + ".jar) into " + bundlerExtractDir + "...");
            }
            List<Path> extractedClasspath = extractor.extractBundlerJar(bundlerJar, bundlerExtractDir);
            if (!alreadyUnpacked) {
                consoleLogger.logInstaller("Unpacked " + extractedClasspath.size()
                        + " server & library JARs for Minecraft " + resolvedActiveMcVersion);
            } else if (isModernBundler) {
                consoleLogger.logInfo("Verified cached Minecraft " + resolvedActiveMcVersion
                        + " bundler (" + extractedClasspath.size() + " classpath JARs ready)");
            } else {
                consoleLogger.logInfo("Verified cached Minecraft " + resolvedActiveMcVersion
                        + " standalone server JAR (" + bundlerJar.getFileName() + ")");
            }
            if (!extractedClasspath.isEmpty()) {
                String mainClassName = extractor.readBundlerMainClass(bundlerJar);
                int requiredClassMajor = jvmGuard.detectRequiredClassFileMajorVersion(
                        extractedClasspath, mainClassName);
                int requiredJavaVer = jvmGuard.classFileMajorToJavaFeatureVersion(requiredClassMajor);
                consoleLogger.logDebug("Detected entrypoint " + mainClassName
                        + " compiled with class file version " + requiredClassMajor
                        + ".0 (Requires Java " + requiredJavaVer + "+)");

                boolean jvmCompatible = jvmGuard.verifyJvmCompatibilityOrLogError(
                        runningJavaVer,
                        requiredClassMajor,
                        resolvedActiveMcVersion,
                        mainClassName,
                        consoleLogger);
                if (!jvmCompatible) {
                    jvmVersionMismatchHalted = true;
                    dfuOptimizer.evictTransientBuildStageModels(1024L * 1024L);
                    completedBootSteps = 16;
                    lastBootConsoleLogs = consoleLogger.getRecordedLines();
                    return 1;
                }

                BuiltInGeyserBedrockBridgeService geyserBridge = new BuiltInGeyserBedrockBridgeService();
                activeGeyserBridgeService = geyserBridge;
                boolean bedrockBridgeStarted = geyserBridge.startGeyserBridgeIfAvailable(
                        serverRoot,
                        serverPort,
                        BuiltInGeyserBedrockBridgeService.DEFAULT_BEDROCK_UDP_PORT);
                if (bedrockBridgeStarted) {
                    consoleLogger.logInfo("GeyserMC Bedrock Edition RakNet Bridge started: UDP 0.0.0.0:"
                            + BuiltInGeyserBedrockBridgeService.DEFAULT_BEDROCK_UDP_PORT
                            + " -> TCP 127.0.0.1:" + serverPort + " (Floodgate auth ready)");
                } else {
                    consoleLogger.logDebug("GeyserMC runtime config scaffolded at cache/ecosystem/geyser-runtime/config.yml (UDP 19132 -> TCP "
                            + serverPort + ")");
                }

                consoleLogger.logInfo("JVM compatibility verified (Java " + runningJavaVer
                        + " >= required Java "
                        + Math.max(JvmVersionCompatibilityGuard.VESSEL_MINIMUM_JAVA_VERSION, requiredJavaVer)
                        + "). Handing off to Minecraft DedicatedServer (" + mainClassName + ")...");
                consoleLogger.logInfo("Sovereign Console Commands active: type 'vessel', 'geyser', 'floodgate', or 'spark' at any time.");
                installConsoleCommandInterceptor();
                TierOneVanillaGameLauncher vanillaLauncher = new TierOneVanillaGameLauncher();
                // Keep activeTierOneClassLoader open for the entire lifetime of the server process
                // so the background Minecraft "Server thread" never loses access to its JARs.
                activeTierOneClassLoader = vanillaLauncher.createTierOneGameClassLoader(
                        extractedClasspath,
                        VesselServerLauncher.class.getClassLoader());
                try {
                    vanillaMainInvoked = vanillaLauncher.invokeMinecraftServerMain(
                            activeTierOneClassLoader,
                            mainClassName,
                            new String[]{"nogui"});
                    VesselBrigadierCommandBridge brigadierBridge = new VesselBrigadierCommandBridge();
                    activeBrigadierBridge = brigadierBridge;
                    brigadierBridge.scheduleBackgroundBrigadierRegistration(activeTierOneClassLoader, this);
                    consoleLogger.logDebug("Scheduled in-game Brigadier command registration for /vessel, /geyser, /floodgate, /spark, /restart");
                } catch (Exception e) {
                    consoleLogger.logError("Failed to launch Minecraft DedicatedServer: " + e.getMessage());
                }
            }
        }
        dfuOptimizer.evictTransientBuildStageModels(1024L * 1024L);
        completedBootSteps = 16;
        long totalBootMs = System.currentTimeMillis() - bootStartMillis;
        consoleLogger.logDebug("VESSEL 16-step boot sequence completed in " + totalBootMs + " ms.");
        lastBootConsoleLogs = consoleLogger.getRecordedLines();
        return 0;
    }

    /**
     * Returns the number of boot sequence steps completed during the most recent launch.
     *
     * @return completed boot step count ({@code 16} on single-pass full boot)
     */
    public int getCompletedBootSteps() {
        return completedBootSteps;
    }

    /**
     * Returns the active Minecraft version resolved during the most recent launch (defaults to
     * Mojang {@code latest.release}).
     *
     * @return the resolved Minecraft version string
     */
    public String getResolvedActiveMcVersion() {
        return resolvedActiveMcVersion;
    }

    /**
     * Returns all physical mod and plugin JARs discovered in {@code mods/} and {@code plugins/}
     * during the most recent launch.
     *
     * @return unmodifiable map of discovered JAR paths to module identifiers
     */
    public Map<Path, VesselModuleIdentifier> getDiscoveredModules() {
        return discoveredModules;
    }

    /**
     * Returns the number of mod and plugin entrypoints activated during the most recent single-pass
     * launch.
     *
     * @return count of activated entrypoints
     */
    public int getActivatedEntrypointsCount() {
        return activatedEntrypointsCount;
    }

    /**
     * Returns {@code true} if the Tier-1 Vanilla {@code Main#main(String[])} entrypoint was
     * invoked during the most recent launch.
     *
     * @return {@code true} if vanilla server main was invoked
     */
    public boolean isVanillaMainInvoked() {
        return vanillaMainInvoked;
    }

    /**
     * Returns {@code true} if a pending staged kernel patch was promoted and applied at Step 0 of
     * the most recent server startup.
     *
     * @return {@code true} if a staged patch was applied on startup
     */
    public boolean wasStagedPatchAppliedOnBoot() {
        return stagedPatchAppliedOnBoot;
    }

    /**
     * Returns {@code true} if the server launch was cleanly halted because the running Java VM is
     * older than the Java version required by the target Minecraft server or VESSEL.
     *
     * @return {@code true} if halted due to JVM version mismatch
     */
    public boolean isJvmVersionMismatchHalted() {
        return jvmVersionMismatchHalted;
    }

    /**
     * Returns the active Tier-1 {@link URLClassLoader} hosting the running Minecraft server.
     *
     * @return active Tier-1 game classloader, or {@code null} if not launched
     */
    public URLClassLoader getActiveTierOneClassLoader() {
        return activeTierOneClassLoader;
    }

    /**
     * Returns all structured console log lines emitted during the most recent launch.
     *
     * @return unmodifiable list of console log lines
     */
    public List<String> getLastBootConsoleLogs() {
        return lastBootConsoleLogs;
    }

    /**
     * Returns the active VESSEL version code string after Step 0 patch promotion.
     *
     * @return active VESSEL version code
     */
    public String getActiveVesselVersionCode() {
        return activeVesselVersionCode;
    }

    /**
     * Installs {@link VesselConsoleCommandInterceptorStream} onto {@link System#in} so that
     * {@code /vessel} and {@code vessel} commands typed in the server console are handled by
     * VESSEL directly while standard Minecraft commands pass through to Brigadier.
     */
    public void installConsoleCommandInterceptor() {
        InputStream currentIn = System.in;
        if (currentIn instanceof VesselConsoleCommandInterceptorStream existingInterceptor) {
            currentIn = existingInterceptor.getDelegateStream();
        }
        if (currentIn != null) {
            System.setIn(new VesselConsoleCommandInterceptorStream(currentIn, this));
        }
    }

    /**
     * Executes a raw console command line intercepted from {@link System#in} (such as
     * {@code /vessel}, {@code vessel mods}, {@code /vessel mod}, {@code /vessel top}, or
     * {@code cls}) and logs the output to the server console.
     *
     * @param rawCommandLine raw console command line
     * @return list of response lines emitted to the console
     */
    public List<String> executeVesselConsoleCommand(String rawCommandLine) {
        VesselStartupConsoleLogger logger = activeConsoleLogger;
        if (logger == null) {
            logger = new VesselStartupConsoleLogger(false);
            activeConsoleLogger = logger;
        }
        if (rawCommandLine == null || rawCommandLine.isBlank()) {
            return List.of();
        }
        String trimmed = rawCommandLine.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if ("cls".equals(lower) || "clear".equals(lower)) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
            String msg = "Console screen cleared (type 'vessel help' for VESSEL commands or 'help' for Minecraft commands).";
            logger.logInfo(msg);
            lastBootConsoleLogs = logger.getRecordedLines();
            return List.of(msg);
        }

        String withoutSlash = trimmed.startsWith("/") ? trimmed.substring(1).trim() : trimmed;
        String[] tokens = withoutSlash.split("\\s+");
        String rootCmd = tokens[0].toLowerCase(Locale.ROOT);
        String[] subArgs;
        if (tokens.length <= 1) {
            subArgs = new String[0];
        } else {
            subArgs = new String[tokens.length - 1];
            System.arraycopy(tokens, 1, subArgs, 0, subArgs.length);
        }

        VesselRootCommand rootCommand = new VesselRootCommand();
        String geyserVer = System.getProperty(
                "vessel.active.geyser.version",
                BuiltInGeyserAndSparkProvisioner.DEFAULT_LATEST_GEYSER_VERSION);
        String floodgateVer = System.getProperty(
                "vessel.active.floodgate.version",
                BuiltInGeyserAndSparkProvisioner.DEFAULT_LATEST_FLOODGATE_VERSION);
        String sparkVer = System.getProperty(
                "vessel.active.spark.version",
                BuiltInGeyserAndSparkProvisioner.DEFAULT_LATEST_SPARK_VERSION);

        List<String> outputLines;
        if ("geyser".equals(rootCmd)) {
            BuiltInGeyserBedrockBridgeService bridge = activeGeyserBridgeService;
            if (bridge != null) {
                bridge.dispatchCommandToBridgeProcess(withoutSlash);
            }
            outputLines = rootCommand.executeGeyserConsoleCommand(
                    subArgs,
                    geyserVer,
                    floodgateVer,
                    BuiltInGeyserBedrockBridgeService.DEFAULT_BEDROCK_UDP_PORT,
                    bridge != null ? bridge.getActiveJavaTargetPort() : 25565,
                    bridge != null && bridge.isBridgeRunning());
        } else if ("floodgate".equals(rootCmd) || "fwhitelist".equals(rootCmd)) {
            String[] effectiveArgs = "fwhitelist".equals(rootCmd)
                    ? new String[]{"whitelist", subArgs.length > 0 ? subArgs[0] : "status"}
                    : subArgs;
            outputLines = rootCommand.executeFloodgateConsoleCommand(
                    effectiveArgs,
                    floodgateVer,
                    geyserVer);
        } else if ("spark".equals(rootCmd)) {
            outputLines = rootCommand.executeSparkConsoleCommand(
                    subArgs,
                    sparkVer,
                    resolvedActiveMcVersion);
        } else if ("restart".equals(rootCmd)) {
            logger.logInfo("[VESSEL] Executing server graceful restart sequence...");
            VesselServerRestartService restartService = new VesselServerRestartService();
            restartService.triggerServerProcessRestart(serverRoot);
            String msg = "[VESSEL] Server restart dispatched. Relaunching replacement JVM...";
            logger.logInfo(msg);
            lastBootConsoleLogs = logger.getRecordedLines();
            return List.of(msg);
        } else if (bukkitCommandBridge.isBukkitCommand(rootCmd)) {
            outputLines = bukkitCommandBridge.executeBukkitCommand(rootCmd, subArgs, null);
        } else {
            List<VesselModuleIdentifier> moduleList = new ArrayList<>(discoveredModules.values());
            boolean ultraOpt = activeCustomFlags != null && activeCustomFlags.isUltraOptimizationEnabled();
            boolean deepDbg = activeCustomFlags != null && activeCustomFlags.isDeepDebugEnabled();
            outputLines = rootCommand.executeLiveConsoleCommand(
                    subArgs,
                    activeVesselVersionCode,
                    resolvedActiveMcVersion,
                    moduleList,
                    activatedEntrypointsCount,
                    ultraOpt,
                    deepDbg);
        }
        for (String line : outputLines) {
            logger.logInfo(line);
        }
        lastBootConsoleLogs = logger.getRecordedLines();
        return outputLines;
    }
}
