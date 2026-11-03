# VESSEL Third-Party Open-Source Credits, Attributions & License Compliance

- **Version**: `1.0.0`
- **Last Updated**: `2026-11-03`
- **Repository**: `VesselLoader/Vessel` (`Works/VESSEL`)

VESSEL is a Sovereign Standalone Universal Minecraft Server & Mod/Plugin Runtime built upon clean-room engineering and the collective breakthroughs of the open-source Minecraft server, modding, and performance communities.

This document provides full, explicit attribution, copyright notices, upstream repositories, and license compliance disclosures for all third-party ecosystems, supported mod/plugin loaders, runtime libraries, and memory-leak / lag-reduction optimization algorithms integrated or adapted in VESSEL.

---

## Section I: Bundled & Auto-Provisioned Ecosystem Integrations

VESSEL automatically provisions and bridges these companion projects as separate runtime artifacts in `cache/ecosystem/` in full compliance with their respective licenses:

### 1. GeyserMC (`Geyser` & `Floodgate`)
- **Authors:** GeyserMC Team & Contributors
- **License:** MIT License
- **Official Website & Repositories:** https://geysermc.org | https://github.com/GeyserMC/Geyser | https://github.com/GeyserMC/Floodgate
- **Role in VESSEL:** Enables seamless Minecraft: Bedrock Edition client connections (`UDP 19132`) to VESSEL Java Edition servers with protocol translation, Xbox Live / Floodgate authentication (`BuiltInGeyserAndSparkProvisioner`, `BuiltInGeyserBedrockBridgeService`, `BedrockRakNetPortBinder`, `BedrockGeometryModelBridge`, `GeyserCustomGeometryTranslator`), and automatic custom block/item resource pack mapping.

### 2. spark (`lucko/spark` Performance Profiler)
- **Authors:** Lucko (`lucko`) & Contributors
- **License:** GPL-3.0 (Provisioned as an external/isolated standalone profiler plugin artifact in `cache/ecosystem/`; zero GPL source code is linked or copied into VESSEL Core)
- **Official Website & Repository:** https://spark.lucko.me | https://github.com/lucko/spark
- **Role in VESSEL:** Provides production-ready async CPU sampling, MSPT tick duration histograms, GC pressure monitoring, and heap summary diagnostics alongside VESSEL's internal microkernel telemetry.

---

## Section II: Built-In Memory-Leak Sanitization, Multi-Threading & Lag-Reduction Architectures

VESSEL's internal optimization packages (`dev.vessel.kernel.optimization.*` and `dev.vessel.api.*`) are **100% clean-room Java 21/25 implementations** (adhering to Rule 17 Single-Responsibility Class design) that credit and build upon the foundational algorithms, data structures, and research pioneered by the following upstream projects:

