# COMPREHENSIVE ADMINISTRATOR & DEVELOPER GUIDE — VESSEL SOVEREIGN SERVER MICROKERNEL (v2.3.1)

> **Document Version:** `1.0.1`  
> **Active VESSEL Build Version Code:** `VESSEL-2.3.1-rev1047`  
> **GitHub Repository:** `https://github.com/VesselLoader/Vessel`  
> **Runnable Server Binary:** `build/libs/server.jar`

---

## 1. Overview of the VESSEL Microkernel

**VESSEL** is a next-generation **Universal Minecraft Server Microkernel & Loader** capable of executing all **6 Mod & Plugin Ecosystems** concurrently inside a **single `server.jar` binary**:
- **Mod Ecosystems:** `Fabric`, `Quilt`, `Minecraft Forge`, `NeoForge`.
- **Plugin Ecosystems:** `Bukkit`, `Spigot`, `Paper`, `Folia`.
- **Built-In Ecosystem Provisioning (Out-of-the-Box):**
  1. **GeyserMC (`Geyser` & `Floodgate` Latest Official Release)** — Enables Minecraft: Bedrock Edition players to connect directly to a VESSEL Java Edition server.
  2. **Spark Profiler (`lucko/spark` Latest Official Release)** — Industry-standard CPU, MSPT, GC, and heap memory telemetry profiler integrated alongside VESSEL's internal flight recorder.
  3. **Mojang Auto-Latest Bundler Resolver** — Automatically queries and downloads the **latest official Minecraft release (`latest.release`)** from Mojang's Piston Meta API by default.

---

## 2. Single-Pass First-Run Boot (Zero Restart Required)

Unlike traditional server loaders that require an initial run to scaffold directories and configuration files followed by a mandatory restart, **VESSEL completes all 16 startup steps and launches the live server in a single execution pass** even on a brand-new empty directory (`--vessel-single-pass-boot` enabled by default):

```bash
# Standard Launch (Automatically resolves latest Minecraft release, provisions Geyser + Spark, loads all mods & plugins):
java -jar server.jar --nogui

# Maximum Optimization Suite (Recommended for Production & Large Modpacks):
java -jar server.jar --vessel-ultra-optimize --nogui

# Deep Diagnostic Debug Mode (Recommended for Developers & Troubleshooting):
java -jar server.jar --vessel-deep-debug --nogui
```

---

## 3. Complete VESSEL Custom CLI Flags Reference

### 3.1. Maximum Performance Optimization Flags (`--vessel-ultra-optimize` / `--vessel-max-perf`)

| CLI Flag | Default | Detailed Behavior |
| :--- | :---: | :--- |
| `--vessel-ultra-optimize` / `--vessel-max-perf` | `false` | **Master Optimization Bundle:** Simultaneously enables all 6 microkernel optimization flags below to sustain a locked 20.0 TPS. |
| `--vessel-simd-physics` | `false` | Uses the **Java 21 Vector API (`FloatVector` SIMD)** for hardware-accelerated parallel AABB collision and entity physics batching. |
| `--vessel-zero-copy-netty` | `false` | Switches the Netty packet pipeline to **Zero-Copy Direct ByteBuf** framing and adaptive VarInt/Zstd compression, minimizing GC pressure. |
| `--vessel-aggressive-ai-lod` | `false` | Activates **Dynamic Entity AI Level-of-Detail (LOD)**, throttling distant mob goal selector evaluations without breaking mob farms. |
| `--vessel-zstd-ram-cache` | `false` | Compresses idle chunk sections and cached bytecode in RAM using high-speed Zstd dictionaries, saving 35%–50% heap memory. |
| `--vessel-lockless-strands` | `false` | Enables the **Lockless Regionized Strand** scheduler (Folia-compatible) for lock-free multi-threaded region execution. |
| `--vessel-lazy-dfu-noisium` | `false` | Defers DataFixerUpper schema compilation (LazyDFU) at boot and enables direct palette block-state writes (Noisium) during chunk generation. |

### 3.2. Deep Diagnostic & Developer Flags (`--vessel-deep-debug` / `--vessel-debug-all`)

| CLI Flag | Default | Detailed Behavior |
| :--- | :---: | :--- |
| `--vessel-deep-debug` / `--vessel-debug-all` | `false` | **Master Diagnostic Bundle:** Simultaneously enables all 5 deep diagnostic tracing flags below. |
| `--vessel-trace-bytecode` | `false` | Logs detailed ASM bytecode transformations (`ResourceLocation`, `Entity.level`, `InventoryView` interface promotion, and Mixin hooks). |
| `--vessel-trace-classloader` | `false` | Traces class loading delegation across the 3-tier ClassLoader topology (Tier-1 Vanilla, Tier-2 Kernel, Tier-3 Ecosystem Spokes). |
| `--vessel-trace-events` | `false` | Traces cross-ecosystem event firing order (`Fabric` $\leftrightarrow$ `Forge/NeoForge IEventBus` $\leftrightarrow$ `Bukkit/Paper PluginManager`). |
| `--vessel-trace-registry` | `false` | Audits `MappedRegistry` unfreezing/refreezing, `DeferredRegister` replay, and Tag/DataPack synchronization. |
| `--vessel-profile-boot` | `false` | Records millisecond execution timing for each of the 16 startup steps. |

