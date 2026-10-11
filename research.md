# Upstream Research: Fabric and Minecraft Core

**Document version:** 0.1.0
**Research date:** 2026-09-28
**Research status:** `PARTIAL` (version data only, no API inventory yet)
**Platform:** FabricMC (source loader id in VESSEL: `fabric`) and Minecraft Java Edition core

Rules used in this file: every claim is tied to a source in section 5. Where the source
was read only as a search excerpt this is said explicitly. Nothing here was invented to
fill a gap.

---

## 1. Minecraft Java Edition versions

Sources: Minecraft Wiki version history (S1), Fabric's official 26.3 post (S2), and the
minecraft.net release articles (S3).

| Version | Name | Release state | Date |
|---|---|---|---|
| 26.1, 26.1.1, 26.1.2 | | released | 2026 (three builds in the 26.1 line) |
| 26.2 | Chaos Cubed | released | 2026-06-16 |
| 26.3 | Wilderness Bound | released | 2026-09-15 |
| 26.4 | | in development (development releases) | since 2026-09-22 |
| 1.21.11 | | last release of the `1.21.x` line before the year-based numbering | |

Notes:
- The old spec text treated 26.3 as only a "migration target". It has been a full
  release since 2026-09-15, so the Version Registry must classify it as
  `CURRENT_STABLE` for Minecraft itself.
- One search result (a comic-shop blog post, S4) called 26.2 the current version. It
  was dated well before 26.3's release and was rejected in favor of the official sources.

---

## 2. Pack format history (source S5, Minecraft Wiki "Pack format")

This page is the reference for every resource pack and data pack version number.
Facts VESSEL depends on:

1. **The single `pack_format` number was replaced by a range** in `pack.mcmeta`:
   `min_format` and `max_format`. It started at snapshot 25w31a and is official from
   Java Edition **1.21.9**. One pack file can now declare compatibility with several
   Minecraft versions.
2. Data pack format and resource pack format are **two separate numbers** that move
   independently. Example from the pages read for 26.2: resource pack format 88.0 and
   data pack format 107.1.
3. The same resource pack format number can cover more than one Minecraft version
   (example: 1.21.9 and 1.21.10 share a resource pack format).
4. The page lists the breaking change behind each format increase, which is the input
   VESSEL's pack compiler needs to know what to transform between versions.

### 2.1 Effect on the build cache

Because several Minecraft versions can share one pack format, the pack part of the
Startup Build Stage cache can be keyed by **pack format number** instead of Minecraft
version string, which avoids duplicate builds. The mod and plugin logic part must still
be keyed by Minecraft version. Shiroz answered "whichever is optimal", so the optimal
split is recorded here: **pack outputs keyed by pack format, logic outputs keyed by
Minecraft version.** (Not yet written into the master spec; listed as an open item.)

---

## 3. Fabric Loader

Sources: fabricmc.net 26.3 post (S2), Fabric Loader GitHub releases (S6).

| Item | Value | Confidence |
|---|---|---|
| Recommended loader for 26.3 | 0.19.5 | High (official post) |
| 0.19.5 release date | 2026-08-28 | High (search listing plus the official post) |
| 0.19.4 release date | 2026-08-26 | Medium (search listing only) |
| 0.19.3 | earlier release | Medium |

Two contradictions found and how they were handled:
- A direct fetch of the GitHub releases page returned a cached page that stopped at
  0.19.3, while the search listing showed 0.19.4 and 0.19.5. The official Fabric 26.3
  post names 0.19.5, so 0.19.5 is used.
- Two search results were rejected because they were not the official project: a GitHub
  repository under `fabric-mc/fabric-mod-loader` (the official organization is
  `FabricMC`) and an unrelated Wikipedia page about "Fabric OS".

### 3.1 Changes the 26.3 post lists (paraphrased)

The official 26.3 post describes API changes for modders, including new block
transformers, dynamic registries, a move from GLFW to SDL, and brewing recipes becoming
data driven. These were read from the post summary. **The post was not read in full and
no API symbols were extracted**, so they are noted here only as topics that need a proper
inventory.

---

## 4. What is still missing for Fabric

1. The `fabric.mod.json` schema (fields, entrypoints, environment, dependencies).
2. Fabric Loader API surface: `FabricLoader`, `getGameDir`, `getConfigDir`, entrypoint
   types, and how `fabric.modsFolder` works.
3. Lifecycle and threading.
4. Nested JAR behavior.
5. Mixin details and access widener format.
6. Fabric API module list per Minecraft version.
7. Mapping situation: from 26.1 onward Minecraft is unobfuscated, which other sources
   reported as removing the need for mappings. This was seen in third-party and
   NeoForge sources only and is not yet confirmed from a Fabric source.
8. Older Fabric-supported Minecraft versions and their loader ranges.

---

## 5. Sources

| Id | Source | What it gave |
|---|---|---|
| S1 | minecraft.wiki/w/Java_Edition_version_history (fetched) | Version list, dates and names |
| S2 | fabricmc.net/2026/09/15/263.html (fetched) | 26.3 release, loader 0.19.5, list of API changes |
| S3 | minecraft.net articles: 26.3 Release Candidate 2, Minecraft Java Edition 26.3 (search excerpts) | 26.3 release confirmation |
| S4 | A comic-shop blog page (search result) | Rejected, outdated |
| S5 | minecraft.wiki/w/Pack_format (fetched) | Pack format history and the `min_format` / `max_format` change |
| S6 | github.com/FabricMC/fabric-loader/releases (fetched, cached older page) and search listing | Loader release numbers and dates |