| # | Upstream Project | Authors & Maintainers | Upstream License | Upstream Repository | Clean-Room VESSEL Classes & Architectural Role |
| :---: | :--- | :--- | :--- | :--- | :--- |
| **3** | **Sodium / Embeddium** | CaffeineMC (`JellySquid`, `IMS21`), `embeddedt` & Contributors | LGPL-3.0 *(Clean-Room Algorithmic Adaptation)* | https://github.com/CaffeineMC/sodium-fabric | `CompactBlockNeighborTable`, `FastChunkSectionPalette`, `BreadthFirstFluidSimulator` (16-bit neighbor tables, dense palette serialization, BFS fluid simulation). |
| **4** | **Lithium / Radium** | CaffeineMC (`JellySquid`, `2No2Name`), `Asek3` & Contributors | LGPL-3.0 *(Clean-Room Algorithmic Adaptation)* | https://github.com/CaffeineMC/lithium-fabric | `WorldEntitySpatialGrid`, `FastBlockTickPriorityQueue`, `EntityActivationRangeLOD` (O(1) spatial cell hashing, block tick priority queues, entity LOD activation). |
| **5** | **FerriteCore** | `malte0811` & Contributors | MIT License | https://github.com/malte0811/FerriteCore | `CompactStatePropertyTable`, `IdentifierPoolDeduplicator` (block state property deduplication and string/ResourceLocation interning). |
| **6** | **MemoryLeakFix & AllTheLeaks** | `FX - PR0CESS` (`fxmorin`), `Uncandango` & Contributors | LGPL-3.0 / MIT *(Clean-Room Algorithmic Adaptation)* | https://github.com/fxmorin/MemoryLeakFix / https://github.com/Uncandango/AllTheLeaks | `MemoryLeakSentinelWatchdog`, `StaticWorldReferenceSanitizer`, `EphemeralWorldInstanceEngine`, `ThreadLocalLeakSanitizer`, `LifecycleDisposalRegistry`, `NativeMemoryPoolRecycler` (automatic static `ServerLevel`/`Entity` reference scrubbing on world unload, ThreadLocal buffer cleanup, and native off-heap memory pool recycling). |
| **7** | **ModernFix** | `embeddedt` & Contributors | LGPL-3.0 *(Clean-Room Algorithmic Adaptation)* | https://github.com/embeddedt/ModernFix | `DfuAotOptimizationPath`, `DirectByteBufAndEventBusLeakGuard`, `AsyncStructureLocateAndJigsawGovernor` (Netty DirectByteBuf & dead event listener leak guards, async structure search backoff). |
| **8** | **Starlight & Moonrise** | `Spottedleaf` (Tuinity / PaperMC) & Contributors | LGPL-3.0 / GPL-3.0 *(Clean-Room Algorithmic Adaptation)* | https://github.com/PaperMC/Starlight / https://github.com/Tuinity/Moonrise | `StarlightZeroCopyLightEngine`, `ChunkSectionLightData` (zero-copy nibble-packed voxel sky/block light BFS propagation). |
| **9** | **C2ME** | RelativityMC (`ishland`) & Contributors | MIT License | https://github.com/RelativityMC/C2ME-fabric | `LinearRegionFileFastPath`, `ChunkTicketAdaptiveThrottle`, `ChunkTicketGovernor` (async multi-threaded chunk I/O and non-blocking chunk ticket throttling). |
| **10** | **Krypton** | Andrew Steinborn (`astei`) & Contributors | LGPL-3.0 *(Clean-Room Algorithmic Adaptation)* | https://github.com/astei/krypton | `FastVarIntCodec`, `PacketBandwidthOptimizer`, `VesselNettyChannelInterceptor` (unrolled VarInt encoding, micro-packet coalescing, and Netty pipeline slicing). |
| **11** | **Alternate Current** | `SpaceWalkerRS` & Contributors | MIT License | https://github.com/SpaceWalkerRS/alternate-current | `AlternateCurrentBfsRedstoneSolver` (deterministic breadth-first redstone wire network power solver). |
| **12** | **Clumps, ServerCore & EntityCulling** | `Jaredlll08`, `Wesley1808`, `tr7zw` & Contributors | MIT License | https://github.com/jaredlll08/Clumps / https://github.com/wesley1808/ServerCore | `ItemAndXpOrbSpatialMergeAccelerator`, `PoiSpatialIndexAndVillagerLobotomizer`, `EntityBrainTickThrottle`, `SpatialSoundAndParticleAcousticCuller`, `MovementDeltaCullingFilter`. |
| **13** | **LazyDFU** | `astei` & Contributors | MIT License | https://github.com/astei/lazydfu | `DfuAotOptimizationPath` (deferred DataFixerUpper schema compilation). |
| **14** | **FastSuite** | `Shadows-of-Fire` & Contributors | MIT License | https://github.com/Shadows-of-Fire/FastSuite | `IndexedRecipeLookupAccelerator`, `CapabilityCacheAndPipeBackoffGovernor` (indexed recipe lookup and cross-chunk pipe/hopper backoff). |
| **15** | **Ksyxis** | `VidTu` & Contributors | MIT License | https://github.com/VidTu/Ksyxis | Instant server startup via deferred spawn chunk pre-generation. |
| **16** | **PaperMC, Folia, Purpur & Canvas** | PaperMC Team (`Spottedleaf`, `electronicboy`, `Machine_Maker`), PurpurMC, CanvasMC | GPL-3.0 / MIT-Licensed API Contributors *(Clean-Room API & Actor Design)* | https://github.com/PaperMC/Paper / https://github.com/PaperMC/Folia | `RegionalActorStrand`, `GlobalRegionActorStrand`, `GlobalRegionSyncBarrier`, `FoliaRegionExecutionBridge`, `AsyncCatcherThreadValidator` (16x16 chunk regionalized actor strands and MPSC lock-free mailboxes). |
| **17** | **Valkyrien Skies, Create, Botania, CC:Tweaked, Curios/Trinkets, Architectury & GeckoLib** | Valkyrien Skies Team, `Simibubi` (Create), `Vazkii` (Botania), `SquidDev` (CC:Tweaked), `TheIllusiveC4` (Curios), `Emi` (Trinkets), Architectury Team, `Gecko`/`Tslat` (GeckoLib) | MIT / MPL-2.0 / LGPL-3.0 *(Clean-Room Interoperability Governors)* | https://github.com/ValkyrienSkies/Valkyrien-Skies-2 / https://github.com/Creators-of-Create/Create | `ValkyrienPhysicsStrandBarrier`, `ValkyrienPhysicsAndVoxelShipCollisionGovernor`, `KineticNetworkGraphCache`, `BeltItemCoalescingOptimizer`, `ManaBurstAndCorporeaNetworkCoalescer`, `LuaCoroutineAndPeripheralQuotaGovernor`, `CuriosAccessoriesInventoryBridge`, `ArchitecturySyntheticBridge`, `GeckoLibRuntimeAdapter`. |

