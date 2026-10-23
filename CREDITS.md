# VESSEL Third-Party Open-Source Credits & Attributions

VESSEL is built upon the collective wisdom, breakthroughs, and architectural patterns of the open-source Minecraft server, modding, and performance communities.

This document provides full, explicit attribution, copyright notices, upstream repositories, and licenses for all third-party ecosystems and lag-reduction / optimization algorithms bundled or adapted in VESSEL for **Minecraft latest.release**.

---

## Section I: Bundled Ecosystem Integrations (Auto-Provisioned)

VESSEL automatically fetches and maintains the latest releases of these companion projects via official APIs:

### 1. GeyserMC (`Geyser` & `Floodgate`)
- **Integrated Geyser Version:** `2.6.1-b756` (Auto-updated to latest release on every VESSEL update)
- **Integrated Floodgate Version:** `2.2.7-b69` (Auto-updated to latest release on every VESSEL update)
- **Authors:** GeyserMC Team & Contributors
- **License:** MIT License
- **Official Website & Repositories:** https://geysermc.org / https://github.com/GeyserMC/Geyser | https://geysermc.org / https://github.com/GeyserMC/Floodgate
- **Role in VESSEL:** Enables seamless Minecraft: Bedrock Edition client connections (UDP 19132) to VESSEL Java Edition servers with protocol translation, Xbox Live / Floodgate authentication, and automatic custom block/item resource pack mapping.

### 2. spark (`lucko/spark` Performance Profiler)
- **Integrated Version:** `1.10.119` (Auto-updated to latest release on every VESSEL update)
- **Authors:** Lucko (lucko) & Contributors
- **License:** GPL-3.0 (Standalone Profiler Agent/Plugin Integration)
- **Official Website & Repository:** https://spark.lucko.me / https://github.com/lucko/spark
- **Role in VESSEL:** Provides production-ready async CPU sampling, MSPT tick duration histograms, GC pressure monitoring, and heap summary diagnostics alongside VESSEL's internal microkernel telemetry.

---

## Section II: Server Engine, Multi-Threading & Lag-Reduction Architectures

VESSEL's 40 microkernel optimization engines incorporate, adapt, and build upon algorithms, data structures, and optimization paradigms pioneered by the following upstream projects:

### 3. Sodium / Embeddium
- **Project:** Sodium / Embeddium
- **Authors:** CaffeineMC (JellySquid / IMS21) & Contributors
- **License:** LGPL-3.0
- **Upstream Repository:** https://github.com/CaffeineMC/sodium-fabric
- **Role in VESSEL:** Pioneered high-performance chunk meshing, compact 16-bit block neighbor index table (CompactBlockNeighborTable), fast chunk section palette serialization (FastChunkSectionPalette), and BFS fluid propagation algorithms adapted in VESSEL's ultra-optimize kernel pipeline.

### 4. Lithium / Radium
- **Project:** Lithium / Radium
- **Authors:** CaffeineMC (JellySquid / 2No2Name) & Contributors
- **License:** LGPL-3.0
- **Upstream Repository:** https://github.com/CaffeineMC/lithium-fabric
- **Role in VESSEL:** Architected spatial entity hashing (SpatialEntityHashGrid), priority block tick scheduling (FastBlockTickPriorityQueue), entity activation range throttling (EntityActivationRangeGovernor), dropped item/XP orb batch merging, and light suppression techniques foundational to VESSEL's tick engine.

### 5. FerriteCore
- **Project:** FerriteCore
- **Authors:** malte0811 & Contributors
- **License:** MIT License
- **Upstream Repository:** https://github.com/malte0811/FerriteCore
- **Role in VESSEL:** Innovated block state property deduplication (CompactPropertyStorage), compact integer sets (FastFastutilIntSet), and block model geometry memory compaction, powering VESSEL's sub-gigabyte memory footprint.