### 3.3. Version Control & Compatibility Flags

| CLI Flag | Default | Detailed Behavior |
| :--- | :---: | :--- |
| `--vessel-single-pass-boot` | `true` | Completes first-run initialization and starts the server in a single pass with zero restarts. |
| `--vessel-universal-compat` | `true` | Enables the multi-era backward-compatibility bytecode transformation pipeline for older mods and plugins. |
| `--vessel-auto-latest` | `true` | Automatically resolves and launches the latest official Minecraft release (`latest.release`). |
| `--vessel-version=<ver>` | *(Empty)* | Pins a specific Minecraft release version (e.g., `--vessel-version=1.21.4` or `--vessel-version=1.20.1`). |
| `--vessel-accept-eula` | `true` | Automatically sets `eula=true` in `eula.txt`. |
| `--port=<port>` | `25565` | Configures the default TCP/UDP listening port in `server.properties`. |

---

## 4. Multi-Era Mod/Plugin Compatibility & Strict Forward-Version Rejection Guard

1. **Backward-Compatibility Direction (`Mod/Plugin Version <= Server Version` — ALLOWED & AUTOMATICALLY UPGRADED):**
   - When the server runs the latest Minecraft release (or a version higher than or equal to what a mod/plugin targets), VESSEL **automatically loads the module** and runs `MultiEraBytecodeTransformationPipeline` in RAM (`ResourceLocation.<init>` $\rightarrow$ `fromNamespaceAndPath`/`parse`, `Entity.level` $\rightarrow$ `level()`, `InventoryView` `INVOKEVIRTUAL` $\rightarrow$ `INVOKEINTERFACE`, DataPack `loot_tables/` $\rightarrow$ `loot_table/`).
2. **Forward-Version Rejection Direction (`Mod/Plugin Version > Server Version` — IMMEDIATELY REFUSED):**
   - If the server is running a **lower** Minecraft version than the minimum version required by a mod or plugin, `ForwardVersionModRejectionGuard` **immediately refuses to load that mod/plugin** (`ForwardVersionModRejectedException`) and instructs the administrator to either install a lower build of the mod/plugin that supports the server version OR upgrade the server to a higher Minecraft version (`--vessel-version=<ver>` or default `latest.release`).

---

## 5. Hourly Zero-Lag GitHub Auto-Updater (Staged Patching & 100% User Data Preservation)

- **Automatic Hourly Polling (`3600` seconds) with Zero Tick Lag:** `HourlyGitHubAutoUpdateDaemon` executes on a dedicated background daemon thread (`vessel-hourly-auto-updater`), ensuring **0.00 ms impact** on the 20 TPS server tick loop.
- **Deterministic Version Code (`VesselBuildVersionCode`):** `VESSEL-<major>.<minor>.<patch>-rev<revision>+mc<mcVer>-geyser<geyserVer>-spark<sparkVer>` allows VESSEL to deterministically compare whether the running server is older (`<`), identical (`==`), or newer (`>`) than the remote GitHub release.
- **Live Update Staging & Apply on Restart (`StagedKernelPatchManager`):** Even while the server is actively running, VESSEL downloads staged kernel patches and refreshes the latest **Geyser**, **Spark**, and **Minecraft `latest.release`** descriptors into `cache/updates/staged/`, sets `isRestartRequiredToApplyPatches() == true`, and automatically promotes the staged patches at Step 0 of the next server restart.
- **100% User Data Protection (`UserDataPreservationSnapshotGuard`):** All user directories and files (`mods/`, `plugins/`, `config/`, `vessel.yml`, `server.properties`, `eula.txt`, `world/`, `ops.json`, `whitelist.json`) are strictly protected via SHA-256 integrity checks and are never overwritten or deleted.

---

## 6. Third-Party Open-Source Credits & Attributions

1. **GeyserMC (`Geyser` & `Floodgate`)** — Copyright (c) 2019–2026 GeyserMC Team & Contributors (MIT License) — [https://geysermc.org](https://geysermc.org)
2. **spark (`lucko/spark` Performance Profiler)** — Copyright (c) 2018–2026 Lucko (`lucko`) & Contributors (GPL-3.0 Standalone Profiler Integration) — [https://spark.lucko.me](https://spark.lucko.me)
3. **Minecraft Dedicated Server** — Copyright (c) Mojang AB / Microsoft Corporation.