---

## Section III: Directly Supported Mod & Plugin Loader Ecosystems (Clean-Room Compatibility)

VESSEL's VSTM Super-API (`Vessel-api`) and 4-Tier ClassLoader hierarchy provide clean-room binary and lifecycle compatibility for modules built for the following ecosystems, strictly respecting each project's license (`22-SECURITY-AND-RECOVERY.md` Rules L-1, L-2, L-3):

| # | Loader / Ecosystem | Upstream Organization & Authors | Upstream License | Upstream Repository | Clean-Room Compliance & Integration Mechanism in VESSEL |
| :---: | :--- | :--- | :--- | :--- | :--- |
| **18** | **Fabric Loader & Fabric API** | FabricMC Team & Contributors | **Apache-2.0** | https://github.com/FabricMC/fabric-loader / https://github.com/FabricMC/fabric | Clean-room implementation of `FabricLoader`, `ModContainer`, `ModInitializer`, `DedicatedServerModInitializer`, `fabric.mod.json` parser, AccessWidener v1/v2 reader, and `FabricRegistrySymmetryBridge` / `FabricStorageSymmetryAdapter` / `FabricEnergySymmetryAdapter`. |
| **19** | **Quilt Loader, QSL & QFAPI** | QuiltMC Team & Contributors | **Apache-2.0 / CC0-1.0** (`FlexVer`) | https://github.com/QuiltMC/quilt-loader / https://github.com/QuiltMC/quilt-standard-libraries | Clean-room `quilt.mod.json` (JSON5) metadata support, FlexVer version range compatibility, `QuiltModInitializerInvoker`, and VSTM bytecode transmutation for `org.quiltmc.qsl.*`. |
| **20** | **MinecraftForge (`FML` & `EventBus`)** | MinecraftForge Team (`cpw`, `LexManos` & Contributors) | **LGPL-2.1** | https://github.com/MinecraftForge/MinecraftForge / https://github.com/MinecraftForge/EventBus | Clean-room interface/signature compatibility (`@Mod`, `IEventBus`, `DeferredRegister`, `ICapabilityProvider`, `ForgeConfigSpec`, `META-INF/mods.toml`, `mcmod.info`) routed into `SovereignEventBus` and `VesselUniversalRegistry` with zero copied LGPL implementation code. |
| **21** | **NeoForge (`FancyModLoader` & `Bus`)** | NeoForged Team & Contributors | **LGPL-2.1** | https://github.com/neoforged/NeoForge / https://github.com/neoforged/Bus | Clean-room compatibility for `META-INF/neoforge.mods.toml`, `net.neoforged.fml.common.Mod`, `net.neoforged.bus.api.IEventBus`, `DeferredHolder`, `AttachmentType<T>` (`NeoForgeAttachmentContainer`), and `DataMapType`. |
| **22** | **Bukkit, Spigot, Paper & Folia API** | Bukkit Team, SpigotMC (`md_5`), PaperMC Team & Contributors | **GPL-3.0 / MIT-Licensed Contributors** | https://github.com/PaperMC/Paper / https://hub.spigotmc.org/stash/projects/SPIGOT | Clean-room public API surface (`org.bukkit.*`, `JavaPlugin`, `PluginDescriptionFile`, `PaperPluginDescriptor`, `FoliaSchedulerShim`) backed 100% by `dev.vessel.api.*` and `dev.vessel.kernel.*`. **Zero GPL CraftBukkit (`org.bukkit.craftbukkit`) or Spigot server implementation code is present in VESSEL.** |
| **23** | **SpongePowered Mixin, MixinExtras & Sinytra Connector** | SpongePowered (`Mumfrey`), `LlamaLad7` (`MixinExtras`), Sinytra (`Su5eD`) | **MIT License** | https://github.com/SpongePowered/Mixin / https://github.com/LlamaLad7/MixinExtras / https://github.com/Sinytra/Connector | Clean-room Mixin annotation parser (`MixinAnnotationParser`, `MixinToSemanticHookConverter`, `MixinConflictArbitrationEngine`) and cross-loader architectural research from MIT-licensed `Sinytra/Connector` (strictly zero code from GPL-3.0 Sinytra Launchpad). |