### 6. ModernFix
- **Project:** ModernFix
- **Authors:** embeddedt & Contributors
- **License:** LGPL-3.0
- **Upstream Repository:** https://github.com/embeddedt/ModernFix
- **Role in VESSEL:** Conceived on-demand DFU bootstrap (LazyDfuAndDynamicResourceOptimizer), direct ByteBuf and event listener leak guards (DirectByteBufAndEventBusLeakGuard), and structure location adaptive backoff algorithms incorporated into VESSEL.

### 7. C2ME (Concurrent Chunk Management Engine)
- **Project:** C2ME (Concurrent Chunk Management Engine)
- **Authors:** RelativityMC (ishland) & Contributors
- **License:** MIT License
- **Upstream Repository:** https://github.com/RelativityMC/C2ME-fabric
- **Role in VESSEL:** Advanced asynchronous multi-threaded chunk loading, linear region file zero-allocation I/O (LinearRegionFileFastPath), and non-blocking chunk ticket scheduler (ChunkTicketAdaptiveThrottle).

### 8. Krypton
- **Project:** Krypton
- **Authors:** Andrew Steinborn (TuxDidier) & Contributors
- **License:** LGPL-3.0
- **Upstream Repository:** https://github.com/astei/krypton
- **Role in VESSEL:** Pioneered zero-copy packet framing (ZeroCopyPacketFrame), Netty channel write consolidation ring buffer (PacketCoalescingRingBuffer), and microkernel network pipeline bypass (VesselNettyChannelInterceptor).

### 9. LazyDFU
- **Project:** LazyDFU
- **Authors:** astei & Contributors
- **License:** MIT License
- **Upstream Repository:** https://github.com/astei/lazydfu
- **Role in VESSEL:** Eliminated server startup freeze by deferring Mojang DataFixerUpper (DFU) rules and compound schema compilation until required.

### 10. FastSuite
- **Project:** FastSuite
- **Authors:** Shadows-of-Fire / Placebo & Contributors
- **License:** MIT License
- **Upstream Repository:** https://github.com/Shadows-of-Fire/FastSuite
- **Role in VESSEL:** Provided indexed recipe lookup and item transfer backoff algorithms utilized in VESSEL cross-chunk hopper and pipe networks.

### 11. Ksyxis
- **Project:** Ksyxis
- **Authors:** VidTu & Contributors
- **License:** MIT License
- **Upstream Repository:** https://github.com/VidTu/Ksyxis
- **Role in VESSEL:** Inspired zero-lag instant server launch by deferring synchronous spawn chunk generation until first player login.

### 12. PaperMC & Folia
- **Project:** PaperMC & Folia
- **Authors:** PaperMC Team & Contributors
- **License:** GPL-3.0
- **Upstream Repositories:** https://papermc.io / https://github.com/PaperMC/Paper / https://github.com/PaperMC/Folia
- **Role in VESSEL:** Architected regionized multithreaded tick strands (RegionalActorStrand, GlobalRegionActorStrand), watchdog freeze avoidance (max-tick-time=-1), and production-grade restart automation.

### 13. Valkyrien Skies
- **Project:** Valkyrien Skies
- **Authors:** Valkyrien Skies Team & Contributors
- **License:** MIT License
- **Upstream Repository:** https://valkyrienskies.org / https://github.com/ValkyrienSkies/Valkyrien-Skies-2
- **Role in VESSEL:** Conceived rigid-body ship physics strand boundaries (ValkyrienPhysicsStrandBarrier) and transform collision governors (ValkyrienPhysicsAndVoxelShipCollisionGovernor) integrated into VESSEL's dynamic physics engine.

---

## Section III: Vanilla Game Platform

### 14. Minecraft Dedicated Server (`Mojang Studios`)
- **Target Default Release:** `latest.release` (`latest.release` via Mojang Piston Meta API)
- **Authors:** Mojang AB / Microsoft Corporation
- **License:** Mojang EULA (https://www.minecraft.net/en-us/eula)
- **Official Website:** https://www.minecraft.net
- **Role in VESSEL:** Core vanilla game mechanics, world generation, and official server protocol foundation.