---

## Section IV: Core Runtime Dependencies & Vanilla Game Platform

| # | Library / Platform | Authors / Copyright Holder | License | Official URL | Usage in VESSEL |
| :---: | :--- | :--- | :--- | :--- | :--- |
| **24** | **Minecraft Dedicated Server, Brigadier & DataFixerUpper** | Mojang AB / Microsoft Corporation | **Mojang EULA** (`server.jar`) / **MIT License** (`Brigadier`, `DataFixerUpper`) | https://www.minecraft.net / https://github.com/Mojang/brigadier | Downloaded dynamically at runtime from official Mojang Piston Meta CDN into `cache/mojang/` upon operator acceptance of `eula.txt`. Never redistributed in VESSEL Git repositories. |
| **25** | **OW2 ASM (`asm`, `asm-tree`, `asm-commons`, `asm-util` 9.7)** | INRIA, France Telecom | **BSD-3-Clause** | https://asm.ow2.io | Core bytecode reader, analyzer, and writer for VESSEL's single-pass transformation and VSTM transmutation engine. |
| **26** | **Kyori Adventure** | KyoriPowered & Contributors | **MIT License** | https://github.com/KyoriPowered/adventure | Modern rich text component and audience bridge (`AdventureToBedrockChatHarmonizer`). |
| **27** | **Netty, Fastutil, Google Guava/Gson, Apache Log4j2, JOptSimple, OSHI** | The Netty Project, Sebastiano Vigna (`Fastutil`), Google LLC, Apache Software Foundation | **Apache-2.0 / MIT** | https://netty.io / https://fastutil.di.unimi.it | Provided via the unpacked Mojang server bundler runtime (`META-INF/libraries/`) for network transport, primitive collections, and logging. |
| **28** | **JUnit 5 (`Jupiter`) & AssertJ** | JUnit Team, AssertJ Team | **EPL-2.0 / Apache-2.0** | https://junit.org/junit5 / https://assertj.github.io/doc | Test-only dependencies (`testImplementation`) for VESSEL and `Vessel-api` verification suites. |
