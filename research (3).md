# Upstream Research: Forge

**Document version:** 0.4.55
**Research date:** 2026-09-28 (passes 1 to 4), 2026-09-29 (passes 5 to 11), 2026-09-30 (passes 12 to 17), 2026-10-01 to 2026-10-08 (passes 18 to 26)
**Research status:** `PARTIAL` (pass 5 added the official promotions data, update checker, versioning convention, 26.3 changelog and the full docs map as sections 1.2a, 13a, 18 and 19; still not complete: see section 15; pass 6 added section 20 on data packs, resource packs, conditions, loot modifiers, tags and recipes; pass 7 added section 21 on loot tables, read from the 1.20.x docs; pass 8 added section 22 on advancements and items; pass 9 added section 23 on block entities and model loaders; pass 10 added section 24 on client-side renderers and item model state; pass 11 added section 25 on block states, texture tinting, the recipes introduction and a renderer check; pass 12 added section 26 from Forge source code; pass 13 added section 27, docs checked against the code; pass 14 added section 28 on conditional data, tags and registry flags as coded; pass 15 added section 29, the re-verification register; pass 16 added section 30 on pre-1.8 vanilla block metadata; pass 17 added section 31, the vanilla metadata table; pass 18 added section 32, the four unchecked register rows; pass 19 added section 33 on the client and item side at 26.3; pass 20 added section 34 on threading, trigger registration and duplicate keys; pass 21 added section 35 on conditional data of 1.20.4 and 1.21.1, the tags list and the Javadoc inventory; pass 22 added section 36 on entities, commands, the scheduler and Mojang's official names; pass 23 added section 37 on worldgen, data generation and the source of official-name data; pass 24 added section 38 on the ModLoader compatibility layer, the E2 launch flow and threading per era; pass 25 added section 39 on rendering hooks, recipes, loot tables, the scheduler and items; pass 26 added section 40 on the loader mechanisms, networking at 26.3, the ModLoader layer, the early Forge history, build numbers and items)
**Platform:** MinecraftForge (source loader id in VESSEL: `forge`)
**Companion file:** `version-index.json` (all 78 official Minecraft versions, era, Java runtime, builds seen)

How to read this file:
- Every claim carries a source id from section 17. Source authority is rated A (official Forge
  site, docs, repository, maven), B (official neighbour projects: NeoForged, SpongePowered,
  Fabric docs), C (community wikis, forum posts, gists, mirrors, third-party projects).
- API names, class names, TOML keys, annotation names and regexes are reproduced exactly.
  Explanatory prose is paraphrased.
- "Evidenced" means a source shows it. "Proposal" means it is VESSEL's own design decision.
  "Unverified" means it was seen only weakly or not at all.

---

## 0. Corrections to v0.1.0 (read first)

1. **The rejected third-party claim was rejected too strongly.** v0.1.0 called the statement
   "Forge is not available for Minecraft 26.x" wrong. The official pages do list Forge builds
   for 26.1 to 26.3, so Forge itself exists for 26.x. But two third-party projects genuinely
   dropped Forge for 26.x as a support decision: Dynamic Pack Refresher (S7) and the
   Architectury API, whose 26.1 to 26.2 porting guide speaks of "the removal of forge support"
   (S24). The correct reading is: Forge publishes 26.x builds, but part of the mod ecosystem
   stopped targeting it. This may mean few Forge 26.x mods exist in the wild. It does not
   change the requirement that VESSEL supports them.
5. **E0 does not exist as previously framed.** v0.2.0 speculated Forge might have Beta-era
   builds older than 1.1. Independent evidence (S30) states plainly that Forge was first
   released for Minecraft 1.1 in 2011, matching the official index's earliest entry. There is
   no older Forge. Section 2's era table is corrected: E0 is removed, and 1.1 to 1.2.5 is
   folded into what is now called era E1a (see section 2).
6. **`mcmod.info`'s root has always been a bare JSON array**, `[{...}, {...}]`, one object per
   mod, confirmed identically from 1.7.10 through 1.13.x documentation (S19, S19c). A different
   JSON shape seen in v0.2.0's source material (an object with `modListVersion` and a `modList`
   array) is a separate, unofficial "mod list" concept used by some launchers/modpacks for
   *listing* installed mods to players, not the in-JAR `mcmod.info` metadata file. The two must
   not be confused; only the bare-array form is what VESSEL's Forge metadata reader parses.
2. **Registry duplicates.** `identity-and-collision.md` v0.1.0 treated a duplicate registry key as
   a conflict the platform would report. The official Forge docs show the opposite: inside one
   registry the second object silently overrides the first (section 6). The identity document is
   corrected to v0.1.1 accordingly.
3. **Config timing.** v0.1.0 said Forge's config read timing was unknown. The registries page states
   that `RegisterEvent` fires after mod constructors and before configs are loaded (section 6).
   So registered content cannot depend on config values.
4. v0.1.0 said `defaultconfigs` had no evidence. A 1.14.4 server log now proves the directory
   exists and is looked up at startup (section 11). The copy behavior is still unverified.

---

## 1. Version index

### 1.1 The official list (S5, fetched in full from the official downloads site)

The official Forge index lists **78 Minecraft versions**, from **1.1** to **26.3**. It is stored
in full in `version-index.json`. Summary by line:

| Line | Versions listed |
|---|---|
| 26.x | 26.1, 26.1.1, 26.1.2, 26.2, 26.3 |
| 1.21 | 1.21, 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11 (no 1.21.2) |
| 1.20 | 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.6 (no 1.20.5) |
| 1.19 | 1.19, 1.19.1, 1.19.2, 1.19.3, 1.19.4 |
| 1.18 | 1.18, 1.18.1, 1.18.2 |
| 1.17 | 1.17.1 only |
| 1.16 | 1.16.1, 1.16.2, 1.16.3, 1.16.4, 1.16.5 |
| 1.15 | 1.15, 1.15.1, 1.15.2 |
| 1.14 | 1.14.2, 1.14.3, 1.14.4 |
| 1.13 | 1.13.2 only |
| 1.12 | 1.12, 1.12.1, 1.12.2 |
| 1.11 | 1.11, 1.11.2 |
| 1.10 | 1.10, 1.10.2 |
| 1.9 | 1.9, 1.9.4 |
| 1.8 | 1.8, 1.8.8, 1.8.9 |
| 1.7 | 1.7.2, 1.7.10_pre4, 1.7.10 |
| 1.6 | 1.6.1, 1.6.2, 1.6.3, 1.6.4 |
| 1.5 | 1.5, 1.5.1, 1.5.2 |
| 1.4 | 1.4.0, 1.4.1, 1.4.2, 1.4.3, 1.4.4, 1.4.5, 1.4.6, 1.4.7 |
| 1.3 | 1.3.2 only |
| 1.2 | 1.2.3, 1.2.4, 1.2.5 |
| 1.1 | 1.1 only |

The earliest entry on the official index is Minecraft **1.1**. This supports Shiroz's statement
that Forge for 1.1 must be supported. The index cannot show whether builds older than 1.1
(the Beta era) exist. Community history says Forge was started in the Beta era (S20), but
those builds are not on the index. Open question in section 15.

### 1.2 Builds seen

Build numbers are known for **31 of the 78** versions. The other 47 versions have no build
number in this research. The full table is in `version-index.json`. Highlights:

| Minecraft | Forge build | Detail | Source |
|---|---|---|---|
| 26.3 | 66.0.6 | latest, 2026-09-27, no recommended build listed | S6 |
| 26.2 | 65.1.3 latest (2026-08-26), 65.1.0 recommended (2026-07-27), 65.0.0 first (2026-06-17) | 14 builds listed | S5 |
| 26.1.2 | 64.1.3 latest, 64.1.0 recommended | | S4 |
| 26.1.1 | 63.0.2 latest (2026-04-08) | | S3 |
| 26.1 | 62.0.9 latest (2026-03-29) | | S2 |
| 1.21.11 | 61.0.3 latest (2025-12-14), 61.0.0 first (2025-12-09) | | S1 |
| 1.7.10 | 10.13.4.1614 | javadoc mirror | S19b |
| 1.12.2 | 14.23.5.2854 and 14.23.5.2859 | javadoc mirror | S19b |
| 1.13.2 | 25.0.107 and 25.0.215 | startup logs | S26 |
| 1.14.4 | 28.1.1 | startup log | S26 |
| 1.2.3 / 1.2.4 / 1.2.5 / 1.1 | 1.4.1.64 / 2.0.0.68 / 3.4.9.171 / 1.3.4.29 | community wiki | S20 |

The Forge major number rises by one per Minecraft release from 26.1 (62, 63, 64, 65, 66). The
scheme was `<minecraft>-<forge>` before (for example `1.21.11-61.0.3`) and the Gradle coordinate
keeps that form (`'26.3-66.0.6'`). NeoForge uses a different four-part scheme, so the Version
Registry needs a separate parser for each loader.

The build 26.3 has only "Latest". Whether VESSEL treats a Latest-without-Recommended build as
stable is a VESSEL decision, see section 15.

### 1.2a Addendum (v0.4.1, 2026-09-29): the official promotions file fills most of the gap

Forge publishes one small official JSON file that lists, for each Minecraft version, the
**latest** build and (only where one exists) the **recommended** build (S50, fetched in full on
2026-09-29). Its format is the same "update JSON" format that mods use (section 18.1). It
covers **77 of the 78** versions. The only version without an entry is `1.7.10_pre4`.

What it changes for section 1.2:
- The count of versions with a known build number rises from **31 of 78** to **77 of 78**
  (latest build known for every version except `1.7.10_pre4`). All values are stored in
  `version-index.json` v0.1.2 under `promotions`.
- It is **not** a full build history. It gives at most two builds per Minecraft version. The full
  per-version build list (for example the 14 builds of 26.2) is still known only for the few
  versions whose index pages were read.
- **40 versions have a recommended build. 37 versions have only a latest build.**

Correction to the table above: the row for **1.21.11** shows `61.0.3`, taken from an older cached
copy of the index page (S1). The promotions file now gives **61.2.1 latest / 61.2.0 recommended**.
Build data goes stale within weeks, which is exactly why the "every VESSEL release refreshes every
loader" rule exists (MASTER-SPEC-STATUS section 2, decision 14). The rows for 1.20.1 (47.4.23
latest, 47.4.10 recommended), 1.19.4, 1.19.2, 1.18.2, 1.16.5, 1.14.4, 1.12.2, 1.9.4 and 1.8.9
in `version-index.json` also had older builds recorded than the promotions file shows. The older
values are kept in `builds_seen`, the current ones are in `promotions`.

Also seen (S55, community mirror, listing only): builds `64.0.15` and `64.0.16` exist for
26.1.2 before `64.1.0`, and `65.1.0` to `65.1.3` exist for 26.2. So the middle number of a 26.x
build (`64.0.x` versus `64.1.x`) is not just a counter and Forge's own recommended builds sit on
`.1.0` for 26.1.2 and 26.2. Why the middle number changes is **unverified**.

### 1.3 Java runtime per Minecraft version (official README, S8)

| Minecraft | Java |
|---|---|
| 26.1 and newer | 25 |
| 1.20.6 to 1.21.11 | 21 |
| 1.18 to 1.20.4 | 17 |
| 1.17.1 | 16 |
| 1.16.5 and older | 8 |

Consequence for VESSEL's bytecode reader: it must read class files from Java 8 up to at least
Java 25, and the 26.2 changelog shows Forge bumping ASM for **Java 27** support (S23). The
class-file limit must be read from the bundled ASM at runtime, never hard coded.

---

## 2. Eras (VESSEL proposal, boundaries partly evidenced)

VESSEL needs one adapter per era, not per version. The era boundaries below come from evidence
where noted. Otherwise they are VESSEL's own grouping. **[Update 2026-10-07 (v0.4.54): the line between E1a and E1b moved from 1.3.2 to 1.2.5 by decision 34 (Shiroz, 2026-10-06); the two rows below and the counts (3 and 20, total 78) are changed accordingly; see 38.1 and 39.6. Pass 26 refined the meaning: at 1.2.5 the Forge line is still ModLoader-based and FML 2.x is a separate loader (40.4).]**

| Era | Minecraft | Count | What defines it | Evidence level |
|---|---|---|---|---|
| E1a | 1.1 to 1.2.4 | 3 | The first Forge releases: Forge on top of a real ModLoader (Risugami's). Forge is a patch set over jars that already contain ModLoader and ModLoaderMP; the mods are ModLoader `BaseMod` mods that use Forge as an API (decision 34; section 38.1) | Evidenced: 1.1 start date (S30); the early Forge tags (S149, 38.1) need a real ModLoader underneath; Forge builds for 1.2.1 to 1.2.4 not read |
| E1b | 1.2.5 to 1.7.10 | 20 | Forge Mod Loader (FML) by cpw as its own project, with its own ModLoader-compatibility layer (FML 1.2.5 to 1.6.4, absent from 1.7), `@Mod` annotation, `mcmod.info` (bare JSON array), coremods via `IFMLLoadingPlugin`, ASM through LaunchWrapper's predecessor. Package `cpw.mods.fml` | Evidenced: FML tags `v2.0` to `v2.9` for 1.2.5 (S148, 38.1), 1.3.2 log (S20), 1.7.10 Javadoc (S19b, S38) |
| E2 | 1.8 to 1.12.2 | 12 | Package renamed to `net.minecraftforge.fml`. LaunchWrapper. `@Mod` plus `mcmod.info`. Pre-init, init, post-init events. Coremods via `IFMLLoadingPlugin` with an added `getAccessTransformerClass` | Evidenced: 1.8.9 tutorial imports, 1.9.4 to 1.11.2 Javadoc mirrors (S19, S19b, S39, S40), 1.12.2 docs |
| E3 | 1.13.2 to 1.16.5 | 12 | ModLauncher, `mods.toml`, `javafml`, `FMLCommonSetupEvent`, capabilities with `@CapabilityInject`, Mixin through ModLauncher | Evidenced: 1.13 to 1.14.4 logs (S26), 1.14.x and 1.16.x docs (S18, S19) |
| E4a | 1.17.1 to 1.20.1 | 11 | Renames to `BlockEntity`, `Level`, `SavedData`. `ForgeConfigSpec`. Java 16 then 17. A major client-code break at 1.19 (41.0.64) | Evidenced: docs 1.17.x to 1.19.2 (S18), porting gist (S29) |
| E4b | 1.20.2 to 1.21.11 | 15 | Java 21 from 1.20.6. Networking rework at 1.20.2 or later. EventBus 7 arrives at some 1.21.x version | Weak: forum post for networking (S17b), EventBus 7 gist with no version (S22). Boundaries unverified |
| E5 | 26.1 to 26.3 | 5 | Unobfuscated Minecraft, Java 25, Gradle 9.1 or newer. IForgeShearable removed at 26.2 | Evidenced: NeoForge primer (S25), Forge changelog (S23) |

The E1b/E2 boundary and the ModLoader compatibility end date have conflicting sources: the
FTB wiki says FML stayed compatible with ModLoader mods until 1.6, while the FML readme text
says that support was dropped as of Minecraft 1.7 (S20). Unresolved. **[Update 2026-10-06 (v0.4.53): resolved by the FML code, see 38.1: support exists through 1.6.4 and is absent from 1.7.10, so the two texts agree.]**

The E1a/E1b split itself is VESSEL's own proposal, not a named era in any source read: it
separates "Forge exists mainly to keep ModLoader mods working" (1.1-1.2.5) from "Forge is FML,
its own project with its own metadata and coremod system" (1.3.2 onward). No source gives an
exact version where this shift completed; it is inferred from the 1.1 start date (S30) and the
first FML-specific evidence beginning at 1.3.2 (S20). **[Update 2026-10-06 (v0.4.53): the FML tags `v2.0` to `v2.9` are for Minecraft 1.2.5 and already contain `@Mod` and the ModLoader layer, and the early Forge tags `1.0.0` to `1.3.2` are a patch set over ModLoader, not a loader (38.1). The proposal to move the E1a/E1b line to 1.2.5 waits for Shiroz; this table is unchanged.]**

---

## 3. Mod metadata by era

### 3.1 E1 and E2 (1.3.2 to 1.12.2): `@Mod` annotation plus `mcmod.info`

Source: 1.12.x docs (S19), Javadocs (S19b).

`@Mod` properties in the 1.12.x docs:

| Property | Type | Default | Meaning |
|---|---|---|---|
| `modid` | String | required | Unique id. Must be lowercase and is truncated to 64 characters |
| `name` | String | `""` | Display name |
| `version` | String | `""` | Version, digits and dots, ideally semantic |
| `dependencies` | String | `""` | Dependency string (grammar below) |
| `useMetadata` | boolean | `false` | When true, values from `mcmod.info` override the annotation |
| `clientSideOnly`, `serverSideOnly` | boolean | `false` | Side restriction |
| `acceptedMinecraftVersions` | String | (empty) | Version range. Empty means only the current Minecraft version |
| `acceptableRemoteVersions` | String | | Replaces the old `versionRange` of `NetworkMod` (1.7.10 Javadoc) |
| `acceptableSaveVersions` | String | | Compatible save version range |

Package: `cpw.mods.fml.common.Mod` in 1.7.10 (S19b), `net.minecraftforge.fml.common.Mod` from 1.8.9
through 1.12.2 (S19, S19b).

Dependency string grammar (1.12.x Javadoc, S19b): entries are separated by `;`. Each entry is a
prefix, `:`, a modid, and an optional `@` version range. Prefixes in the docs: `before`, `after`,
`required-before`, `required-after`. The 1.12.2 Javadoc examples also show `required` and
`required-client`, and say that for compatibility with Forge older than 14.23.0.2501 the older
`required-after` form has to be used (it points to issue 4918). If a required mod is missing or
out of range the game does not start.

`mcmod.info`: a JSON file at `src/main/resources/mcmod.info`. The root is a **bare JSON array**,
one object per modid, so one file can describe several mods. This exact shape is confirmed
identically across 1.7.10 (S25b forum example), 1.10.2/1.12/1.13.x official docs (S19, S19c), and
a 1.7.10 community guide (S25b). Fields confirmed across those sources:

| Field | Type | Default | Meaning |
|---|---|---|---|
| `modid` | string | required | Must match the `@Mod` modid |
| `name` | string | required | User-friendly display name |
| `description` | string | `""` | 1-2 paragraph description |
| `version` | string | `""` | Mod version |
| `mcversion` | string | `""` | Minecraft version. Gradle can substitute `${mcversion}` |
| `url` | string | `""` | Homepage link |
| `updateUrl` | string | `""` | Defined but documented as unused, superseded by `updateJSON` |
| `updateJSON` | string | `""` | URL to a version-check JSON |
| `authorList` (or `authors` in at least one 1.7.10 community example) | array of strings | | Author names |
| `credits` | string | | Free-text thanks |
| `logoFile` | string | | Path to a logo image, resolved on the classpath |
| `screenshots` | array of strings | `[]` | Documented as currently unimplemented |
| `parent` | string | `""` | Another modid this mod is grouped under in the mod list (the docs' own example is BuildCraft) |
| `useDependencyInformation` (also written `useDependencyInformation` as a **string** `"true"` in some real-world examples, S28b) | boolean | `false`/unset | Gates whether `requiredMods`, `dependencies` and `dependants` do anything at all |
| `requiredMods` | array of strings | `[]` | **Hard dependency.** If a listed modid is missing, the game shows an error screen and shuts down. Documented explicitly: this field alone does **not** affect load order |
| `dependencies` | array of strings | `[]` | **Soft, order-only.** Listed mods load before this one if present. If one is absent, nothing happens: no crash, no error |
| `dependants` | array of strings | `[]` | Listed mods load after this one if present. Same no-op-if-absent behavior |

The practical consequence, confirmed by three independent sources (official docs S19, a real
mod's mcmod.info on the Forge forums S28b, and a third-party TypeScript parser's own doc
comments S28c, all agreeing word for word on the semantics): **to make a dependency both
mandatory and order-guaranteed, its modid must appear in both `requiredMods` and
`dependencies`.** Listing it only in `requiredMods` guarantees presence but not order.

A 2015 Forge team proposal (S28a) to replace this format with an npm-`package.json`-style
schema (semantic versioning, a dependency map with version ranges, `repository`/`bugs` fields)
was never implemented; Forge instead moved straight to the TOML-based `mods.toml` at 1.13.

Consequence for VESSEL: the compatibility layer must reproduce the crash-vs-silent-noop split
exactly. A missing `requiredMods` entry is a hard load failure with a clear reason (matches
VESSEL's own "REQUIRED dependency missing -> conversion failure with exact symbol", spec v3
#397); a missing `dependencies`/`dependants` entry is not an error at all, only a possible
ordering difference, and must not be reported as a problem.

### 3.2 E3 and later (1.13.2 onward): `META-INF/mods.toml`

Evidence: 1.13.2 startup logs show a `mods.toml` lookup and mod locators (userdev classpath,
mods folder, maven libs, exploded directory) (S26). 1.14.x docs describe the file (S19).

- Format: TOML. Stored at `META-INF/mods.toml` (S19).
- Loader keys: `modLoader="javafml"`, `loaderVersion="[28,)"` (28 is the Forge major for 1.14.4).
  Another Forge major evidenced in docs: 1.19 is 41 (`loaderVersion="[41,)"`, S19).
- Per-mod table `[[mods]]`, per-mod dependency table `[[dependencies.<modid>]]` with `modId`,
  `mandatory`, `versionRange`, `ordering`, `side`.
- Multiple mods per file are supported (`[[mods]]` is an array of tables).

### 3.3 Key list for the latest documented docs (1.21.x, S9). Confirmed only for 1.19 to 1.21.

- JAR level: `modLoader` (mandatory, `"javafml"` or `"lowcodefml"`), `loaderVersion` (mandatory),
  `license` (mandatory), `showAsResourcePack` (default false), `clientSideOnly` (default false),
  `services`, `properties`, `issueTrackerURL`.
- Per mod: `modId` (regex `^[a-z][a-z0-9_]{1,63}$`), `namespace` (regex `^[a-z][a-z0-9_.-]{1,63}$`,
  docs say currently unused), `version`, `displayName`, `description`, `logoFile`, `logoBlur`,
  `updateJSONURL`, `features` (for example `java_version`), `modproperties`, `modUrl`, `credits`,
  `authors`, `displayURL`, `displayTest`.
- Dependencies: `modId`, `mandatory`, `versionRange` (Maven range), `ordering`
  (`BEFORE`/`AFTER`/`NONE`), `side` (`CLIENT`/`SERVER`/`BOTH`), `referralUrl`.

`clientSideOnly=true` makes Forge skip the mods in that file on a dedicated server (S9).
`modLoader="lowcodefml"` marks a JAR that carries only data packs or resource packs (S9).

Differences between eras that the classifier must handle:
- `modid` rule in E1/E2: lowercase, cut at 64 characters. In E3+ (documented latest): the regex above.
  The regex for 1.13 to 1.18 was not verified.
- Dependency syntax differs completely between the `dependencies` string (E1/E2) and the TOML
  tables (E3+).
- The 1.13.x docs pages mix content: their navigation still lists 1.12-only topics next to the
  1.13 networking API. Treat the 1.13.x docs as unreliable for era boundaries.

---

## 4. Lifecycle by era

### 4.1 E1 and E2 (S19, S20)

Events dispatched to the `@Mod` instance through `@Mod.EventHandler` methods:
`FMLPreInitializationEvent` (read config, create and register blocks and items),
`FMLInitializationEvent` (recipes and similar), `FMLPostInitializationEvent` (talk to other
mods). Related annotations: `@Mod.Instance`, `@SidedProxy`. Listeners are called reflectively
(S20 cleanroom wiki). A 1.3.2 startup log shows the FML sequence: discover coremods, check
library files (argo, guava, asm-all), scan the Minecraft jar for mods, build dummy containers
`FMLDummyContainer` and `ForgeDummyContainer` (S20 gist).

### 4.2 E3 onward (S10, latest docs)

Constructor, `FMLConstructModEvent`, `NewRegistryEvent`, `DataPackRegistryEvent$NewRegistry`,
`RegisterEvent`, `GatherDataEvent`, `FMLCommonSetupEvent`, `FMLClientSetupEvent` or
`FMLDedicatedServerSetupEvent`, `InterModEnqueueEvent`, `InterModProcessEvent`,
`FMLLoadCompleteEvent`. Most events run in parallel across mods. Registry events and
`GatherDataEvent` are synchronous. The full threading table is in v0.1.0 section 4 and is unchanged.

### 4.3 Mapping to VESSEL's universal lifecycle (proposal)

| Forge | Universal stage |
|---|---|
| E1/E2 pre-init | `REGISTER` |
| E1/E2 init | `ENABLE` (early) |
| E1/E2 post-init | `ENABLE` (late) |
| E3+ `RegisterEvent` family | `REGISTER` |
| E3+ `FMLCommonSetupEvent` | `ENABLE` (common) |
| E3+ `FMLDedicatedServerSetupEvent` | `ENABLE` (server) |
| E3+ `FMLClientSetupEvent` | not run on a dedicated server, feeds the presentation compiler |

---

## 5. Sides

- **1.12.x (E2):** side-specific code is reached through `@SidedProxy` (S15).
- **1.13.x to 1.17.x and later:** `DistExecutor` with `runWhenOn` and `callWhenOn`, which avoids
  class loading by taking a supplier and relying on `invokedynamic` (S15). Thread groups can also
  be used to guess the logical side (`SidedThreadGroups.SERVER`, 1.17.x docs).
- **Later docs (S15):** `@EventBusSubscriber` accepts `value = Dist.CLIENT` or `value = Dist.DEDICATED_SERVER`.
  `@OnlyIn(Dist)` marks members stripped from the other physical side.
- Definitions from the docs: physical client is the whole game program, physical server is the
  dedicated server program, logical server runs game logic. FML decides by **physical** side: a
  single-player world is always `Dist.CLIENT`.
- Code in `net.minecraft.client` does not exist on a dedicated server. Referencing it from a
  class that is loaded on the server crashes the game when the class loads.

Consequence for VESSEL: the analyzer must find, for every class, whether it can be loaded on
`Dist.DEDICATED_SERVER`. A class that references `net.minecraft.client` outside a guarded call
(`DistExecutor`, `@OnlyIn`, `@SidedProxy`, a side-restricted subscriber) is client-only.

---

## 6. Registries and correction on duplicates

Source: registries page for latest docs (S14, fetched in full).

- A registry maps `ResourceLocation` keys to objects. Each registrable type has its own registry;
  the `ForgeRegistries` class lists those wrapped by Forge.
- **Names must be unique within one registry. Names in different registries never collide**: a
  block and an item may both be `example:thing`.
- **If two different objects of the same registry get the same name, the second one overrides
  the first.** Forge does not stop the game.
- Two ways to register: `DeferredRegister` (recommended, keeps a list of suppliers and registers
  them during `RegisterEvent`) and `RegisterEvent` (`event.register(ForgeRegistries.Keys.BLOCKS, helper -> ...)`).
- `RegisterEvent` fires once per registry, after the mod constructors and **before configs are loaded**.
- `RegistryObject` and `@ObjectHolder` are the two supported ways to reference registered objects.
  Registered objects must not be cached in fields, because Forge wants to allow dynamic loading and
  unloading of mods in a future version. `RegistryObject` values are refreshed after `RegisterEvent`;
  the 26.2 changelog notes `RegistryObject` equality now also compares its key (S23).
- Dynamic registries (for example `ConfiguredFeature` and other worldgen ones) can only be filled from
  data files (JSON), never from code. Datapack registries cannot be created with `DeferredRegister`,
  only with `DataPackRegistryEvent$NewRegistry`, given a `Codec` (and an optional sync `Codec`).
- Custom registries: `NewRegistryEvent` with `RegistryBuilder`, or `DeferredRegister#makeRegistry`
  (must be called before the register is attached to the mod event bus). Reading the registry
  before `NewRegistryEvent` finishes gives `null`.
- `MissingMappingsEvent` fires on the **Forge** bus. For each missing entry an action can be
  `IGNORE`, `WARN`, `FAIL` (blocks the world from loading) or `REMAP`. The default asks the
  player whether to continue.

Consequences for VESSEL:
1. VESSEL's rule "same key in the same registry stops the server" is **stricter than Forge**. It
   turns a silent override into a clear error. That is intentional and useful, and it must be
   documented as a deliberate difference.
2. Collision checks must be **per registry**. A block and an item with the same name are not a
   collision. `identity-and-collision.md` v0.1.1 states this.
3. Keys built through `DeferredRegister` and computed strings may not be visible to static analysis.
   VESSEL needs a best-effort scan at startup step 7 and a runtime check when each registry is
   filled. The runtime check happens after step 7, so a duplicate found at that point stops the
   server later than the "before the build" promise. Design decision in section 15.
4. `MissingMappingsEvent` is the Forge-side hook for save migration (spec v3 #410).
5. Content that a mod defines only in JSON (dynamic registries) is a pack-compiler concern, not a
   registry-collision concern.

---

## 7. Events

### 7.1 Buses and event features (S22, community wiki and older docs)

- Two buses: the main Forge bus `MinecraftForge.EVENT_BUS` (game state: entities, ticking, server
  events) and the per-mod bus (registration, lifecycle, model and config events).
- `@SubscribeEvent` marks a listener; `EventPriority` orders listeners; a listener can opt in to
  cancelled events with `receiveCancelled`.
- Cancelable events carry `@Cancelable`. Cancelling a non-cancelable event throws an
  `UnsupportedOperationException`.
- Results: `Event.Result` with `ALLOW`, `DEFAULT`, `DENY`, on events annotated `@HasResult`.

### 7.2 EventBus 7 (S22, migration gist by a Forge developer)

A full rewrite of Forge's event system. Key changes, in the gist's own summary:
- `extends Event` becomes `extends MutableEvent`. `@Cancelable` becomes `implements Cancellable`.
  Events can be records implementing `RecordEvent`. Each event class exposes a
  `public static final EventBus<MyEvent> BUS = EventBus.create(MyEvent.class)` (or a
  `CancellableEventBus`).
- Cancelling is done by **returning `true` from the listener** (or `@SubscribeEvent(alwaysCancelling = true)`
  or `addListener(true, this::onEventName)`), not by calling `setCanceled`.
- `EventPriority` is replaced by any number in a Java `byte` (or the `Priority` class).
- `MinecraftForge.EVENT_BUS.register(...)` becomes `BusGroup.DEFAULT.register(MethodHandles.lookup(), ...)`.
- `Event.Result` moves to `net.minecraftforge.common.util.Result`, and `@HasResult` becomes an interface.
- Build changes: an `eventbus-validator` annotation processor and a `strictRuntimeChecks` property.
- The gist says Forge intends to modularize its events in a future Minecraft release.

The gist has no Minecraft version. Its age label is about 481 days, which points to mid-2025, but
the version that introduced EventBus 7 is **unverified**. Whether Forge 26.x uses EventBus 7 is
also unverified.

Consequence for VESSEL: two listener shapes exist for the same idea. The universal event bus
(spec v3 #226-#228) must accept both "call `setCanceled`" (EventBus 6) and "return true"
(EventBus 7), plus Result values, and must preserve ordering (byte priorities versus
`EventPriority`).

---

## 8. Networking by era

| Era | API | Source |
|---|---|---|
| 1.9.4 to 1.12.2 | `NetworkRegistry.INSTANCE.newSimpleChannel(String)` returns `SimpleNetworkWrapper` (package `net.minecraftforge.fml.common.network`). A second factory returns an `FMLEventChannel` | S17b (1.9.4 Javadoc mirror) |
| 1.13 to 1.20.1 | `NetworkRegistry.newSimpleChannel(ResourceLocation, Supplier<String>, Predicate<String>, Predicate<String>)` returns `SimpleChannel` (package `net.minecraftforge.network.simple`). Also `newEventChannel` returning `EventNetworkChannel`. A protocol version string, and version predicates for client and server | S17 (docs 1.13.x, 1.16.x, 1.20.x), S17b |
| 1.20.2 or later | `ChannelBuilder.named(ResourceLocation).simpleChannel()`. `NetworkRegistry` is marked internal. Forum post about Forge 49.0.27 for 1.20.4 | S17b (forum, weak) |
| 26.x | unverified | |

Login handshake: if the version predicates reject the other side's version, FML denies the login.
`acceptMissingOr(...)` makes a predicate accept vanilla peers or peers without the channel (1.20
Javadoc mirror, S17b). The 26.2 changelog also records a fix so that mod packets are not handled
after vanilla packets received in the same tick (S23).

Consequences for VESSEL:
- A mod channel that requires the client to have the channel would deny login for a vanilla or
  Bedrock client. VESSEL must answer the handshake on the server side and route the channel's
  payloads through the universal packet layer, since the client cannot have the mod.
- Packet ordering relative to vanilla packets matters. The 26.2 fix shows ordering is a real
  compatibility concern, so VESSEL's broadcast layer must keep per-connection order.

---

## 9. Persistent data by era

| Era | Mechanism | Source |
|---|---|---|
| E1/E2 | `IExtendedEntityProperties` (old entity data), `WorldSavedData`, config annotations, `PermissionAPI`. Seen in the navigation of the 1.13.x docs pages, which still carry 1.12-era topics | S18 |
| E3 (1.13 to 1.16) | Capabilities: `ICapabilityProvider` with `hasCapability`/`getCapability`, `@CapabilityInject`, `Capability$IStorage`, `AttachCapabilitiesEvent<TileEntity/Entity/ItemStack/World/Chunk>`, `ICapabilitySerializable` for persistence. `WorldSavedData` | S18 |
| E4a (1.17+) | Same ideas with renamed types: `AttachCapabilitiesEvent<BlockEntity/Entity/ItemStack/Level/LevelChunk>`, `SavedData` | S18 |
| all | `Item#initCapabilities` for item stacks. Forge-provided capabilities: `IItemHandler`, `IFluidHandler`, `IEnergyStorage` (1.19.2 docs) | S18 |

Saved data storage: created through `DimensionDataStorage#computeIfAbsent(load function, supplier, name)`,
reachable from `ServerChunkCache#getDataStorage` or `ServerLevel#getDataStorage`. The file lands in the
level's `data` folder, for example `<level_folder>/DIM-1/data/example.dat` for the Nether. `setDirty` must be
called after a change or `save` is never invoked (S18).

Docs also warn that capability checks run many times per tick and must stay fast.

Consequences for VESSEL: these map to `UniversalPersistentData` (spec v3 #149). Per-object data
(capabilities) belongs with the object, per-level data (saved data) belongs in the world save.
VESSEL must not store either in `data/forge/<id>/` files (spec v3 #150).

---

## 10. Configuration

Unchanged from v0.1.0 section 5, plus:
- Forge config types CLIENT, COMMON, SERVER with paths and sync as listed there (S11).
- The registries page says `RegisterEvent` fires before configs load, so **registered content cannot
  depend on config values** in Forge either (S14).
- The Startup Build Stage consequence stays as in v0.1.0 section 5.3.

---

## 11. Files and paths

Evidence for the loader's own files, from a Forge 1.14.4 (28.1.1) dedicated server startup log (S26):
- `FMLPaths` has a `FMLCONFIG` path resolving to `config/fml.toml`. The loader has its own config.
- The loader looks for a default config directory and logs "Found existing default config
  directory" for `defaultconfigs` in the game folder.
- The launch target is named `fmlserver` and ModLauncher 3.2.0 is the launcher.

Still unverified: the copy behavior of `defaultconfigs` into `world/serverconfig`, and the other
`FMLPaths` constants (`GAMEDIR`, `MODSDIR`).

---

## 12. Class transformation

### 12.1 E1 and E2

- Coremods and ASM: the 1.3.2 log shows coremod discovery and ASM libraries (S20).
- LaunchWrapper is the launcher (the 1.8.9 classpath shows `launchwrapper-1.12` and ASM 5.0.3, S20).
  Mixin is started from an `ITweaker` (`MixinBootstrap`, `Mixins.addConfiguration`) (S21). A
  third-party library (MixinBooter) covers 1.8 to 1.12.2 and reads `MixinConfigs` and
  `MixinConnector` attributes from the JAR manifest (S21).

### 12.2 E3 onward

(Numbering note: section 12.3 was renumbered to 12.6 during a v0.4.0 edit to make room for two
new subsections; nothing was removed, only reordered.)

- ModLauncher is the launcher. A 1.13 pre-release log lists its launch plugins:
  `eventbus`, `object_holder_definalize`, `runtime_enum_extender`, `accesstransformer`,
  `capability_inject_definalize`, `runtimedistcleaner` (S26). The `runtimedistcleaner` plugin is
  probably the one that removes client-only members on a server (inferred from the name only).
- Mixin on Forge runs as a ModLauncher service (SpongePowered README: LegacyLauncher is
  deprecated in favor of ModLauncher, S21). Build side: MixinGradle with ForgeGradle 3.0 or newer.
- A Forge Mixin config is normally `<modid>.mixins.json` with the keys `required`, `package`,
  `compatibilityLevel`, `refmap`, `mixins`, `client`, `minVersion`. `compatibilityLevel` is
  `JAVA_8` for 1.16 and older and `JAVA_17` for 1.18 and newer (S21, one article). The refmap
  (`<modid>.refmap.json`) is generated by the annotation processor.
- A third-party mod, BlendIn, replaces Forge's Mixin with Fabric's maintained fork on Forge
  1.21.1 (S21). This shows that the Mixin implementation on a given Forge server can differ.

### 12.4 Coremods and `IFMLLoadingPlugin` (E1b through E2, before ModLauncher)

Before ModLauncher (E3+), class transformation ran through LaunchWrapper (E2) and its
predecessor (E1b), using a coremod interface. Confirmed identically across Javadoc mirrors for
1.7.10, 1.9.4, 1.10.2 and 1.11.2 (S38, S39, S40), plus two independent tutorial write-ups (S33, S35):

- **Declaration:** a `META-INF/MANIFEST.MF` attribute `FMLCorePlugin: <fully-qualified class name>`
  names the coremod's entry point. An optional `FMLCorePluginContainsFMLMod: true`/`false`
  attribute tells FML whether to also scan the same JAR for an ordinary `@Mod`-annotated class;
  if explicitly `false`, FML skips that scan (S35).
- **Interface `IFMLLoadingPlugin`** (package `cpw.mods.fml.relauncher` through 1.7.10, then
  `net.minecraftforge.fml.relauncher` from 1.8.9 onward, S38/S39/S40), five methods:
  - `String[] getASMTransformerClass()` — the actual point of a coremod: a list of fully
    qualified class names, each implementing `IClassTransformer`.
  - `String getModContainerClass()` — optional (nullable), a `ModContainer` implementation so
    the coremod also shows up as an entry in the in-game mod list.
  - `String getSetupClass()` — optional (nullable), an `IFMLCallHook` implementation run once,
    on the main thread, for extra setup.
  - `void injectData(Map<String, Object> data)` — receives a data map documented (S38) to
    contain `mcLocation` (the Minecraft directory), `coremodList` (the list of coremods) and
    `coremodLocation` (the file this coremod loaded from).
  - `String getAccessTransformerClass()` — optional (nullable), added from 1.9.4 onward in the
    Javadoc mirrors read (present in S38 1.7.10, S39 1.9.4, S40 1.10.2, and an 1.11.2 mirror
    S41). Its own Javadoc says it is injected **after deobfuscation**, so its AT entries must
    use the post-deobfuscation SRG naming scheme, unlike a normal mod's AT file.
- An optional `@Name("...")` annotation labels the coremod for logs and diagnostics (S35).
- **`IClassTransformer.transform(String name, String transformedName, byte[] basicClass)`**:
  LaunchWrapper calls this once for every class right before it is defined into the
  classloader, passing the original bytecode; the transformer must return the (possibly
  modified) bytecode. `name` is the original class name and `transformedName` is the
  deobfuscated (MCP) name (S35's own explanation, paraphrased).

**Operational hazard, stated directly by an experienced coremod author (S32, paraphrased):**
classloading is normally transparent, but a coremod and its transformer classes break that
transparency, because the transformer itself must already be loaded (and thus not itself be a
transform target) before it can transform anything else. The guidance given is to isolate
coremod-only code in its own package, and to list that package in `TransformerExclusions`, and
never to reach from that package into anything the transformer might itself be asked to
transform. The source could not find an official, canonical description of
`TransformerExclusions`; it is documented here only as community guidance (S32).

Consequence for VESSEL: the bytecode analyzer (spec v3 #163, #172) must treat pre-ModLauncher
coremods as first-class citizens for E1b/E2, not just an edge case: it needs to (1) read
`FMLCorePlugin` from the manifest, (2) resolve and run (or statically emulate) every class
named by `getASMTransformerClass`, (3) apply `getAccessTransformerClass` entries using SRG
names specifically for this AT (potentially different naming timing than a mod's declared AT
file), and (4) never attempt to statically pre-load a class that a coremod's own transformer
package needs to remain untouched, mirroring the exclusion hazard above.

### 12.5 ModLauncher's transformation services (E3 onward, replacing coremods' `IClassTransformer`)

ModLauncher (from 1.13) replaces the LaunchWrapper coremod mechanism with a different,
service-based system. Source: a full Chinese-language technical walkthrough of ModLauncher's
own source code (S42, read in full) plus multiple real startup logs spanning 2020 to 2024
(S26, S43-S47).

**Startup sequence** (paraphrased from S42's description of `Launcher`'s constructor and `run`
method): `Launcher` builds a `TransformStore` and a `TransformationServicesHandler`. At run
time it initializes transformation services, builds a `TransformingClassLoader`, sets it as the
thread's context classloader, then launches the game through that loader.

**Discovery:** `TransformationServicesHandler` uses `ServiceLoader` to find every
`ITransformationService` implementation, listed by the mod in
`META-INF/services/cpw.mods.modlauncher.api.ITransformationService` (one fully qualified class
name per line). Each found service is wrapped in a `TransformationServiceDecorator`.

**`ITransformationService` interface** (S42's own description of its methods, paraphrased):
- `name()` — the service's identifying name.
- `arguments(...)` — registers command-line arguments the service wants to read, exposed as
  `<serviceName>.<argumentName>`.
- `argumentValues(...)` — receives the parsed argument results.
- `initialize(IEnvironment)` — the docs explicitly warn: never call into Minecraft, another
  mod, or even your own mod's ordinary (non-service) code from here, because doing so forces
  those classes to load too early and breaks things.
- `onLoad(IEnvironment, Set<String> otherServices)` — called when the service loads, can throw
  `IncompatibleEnvironmentException`.
- `transformers()` — returns the list of `ITransformer` instances this service provides.

**`ITransformer` interface**, described as having two key differences from LaunchWrapper's
`IClassTransformer`: it operates on **ASM tree nodes** (a `ClassNode`, `MethodNode`, etc. —
generic type `T`) instead of raw `byte[]`, and it must **declare its targets up front** rather
than deciding inside `transform` whether to act. Methods:
- `transform(T input, ITransformerVotingContext context)` — returns the (possibly modified) node.
- `castVote(ITransformerVotingContext)` — returns a `TransformerVoteResult`: `YES` runs
  `transform`, `NO` skips this transformer for that target, `REJECT` throws
  `VoteRejectedException`, `DEFER` throws `VoteDeadlockException`.
- `targets()` — the set of `Target`s (for example `Target.targetClass("abc")`) this transformer
  applies to; only classes appearing here ever reach `castVote`/`transform`.
- Optional (default methods): `arguments(...)`, `argumentValues(...)`, and two
  `additionalClassesLocator`-style methods that let a service add extra classpath/resource
  locations to the classloader.

**How transforms actually run:** `TransformingClassLoader` (a `ClassLoader` subclass wrapping a
`URLClassLoader`-based `DelegatedClassLoader`) intercepts `loadClass`, reads the raw class
bytes, and calls a `ClassTransformer` which in turn invokes each applicable `ITransformer`.

**Installation without Forge:** ModLauncher can be used standalone by pointing a version JSON's
`mainClass` at `cpw.mods.modlauncher.Launcher` and listing ModLauncher plus the mod's service
JAR under `libraries`. **From Forge 1.13.2-25.0.216 onward, Forge itself can load a
ModLauncher-based transformation service placed in `mods/`**, whether or not that JAR also
contains an ordinary `@Mod` (S42's own installation section).

**Built-in launch plugins actually seen in real startup logs, by date (S26, S43-S47):**

| Date / Forge / MC (from the log) | Launch plugins found |
|---|---|
| 2020-05-02, FML 31.1 | `eventbus, object_holder_definalize, runtime_enum_extender, accesstransformer, capability_inject_definalize, runtimedistcleaner` |
| 2020-08-20 | `mixin, eventbus, object_holder_definalize, runtime_enum_extender, capability_inject_definalize, accesstransformer, runtimedistcleaner` |
| 2020-10-07 (modpack with OptiFine + MixinBootstrap) | `eventbus, object_holder_definalize, runtime_enum_extender, accesstransformer, capability_inject_definalize, runtimedistcleaner` (note: no `mixin` plugin here, but transformer *services* found were `OptiFine, fml, FakeOptiFine, mixinbootstrap, optiforge`) |
| 2020-11-21, FML 34.1 | `mixin, eventbus, object_holder_definalize, runtime_enum_extender, capability_inject_definalize, accesstransformer, runtimedistcleaner` |
| 2021-05-24, ModLauncher 5.1.2 | same six-plus-mixin set as above |
| 2024-05-09 (a Forge userdev/dev build, multiple `forge_*` launch services) | `mixin, eventbus, slf4jfixer, object_holder_definalize, runtime_enum_extender, capability_token_subclass, accesstransformer, runtimedistcleaner` |

Two changes visible directly from comparing these logs, not inferred: `slf4jfixer` is a new
plugin present in the 2024 log but absent from every 2020-2021 log read, and
`capability_inject_definalize` (2020-2021) is replaced by `capability_token_subclass` in the
2024 log. Neither change's exact introduction version is known; only that it happened somewhere
between 2021 and 2024 for this project. The 2020-10-07 log's absent `mixin` plugin, next to
`mixin`'s presence in every other log, is most likely explained by that server not having
SpongePowered Mixin's own launch plugin registered (it used MixinBootstrap instead, a
third-party compatibility shim, per S21) rather than a Forge-version difference; this is an
inference, not confirmed.

Consequence for VESSEL: the ASM-tree-based `ITransformer` model is fundamentally different from
LaunchWrapper's raw-bytecode `IClassTransformer` (section 12.4). VESSEL's bytecode analysis
needs two separate code paths for E1b/E2 versus E3+, and for E3+ specifically must resolve
`META-INF/services/cpw.mods.modlauncher.api.ITransformationService` to find transformer entry
points, mirroring how it already must resolve Java's standard `ServiceLoader` files elsewhere
(spec v3 #292). The exact launch-plugin set is version-dependent and must be read live from
each real installation rather than hard-coded, matching spec v3's own capability-profile
philosophy (#569-#570).

### 12.6 Access transformers (S16)

- File: `META-INF/accesstransformer.cfg`. Declared in the mod project's `build.gradle` (older docs)
  and, from newer docs, also in `mods.toml` through `[[accessTransformers]]` with a `file` key. In a
  production JAR the 1.16.x docs say Forge only looks at the exact path
  `META-INF/accesstransformer.cfg`. Multiple AT files can be listed and are applied in order.
- Syntax: `<modifier> <class> [<member>]`. Modifiers: `public`, `protected`, `default`, `private`.
  `+f` or `-f` appended adds or removes `final` (`public-f`, `protected-f`). Comments start with `#`.
  Method targets use a JVM descriptor (`I`, `J`, `Z`, `[` and so on).
- Member names: in 1.19 to 1.20 docs they are SRG-style ids (`f_129758_`, `m_137477_(...)`),
  older docs use `field_71308_o`, `func_71260_j`. The specification is in the Forge
  `AccessTransformers` repository (`FMLAT.md`).
- Only widening is supported. Making a member less accessible is not (Sponge docs).

Consequence: VESSEL's transformer analysis (spec v3 #170-#171) must parse AT files per era, handle
both SRG names and (from 26.1, unobfuscated) plain Mojang names, and apply widening. Any AT target
it cannot resolve is an unmapped symbol (see `compatibility-and-release-policy.md`).

---

## 13. Forge 26.x evidence (changelogs from Forge's own maven, S23)

The changelog for 26.2 is short and worth knowing in full because it is direct primary evidence:

| Build | Entry (paraphrased) |
|---|---|
| 65.0.0 | 26.2 first build |
| 65.0.1 | `IForgeShearable` removed. Entities use vanilla's `Shearable`; blocks use loot table conditions or `useItemOn` when they need position |
| 65.0.2 | "ForgeDev 7" |
| 65.0.3 | Graphics-backend awareness for window providers; early loading screen disabled with Vulkan |
| 65.0.4 | TOML parsing errors now visible in the latest log |
| 65.0.5 | Delta tracker context added to render pass definitions |
| 65.0.6 | Graceful handling of GLFW window-position errors on Wayland |
| 65.0.7 | `RegistryObject` equality now includes its key |
| 65.0.8 | Mod list screen selection fix |
| 65.0.9 | Dependencies bumped, notably ASM for Java 27 support |
| 65.1.0 | "26.2 RB 1" (the recommended build; "RB" presumably means Recommended Build, an inference) |
| 65.1.1 | Fix: mod packets processed after vanilla packets when received in the same tick |
| 65.1.2 | `TooltipContext` and `TooltipDisplay` added to `ItemTooltipEvent` |
| 65.1.3 | Fix to `getToolModifiedState` for `AXE_SCRAPE` |

Other 26.x evidence:
- Minecraft 26.1 is unobfuscated (official Mojang names), the JDK moved from 21 to 25, and Gradle 9.1.0
  or newer is needed (NeoForge blog and primer, S25). Fabric's docs add that no mod built for
  1.21.11 or older works on 26.1, not even as a compile-only dependency (S25).
- So for a 26.x Forge mod, bytecode references use plain Mojang names. For older versions, class and
  member names were remapped (SRG ids in 1.16 to 1.20 era ATs). VESSEL's mapping layer must convert
  per era (spec v3 #402-#406).

Not found: the changelogs for 26.1 and 26.3 (their links never appeared in a page this research
could open), and any Forge documentation page for 26.x.

The changelog format is regular (`<MC> Changelog`, `<major>.<minor>` heading, ` - <build> <text>`),
so VESSEL's release process could parse it to produce per-release API diffs (spec v3 #571).

---

### 13a Addendum (v0.4.2, 2026-09-29): the 26.3 changelog

The official changelog of build 66.0.5 (S52, fetched in full) lists the whole 66.0 line so far:

| Build | Change (paraphrased) |
|---|---|
| 66.0.0 | Initial update to Minecraft 26.3 |
| 66.0.1 | Fix players not drowning correctly |
| 66.0.2 | Fix players dropping items (closes issue 10885) |
| 66.0.3 | Migrate to a fully declarative ForgeGradlePlugin and a local `build-logic` |
| 66.0.4 | Fix a typo in the `BlockScanUtils` patch, which fixes placing cushions (issue 10889) |
| 66.0.5 | Fix entities not being pushed by fluids |

Reading for VESSEL:
- Every fix so far is a **gameplay-behavior patch to Minecraft code**, not an API change. That
  matches the pattern that Forge's 26.x work at this stage is mostly restoring vanilla behavior
  that its own patches broke. It does not say anything about API stability.
- Forge patches Minecraft classes directly (there is a patch named `BlockScanUtils`). A mod that
  depends on vanilla behavior can therefore see it change between two Forge builds of the same
  Minecraft version. VESSEL's per-loader version tracking must key on the **Forge build**, not only
  on the Minecraft version.
- The official index page for 26.3 (S51, search excerpt) lists builds 66.0.4, 66.0.5, 66.0.6 and
  **66.0.8** (dated 2026-09-28) with 66.0.8 as Latest. There is no 66.0.7 on that excerpt. The
  promotions file (S50) still said 66.0.6 at fetch time. See section 18.2.

## 14. What each finding means for VESSEL (summary)

| # | Finding | Effect on VESSEL |
|---|---|---|
| 1 | 78 official versions from 1.1 to 26.3 | Forge version registry has 78 entries, grouped into 7 eras, one adapter per era |
| 2 | Two metadata systems (`mcmod.info` + `@Mod`, then `mods.toml`) | Classifier reads both, produces the same module identity |
| 3 | `lowcodefml` JARs carry only packs | Treated as pack sources, not code modules |
| 4 | `clientSideOnly=true` | Skip on server, keep assets for the bundle |
| 5 | Registry duplicates silently override in Forge | VESSEL's hard stop is intentional and stricter, checks are per registry |
| 6 | `RegisterEvent` runs before configs load | Built content cannot rely on config values |
| 7 | Most lifecycle events are parallel | Explicit per-thread module context |
| 8 | Two listener models (EventBus 6 and 7) | Universal bus supports both cancel styles |
| 9 | Mod network channels reject vanilla peers by default | VESSEL answers handshakes and re-routes payloads |
| 10 | Capabilities and saved data | Map to universal persistent data, never to module files |
| 11 | ATs use SRG ids before 26.1, plain names after | Per-era mapping tables |
| 12 | ASM is bumped for Java 27 already | Bytecode reader limit read from ASM at runtime |
| 13 | Removed APIs exist across versions (`IForgeShearable` at 26.2) | Symbol tables per version, an unmapped symbol crashes with a reason |
| 14 | Forge has always existed from Minecraft 1.1 onward; there is no pre-1.1 Forge | Version registry's lower bound for Forge is fixed at 1.1, no open question remains here |
| 15 | `mcmod.info`'s `requiredMods` crashes on missing, `dependencies`/`dependants` silently no-op on missing and only affect order | VESSEL's compatibility layer must reproduce this exact split, not report a missing soft dependency as an error |
| 16 | Pre-ModLauncher coremods (`IFMLLoadingPlugin`, `IClassTransformer`) are a distinct, essential mechanism for E1b/E2, separate from ModLauncher's later transformation services | Bytecode analysis needs an E1b/E2-specific coremod path, including the self-exclusion hazard for a coremod's own transformer package |
| 17 | ModLauncher's `ITransformationService`/`ITransformer` operate on ASM tree nodes with declared targets, a different model from raw-bytecode `IClassTransformer` | A second, separate E3+ bytecode analysis path, keyed off `META-INF/services/cpw.mods.modlauncher.api.ITransformationService` |
| 18 | The built-in launch-plugin set changes across Forge versions (`slf4jfixer`, `capability_token_subclass` appear later) | The plugin/transformer set must be read live per installation, never hard-coded |
| 19 | Forge and NeoForge have diverged again, this time on the block registration API itself (`RegistryObject` vs `DeferredBlock`) | Confirms `forge` and `neoforge` need fully separate adapters, not a shared one with small branches |

---

## 15. Still missing for Forge (why status is PARTIAL)

Resolved since v0.3.0:
- ~~ModLauncher and coremods in E3+: service files, transformation services~~ — done, section 12.5.
- ~~The built-in launch-plugin/transformer-service set~~ — done, section 12.5 (read live per
  version, five real-world snapshots from 2020 to 2024 compared).
- A first slice of the gameplay API (block registration only) is done, section 16 — this only
  partially resolves item 11 below, which otherwise remains open.

Forge pass 4 must still cover:
1. **[Confirmed absent 2026-09-29, section 19]** **No official Forge docs for 26.x** were found. The site's version selector stops at 1.21.x. The
   section 3.3 keys, sides, events, networking and the new section 16 block API are confirmed only
   up to 1.21.x. The 26.1 and 26.3 changelogs were not read.
2. **[Mostly resolved in v0.4.1, section 1.2a: 77 of 78 versions now have a latest build, 40 have a recommended build. Still open: the full per-version build history, and 1.7.10_pre4]** **Build numbers for 47 of 78 versions**, and their per-version support status.
3. **`ITweaker` use** in LaunchWrapper (E2) and its E1b predecessor, and the precise
   ModLoader/BaseMod compatibility layer Forge E1a provided (how a ModLoader `mod_*` class ran
   unmodified under early Forge, if that is in fact how it worked; this framing is still an
   inference, see section 2's note on the E1a/E1b split). **[Pass 24, v0.4.53: read for FML 1.2.5 to 1.6.4, partly; see 38.1 and 38.6.]**
4. **E2 details**: full LaunchWrapper flow beyond coremods, `@SidedProxy` semantics in full,
   `FMLInterModComms`. **[Pass 24, v0.4.53: read, see 38.2 and 38.6.]**
5. A `mods.toml` mixin table (`[[mixins]]`, unverified), JavaScript coremods (unverified), `fml.toml`
   keys, and how (or whether) E3+ kept any `IFMLLoadingPlugin`-style compatibility path.
6. **Resources and packs**: `showAsResourcePack`, how mod resources become packs, `pack.mcmeta`
   for mods, conditional loading, global loot modifiers, tags.
7. **`defaultconfigs` copy behavior** and the other `FMLPaths` constants.
8. **[Resolved in v0.4.3, section 18.1 to 18.4]** **Version registry details**: how Forge marks a build as recommended or latest (the changelog
   shows "RB"), and the versioning convention page.
9. Javadoc inventory and source repository revision per era, mod update checker JSON format. **[Update checker JSON format resolved in v0.4.3, section 18.5. Javadoc inventory and source revisions still open.]**
10. Threading rules per era for capabilities, entity data and packets. **[Pass 24, v0.4.53: read, see 38.3 and 38.6.]**
11. **The rest of the gameplay API**: items, entities, block entities, commands, the scheduler,
    rendering hooks, recipes, loot tables, tags, worldgen, data generation. Only block
    registration itself is done (section 16). **[Pass 22, v0.4.51: entities and commands are read (section 36), the scheduler partly. Pass 23, v0.4.52: worldgen and data generation are read (section 37). Pass 25, v0.4.54: the leftovers of rendering hooks, recipes, loot tables, the scheduler and items are read (section 39). Pass 26, v0.4.55: the last PARTIAL rows are read (section 40). Items of this list that are outside the 37 rows stay open.]**
12. Whether the `mcmod.info` `authors` (versus `authorList`) variant seen in one 1.7.10 forum
    example (S25b) is a real alternate field name or a mistake made by that guide's author; only
    one low-authority source shows it, against three higher-authority sources showing `authorList`.
13. Whether Forge itself ever renamed the block-property setter `strength(...)` to
    `destroyTime(...)`/`explosionResistance(...)`, or whether that naming is NeoForge-only and
    Forge kept `strength` (section 16.1) — the only comparison available was Forge 1.19.2 against
    NeoForge, not two Forge versions against each other.

Open design questions for Shiroz (also asked in chat, still unanswered):
- Is a Latest-without-Recommended build (26.3 on Forge) treated as stable?
- A duplicate registry key found at runtime (not statically) stops the server at that moment: acceptable?

**Answered by Shiroz on 2026-09-29:**
- A Latest-without-Recommended build is **not** stable (consequences in section 18.3).
- Duplicate registry keys: VESSEL scans the whole module set before anything loads. If any duplicate
  exists it prints the error and **does not start at all**. Only when no duplicate exists does loading
  begin, and only then are render bundles built for joining players. So the runtime-only case above
  is replaced by a full up-front scan. Keys that only exist at runtime (for example `DeferredRegister`
  names computed in code) cannot always be seen by a static scan; **decided on 2026-09-29 (option C):**
  static scan first, then an isolated dry run of the registration step for modules that build names in
  code, with clear console reporting. Details in `identity-and-collision.md` v0.1.3 section 5.1.

---

## 16. Gameplay API: blocks (first pass, E3+ only)

Source: three snapshots of Forge's own `Documentation` GitHub repository at branches `1.18.x`,
`1.20.x` and `1.21.x` (S48, all fetched as search excerpts, not full pages), plus the already-read
registries page (S14) and, for comparison only, NeoForge's docs at three versions (S49).

### 16.1 Registering a block: unchanged API across three major versions

The exact same code pattern is shown, word for word, in the Forge `Documentation` repo at
1.18.x, 1.20.x and 1.21.x:

```java
private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
public static final RegistryObject<Block> ROCK_BLOCK = BLOCKS.register("rock",
    () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
```

with registration completed in the mod constructor: `BLOCKS.register(context.getModEventBus())`
(1.20.x/1.21.x, taking a `FMLJavaModLoadingContext context` constructor parameter) or
`BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus())` (1.18.x, using the static
`get()` accessor instead of a constructor parameter). This constructor-signature difference
matches the two inconsistent forms already noted in section 3 (S9 vs S10) and confirms it is a
real difference across versions, not a documentation typo in one place only: older docs use the
static `FMLJavaModLoadingContext.get()`, newer docs pass the context into the constructor.

An older Forge docs snapshot (1.19.2, S21-adjacent excerpt) shows the *pre-DeferredRegister*
style still being documented side by side: instantiate `Block` directly with a
`BlockBehaviour$Properties` built from `BlockBehaviour.Properties#of()`, customized with methods
whose names differ by version — 1.19.2 documents `strength(...)`, while the NeoForge 1.21.x docs
(S49) document `destroyTime(...)` and `explosionResistance(...)` as the current property-setter
names. Whether Forge itself (not NeoForge) renamed `strength` to `destroyTime`/`explosionResistance`
at some point, or whether Forge kept `strength` throughout, is **unverified** — the only side-by-side
comparison available in this research is Forge 1.19.2 against NeoForge, two different projects,
not two Forge versions.

Reference example values given in the NeoForge docs (kept here because they are concrete,
version-independent gameplay constants likely to still hold for Forge): stone has a destroy time
of 1.5, dirt 0.5, obsidian 50, bedrock -1 (unbreakable); stone's explosion resistance is 6.0, dirt
0.5, obsidian 1200, bedrock 3,600,000.

### 16.2 Forge and NeoForge have diverged on the block registration API

NeoForge 1.21.x documents a **different, NeoForge-only** API: `DeferredRegister.Blocks`,
created with `DeferredRegister.createBlocks("yourmodid")` instead of the generic
`DeferredRegister.create(...)`, whose `#register` returns a `DeferredBlock<T extends Block>`
(itself extending `DeferredHolder<Block, T>`) instead of Forge's `RegistryObject<Block>`. NeoForge
also documents a `MapCodec`-based "block type" registration path
(`BuiltInRegistries.BLOCK_TYPE`, `BlockBehaviour#simpleCodec`,
`BlockBehaviour#propertiesCodec`) that has no Forge equivalent in any source read here.

This is a second, independent confirmation (alongside the version-scheme and config-timing
differences already in sections 1 and 5) that **Forge and NeoForge cannot share one adapter**,
even though they are historically the same codebase split apart. VESSEL's `forge` and
`neoforge` source-loader adapters must be maintained as genuinely separate implementations from
this point in their history onward, not as one adapter with minor per-loader branches.

### 16.3 What this first pass does not cover

This is a first pass on one gameplay area (blocks) chosen because it was already half-visible
through the registries research. It is not a survey of the gameplay API as a whole. Not covered
at all yet: items, entities, block entities, commands, the scheduler, rendering hooks, recipes,
loot tables, tags, worldgen, or any Forge-specific data generation. Section 15's item 11 (the
gameplay API is unresearched) is only very partially addressed by this section, not resolved.

---



## 17. Sources

Authority: A official Forge, B official neighbour project, C community or third party.
"Fetched" means the whole page was opened. "Excerpt" means only a search excerpt was read.

| Id | Auth | How read | Source | Used for |
|---|---|---|---|---|
| S1 | A | fetched (older snapshot) | files.minecraftforge.net main page | 1.21.11 builds, old line list |
| S2 | A | excerpt | files.minecraftforge.net .../index_26.1.html | 26.1 builds |
| S3 | A | excerpt | .../index_26.1.1.html | 26.1.1 builds |
| S4 | A | excerpt | .../index_26.1.2.html | 26.1.2 builds |
| S5 | A | fetched in full | .../index_26.2.html | 26.2 builds, full 78-version index |
| S6 | A | excerpt | files.minecraftforge.net (26.3 page) | 26.3 builds |
| S7 | C | excerpt | github.com/42atomys/mc-dynamic-pack-manager v3.0.0-26.1.2 | Third-party drop of Forge for 26.x |
| S8 | A | excerpt | github.com/MinecraftForge/MinecraftForge README | Java runtime per version |
| S9 | A | fetched | docs.minecraftforge.net/en/latest/gettingstarted/modfiles/ | `mods.toml` keys |
| S10 | A | fetched | docs.minecraftforge.net/en/latest/concepts/lifecycle/ | Lifecycle |
| S11 | A | fetched | docs.minecraftforge.net/en/latest/misc/config/ | Config types |
| S12 | B | excerpt | docs.neoforged.net misc/config (1.20.x, 1.21.x) | Config timing (comparison) |
| S13 | C | excerpt | forums.minecraftforge.net topic 87323 | `FMLPaths.CONFIGDIR` sample |
| S14 | A | fetched | docs.minecraftforge.net/en/latest/concepts/registries/ | Registries, duplicates, missing mappings |
| S15 | A | excerpt | docs.minecraftforge.net concepts/sides (latest, 1.12.x, 1.13.x, 1.14.x, 1.16.x, 1.17.x) | Sides |
| S16 | A | excerpt | docs.minecraftforge.net advanced/accesstransformers (latest, 1.16.x); github.com/MinecraftForge/AccessTransformers FMLAT.md | AT format |
| S17 | A | excerpt | docs.minecraftforge.net networking/simpleimpl (latest, 1.13.x, 1.16.x, 1.20.x) | SimpleImpl |
| S17b | C | excerpt | Javadoc mirrors (1.9.4 `NetworkRegistry`, 1.20-46.0.14 `NetworkRegistry` and `SimpleChannel`, 1.20.4 `NetworkInstance`), forum topic 148477 | Network API by era |
| S18 | A | excerpt | docs.minecraftforge.net datastorage capabilities and saveddata (1.13.x to 1.19.2), MinecraftForge/Documentation 1.15.x | Capabilities, saved data |
| S19 | A | excerpt | docs.minecraftforge.net gettingstarted/structuring (1.12.x, 1.13.x, 1.14.x), modfiles (1.19.x) | `mcmod.info`, `@Mod`, early `mods.toml` |
| S19b | C | excerpt | Javadoc mirrors: `Mod` for 1.7.10, 1.12.2; `NetworkRegistry` 1.9.4 | `@Mod` properties, builds |
| S20 | C | excerpt | FTB wiki (Minecraft Forge, Forge Mod Loader), Packt history article, instantmods.com, 1.3.2 and 1.8.9 log gists, FML readme text in a repository commit, cleanroom wiki | History, early eras, first builds |
| S21 | B/C | excerpt | github.com/SpongePowered/Mixin, SpongePowered/MixinGradle, MixinBooter (CurseForge), darkhax.net mixin article, BlendIn (CurseForge), Sponge forum threads | Mixin on Forge |
| S22 | B/C | excerpt | EventBus 7 migration gist by PaintNinja, forge.gemwire.uk Events wiki, docs.neoforged.net events | Events |
| S23 | A | fetched | maven.minecraftforge.net .../26.2-65.0.0 and 26.2-65.1.3 changelog.txt | 26.2 changes |
| S24 | C | excerpt | docs.architectury.dev api/port-26.1-26.2 | Ecosystem dropped Forge for 26.2 |
| S25 | B | excerpt | neoforged.net/news/26.1release, docs.neoforged.net/primer/docs/26.1, docs.fabricmc.net porting 26.1 | 26.1 unobfuscation, Java 25 |
| S26 | C | excerpt | Startup log gists for Forge 1.13-pre, 1.13.2, 1.14.4 | ModLauncher, launch plugins, `mods.toml`, `fml.toml`, `defaultconfigs` |
| S27 | C | excerpt | maven mirror listing (forge maven-metadata) | 1.14.2, 1.21 and 26.1.2 build ids |
| S28 | C | excerpt | gist of Forge installer links | 1.18.2 to 1.20.3 builds |
| S29 | C | excerpt | gist "Forge 1.19 porting" | 1.19 (41.0.64) client break |
| S30 | C | excerpt | g-portal.com wiki "Minecraft Forge – Old but Gold" | Forge first released for Minecraft 1.1 in 2011; NeoForge split context |
| S31 | C | excerpt | ftb.fandom.com "Risugami's Modloader"; Hypixel and Minecraft Forum threads on Risugami's ModLoader; archive.org ModLoader download; a ModLoader crash log gist | ModLoader as a separate Beta-era project, `BaseMod`, `mod_` prefix, FML as its cleanroom rewrite |
| S32 | C | excerpt | gist "Psst, hey kid, wanna write a coremod?" (quat1024) | Coremod hazards, `TransformerExclusions` guidance, plain-English purpose of each `IFMLLoadingPlugin` method |
| S33 | C | excerpt | xfl03.gitbook.io CoreModTutor 4.1 (1.3.2-1.5.2 coremod chapter) | Worked coremod example for the earliest FML era |
| S34 | C | excerpt | github.com/Sk1erLLC/MotionBlur commit (third-party coremod) | A real-world `getASMTransformerClass` implementation, 1.8.9 |
| S35 | C | excerpt | xfl03.gitee.io CoreModTutor 4.2 (1.6.1-1.12.2 coremod chapter) | `FMLCorePlugin` manifest attribute, `FMLCorePluginContainsFMLMod`, `@Name`, `IClassTransformer.transform` parameters |
| S36 | C | excerpt | skmedix.github.io Forge Javadoc mirror, `FMLForgePlugin` (1.8.9-11.15.1.2318) | Confirms `injectData` map keys `mcLocation`/`coremodList`/`coremodLocation` |
| S37 | | (merged into S30) | | |
| S38 | C | excerpt | skmedix.github.io Forge Javadoc mirror, `IFMLLoadingPlugin` (1.7.10-10.13.4.1614) | Full interface method list including `getAccessTransformerClass` |
| S39 | C | excerpt | skmedix.github.io Forge Javadoc mirror, `FMLCorePlugin` (1.9.4-12.17.0.2051) | Confirms `getAccessTransformerClass` present at 1.9.4, "injected post-deobf" wording |
| S40 | C | excerpt | skmedix.github.io Forge Javadoc mirror, `FMLCorePlugin` (1.10.2-12.18.3.2185) | Same confirmation at 1.10.2 |
| S41 | C | excerpt | Forge API doc mirror, 1.11.2-13.20.0.2228 | Same confirmation at 1.11.2 |
| S25b | C | excerpt | Minecraft Forum "MCMOD.INFO File Guide and Help" (1.7.10) and "[MOD] Question about mcmod.info" and "need assistance" threads | Real-world `mcmod.info` examples, the `authors` variant, `useDependencyInformation` usage |
| S28a | C | excerpt | github.com/MinecraftForge/FML issue #637 | The unimplemented 2015 `package.json`-style metadata proposal |
| S28b | C | excerpt | forums.minecraftforge.net "[1.12] mcmod.info specify dependency version" | Real mcmod.info confirming `useDependencyInformation`/`requiredMods` together |
| S28c | C | excerpt | app.unpkg.com and cdn.jsdelivr.net, `@xmcl/mod-parser` TypeScript type definitions | Independent third-party parser's doc comments, matching official semantics word for word |
| S19c | A | excerpt | docs.minecraftforge.net/en/1.13.x/gettingstarted/structuring/ | Confirms the same bare-array `mcmod.info` shape and field table at the 1.13.x docs snapshot |
| S42 | C | fetched in full | xfl03.gitbook.io CoreModTutor 3.4 "ModLauncher" | Full `ITransformationService`/`ITransformer`/`ServiceLoader` mechanism, standalone and Forge-hosted installation |
| S43 | C | excerpt | gist.github.com/Shadows-of-Fire (2020-08-20 log) | Launch plugin list including `mixin` |
| S44 | C | excerpt | gist.github.com/odasety (2021-05-24 log) | Launch plugin list, ModLauncher 5.1.2 |
| S45 | C | excerpt | gist.github.com/aharrisw (2020-11-21 log) | Launch plugin list, FML 34.1 |
| S46 | C | excerpt | gist.github.com/DarkerFALLEN (2020-10-07 log, OptiFine/MixinBootstrap modpack) | Launch plugin list without `mixin`, transformer services list with OptiFine/MixinBootstrap |
| S47 | C | excerpt | gist.github.com/Insane96 (2020-05-02 log) and gist.github.com/Yeregorix (2024-05-09 log) | Earliest and latest launch-plugin snapshots read, showing `slf4jfixer` and `capability_token_subclass` appearing over time |
| S48 | A | excerpt | github.com/MinecraftForge/Documentation, branches 1.18.x, 1.20.x, 1.21.x, `docs/concepts/registries.md` and `docs/blocks.md`-equivalent pages; docs.minecraftforge.net/en/1.19.2/blocks/ | Block registration code samples across three major versions |
| S49 | B | excerpt | docs.neoforged.net/docs/blocks/, /docs/1.21.1/blocks/, /docs/1.20.6/blocks/, /docs/1.20.4/blocks/ | NeoForge's diverged block API, for comparison only |
| S50 | A | fetched in full | files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json | Latest and recommended build per Minecraft version (77 versions) |
| S51 | A | excerpt | files.minecraftforge.net index page for 26.3 (search excerpt, listing 66.0.4 to 66.0.8) | 26.3 builds, Latest 66.0.8 dated 2026-09-28 |
| S52 | A | fetched in full | maven.minecraftforge.net/net/minecraftforge/forge/26.3-66.0.5/forge-26.3-66.0.5-changelog.txt | 26.3 changelog 66.0.0 to 66.0.5 |
| S53 | A | fetched in full | docs.minecraftforge.net/en/latest/misc/updatechecker/ | Update JSON format, update checker statuses, full 1.21.x docs page list, docs version selector |
| S54 | A | fetched in full | docs.minecraftforge.net/en/latest/gettingstarted/versioning/ | Mod versioning convention, Maven ComparableVersion |
| S55 | C | excerpt | mcjars.com/forge (Forge server download listing) | Build ids 64.0.15, 64.0.16 (26.1.2), 65.1.0 to 65.1.3 (26.2) |
| S56 | C | excerpt | github.com/itzg/docker-minecraft-server issues 2748 and 3160 | Old promotions path returns 301 or 403 |
| S57 | C | excerpt | github.com/MinecraftForge/MinecraftForge issue 6643 | Old promotions path used by 1.12.2 clients |
| S58 | A | fetched in full | docs.minecraftforge.net/en/latest/resources/server/ | Server data (data pack) introduction, full 1.21.x page list |
| S59 | A | fetched in full | docs.minecraftforge.net/en/latest/resources/server/conditional/ | Conditionally-loaded data, conditions, custom conditions |
| S60 | A | fetched in full | docs.minecraftforge.net/en/latest/resources/server/glm/ | Global Loot Modifiers |
| S61 | A | fetched in full | docs.minecraftforge.net/en/latest/resources/server/tags/ | Tags: paths, merge rules, `remove`, sync, conventions |
| S62 | A | fetched in full | docs.minecraftforge.net/en/latest/resources/server/recipes/ingredients/ | Forge ingredient types, custom ingredients |
| S63 | A | fetched in full | docs.minecraftforge.net/en/latest/resources/client/ | Resource pack introduction, `pack.mcmeta` |
| S64 | A | excerpt | docs.minecraftforge.net/en/latest/resources/server/recipes/custom/ (search excerpt) | Recipe, RecipeType, RecipeSerializer, RecipeManager |
| S65 | A | excerpt | docs.minecraftforge.net/en/latest/resources/server/recipes/incode/ (search excerpt) | Brewing, anvil, loom recipes in code |
| S66 | A | excerpt | docs.minecraftforge.net/en/latest/rendering/modelloaders/transform/ (search excerpt) | Two model transform systems |
| S67 | A | excerpt | docs.minecraftforge.net/en/latest/datagen/server/ (advancements, loottables, datapackregistries; search excerpts) | Datagen provider names only |
| S68 | A | fetched in full | docs.minecraftforge.net/en/1.20.x/resources/server/loottables/ | Loot tables, Forge additions (1.20.x docs line, not the latest line) |
| S70 | B | excerpt | docs.minecraftforge.net/en/1.12.x/items/loot_tables/ | Loot tables in the 1.12 era |
| S71 | B | excerpt | docs.neoforged.net/docs/1.20.6/resources/server/loottables/ | NeoForge's loot table page, comparison only |
| S72 | A | fetched in full | docs.minecraftforge.net/en/latest/resources/server/advancements/ | Advancements: criteria, requirements, custom triggers, rewards (code names look JSON-era) |
| S73 | A | fetched in full | docs.minecraftforge.net/en/1.21.x/items/ | Items introduction: properties, creative tabs, registration |
| S74 | B | excerpt | docs.architectury.dev migration page for version 9 (1.20) | 1.20 changes: creative tabs became a registry, loot lookup object replaced |
| S75 | B | excerpt | docs.minecraftforge.net/en/1.18.x/resources/server/tags/ | Tags for custom registries (1.18.x) |
| S76 | B | excerpt | docs.neoforged.net/docs/1.20.6/resources/server/advancements and /docs/1.21.1/resources/server/advancements/ | NeoForge advancements, comparison only |
| S77 | A | fetched in full | docs.minecraftforge.net/en/latest/blockentities/ | Block entities: registering, saving, ticking, three sync methods (1.20.x-era names) |
| S78 | A | fetched in full | docs.minecraftforge.net/en/latest/rendering/modelloaders/ | Custom model loaders introduction, OBJ loader |
| S79 | A | excerpt | docs.minecraftforge.net/en/latest/rendering/modelextensions/transforms/, .../visibility/ and /en/1.21.x/rendering/modelextensions/rendertypes/ | `transform`, `visibility`, `render_type` model keys |
| S80 | A | excerpt | docs.minecraftforge.net/en/1.20.x/rendering/modelloaders/bakedmodel/ | BakedModel members |
| S81 | B | excerpt | docs.fabricmc.net/develop/blocks/block-entities (page titled for 26.2) | Value-output based save method, registry lookup in update tag |
| S82 | B | excerpt | docs.neoforged.net blockentities pages (1.20.4, 1.20.6, 1.21.3) and the NeoForge documentation repository on GitHub | Newer block entity signatures, comparison only |
| S83 | B | excerpt | docs.neoforged.net/docs/1.20.4/ and /docs/1.21.3/resources/client/models/modelloaders/ | Custom loader classes, `neoforge:` loader ids, comparison only |
| S84 | C | excerpt | forge.gemwire.uk (Forge Community Wiki, Model JSONs and Tile Entities pages) | Older OBJ key `flip-v` |
| S85 | A | fetched in full | docs.minecraftforge.net/en/1.19.x/blockentities/ber/ | BlockEntityRenderer (1.19.x docs line) |
| S86 | A | fetched in full | docs.minecraftforge.net/en/1.19.x/items/bewlr/ | BlockEntityWithoutLevelRenderer (1.19.x docs line) |
| S87 | A | fetched in full | docs.minecraftforge.net/en/1.19.x/rendering/modelloaders/itemoverrides/ | ItemOverrides (1.19.x docs line) |
| S88 | A | fetched in full | docs.minecraftforge.net/en/1.19.x/resources/client/models/itemproperties/ | Item properties (1.19.x docs line) |
| S89 | C | excerpt | forge.gemwire.uk (Forge Community Wiki, Block Entity Renderer page and a revision diff) | Older registration route for BERs |
| S90 | B | excerpt | docs.neoforged.net/docs/blockentities/ber/ and docs.fabricmc.net/develop/blocks/block-entity-renderer | Redesigned BER with render state, comparison only |
| S91 | A | fetched in full | docs.minecraftforge.net/en/latest/blockentities/ber/ | BlockEntityRenderer, "latest" docs line (same content as the 1.19.x page) |
| S92 | A | fetched in full | docs.minecraftforge.net/en/latest/blocks/states/ | Block States |
| S93 | A | fetched in full | docs.minecraftforge.net/en/latest/resources/client/models/tinting/ | Texture Tinting, BlockColor and ItemColor |
| S94 | A | fetched in full | docs.minecraftforge.net/en/latest/resources/server/recipes/ | Recipes introduction, RecipeManager, Forge additions |
| S95 | A | source code | github.com/MinecraftForge/MinecraftForge, branch and tag lists via git ls-remote (2026-09-30) | 44 branches, 131 tags, head commits; stored in `source-map.json` |
| S96 | A | source code, read | branch `1.7.10`, head 9274e4fe435c (2017-06-07), shallow clone | Metadata handling, RotationHelper, Block patches, ModMetadata, MetadataCollection |
| S97 | A | source code, read | branch `26.3`, head 932c6e96491f (2026-09-28), shallow clone | Module layout, minecraft.versions.toml, ModFileInfo, ResourcePackLoader, IForgeBlockEntity, BlockEntityRenderers patch |
| S98 | A | source code, read | raw build files (`build.gradle`, `settings.gradle`, `gradle/libs.versions.toml`) of 13 branches | EventBus version per branch head |
| S99 | B | repository, read | github.com/PrismarineJS/minecraft-data commit f5d7d74604d8 (2026-09-29), data/pc/1.7 and 1.8 | Block variations by metadata (display names only) |
| S100 | C | excerpt | minecraft.wiki and minecraft.fandom.com, pre-flattening data value pages | Human tables of block data values |
| S101 | A | source code, read | raw `ForgeRegistries.java` and `CraftingHelper.java` of 13 branch heads (probe of 2026-09-30) | Registry era of conditions and ingredients; stored in `source-map.json` v0.1.1 |
| S102 | A | source code, read | branch `26.3`: `ForgeRegistries.java`, `ForgeMod.java`, `ForgeRegistry.java`, `RegistryBuilder.java`, `GameData.java`, `DeferredRegister.java`, `NBTIngredient.java` | Forge registries, duplicate handling, ingredient serializers |
| S103 | A | source code, read | branch `26.3`: `fmlloader` moddiscovery classes and `fmlcore` event classes | `mods.toml` keys, lifecycle event names |
| S104 | A | source code, read | branch `26.3`: `ICondition.java`, `ConditionCodec.java`, `ForgeHooks.java` (`readConditional`, `onJsonDataParsed`), patches `SimpleJsonResourceReloadListener`, `LootPool`, `TagFile`, `TagLoader`, `GameData.java` | Conditional data format, tag removal, registry flags |
| S105 | A | source code, read | raw `ICondition.java`, `ForgeHooks.java`, `ConditionCodec.java` of 13 branch heads (probe of 2026-09-30) | Era of `forge:condition` and `forge:conditional`; stored in `source-map.json` v0.1.2 |
| S106 | A | source code, searched | branch `26.3`: `Mod.java`, `FMLJavaModLoadingContext.java`, `RuntimeDistCleaner.java`, `FMLLoader.java`, `ConfigFileTypeHandler.java`, `FMLConfig.java`, `VersionChecker.java`, `LootModifierManager.java`, `ClientForgeMod.java`, the `network` and `capabilities` packages, `JarInJarDependencyLocator.java`, `ModDirTransformerDiscoverer.java`, `forge-transformers` | Re-verification register of section 29 |
| S107 | B | fetched, cut off | minecraft.wiki/w/Java_Edition_pre-flattening_data_values | Metadata summary table (1.12.2 state), detail tables up to the torches section |
| S108 | B | repository, read | PrismarineJS/minecraft-data commit f5d7d74604d8, `data/pc/1.7/blocks.json` and `data/pc/1.8/blocks.json` (`variations` compared) | Variations by metadata; the "1.7" data contains later content |
| S109 | C | excerpt | skmedix.github.io/ForgeJavaDocs, 1.7.10-10.13.4.1614 `BlockStairs` page (search excerpt) | Signature-only Javadoc mirror exists |
| S110 | A | source code, read | github.com/Bukkit/Bukkit master (head f210234e59275330f83b994e199c76f6abd41ee7, 2013-07-07, GPL v3), package `org.bukkit.material` | Independent code reference for block metadata bits; facts only |
| S111 | A | source code, listed | github.com/SpigotMC/Spigot-API (modern, reachable, not used) and github.com/cuberite/cuberite (C++, not used) | Checked that other open implementations exist |
| S112 | A | source code, searched and partly read | branch `26.3`: `ShapedRecipe.java.patch`, `RegisterColorHandlersEvent.java`, `IClientItemExtensions.java`, `FMLModContainer.java`, `ModLoadingStage.java`, `DeferredWorkQueue.java`, the model patches | Rows 23 to 26 of the register (section 32) |
| S113 | A | source code, cloned (shallow) and read in part | github.com/MinecraftForge/MinecraftForge branch `26.3`, head `939fb13d3b96` (2026-09-30): `RegisterColorHandlersEvent`, `ModelEvent`, `RegisterNamedRenderTypesEvent`, `IClientItemExtensions`, `ForgeBlockModelData`, `ForgeFaceData`, `RenderTypeGroup`, `NamedRenderTypeManager`, `ClientForgeMod`, `ForgeHooksClient` (model key parsing), `TransformationHelper`, `ModelContext`, `IGeometryBakingContext`, `IForgeBlockStateModel`, `accesstransformer.cfg` and the patches for `ModelManager`, `ModelProvider`, `CuboidModel`, `CuboidFace`, `ItemTransform(s)`, `ItemDisplayContext`, `BlockColors`, `UnbakedModel`, `BlockStateModel` | Section 33 (all of it) |
| S114 | A | fetched | docs.minecraftforge.net, line labeled "latest" (menu 1.21.x): `rendering/modelextensions/` pages `facedata`, `rendertypes`, `transforms`; `rendering/modelloaders/` pages `bakedmodel`, `transform`, `itemoverrides`; `resources/client/models/` and `.../itemproperties`; `items/bewlr` | Sections 33.1 and 33.2 (docs side) |
| S115 | A | fetched (one page failed) | docs.minecraftforge.net `rendering/modelextensions/visibility/` (fetched in full after two search extracts); `rendering/modelextensions/index.md`, the navigation target of the Model Extensions introduction, answered HTTP 404 and a search found no other address for it | Section 33.2 row 4; the unread page named in the section 33 introduction |
| S116 | B | fetched | docs.neoforged.net `docs/resources/client/models/items` ("Client Items", line 26.1; its version selector links the page from 1.21.4 on and not for 1.21.2 to 1.21.3) | Section 33.1 (vanilla client item definitions) |
| S117 | B | excerpt | docs.neoforged.net `primer/docs/1.21.4` (search extract) | Section 33.1 (range dispatch compared with the old override system) |
| S118 | C | excerpt | docs.nexomc.com "ItemModel Builder" (a third-party plugin's documentation; search extract; says item models are the way "as of 1.21.4+") | Section 33.1 (start of the system, weak evidence) |
| S119 | A | source code, shallow clones read in part | github.com/MinecraftForge/MinecraftForge, 20 branch heads (heads stored in `source-map.json`, key `pass20_clones`; `26.3` as in S113): `DeferredWorkQueue`, `ModWorkManager`, `ModLoader`, `ModLoadingStage`, `ModStateProvider`, `ForgeStatesProvider`, `ParallelTransition`, `ModStateTransitionHelper`, `ThreadSelector`, `ParallelDispatchEvent`, `ModLifecycleEvent`, `CustomPayloadEvent`, `FMLModContainer`, `GameData`, `RegistryManager`, `RegisterEvent`, `DeferredRegister`, `ForgeRegistries`, `RegistryBuilder`, `ForgeRegistry`, `FMLControlledNamespacedRegistry` and `GameData` of 1.7.10, `RegisterColorHandlersEvent`, `IClientItemExtensions`, `CreateSpecialBlockRendererEvent`, `CriterionTest` (module `forge-tests`), the access transformer and the patch lists of each head | Section 34 (all of it) |
| S120 | B | excerpt | docs.neoforged.net advancements pages (latest line, 1.21.4, 1.21.5, 1.20.6, 1.20.4; search extracts) | Section 34.3 (registry for triggers) |
| S121 | A | excerpt | docs.minecraftforge.net advancements pages for 1.18.x and 1.20.1 (search extracts) | Section 34.3 (triggers up to 1.20.1) |
| S122 | C | supplied by Shiroz in the chat on 2026-10-03, read in full | PayBot mapping and syntax cookbook (cross-reference of Mojmap, Intermediary, Yarn, SRG and MCP names, usage cookbook, table of 100 targets from 1.14.2 to 26.2); not part of this package; reference only | Section 34.6 |
| S123 | A | source code, full clone read in part | github.com/Bukkit/Bukkit, branch `master`, head `f210234e59275330f83b994e199c76f6abd41ee7` (author date 2013-07-07, commit date 2014-08-17): `CocoaPlant`, `Vine`, `Mushroom`, `PoweredRail`, `DetectorRail`, `ExtendedRails`, `Rails`, the `Material` enum, commit dates of the package; licence GPL v3, facts only | Section 35.3 |
| S124 | A | source code, shallow clones read in part | github.com/MinecraftForge/MinecraftForge branches `1.6` (head `ba4637f5a3fb`), `v164-pregradle` (`2f7ad51a025c`), `mc172` (`e08c63b57bc5`), `1.7.10`: `RotationHelper` compared by script; word search for 1.7 blocks | Section 35.3 |
| S125 | A | source code, shallow clones of 15 tags (commit date, subject and top-level names only) | github.com/MinecraftForge/MinecraftForge tags `1.0.0`, `1.0.4`, `1.2.1`, `1.2.5`, `1.2.5@213`, `1.3.2`, `v7.2`, `v7.99`, `v99.99`, `23.5`, `25.0`, `26.0`, `28.2`, `36.2`, `37.1` | Section 35.5 |
| S126 | A | source code, read in part (clones of S119) | `ICondition`, `ConditionalRecipe`, `ConditionalAdvancement`, `ForgeHooks` (condition methods), `ForgeMod` and `ForgeRegistries` (condition registrations), the `RecipeManager` and `ServerAdvancementManager` patches, `CraftingHelper` of 1.20.1, and `Tags.java` of 12 heads | Sections 35.1 and 35.2 |
| S127 | A | excerpt | docs.minecraftforge.net tags pages for the 1.13.x, 1.16.x, 1.18.x, 1.19.2, 1.21.x and latest lines (search extracts) | Section 35.2 |
| S128 | C | excerpt | minecraft.wiki Mushroom Block data values as shown on gamepedia file pages (search extracts; the row for value 5 is missing) | Section 35.3 |
| S129 | B | excerpt | Spigot and Paper API Javadoc `org.bukkit.material.types.MushroomBlockTexture` (later API than the 2014 snapshot) | Section 35.3 |
| S130 | C | excerpt | Skript aliases `other.sk` (cocoa damage values) and Minecraft-Overviewer commits on vines and mushroom blocks; community files with no stated origin, never counted as a second source | Section 35.3 |
| S131 | C | excerpt | Javadoc mirror list pages: skmedix/ForgeJavaDocs, Mandi-OS/forge.doc (fork), Nekoyue/ForgeJavaDocs-NG, MCStreetguy/ForgeJavaDocs and its guide, forgecommunitywiki/javadocs (archived 2022-12-16), MinecraftForge/Documentation readme | Section 35.4 |
| S132 | A | source code, shallow clone read in part | github.com/MinecraftForge/MinecraftForge branch `26.3`, head `939fb13d3b96664d6b5057e128bb05395417063b` (2026-09-30): patches and packages for entities, commands, tick events and permissions, as listed in 36.0 | Sections 36.1 to 36.3 |
| S133 | A | read in full (one page), excerpts (others) | docs.minecraftforge.net `networking/entities` of the 1.21.x line (read in full) and search extracts of the same page for the latest, 1.20.1, 1.19.2, 1.19.x, 1.18.x and 1.12.x lines; the Sides page of the 1.21.x line (extract) | Section 36.1 |
| S134 | B | read in full | docs.neoforged.net, 26.1 line: Entities, Data and Networking, Living Entities/Mobs & Players, Attributes; search extracts of the entity pages of the 1.20.4 to 1.21.10 lines | Section 36.1 |
| S135 | B | read in full | docs.neoforged.net Sides page, 26.1 line | Section 36.3 |
| S136 | B | read in full | docs.fabricmc.net `develop/commands/`: Creating Commands, Command Arguments, Command Suggestions, Minecraft 26.2 line | Section 36.2 |
| S137 | A | read in full | minecraft.net article "Removing obfuscation in Java Edition", published 2025-10-29 | Section 36.4 |
| S138 | C | excerpt | minecraft.wiki "Client.json" (fields `client_mappings` and `server_mappings`, added in 19w36a and also in 1.14.4) | Section 36.4 |
| S139 | B | excerpt | maven.fabricmc.net Yarn Javadoc pages for block classes (`yarn-1.20.2+build.5`, `yarn-24w20a+build.5`; scheduled tick and random tick descriptions; search extracts) | Section 36.3 |
| S140 | C | excerpt | Javadoc mirror pages for `TickEvent` and `TickEvent.LevelTickEvent` (MCStreetguy/ForgeJavaDocs 1.20-46.0.14, 1.20.6-latest, 1.21-latest; NeoForge mirror 1.20.3-20.3.8; search extracts; the first mirror pages opened, the list pages of 35.4 are S131) | Section 36.3 |
| S141 | C | excerpt, not counted as a source | Forge Forums topics 109342 and 15435 (random versus scheduled ticks) | Section 36.3 |
| S142 | C | excerpt | github.com/hube12/DecompilerMC and github.com/Fox2Code/Repacker readme text (mappings from 19w36a and 1.14.4; the first is archived since unobfuscated jars) | Section 36.4 |
| S143 | A | read in full (datagen), searched (the rest) | github.com/MinecraftForge/Documentation, branch `1.21.x`, head `ac05e12aae903703cb8a9908a153623ce476a509` (2025-08-16): the ten files under `docs/datagen/` read in full; full-text search of `docs/` for worldgen and biome-modifier terms | Sections 37.1, 37.2 |
| S144 | B | partly read | github.com/neoforged/documentation, branch `main`, head `89528c36e` (2026-10-01): `docs/worldgen/biomemodifier.md` (lines 1 to 150 and 570 to the end read; the middle checked by headings and `type` names), the "Data Generation" section of `docs/resources/index.md` (read in full), file lists for datagen and worldgen | Sections 37.1, 37.2 |
| S145 | B | read in full (one page), searched (the rest) | github.com/FabricMC/fabric-docs, head `f17bf08e377fc572169fee8df71bbef95628ed2d` (2026-09-30): `develop/porting/mappings/index.md` read in full; word search of `develop/` for mapping terms (Loom pages by extract) | Section 37.3 |
| S146 | A | partly read | github.com/MinecraftForge/MCPConfig, branch `master`, head `751a4f362ef15ec19d3616416e18853fee68968e` (2026-09-18): the `versions/` listing, `config.json` of 26.3 (read in full), key lists of 1.21.11 and 1.20.1, file list of 1.21.11; a search of the whole tree for the Mojang licence wording (not found) | Section 37.3 |
| S147 | C | excerpt | search extracts of Forge Forums posts, GitHub gists and an issue that quote Forge Gradle's warning about Mojang's mapping licence; the address `raw.githubusercontent.com/MinecraftForge/MCPConfig/master/Mojang.md` returned 404 on 2026-10-05 | Section 37.3 |
| S148 | A | partly read | github.com/MinecraftForge/FML: branches `1.4` (`e6f57e5d94a17aad45e9c5b6aa0a9bd6edc6fe10`, 2012-10-20), `1.5.1` (`aaf02ea2ac938d8cbaafdd2f59985c0604d648d7`, 2013-03-20), `1.6.4` (`f1b3381e61fac1a0ae90f521223c6bc613eb4888`, 2013-12-17), `1.7.10` (`a099592d3d1418245e3af65eda195da244287188`, 2015-05-11), `1.8` (`d4ded9d6e218ac097990e836676bbe22b47e5966`, 2015-01-18); the 28 tags `v2.0` to `v8.0` read by path without file contents | Sections 38.1 to 38.3 |
| S149 | A | partly read | github.com/MinecraftForge/MinecraftForge: tags `1.0.0` (`03267f3a9d7c0da889448376bb2b62e099ed0579`, 2011-07-31), `1.2.5` (`adb6b330b9726fbd90f1f83e0aa769299d062c7d`, 2012-01-14), `1.3.2` (`9ca1255fc4b5805147a52dd9e5ab7f348b3c51c0`, 2012-01-29) cloned; `conf/version.cfg` and dates of the tags `1.0.0` to `1.3.2` and the numeric tags `23.5` to `66.0` read without cloning | Section 38.1 |
| S150 | A | partly read | github.com/Mojang/LegacyLauncher (LaunchWrapper) master, head `a4801b70f8a0148c6e6279ec2e91527e8019e1c8` (2016-12-20): `Launch`, `ITweaker`, file list | Section 38.2 |
| S151 | A | partly read | github.com/MinecraftForge/MinecraftForge branches `1.7.10`, `1.8.9`, `1.12.x`, `1.16.x`, `1.20.1`, `1.21.1` (heads in 38.0): launcher, relauncher, `ProxyInjector`, `FMLInterModComms`, `Loader`, `SimpleChannelHandlerWrapper`, `NetworkEvent`, `CustomPayloadEvent`, the capability packages and the entity patches | Sections 38.2, 38.3 |
| S152 | A | read in full (two pages), searched (the rest) | github.com/MinecraftForge/Documentation branch `1.12.x`, head `b080c8753d7ba6f69b159de57181fe0f4a1cb124` (2024-01-23): "Sides" and "Loading Stages" in full; word searches for IMC, tweaker, LaunchWrapper and thread; the thread lines of the networking page | Sections 38.2, 38.3 |
| S153 | A | searched by history | github.com/MinecraftForge/Documentation, a blob-less clone with all 15 branches and 502 commits (2026-10-07), used to ask which commits touched `docs/rendering/modelextensions/index.md` (none) and which commit added the folder (`23ba528`, 2023-05-15); plus raw-address checks of that path on eight branches | Section 39.1 |
| S154 | C | read in full | minecraft.wiki "Items model definition", `oldid=3779697`, last edited 2026-09-16 (Latest Java release named 26.3) | Section 39.1 |
| S155 | A | partly read | raw files of github.com/MinecraftForge/MinecraftForge on the branches `1.20.1` to `26.2` (`ConditionalRecipe.java`, `ForgeMod.java`) | Section 39.2 |
| S156 | C | excerpt, not counted as a source | `mappings.dev` pages for `ItemModelUtils` (versions 25w04a to 1.21.11) and a CraftEngine wiki page that lists the range-dispatch properties | Section 39.1 |
| S157 | A | searched by history | github.com/MinecraftForge/MinecraftForge, a blob-less clone with all branches (2026-10-08): 11,821 commits, 45 remote branches, first commit 2011-07-12; the 38 commits that changed `conf/version.cfg`, the 781 commits between 2012-04-06 and 2012-08-01, the first appearance of FML (a source zip on 2012-08-09, a submodule on 2013-03-03) | Section 40.4 |
| S158 | A | read in part | Forge 26.3 clone (S132): modules `fmlcore`, `fmlloader`, `javafmllanguage`, `forge-transformers`, and the `network` package (channel builder, tasks, filters, status ping) | Sections 40.1, 40.2 |
| S159 | A | raw-file checks | raw files of the Forge branches `1.20.1` to `26.3`: `CoreModFile.java` (present through `1.21.10`, absent from `1.21.11`) and `JarInJarDependencyLocator.java` (MixinExtras dependency from `1.21.10`) | Section 40.1 |
| S160 | A | read in part, cut | `maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml` as fetched on 2026-10-08 (an older snapshot: newest entry `1.21.8-58.0.3`; entries from `1.21-51.0.33` to `1.10.2-12.18.3.2281` returned, the rest cut) | Section 40.5 |
| S161 | B | read in part | docs.neoforged.net sources: `items/interactions.md` (left-click and right-click pipelines, result types, read in full) and `items/datacomponents.md` (headings and introduction) | Section 40.6 |

---

## 18. Version registry evidence (pass 5, v0.4.3, 2026-09-29)

### 18.1 The promotions file and the update JSON format

Forge's own list of latest and recommended builds is a JSON file at
`https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json` (S50). Its shape
is the general Forge update-JSON shape documented for mods (S53): a `homepage` string and a
`promos` object whose keys are `<mcversion>-latest` and `<mcversion>-recommended` and whose values
are version strings. The official docs describe the two as the newest "bleeding-edge" build and the
newest "stable" build of the given Minecraft version (S53). A mod's own update JSON may also carry
one object per Minecraft version mapping each mod version to a changelog string, but the Forge file
itself only uses `homepage` and `promos`.

Observed facts about the file (fetched 2026-09-29):
- 77 versions, 117 keys (77 `-latest`, 40 `-recommended`).
- **Versions with a recommended build (40):** 1.5.2, 1.6.2, 1.6.4, 1.7.2, 1.7.10, 1.8, 1.8.9,
  1.9, 1.9.4, 1.10.2, 1.11, 1.11.2, 1.12, 1.12.1, 1.12.2, 1.14.4, 1.15.2, 1.16.3, 1.16.4, 1.16.5,
  1.17.1, 1.18.1, 1.18.2, 1.19, 1.19.2, 1.19.3, 1.19.4, 1.20.1, 1.20.2, 1.20.4, 1.20.6, 1.21.1,
  1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2.
- **Versions with only a latest build (37):** 1.1, 1.2.3, 1.2.4, 1.2.5, 1.3.2, 1.4.0 to 1.4.7,
  1.5, 1.5.1, 1.6.1, 1.6.3, 1.8.8, 1.10, 1.13.2, 1.14.2, 1.14.3, 1.15, 1.15.1, 1.16.1, 1.16.2,
  1.18, 1.19.1, 1.20, 1.20.3, 1.21, 1.21.6, 1.21.7, 1.21.9, 26.1, 26.1.1, 26.3.
- Latest equals recommended for 12 of the 40 versions that have both (1.5.2, 1.6.2, 1.6.4, 1.7.2,
  1.7.10, 1.8.9, 1.9.4, 1.10.2, 1.11.2, 1.17.1, 1.19, 1.20.2). For the other 28 the two differ, for
  example 1.12.2 (last number 2864 versus 2859) and 1.20.1 (47.4.23 versus 47.4.10).
- The key list has no entry for `1.7.10_pre4`, although the official downloads site lists it.
- The file is served only at the newer path. Older paths (`/maven/net/minecraftforge/forge/...`)
  answered 301 or 403 for other people (S56, S57), so a tool that reads it must follow the
  current path and expect blocking of automated access.

### 18.2 Two official sources disagree on 26.3

On 2026-09-29 the promotions file gave `26.3-latest = 66.0.6` while the official 26.3 index page
excerpt (S51) listed 66.0.8, dated 2026-09-28, as Latest. Two readings are possible and the
evidence read here does not decide between them: the promotions file is refreshed later than the
index page, or one of the two sources read was a cached copy. **Proposal for VESSEL:** the Version
Registry must never trust one Forge source alone. It reads promotions, the maven metadata and the
index page, takes the highest build by `ComparableVersion`, and writes a yellow warning naming
both values when they differ. **Decided by Shiroz on 2026-09-29 (option A): this proposal is now the rule.**

### 18.3 Stability decision (Shiroz, 2026-09-29) and its consequence

Decision: a Forge build state with **Latest but no Recommended** (Forge 26.3 today) is **not**
treated as stable by VESSEL's Version Registry.

Applied literally to the data in 18.1, that decision marks **37 of the 77 versions as not stable**,
which includes every 26.x version except 26.1.2 and 26.2, and also very old versions such as 1.1,
1.2.x, 1.3.2 and 1.4.x that never had a recommended build. **Decided by Shiroz on 2026-09-29 (follow-up): "not stable" only produces a warning.** It does not
block startup and needs no confirmation flag. The Version Registry marks such a build as not stable and
VESSEL prints a yellow console warning when a server or a mod targets it. Because 37 of 77 versions
are affected, the warning text should say plainly that the upstream project has not marked that build
as recommended, so admins are not alarmed by very old or very new versions that simply never got a
recommended build.

### 18.4 Official mod versioning convention (S54)

The official Versioning page recommends `MCVERSION-MAJORMOD.MAJORAPI.MINOR.PATCH` for mods, with
`-final` for the last build of a dropped Minecraft version, `-betaX` for prereleases and `-rcX`
for release candidates. It states that Forge compares versions with the Maven `ComparableVersion`
rules, which are **not fully compatible with Semantic Versioning 2.0.0** (the pre-release tag
differs). The same comparison logic is used by the update checker (S53).

Consequences for VESSEL (evidenced, then proposals):
- Evidenced: any Forge module's `version`, and every `versionRange` in its metadata, follows Maven
  version-range syntax (for example `loaderVersion="[45,)"` and `versionRange="[45,)"` in the
  1.19.x docs, S9). VESSEL's dependency resolver therefore needs a faithful
  `ComparableVersion` and Maven range parser, not a semver library.
- Proposal: reuse that one implementation for the update checker emulation and for the Forge
  Version Registry parser (the Forge build string `<minecraft>-<forge>` is a Maven-style string
  too).

### 18.5 Update checker (S53)

- A mod opts in by setting `updateJSONURL` in its `mods.toml`. Forge shows a flashing icon on the
  Mods button and lists the changelogs. It never downloads anything.
- Results are read with `VersionChecker#getResult(IModInfo)`. The result has a `status` with these
  exact values: `FAILED` (URL not reachable), `UP_TO_DATE` (equal to recommended), `AHEAD` (newer than
  recommended when no latest exists), `OUTDATED` (a newer recommended or latest exists),
  `BETA_OUTDATED` (a newer latest exists), `BETA` (equal to or newer than latest), `PENDING`
  (still running).
- A mod finds its own `ModContainer` through `ModLoadingContext.get().getActiveContainer()`, or any
  mod through `ModList.get().getModContainerById(<modId>)` or `getModContainerByObject(...)`.

For VESSEL: a server has no Mods menu, so the checker's UI is irrelevant, but mods can still call
`VersionChecker#getResult`. Proposal: VESSEL's Forge adapter answers that call from its own
Version Registry data, or returns `FAILED`, and never makes network requests on a mod's behalf
without an admin setting. The behavior of the real checker on a dedicated server (whether it runs
there at all) is **unverified**.

---

## 19. The official docs map for 1.21.x (pass 5, v0.4.3, 2026-09-29)

The version selector of the official docs (S53, S54, fetched) offers exactly four documentation
lines: **1.21.x, 1.20.x, 1.20.1 and 1.19.x**. **No 26.x line exists**, confirmed on 2026-09-29
(this closes item 1 of section 15 as "confirmed absent"). A "Legacy Versions" section holds a
page named "Porting to This Version" and an introduction, not read yet.

The full page list of the 1.21.x docs is the checklist for the rest of the gameplay API. Status
column: `done` = read and written up in this file, `not read` = only the title has been seen.

| Docs section | Pages | Status |
|---|---|---|
| Getting Started | Introduction, The Mod Files, Structuring Your Mod, Versioning | Mod Files and Structuring done (section 3), Versioning done (18.4), Introduction not read |
| Core Concepts | Registries, Sides, Events, Mod Lifecycle, Resources, Internationalization | first four done (sections 4 to 7), Resources and Internationalization not read |
| Blocks | Introduction, Block States | Introduction done (section 16), Block States read in full from the "latest" docs (25.1) |
| Items | Introduction, BlockEntityWithoutLevelRenderer | Introduction done from the 1.21.x docs (22.2), BlockEntityWithoutLevelRenderer read in full from the 1.19.x docs (24.2) |
| Networking | Introduction, SimpleImpl, Synchronizing Entities | SimpleImpl covered (section 8), Synchronizing Entities read in full from the 1.21.x docs (36.1, pass 22), Introduction not read |
| Block Entities | Introduction, BlockEntityRenderer | Introduction done from the "latest" docs (23.1), BlockEntityRenderer read in full from the 1.19.x docs (24.1) and from the "latest" docs (25.4) |
| Game Effects | Particles, Sounds | not read |
| Data Storage | Capabilities, Saved Data, Codecs | Capabilities and Saved Data done (section 9), Codecs not read |
| Graphical User Interfaces | Menus, Screens | not read |
| Rendering | Model Extensions (Introduction, Root Transforms, Render Types, Part Visibility, Face Data), Model Loaders (Introduction, Baked Model, Transform, Item Overrides) | Model Loaders Introduction done in full (23.2); Baked Model, Root Transforms, Render Types and Part Visibility seen as excerpts (23.3, 23.4); Transform seen as an excerpt (20.6); Item Overrides read in full from the 1.19.x docs (24.3); Face Data not read |
| Resources, client | Introduction, Models, Texture Tinting, Item Properties | Introduction done (20.1), Item Properties read in full from the 1.19.x docs (24.4); Models introduction not read; Texture Tinting read in full from the "latest" docs (25.2) |
| Resources, server | Introduction, Recipes (Introduction, Custom Recipes, Ingredients, Non-Datapack Recipes), Loot Tables, Global Loot Modifiers, Tags, Advancements, Conditionally-Loaded Data, Tags List | Introduction, Ingredients, Global Loot Modifiers, Tags and Conditionally-Loaded Data done (20.1 to 20.4); Custom Recipes and Non-Datapack Recipes excerpt only (20.5); Tags List not read; Recipes introduction read in full from the "latest" docs (25.3); Advancements read in full from the "latest" docs (22.1); Loot Tables read from the 1.20.x docs only (21.1 to 21.4) |
| Data Generation | Introduction, Model Providers, Language Providers, Sound Providers, Recipe Providers, Loot Table Providers, Tag Providers, Advancement Providers, Global Loot Modifier Providers, Datapack Registry Object Providers | all ten read in full from the docs repository (37.2, pass 23, v0.4.52) |
| Miscellaneous | Configuration, Key Mappings, Game Tests, Forge Update Checker, Debug Profiler | Configuration done (section 10), Update Checker done (18.5), the rest not read |
| Advanced Topics | Access Transformers | done (12.6) |
| Contributing to Forge | Introduction, Pull Request Guidelines | not read |

Note for VESSEL's priorities: the pages that decide what a server must understand to build the
render bundle (resource pack, data pack) are **Resources (client and server), Recipes, Loot Tables,
Global Loot Modifiers, Tags, Advancements, Conditionally-Loaded Data, Model Loaders and Model
Extensions**. They are read first in the next pass. There is no "commands" page in the official
docs at all, so the command API needs a different source (Javadoc or source code).

---

## 20. Data packs, resource packs, recipes, tags and loot modifiers (pass 6, v0.4.9, 2026-09-29)

Scope and limits, stated first. Everything below comes from the official Forge docs for the **1.21.x
line** (S58 to S63, fetched in full) plus two search excerpts (S64, S65). There are **no official docs
for 26.x** (section 19), so every statement here is **unverified for 26.x**. Pages read only as a
short excerpt are marked. Text is paraphrased. The JSON key names are quoted only where a key name
is the fact.

### 20.1 Where a mod's server data and client assets live (S58, S63)

- **Server data** ("data pack" side) sits in the `data` directory of the mod's resources. The docs say
  a mod can hold several data domains (namespaces), and that it can add to or change data of vanilla,
  of Forge, or of another mod's domain. Any user can therefore change every recipe, loot table and
  other file under `data`.
- **Client assets** ("resource pack" side) sit in the `assets` directory. The pack itself is defined by
  a `pack.mcmeta` file placed next to the `assets` folder. Textures, models, sounds and localizations
  live there. Forge itself also ships such a pack.
- Both pages send the reader to the Minecraft wiki for the actual pack file formats. Forge documents
  only its own extensions on top of them.
- **Not found on these pages:** the `showAsResourcePack` key of `mods.toml` and anything about how
  Forge orders or enables mod packs. That item was resolved later from the source code, see section 26.3.

For VESSEL: this is the input of the Resource Pack Compiler and the Datapack Compiler (phases 14 and
15). Evidenced: a Forge mod is by definition already a data pack plus a resource pack. Not evidenced
here: how mod packs are ordered against vanilla and against each other.

### 20.2 Conditionally-loaded data (S59)

- The docs say conditional loading is implemented **only for recipes and advancements**.
- A conditional file holds a list of pairs "conditions, data". The first pair whose conditions are all
  true is used, the others are discarded. Conditions inside one pair are ANDed.
- Recipes need `type` = `forge:conditional` because recipes can have custom serializers. Advancements
  do not need a type. The list key is `recipes` (or `advancements`), each entry has `conditions` and
  `recipe` (or `advancement`).
- Built-in conditions, by type name: `forge:true`, `forge:false`, `forge:not` (key `value`),
  `forge:and` and `forge:or` (key `values`), `forge:mod_loaded` (key `modid`), `forge:item_exists`
  (key `item`, true when that item is registered), `forge:tag_empty` (key `tag`, item tags only).
- Custom conditions implement `ICondition` (methods `getID`, `test`) and `IConditionSerializer`
  (`getID`, `read`, `write`) and are registered with `CraftingHelper#register`, either in the
  `RegisterEvent` for recipe serializers or in `FMLCommonSetupEvent`. In the second case the call must
  go through `enqueueWork`, because the docs say `CraftingHelper#register` is not thread-safe.
- The docs note that a condition's `test` gets a context that can, for now, only read tags of a
  registry.

For VESSEL (evidenced, then open questions):
- Evidenced: to build a data pack VESSEL must **evaluate** these conditions, and two of them depend on
  runtime state: `mod_loaded` needs the set of loaded modules, `item_exists` and `tag_empty` need
  finished registries and merged tags.
- **Question A (identity), answered 2026-09-29, see below:** `forge:mod_loaded` takes a bare id. With decision 7 (same id from
  different loaders both run) a plugin and a mod called `worldedit` can coexist. Which one does the
  condition mean? **Decided by Shiroz on 2026-09-29:** a Forge-source condition matches only modules
  whose source loader is `forge`.
- **Question B (order), answered 2026-09-29, see below:** the Startup Build Stage builds bundles before the world loads.
  `item_exists` and `tag_empty` need registry contents. The scan and dry run of
  `identity-and-collision.md` section 5.1 do not necessarily produce full registries. How the compiler
  gets complete registries and tags was open. **Decided on 2026-09-29 (Shiroz delegated the choice):**
  registration finishes at step 10 (registry bootstrap) before the stage starts, so the stage works from a
  registry snapshot; VESSEL merges tags itself; mod code keeps running on the server for global loot
  modifiers and code-only recipes. Details in `startup-build-stage.md` section 2.1. Still open there: how
  registration code is kept from touching the world.

### 20.3 Global Loot Modifiers (S60)

- A GLM changes harvested drops without overwriting vanilla loot tables. The docs stress that
  modifiers **stack** instead of "last load wins" (like tags).
- Four parts are needed: a `global_loot_modifiers.json`, one serialized JSON per modifier, a class that
  implements `IGlobalLootModifier` (usually by extending `LootModifier`), and a codec for it.
- `global_loot_modifiers.json` is read **only in the `forge` namespace**, at
  `data/forge/loot_modifiers/global_loot_modifiers.json`. A copy under a mod's own namespace is ignored.
  It has a boolean `replace` (must be present) and an ordered list `entries` of resource locations that
  point to `data/<namespace>/loot_modifiers/<path>.json`. `replace` = true replaces the global list
  instead of appending. The docs say modders should use false and data pack makers may use true.
- Each modifier JSON has `type` (registry name of its codec) and `conditions` (loot conditions, ANDed
  together when `LootModifier` is the base class), plus any modifier-specific keys.
- `IGlobalLootModifier` has `apply` (takes the loot generated so far and the loot context, returns the
  drops) and `codec`. With `LootModifier` the method to write is `doApply`, which runs only when all
  conditions pass. The result of one modifier is fed to the next in registration order.
- The codec is a registry entry (a registry of codecs), built with `LootModifier#codecStart`.

For VESSEL (inference, marked): a GLM cannot be reduced to plain data. Its behavior is mod Java code
(`doApply`), only its parameters are data. So a converted server must keep running that code, or
reimplement it, inside the loot pipeline. Also, the fact that a Forge codec registry is itself a
registry matters for the duplicate scan: it is one more place where registry keys appear.

### 20.4 Tags (S61)

- Tags are sets of registry objects for grouping and fast membership checks. Vanilla tag holders live in
  `net.minecraft.tags`, Forge's extra tags in `net.minecraftforge.common.Tags`. The docs give a full list
  of Forge's tags on a separate page (not read yet).
- A tag `modid:foo/tagname` for blocks is the file `data/<modid>/tags/blocks/foo/tagname.json`. Blocks,
  items, entity types, fluids and game events use **plural** folder names, all other registries the
  **singular** (example given: `entity_types` versus `potion`).
- A mod can append to or override tags of other namespaces. Tags of the same name **merge** on reload
  when `replace` is false. When `replace` is true, entries before that file are dropped. An entry can be
  an object with `id` and `required: false`, otherwise a missing entry makes the tag error.
- **Forge extension:** a `remove` array, same format as `values`, deleting entries. The docs call it a
  finer version of `replace`.
- Tags for all registries are **sent from server to client** on login and on reload. Only the five
  registries above have intrusive holders, which the docs say may be removed in a future Minecraft version.
- Conventions: use `forge:type/material` (for example `forge:ingots/iron`). The docs warn that the
  common `c` tags are shared by all loaders, but other loaders may add their own `c` tags, and such
  tags may sit under `forge` in Forge until they become common.

For VESSEL (evidenced, then proposal): tag merging with `replace`, `required` and `remove` is a precise
algorithm the Datapack Compiler must follow. The `c` versus `forge` note is direct evidence for the
cross-loader tag mapping (phase 13): `c` is the shared space, `forge:` is Forge-only. **Proposal:**
map `forge:` tags to `c:` only when the docs or source of both loaders show they are equal, and log a
yellow warning otherwise. Not decided.

### 20.5 Recipes (S62, S64, S65)

- **Ingredients** (S62, fetched in full): a predicate over an item stack. With no `type`, the ingredient
  is vanilla `minecraft:item`. Forge types: a compound ingredient (a plain list, matches if any element
  matches, no `type` needed), `forge:nbt` (item and exact share tag), `forge:partial_nbt` (only the
  listed keys are checked, `item` or `items`, `item` wins if both), `forge:intersection` (key
  `children`, at least two, all must match), `forge:difference` (keys `base` and `subtracted`).
- A custom ingredient implements an `Ingredient` subclass (`getSerializer`, `test`, `isSimple`; false
  when it matches on tag) and an `IIngredientSerializer` (parse from JSON, parse from network, write to
  network), registered with the same `CraftingHelper#register` and the same thread-safety rule as
  conditions.
- **Custom recipes** (S64, excerpt only): a recipe is three parts: the `Recipe` implementation (data and
  matching logic), the `RecipeType` (the category or context) and the `RecipeSerializer` (JSON decoding
  and network transfer). Each decoded recipe is one instance held by the `RecipeManager`. Recipe JSONs
  sit with the normal recipes, and their `type` names the serializer.
- **Recipes that are not data** (S65, excerpt only): brewing recipes are still in code. They are added
  by calling `BrewingRecipeRegistry#addRecipe` in `FMLCommonSetupEvent`. Anvil recipes are handled
  through an update event on the Forge bus. Loom recipes have their own section (contents not read).

For VESSEL: evidenced, custom ingredient and recipe types need mod code both to match and to sync to
clients (network read and write). **Not evidenced:** what a client without the mod sees for a recipe
with a custom ingredient. Since VESSEL's target includes vanilla and Bedrock clients, this needs its
own check. Code-only recipes (brewing, anvil, loom) cannot be put into any data pack.

### 20.6 Fragments seen only as search excerpts (do not treat as read)

- **Model transforms** (S66): the docs describe two systems, a deprecated vanilla one
  (`BakedModel#getTransforms`, `ItemTransforms`, `ItemTransform`) and the Forge one
  (`IForgeBakedModel#applyTransform`, a method patched into `BakedModel`). Most `BakedModel`
  implementations should return the default no-transform value. A `NONE` display context is used for
  display entities by default and by Forge when a block's render shape is the animated entity-block kind.
  Relevant to phase 16 (Display/Model backend); the full page is not read.
- **Datagen** (S67): `ForgeAdvancementProvider` with `AdvancementGenerator`, loot table providers with
  sub-provider entries and builders, and `DatapackBuiltinEntriesProvider` (a Forge extension of the
  registries generator that handles references to existing datapack registry objects). Only headings
  and one or two sentences were seen. Data generation is a build-time tool, so it matters for VESSEL only
  as a way to recognize generated files. Not read.

### 20.7 Still missing after pass 6 (from the section 19 checklist)

(Update v0.4.14: the Loot Tables page was afterwards read from the 1.20.x docs, see section 21. The latest 1.21.x version of that page is still unread. Update v0.4.16: Advancements and the Items introduction were read afterwards, see section 22. Update v0.4.19: Block Entities introduction and Model Loaders introduction were read afterwards, see section 23.)

Loot Tables page (server side), Recipes introduction, Advancements page, Tags List, client Models,
Texture Tinting, Item Properties, Model Extensions (5 pages), Model Loaders (4 pages, Transform only as
an excerpt), Items, BlockEntityWithoutLevelRenderer, Block Entities, BlockEntityRenderer, Block States,
Resources and Internationalization concept pages, Particles, Sounds, Menus, Screens, Codecs, Synchronizing
Entities, the datagen pages, and the still-unverified `showAsResourcePack` key.

---

## 21. Loot tables (pass 7, v0.4.13, 2026-09-29)

**Version caveat, stated first.** The loot tables page was read in full from the **1.20.x** documentation line
(S68), not from the latest 1.21.x line: the search tool did not return the 1.21.x page and it could not be
fetched. A NeoForge page for 1.20.6 (S71, excerpt, comparison only) shows that the Java API for looking up a
loot table changed between 1.20.x and later versions (a resource key and a reloadable-registries holder instead
of a loot data resolver). So the API names below are **1.20.x names and are unverified for 1.21.x and 26.x**.
The data-file facts (path, Forge's extra conditions and pool naming) are less likely to have changed, but that
is an inference, not something read.

### 21.1 How a loot table is found and run (S68, 1.20.x)

- The docs call loot tables "logic files" that describe what happens in various scenarios. Vanilla only uses
  them for item generation, but the system can be extended to other actions.
- Most vanilla tables are plain JSON, so a **data pack alone** can add one. A table is referenced by a
  `ResourceLocation` that points to `data/<namespace>/loot_tables/<path>.json`. The page sends the reader to
  the Minecraft wiki for the JSON format itself.
- In code (1.20.x names): `MinecraftServer#getLootData` returns a `LootDataResolver`, and its `getLootTable`
  returns the table.
- A table always runs with parameters: `LootParams` holds the level, luck, the `LootContextParam` values that
  describe the scenario and any dynamic data. It is built with `LootParams$Builder` and a
  `LootContextParamSet`. A `LootContext` wraps the params and can take a nullable resource location that names
  a seeded random instance.
- Three ways to use a table: `getRandomItemsRaw` (consumes the items), `getRandomItems` (returns them) and
  `fill` (fills a container). The docs note that these methods assume the caller handles item stacks.

### 21.2 What Forge adds to loot tables (S68, 1.20.x)

| Addition | What the page says (paraphrased) |
|---|---|
| `LootTableLoadEvent` | Fired on the Forge event bus whenever a table loads. Canceling it loads an empty table instead. The docs say **not** to change drops through it, use global loot modifiers (section 20.3). |
| Pool names | A pool can carry a `name` key. A pool without a name gets the prefix `custom#` followed by the hash code of the pool. |
| Looting | Tables are also affected by `LootingLevelEvent` (Forge bus), not only by the enchantment. |
| Extra context parameters | Forge lets the chest parameter set and the fishing parameter set carry a killer entity, because a chest minecart and a fishing hook are entities that can be broken or retracted. |
| Smelting function | `SmeltItemFunction` now returns the real result count of the smelting recipe times the number of drops (3 drops of a recipe that yields 3 give 9, not 3). |
| `forge:loot_table_id` | A condition, key `loot_table_id`, true only when the running table has that id. The docs say it is mostly used inside global loot modifiers. |
| `forge:can_tool_perform_action` | A condition, key `action` (example given: `axe_strip`), true when the tool in the context can perform that tool action. |

### 21.3 Excerpt-only facts (S67, S70, S71)

- **Datagen, latest docs (S67 excerpt):** loot conditions and functions each have a registered type. By
  default all conditions in a list must pass, and a condition can be built so that either one may pass.
  Loot functions change a result before it is handed to the output.
- **Old eras (S70 excerpt, 1.12.x docs):** a mod table was made known by calling `LootTableList.register`, and
  the docs of that era say it loaded from a path under `assets`, not `data`. Pools in mod tables needed a
  unique `name`. Custom loot conditions, loot functions and entity properties were registered through
  serializers. Vanilla let a user override any table by placing one in the world save. This is the E1b and E2
  situation (section 2). It was **not** read in full and should not be relied on for exact names.
- **NeoForge 1.20.6 page (S71 excerpt, comparison only):** carries the same two extra conditions but writes
  them with the `forge:` prefix, and says the loader adds them. This suggests the condition ids were kept when
  the projects split, which is relevant to future cross-loader mapping. It is an observation from one
  excerpt, not a verified rule.

### 21.4 What this means for VESSEL

Evidenced by the pages above:
- A Forge loot table is mostly a data-pack file, so the Datapack Compiler (phase 15) can carry it as data.
- The two `forge:` conditions, pool naming by hash code, and the killer-entity rule for chest and fishing sets
  are Forge behaviors on top of vanilla data.

Inference (marked, not read):
- A loader that does not know `forge:loot_table_id` or `forge:can_tool_perform_action` cannot evaluate them, so
  a table that uses them keeps needing Forge-side logic when it is converted. What a vanilla or Bedrock target
  does with an unknown condition type is **not verified** and must be checked before VESSEL relies on it.
- Loot tables are described as logic that the server runs. If that holds for every era, no loot table needs to
  reach a player's client, which would take loot tables out of the render bundle. Not verified.
- Custom loot conditions and functions are code registered through registries (section 20.3 for the same
  pattern), so they matter for the duplicate scan in `identity-and-collision.md` section 5.1.

### 21.5 Still missing on this topic

The latest 1.21.x loot tables page, the 26.x behavior (no docs exist), the full 1.12.x page, and the vanilla
JSON format itself (out of scope: it is Minecraft's, not Forge's).

---

## 22. Advancements and items (pass 8, v0.4.15, 2026-09-29)

**Version caveat, stated first.** Two pages were read in full: the advancements page of the docs line labeled
"latest" (S72) and the items introduction of the **1.21.x** docs line (S73). The advancements page carries a
1.21.x navigation menu, but its **code names are JSON-era names** (a trigger instance class with a JSON
serialize method, a trigger class with `getId` and a `createInstance` that reads a `JsonObject`). NeoForge's
docs for 1.20.6 and 1.21.1 (S76, excerpts) describe a different shape (a trigger instance interface and a
`Criterion` object). So it is **not verified** whether Forge 1.21.x still uses the names below. Treat every class
name in 22.1 as "as written on that page". Nothing here is verified for 26.x, for which no docs exist.

### 22.1 Advancements (S72, fetched in full)

- **Data first.** All vanilla advancements are JSON, so a data pack alone can add one. The page sends the reader
  to the Minecraft wiki for the JSON format and adds that advancements can be loaded conditionally
  (section 20.2).
- **How criteria work.** Criteria are tracked through triggers, which run when an action happens (killing an
  entity, an inventory change, breeding). When an advancement loads, its criteria are read and added as
  listeners on the matching trigger. A trigger function (usually called `trigger`) checks all listeners against
  the current state. The listeners of an advancement are removed only after the advancement is fully obtained.
- **Requirements.** An array of arrays of criterion names. The advancement completes once **all criteria of at
  least one inner array** are met. That is an OR over groups and an AND inside each group (the page's example:
  criteria 1 and 2, or criteria 3 and 4).
- **Vanilla triggers** are listed in the `CriteriaTriggers` class.
- **Custom triggers (names as written on the page):** two classes.
  - The instance class (`AbstractCriterionTriggerInstance` subclass) is one criterion from the `criteria`
    object. It holds the conditions, tells whether given inputs match (usually a method named `matches`), and
    writes itself to JSON (`serializeToJson`) for data generation. Its constructor takes the trigger's registry
    name and the player conditions as a `ContextAwarePredicate`.
  - The trigger class (`SimpleCriterionTrigger<T>`) supplies the trigger's registry name (`getId`), builds an
    instance from JSON (`createInstance`), and offers a `trigger` method that calls
    `SimpleCriterionTrigger#trigger` so all listeners are checked.
- **Registration.** The trigger is registered with `CriteriaTriggers#register` during `FMLCommonSetupEvent`. The
  page warns that the call must go through `enqueueWork`, because it is not thread-safe. This is the same rule
  and the same event as ingredient and condition serializers (section 20.2, 20.5).
- **Calling it.** The mod calls its trigger method wherever the action happens, passing the `ServerPlayer` and
  whatever the instance's match method needs.
- **Rewards.** An advancement can give experience, loot tables, recipes for the recipe book, and run a function
  as a creative player. The keys are `experience`, `loot`, `recipes` and `function`.

### 22.2 Items (S73, fetched in full, 1.21.x docs)

- An item is created by making an `Item` with an `Item$Properties` object. The methods listed: `requiredFeatures`
  (feature flags needed to see the item in its creative tab), `durability` (if above 0, two item properties named
  "damaged" and "damage" are added), `stacksTo` (maximum stack size, an item cannot be both damageable and
  stackable), `setNoRepair`, `craftRemainder` (the container left behind, like the empty bucket from lava).
- A complex item subclasses `Item` and overrides methods. The page gives no more detail on that.
- **Creative tabs.** An item is added to a tab through `BuildCreativeModeTabContentsEvent` on the mod event bus,
  with `accept`. Feature flags and an operator-tab permission flag can hide it. A custom tab is a **registered
  object** made with `CreativeModeTab#builder` (title, icon, displayed items) plus Forge's extras (tab image,
  label and slot colors, ordering).
- Items must be registered (the page links to the registries page, section 4 of this file).
- **Not on this page:** item components or NBT layout, how an item gets its model, item use behavior, and
  anything about how a player's client shows the item. The client-side pages (item properties,
  `BlockEntityWithoutLevelRenderer`) are unread.

### 22.3 Excerpt-only facts (S74, S75, S76)

- **1.20 migration (S74, Architectury docs excerpt):** in 1.20 Minecraft made creative tabs a normal registry
  entry, and a loot table lookup object called `LootTables` was replaced by `LootDataManager`. This matches the
  version caveat in section 21 (loot table API names changed at 1.20) and shows that the older `Item$Properties`
  `tab` method (still seen in a 1.19.2 docs excerpt) is a 1.19-and-earlier form.
- **Tags on custom registries (S75, 1.18.x docs excerpt):** a mod's own registry can have tags. The tag key is
  made through `DeferredRegister#createTagKey` or `createOptionalTagKey`, and membership is checked through the
  registry returned by `DeferredRegister#makeRegistry`. Not checked for 1.21.x.
- **NeoForge advancements (S76, comparison only):** the 1.20.6 and 1.21.1 pages describe the trigger instance
  and the criterion as separate objects and register the trigger through a normal registry supplier, not
  through a setup event. This is one more sign that the Forge and NeoForge APIs diverged here.

### 22.4 What this means for VESSEL

Evidenced:
- Advancements with vanilla triggers are pure data, so the Datapack Compiler can carry them. Their
  `requirements` semantics (OR of AND groups) are precise and must be followed by any converter.
- A custom criteria trigger is server code registered in `FMLCommonSetupEvent`, outside the usual `RegisterEvent`
  registry step. A creative tab is a registry entry, so it appears in the registry scan of
  `identity-and-collision.md` section 5.1.

Inference (marked, not read):
- Items and creative tabs are what a player's client actually sees, so they are core input for the render bundle
  (phase 14), but this pass found nothing about the visual side of an item.

Question for Shiroz, **answered "yes" on 2026-09-29** (see `identity-and-collision.md` section 5.3): trigger ids added with `CriteriaTriggers#register`, and ingredient and
condition serializers added with `CraftingHelper#register`, are not filled through Forge's `RegisterEvent`. Should
VESSEL treat their ids as registry keys for the duplicate rule (decision 8, hard stop on the same key in the same
registry)? What the docs say happens on a duplicate id for these calls was **not read**.

### 22.5 Still missing on these topics

The visual half of items (item properties page, `BlockEntityWithoutLevelRenderer`, client models), the exact API
of advancements in Forge 1.21.x (page text looks older than its menu), and 26.x for both.

---

## 23. Block entities and model loaders (pass 9, v0.4.18, 2026-09-29)

**Version caveat, stated first.** The block entities page (S77) and the model loaders introduction (S78) were read in
full from the docs line labeled "latest" (menu says 1.21.x). Their code names are **1.20.x-era names**: the data methods
take a `CompoundTag`, and update packets are built without a registry lookup argument. Two excerpts show that later
versions changed this: NeoForge's newer docs (S82) and Fabric's 26.2 docs (S81) describe `saveAdditional` taking a
value-output object, and `getUpdateTag` taking a registry lookup. So every method signature in 23.1 is **unverified for
1.21.x and for 26.x**. The concepts (what a block entity is, the three ways to sync it) are less likely to have changed,
but that is an inference. Model extension keys (23.3) and the baked model (23.4) come from search excerpts only.

### 23.1 Block entities (S77, fetched in full)

- **What they are.** Simplified entities bound to a block. They store dynamic data, run tick tasks and do dynamic
  rendering. The page's examples: chest inventories, furnace smelting, beacon area effects, and in mods quarries, sorting
  machines, pipes and displays. It warns that they can cause lag and should be avoided when possible.
- **Registering.** A block entity instance is created and removed dynamically, so it is **not a registry object**. What is
  registered is its **`BlockEntityType`**, like any other registry object. The type is built with
  `BlockEntityType$Builder#of` (a supplier taking a position and a block state, plus the blocks it may attach to) and
  `build`, which takes a type used only by the data fixer system and may be `null`. The block entity's constructor passes
  its registered type to `super`.
- **Attaching it to a block.** The block subclass implements `EntityBlock` and returns a new instance from
  `newBlockEntity(BlockPos, BlockState)`.
- **Saving.** Override `saveAdditional(CompoundTag)` and `load(CompoundTag)` (1.20.x names). They run when the chunk that
  holds the block entity is saved or loaded. Call `setChanged` whenever data changes, or the chunk may be skipped when the
  level saves. Calling the `super` methods is required. The tag names `id`, `x`, `y`, `z`, `ForgeData` and `ForgeCaps` are
  **reserved** by the `super` methods (this ties to the capability storage in section 9).
- **Ticking.** Override `EntityBlock#getTicker(Level, BlockState, BlockEntityType)` and return a `BlockEntityTicker`. It can
  differ per logical side. The page says the ticker runs every tick, advises against heavy work in it, and notes that a
  second may hold fewer than 20 ticks but never more.
- **Syncing to the client, three ways:**
  1. **On chunk load:** `getUpdateTag` builds the data and `IForgeBlockEntity#handleUpdateTag` reads it.
  2. **On block update:** `getUpdatePacket` returns a `ClientboundBlockEntityDataPacket` (by default made from
     `getUpdateTag`); `onDataPacket` reads it and by default defers to `load`. The server triggers it with
     `Level#sendBlockUpdated(pos, oldState, newState, flags)`, and `flags` must include 2 (`Block#UPDATE_CLIENTS`).
  3. **Custom network message:** sent with `SimpleChannel#send` to a `PacketDistributor` target, usually the most
     optimized. The page warns that the block entity may already be gone when the message arrives, so the receiver must
     check that and that the chunk is loaded (`Level#hasChunkAt`).
- The page warns that sending too much (for example an inventory that a container menu already syncs) can congest the
  network.

### 23.2 Model loaders introduction (S78, fetched in full)

- A model is a shape. Most are vanilla JSON. Other formats are turned into `IUnbakedGeometry` objects by an
  `IGeometryLoader` **at runtime**. Forge supplies loaders for WaveFront OBJ files, buckets, composite models, models in
  different render layers, and a reimplementation of vanilla's `builtin/generated` item model. Everything ends up as a
  `BakedModel`.
- A top-level `loader` key in a model JSON makes the `elements` key be ignored unless that loader uses it. The other
  vanilla keys stay available in the unbaked `BlockModel`.
- **OBJ loader:** the JSON names `forge:obj`. The model location must be in a registered namespace and its path must end
  in `.obj`. A `.mtl` file with the same name in the same place is used automatically, but its texture paths usually need
  manual editing to match the textures in the JSON. Texture V may be flipped depending on the tool that made the model;
  the JSON key on this page is `flip_v`. (An older community wiki excerpt, S84, writes `flip-v`, so the key name changed
  between versions; when is not known.)

### 23.3 Model extension keys (excerpts only, S79)

Three optional top-level model JSON keys, all of which a custom loader may ignore:
- `transform`: asks the loader to apply a transform to all geometry right before the block state rotations (blocks) or
  before display transforms (items). It is read in code through `IGeometryBakingContext#getRootTransform()` in
  `IUnbakedGeometry#bake`.
- `visibility`: chooses which parts of the model are baked. What a "part" is depends on the loader. Of Forge's own loaders
  only the composite loader and the OBJ loader use it. A child model can override the visibility of a composite parent.
- `render_type`: suggests a render type. If it is missing the loader picks, often falling back to
  `ItemBlockRenderTypes#getRenderLayers()`. Forge supplies a set of named chunk and entity render types, and a mod can add
  its own through `RegisterNamedRenderTypesEvent`.

### 23.4 Baked model (excerpt only, S80, 1.20.x docs)

`BakedModel` is what `UnbakedModel#bake` (vanilla) or `IUnbakedGeometry#bake` (custom loader) returns. Unlike the unbaked
forms, it is not abstract about items and blocks: it supplies the `ItemOverrides` (used only when rendered as an item), an
ambient-occlusion flag, and a flag that, when rendering as an item, makes the model be skipped in favor of
`BlockEntityWithoutLevelRenderer#renderByItem`. Particle icons now depend on `ModelData` (the no-argument vanilla method is
deprecated), and extra render data can be passed through `ModelProperty` values (example: the composite loader's own data).

### 23.5 Comparison excerpts (S82, S83, S81)

- **Custom loaders, NeoForge docs (S83):** a custom loader is a geometry loader class, a geometry class and a client-side
  event (`ModelEvent.RegisterGeometryLoaders`) that registers the loader. The Forge page for this was not read.
- **Loader ids in NeoForge 1.21.3 (S83):** the ids carry the `neoforge:` prefix and list composite, fluid container,
  elements, empty, item layers, OBJ and separate transforms loaders. Whether Forge uses the same set is **not verified**.
  The NeoForge OBJ text says the loader allows arbitrary shapes, including triangles and circles.
- **Block entities in newer docs (S81, S82):** Fabric's 26.2 page and NeoForge's newer pages show value-input and
  value-output objects in the save and load methods, and a registry lookup argument in `getUpdateTag`. They confirm that
  the CompoundTag-based names of 23.1 are not the 26.2 shape.

### 23.6 What this means for VESSEL

Evidenced:
- A `BlockEntityType` is a registry entry, so it belongs in the duplicate scan (decision 8). The block entity itself is not.
- Block entity data reaches a client through update tags, update packets or a mod's own message. A client without the mod
  cannot read custom messages. This bears on decision 11 (one broadcast per state change): the block update route is
  triggered per position, which fits that principle, but the docs do not say whether the packet is built once per tracker
  or once per player. That is **unverified**.
- The ticker runs every tick on the server and can differ per logical side. That matters for the Folia work (phase 10), but
  nothing about threading was read on this page.

Inference (marked):
- Model loaders run on the client at runtime. A vanilla or Bedrock client cannot run them, so the Resource Pack Compiler
  (phase 14) or the Display/Model backend (phase 16) must turn these models into formats the target client understands
  before it joins. The OBJ case is the hard one: the NeoForge text says OBJ allows arbitrary shapes, so a conversion to
  vanilla JSON models may lose detail. **Decided by Shiroz on 2026-09-29 (option C):** simple models are converted into vanilla JSON models, and complex models go through the Display/Model backend (phase 16). Where the line between simple and complex lies is **not defined yet**.

### 23.7 Still missing on these topics

(Update v0.4.22: BlockEntityRenderer, BlockEntityWithoutLevelRenderer, Item Overrides and Item Properties were read afterwards from the 1.19.x docs, see section 24.)

(Update v0.4.42: Root Transforms, Render Types, Part Visibility, Face Data, Baked Model, Transform and the client Models introduction were read afterwards, see section 33.)

BlockEntityRenderer and `BlockEntityWithoutLevelRenderer`, Item Overrides, the full Transform, Root Transforms, Render
Types, Part Visibility and Face Data pages, the Baked Model page as fetched text, the client model pages (models, texture
tinting, item properties), the Forge page on writing a custom loader, and 26.x for all of it.

---

## 24. Client-side renderers and item model state (pass 10, v0.4.21, 2026-09-29)

**Version caveat, stated first.** All four pages below were read in full from the **1.19.x** docs line (S85 to S88), because
the search tool returned that line and its pages could be fetched. The item pages use `ItemDisplayContext` and the model
pages use `BakedModel`, so the text is not older than 1.19.3. But excerpts of newer docs (S90) show that the block entity
renderer API was **redesigned** later: the renderer gets a second type parameter, a separate "render state" object, and
submits its output to a queue instead of drawing through a buffer each frame. So the block entity renderer names in 24.1
are **unverified for 1.21.x and 26.x, and probably wrong there** (partly corrected in v0.4.23: Forge's own "latest" renderer page shows the old shape, see 25.4). For the item pages (24.2 to 24.4) no newer text was
read either way, so nothing can be said about whether they changed.

### 24.1 BlockEntityRenderer (S85 in full, S89 and S90 excerpts)

- A BER draws blocks in ways a static baked model cannot (JSON, OBJ, B3D and others). The block must have a block entity.
- **One BER exists per `BlockEntityType`.** Anything specific to one placed block must be stored in the block entity, not in
  the BER. The page's example: a counter kept in the BER would count once per frame for every block entity of that type.
- `render` runs **every frame**. Parameters (1.19.x): the block entity, the partial tick fraction, a `PoseStack` already
  offset to the block's position, a `MultiBufferSource`, the combined light value, and the combined overlay value (usually
  `OverlayTexture#NO_OVERLAY`, 655,360).
- **Registration:** subscribe to `EntityRenderersEvent$RegisterRenderers` on the mod event bus and call
  `#registerBlockEntityRenderer`. (S89, an older community wiki revision, shows a second route:
  `BlockEntityRenderers#register` called inside `FMLClientSetupEvent#enqueueWork`, because it is not thread-safe. Which of the
  two routes the official docs used when is not known.)
- **Newer shape (S90, excerpts):** NeoForge's current page and Fabric's 26.2 page show a renderer with a render-state type,
  methods to create and fill that state from the block entity, and a render method that submits to a command queue.

### 24.2 BlockEntityWithoutLevelRenderer (S86, fetched in full)

- A method for dynamic rendering on **items**. The page calls it much simpler than the older system that needed a block
  entity and gave no access to the item stack.
- It renders through `renderByItem(ItemStack, ItemDisplayContext, PoseStack, MultiBufferSource, light, overlay)`.
- It is used only if the item's model returns true for `BakedModel#isCustomRenderer`. Otherwise `ItemRenderer` uses its own
  default. **Blocks also use it** when `Block#getRenderShape` returns `RenderShape#ENTITYBLOCK_ANIMATED`.
- It is attached in `Item#initializeClient`, by handing an anonymous `IClientItemExtensions` to the consumer and overriding
  `getCustomRenderer`. The page says each mod should keep **one** instance of its custom renderer.

### 24.3 Item Overrides (S87, fetched in full)

- `ItemOverrides` lets a `BakedModel` look at an `ItemStack` and return a different `BakedModel`, which then replaces the
  first. It is a function of (model, stack, client level, living entity, an int) that returns a model. Vanilla uses it for
  item property overrides.
- `resolve` does the work and **must not change the level**. `getOverrides` returns an immutable list of `BakedOverride`
  objects (empty if none apply).
- `BakedOverride` is one entry of the `overrides` array in a vanilla item model JSON: a `predicate` (property name to a
  minimum value) and a target `model`.

### 24.4 Item Properties (S88, fetched in full)

- An item property assigns a **float** to every `ItemStack` it is registered for. Vanilla model JSON uses these floats in
  `overrides` to switch models. The page's examples are the bow pull, the compass and the clock. The values are
  continuous, generally 0.0 to 1.0, so a resource pack can add as many models along that range as it wants.
- `ItemProperties#register(item, name, function)` adds one. The function takes the stack, the client level (may be null),
  the holding living entity (may be null) and an int (may be 0), and returns the float. `registerGeneric` adds a property
  to every item. Use the mod id as the property namespace.
- The call belongs in `FMLClientSetupEvent` through `enqueueWork`, because the structures are not thread-safe. The class
  exists **only on the client**.
- A predicate matches values **greater than or equal to** the given number. If several overrides match, the **last** one in
  the list wins. If none match, the default model is used. `ClampedItemPropertyFunction` (clamps to 0 to 1) is preferred
  over the deprecated plain function.

### 24.5 What this means for VESSEL

Evidenced:
- Everything on these pages is **client code** (`ItemProperties` does not exist on the server, renderers run each frame on
  the client). A vanilla or Bedrock client cannot run it.
- Item overrides in JSON refer to property names such as `examplemod:power`. Those floats come from mod client code.

Inference (marked, not read):
- On a client that does not know a mod's property name, such an override would never match and the item would show its
  default model. What selector a vanilla or Bedrock client does support, and how VESSEL would feed it, is not researched
  here.

**Proposal for the open "simple versus complex" line of decision 21 (accepted by Shiroz on 2026-09-29, with the grey case decided in 25.7):**
- **Complex** (Display/Model backend, phase 16): anything that needs code at draw time or a per-item state, meaning block
  entity renderers, `BlockEntityWithoutLevelRenderer` items, `ItemOverrides#resolve`, and models whose look depends on an
  item property.
- **Simple** (convert to vanilla JSON): static shapes with no per-frame or per-stack code, such as plain JSON models and OBJ
  models that fit what vanilla JSON can hold.
- A static OBJ model that does **not** fit vanilla JSON is the grey case; **decided on 2026-09-29, see section 25.7.**

### 24.6 Still missing on these topics

(Update v0.4.24: the "latest" renderer page, Block States, Texture Tinting and the Recipes introduction were read afterwards, see section 25.)

The latest 1.21.x versions of all four pages, the current shape of the block entity renderer in Forge (only NeoForge and
Fabric excerpts seen), Face Data, Texture Tinting, the client Models introduction, Block States, and 26.x for all of it.

---

## 25. Block states, texture tinting, recipes introduction, and the block entity renderer check (pass 11, v0.4.23, 2026-09-29)

**Version note, stated first.** All four pages below were read in full from the docs line labeled "latest" (menu says
1.21.x): Block States (S92), Texture Tinting (S93), the Recipes introduction (S94) and the BlockEntityRenderer page (S91).
These are the official Forge docs and are **not** the older 1.19.x line used in section 24. Code names are still
1.20.x-era in places (for example `Container`, `BlockColor`), so it is not verified that Forge's code for 1.21.x or 26.x
uses the same names. No 26.x docs exist.

### 25.1 Block States (S92)

- **Legacy (Minecraft 1.7 and before):** a block stored extra state as **metadata**, a plain number next to the block id
  with no meaning except what the code comments said. The page's example: a stair needs one number per facing and half.
- **From 1.8:** metadata and numeric block ids were replaced by the **block state system**. Each property is a
  `Property<T>`. A `BlockState` is the unique pair of a `Block` and a map from properties to values. The page shows the
  notation `minecraft:stone_button[facing=east,powered=true]`, which replaces "stone button with metadata 9".
- **Limits:** block states are **immutable**, and **every combination of properties is generated when the game starts**.
  Many properties or values slow the loading. Only the most basic properties belong in a state, the rest belongs in a
  block entity or in a separate block. Rule of thumb from the page: if it has a different name, it should be a separate
  block (the chair example: direction is a property, wood type is a different block).
- **Property kinds:** `IntegerProperty#create(name, min, max)`, `BooleanProperty#create(name)`, `EnumProperty#create(name,
  enumClass)` (an overload takes a subset of the values), `DirectionProperty` (with helpers for horizontal directions or one
  axis). `BlockStateProperties` holds shared vanilla properties and should be reused.
- **Implementing:** override `Block#createBlockStateDefinition(StateDefinition$Builder)` and call `add(...)` for each
  property. The default state is chosen automatically and can be changed with `Block#registerDefaultState` in the
  constructor. `Block#getStateForPlacement(BlockPlaceContext)` changes what state placement produces.
- **Using:** `getValue`, `setValue` (returns the already existing state with those values), `Level#setBlockAndUpdate`,
  `Level#getBlockState`, `Block#defaultBlockState`. Because all states exist once, comparing with `==` is encouraged.

### 25.2 Texture Tinting (S93)

- Model faces can carry an integer **tint index**. A `BlockColor` (takes a state, a nullable `BlockAndTintGetter`, a nullable
  `BlockPos`, and the tint index) or an `ItemColor` (takes a stack and the tint index) turns it into a color multiplier.
- The multiplier is four unsigned bytes, alpha, red, green, blue (most significant first). For each pixel and channel the
  result is the base value times the multiplier byte divided by 255. **Blocks ignore the alpha channel.** Grass is the
  page's example: its texture is white and gray, the handler returns a mostly green multiplier in warm biomes.
- An item that inherits from `builtin/generated` gets one tint index per layer (layer 0 is index 0, and so on).
- **Registration:** `BlockColor` through `RegisterColorHandlersEvent$Block`, `ItemColor` through
  `RegisterColorHandlersEvent$Item`, both with `register(handler, targets...)`. Coloring a block does **not** color its
  `BlockItem`, which needs its own `ItemColor`. The item overload also accepts blocks and then registers for
  `Block#asItem`.

### 25.3 Recipes introduction (S94)

- Most vanilla recipes are JSON, so a data pack alone can add one. The page points to the Minecraft wiki for the format.
- A recipe reaches the recipe book through an advancement. Recipe advancements always have the parent
  `minecraft:recipes/root` (so they stay off the advancement screen). The default criterion uses the trigger
  `minecraft:recipe_unlocked` for that recipe, and the `requirements` array is ORed (section 22.1).
- `RecipeManager` loads and stores recipes. The two lookup methods: `getRecipeFor` (first match) and `getRecipesFor` (all
  matches). Both take a `RecipeType`, a `Container` with the inputs and the level, which is passed on to `Recipe#matches`.
  Forge's `RecipeWrapper` is a `Container` that wraps an `IItemHandler`.
- **Forge additions to the recipe schema:**
  - Except for stonecutting, vanilla serializers accept the `result` as a full item stack object with `item`, `count` and
    `nbt`. The `nbt` value may be a string holding stringified NBT for data JSON cannot express (such as int arrays).
  - Recipes and their unlocking advancements can be loaded conditionally (section 20.2).
  - **Larger crafting grids:** `ShapedRecipe#setCraftingSize(width, height)` raises the vanilla 3 by 3 limit. The page says
    it is **not thread-safe** and must run through `FMLCommonSetupEvent#enqueueWork`.
  - Extra ingredient types (section 20.5).

### 25.4 The block entity renderer check (S91)

Section 24 said the block entity renderer names were "unverified and probably wrong" because NeoForge and Fabric excerpts
show a redesigned renderer. The check now made: Forge's **own "latest" page for the renderer (S91) has the same content as
the 1.19.x page** (one parameter is spelled `partialTick` instead of `partialTicks`). So Forge's docs show **no**
redesign. Two readings remain, and nothing read decides between them: Forge's code for its 1.21.x line kept the old shape,
or Forge's docs lag behind its code. The redesigned shape is documented only by NeoForge and Fabric. For 26.x nothing is known. (Resolved in v0.4.27 from source: Forge 26.3 has the redesigned renderer, see section 26.4.)

### 25.5 What this means for VESSEL

Evidenced:
- Two things are numbers-to-meaning mappings VESSEL must handle for old versions. In Minecraft 1.7 and earlier, block state was
  a **numeric metadata value with no attached meaning**. Decision 13 makes VESSEL support versions from the first release, so the
  pre-1.8 Forge line (eras E1a and E1b, section 2) needs a metadata-to-property layer. The docs give no such mapping (open).
- All state combinations are generated at startup, so the registration scan of `identity-and-collision.md` section 5.1 runs
  against a fully expanded state set, not against a list of blocks.
- A tint index is only a number. The color comes from **client code** (`BlockColor`, `ItemColor`), computed from the position
  and state. A client without the mod cannot run it.
- `ShapedRecipe#setCraftingSize` changes a global limit at setup time, so a data pack compiler that validates recipe shapes
  must know the effective grid size.

Inference (marked, not read):
- Tint handlers depend on position or state, so they fit the "complex" side of the pending proposal in section 24.5
  (code at draw time or per-item state). Adding them to that list was proposed here and **accepted by Shiroz on 2026-09-29** (section 25.7).
- A recipe `result` with `nbt` is a Forge schema extension. A converter must translate it for targets that do not read it.
  What those targets do is not researched here.

### 25.6 Still missing on these topics

The latest versions of the item renderer, item overrides and item properties pages (only 1.19.x read), the full custom recipes
and non-datapack recipes pages, face data, root transforms, render types and part visibility (full pages), the client models
introduction, the tags list, and 26.x for all of it.

(Update v0.4.48: the tags list was taken from the code of 12 heads in pass 21, see 35.2; the docs page "Tags List" itself was not fetched.)

(Update v0.4.42: the latest versions of the item renderer, item overrides and item properties pages, face data, root transforms, render types, part visibility and the client models introduction were read in pass 19, see section 33. Still missing: the tags list, the full custom recipes and non-datapack recipes pages, and 26.x for the recipes pages.)

### 25.7 Decision on the simple/complex line and the grey case (2026-09-29, added v0.4.25)

Shiroz accepted the proposal of 24.5 including the tint handlers of 25.5, and **delegated** the grey case (a static OBJ model
that vanilla JSON cannot hold) with the instruction to pick the most reasonable option after careful thought.

Rule now in force:
- **Complex** (Display/Model backend, phase 16): needs code at draw time or a per-item or per-position state. That covers block
  entity renderers, item renderers (`BlockEntityWithoutLevelRenderer`), item override resolution, models driven by an item
  property, and color tint handlers (`BlockColor`, `ItemColor`).
- **Simple** (converted into vanilla JSON models): static shapes with no such code, **when vanilla JSON can hold them exactly**.
- **Grey case (chosen by Claude on delegation):** a static model that vanilla JSON cannot hold exactly goes to the Display/Model
  backend as well. It is **not** approximated silently. If the backend also cannot hold it, VESSEL may use a best-effort
  approximation, but only with a yellow console warning that names the model.

Reasoning for the choice, so it can be challenged: (1) decision 21 already defines the backend for everything that plain JSON
cannot do, so the grey case adds no new route; (2) silently losing shape detail would be a visible, hard-to-trace defect for a
player, while a routing choice is invisible to them; (3) the warning keeps the last resort honest. The cost is that the backend
must handle static meshes, which phase 16 has not designed, so the rule depends on that design. Open: the exact "fits vanilla
JSON exactly" test. It needs the vanilla model format researched (phase 14) and is **not** answered by any Forge page.

---

## 26. Forge source code: repository map, 1.7.10 metadata, and the 26.3 tree (pass 12, v0.4.26, 2026-09-30)

**Why this pass exists.** Shiroz asked that the pre-1.8 metadata layer be researched from **Forge's source code first**,
with other sources as support. This pass is therefore the first one that reads code instead of documentation, and it also
tests the documentation against the code. Source code is the strongest evidence in this file: it says what Forge does, not what
a page says it does.

**Method and limits.** Branch and tag lists come from `git ls-remote`. Two branches were shallow-cloned (depth 1, so **no
history**): `1.7.10` (head `9274e4fe435c`, 2017-06-07, subject "Fix log4j configuration") and `26.3` (head `932c6e96491f`,
2026-09-28, subject "Fix checkLicense tasks with projectInfo task"). Build files of other branches were read as raw files.
Every value is the state of a branch **head on the run date**, which may differ from the state at the first build of that
Minecraft version. The raw lists are stored in `source-map.json` (v0.1.0). The other sources are the Minecraft Wiki
pre-flattening data value pages (excerpts) and the `PrismarineJS/minecraft-data` repository (commit `f5d7d74604d8`,
2026-09-29), sparse-checked-out for versions 1.7 and 1.8.

### 26.1 Repository map (closes the "source repository revision per era" part of section 15, partly)

- **44 branch heads.** Version branches: `1.6`, `1.7.10`, `1.8`, `1.8.8`, `1.8.9`, `1.9`, `1.9.0`, `1.9.4`, `1.10.x`,
  `1.11.x`, `1.12.x`, `1.13.x`, `1.14.x`, `1.15.x`, `1.16.x`, `1.17.x`, `1.18.x`, `1.19.2`, `1.19.x`, `1.20.1`, `1.20.4`,
  `1.20.x`, `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.8`, `1.21.10`, `1.21.11`, `26.1`, `26.1.1`, `26.1.2`, `26.2`,
  `26.3` (34 names). Others: `failtests`, `mc172`, `noci-1.10.2-mezz`, `noci-1.11.x`, `noci-1.12.x`,
  `noci-1.12.x-regrewrite`, `noci-1.14.4`, `noci-omnibus/tags`, `noci-omnibus/typos`, `v164-pregradle` (10 names).
- **No branch** exists for Minecraft 1.1 to 1.5.x, and none for 1.21.2, 1.21.6, 1.21.7 and 1.21.9, although section 1
  lists official builds for 1.21.6, 1.21.7 and 1.21.9. Where their code lives was not checked.
- **131 tag names.** Early tags: `1.0.0`, `1.0.1`, `1.0.3`, `1.0.4`, `1.2.1`, `1.2.2`, `1.2.3`, `1.2.3@183`, `1.2.4`,
  `1.2.5`, `1.2.5@213`, `1.3.0`, `1.3.1`, `1.3.2`. Then **nothing until `23.5`**, followed by one tag per Forge
  major.minor series (`24.0`, `25.0`, ... `65.0`, `65.1`, `66.0`). What a series tag points at (first build or last build
  of the series) was not checked.
- Head commits read: `1.6` `ba4637f5a3fb`, `1.7.10` `9274e4fe435c`, `1.8` `ab39b7e4e00c`, `1.12.x` `3effde4f1fc9`, `1.16.x`
  `b3426008d3c3`, `1.20.1` `3c3f49601563`, `1.21.1` `a11d1936c53f`, `26.1` `38ebbf189ccb`, `26.2` `41dfdc8f8b04`, `26.3`
  `932c6e96491f`.
- Era fit (only where the counts match): era E1a has four versions (1.1, 1.2.3, 1.2.4, 1.2.5) and tags exist for 1.2.3, 1.2.4
  and 1.2.5 but not for 1.1. Era E5 has five versions (26.1, 26.1.1, 26.1.2, 26.2, 26.3) and each has its own branch.
  The fit of the other eras to branches was not worked out. The Javadoc inventory was **not** started.

### 26.2 Forge 1.7.10 (source): how block metadata is handled

Layout: `src`, `patches/minecraft` (patches on Minecraft classes), and `fml` with its own `src`, `patches` and `conf`. 903
files. Findings:

- **Metadata is a plain int everywhere.** Forge's patched `Block` adds methods that take the metadata as a parameter, for
  example `hasTileEntity(int metadata)`, `createTileEntity(world, metadata)`, `quantityDropped(meta, fortune, random)`,
  `getDrops(world, x, y, z, metadata, fortune)`, `canHarvestBlock(player, meta)`, `canSilkHarvest(..., metadata)`,
  `getExpDrop(world, metadata, fortune)`. `BlockSnapshot` stores `int meta`. `ItemBlock#placeBlockAt` takes
  `int metadata` and calls `world.setBlock(x, y, z, block, metadata, 3)`. `OreDictionary.WILDCARD_VALUE` is
  `Short.MAX_VALUE` (32767), the "any metadata" marker.
- **Forge hardcodes the meaning of metadata bits for vanilla blocks in two places.**
  1. `Block#isSideSolid` and `Block#canCreatureSpawn` (patched into `Block`): slab, `(meta & 8) == 8` means the upper half;
     stairs, `(meta & 4) != 0` means flipped and `(meta & 3)` is the facing; snow, `(meta & 7) == 7` means a full block;
     farmland and hopper are special-cased by class.
  2. `RotationHelper` (439 lines, "a helper for vanilla blocks", not for mods): a table of which bits of the metadata hold the
     orientation, per block type, and how to rotate them.

  | Mask | Blocks (from `rotateVanillaBlock`) | How the orientation bits map |
  |---|---|---|
  | `0x3` | bed, pumpkin, fence gate, end portal frame, tripwire hook, cocoa | 0 south, 1 west, 2 north, 3 east |
  | `0xF` | rail | 0 and 1 toggle; 2 to 5 ascending (2 east, 3 west, 4 north, 5 south); 6 to 9 corner (6 west, 7 north, 8 east, 9 south) |
  | `0x7` | powered rail, detector rail | 0 and 1 toggle; 2 to 5 ascending, same map as rail |
  | `0xF` | standing sign | 16 values, a turn adds 4 or 12 (mod 16) |
  | `0x7` | chest, ender chest, furnace, ladder, wall sign | direction taken from the value through `ForgeDirection#getOrientation` |
  | `0x7` | dispenser, piston base, piston extension, hopper, skull | same direct mapping |
  | `0x7` | button | `6 - meta` |
  | `0x7` | lever | 1 east, 2 west, 3 south, 4 north, 5 up, 7 down (6 treated as 5, 0 treated as 7) |
  | `0x3` | stairs | `5 - meta` |
  | `0x3` | door | 0 west, 1 north, 2 east, 3 south; bit `0x8` (upper half) is never rotated |
  | `0x3` | redstone repeater, comparator | 0 north, 1 east, 2 south, 3 west |
  | `0x3` | trapdoor | opposite of the direction for `meta + 2` |
  | `0xF` | huge mushroom | even values use the side map (2 north, 4 west, 6 east, 8 south), odd values the corner map (1 east, 3 south, 7 north, 9 west) |
  | `0xF` | vine | bits shift left by one, the top bit wraps to bit 0 |
  | `0xF` | torch | `6 - meta` |
  | `0x1` | anvil | 0 south, 1 east |
  | `0xC` | log | 0 up, 4 east, 8 south (bits 2 and 3 hold the axis) |

  The `ForgeDirection` ordinal order behind the "direct mapping" rows was not read.
- **What this gives and what it does not.** It gives **orientation bits for about 22 vanilla block kinds** plus a few
  more bit meanings. It does **not** give the other bits (powered, open, half, variant) and it says nothing about mod blocks.
  The meaning of a mod block's metadata lives in that mod's own code. The two supporting sources do not fill the gap: the
  minecraft-data files for 1.7 and 1.8 list, per block id, only a `variations` array of display names by metadata (16 of 176
  blocks for 1.7, 38 of 198 for 1.8) and have no property structure; the Minecraft Wiki pre-flattening pages are human
  tables, read only as excerpts.
- **Registry classes of this era** (names only, not read): `GameData`, `GameRegistry`, `FMLControlledNamespacedRegistry`,
  `ObjectHolderRegistry`, `ItemStackHolderInjector`, `RegistryDelegate`, `ExistingSubstitutionException`,
  `IncompatibleSubstitutionException`. The last two suggest a substitution mechanism for blocks and items. What it does
  exactly was not checked.

### 26.3 Forge 26.3 (source): layout, build facts and pack loading

- **Layout:** root modules `build-logic`, `fmlcore` (45 Java files), `fmlearlydisplay` (10), `fmlloader` (65),
  `forge-examiner` (3), `forge-tests` (53), `forge-transformers` (3), `javafmllanguage` (5), `lowcodelanguage` (2),
  `mclanguage` (2), plus `patches/minecraft` (**623 patch files**), `src`, `docs`, `ide`, `server_files`. 3,026 files.
- **`minecraft.versions.toml`:** `java = "25"`, `minecraft = "26.3"`, `minecraft-next = "26.4"`, `mcp = "20260918.230105"`,
  `changelog-base = "66.0"`. So the branch already tracks 26.4 as the next target, and 26.3 builds on Java 25.
- **EventBus:** `7.0.6` (`gradle/libs.versions.toml`). See 26.5 for the other branches.
- **Mod file keys, answering the open `showAsResourcePack` item.** `ModFileInfo` reads these optional keys from the
  file-level part of `mods.toml`, each defaulting to false unless noted: `license` (default empty), `showAsResourcePack`,
  `clientSideOnly`, `showAsDataPack`, `services` (list, default empty), `issueTrackerURL`. `showAsDataPack` is **not** in the
  documentation pages read so far. Synthetic mod files (`AbstractModProvider`) return false for both show flags.
- **How the pack is used (`ResourcePackLoader`).** For each mod file it builds a pack named `mod:<modid>` and reads its
  metadata with `Pack.readPackMetadata`. If that returns nothing, it logs a warning and adds a loading warning with the key
  `fml.modloading.brokenresources`, and **skips that mod's pack**, so a mod without a readable `pack.mcmeta` is not loaded as
  a pack. The pack goes straight to the pack acceptor if `!client || mod.showAsResourcePack()`. Otherwise its contents are
  opened and merged into one shared **hidden** pack called `mod_resources`. So on a dedicated server every mod pack is
  accepted, and on a client a mod pack stays hidden unless the mod set `showAsResourcePack`. The per-mod pack and the merged
  pack use different selection settings in the code (first argument false versus true, bottom position in both). What the
  arguments mean was not read. **Where `showAsDataPack` is used was not checked** (the search output was cut off).
- **Loader classification heuristic inside Forge itself (`ModFileInfo`).** A file whose loader is `javafml`, whose loader
  version range has restrictions and whose loader version text is exactly the constant `MAYBE_NOT_JAVAFML_VER` (the
  source comment says the value is `[2,)`) is marked "this might not be a Forge mod". `clientSideOnly = true` makes it
  definitely a Forge mod. This is evidence for decision 9 (classification), which so far only covered Fabric versus Quilt.

### 26.4 Documentation tested against the 26.3 code

| Topic | What the docs say (sections 23 to 25) | What the 26.3 source shows |
|---|---|---|
| Block entity load and update | `CompoundTag` based `load`, `handleUpdateTag`, `onDataPacket` | `IForgeBlockEntity` has `onDataPacket(Connection, ValueInput, HolderLookup.Provider)` and `handleUpdateTag(ValueInput, HolderLookup.Provider)`. Its comment text still mentions `CompoundTag`. The docs are **stale** for 26.3. |
| Block entity renderer | one renderer class, `render(...)` every frame | the `BlockEntityRenderers` patch shows `register(BlockEntityType<? extends T>, BlockEntityRendererProvider<T, S>)` with `S extends BlockEntityRenderState`: a **render state** exists in Forge 26.3. So the redesign seen in NeoForge and Fabric is also in Forge code, and the docs lag. This resolves the two readings left open in 25.4. The same patch turns the provider map into a `ConcurrentHashMap`. |
| `CraftingHelper` | `CraftingHelper#register` adds serializers (sections 20.2, 20.5) | No `CraftingHelper` file was found **by name** in the 26.3 tree, although `JsonCodecProvider` (line 89) still calls `CraftingHelper.serialize(conditions)` and writes the result under the key `forge:conditions`. Whether the class is defined elsewhere is **not determined**. (Corrected in v0.4.31: that call is inside a block comment marked TODO, see section 28.1.) |
| Conditions | conditional wrapper, built-in conditions | `ICondition`, `IConditionBuilder`, `ModLoadedCondition` exist under `net/minecraftforge/common/crafting/conditions`, `ConditionalRecipe` under `common/crafting`. |
| `CriteriaTriggers` | trigger registered in `FMLCommonSetupEvent` | The search (first five hits) found only vanilla call sites patched (for example `ServerPlayerGameMode`, `BucketItem`, `FishingHook`), no Forge class. Not enough to conclude. |
| `strength()` versus `destroyTime` / `explosionResistance` | (open item) | Both exist in 26.3: `strength(...)` is the builder call in block registrations, while `destroyTime` and `explosionResistance` are fields read through the block properties (Forge patches return `properties.destroyTime` and `explosionResistance`). They are not alternative names for one thing. |

### 26.5 EventBus version per branch head (answers "EventBus 7 introduction version")

Read from the build files of each branch head on 2026-09-30:

| Branch | EventBus | Branch | EventBus |
|---|---|---|---|
| 1.19.2 | 6.0.3 | 1.21.8 | **7.0.1** |
| 1.20.1 | 6.2.33 | 1.21.10 | 7.0.1 |
| 1.20.4 | 6.2.33 | 1.21.11 | 7.0.5 |
| 1.21.1 | 6.2.33 | 26.1 | 7.0.1 |
| 1.21.3 | 6.2.33 | 26.2 | 7.0.5 |
| 1.21.4 | 6.2.27 | 26.3 | 7.0.6 |
| 1.21.5 | 6.2.32 | | |

So among the branches probed, **EventBus 7 first appears at the head of branch `1.21.8`**, and branch `1.21.5` is still on 6.
There are no branches for 1.21.6 and 1.21.7, so the exact first version is **not determined** (it is 1.21.6 to 1.21.8 at
the earliest and 1.21.8 at the latest, for branch heads). Branch heads are not always newest-first (1.21.4 is on 6.2.27 while
1.21.3 is on 6.2.33).

### 26.6 `mcmod.info`: `authorList`, not `authors` (answers open item 4 of the status file)

The 1.7.10 class `ModMetadata` has the field `authorList` (a list of strings) and **no** field named `authors`. The other
fields are `modid` (serialized name), `name`, `description`, `url`, `updateUrl`, `logoFile`, `version`, `credits`, `parent`,
`screenshots`, `useDependencyInformation`, `requiredMods`, `dependencies` and `dependants`. `MetadataCollection` reads either a
root object with `modListVersion` and `modList` or a bare array, and on invalid JSON logs "cannot be parsed as valid JSON. It
will be ignored" (the file is skipped). So the single low-authority source that wrote `authors` was wrong or used a
different key, and VESSEL should read `authorList`. Whether the JSON reader tolerates unknown keys such as `authors` was not
read in this pass.

### 26.7 What this means for VESSEL

Evidenced by source:
- For pre-1.8 versions, block state is an int and meaning is per block. Forge itself only knows the orientation bits of about
  22 vanilla block kinds. So the **metadata-to-property layer cannot be built from Forge source alone**: vanilla blocks need the
  vanilla block classes (which define which bits mean what), and mod blocks need analysis of the mod's own code.
- For 26.x, **documentation is behind the code** in at least three places (26.4). So for 26.x, source code is the
  reference and the docs are a hint. This follows Shiroz's "source code first" instruction for the metadata layer and is
  extended here to 26.x by Claude as an inference, **not a decision**.
- Decision 20 (ids registered through `CraftingHelper#register` count as registry keys) rests on a documentation API
  that may not exist under that name in 26.3. It must be re-checked against the 26.3 code before being implemented.
- A mod without a readable `pack.mcmeta` is skipped as a pack by Forge 26.3 with a warning. VESSEL's bundle compiler should
  do the same and log the same kind of warning (proposal, not a decision).

### 26.8 Still missing

The vanilla source of the pre-1.8 block classes (needed for a full metadata layer), the exact bit meanings beyond orientation,
the registry substitution classes of 1.7.10, where `showAsDataPack` is read, where `CraftingHelper` and trigger registration
live in 26.3, branch-by-branch build facts for eras E2 to E4, and the Javadoc inventory.

(Update v0.4.46: the registry substitution classes of 1.7.10 were read in pass 20 (34.4), and trigger registration was located (34.3). `CraftingHelper` was not looked at again.)

---

## 27. Checking the docs against the code: registries, conditions, ingredients, duplicates (pass 13, v0.4.28, 2026-09-30)

**Why this pass exists.** Shiroz asked (2026-09-30) that claims about 26.x be re-verified against the 26.x source code wherever the docs
and the code might disagree, "the same way as in the other places". This pass does that for the points that bear on decisions 8, 17 and
20. All findings come from the `26.3` branch head (commit `932c6e96491f`) unless a branch is named. Evidence class A (source code),
same limits as section 26: branch heads on the run date, no history.

### 27.1 Forge 26.3 has eleven Forge-specific registries

`ForgeRegistries.Keys` lists, besides the vanilla registries Forge wraps (blocks, fluids, items, mob effects, potions, attributes, stat
types, command argument types, sound events, entity types, painting variants, particle types, menus, block entity types, recipe
types, recipe serializers, villager professions, POI types, memory module types, sensor types, activities, chunk status, foliage placer
types, tree decorator types, biomes), these Forge registries:

| Registry key | Holds |
|---|---|
| `forge:entity_data_serializers` | entity data serializers |
| `forge:global_loot_modifier_serializers` | codecs of global loot modifiers |
| `forge:biome_modifier_serializers` | codecs of biome modifiers |
| `forge:structure_modifier_serializers` | codecs of structure modifiers |
| `forge:fluid_type` | fluid types |
| `forge:holder_set_type` | holder set types (and, or, not, any) |
| `forge:display_contexts` | item display contexts |
| `forge:condition_codecs` | data condition codecs |
| `forge:ingredient_serializers` | ingredient serializers |
| `forge:biome_modifier` (dynamic) | biome modifiers |
| `forge:structure_modifier` (dynamic) | structure modifiers |

### 27.2 Conditions and ingredient serializers: registry from 1.20.4, `CraftingHelper` until 1.20.4

- In 26.3 the built-in condition codecs are registered through a `DeferredRegister` into `forge:condition_codecs`: `and`, `false`,
  `item_exists`, `mod_loaded`, `not`, `or`, `true`, `tag_empty` (eight, the same set as the docs page in section 20.2).
- The built-in ingredient serializers registered in `ForgeMod` are `compound`, `nbt`, `difference` and `intersection` (four). **There is no
  separate `partial_nbt` entry.** `NBTIngredient` has a `strict` flag: if strict it uses `strictMatchedBy`, otherwise `matchedBy` on the
  `CUSTOM_DATA` data component. So in 26.3 the partial and strict cases are very likely one type with a flag (the JSON shape of that
  flag was not read). The docs page (section 20.5) lists two separate types.
- The two Forge loot condition types (`loot_table_id`, `can_tool_perform_action`) and the holder set types `and`, `or`, `not` are also
  registered through deferred registers in `ForgeMod`.
- Probe of branch heads (stored in `source-map.json` v0.1.1):

| Branch | Condition and ingredient registries | `CraftingHelper.java` |
|---|---|---|
| 1.19.2, 1.20.1 | not present | exists |
| 1.20.4 | present | exists |
| 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1, 26.2, 26.3 | present | **does not exist** |

  So the mechanism of sections 20.2 and 20.5 (`CraftingHelper#register`, run in `FMLCommonSetupEvent`) is the **1.20.1-and-earlier**
  mechanism. The docs page that calls itself 1.21.x still describes it, so those docs are stale for 1.21.1 and later. (Correction in v0.4.31, see 28.1: the line described next is inside a commented-out block, so it is dead code.) One leftover
  reference remains in 26.3: `JsonCodecProvider` line 89 still calls `CraftingHelper.serialize(conditions)` and writes the result under
  the key `forge:conditions`. No such class file exists in the tree by name, so this line's meaning is unresolved.
- **`CriteriaTriggers`:** no file under `src/main/java` or `forge-tests` of the 26.3 tree mentions it (vanilla call sites are only patched).
  So Forge 26.3 has no Forge-side code for registering advancement triggers. How a mod registers a trigger in 26.x was **not** found
  (vanilla's own registry is the likely route, not read).

### 27.3 Duplicate registry keys: what Forge 26.3 really does

Read in `ForgeRegistry#add` (the same-key case):
- The **same object** registered twice under the same name: a warning is logged and the call returns the existing id.
- A **different object under an existing name**: if the registry's `allowOverrides` is false, the call throws
  `IllegalArgumentException` ("the name ... has been registered twice"). If `allowOverrides` is true, the new object **replaces** the old
  one, the old one is remembered in an `overrides` map with an owner (the active mod's namespace), and only a **debug** line is logged. If
  no owner can be determined, it throws `IllegalStateException`.
- The **same object under two different names**: throws `IllegalArgumentException`.
- Adding after the registry is locked: throws `IllegalStateException` ("being added too late").
- **The default is `allowOverrides = true`** (`RegistryBuilder` field initializer). Only two registries call `disableOverrides()` in the
  tree: the entity data serializer registry and one more created at `GameData` line 163 (its name was not read). Every other Forge
  registry allows overrides.
- Separately, `DeferredRegister` and `DeferredRegisterData` throw `IllegalArgumentException("Duplicate registration ...")` if the **same
  `DeferredRegister` instance** gets the same name twice.

What this confirms: the statement behind decision 8 ("VESSEL's hard stop is stricter than Forge, which overrides silently on a duplicate
key") **holds for 26.3**, with two precisions: (1) Forge overrides only at debug log level, so an admin would not see it; (2) an override
needs an owner namespace, and two registries refuse overrides outright. For the older era (1.7.10) the registry classes
`ExistingSubstitutionException` and `IncompatibleSubstitutionException` exist but were not read.
(Update v0.4.46: read in pass 20, see 34.4. In 1.7.10 Forge itself refuses a duplicate name, so the statement above about silent overrides does not hold for that era.)

### 27.4 Other checks of "unverified for 26.x" rows

- **Lifecycle events present in 26.3:** `FMLConstructModEvent`, `FMLCommonSetupEvent`, `FMLClientSetupEvent`,
  `FMLDedicatedServerSetupEvent`, `InterModEnqueueEvent`, `InterModProcessEvent`, `FMLLoadCompleteEvent`, `ModConfigEvent`, plus the base
  types `ModLifecycleEvent`, `ParallelDispatchEvent`, `IModBusEvent`, `ServerLifecycleEvent`. Names are verified, **threading and order
  are not**.
- **Keys of `mods.toml` read by the 26.3 loader** (`getConfigElement` and `getConfigList` calls in `ModFileInfo`, `ModInfo`, `ModFile`,
  `AbstractModProvider`, `NightConfigWrapper`): `modLoader`, `loaderVersion`, `license`, `showAsResourcePack`, `showAsDataPack`,
  `clientSideOnly`, `services`, `issueTrackerURL`, `properties`, `accessTransformers`, `features`, list keys `mods` and `dependencies`,
  and per mod `modId`, `namespace`, `version`, `displayName`, `description`, `logoFile`, `logoBlur`, `updateJSONURL`, `modUrl`,
  `modproperties`, `referralUrl`; per dependency `modId`, `mandatory`, `versionRange`, `ordering`, `side`. Which of these sit at file level and which per
  mod was not read. Checked against the sections of this file written **before pass 12** (sections 1 to 25, so that the new sections do not count): 25 of the 28 keys appear there as a code span. The ones that do not appear are: `showAsDataPack`, `accessTransformers`, `mods`. Of these, `showAsDataPack` and `logoBlur` are new compared with the docs pages read; the others were simply not written as code spans. (Correction made in v0.4.30: the first version of this sentence counted the new sections 26 and 27 as well and so overstated how much was already known.)
- **Network tasks in 26.3 (names only, not read):** `forge:sync_configs`, `forge:mod_versions`, `forge:send_vanilla_channel_list`,
  `forge:sync_registries`, `forge:channel_list`, a `forge:vanilla_filter` network filter, and a `forge:split` packet splitter. They are
  leads for the networking row, not findings.

### 27.5 What this means for VESSEL

- **Decision 8 and 17 stand and are now verified** against 26.3: duplicates are handled by Forge as overrides in almost every
  registry, so VESSEL's hard stop is a deliberate difference, and any "same as Forge" assumption in the registry scan would be wrong.
- **Decision 20 needs a version scope.** For Forge 1.20.4 and later (branch heads), condition and ingredient serializer ids are **ordinary
  registry keys** (`forge:condition_codecs`, `forge:ingredient_serializers`), so decisions 8 and 17 already cover them and the special
  `CraftingHelper` path does not exist. The special case still matters for 1.20.1 and earlier (decision 13 keeps those in scope). The
  trigger-id part of decision 20 is unresolved for 26.x because no Forge-side registration code was found.
- **The registry scan must know all eleven Forge registries** of 27.1 (and the vanilla ones), including the two dynamic ones, and the list
  is version dependent (for example `CONDITION_SERIALIZERS` does not exist in 1.20.1).
- **For 1.21.1 and later the documentation of Forge (as read in sections 20 to 25) cannot be trusted for API names**; the source is the
  reference. This is the working practice Shiroz asked for, applied here. It is not a rule that code always overrides docs.

### 27.6 Still missing

The name of the second registry that disables overrides, the JSON shape of the NBT ingredient's `strict` flag, what `forge:conditions` is
for (answered in 28.1 and 28.2), how a 26.x mod registers a trigger, the file-level versus per-mod split of the `mods.toml` keys, and the same registry check for eras
before 1.19.2.

(Update v0.4.46: how a mod registers a trigger and the registry check for eras before 1.19.2 were answered in pass 20, see 34.3 and 34.4.)

---

## 28. Conditional data, tags and registry flags as the 26.3 code really does them (pass 14, v0.4.31, 2026-09-30)

Same method and limits as sections 26 and 27: source code of the `26.3` branch head (commit `932c6e96491f`) plus raw-file probes of other branch heads
(stored in `source-map.json` v0.1.2). This pass closes three open points of section 27.6 and **corrects two statements of section 27**.

### 28.1 Correction: the `CraftingHelper` line in `JsonCodecProvider` is dead code

Sections 26.4 and 27.2 said that `JsonCodecProvider` line 89 "still calls" `CraftingHelper.serialize(conditions)`. Reading the lines around it shows that the call sits
inside a **block comment marked `TODO: Conditional JSONS`**. It is not executed. So there is no live reference to `CraftingHelper` anywhere in the 26.3 tree, which
fits the finding of 27.2 that the class was removed. (My earlier statement came from a search that printed the line without its comment context.)

### 28.2 Conditional data in 26.3: a different format from the docs

| Aspect | Forge docs as read (section 20.2) | Forge 26.3 code |
|---|---|---|
| Where conditions apply | recipes and advancements only | **every** JSON listener built on `SimpleJsonResourceReloadListener` (a patch calls `ForgeHooks.readConditional` on each file before decoding). `ServerAdvancementManager` and `ReloadableServerResources` are patched too. |
| One condition | a list named `conditions` | one field named **`forge:condition`** (`ICondition.DEFAULT_FIELD`). Several conditions are combined with the `and`, `or` and `not` codecs. |
| Several alternatives | wrapper of `type = forge:conditional` with a list key such as `recipes` | a root key **`forge:conditional`** whose value is an array of JSON objects. Each object is a full data file with its own `forge:condition`. The **first** object whose condition passes replaces the file content. A non-object entry is an error. |
| Condition not met | not stated | the file is skipped, with a **debug** log line ("Skipping loading ... as its conditions were not met") |
| Loot pools | not mentioned | `LootPool` has an optional field `forge:condition` of its own, so single pools can be conditional (name and condition are extra fields on the pool codec) |
| Parsed-data hook | not mentioned | after decoding, `ForgeHooks.onJsonDataParsed` runs. For loot tables it sets the table id and fires the loot table load event (section 21.2). |
| How the condition is decoded | `IConditionSerializer` via `CraftingHelper` | `ICondition.CODEC` dispatches through the `forge:condition_codecs` registry (section 27.2) |

Probe of branch heads (two files only): the `forge:condition` field and `ConditionCodec` exist from the **1.20.4** branch on. A mention of `forge:conditional` in
`ForgeHooks.java` exists from **1.21.3** on, and is absent from 1.19.2, 1.20.1, 1.20.4 and 1.21.1. So there are **three conditional data formats by era**:
(1) up to Forge 1.20.1 the wrapper format of the docs, (2) 1.20.4 and 1.21.1 with `forge:condition` but no `forge:conditional` array in `ForgeHooks` (other files were
not probed, so the exact format of these two is **not determined**), (3) 1.21.3 and later with both keys as in the table.

(Update v0.4.48: determined in pass 21, see 35.1: the formats of 1.20.4 and 1.21.1 differ from each other in the shape of the recipe entries, so there are **four** formats by era, not three.)

### 28.3 Tags: the `remove` extension as coded (matches the docs, with two details)

- `TagFile` is patched to `TagFile(entries, replace, remove)`: a third, optional list `remove` (same entry format as `values`).
- `TagLoader` is patched so that removal entries are carried with a flag and applied to an insertion-ordered set (`LinkedHashSet`) with `remove` instead of `add`. The order
  of a tag is therefore insertion order, and the code comment says some people rely on that.
- **Removals are always optional at runtime:** if a removal entry cannot be resolved, it is not reported as an error (the comment gives the reason that the entry could never have
  been added either). The docs said a missing ordinary entry is an error unless marked `required: false`; for removals that is stricter than the code.

### 28.4 Registry flags of Forge 26.3 (answers the open point of 27.6)

`GameData` builds each Forge registry with flags. Read values:
- **Overrides disabled:** only `forge:entity_data_serializers` and `forge:display_contexts` (the "second registry" left open in 27.3). All other Forge registries allow overrides.
- **Not saved to disk and not synced to clients:** `forge:global_loot_modifier_serializers`. The biome registry is not synced. Fluid types are not saved. A helper for "unsaved and
  unsynced" registries exists (its users were not listed).
- All registries created through the common helper have a wrapper around the vanilla-style registry and a maximum id of the maximum varint.
- What "synced" and "saved" mean exactly (handshake content, the world save's registry map) was not read, and `forge:sync_registries` is only a name seen (section 27.4).

### 28.5 Advancement triggers: no Forge registry exists

The search for `TRIGGER_TYPE`, `TriggerType` and `CriterionTrigger` in `src/main/java` and in the patches found nothing, and the list of Forge registries (27.1) has no trigger registry. So in
26.3 Forge neither wraps nor extends trigger registration. A mod's trigger id goes into whatever vanilla provides (which was **not read**). Whether vanilla rejects a duplicate trigger id is
**not verified** (not read in this pass).
(Update v0.4.46: the search of this section missed the `forge-tests` module. A trigger is registered through a `DeferredRegister` on the vanilla registry `BuiltInRegistries.TRIGGER_TYPES` from the 1.20.4 branch on, see 34.3. Forge still adds no rule of its own for that registry.)

### 28.6 What this means for VESSEL

Evidenced:
- **The conditional data format is version dependent, and the docs describe only the oldest of three.** The Datapack Compiler (phase 15) and the converter for mod data must pick the reader and writer by
  Forge version, and for 26.x must implement the `forge:condition` and `forge:conditional` forms, applied to **all** JSON reload listeners and to loot pools, not only recipes and advancements.
- **Decision 20, trigger part:** Forge gives VESSEL nothing to hook for trigger ids in 26.3. VESSEL must apply its duplicate rule to trigger ids **itself**, from its own view of what the mod
  registers. This is consistent with decision 17 (scan before loading), but it means the scan cannot simply defer to Forge's registry classes for triggers.
  (Update v0.4.46: partly superseded by 34.3 and 34.7: from the 1.20.4 head on, trigger ids are entries of a vanilla registry that goes through `RegisterEvent`, so the registry scan sees them; the special rule is needed only up to 1.20.1.)
- **Decision 19 still holds** (registries complete at step 10): conditions `item_exists` and `tag_empty` read registry and tag state through the condition context, which matches the plan to merge tags
  in VESSEL (section 20.4).
- The fact that removals are optional at runtime and tags keep insertion order are small behaviors a faithful tag merger must copy.

Inference (marked): the GLM serializer registry not being synced supports, but does not prove, the section 21.4 guess that loot modifiers never reach a player's client.

### 28.7 Still missing

The exact data formats of branches 1.20.4 and 1.21.1 (read in pass 21, see 35.1), the vanilla registry used for triggers, the meaning of "saved" and "synced" in the Forge registry code, which registries use the unsaved-and-unsynced helper, how
mod data packs are ordered against each other, and everything in this section for eras before 1.19.2.

(Update v0.4.46: the vanilla registry used for triggers is `BuiltInRegistries.TRIGGER_TYPES` (34.3); the registries that use the unsaved-and-unsynced helper are listed in `ForgeRegistries` (biome modifier serializers, structure modifier serializers, holder set types, condition serializers, ingredient serializers; read in pass 20).)

---

## 29. Re-verification register: what earlier sections say against the 26.3 code (pass 15, v0.4.33, 2026-09-30)

**Why this pass exists.** Shiroz said (2026-09-30) to go through the parts already researched first and only then move to the pre-1.8 vanilla code. Decision 24 (verify 26.x claims against
the code) is applied here to **every topic of the earlier documentation-based sections that could be checked with searches in the `26.3` tree** (commit `932c6e96491f`). This is a
register, not a deep read: for most rows only the existence, names and a few values were read, and the "verdict" says what was checked. Branch-head limits of section 26 apply.

| # | Topic | Earlier section | What the 26.3 code shows | Verdict |
|---|---|---|---|---|
| 1 | Event buses | 7 | `BusGroup` and `FMLJavaModLoadingContext#getModBusGroup()`; `@Mod.EventBusSubscriber` has `bus` (FORGE, MOD, BOTH, default BOTH), `value` (default CLIENT and DEDICATED_SERVER) and `modid`; EventBus 7.0.6. No `getModEventBus` in that class. | CHANGED |
| 2 | Sides: Dist, @OnlyIn, DistExecutor | 6 | `Dist` and `OnlyIn` come from the external package `net.minecraftforge.api.distmarker` (not in this tree). No `DistExecutor` class exists (the only hits are names of test files). `RuntimeDistCleaner` (launch plugin `runtimedistcleaner`): in production it acts only on Minecraft classes on a dedicated server; a **mod class annotated `@OnlyIn` throws `UnsupportedOperationException`** ("no longer supported as it slowed down startup times"); loading a class for the wrong dist throws `RuntimeException`. | CHANGED |
| 3 | Lifecycle events | 5 | Event class names present: `FMLConstructModEvent`, `FMLCommonSetupEvent`, `FMLClientSetupEvent`, `FMLDedicatedServerSetupEvent`, `InterModEnqueueEvent`, `InterModProcessEvent`, `FMLLoadCompleteEvent`. Threading and order not read. | CONFIRMED (names only) |
| 4 | Configs and `defaultconfigs` | 10 | `ForgeConfigSpec` and `ForgeConfig` exist. `FMLConfig` option `defaultConfigPath`, default `defaultconfigs`, resolved against the game directory. `setupConfigFile` creates the parent folders, then **copies** `defaultconfigs/<file name>` over as the new config file if it exists (plain `Files.copy`, no merge), else creates an empty file and initializes it. SERVER type configs live in `<world>/serverconfig`. `backUpConfig` keeps up to 5 rotated `.bak` copies. The call site of `setupConfigFile` (when it runs) was not read. | CONFIRMED and extended |
| 5 | Capabilities and saved data | 9 | `Capability`, `CapabilityManager`, `CapabilityToken`, `CapabilityProvider`, `CapabilityDispatcher`, `AttachCapabilitiesEvent` exist. Forge keeps level capabilities in `LevelCapabilityData` (a `SavedData` subclass). API shape not read. | CONFIRMED (presence only) |
| 6 | Networking | 8 | `SimpleChannel` (573 lines) still exists. Also `Channel`, `ChannelBuilder`, `PayloadChannel`, `EventNetworkChannel`, `PacketDistributor`, `NetworkRegistry`, and packages `payload`, `packets`, `tasks`, `filters`, `config`, `simple`. The newer channel and payload classes are not in the docs read. | PARTLY CONFIRMED (new parts unread) |
| 7 | Update checker statuses | 18.5 | `VersionChecker.Status` has seven values: PENDING, FAILED, UP_TO_DATE, OUTDATED, AHEAD, BETA, BETA_OUTDATED (same seven as the docs). `VersionChecker` uses `ComparableVersion`. | CONFIRMED |
| 8 | Access transformers | 12.6 | Each mod file is searched for `META-INF/accesstransformer.cfg`; the `mods.toml` list key `accessTransformers` also exists; `AccessTransformerService` is loaded as launch plugin `accesstransformer` (version "1.0"). | CONFIRMED |
| 9 | ModLauncher and transformers | 12 | `FMLLoader` checks the `ITransformationService` package at version "4.0" and loads the launch plugins `accesstransformer` (1.0) and `runtimedistcleaner` (1.0); NightConfig is required. The `forge-transformers` module holds `FieldToMethodTransformer`, `ForgeModTransformers`, `MethodRedirector`; `fmlloader` also has `RuntimeEnumExtender` and `CapabilityTokenSubclass`. | CONFIRMED (ModLauncher still in use in 26.3) |
| 10 | Mixin in `mods.toml` | 12 / 15 | No `mixins` key is read by `fmlloader` or `fmlcore`. The loader only handles jar manifest attributes `MixinConfigs` and `MixinConnector` (in `JarInJarDependencyLocator`, which adds a MixinExtras dependency) and a `-mixin` launch argument that sets a dev flag. The Mixin library itself is not in this tree. | ABSENT from the loader |
| 11 | Global loot modifiers path | 20.3 | `LootModifierManager` reads `forge:loot_modifiers/global_loot_modifiers.json` (folder constant `loot_modifiers`, namespace `forge`). | CONFIRMED |
| 12 | Loot conditions and event | 21.2 | `LootTableIdCondition` and `CanToolPerformAction` exist and are registered as loot condition types; `LootTableLoadEvent` exists. | CONFIRMED (names only) |
| 13 | Conditional data | 20.2 | Different format and scope (section 28.2). | CHANGED |
| 14 | Ingredients | 20.5 | Four serializers, no separate `partial_nbt` (section 27.2). | CHANGED |
| 15 | Tags `remove` | 20.4 | `TagFile` has `remove`, removals optional at runtime, insertion-ordered (section 28.3). | CONFIRMED (plus details) |
| 16 | Model loaders | 23.2 | `ClientForgeMod` registers `forge:empty`, `forge:obj`, `forge:fluid_container`, a reload listener for the OBJ loader, and a named render type `item_unlit`. The docs also list composite, render-layer and generated-item loaders; none of those was found in that class. | CHANGED (list differs) |
| 17 | Block entity data and renderer | 23.1 / 24.1 | Value-input based methods and a render-state renderer (section 26.4). | CHANGED |
| 18 | Registry duplicates | 6 | Overrides by default, debug log only (section 27.3). | CONFIRMED (precisions added) |
| 19 | Forge registries | 4 | Eleven Forge registries, `DeferredRegister` and `RegistryObject` still present (section 27.1). | CONFIRMED (list extended) |
| 20 | `mods.toml` key list | 3 | 28 keys extracted from the loader (section 27.4). | CONFIRMED (3 new keys) |
| 21 | Advancement triggers | 22.1 | No Forge-side code or registry (section 28.5). | CHANGED / unresolved |
| 22 | Creative tab event | 22.2 | `BuildCreativeModeTabContentsEvent` exists in the event package. | CONFIRMED (name only) |
| 23 | Recipes: shape size, result NBT | 25.3 | Not checked. | NOT CHECKED |
| 24 | Texture tinting | 25.2 | Not checked. | NOT CHECKED |
| 25 | Item properties, item overrides, item renderer | 24.2 to 24.4 | Not checked (docs read were the 1.19.x line, and the client item API was redesigned in newer versions). | NOT CHECKED |
| 26 | `@Mod` constructor rules, event threading | 5 | Only the annotation members were read. | NOT CHECKED |

**Count:** 26 topics: 13 confirmed, 1 partly confirmed, 7 changed in 26.3, 1 absent from the loader, 4 not checked. "Confirmed" means the
name or value matched, not that the whole behavior was read.

(Update v0.4.40: rows 23 to 26 were checked afterwards in pass 18, see section 32. Result: row 23 confirmed (existence only), rows 24 and 25 changed, row 26 partly verified.)

(Update v0.4.42: rows 24 and 25 were taken further in pass 19, see section 33: the replacement behind row 25 is identified (client item definitions) and the Forge model extension pages were checked against the 26.3 code.)

### 29.1 Answers to earlier open items

- **`defaultconfigs` copy behavior** (open in section 15): answered, row 4 (with the call-site caveat).
- **`mods.toml` `[[mixins]]`** (not started in section 15): the loader does not read such a key in 26.3 (row 10). JS coremods and `fml.toml` were not checked. (`FMLConfig` has a bundled `defaultfmlconfig.toml`, which suggests the `fml.toml` file, but it was not read.)
- **Dist handling:** the claim in section 6 that `DistExecutor` and `@OnlyIn` are the tools for side-specific code is true only for older versions. In 26.3 they are not usable in mod code (row 2).

### 29.2 What this means for VESSEL

Evidenced:
- **VESSEL's adapter must present the API of the era of the mod, not of the newest loader.** Examples seen here: `getModEventBus` (EventBus 6) versus `getModBusGroup` (EventBus 7); `DistExecutor` and `@OnlyIn` in old mods versus
  a hard error for `@OnlyIn` in 26.3 mod classes; `CraftingHelper` versus registries (section 27); block entity methods with `CompoundTag` versus value objects.
- **Client-only code inside server-loaded mods.** In 26.3 production, dist stripping is done only for Minecraft classes, and only on a dedicated server. For mods from older versions, VESSEL has to do the
  dist cleaning itself when it loads their classes on a server, otherwise a class that refers to client-only classes can fail when loaded. This is an inference from the evidence, **not** a finding about how VESSEL works.
- **Default configs.** Forge copies `defaultconfigs/<file>` into the config folder when the config file is first created, relative to the game directory, and keeps server configs per world in `serverconfig`. With decision 6 (data split per loader
  and `vessel-config/`), VESSEL has to decide where this folder lives. **Open question for Shiroz** (status section 5). **[Answered 2026-10-01 (v0.4.45): decision 26 of the status file, `vessel-config/defaults/forge/`.]**
- **Mixin conversion (phase 20):** for Forge mods, Mixin configuration is found through jar manifest attributes, not through `mods.toml`, so the converter must read the manifest.
- **The model loader list is era dependent** (row 16), so the model conversion rule of decision 22 needs a per-era list of loaders.

### 29.3 Still missing

The "not checked" rows 23 to 26 (the first version of this line said 24 to 27, which was an off-by-one error corrected in v0.4.35), everything for eras before 1.19.2 except the two probes already stored in `source-map.json`, a deep read of the networking classes, the call site of `setupConfigFile`, JS coremods, and whether the composite and render-layer model loaders were moved or removed.

(Update v0.4.48: the Javadoc inventory was made in pass 21 from mirror list pages and the source revision per era was tabulated as far as clones and tags allow, see 35.4 and 35.5; the era-to-branch fit of E1a and of the early E1b is still open.)

---

## 30. Pre-1.8 vanilla block metadata: what could and could not be established (pass 16, v0.4.36, 2026-09-30)

**Why this pass exists.** Shiroz agreed (2026-09-30, decision 23) that the pre-1.8 metadata layer decodes the metadata of **vanilla** blocks, keeps mod-block metadata opaque, and asked for the vanilla part after
the re-verification of pass 15. Forge's own source gave the orientation bits of about 22 block kinds (section 26.2). This pass looked for the rest.

### 30.1 What was available

| Source | Class | What it offers | Limit found |
|---|---|---|---|
| Minecraft Wiki, pre-flattening data values (fetched, text cut off after the torches section) | B | a summary table of **60 block and item kinds** with the range of metadata values and what the bits mean, plus full value tables for planks, stone, dirt, saplings, liquids, sand, logs, leaves and the 16 colors | it describes **1.12.2**, not 1.7.10; its own header says the section lacks the names of the block states for each value |
| `minecraft-data` 1.7 and 1.8 `blocks.json` | B | `variations`: display names by metadata for 16 blocks (1.7) and 38 blocks (1.8) | the directory named 1.7 contains **later content** (stone variants, coarse dirt, prismarine id 168), so it is not a true 1.7.10 table; no property structure |
| Forge 1.7.10 source | A | orientation bits of about 22 vanilla block kinds (26.2) | only orientation, only vanilla |
| Forge Javadoc mirrors (for example `skmedix.github.io/ForgeJavaDocs`, 1.7.10-10.13.4.1614) | C | method signatures, for example `onBlockPlaced` with an `int` metadata parameter | signatures only, no method bodies |
| Decompiled vanilla 1.7.10 source | n/a | would give exact bit meanings per block | **not read.** Decompiling needs Mojang's game files, which this environment's allowed network does not provide. A third-party cache of decompiled sources was seen in search results; it was **not** used (see 30.4). |

### 30.2 What the wiki summary table shows (stored in `metadata-inventory.json` v0.1.0)

The 60 rows fall into these kinds of meaning (classification by Claude, not by the wiki): variant 16, age/level 13, orientation+flags 13, orientation 6, variant+flags 4, flag 3, variant+orientation 2, flags 2, damage 1. In words:
- **Variant only** (for example planks, stone, dirt, sand, wool colors, flowers, stone brick): the whole value picks a type.
- **Level or age** (fire, crops, cactus, sugar cane, liquids, snow, cake, redstone wire, cauldron): the value is a counter.
- **Orientation** (torches, stairs, sign posts, chests, ladders, furnaces, pumpkins, nether portal) and **orientation plus flags** (rails, levers, doors, buttons, beds, repeaters, comparators, trapdoors, pistons, fence gates, end portal frames, dispensers and hoppers): bits are split between a direction and a state such as open, powered or extended.
- **Variant plus flags** (leaves, saplings, slabs, double plants): low bits pick the type and a high bit is a flag (leaves 0x4 permanent and 0x8 check-decay, saplings 0x8 ready to grow, slabs 0x8 upper half, double plants 8 and up upper half).
- The wiki states that at most **4 bits** of data exist per block and that more state needs a block entity or a second block. This matches the `0xF` masks seen in Forge's `RotationHelper`.

Value tables read in full (1.12.2 state) are in the JSON file: planks 0 to 5, stone 0 to 6, dirt 0 to 2, saplings, liquids (low 3 bits level, 0x8 falling), sand, log and log2 (type bits 0x3, axis bits 0xC), leaves and leaves2, and the 16 colors shared by wool, stained clay, stained glass and carpet.

### 30.3 Conflicts found between the sources (the reason a single source cannot be used)

1. **Anvil:** the wiki summary row says 0 to 2 and only damage. Forge uses mask `0x1` for the anvil's orientation, and minecraft-data 1.8 lists orientation values 3 to 14 with damage. The wiki summary row omits the orientation bits.
2. **Torch:** the wiki starts at 1 (0 unused), Forge uses `6 - meta`, minecraft-data 1.8 labels 0 to 4, one lower than the other two. This agrees with the wiki and Forge only if the order of `ForgeDirection` is down, up, north, south, west, east, which was not read.
3. **Dataset naming:** `minecraft-data` "1.7" includes content from later versions.
4. **Version drift:** many values of the 1.12.2 tables did not exist in 1.7.10 (stone variants and coarse dirt are 1.8, red sandstone and prismarine are 1.8, acacia and dark oak planks and leaves exist in 1.7). Which values exist in 1.7.10 is **not established** by any source read here. Forge versions from 1.1 to 1.6.4 are even further away from the 1.12.2 table.

### 30.4 Decision needed, and why decompiled vanilla code was not used

What is established: an **inventory of kinds** and a handful of exact tables. What is not: a version-accurate decode table for 1.1 to 1.7.10. Closing that gap needs the vanilla block classes of each version. Options:
- **A.** Shiroz runs the decompile locally (MCP or ForgeGradle with his own game files) and uploads the vanilla block classes, or a folder of them, for analysis. Cleanest for rights and for version accuracy.
- **B.** VESSEL ships a **data-driven table** for vanilla blocks built from the sources of this section plus the Forge source, marked per block as `exact`, `from 1.12.2` or `unknown`, and treats every block marked `unknown` as opaque (the same rule as for mod blocks). Needs no vanilla source, but is partial.
- **C.** A third-party cache of decompiled sources (one was seen in search results). Not used here because it would mean working from a redistributed copy of Mojang's code, and a release asset of unknown size and origin; Shiroz can decide that for his own project.

Recommendation (not a decision): **B now, A later for blocks that matter**, since decision 23 already accepts that mod blocks stay opaque, so an `unknown` value is an accepted outcome.

**Decided by Shiroz on 2026-09-30: option B, combined with option C as reference only.** A value taken from a third-party cache of decompiled sources is used only if an independent source proves it; otherwise it is not used. Section 31 applies this decision.

### 30.5 What this means for VESSEL

- The metadata layer needs a **per-block, per-version table with a confidence flag**, not a single global table.
- Because 4 bits is the hard limit, **a decoded property set for a vanilla block is at most 16 states**, and conversion to a modern property set (several booleans and enums) is an expansion of that.
- `RotationHelper` of Forge 1.7.10 is the only source here that is itself code and can be reused as ground truth for orientation; its table (26.2) should be cross-checked against the wiki rows for the same block kinds. Where they disagree (anvil, torch) the Forge source wins for orientation, per decision 24's spirit.

### 30.6 Still missing

The wiki page after the torches section (the detail of rails, stairs, levers, doors and the rest), the set of values that exist per block in 1.1 to 1.7.10, and any decompiled vanilla source.

---

## 31. The vanilla metadata table (option B) and the Bukkit API as independent reference (pass 17, v0.4.38, 2026-09-30)

**What was decided.** Shiroz chose option B (a data-driven table shipped with VESSEL, with a confidence flag per field and `unknown` treated as opaque) and allowed option C **only as reference, with a value used only if proven by another source** (decision 25).

### 31.1 The third-party cache was not downloaded, and why this is not a loss

The cache seen in section 30 (a release of a third-party repository) lists, in its own description, **"Minecraft client/server/merged JARs"** next to the decompiled sources. Downloading it would copy Mojang's unmodified game files from a
redistribution of unknown permission. Since option C is allowed only as reference and anything from it must be proven by another source anyway, the same job can be done by sources that are open and independent of Mojang's files: the **Forge 1.7.10 source** (read in pass 12), the **Bukkit API** and the **Minecraft Wiki**. This is a judgment about what to download, not a refusal of Shiroz's decision: if Shiroz wants a value checked against that cache, he can run
or download it himself and tell me which value to compare. Under the rule "used only if proven", such a check could only add confidence, never new values.

### 31.2 The independent reference that was used: the Bukkit API `org.bukkit.material` package

- Repository `Bukkit/Bukkit`, branch `master`, head `f210234e59275330f83b994e199c76f6abd41ee7`, **commit dated 2013-07-07**, `pom.xml` version string `1.7.10-R0.1-SNAPSHOT`, licence **GPL v3** (so only facts were taken, no code copied).
  The date and the version string do not agree (a 2013 commit cannot be for 1.7.10), so its content may belong to an **earlier era than 1.7.10**. **[Update v0.4.48: examined in pass 21: the content is not older than the 1.7.2 era, see 35.3.]** 60 files, among them one class per block kind that has structured metadata.
- Each class says in code which bits of the data byte mean what. Classes read in this pass: `Stairs`, `Torch`, `Lever`, `Door`, `Bed`, `Button`, `Ladder`, `TrapDoor`, `Gate`, `Diode`, `Pumpkin`, `Sign`, `DirectionalContainer`, `PistonBaseMaterial`, `TripwireHook`, `Rails`, `Skull`.
- A modern Spigot API repository (`SpigotMC/Spigot-API`) also exists and is reachable but was not used: it is for current versions.

### 31.3 What the comparison of three sources produced

Compared fields: Forge 1.7.10 `RotationHelper` (section 26.2), the Bukkit classes above and the wiki summary. Result (stored in `vanilla-metadata-table.json` v0.1.0):

| Level | Fields | Meaning |
|---|---|---|
| per-field confidence counts | corroborated 29, single_source 9, convention_unresolved 2, conflict 2 | see `confidence_levels` in the file |

Agreements worth keeping as facts:
- **Beds, fence gates, tripwire hooks, repeaters and doors** use the same low-bit values in Forge and Bukkit (for example bed and fence gate: 0 south, 1 west, 2 north, 3 east; repeater: 0 north, 1 east, 2 south, 3 west; door lower half: 0 west, 1 north, 2 east, 3 south).
- **Stairs:** low two bits are the direction (0 east, 1 west, 2 south, 3 north) and bit `0x4` is upside down; Forge's `5 - meta` formula gives the same directions.
- **Flags:** the 0x8 bit is the powered state on levers, buttons and piston bases, the head half on beds and the upper half on doors; the 0x4 bit is open on doors, trapdoors and fence gates.
- **Repeater delay** is `(data >> 2) + 1`, so 1 to 4.
- **Chests, furnaces, dispensers and pistons** use the numbers 2 north, 3 south, 4 west, 5 east in both Forge and Bukkit. This also supports the assumption (not read) that `ForgeDirection` is ordered down, up, north, south, west, east.
- **Rails:** ascending shapes 2 east, 3 west, 4 north, 5 south agree between Forge and Bukkit.

Disagreements that must not be hidden:
- **Pumpkin:** Forge 0 south, 1 west, 2 north, 3 east; Bukkit 0 north, 1 east, 2 south, 3 west. Same structure, 180 degrees apart in naming. `convention_unresolved`.
- **Trapdoor facing:** same situation. `convention_unresolved`. And the wiki summary row says 0 to 7 while Bukkit has a bit `0x8` (upper half), so the **wiki row omits a bit**.
- **Skull:** east and west differ between Forge's direct numbers and Bukkit (and Bukkit uses 4 for west on chests but 4 for east on skulls). `conflict`: opaque.
- **Anvil:** three different descriptions (Forge mask 0x1, minecraft-data values 0 to 14, wiki 0 to 2 damage only). `conflict`: opaque.
- Torch values: three sources agree on 1 to 5 with 0 unused; minecraft-data is one lower (already in 30.3).

### 31.4 The rule for the table in VESSEL (design requirement, from decision 25)

1. Each field of each vanilla block kind has a **confidence level**: `corroborated`, `single_source`, `convention_unresolved`, `conflict`, or `unknown`.
2. VESSEL converts a field only at `corroborated`. At `single_source` it converts and prints a **yellow warning** that names the block and the source. At the other three levels the metadata value stays an **opaque integer**, exactly like a mod block (decision 23).
3. A value from a third-party decompiled cache enters the table only when an independent source proves it, otherwise it is recorded as unverified and not used.
4. The table is **data**, not code, so a later pass (or Shiroz's own decompile, option A) can raise a field's level without changing VESSEL.

### 31.5 What this means for the status of the metadata layer

Roughly 29 fields across 24 block kinds are `corroborated`. The wide set of age, level and variant kinds remains summary-only (`unknown` for conversion). The table is a **design seed**: it shows the schema and the first evidence-backed rows. It does not yet decode a whole world.

### 31.6 Still missing

The wiki page after the torches section, rows for the remaining orientation kinds (cocoa, end portal frame, vine, comparator mode and power, powered rails, mushroom blocks), a check of the table against the pre-1.4 and pre-1.6 eras (the Bukkit snapshot may be older than 1.7.10, and Forge before 1.7 was not read), and Shiroz's own decompile if he chooses option A for the blocks that matter.

(Update v0.4.48: pass 21 added rows for cocoa, end portal frame, vine, powered and detector rails and huge mushrooms, looked for the comparator bits (not found), settled the Bukkit snapshot era in part and read the Forge side for 1.6.4 and 1.7.2, see 35.3. The wiki rows and the pre-1.6.4 check are still missing.)

---

## 32. The four unchecked rows of the register: recipes, tinting, client item API, event threading (pass 18, v0.4.40, 2026-10-01)

**Date note.** The session crossed midnight; from this pass on entries are dated 2026-10-01. Same method and limits as sections 26 to 29: the `26.3` branch head (commit `932c6e96491f`), names and a few
lines read, no deep behavior reading. This pass checks the four rows that section 29 left as "not checked" (rows 23 to 26).

| # | Topic | Earlier section | What the 26.3 code shows | Verdict |
|---|---|---|---|---|
| 23 | Recipes: crafting grid size | 25.3 | `ShapedRecipe#setCraftingSize(int width, int height)` is still added by a patch (`ShapedRecipe.java.patch`); patches for `ShapedRecipePattern`, `Ingredient`, `ShapelessRecipe`, `CraftingRecipe`, `RecipeType`, `BannerDuplicateRecipe` and `BookCloningRecipe` exist. The thread-safety statement and the `nbt` result field were not read. | CONFIRMED (existence only) |
| 24 | Texture tinting | 25.2 | `RegisterColorHandlersEvent` is now a sealed interface with two members: `Block` (a record whose `register(List<BlockTintSource> sources, Block... blocks)` takes **tint sources**, not a `BlockColor` function) and `ColorResolvers` (registers `ColorResolver` objects). **No item sub-event exists in this file.** | CHANGED |
| 25 | Item properties, item overrides, item renderer | 24.2 to 24.4 | The classes `ItemProperties`, `ItemOverrides` and `BlockEntityWithoutLevelRenderer` are **not in the Forge tree** (absence from Forge's tree does not prove removal from vanilla, because Forge patches only some vanilla classes). `IClientItemExtensions` exists but, in the lines read (up to line 138), has no `getCustomRenderer`; it offers font, arm pose, hand transform, humanoid and generic armor model, and helmet overlay methods. The model patches are now named `UnbakedGeometry`, `QuadCollection`, `cuboid/CuboidModel`, `cuboid/ItemModelGenerator`, `cuboid/ItemTransform(s)`, `ResolvedModel`, `ModelDiscovery`. These patch file names suggest a refactored model system, but nothing of it was read. | CHANGED (docs stale; replacement not identified) |
| 26 | `@Mod` constructor rules and event threading | 5, 7 | `FMLModContainer` looks for a constructor taking **exactly** the loading context class (`FMLJavaModLoadingContext`) and otherwise the no-argument constructor; with zero parameters it calls the no-argument one, otherwise it passes the context. No other injected parameter appears. Each mod gets its own `BusGroup` (`"modBusFor" + modid`, for `IModBusEvent`). `ModLoadingStage` has nine values: `ERROR`, `VALIDATE`, `CONSTRUCT`, `COMMON_SETUP`, `SIDED_SETUP`, `ENQUEUE_IMC`, `PROCESS_IMC`, `COMPLETE`, `DONE`. `DeferredWorkQueue#enqueueWork(ModContainer, Runnable or Supplier)` returns a `CompletableFuture`. `ParallelDispatchEvent` lives in `src/main/java/net/minecraftforge/fml/event/lifecycle`. The execution thread and the order of listeners were **not** read. | PARTLY (constructor rule and stages verified, threading not read) |

### 32.1 Other things seen in passing

- Events in 26.3 are `record` or `MutableEvent` types with a static field `BUS = EventBus.create(...)` per event class (for example `RegisterColorHandlersEvent.Block.BUS`). Together with the per-mod `BusGroup`
  this is the EventBus 7 model (sections 26.5 and 29, row 1).
- `CustomPayloadEvent` has its own `enqueueWork` that returns a `CompletableFuture`, which suggests a hand-off from the network thread (name only, not read).
  (Update v0.4.46: read in pass 20, see 34.1: it submits the task to the executor of the logical side, or runs it at once when already on that thread.)

### 32.2 What this means for VESSEL

Evidenced:
- **Constructor injection differs by era.** In 26.3 a mod constructor can take only the loading context or nothing, so VESSEL's adapter for a 26.x mod must not offer an event bus or container as a parameter. For older eras the allowed
  parameters are not established here and must come from the era's source.
- **The client item and tint mechanisms of the docs-based sections (23 to 25) are stale for 26.3.** Decision 22's "complex" list names `BlockColor` and `ItemColor`, block entity renderers, item renderers, item override resolution and item-property models.
  Those names are the 1.21.x-docs era. For 26.x the matching mechanisms are `BlockTintSource` (block tint), a render-state block entity renderer (section 26.4), and an unidentified replacement for item renderers and overrides.
  The **principle** of decision 22 (code at draw time or per-item or per-position state is complex) is not affected, but the list of API names has to be kept **per era**.
- **Event emulation per era.** To give a mod the API it was built for, the adapter must offer the `getModEventBus`-style registration for old mods and the per-event `BUS` model for 26.x mods.

Inference (marked): since `ItemProperties`, `ItemOverrides` and `BlockEntityWithoutLevelRenderer` are missing from Forge's tree and `IClientItemExtensions` lost its custom renderer hook, the old item-model switching probably moved
into data or into vanilla classes. This is a guess from names, not a finding.

### 32.3 Still missing

The replacement of item properties and overrides in 26.x (vanilla classes were not read), the item tint mechanism in 26.x, threading semantics of `enqueueWork` and `ParallelDispatchEvent`, the same four checks for the branches
1.20.4, 1.21.1 and 1.21.8 (to find in which version each docs-era mechanism ended), and everything for eras before 1.19.2.

(Update v0.4.42: the replacement of item properties and overrides in 26.x and the item tint mechanism were worked out in pass 19, see section 33. The other three points stay open.)

(Update v0.4.46: the threading semantics and the same checks on the branches 1.20.4, 1.21.1, 1.21.8 and more were done in pass 20, see section 34. Still open: the four checks for eras before 1.19.2 beyond the `@Mod` constructor rule, and the threading rules of capabilities and entity data.)

---

## 33. Client and item side at 26.3: what replaced item overrides, properties and renderers, and the model extensions checked (pass 19, v0.4.42, 2026-10-01)

**Method and limits.** Branch `26.3` was cloned again (shallow). Its head is now commit `939fb13d3b96` (2026-09-30); section 32 had read `932c6e96491f`, so the head moved between the two passes and everything below is stated as of `939fb13d3b96` (S113).
Small Forge files were read in full (the events, `IClientItemExtensions`, `ForgeBlockModelData`, `ForgeFaceData`, `RenderTypeGroup`, the key parsing in `ForgeHooksClient` and `TransformationHelper`), and patches were read where they touch the model system.
Nothing was built or run, so every statement about behavior is a reading of code. Vanilla classes cannot be read, so for vanilla behavior the evidence is the names that Forge's patches and access transformer give (class A, names only) plus NeoForged documentation (class B, S116 and S117).

**Docs read this pass** (Forge docs line labeled "latest", menu 1.21.x, S114): Face Data, Render Types, Root Transforms, Transform, Baked Model, Item Overrides, Item Properties, BlockEntityWithoutLevelRenderer and the client Models introduction, all in full.
Part Visibility was first seen as prose in two search extracts and then fetched in full, including its three JSON examples (S115). The navigation link of the Model Extensions introduction (`.../rendering/modelextensions/index.md`) answers with HTTP 404, and a search for that page returned no other address, so that one page is **unread** (S115).
With that single exception, every page the docs devote to rendering hooks has been read.

### 33.1 What replaced item overrides, item properties and item renderers

Evidence in the 26.3 tree (class A, names only):
- `ModelManager.java.patch` shows a map `Map<Identifier, ClientItem.Properties> itemProperties`, a method `getItemProperties(Identifier)` that falls back to `ClientItem.Properties.DEFAULT`, and a Forge-added `getItemModels()` that returns the baked item models (`bakedItemStackModelsView`).
- `ModelProvider.java.patch` (data generation) shows an item collector `accept(Item, ItemModel.Unbaked, ClientItem.Properties)` over a `Map<Item, ClientItem>`.
- `accesstransformer.cfg` names `net.minecraft.client.renderer.item.ItemModel$Unbaked`, `net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty`, `net.minecraft.client.renderer.special.SpecialModelRenderer$Unbaked` and `net.minecraft.client.color.item.ItemTintSource`, and makes `ItemTintSources.ID_MAPPER` public.
- `ItemProperties`, `ItemOverrides` and `BlockEntityWithoutLevelRenderer` are absent from the tree (section 32, row 25). `ItemModelResolver`, `SelectItemModelProperty`, `RangeSelectItemModelProperty` and `ItemStackRenderState` are absent from Forge's tree too, which only shows that Forge does not patch them.
- **Forge 26.3 has no event for registering custom item model types, item model properties, item tint sources or special model renderers.** A search of `src/main` for the NeoForge event names found only the access transformer line above. `ModelEvent` has five members (`ModifyBakingResult`, `BakingCompleted`, `RegisterModelStateDefinitions`, `RegisterGeometryLoaders`, `BakeFluidModels`) and `RegisterColorHandlersEvent` has two (`Block`, `ColorResolvers`).
- `IClientItemExtensions` was read in full (154 lines): `of(ItemStack)`, `of(Item)`, `getFont(ItemStack, FontContext)`, `getArmPose`, `applyForgeHandTransform`, `getHumanoidArmorModel`, `getGenericArmorModel`, `renderHelmetOverlay` and the enum `FontContext`. There is no custom renderer method.

Behavior of the vanilla system (class B, NeoForged docs for 26.1, page "Client Items", S116):
- A **client item definition** is a JSON asset at `assets/<namespace>/items/<path>.json`, selected by the item's `DataComponents#ITEM_MODEL` component (default: the registry name). It has two parts: `model` and `properties` (`hand_animation_on_swap`, `oversized_in_gui`, `swap_animation_scale`).
- The `model` types shown on the page: `minecraft:model` (fields `model`, `tints`, `transformation`), `minecraft:composite` (`models`), `minecraft:range_dispatch` (`property`, `scale`, `entries` with `threshold`, `fallback`), `minecraft:select` (`property`, `cases` with `when`, `fallback`), `minecraft:condition` (`property`, `on_true`, `on_false`) and `minecraft:special` (`base`, `model` with a special renderer `type`).
- Item tints are a `tints` list of `ItemTintSource` objects (`minecraft:constant` and `minecraft:dye` appear); the list index is the tint index.
- The NeoForged primer for 1.21.4 calls range dispatch models the type most similar to the previous item override system (S117, search extract). Special models (`SpecialModelRenderer`) are the answer for models that need code, which is the role `BlockEntityWithoutLevelRenderer` had.
- NeoForge offers registration events for all of these (`RegisterItemModelsEvent`, `RegisterRangeSelectItemModelPropertyEvent`, `RegisterSelectItemModelPropertyEvent`, `RegisterConditionalItemModelPropertyEvent`, `RegisterSpecialModelRendererEvent`, `RegisterColorHandlersEvent.ItemTintSources`), according to that page. Forge 26.3 does not.
- Start of the system: the NeoForged docs list the page from their 1.21.4 line and not for 1.21.2 to 1.21.3, and a community page says "as of 1.21.4+" (S117, S118). Forge's own 1.21.4 branch was not read.

**Verdict: the replacement is identified.** In 26.x the unit is a client item definition (data), not a Java override; custom types of it need hooks that Forge 26.3 does not provide as events. Complete lists of vanilla properties and tint source types were not read.

### 33.2 The model extension pages against the 26.3 code

| # | Topic | Forge docs ("latest", menu 1.21.x) | Forge 26.3 code | Verdict |
|---|---|---|---|---|
| 1 | Face data | Key `forge_data` on an element or on a face, with `color` (ARGB), `block_light`, `sky_light` and `ambient_occlusion`. A face-level object does not inherit from the element level. For generated items the loader `forge:item_layers` takes a top-level `forge_data` keyed by layer index. | Record `ForgeFaceData(boolean ambientOcclusion, boolean calculateNormals)` with codec keys `ambient_occlusion` (default true) and `calculate_normals` (default false). The string `forge_data` occurs once in the whole tree, in the `CuboidFace` deserializer patch, so it is read at face level only. No `color`, `block_light` or `sky_light`. No loader `forge:item_layers`. A search found no code that reads either flag. | CHANGED (reduced) |
| 2 | Root transform | Top-level `transform`: either `matrix` (3 by 4) or the elements `origin` (default `opposing-corner`), `translation`, `rotation` or `left_rotation`, `scale`, `right_rotation` or `post_rotation`. Available as `IGeometryBakingContext#getRootTransform()`. | Key `transform` is parsed in `ForgeHooksClient#deserializeBlockModel` through the `Transformation` adapter of `TransformationHelper`. That adapter accepts `matrix`, `translation`, `rotation` or `left_rotation`, `scale` (a number or an array), `right_rotation` or **`post-rotation`** (with a hyphen, not an underscore), and `origin` (`center`, `corner`, `opposing-corner` or an array). `getRootTransform()` exists, as do `UnbakedGeometryHelper#applyRootTransform` and `#composeRootTransformIntoModelState`. In `ObjModel` the two lines that would use the root transform are commented out. | CONFIRMED, with differences |
| 3 | Render types | Top-level `render_type`. Six named values supplied by Forge: `minecraft:solid`, `cutout`, `cutout_mipped`, `cutout_mipped_all`, `translucent`, `tripwire`. Custom ones through `RegisterNamedRenderTypesEvent` on the mod event bus. | Keys `render_type` and a new **`render_type_fast`**; values are `Identifier`s. `RenderTypeGroup(ChunkSectionLayer block, RenderType entity, RenderType entityFabulous)`. `NamedRenderTypeManager` pre-registers only `minecraft:solid` and `minecraft:cutout`; Forge's own mod class registers `forge:item_unlit` (layer `TRANSLUCENT`). The event's `register(Identifier, ChunkSectionLayer, RenderType[, RenderType])` rejects a duplicate key and requires the vertex format `DefaultVertexFormat.ENTITY` (the docs say `NEW_ENTITY`). An unknown name gives the empty group. | CHANGED |
| 4 | Part visibility | Top-level `visibility`, a map from part name to boolean; the value is looked up through the parents and defaults to true; a loader may ignore it; only the composite and OBJ loaders of Forge use it. | Key `visibility` becomes `Map<String, Boolean>`; the API is `IGeometryBakingContext#isComponentVisible(String, boolean fallback)`. `ForgeBlockModelData#merge` combines the two maps with `Map.copyOf(...)` followed by `putAll(...)`. It is called from `ModelContext#makeData` as `data.merge(block.forgeData())`, so `this` is the child and the argument is the parent. As written, `Map.copyOf` gives an immutable map, so `putAll` would throw `UnsupportedOperationException` when both define `visibility`; and if it did not throw, the parent's entries would overwrite the child's, which is the reverse of the inheritance the docs describe. The docs' own example (a child overriding the `visibility` of a composite parent) is exactly the case that reaches this code. The loader `forge:composite` is not registered, so OBJ is the only Forge consumer left. | CONFIRMED (key and API); probable defect noted |
| 5 | Display transform | `IForgeBakedModel#applyTransform` supersedes the vanilla `getTransforms`. Contexts: `NONE`, the four hands, `HEAD`, `GUI`, `GROUND`, `FIXED`. | Neither `IForgeBakedModel` nor `BakedModel` exists (Javadoc mentions only). `ItemTransform` gained `rightRotation` (JSON `right_rotation`). `ItemTransforms` gained a map `moddedTransforms` for modded `ItemDisplayContext` values: the enum implements `IExtensibleEnum`, has `create(String, Identifier, fallback)`, a fallback chain and registry-backed ids, and the enum has `ON_SHELF`. The patched deserializer builds the modded map but still ends with the unchanged call to the old nine-argument constructor, which reads as dropping the modded transforms. | CHANGED; probable defect noted |
| 6 | Baked model | `BakedModel` with `getOverrides`, `useAmbientOcclusion`, `isGui3d`, `isCustomRenderer`, `getParticleIcon(ModelData)`, `getQuads(..., ModelData)`; extra data through `ModelData` and `ModelProperty`. | No `BakedModel`. The vanilla interface `BlockStateModel` extends `IForgeBlockStateModel`, which adds `getModelData(level, pos, state, modelData)`, `particleMaterial(ModelData)` and `collectParts(random, output, ModelData)`; the patch marks the vanilla `collectParts(random, output)` and `particleMaterial()` as deprecated. `ModelData` (`EMPTY`, `has`, `get`, builder) and `ModelProperty` still exist. | CHANGED |
| 7 | Model loaders | Docs name `forge:obj`, `forge:composite` and `forge:item_layers`. | `ClientForgeMod` registers `forge:empty`, `forge:obj` and `forge:fluid_container` (`DynamicFluidContainerModel.Loader`). | CHANGED |
| 8 | Item overrides, item properties, BEWLR (latest docs) | `ItemOverrides` and the `overrides` array with `predicate` maps; `ItemProperties#register` (the docs add that its data structures are not thread-safe, so registration goes through `FMLClientSetupEvent#enqueueWork`); BEWLR through `IClientItemExtensions#getCustomRenderer` and `BakedModel#isCustomRenderer`. These "latest" pages describe the same API as the 1.19.x pages of section 24. | See 33.1. | CHANGED (replacement identified) |
| 9 | Model storage keys | Models are stored in `ModelManager` under `ModelResourceLocation` (blocks: registry name plus the stringified state; items: registry name plus `inventory`). | Item models and client item properties are keyed by `Identifier` in the `ModelManager` patch. `ModelResourceLocation` was not searched. | CHANGED (item side only) |

### 33.3 Seen in passing

- The vanilla class `net.minecraft.resources.ResourceLocation` has **no reference left** in the 26.3 Forge tree (0 files); `net.minecraft.resources.Identifier` takes its place (`Identifier.parse`, `Identifier.fromNamespaceAndPath`, `Identifier.withDefaultNamespace`). The NeoForged 26.1 docs use `Identifier` too (class B). The docs and the earlier sections of this file use `ResourceLocation`, so any per-era API table must carry this rename for 26.x.
- `BlockColors#register(List<BlockTintSource>, Block...)` is marked `@Deprecated` by Forge with a pointer to `RegisterColorHandlersEvent.Block`, yet that event's own `register` method calls it.
- The Forge docs for Item Properties state that the data structures of `ItemProperties` are not thread-safe and that registration should be queued with `FMLClientSetupEvent#enqueueWork` (docs, class A). This is input for the threading row (pass 20).

### 33.4 What this means for VESSEL

Evidenced:
- **In 26.x the item model is data.** Overrides, item properties and item renderers are replaced by client item definitions (`assets/<namespace>/items/<id>.json`, selected by the `ITEM_MODEL` component). A definition that uses only types of the `minecraft` namespace is pure data, which is the simple side of decision 22.
- **Custom types are code.** A mod-defined item model type, property, tint source or special model renderer runs at draw time, which makes it complex by decision 22. Forge 26.3 gives no event for registering them (only the public `ItemTintSources.ID_MAPPER` through the access transformer), whereas NeoForge does (class B).
- **Decision 22's list of API names needs a 26.x column.** By the decision's own principle, complex in 26.x would be: custom `ItemModel` types, custom item properties and tint sources, special model renderers (instead of item renderers, overrides and item properties), `BlockTintSource` lists (instead of `BlockColor`), render-state block entity renderers (section 26.4). The old names stay valid for the eras before the new system; the exact version where each ended is still open (pass 20).
- **Model JSON keys differ per era.** Root-level `transform`, `render_type` and `visibility` exist in both lines; `render_type_fast` is new in 26.3; the face-level `forge_data` shrinks from {`color`, `block_light`, `sky_light`, `ambient_occlusion`} to {`ambient_occlusion`, `calculate_normals`}; the root transform alias is spelled `post-rotation` in the 26.3 code but `post_rotation` in the docs.

Inference (marked):
- Since only NeoForge offers events for custom item model types, properties, tint sources and special renderers, mods that rely on them are probably more common on NeoForge than on Forge 26.x. This is a guess; the NeoForge pass should check it.
- A pass-through of Forge model JSON to a vanilla client would have to drop or translate these keys, and the translation table has to be kept per era. This is a design consequence, not something the sources state.
- The missing readers of the two face data flags and the missing loaders `forge:composite` and `forge:item_layers` suggest that parts of the 1.21.x model extension system were dropped or are being reworked at this head. This is a guess from names.
- The two probable defects (visibility merge in row 4, modded display transforms in row 5) are readings of code. They would need a test run to be confirmed, and Forge was not built here. They matter for VESSEL only if a converted mod relies on part visibility inheritance or on modded display contexts.

### 33.5 Still missing

The complete property lists for `range_dispatch`, `select` and `condition` and the tint source types beyond `constant` and `dye`; Forge's own branches for 1.20.4, 1.21.1, 1.21.4 and 1.21.8 (when did each old class disappear); the Forge side of custom model loaders (`IGeometryLoader`, `UnbakedGeometry`) beyond names; the Model Extensions introduction page (dead navigation link, see the top of this section); item components and use behavior (row 16); `ModelResourceLocation` in the 26.3 tree.

---

## 34. Threading of the loading stages, trigger registration, duplicate registry keys before 1.19.2, and the same checks on more branches (pass 20, v0.4.46, 2026-10-02 to 2026-10-03)

**Method and limits.** Shallow clones of 20 branch heads were read (S119): `1.7.10`, `1.8.9`, `1.10.x`, `1.11.x`, `1.12.x`, `1.16.x`, `1.18.x`, `1.19.x`, `1.20.1`, `1.20.4`, `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.8`, `1.21.10`, `1.21.11`, `26.1`, `26.2` and `26.3` (head `939fb13d3b96`, the same as in pass 19). The heads of all of them are stored in `source-map.json` (v0.1.4, key `pass20_clones`).
A branch head is the last commit on the run date, not the first build of that Minecraft version, and Forge has no branch for some versions (for example 1.20.2, 1.20.3, 1.21.6, 1.21.7), so "between the heads of X and Y" below means "somewhere in that range of Forge's history". Nothing was built or run; every behavior below is a reading of code (class A) unless it says otherwise.
Shiroz also supplied a reference document on 2026-10-03; section 34.6 says what it is and what was cross-checked against the trees.

### 34.1 Threading and listener order in the loading stages of 26.3

- **Two kinds of state.** `ModStateProvider` defines `ERROR`, `VALIDATE`, `CONSTRUCT`, `CONFIG_LOAD`, `COMMON_SETUP`, `SIDED_SETUP`, `ENQUEUE_IMC`, `PROCESS_IMC`, `COMPLETE`, `DONE`; Forge's own `ForgeStatesProvider` adds `CREATE_REGISTRIES` (after `CONSTRUCT`), `INJECT_CAPABILITIES`, `UNFREEZE_DATA`, `LOAD_REGISTRIES`, `FREEZE_DATA` (after `COMPLETE`) and `NETWORK_LOCK`. Each state has a phase (`ModLoadingPhase`: `ERROR`, `GATHER`, `LOAD`, `COMPLETE`, `DONE`). The merged order of the two providers and the mapping of phases to the three calls `gatherAndInitializeMods`, `loadMods` and `finishMods` (made in this order by `ServerModLoader`) were **not** read.
- **Parallel states.** Six states use `ParallelTransition`: `CONSTRUCT`, `COMMON_SETUP`, `SIDED_SETUP`, `ENQUEUE_IMC`, `PROCESS_IMC`, `COMPLETE`. Their events are `FMLConstructModEvent`, `FMLCommonSetupEvent`, `FMLClientSetupEvent` (client) or `FMLDedicatedServerSetupEvent` (dedicated server), `InterModEnqueueEvent`, `InterModProcessEvent` and `FMLLoadCompleteEvent`; the transition's event factory has the type `BiFunction<ModContainer, ModLoadingStage, ParallelDispatchEvent>`, so all six are `ParallelDispatchEvent`s.
- **Where the listeners run.** `ParallelTransition` selects `ThreadSelector.PARALLEL`, i.e. the executor `ModWorkManager.parallelExecutor()`: a `ForkJoinPool` with as many threads as the FML config value `MAX_THREADS`, threads named `modloading-worker-N`. Each mod gets one task that sets the active container, runs a per-stage activity if one exists, and posts the event on that mod's own bus group.
- **Order between mods.** In the 26.3 code (`ModStateTransitionHelper`) the task of a mod starts only after the tasks of the mods it depends on (`mod.dependencies`) have finished; a mod without dependencies starts at once, so unrelated mods run **concurrently and in no defined order**. Before the 1.21.3 head this chaining does not exist (34.2).
- **`enqueueWork`.** `ParallelDispatchEvent#enqueueWork(Runnable)` and `#enqueueWork(Supplier)` find the queue of the event's stage (`DeferredWorkQueue`, one per `ModLoadingStage`) and return a `CompletableFuture`; if no queue exists they throw `RuntimeException("No work queue found!")`. The future is built with an executor that only stores the task, so it is not completed until the queue runs.
- **When the queued work runs.** After all mods have finished the stage's event, `ParallelTransition#finalActivityGenerator` runs `stage.getDeferredWorkQueue().runTasks()` on the **synchronous executor**. `runTasks` executes the tasks one after another in queue order. The queue is a `ConcurrentLinkedDeque` filled by the parallel listeners, so the order **between mods** is the order in which the calls arrived and is not deterministic. During each task the owning mod is the active container; exceptions are collected, logged as fatal and rethrown together; a task that takes a second or more produces a warning.
- **The synchronous executor.** `ModWorkManager.SyncExecutor` only collects tasks and runs them when `drive` is called; `ModLoader` calls `drive` in a loop while it waits for a transition, so queued work runs **on the thread that runs the ModLoader loop**. Callers are `ServerModLoader`, `ClientModLoader` and `DatagenModLoader`. Which OS thread that is on the client was not read.
- **Inline states are not parallel.** `CONFIG_LOAD` (loads the CLIENT and COMMON configs), `CREATE_REGISTRIES`, `INJECT_CAPABILITIES`, `UNFREEZE_DATA`, `LOAD_REGISTRIES`, `FREEZE_DATA` and `NETWORK_LOCK` run a `Runnable` directly on the driving thread (`ModLoader#handleInlineTransition`).
- **Registration is sequential.** `GameData#postRegisterEvents` sends one `RegisterEvent` per registry: the vanilla registries first in vanilla order, then the others sorted by id. Every registry key of `BuiltInRegistries.REGISTRY` counts as a vanilla registry (`RegistryManager`). Each event goes through `ModLoader#postEventWrapContainerInModOrder`, which visits the mods in sorted order (`ModList#forEachModInOrder`, which iterates the sorted container list; the body of `postEventWithWrapInModOrder` was not read), with the mod as active container. The Forge registry is unfrozen for its event and frozen after it; object holders are applied after each registry. Any exception is collected, the registries are rolled back to the vanilla snapshot and the aggregate is rethrown.
- **`DeferredRegister`.** It listens to `RegisterEvent` on the mod's bus group and throws `IllegalStateException` if entries are added after the event has fired.
- **Network packets.** `CustomPayloadEvent.Context#enqueueWork(Runnable)` takes the executor of the logical side (`LogicalSidedProvider.WORKQUEUE`, a `BlockableEventLoop`). If the calling thread is not that executor's thread it calls `submitAsync` and returns the future; otherwise it runs the task at once and returns a completed future. The thread on which a packet handler is called was not read, and what the `BlockableEventLoop` objects are was not read.

### 34.2 The same loading code on older branches (heads)

| Branch head | How the event reaches the mods | Where queued work is run | `@Mod` constructor | Mod bus type |
|---|---|---|---|---|
| `1.12.x` | none of `DeferredWorkQueue`, `ParallelTransition`, `ModWorkManager`, `ParallelDispatchEvent` exists; the loading code of this era (`LoadController`) was not read | n/a | not read | not read |
| `1.16.x` | `ModContainer` posts the event per mod; no `ParallelTransition`, no `ThreadSelector` | `runTasks()` is called from `ModLoadingStage` | not read | not read |
| `1.18.x` | `ModContainer#buildTransitionHandler` per mod, all started on the executor, no dependency chaining; `ThreadSelector` exists | `ParallelTransition` | **no-argument constructor only** | `IEventBus` |
| `1.19.x`, `1.20.1`, `1.20.4`, `1.21.1` | as `1.18.x` (`ModList#futureVisitor` maps all mods and gathers them, read in `1.20.4`) | `ParallelTransition` | the constructor taking the loading context class, else the no-argument one | `IEventBus` read in `1.20.4` and `1.21.1`; not read in `1.19.x` and `1.20.1` |
| `1.21.3`, `1.21.4`, `1.21.5` | `ModStateTransitionHelper` with dependency chaining (found by text search; code read only at `1.21.8` and `26.3`) | `ParallelTransition` | context or no-argument (read at `1.21.4`) | `IEventBus` |
| `1.21.8`, `1.21.10`, `1.21.11`, `26.1`, `26.2`, `26.3` | dependency chaining (`1.21.8` and `26.3` read, the others by text search) | `ParallelTransition` | context or no-argument | `BusGroup` per mod (`BusGroup.create("modBusFor" + id)` at `1.21.8`; at `26.3` the call also passes `IModBusEvent.class`) |

So the boundaries are: context-taking `@Mod` constructor between the `1.18.x` and `1.19.x` heads; dependency-ordered dispatch between `1.21.1` and `1.21.3`; `IEventBus` to `BusGroup` between `1.21.5` and `1.21.8`. The docs pages and the `FMLModContainer` code agree on the constructor rule from `1.19.x` on.

### 34.3 Advancement trigger registration (answers the open points of 26.8, 27.6, 28.5 and 28.7)

- **The earlier search was too narrow.** Section 28.5 searched `src/main/java` and the patches and found no trigger code. The Forge tests are a separate module in 26.3 (`forge-tests`; `src/test` on the older branches), and that module has the answer. The access transformer of 26.3 also exposes `CriteriaTriggers#register(String, CriterionTrigger)`.
- **How a mod registers a trigger from the 1.20.4 branch on.** The test mod `CriterionTest` declares `DeferredRegister.create(BuiltInRegistries.TRIGGER_TYPES.key(), MOD_ID)` and registers `BreakWithItemCriterion::new` under `"criterion"`. The same lines are in `1.20.4`, `1.21.1`, `26.1`, `26.2` and `26.3`. So **the vanilla registry used for triggers is `BuiltInRegistries.TRIGGER_TYPES`**, and a mod fills it through `DeferredRegister`, i.e. through `RegisterEvent`.
- **Name of the registry key.** The NeoForged documentation (class B, S120) says triggers "must be registered to the `Registries.TRIGGER_TYPE` registry", with a `DeferredRegister` example, on its pages for 1.20.4, 1.20.6, 1.21.4, 1.21.5 and the latest line. The two spellings (`BuiltInRegistries.TRIGGER_TYPES` in Forge's code, `Registries.TRIGGER_TYPE` in the NeoForged text) name the registry and its key constant and do not contradict each other.
- **Up to 1.20.1.** The access transformer exposes the one-argument `CriteriaTriggers#register(CriterionTrigger)`; the Forge docs for 1.18.x (S121, excerpt) say to call it in `FMLCommonSetupEvent` through `enqueueWork` because it is not thread-safe. The test mods of `1.20.1` and older have no `TRIGGER_TYPES` line. The change lies between the `1.20.1` and `1.20.4` heads.
- **What Forge adds.** `GameData#postRegisterEvents` sends a `RegisterEvent` for every registry key of `BuiltInRegistries.REGISTRY` with no Forge registry for non-Forge registries, and `RegisterEvent#register` then calls vanilla `Registry.register`. For a vanilla registry such as the trigger types Forge therefore adds **no override rule and no duplicate rule of its own**. What vanilla does on a duplicate key was **not read** (vanilla code cannot be read).
- **Package churn.** The trigger classes moved: `net.minecraft.advancements.critereon.SimpleCriterionTrigger` (`1.20.4`, `1.21.1`), `net.minecraft.advancements.criterion.SimpleCriterionTrigger` (`26.1`), and the package `net.minecraft.advancements.triggers` for `CriteriaTriggers`, `CriterionTrigger` and (seen at `26.2`) `SimpleCriterionTrigger` from the `26.2` head on. This is read from the imports of the test mod and from the access transformer.

### 34.4 Duplicate registry keys before 1.19.2 (answers the open point of 27.6)

- **`RegistryBuilder`.** `allowOverrides` starts as true in every branch head from `1.12.x` to `26.3`. The callers of `disableOverrides()`: one in `1.12.x`, `1.16.x` and `1.18.x` (the data serializer registry); two from the `1.19.x` head on (entity data serializers and the item display context registry, whose builder in `1.19.x` also has `setMaxID(128 * 2)` and `disableSaving()`). So the "second registry" of 27.3 first appears at the `1.19.x` head, not at `1.18.x`.
- **`ForgeRegistry#add` in `1.12.x`** (read) has the same structure as in 26.3: the same object under the same name gives a big warning and returns its id; a different object under an existing name throws `IllegalArgumentException` ("has been registered twice") when overrides are off, throws `IllegalStateException` when no owner can be determined, and otherwise replaces the old entry (the old one goes to an `overrides` multimap) with a log line **only when a `DEBUG` flag is on**; the same object under two names throws; adding after the lock throws `IllegalStateException` ("added too late"). The same messages are found by text search in `1.16.x`, `1.18.x`, `1.19.x` and `1.20.1`.
- **`1.7.10` (E1)** (read): `GameData#register(Object, String, int)` accepts only `Block` and `Item`, otherwise `IllegalArgumentException`; the name gets the active mod id as prefix (`minecraft` when no mod is active), and an existing different prefix is prefixed again. `FMLControlledNamespacedRegistry#add` throws `IllegalArgumentException` ("has been registered twice") when a **different** object already has the name: **there is no override**. The same object under the same name only gives a big warning. Adding after the freeze gives a big warning, **not** an exception. The vanilla-style call `putObject` never replaces: it ignores the call, or adds an alias when the object already has another name. The only replacement path is the substitution API: `GameRegistry#addSubstitutionAlias` throws `ExistingSubstitutionException` when an alias from or to the name already exists and `IncompatibleSubstitutionException` when the replacement is incompatible; `activateSubstitution` then makes the substitute the active object (the delegate swap with the debug line "Replacing ..." is shown for items).
- **`1.8.9`, `1.10.x`, `1.11.x`.** These heads have `FMLControlledNamespacedRegistry` (no `ForgeRegistry`; `1.10.x` and `1.11.x` also have a `RegistryBuilder`, `1.8.9` does not). Text search finds the same three "has been registered twice" messages as in `1.7.10` and no `allowOverrides`, no owner check and no throw after "added too late". The code itself was **not** read for these three heads, so "same as 1.7.10" is an inference from message counts.
- **Result for the decision 8 statement ("Forge overrides silently on a duplicate key").** It holds from the `1.12.x` head on (with the two exceptions above and the `DEBUG` precision), and it does **not** hold for E1: in `1.7.10` Forge itself stops on a duplicate name, as VESSEL does, and replaces only through substitution aliases.

### 34.5 The four checks of section 32 on more branches (heads)

| Head | `setCraftingSize` patch | Item sub-event of `RegisterColorHandlersEvent` | `ItemProperties` patch | `ItemOverrides` patch | `getCustomRenderer` | Client item patches (`ClientItem`, `ItemModel`) | `CreateSpecialBlockRendererEvent` | Block tint sources |
|---|---|---|---|---|---|---|---|---|
| `1.20.4` | yes | yes | yes | yes | yes | no | no | no |
| `1.21.1` | yes | yes | yes | yes | yes | no | no | no |
| `1.21.3` | yes | yes | yes | no (name left only in `IModelBuilder`) | yes | no | no | no |
| `1.21.4`, `1.21.5` | yes | no | no | no | no | yes | yes | no |
| `1.21.8`, `1.21.10`, `1.21.11` | yes | no | no | no | no | yes | yes | no |
| `26.1`, `26.2`, `26.3` | yes | no | no | no | no | yes | no | yes |

The client item patches were found by a word-bounded search at `1.21.1`, `1.21.3`, `1.21.4`, `1.21.8`, `26.1` and `26.3`; for `1.21.5`, `1.21.10`, `1.21.11` and `26.2` a looser search of the `ItemModel` names gave the same count as at `1.21.4` and `26.3`.

Boundaries from this table (all between heads):
- **Item overrides and the item API of the docs era** (`ItemProperties`, `ItemOverrides`, `getCustomRenderer`, the item color event) are in the `1.21.1` head; `ItemOverrides` is gone at `1.21.3`; the rest is gone at the `1.21.4` head, where the client item definition patches (`ModelManager`, `ModelProvider`) first appear. This matches NeoForged's documentation, which lists the client item page from 1.21.4 on (S116).
- **`CreateSpecialBlockRendererEvent`** exists only in the `1.21.4` to `1.21.11` heads: an event with `register(Block, SpecialModelRenderer.Unbaked)` over a `Map<Block, SpecialModelRenderer.Unbaked>`, posted by `ForgeEventFactoryClient`. This corrects the sentence in 33.1 that Forge has no event for special model renderers: it had one, for blocks only, until the `26.1` head.
- **`ShapedRecipe#setCraftingSize`** is patched in every head from `1.20.4` to `26.3` (existence only; the thread-safety statement was not read).
- **Block tint sources** (`BlockTintSource` lists instead of `BlockColor`) first appear at the `26.1` head; the `1.21.4` to `1.21.11` heads still register a `BlockColor`.

### 34.6 The reference document supplied on 2026-10-03

- **What it is (S122).** A cookbook by Shiroz for PayBot: a cross-reference of Mojmap, Fabric Intermediary, Yarn, SRG and MCP names for `ItemStack`, `DataComponents`, `CompoundTag` and the registries, a reflection cookbook, and a table of 100 targets from 1.14.2 to 26.2 (41 Fabric, 39 Forge, 19 NeoForge, 1 Paper plugin). He asked that it be used **as a reference only**. It is evidence class C: user-provided, produced by his own tooling from jars on his disk, which were not available here, so none of its name tables could be checked against bytecode.
- **Cross-checked against the Forge trees (class A):**
  1. *Mapping namespace of Forge.* The document names SRG for Forge 1.17.1 to 1.20.4 and Mojmap for "Forge 1.20.6+". The access transformers of the heads use MCP-style `func_` and `field_` names in `1.16.x` (196 of 331 lines), SRG `m_` and `f_` names in every head from `1.18.x` to `1.21.11` (for example 612 of 796 lines at `1.21.11`), and official names from `26.1` (no SRG name in 714 lines). So for the access transformer the switch to official names is at `26.1`, not at 1.20.6 (1.20.6 has no branch; `1.21.1` to `1.21.11` were checked). Whether the **runtime** names of mods for Forge 1.21.x are SRG or official cannot be decided from source; it needs a look at the jars.
  2. *`ResourceLocation`.* The cookbook gives `ResourceLocation` as the Mojmap name from 1.21.0 to 26.2. In the Forge trees `ResourceLocation` occurs in 225 to 252 files in every head from `1.16.x` to `1.21.10` and in **none** from the `1.21.11` head on, where `Identifier` takes its place (828 matching lines at `1.21.11`). So the rename lies between the `1.21.10` and `1.21.11` heads, and the cookbook is out of date for 1.21.11 and 26.x on this point.
  3. *Forge build numbers.* Of its 39 Forge targets, 18 can be compared with `version-index.json`: 9 builds are identical and 9 differ but have the same major number; for the other 21 versions `builds_seen` is empty. No contradiction in the major numbers; the exact builds were not confirmed.
  4. *MCP and SRG eras.* `func_` names in `1.16.x` and `m_` names in `1.18.x` agree with the document (1.17.x was not checked).
- **Seen on reading, not checked against jars:** the `forge-1.16.1` row uses the path `net.minecraft.world.item.ItemStack` while `forge-1.16.2` uses `net.minecraft.item.ItemStack`; the CustomData column of the Forge 1.16.1, 1.17.1, 1.18, 1.18.1 and 1.18.2 rows shows `net.minecraft.network.chat.NbtComponent`, which looks like a Yarn name of 1.20.5 and later and not a class of those versions; the Paper plugin row repeats `DataComponents` columns that do not apply to the Bukkit API; the Fabric 1.14.2 row says `getOrCreateNbt` where the 1.14.3 row says `getOrCreateTag`.
- **Not checked at all:** every Fabric and NeoForge name, the NBT and `DataComponents` names, method names inside vanilla classes, and the `getString` change to `Optional` at 1.21.5 (vanilla code cannot be read).

### 34.7 What this means for VESSEL

Evidenced:
- **Registration order.** In Forge 26.3 registration is sequential: registry by registry, mod by mod in sorted order, on the thread that drives the loader. The setup events of all mods run in parallel, with no cross-mod order before the `1.21.3` head, and `enqueueWork` queues run afterwards one by one on the driving thread. This is the model that VESSEL's step 10 (decision 19) and its module scheduler have to imitate or deliberately differ from.
- **Trigger ids (decision 20, `identity-and-collision.md` section 5.3).** From the `1.20.4` head on, trigger ids are plain entries of a vanilla registry that goes through `RegisterEvent`, so the registry scan of section 5 sees them like any other registry key; the special rule for `CriteriaTriggers#register` is needed only up to `1.20.1`. Forge adds no duplicate rule for that registry, so the rule for it must come from VESSEL.
- **Duplicate keys by era.** The justification of decision 8 needs a per-era wording: Forge overrides from the `1.12.x` head on (silently unless a debug flag or debug log is on), but in E1 Forge itself refuses a duplicate name, so VESSEL's hard stop is not stricter there.
- **Era boundaries** of the docs-era mechanisms are in 34.2 and 34.5; they give the version ranges in which each name table of the IR applies.
- **Names per era.** The namespace used by Forge's own transformers is SRG from 1.18 to 1.21.11 and official from 26.1; `ResourceLocation` is called `Identifier` from the `1.21.11` head on.

Inference (marked):
- Mods built for Forge versions up to 1.21.11 probably reference SRG names at runtime, since Forge's own access transformers do. If so, the name tables of the IR need a namespace column per era. This is not confirmed (see 34.6, point 1).
- The two probable defects of 33.2 and the missing readers of the face data flags suggest that the model extension code of 26.x is being reworked; this section adds nothing to that.

### 34.8 Still missing

The body of `postEventWithWrapInModOrder`; the OS thread of the client loader and the thread that calls packet handlers; the duplicate handling code of the `1.8.9`, `1.10.x` and `1.11.x` heads beyond message counts; what vanilla does on a duplicate trigger key (vanilla code); the loading code of `1.12.x` (`LoadController`) and of `1.17.x`; the threading rules for capabilities and entity data (row 34); the thread-safety statement of `CraftingHelper#register` for each era up to 1.20.1; a jar-level check of runtime names (34.6, point 1).

---

## 35. Conditional data of 1.20.4 and 1.21.1, the tags list, the rest of the vanilla metadata table, and the Javadoc inventory (pass 21, v0.4.48, 2026-10-03 to 2026-10-04)

**Method and limits.** The shallow clones of pass 20 were reused (S119). New in this pass: a full clone of `Bukkit/Bukkit` (S123), the Forge branches `1.6`, `v164-pregradle` and `mc172` (S124), 15 Forge tags of which only the commit date, the subject and the top-level names were read (S125), the Forge source files named in S126, and web search extracts (S127 to S131). Nothing was built or run. Heads and dates are stored in `source-map.json` (v0.1.5, key `pass21_clones`).

### 35.1 Conditional data in 1.20.4 and 1.21.1 (answers the open point of 28.2 and 28.7)

Section 28.2 left the exact format of these two heads open and counted three formats by era. Reading `ICondition`, `ConditionalRecipe`, `ConditionalAdvancement`, `ForgeHooks`, `ForgeMod` and the two patches shows **four** formats:

| Head | Condition key | Recipe wrapper (`forge:conditional` serializer) | Advancement wrapper | Where the game applies conditions |
|---|---|---|---|---|
| `1.20.1` (read) | `conditions`, a list tested with `CraftingHelper.processConditions` | `recipes`: each entry `{ "conditions": [...], "recipe": {...} }` | `advancements`: each entry `{ "conditions": [...], "advancement": {...} }`; a single advancement may carry `conditions` itself | recipes and advancements |
| `1.20.4` | `forge:condition`, one object; `ICondition.CODEC` dispatches through the registry `forge:condition_codecs` (`ForgeRegistries.CONDITION_SERIALIZERS`, element type `Codec`) | `recipes`: each entry is **the recipe JSON itself**, with its normal `type`, and an optional inline `forge:condition`; the first entry that passes is used; if none passes the result is an error ("No recipe passed conditions, if this is the case, you should have an outer condition."); the wrapper can also carry an outer `forge:condition` | `advancements`: each entry is the advancement JSON itself with an optional inline `forge:condition`; the first passing entry is returned; without that array the file itself is tested | `RecipeManager` and `ServerAdvancementManager` patches only |
| `1.21.1` | same key and registry, element type `MapCodec`; evaluation uses the `RegistryOps` context (`ConditionCodec.getContext(ops)`, `condition.test(context, ops)`) | `recipes`: each entry is `{ "forge:condition": ..., "recipe": {...} }`, a **separate `recipe` key** (decoder: `map.get("recipe")`); an outer `forge:condition` is allowed | as in `1.20.4` (inline entries) | the same two patches |
| `1.21.3` and later | `forge:condition`, plus a root key `forge:conditional` holding an array of full data files, on **every** listener built on `SimpleJsonResourceReloadListener` (section 28.2, read at 26.3; the text search finds `forge:conditional` and `readConditional` in the `SimpleJsonResourceReloadListener` patch from this head on) | not re-read here | not re-read here | all such listeners |

- The condition ids are the same in `1.20.4`, `1.21.1`, `1.21.4` and `26.3`: `and`, `false`, `item_exists`, `mod_loaded`, `not`, `or`, `true`, `tag_empty` (read in `ForgeMod`; in `1.20.1` the serializers are added through `CraftingHelper.register(IConditionSerializer)` and the names were not read).
- The recipe serializer type `forge:conditional` is registered in `ForgeMod` at `1.20.4` and `1.21.1`. Whether `1.21.3` and later still use it for recipes was not looked at.
- Illustration of the two recipe shapes, as read from the decoders (not run): `1.20.4`: `{"type":"forge:conditional","recipes":[{"forge:condition":{...},"type":"minecraft:crafting_shaped", ...}]}`; `1.21.1`: `{"type":"forge:conditional","recipes":[{"forge:condition":{...},"recipe":{"type":"minecraft:crafting_shaped", ...}}]}`.

### 35.2 The tags list (answers the open point of 25.6)

- **Method.** `Tags.java` was parsed in 12 heads; the figures count lines of the form `public static final <type> NAME = helper("path")` inside the nested classes, so tags declared in any other way would be missed and the numbers are **lower bounds**. In `26.3` a nested class `BlockItems` declares 107 block-and-item pairs once; they are counted here for both registries.
- **Counts by head** (namespace `forge` / namespace `c`): `1.16.x` 208 / 0; `1.18.x` 308 / 0; `1.19.x` 325 / 0; `1.20.1` 320 / 0; `1.20.4` 320 / 0; `1.21.1` 181 / 334; `1.21.3` 187 / 319; `1.21.4` 187 / 333; `1.21.8` 187 / 333; `1.21.11` 187 / 334; `26.1` 9 / 483; `26.3` 6 / 492.
- **Reading.** Up to the `1.20.4` head every tag of Forge is in the namespace `forge` (classes `Blocks`, `Items`, `Fluids`, with `EntityTypes` and `Biomes` from `1.18.x`). At the `1.21.1` head 334 tags are in the namespace `c`, 111 tags that were `forge:` at `1.20.4` have moved, and the classes `Enchantments` and `Structures` appear. At `26.1` another 75 tags have moved (for example block and item `ores/diamond` is `forge:` through `1.21.11` and `c:` at `26.1`).
- **What is still `forge:` at 26.3:** block `enderman_place_on_blacklist`, block and item `storage_blocks/amethyst` and `storage_blocks/quartz`, item `enchanting_fuels` (6 tags).
- **Of the 320 tags of `1.20.4`:** 177 exist at `26.3` under the same path in `c`, 6 are still `forge:`, and 137 no longer exist under the same registry and path. Many of the 137 are renames, for example block `cobblestone` is `cobblestones` at `26.3`; renames were not matched.
- **Docs.** The Forge docs pages on tags (search extracts, S127) tell modders to use the `forge` namespace for community tags in the 1.13.x, 1.16.x, 1.18.x and 1.19.2 lines, which matches the code up to `1.20.4`; the 1.21.x and latest lines say that Forge bundles Forge-specific and common tags, that `c` tags are common across loaders and that loader-specific `c` tags may be kept under `forge` until they become common, which matches the code from `1.21.1` on. The docs menu has a page "Tags List"; it points to GitHub and was **not fetched**, so the list above comes from the code.
- **Not read:** whether Forge treats `forge:ingots/iron` and `c:ingots/iron` as the same tag at run time (no alias was seen in `Tags.java`), and the matching of renamed paths.

### 35.3 The rest of the vanilla metadata table (answers the open point of 31.6; data in `vanilla-metadata-table.json` v0.1.2)

Sources: Forge `RotationHelper` of the `1.7.10` head and of the `v164-pregradle` and `mc172` heads, the Bukkit `org.bukkit.material` classes (S123), and for mushrooms the wiki text through mirrors and the Spigot Javadoc (S128, S129). Community files with no stated origin (Skript aliases, Minecraft-Overviewer commits, S130) were read and never counted as a second source. Result: five new kinds and the comparator bits, 13 new fields.

| Kind | Field | Value | Level | Basis |
|---|---|---|---|---|
| cocoa | facing, mask `0x3` | 0 south, 1 west, 2 north, 3 east | corroborated | Forge puts cocoa in the same rotation group as beds; Bukkit `CocoaPlant` gives the same numbers and calls the opposite face the attached one |
| cocoa | size, mask `0xC` | 0 small, 4 medium, 8 large | single source | Bukkit |
| end portal frame | facing, mask `0x3` | as the bed map | single source | Forge only; the eye bit is `unknown` |
| vine | four flag bits | `0x1`, `0x2`, `0x4`, `0x8` | corroborated | Forge rotates with `(meta << 1) \| ((meta & 0x8) >> 3)`, which needs four cyclic flags; Bukkit has the same four bits |
| vine | face names | 1 south, 2 west, 4 north, 8 east | single source | Bukkit; Forge confirms only the cyclic order |
| powered and detector rail | shape, mask `0x7` | 0 and 1 flat, 2 to 5 ascending (same map as rails) | corroborated | Forge mask `0x7`; Bukkit `ExtendedRails` |
| powered rail | powered, `0x8` | 1 powered | single source | Bukkit |
| detector rail | pressed, `0x8` | 1 pressed | single source | Bukkit |
| huge mushroom | stem on four sides | 10 | corroborated | Bukkit, wiki mirror, Spigot Javadoc |
| huge mushroom | uniform textures | 11 to 13 pores, 14 cap, 15 stem | single source | wiki mirror and Spigot Javadoc agree, but neither is 1.6 to 1.7 era code, so the rule for corroborated is not met |
| huge mushroom | cap faces 1 to 9 | see below | convention unresolved | Forge, wiki and Spigot against the Bukkit class |
| comparator | mode and power, `0xC` | not read | unknown | no Bukkit class exists for it |

- **Mushroom cap faces.** Forge's side map is 2 north, 4 west, 6 east, 8 south, and its corner map (1, 3, 9, 7) cycles in the clockwise order of the wiki corners (1 north-west, 3 north-east, 9 south-east, 7 south-west); the wiki text and the Spigot Javadoc give the same faces. The Bukkit helper class describes the same structure turned by 90 degrees (2 west, 4 south, 6 north, 8 east). Because one of the two code sources disagrees, the field is kept `convention_unresolved` and opaque. Three sources agree against one, so the Forge reading is the likely one; a decompile could settle it.
- **The Bukkit snapshot era is settled in part.** Head `f210234e59` has author date 2013-07-07 and commit date 2014-08-17. Its `Material` enum contains `DOUBLE_PLANT`, `LOG_2`, `STAINED_GLASS` and `PACKED_ICE`; in the Forge sources double plant, log2 and stained glass appear in the heads of the `mc172` and `1.7.10` branches and not in the 1.6.4 head, and `SLIME_BLOCK` and `BANNER` exist neither in this enum nor in the Forge 1.7.10 tree. So the snapshot is **not older than the 1.7.2 era**; that it is exactly 1.7.10 is not shown.
- **Forge before 1.7.** `RotationHelper` of `v164-pregradle` (1.6.4), `mc172` and `1.7.10` are identical as far as the script compared them (10 direction maps and 18 mask entries, 0 differences). The `1.6` branch head (2013-07-01) has a 47-line version that rotates only chests, pistons, droppers and dispensers. So the Forge side of the table holds for 1.6.4 to 1.7.10; nothing was read for 1.4 and 1.5.
- **New totals:** 55 fields: 33 corroborated, 15 single source, 3 convention unresolved, 2 conflict, 2 unknown (v0.1.0 had 42 fields: 29, 9, 2, 2).
- **The wiki page is still cut off** after the torches section when fetched, so no wiki row for cocoa, vine, rails, comparator or end portal frame was read; only the mushroom values came through mirrors.

### 35.4 Javadoc inventory (answers the first half of row 29)

No official Javadoc site for Forge was found. The Forge documentation repository says of itself that it does not include Javadocs, and the "Forge Community Wiki" Javadoc repository was archived on 2022-12-16. What exists are third-party mirrors (class C), found through their list pages (S131); the mirror pages themselves were **not opened**. **[Update 2026-10-05 (v0.4.51): search extracts of four `TickEvent` mirror pages were read in pass 22 (S140); the other mirror pages are still unopened.]**

| Era | Minecraft and build listed | Mirror |
|---|---|---|
| E1a, E1b before 1.7.10 | none found | |
| E1b end | 1.7.10-10.13.4.1614 | skmedix |
| E2 | 1.8.9-11.15.1.2318, 1.9.4-12.17.0.2051, 1.10.2-12.18.3.2185, 1.11.2-13.20.0.2228, 1.12.2-14.23.5.2859 (and 1.12.2-14.23.5.2854) | skmedix, Nekoyue |
| E3 | 1.13.2-25.0.219, 1.14.4-28.2.23, 1.15.2-31.2.47, 1.16.5-36.2.39 | Nekoyue |
| E4a | 1.17.1-37.1.0, 1.18.2-40.2.1, 1.19.3-44.1.8 (Nekoyue); 1.19.4-45.1.0, 1.20-46.0.14, 1.20.1: 47.1.0, 47.2.0, 47.2.17 (MCStreetguy) | Nekoyue, MCStreetguy |
| E4b | 1.20.2-48.1.0, 1.20.3-49.0.2, 1.20.4-49.0.49, 1.20.6-50.1.0 and 50.1.6, 1.21-51.0.8 | MCStreetguy |
| E4b from 1.21.1, E5 | none found | |

- The MCStreetguy list says its pages "may be outdated and partially incomplete", and that builds not marked stable are based on "latest" and not "recommended" releases. Its guide describes generating Javadocs from a repository tag with a custom Gradle task.
- **Use for VESSEL:** Javadocs are class C and exist only up to 1.21; for 1.21.1 and later the branch source (class A) is the only reference, which is what sections 26 to 34 used.

### 35.5 Source revision per era, as far as the clones and tags show (second half of row 29)

- **Heads read so far, by era:** E1b end `1.7.10`, plus `1.6`, `v164-pregradle` and `mc172` for the metadata work; E2 `1.8.9`, `1.10.x`, `1.11.x`, `1.12.x`; E3 `1.16.x`; E4a `1.18.x`, `1.19.x`, `1.20.1`; E4b `1.20.4`, `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.8`, `1.21.10`, `1.21.11`; E5 `26.1`, `26.2`, `26.3`. E1a (1.1 to 1.2.5) has **no branch** and only tags.
- **Tag probe (S125, date and subject of the commit only).** `1.0.0` 2011-07-31, `1.0.4` 2011-08-06, `1.2.1` 2011-12-10, `1.2.5` 2012-01-14 (top level shows `MLProp.java`, `forge_client`, `forge_common`, `forge_server`), `1.2.5@213` 2012-01-13, `1.3.2` 2012-01-29; `v7.2` 2013-12-12 ("First functional release under reobfuscation"), `v7.99` 2015-05-13 ("Merged FML @ 1.7.10"), `v99.99` 2015-05-11; `23.5` 2019-09-03, `25.0` 2019-02-14 ("Initial 1.13.2 update"), `26.0` 2019-06-07, `28.2` 2020-02-03, `36.2` 2021-07-21 ("New 1.16.5 RB"), `37.1` 2021-11-30.
- **Two cautions from the probe.** The tags named `1.0.0` to `1.3.2` are Forge's own version numbers: `1.3.2` is dated 2012-01-29 and the earliest tag is dated 2011-07-31, so these names cannot be read as Minecraft versions, and the era table's count of four versions for E1a cannot be tested against tags by name. And tag dates do not follow the release order (`23.5` is dated later than `25.0`), so a date alone does not place a tag in an era.
- **Observation, not a conclusion:** the repository has tags from 2011-07-31, earlier than the E1a start of Minecraft 1.1 that the era table states from S30; whether Forge for earlier Minecraft versions belongs in E1a is not decided here.

### 35.6 What this means for VESSEL

Evidenced:
- **Conditional data needs an era table with four reader variants** (35.1). The condition ids are stable from `1.20.4` on; evaluating `mod_loaded` and `item_exists` needs the module list and the registries of VESSEL, which is a design consequence, not something the sources say.
- **Tag namespaces change by era** (35.2): `forge:` only up to `1.20.4`, a mix from `1.21.1`, almost only `c:` at `26.1` and later. VESSEL's own tag merging (decision 19) therefore sees `forge:x` and `c:x` as different names; whether to alias them is a design question that the sources do not answer (open question 3 of the status file). **[Answered 2026-10-04 (v0.4.50): no aliasing, decision 29 of the status file: namespaced ids from different namespaces are completely different names.]**
- **The metadata table** now has 55 fields; the conversion rule of decision 25 (convert only at `corroborated`, warn at `single_source`, opaque otherwise) means cocoa facing, vine flag bits, rail shape and the mushroom stem value convert, and the mushroom cap faces stay opaque.
- **Javadoc is not a source for 1.21.1 and later** (35.4).

Inference (marked): the Bukkit helper class for mushrooms may use a different compass convention than Forge; this is a guess from the pattern of the numbers and is not tested.

### 35.7 Still missing

Wiki rows for cocoa, vine, rails, comparator and end portal frame (the page fetch is still cut off); the activator rail; the comparator mode and power bits; the eye bit of the end portal frame; the Pre-flattening page after the torches section; Forge for 1.4 and 1.5; Shiroz's own decompile if he chooses option A; the `forge:conditional` recipe serializer at `1.21.3` and later; the parameter names of the built-in conditions; the "Tags List" docs page itself and the matching of renamed tags; whether Forge aliases `forge:` and `c:` tags at run time; the content and availability of the Javadoc mirror pages; the mapping of the old tags `1.0.0` to `1.3.2` to Minecraft versions.

---

## 36. Entities, commands, the scheduler and Mojang's official names (pass 22, v0.4.51, 2026-10-04 to 2026-10-05)

Pass 22 takes rows 17, 19 and 20 of the 37-row list (decision 28: the docs, the Forge 26.3 code and a class B source for the vanilla part) and records one requirement that Shiroz gave on 2026-10-05 (decision 32, section 36.4). Dates come from the container clock (UTC). Evidence classes as in the sources table (A official or source code, B credible secondary, C community or excerpt).

### 36.0 What was read, and a method mistake

- **Code (S132):** shallow clone of branch `26.3`, head `939fb13d3b96664d6b5057e128bb05395417063b` (2026-09-30, subject "Fix MDK including wrong resources. Fixes #10892"), the same head as in passes 19 to 21. Read in part: the patches of `Entity`, `EntityType`, `SpawnPlacements`, `SynchedEntityData`, `EntityDataSerializers`, `MobCategory`, `Commands`, `ArgumentTypeInfos` and `MinecraftServer`; the packages `event/entity`, `entity`, `common/command`, `server/command` and `server/permission`; and `ClientCommandHandler`, `TickEvent`, `LogicalSidedProvider`, `ForgeInternalHandler`, `ForgeMod`, `GameData` and parts of `ForgeHooks`.
- **Class B per row (decision 30, delegated by Shiroz):** entities use the NeoForged entity pages (S134); commands use the Fabric command guides (S136), because the NeoForged sidebar of the 26.1 line shows no commands group (its last group was shown collapsed to one entry, so this is "none seen", not "none exists") while Fabric has a three-page guide; the scheduler uses the NeoForged Sides page (S135) and Fabric's Yarn Javadoc excerpts (S139).
- **Mistake and the lesson:** a first word search limited to `src/main` made `SpawnPlacementRegisterEvent` look as if nothing fired it. It is fired from the `SpawnPlacements` patch (`fireSpawnPlacementEvent`), which `GameData` calls. In this tree a search must cover `patches/` as well as `src/`; "not found in src" says nothing about vanilla classes that Forge patches.

### 36.1 Row 17: entities

**Docs (S133).** The Forge docs of the 1.21.x line have one entity page, "Synchronizing Entities", read in full. *Spawn Data:* the spawning of modded entities is handled separately by Forge, so extending a vanilla entity class may not inherit all its behavior; extra data for the spawn packet goes through `IEntityAdditionalSpawnData` (`#writeSpawnData`, `#readSpawnData`, for data the client needs once and that does not change). *Dynamic Data:* an `EntityDataAccessor<T>` is a `static final` field made with `SynchedEntityData#defineId(entityClass, serializer)`, given a start value by overriding `Entity#defineSynchedData` and calling `this.entityData.define(...)`, and read and written through `entityData`; defining parameters on entity classes one does not own can desynchronize the ids and crash. The same page in the 1.12.x line also names a second interface, `IThrowableEntity` (source entity id and start velocity in the spawn packet); no class of that name exists in the 26.3 `src` or `patches` (word search).

**Docs against code and class B.** (1) The 1.21.x page describes `define` on `entityData`; NeoForged's pages for 1.20.5 and later describe `SynchedEntityData.Builder#define` (search extracts, S134), so the Forge page looks out of date for 1.21. It is not settled from source, because the vanilla `Entity` class cannot be read here; the only `defineSynchedData` in the Forge tree is an old test class in `src/test_old` with the old no-parameter form. Recorded as "docs disagree between loaders, the newer form is probably right" (decision 27). (2) The docs say modded entity spawning is "handled separately by Forge"; in the 26.3 code the Forge spawn packet is used only for entities that implement `IEntityAdditionalSpawnData` (below).

**Code at 26.3 (S132).**
- **Entity base.** `Entity` extends `CapabilityProvider.Entities` and implements `IForgeEntity` (385 lines: NBT helpers, `canUpdate`, `captureDrops`, `getPersistentData`, rider and trample hooks, `getClassification(boolean)`, `isAddedToLevel`/`onAddedToLevel`/`onRemovedFromLevel`, `revive`, multipart entities, about twenty fluid-type methods, `hasCustomOutlineRendering`). The constructor calls `ForgeEventFactory.onEntityConstructing` and `gatherCapabilities()`.
- **Three extra save keys.** Every entity saved by Forge 26.3 writes `CanUpdate` (boolean, default true), `ForgeCaps` (capability data) and `ForgeData` (the `getPersistentData()` compound, written only when not null); loading reads them back. This is in the patch of `Entity#addAdditionalSaveData`/`readAdditionalSaveData` through `ValueOutput`/`ValueInput`.
- **Spawn packet.** `Entity#getAddEntityPacket` returns the vanilla `ClientboundAddEntityPacket` unless the entity was an instance of `IEntityAdditionalSpawnData` at construction (`hasExtraSpawnData`); then `ForgeHooks.getEntitySpawnPacket` sends Forge's own `SpawnEntity` packet on the Forge play channel. `SpawnEntity` writes the entity type id (a varint from `BuiltInRegistries.ENTITY_TYPE`, with a TODO about codecs), entity id, uuid, three doubles, three angle bytes, three velocity shorts and a length-prefixed byte block from `writeSpawnData`. On the client, `handle` calls `EntityType#customClientSpawn`, sets position, angles, id and uuid, adds the entity to the `ClientLevel` and then calls `readSpawnData`; the `lerpMotion` line is commented out, so the velocity fields of the packet are not applied (a reading of the code, not tested). A source comment says the packet should be rewritten into something simpler. `IEntityAdditionalSpawnData` uses `FriendlyByteBuf` (NeoForged's counterpart uses `RegistryFriendlyByteBuf`, S134).
- **Entity type builder.** The `EntityType` patch adds `setUpdateInterval`, `setTrackingRange`, `setShouldReceiveVelocityUpdates` and `setCustomClientFactory` to the builder, plus `customClientSpawn` (default: `create(world, EntitySpawnReason.LOAD)`) and `getTags()`. The velocity flag feeds `hasUpdateInterval()`, whose vanilla default is "update interval is not `Integer.MAX_VALUE`".
- **Part entities.** `PartEntity<T extends Entity>` takes its parent in the constructor, reuses the parent's type and level, and its `getAddEntityPacket` throws `UnsupportedOperationException`.
- **Mob category.** `MobCategory` implements `IExtensibleEnum` (a `create(...)` factory that throws "Enum not extended" until the enum extension runs, a name map and a codec that accepts added values).
- **Data serializers.** The registry `forge:entity_data_serializers` (`ForgeRegistries.ENTITY_DATA_SERIALIZERS`) is built with ids from 256 ("vanilla space" below it) up to the largest varint, `disableSaving()` and `disableOverrides()` (so a repeated name throws, see 5.4), and is not marked `disableSync()`. `EntityDataSerializers#getSerializer` and `#getSerializedId` look in the vanilla table first and in the Forge registry second (null while the registry does not exist yet); the vanilla `registerSerializer` throws once its own id reaches 256.
- **`defineId` warning.** The patch makes `SynchedEntityData#defineId` log at WARN whenever it is called from a class other than the entity class (vanilla only logged at debug level); the stack trace is added only when debug logging is on. This matches the docs warning.
- **Attributes.** `EntityAttributeCreationEvent#put` throws `IllegalStateException` ("Duplicate DefaultAttributes entry") when `DefaultAttributes.hasSupplier` is already true for the type. `ForgeHooks.modifyAttributes()` fires the creation event, then `EntityAttributeModificationEvent`, and merges the results with `combine`. `ForgeMod` registers two attributes, `swim_speed` (default 1, range 0 to 1024) and `nametag_distance` (default 64, range 0 to 64), both `setSyncable(true)`, and adds both to every living type in the modification event.
- **When the events fire.** `GameData` fires the attribute events, `SpawnPlacements.fireSpawnPlacementEvent()` and the creative-tab sort **only if every `RegisterEvent` succeeded**; on any failure it rolls back to VANILLA and throws, and none of these events is fired.
- **Spawn placements.** `SpawnPlacementRegisterEvent#register` takes a predicate with `Operation` `AND`, `OR` (default) or `REPLACE`; a type without a vanilla entry needs a non-null placement type and heightmap (otherwise `NullPointerException`), and non-null values with another operation than `REPLACE` throw `IllegalStateException`. After the event, the merged predicates replace the vanilla map entries, and the vanilla result of each check goes through `ForgeEventFactory.checkSpawnPlacements`.
- **Join and leave.** `EntityJoinLevelEvent` (cancellable; `onEntityJoinLevel` returns a boolean) and `EntityLeaveLevelEvent` exist; `ForgeInternalHandler` listens to the join event to replace an `ItemEntity` whose item has a custom entity: it discards the old entity, schedules `new TickTask(0, ...)` on the work queue of the logical side and cancels the join (see 36.3).

**Class B: NeoForged 26.1 pages (S134), where they differ from Forge 26.3.** Spawn data uses `IEntityWithComplexSpawn` (with `RegistryFriendlyByteBuf`) and `IEntityExtension#sendPairingData(ServerPlayer, Consumer<CustomPacketPayload>)`, which Forge 26.3 does not have; entity data uses attachments (`AttachmentHolder`) where Forge has capabilities; the spawn placement event is `RegisterSpawnPlacementsEvent` (same three operations, same two-null rule); the entity data serializer map "becomes a registry" in NeoForge too; two extra attribute classes (`PercentageAttribute`, `BooleanAttribute`) and three `neoforge:` attributes exist, with `nametag_distance` at default 32 and range 0 to 32 (Forge: 64); custom attributes do not block a vanilla client, which receives only the `minecraft` attributes (for Forge 26.3 no equivalent filter was looked for). `MobCategory` is an extensible enum in both. **Damage hooks are different APIs, not renamed ones:** NeoForged documents `EntityInvulnerabilityCheckEvent`, `LivingIncomingDamageEvent`, `LivingShieldBlockEvent`, `ArmorHurtEvent` and `LivingDamageEvent.Pre`/`Post` around a `DamageContainer`; Forge 26.3 has `LivingAttackEvent`, `LivingHurtEvent`, `LivingDamageEvent` and `ShieldBlockEvent` among its living events (names only; their bodies were not read).

**Events by name (not read).** The tree has 70 entity event classes: 12 in `event/entity` (attribute creation and modification, `EntityEvent`, join, leave, mob griefing, mount, struck by lightning, teleport, travel to dimension, projectile impact, spawn placement register), 3 in `item`, 30 in `living` and 25 in `player`. `EntityEvent` has the nested `EntityConstructing` and `EnteringSection`.

### 36.2 Row 19: commands

**Docs.** The Forge docs have no commands page (the full navigation of the 1.21.x line was read; section 19 said the same). The NeoForged 26.1 sidebar shows none in the groups seen (36.0). The only docs for the vanilla part are Fabric's three guides (S136, 26.2 line, read in full): `Command` returns an int, `CommandSourceStack` is the source type; commands are registered in `CommandRegistrationCallback` with the triple (dispatcher, `CommandBuildContext`, `Commands.CommandSelection`); `requires` takes a predicate on the source, shown with `source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)`; Brigadier redirects work only on nodes with arguments; client commands use `ClientCommandRegistrationCallback` and `ClientCommands`; custom argument types are registered with `ArgumentTypeRegistry` on **both** server and client or the command does not work; commands can be added at run time but the command tree must then be sent again with `Commands.sendCommands(ServerPlayer)`; suggestion providers return a `CompletableFuture<Suggestions>`.

**Code at 26.3 (S132).**
- **Registration.** `RegisterCommandsEvent` is a record `(getDispatcher, getCommandSelection, getBuildContext)` (Fabric's triple in another order). The `Commands` patch fires it at the end of the constructor through `ForgeEventFactory.onCommandRegister`, after the vanilla commands; the same patch changes the condition for the dev-only commands from `IS_RUNNING_IN_IDE` to `ForgeGameTestHooks.isGametestEnabled()`. Forge registers its own `/forge` (`tps`, `track`, `entity`, `generate`, `dimensions`, `mods`, `tags`) and `/config` from `ForgeInternalHandler` on this event.
- **Execution hook.** `CommandEvent` (cancellable) is posted from `Commands#performCommand` after parsing and before the command chain is built; a listener may replace the parse results or set an exception that is thrown instead. Server side only.
- **Tree sent to players.** The patch of `sendCommands` replaces vanilla `fillUsableCommands` with `CommandHelper.mergeCommandNode` (the comment cites issue 7551, redirects) and then runs `CommandHelper.filterCommandList`: for a connection whose type is `VANILLA` it removes every argument node whose argument type is not registered under the namespace `minecraft` or `brigadier`, together with the nodes below it (`CommandTreeCleaner`). The code comment says a vanilla client would fail to read the packet and disconnect otherwise.
- **Argument types.** `ArgumentTypeInfos.registerByClass(Class, info)` is added by a patch and does a plain `put` into the by-class map, so a second registration for the same argument class replaces the first without a message. `ForgeMod` registers `enum` (`EnumArgument`) and `modid` (`ModIdArgument`) in the registry `COMMAND_ARGUMENT_TYPE` through a `DeferredRegister` and `registerByClass`.
- **Entity selectors.** `EntitySelectorManager.register(token, type)` stores into a static `HashMap`; it rejects an empty token, the vanilla tokens `p`, `a`, `r`, `s`, `e`, and characters not allowed in an unquoted string, and **does not check for a repeated token** (the new one replaces the old one). `parseSelector` returns null for an unknown token so vanilla parsing continues; `fillSelectorSuggestions` adds `@token` with a tooltip. The call sites in the vanilla parser were not read.
- **Client commands.** `ClientCommandHandler` fires `RegisterClientCommandsEvent` (dispatcher plus build context) into a separate dispatcher, copies it so redirects cannot cross, and merges it into the server tree only for suggestions; if the server never sends a command packet it builds a fallback tree at login. The client source is a `ClientCommandSourceStack` with the local player's `permissions()`.
- **Permission API.** `PermissionAPI.getPermission(ServerPlayer, node, context...)` and `getOfflinePermission(UUID, ...)` throw `UnregisteredPermissionException` when the node is not registered; nodes are registered and handler factories added through `PermissionGatherEvent.Nodes` and `.Handler` (the default handler has its own identifier; adding a handler under an existing identifier throws); `initializePermissionAPI` only accepts a call from `ServerLifecycleHooks` (checked with a stack walk). A `PermissionNode<T>` has an id, a `PermissionType<T>`, a default resolver and optional dynamic context keys. How `requires` uses it was not read.

### 36.3 Row 20: scheduler

**Docs.** None: the Forge docs and the NeoForged sidebar have no scheduling page. **Code at 26.3 (S132):** Forge adds no scheduler class. A word search for "schedul" over `src/main` finds only the brain `Activity` schedule, a network call (`processor.scheduleIfPossible`), the `executor.schedule` call in `ForgeInternalHandler` and a comment in `IForgeBlockEntity`.
- **Tick events.** `TickEvent` is a sealed interface with five groups, each with `Pre` and `Post` records on EventBus 7: `ServerTickEvent` (haveTime supplier, server), `ClientTickEvent` (a singleton instance), `LevelTickEvent` (side, level, haveTime supplier), `PlayerTickEvent` (player, side) and `RenderTickEvent` (`DeltaTracker`).
- **Where they fire.** In the `MinecraftServer` patch the server pre event is posted before the tick counter increases and the children are ticked, and the post event at the very end of the tick; the level events surround `level.tick(haveTime)` inside the loop over `getWorldArray()` (which replaces `getAllLevels()`), and the patch also records a per-level tick time. The `Minecraft` patch fires the level events for the client level with a `haveTime` that is always true.
- **History (S140, class C mirror pages, search extracts).** Up to 1.20.x `TickEvent` was one class with `type`, `side` and `phase`; from 1.20.6 `LevelTickEvent` has `Pre` and `Post` subclasses and the old constructor and the phase are deprecated for removal; at 26.3 the whole family is records.
- **Work queue.** `LogicalSidedProvider.WORKQUEUE` is a `BlockableEventLoop<? super TickTask>`: the `Minecraft` object on the client and the `MinecraftServer` on the server. `CustomPayloadEvent.Context#enqueueWork` runs the task at once on the right thread or calls `submitAsync` otherwise; `ForgeInternalHandler` uses `executor.schedule(new TickTask(0, ...))`. This is the only delayed-work mechanism Forge itself uses.
- **Vanilla block ticks.** The `LiquidBlock` patch still calls `level.scheduleTick(pos, fluid, delay)`, so the vanilla scheduled-tick API keeps that name at 26.3. Class B for the vanilla part is excerpt-level only: Fabric's Yarn Javadoc for 1.20.2 and a 24w20a build says scheduled ticks are added with `WorldAccess.scheduleBlockTick(BlockPos, Block, int)` and are called on the server, and that random ticks are a separate mechanism controlled by a game rule and limited to a cylinder around players (S139, Yarn names, not 26.x). The NeoForged Sides page (S135, read in full) gives the logical-versus-physical side rules and `Level#isClientSide()` but says nothing about scheduling. A Forge Forums post (S141, class C, not counted) says the same about random versus scheduled ticks.
- **Row 20 stays PARTIAL:** the Forge side is complete, but the vanilla side has only 1.20.x excerpts and no 26.x class B page.

### 36.4 Mojang's official names (decision 32)

Shiroz's requirement of 2026-10-05: a mod that uses Mojang's official mappings must be loadable too. What the sources say:
- **Release of the mappings (S137, S138, S142).** Mojang says it released obfuscation mappings in 2019 (S137). The version JSON gained `client_mappings` and `server_mappings` in 19w36a and they were also put into 1.14.4 (S138, class C); mapping tools state the same two starting points and one of them is archived because Mojang now ships unobfuscated jars (S142, class C). So official names exist for 1.14.4 and later only; for earlier versions only community names exist (SRG, MCP, Yarn).
- **End of obfuscation (S137, class A).** Announced on 2025-10-29: the first snapshot after the full Mounts of Mayhem launch is the first version without obfuscation; the jars keep all original names, the version JSON no longer lists mappings, and each jar gets a LICENSE file pointing to the EULA, which is unchanged. Which release is the first unobfuscated one is **not stated** in the sources read; that it is `26.1` is an inference from the Forge access transformers (official names from `26.1`, `research.md` section 29) and from the 26.x docs below.
- **Names in the 26.x docs and code.** The Fabric 26.2 command guides use `CommandSourceStack`, `Commands` and `CommandBuildContext`; the NeoForged 26.1 pages use `Identifier`, `ValueInput`, `ValueOutput`, `EntityDataAccessor` and `SynchedEntityData.Builder`; the Forge 26.3 patches use `Identifier`, `ValueInput` and `ValueOutput`. All three describe official names, and (inference) on an unobfuscated runtime these are also the runtime names, so a mod that uses them needs no translation there.
- **Obfuscated eras.** For 1.14.4 up to the last obfuscated release the runtime classes carry obfuscated names, and official names exist only as a mapping file. The Forge access transformers use MCP names in `1.16.x`, SRG names from `1.18.x` to `1.21.11` (`research.md` section 29), but which names the mod jars carry at run time is still undecided (open since pass 20; Shiroz does not know either, 2026-10-04).
- **Consequence (design, not a source statement).** VESSEL must be able to tell which name space a module's bytecode uses and, where that is not the runtime's, translate it. The open points are in 36.7 and in section 5 of the status file.

### 36.5 What this means for VESSEL

Evidenced:
- **Saved entities carry Forge keys.** `CanUpdate`, `ForgeCaps` and `ForgeData` appear in every entity saved by Forge 26.3 (36.1); reading such saves should keep them as opaque data (design consequence).
- **Forge's entity data serializers share the id space with vanilla.** Mod serializers start at id 256 and are not saved to disk; the registry throws on a repeated name.
- **Mod argument types never reach vanilla clients.** Any command branch below a mod argument type is cut from the tree sent to a vanilla connection (36.2). VESSEL's own command tree for vanilla and Bedrock-side players needs the same rule (design).
- **Two key spaces overwrite silently** and are now registry keys for VESSEL (decision 31): entity selector tokens and the argument-type class map (identity file, section 5.5).
- **Forge has no scheduler API;** ported mods use tick events, the logical-side work queue and vanilla scheduled ticks.

Inference (marked, not tested): a vanilla client cannot read Forge's `SpawnEntity` packet, so VESSEL must send the vanilla add-entity packet and drop the extra spawn data for such clients; on Folia (phase 10) there is no single server thread, so `ServerTickEvent` and the server work queue cannot be mapped one to one; and on 26.x, official names need no translation.

### 36.6 Effect on the row count

Rows 17 and 19 move from NOT STARTED to DONE and row 20 to PARTIAL (all three under decision 28; row 19 counts its docs part as a finding "no page exists", like row 28). The count goes from 21 DONE, 8 PARTIAL, 8 NOT STARTED to 23 DONE, 9 PARTIAL, 5 NOT STARTED: (23 + 0.5 x 9) / 37 = 74.3%, reported as about 74%.

### 36.7 Still missing

**[Update 2026-10-08 (v0.4.55): the point "how Forge 26.3 keeps non-vanilla attributes away from vanilla clients" is answered in 40.2 (a filter on the attribute-update packet for vanilla connections).]** **[Update 2026-10-05 (v0.4.52): the mapping points of this list are answered in part by 37.3 (first unobfuscated release confirmed as 26.1 by a class B source; decision 33 sets the source of translation data); the rest stays open.]**

For entities: the bodies of the 55 living and player event classes and of `MobSpawnEvent`, `EntityTeleportEvent`, `EntityMountEvent`; how Forge 26.3 keeps non-vanilla attributes away from vanilla clients; the vanilla `Entity` class (not readable here) to settle the `defineSynchedData` form. For commands: the call sites of the selector hook, the `/forge` subcommand bodies, how `requires` uses the permission API, a NeoForged commands page if one exists. For the scheduler: vanilla `ServerLevel#scheduleTick`, `LevelTicks` and the `MinecraftServer` task methods at 26.x, and any 26.x class B page on scheduling. For mappings (decision 32): which name space the mod jars of each loader and era carry at run time (needs jars); how VESSEL detects the name space of a module; where the translation data comes from for obfuscated eras and under what licence (the licence text of the mapping files was not read); whether the requirement covers all loaders or only Forge in phase 7.

---

## 37. Worldgen, data generation and the source of official-name data (pass 23, v0.4.52, 2026-10-05)

Pass 23 takes rows 25 (worldgen) and 26 (data generation) of the 37-row list under decision 28 (the docs, the Forge 26.3 code and a class B source for the vanilla part), and follows up decision 32 with the question Shiroz delegated on 2026-10-05 (decision 33, section 37.3). Dates come from the container clock (UTC). Evidence classes as in the sources table (A official or source code, B credible secondary, C community or excerpt).

### 37.0 What was read, and how

- **Docs from their repositories (S143, S144, S145).** This pass read the markdown source of the docs instead of the rendered pages: `MinecraftForge/Documentation` branch `1.21.x` (head `ac05e12aae903703cb8a9908a153623ce476a509`, 2025-08-16), `neoforged/documentation` branch `main` (head `89528c36e`, 2026-10-01; `docs/` is its current line) and `FabricMC/fabric-docs` (head `f17bf08e`, 2026-09-30). It is faster, has no navigation noise and allows a full-text search of a whole docs tree. The Forge docs line is the same 1.21.x line as the docs map of section 19, but its last commit is about a year older than the 26.3 code, so every difference in 37.1 and 37.2 is "docs older than code" until a 26.x docs line exists.
- **Code (S132).** The same 26.3 clone as pass 22 (head `939fb13d3b96664d6b5057e128bb05395417063b`). Read: `common/world/*` (12 files: `BiomeModifier`, `ModifiableBiomeInfo`, `StructureModifier` and the stock types of `ForgeBiomeModifiers` read in part, `ModifiableStructureInfo` only its apply loop and builder head), `registries/holdersets/*` (names and the type interface), `ServerLifecycleHooks#runModifiers`, `ForgeRegistries`, `ForgeMod` (serializer registration and `gatherData`), the patches of `Biome`, `Structure`, `Registries` and 12 further worldgen classes; for data generation `GatherDataEvent`, `DatagenModLoader`, the patches of both `Main` classes and of `DataGenerator`, `RegistryDataBuilder`, `ExistingFileHelper` (constructor), `LanguageProvider`, `SoundDefinitionsProvider` (method list), `GlobalLootModifierProvider` (method list), `JsonCodecProvider` and about 20 patch files of the datagen providers.
- **Class B per row (decision 30).** Row 25: NeoForged "Biome Modifiers", the only page of its worldgen group. Row 26: the "Data Generation" section of the NeoForged "Resources" page.
- **Word searches over `src` and `patches`** this time (the lesson of 36.0).

### 37.1 Row 25: worldgen

**Docs (S143).** A full-text search of the Forge docs tree finds no page on worldgen. The terms `BiomeModifier`, "biome modifier", `biome_modifier` and `structure_modifier` appear nowhere in it; "biome" or "worldgen" appear only in passing in six pages (particles, registries, two tag pages, tinting, the tag datagen page), and the datapack-registry datagen page uses a configured feature and a placed feature as its example. For Forge the docs part of row 25 is therefore a finding, "no page exists" (like rows 19 and 28).

**Code at 26.3 (S132).**
- **Biome hook.** The `Biome` patch makes the four settings fields (climate, generation settings, attributes, special effects) non-final, adds a `ModifiableBiomeInfo` and a private `update` callback; `getGenerationSettings()` and the other getters read the modified info.
- **When modifiers run.** `ServerLifecycleHooks#handleServerAboutToStart` loads the server configs, calls `runModifiers(server)` and only then posts `ServerAboutToStartEvent`. `runModifiers` lists the biome modifiers and the structure modifiers from the registry access (order = load order of the datapack entries), applies the biome modifiers to every biome, calls `refreshFeaturesPerStep()` on the generator of every level stem, and then applies the structure modifiers to every structure.
- **How they run.** `ModifiableBiomeInfo#applyBiomeModifiers` throws `IllegalStateException` if called twice for a biome; otherwise it takes a builder copy of the original info and, for each `Phase` in enum order (`BEFORE_EVERYTHING`, `ADD`, `REMOVE`, `MODIFY`, `AFTER_EVERYTHING`), calls every modifier in list order. `StructureModifier` has the same five phases and the same shape; `ModifiableStructureInfo` is applied the same way.
- **Registries.** Three Forge registries, all created with `makeUnsavedAndUnsynced`: `forge:biome_modifier_serializers`, `forge:structure_modifier_serializers`, `forge:holder_set_type`. Two datapack registries, `forge:biome_modifier` and `forge:structure_modifier`, are registered by `ForgeMod` with their direct codecs. The `Registries` patch (method `registryDirPath`) makes the data directory of any non-`minecraft` registry `<namespace>/<path>`, so the files are at `data/<modid>/forge/biome_modifier/<name>.json` and `.../forge/structure_modifier/...` (derived from the patch and the registry id, not run).
- **Stock types.** Biome modifier serializers registered by `ForgeMod`: `none`, `add_features`, `remove_features`, `add_spawns`, `remove_spawns` (five). Structure modifier serializers: `none` only. `AddFeaturesBiomeModifier(biomes, features, step)` works in phase `ADD` and calls `generationSettings.addFeature(step, holder)`; `RemoveFeaturesBiomeModifier` works in `REMOVE` with a steps set that defaults to all steps; `AddSpawnsBiomeModifier(biomes, WeightedList<SpawnerData>)` works in `ADD`; `RemoveSpawnsBiomeModifier(biomes, entityTypes)` in `REMOVE`. **The Javadoc comments in `ForgeBiomeModifiers` give the type names `forge:removefeatures` and, for the spawn-removal modifier, `forge:add_spawns`; the registered names are `remove_features` and `remove_spawns`.** The `ForgeMod` registration is authoritative (decision 27).
- **Holder sets.** Four custom holder set types are registered in `forge:holder_set_type`: `any`, `and`, `or`, `not` (`AnyHolderSet`, `AndHolderSet`, `OrHolderSet`, `NotHolderSet`; `CompositeHolderSet` is the base of the middle two). `HolderSetType#makeCodec(registryKey, holderCodec, forceList)` builds the codec; `ICustomHolderSet#serializationType` defaults to `OBJECT`.
- **Structure side.** The `Structure` patch makes `settings` private and routes `biomes`, `spawnOverrides`, `step` and `terrainAdaptation` through the modified settings, while the structure's own codec reads the original settings (so saved and generated data is not the modified data). `PieceBeardifierModifier` is a Forge interface read by the patched `Beardifier`: a structure piece can give its own terrain adjustment, box and ground-level delta.
- **Smaller worldgen patches.** `WorldGenRegion` (an entity whose spawn was cancelled is not added), `WorldDimensions` (the dimensions map uses `LenientUnboundedMapCodec`), `StructureStart` and `StructurePiece` (clearer errors for an unregistered type), `StructureProcessor` (a new `processBlock` overload that takes the `StructureTemplate`; the old one is deprecated), `StructureTemplateManager` (adds `ForgeHooks.emptyStructureSource()`), `MonsterRoomFeature` (`DungeonHooks.getRandomDungeonMob`), `PhantomSpawner` (the player-spawn-phantoms event: denied skips, allowed bypasses the position and time checks), `TrunkPlacer` (`BlockState#onTreeGrow`), `AlterGroundDecorator` (an alter-ground event), `DebugLevelSource` (`initValidStates`).

**Class B: NeoForged "Biome Modifiers" (S144, lines 1 to 150 and 570 to the end read; the middle part was checked by headings and `type` names only).** Same contract: `BiomeModifier#modify(Holder<Biome>, Phase, builder)` with the same five phases, a statically registered `MapCodec`, a datapack-registered modifier. Differences from Forge 26.3: (1) names and paths use `neoforge:` and `data/<modid>/neoforge/biome_modifier/<path>.json`, Forge uses `forge:` and `.../forge/biome_modifier/...`; (2) NeoForge has nine stock types (`none`, `add_features`, `remove_features`, `add_spawns`, `remove_spawns`, `add_spawn_costs`, `remove_spawn_costs`, `add_carvers`, `remove_carvers`), Forge has five; (3) NeoForged documents a feature-cycle crash when two biomes list the same two features in a different order in one step, advises copying vanilla placed features under the mod's own namespace, and shows how to target biomes that may not exist through an optional tag entry (`required: false`); (4) NeoForged has no page on structure modifiers (the worldgen group has one page), so the structure side has no class B text. A datapack can override a mod's modifier file by using the same path (NeoForged page); that is a pack override, not a registry-key collision.

### 37.2 Row 26: data generation

**Docs (S143).** All ten pages of the Forge 1.21.x docs on datagen were read in full: the introduction, model providers, language, sounds, recipes, loot tables, tags, advancements, global loot modifiers and datapack registry objects. Summary of the contract they describe: a `GatherDataEvent` on the mod bus; `event.getGenerator().addProvider(boolean, factory)`; four modes (`--client`, `--server`, `--dev`, `--reports`; `--all` for all); `ExistingFileHelper` with `--existing <folder>` and `--existing-mod <modid>` (by default only the vanilla pack is known); providers named `LanguageProvider`, `SoundDefinitionsProvider`, `ModelProvider` and its item and block subclasses, `BlockStateProvider`, `GlobalLootModifierProvider`, `DatapackBuiltinEntriesProvider` (with a `RegistrySetBuilder`), `LootTableProvider`, `RecipeProvider`, `TagsProvider`, `AdvancementProvider`/`ForgeAdvancementProvider`. Details worth keeping: sound names always take the mod id as namespace and are not validated; recipe results need a `RecipeCategory` or a `NullPointerException` follows; shaped recipes have five validation rules; tags have `add`, `addOptional`, `addTag`, `addOptionalTag`, `replace`, `remove`; variant block states throw when a `BlockState` is not covered.

**Docs against the 26.3 tree (S132).** The docs name classes that are **not in `src/main` or `patches/` at 26.3**: the whole package `net.minecraftforge.client.model.generators` (`ModelProvider`, `ItemModelProvider`, `BlockModelProvider`, `BlockStateProvider`, `ModelBuilder`, `ConfiguredModel`, the custom loader builders `ObjModelBuilder`, `CompositeModelBuilder`, `DynamicFluidContainerModelBuilder`, `CustomLoaderBuilder`), `DatapackBuiltinEntriesProvider`, `ForgeAdvancementProvider`, and a Forge `BlockTagsProvider` (the 26.3 names are `ForgeBlockTagsProvider` and seven siblings). They survive only in `src/test_old` (old tests, not built) and in the stale Javadoc of `DeferredRegisterData`. Method shapes also differ: the docs use `ResourceLocation`, `FinishedRecipe` consumers and `LootTableSubProvider#generate(BiConsumer)`; the 26.3 code uses `Identifier`, `RecipeOutput`-based vanilla providers and `GlobalLootModifierProvider#start(HolderLookup.Provider)` with a constructor that takes a `CompletableFuture<HolderLookup.Provider>`. Recorded as "docs older than code" (decision 27); the code is authoritative for 26.3.

**Code at 26.3 (S132).**
- **Event and generator.** `GatherDataEvent` is a final class on the mod bus with `getGenerator`, `getExistingFileHelper`, `getLookupProvider`, `getInputs`, `getModContainer` and `includeServer/Client/Dev/Reports`. Its `DataGeneratorConfig` holds the mod set, the output path, inputs, the lookup provider, the four flags and `flat`; `makeGenerator(Path, shouldExecute)` makes a cached generator (the overload that takes a path function is deprecated for removal since 26.3); `runAll()` groups generators by output folder, adds the providers of the later ones to the first of each group and runs that one. **`GatherDataEvent` is created in only one place, `DatagenModLoader#run`** (word search for `new GatherDataEvent(`; `ForgeMod` only adds a listener); nothing posts it in a normal game or server run.
- **Command line.** `DatagenModLoader` adds `--gameDir` (required, default "."), `--existing`, `--existing-mod`, `--mod` (comma separated; each value is also a regular expression matched against the loaded mod ids, never `forge` or `minecraft`), `--flat`, `--assetIndex` and `--assetsDir`; `--client` exists only when set up by the client `Main`. Both vanilla `Main` classes are patched: the server one calls `loader.run(..., server, allOptions, dev, reports)` and the client one `loader.run(..., allOptions, client, allOptions, allOptions)`, so each side generates the other side only with `--all`. When `loader.run` returns false the vanilla `Main` returns without its own run; a value `--mod minecraft` alone returns true and lets vanilla run.
- **Run.** `run` sets `runningDataGen`, bootstraps (and bootstraps the client when generating client data), loads the mods, builds the lookup provider from `VanillaRegistries::createWorldLookup` and `createReloadableLookup`, adds `forge` as an existing mod unless data for `forge` is being generated, builds the `ExistingFileHelper`, then posts one `GatherDataEvent` to each loaded mod with its own generator (output folder `<output>/<modid>` unless flat or a single mod, or the project path from the `forge.project.list` property and a `.project_info.properties` file in the mod); only the requested mods execute. If the mod loading state is invalid it logs a refusal and generates nothing.
- **Duplicate provider names.** The `DataGenerator` patch adds `addProvider(boolean run, factory or provider)`, which **throws `IllegalStateException("Duplicate provider: <name>")`** when a provider with the same `getName()` was added before (so provider names are a key inside one generator), and adds the provider to the run list only when `run` is true; it also adds `getPackOutput()` overloads, `getProvidersView()` and a startup message "Generating: <id>".
- **Datapack entries.** `RegistryDataBuilder` replaces the docs' `DatapackBuiltinEntriesProvider`: `of()`, `name`, `modid`, `filter`, `layer`/`world`/`reloadable` (each takes a `RegistrySetBuilder` or a consumer of one) and `worldGenerator`/`reloadableGenerator(PackOutput)`, plus the lookups `worldFull()`/`reloadableFull()` that include earlier layers. The patched `RegistriesDatapackGenerator` takes an entry predicate (only entries whose key passes the filter are dumped; default: all), has `getRegistryProvider()` and uses `RegistryDataLoader.getWorldAndDimensionRegistries()`; the patched `VanillaRegistries` exposes `builder()`, `worldBuilder()` and `reloadableBuilder()` copies; `BootstrapContextAccess` gets a default `registryLookup`.
- **Providers in `common/data`.** `LanguageProvider` (writes `assets/<modid>/lang/<locale>.json`; typed `add` for block, item, item stack, enchantment, **biome**, mob effect, entity type and a plain string), `SoundDefinitionsProvider` and `SoundDefinition`, `VanillaSoundDefinitionsProvider`, `ParticleDescriptionProvider`, `GlobalLootModifierProvider`, `JsonCodecProvider<T>` (a codec, a `DynamicOps`, a pack type and a directory; it registers each id with the `ExistingFileHelper`), `ExistingFileHelper` (stacks the vanilla server pack, optional vanilla client assets from `--assetIndex` and `--assetsDir`, existing packs and existing mods; `exists`, `trackGenerated`, `getResource`, `getResourceStack`), `RegistryDataBuilder`, eight tag providers (`ForgeBlockTagsProvider`, `ForgeBlockItemTagsProvider`, `ForgeItemTagsProvider`, `ForgeEntityTypeTagsProvider`, `ForgeFluidTagsProvider`, `ForgeEnchantmentTagsProvider`, `ForgeBiomeTagsProvider`, `ForgeStructureTagsProvider`) and `ForgeRecipeProvider` (final; `create()` returns a multi-registry bootstrap). Client-only: `ForgeAtlasProvider`, added by `ForgeMod#gatherData` only when the dist is the client.
- **What Forge generates for itself.** `ForgeMod#gatherData` adds the pack metadata (`pack.mcmeta`) for the server pack, seven tag providers (block, item, entity type, fluid, enchantment, biome, structure), a datapack layer named `forge` (a predicate `TOOL_CAN_SHEAR` that replaces the vanilla "match tool: shears" test, and the recipes of `ForgeRecipeProvider`), the vanilla sound definitions and the atlas provider.
- **Vanilla hooks added by patches.** `ModelProvider` (client data models: overridable `getKnownBlocks`, `getKnownItems`, `getBlockModelGenerators`, `getItemModelGenerators`; the defaults list only `minecraft` blocks and items), `LootTableProvider` (`getTables`, `getRequired`), `BlockLootSubProvider#getKnownBlocks`, `EntityLootSubProvider#getKnownEntityTypes`, `RecipeProvider` (the `output` fields are no longer final), `TagsProvider` (a `modId`, an optional `ExistingFileHelper` and two resource types used to check that referenced tags and elements exist), `TagAppender` (extends `IForgeTagAppender`, with `getInternalBuilder` and `getSourceName`). `ConditionalRecipe` and `IConditionBuilder` exist in `common/crafting` (not read in this pass).

**Class B: NeoForged "Resources", section "Data Generation" (S144, read in full).** The datagen run is a "Data" run configuration; the event is `GatherDataEvent.Client` or `GatherDataEvent.Server` (the server one may carry only datapack-entry providers), with helpers `createProvider`, `createDatapackRegistryObjects` (which also makes later lookups include the generated entries), `createBlockAndItemTags`, `getResourceManager(PackType)`, `getBuiltinDatapack` and `includeDev/includeReports`. Provider list of 16 kinds, including `EquipmentAssetProvider`, `RecipePrioritiesProvider`, `DataMapProvider`, `SpriteSourceProvider` and `PackMetadataGenerator`, which the Forge docs do not have; command line `--mod`, `--output`, `--existing`, `--existing-mod`, `--includeDev`, `--includeReports`, `--all` (Forge: `--server`, `--client`, `--dev`, `--reports`, `--all`, `--flat`, `--input` and the three asset and game options). The two loaders therefore have **different datagen APIs, not renamed ones**.

### 37.3 Decision 32 follow-up: where the translation data comes from (decision 33)

Shiroz delegated the choice on 2026-10-05 and asked that the data preferably come from Mojang, that more sources be consulted and that mapping data taken from jars is acceptable.

**What the sources say.**
- **Fabric docs (S145, class B).** Minecraft was obfuscated from release to 1.21.11; 26.1 is the first release without obfuscation and needs no obfuscation maps (it includes parameter names). Fabric stopped maintaining third-party mappings (Yarn) from that version, so a Fabric mod for 26.1 or later has to use Mojang's names. For obfuscated releases a Fabric mod is remapped to Intermediary when built for distribution (the Loom pages). Mojang's mappings, released in 2019, have no parameter names or Javadoc; Parchment is layered on top of them for that. This **confirms the inference of 36.4**: 26.1 is the first unobfuscated release (class B), and 1.21.11 the last obfuscated one.
- **MCPConfig (S146, class A; head `751a4f362ef15ec19d3616416e18853fee68968e`, 2026-09-18).** The `config.json` of 26.3 says `official: true`, `obfuscated: false` and the 26.3 folder has no mapping file; the files for 1.21.11 and 1.20.1 also say `official: true`, and the 1.21.11 folder has a `joined.tsrg` (the obfuscated-to-SRG pairs). Forge's own tooling therefore takes Mojang's names for the official layer and a separate SRG file for obfuscated releases.
- **Licence notice (S147, class C).** Forge Gradle prints a warning, quoted in many posts and logs, that the official mappings fall under their own licence: they may be copied and used for development purposes but not redistributed complete and unmodified; they are provided as they are; use of the game code is governed by the Minecraft EULA. The reference copy named in that warning (`Mojang.md` on the master branch of MCPConfig) is gone: its raw address returned 404 on 2026-10-05 and the repository has no file containing that wording. So the current exact text is **not verified**; the paraphrase above rests on log excerpts only.

**Decision 33 (delegated; reasoning above and below).** VESSEL gets the data for Mojang's official names in this order:
1. **Unobfuscated versions (26.1 and later):** no translation; official names are the run-time names (Fabric docs, MCPConfig `obfuscated: false`).
2. **Obfuscated versions (1.14.4 to 1.21.11):** Mojang's own mapping file for that exact game version, downloaded from the address Mojang lists in that version's JSON (`client_mappings` and `server_mappings`, S138), on first need, kept in VESSEL's cache folder and **never put inside the VESSEL package**. Reason: downloading directly from Mojang is the source Shiroz asked for, and it avoids redistributing the file, which the licence notice does not allow in complete and unmodified form.
3. **Modules that use another name space on obfuscated versions** (SRG, Intermediary, MCP, Yarn) need that name space's data as a second input; the obfuscated names are the common key between the two files (design inference). Known sources: `joined.tsrg` per version in MCPConfig for SRG (S146); Fabric's Intermediary data (not read).
4. **Data found in jars VESSEL already holds** (loader jars, mod jars, for example mixin reference maps) may be used to fill gaps and to detect a module's name space, but never overrides Mojang's file; a disagreement is a conflict under decision 27, reported, not resolved silently.
5. **Offline servers:** the owner may put the file into a folder VESSEL names; VESSEL checks that it belongs to the game version before use. Without the data, a module that needs translation is **not loaded, with a clear message**; it is never skipped silently and never loaded with wrong names. This is the one case in which decision 32 cannot be met, and it is written down as a limit.

**Open:** how VESSEL detects the name space of a module; the hash and size fields of the version JSON entries (S138 names the fields only); the file format and licence of Intermediary data; Quilt's own mappings (not researched); Forge's and NeoForge's run-time names on obfuscated versions (open since pass 20, still undecided: the files above show the build inputs, not the names inside shipped jars).

### 37.4 What this means for VESSEL

Evidenced:
- **Datagen never runs at server run time.** `GatherDataEvent` is posted only by `DatagenModLoader` (37.2). A server-side loader does not need to run data generators; only their output (JSON files in the jar) matters.
- **Provider names are keys inside one generator** (`Duplicate provider`); it is a dev-time rule and does not change the registry-key rules of decision 8 and 17.
- **Biome and structure modifiers are datapack data** with a serializer registry: the stock types are plain JSON, a custom type needs its own codec. Forge and NeoForge use different type names, folders and numbers of stock types (5 against 9), so a cross-loader conversion needs a name map; Forge's structure modifier has only `none`.
- **Modifiers run once per biome, after configs and before the about-to-start event,** with feature lists rebuilt afterwards.

Inference (marked, not tested): a mod's `GatherDataEvent` listeners can be registered but never called at run time, so they may be ignored as long as their classes do not fail to load (for example client-only providers); and a cross-loader biome modifier file could be converted by renaming `type` and moving the folder for the stock types, which needs a check against the NeoForge side that was only partly read.

### 37.5 Effect on the row count

Rows 25 and 26 move from NOT STARTED to DONE under decision 28 (row 25 counts its docs part as a finding "no page exists", like rows 19 and 28). The count goes from 23 DONE, 9 PARTIAL, 5 NOT STARTED to 25 DONE, 9 PARTIAL, 3 NOT STARTED (rows 30, 31, 34): (25 + 0.5 x 9) / 37 = 79.7%, reported as about 80%. No row uses the "documented limit" label.

### 37.6 Still missing

For worldgen: the bodies of `MobSpawnSettingsBuilder`, `ClimateSettingsBuilder`, `BiomeSpecialEffectsBuilder` and `StructureSettingsBuilder`; the middle part of the NeoForged page (spawn costs, carvers); how NeoForge handles structure modifiers; the other patches of `common/world` neighbors (`WorldWorkerManager`, flat-level preset tags). For data generation: the bodies of the eight tag providers (the tag lists are in the tags-list inventory of earlier passes), `ForgeRecipeProvider#create`, `ConditionalRecipe` and `IConditionBuilder`, `ParticleDescriptionProvider`, the NeoForged per-topic datagen pages (models, loot, recipes), the Fabric data generation pages (a third class B source), and which `Main` the Gradle `runData` task starts at 26.3. For decision 33: see its "Open" line.

---

## 38. ModLoader compatibility, the E2 launch flow and threading per era (pass 24, v0.4.53, 2026-10-06)

Pass 24 takes rows 30 (E2 launch flow, `ITweaker`, `@SidedProxy`, `FMLInterModComms`), 31 (the E1a ModLoader-compatibility layer) and 34 (threading rules per era) of the 37-row list. Dates come from the container clock (UTC). Evidence classes as in the sources table (A official or source code, B credible secondary, C community or excerpt). **No class B source applies to these rows:** the features exist only in Forge, FML and LaunchWrapper (decision 28 asks for class B only for the gameplay API rows 17 to 26). The sources are the FML repository (S148), the old Forge tags (S149), LaunchWrapper (S150), six Forge branches (S151) and the Forge 1.12.x docs (S152).

### 38.0 What was read

- **FML (S148):** shallow clones of branches `1.4`, `1.5.1`, `1.6.4`, `1.7.10` and `1.8`; the 28 tags `v2.0` to `v8.0` fetched without file contents and read by path (blobless): tree listings, dates and the files that name the Minecraft version.
- **Old Forge tags (S149):** shallow clones of the tags `1.0.0`, `1.2.5`, `1.3.2`; tags `1.0.0` to `1.3.2` and the numeric tags `23.5` to `66.0` fetched blobless for dates and `conf/version.cfg`.
- **LaunchWrapper (S150):** `Mojang/LegacyLauncher` master (head `a4801b70f8a0148c6e6279ec2e91527e8019e1c8`, 2016-12-20): `Launch`, `ITweaker`, the file list.
- **Forge branches (S151), shallow:** `1.7.10` (`9274e4fe435cb415099a8216c1b42235f185443e`, 2017-06-07), `1.8.9` (`d06e0ad71b8471923cc809dde58251de8299a143`, 2017-06-07), `1.12.x` (`3effde4f1fc9d14d6ed1dbf6bebc39c2b18780e1`, 2025-12-03), `1.16.x` (`b3426008d3c3a256fcfaf9d780682c45ab1d1fdb`, 2025-08-11), `1.20.1` (`0ec923d7307eb15bef70d6916329642b733a21ed`, 2026-10-02), `1.21.1` (`a11d1936c53f1f93787f239ff42cb8e93a1d037a`, 2026-07-21); plus the 26.3 clone of passes 22 and 23.
- **Docs (S152):** `MinecraftForge/Documentation` branch `1.12.x` (head `b080c8753d7ba6f69b159de57181fe0f4a1cb124`, 2024-01-23): "Sides" and "Loading Stages" read in full; word searches for IMC, tweaker, LaunchWrapper, thread over the whole tree; the thread lines of the networking page.

### 38.1 Row 31: the ModLoader compatibility layer (E1a and the start of E1b)

**The source conflict of section 2 is not a conflict.** Section 2 recorded two statements: the FTB wiki says FML stayed compatible with ModLoader mods until 1.6, the FML readme text says support was dropped as of 1.7 (S20). The FML code shows both are true of the same fact:
- FML `1.6.4` has the package `cpw.mods.fml.common.modloader` (24 files with `modloader` in their path, counting the `ModLoader` shim classes); `1.5.1` has 23 and `1.4` has 24; `ModContainerFactory` of 1.6.4 builds a `ModLoaderModContainer` for every class that is a `BaseMod` and whose name matches the pattern `mod_...`.
- FML `1.7.10` and `1.8` have **no** such files; the 1.7.10 `ModContainerFactory` still recognises a `BaseMod` class but logs two severe lines saying the mod will not be loaded and will be ignored because the ModLoader mechanisms are no longer available.
So support lasted through the 1.6 line (last version 1.6.4) and ended at 1.7. By decision 27 the primary source settles it; the two secondary texts only used different wording.

**The FML tags show the compatibility layer from the very first FML release, and that release was for Minecraft 1.2.5.** `fmlbuild.mcversion` is `1.2.5` in `v2.0` (2012-05-10) and in the build script of `v2.9` (2012-06-28); `v3.0` (2012-08-11) has `conf/version.cfg` with client and server `1.3.1`; `mc_versions.cfg` names `1.4.5`, `1.4.6`, `1.4.7` for `v4.5` to `v4.7`, `s13w02b` for `v4.99`, `1.5`, `1.5.1`, `1.5.2` for `v5.0` to `v5.2`, and `1.6` to `1.6.4` for `v6.0` to `v6.4` (`v6.3` = 1.6.3, `v6.4` = 1.6.4). The tags `v2.1`, `v2.2`, `v3.1`, `v4.0` to `v4.4`, `v6.99`, `v7.2`, `v7.9`, `v7.10` and `v8.0` were dated but their Minecraft version was not read. `v2.0` already contains `Loader`, the `@Mod` annotation and two files in `common/modloader/` (`ModLoaderHelper`, `ModLoaderModContainer`); the count of files under `common/modloader/` grows to 4 at `v2.2`, 5 at `v2.9`, 11 at `v3.0` and 18 at `v4.0`. **So an FML with `@Mod` and a ModLoader layer exists from Minecraft 1.2.5, not from 1.3.2** (section 2 says the first FML evidence begins at 1.3.2, S20).

**What Forge itself was in its first versions (S149).** The Forge tags `1.0.0` to `1.3.2` are Forge's own version numbers (2011-07-31 to 2012-01-29; section 35.5). Their `conf/version.cfg` gives the Minecraft versions: the tags `1.2.2`, `1.2.3`, `1.2.3@183`, `1.2.4`, `1.2.5` and `1.2.5@213` target client `1.0.0` and server `1.0.1` (MCP 5.0); the tags `1.3.0`, `1.3.1` and `1.3.2` target Minecraft `1.1` (MCP 5.6); the tags `1.0.0`, `1.0.1`, `1.0.3`, `1.0.4` and `1.2.1` have no such file. The numeric tags `23.5` to `66.0` are the modern Forge versions (dates 2018-11-12 to 2026-09-18), not early versions. These early tags are **a patch set over the MCP sources, installed on jars that already contain ModLoader and ModLoaderMP**: the install readme tells the user to use freshly downloaded jars that include solely ModLoader and ModLoaderMP, the build has a `modLoaderMP.patch`, the Forge code calls `ModLoader.getMinecraftInstance()` and `ModLoader.getLogger()`, and the patch of `SlotCrafting` keeps the existing `ModLoader.TakenFromCrafting` call and adds `ForgeHooks.onTakenFromCrafting` next to it; a patch also fixes a short-read bug in `Packet230ModLoader`. Forge starts through a static initializer (`ForgeHooks` prints "MinecraftForge V1.2.5 Initialized" in tag 1.2.5), and the tags contain no `BaseMod` class of their own. **So in these tags Forge does not make ModLoader mods run; ModLoader does, and Forge is an API and a set of hooks on top of it.** The section 2 wording "a compatibility-preserving layer over ModLoader mods" fits FML (from 1.2.5), not these early Forge tags. Forge builds for Minecraft 1.2.1 to 1.2.4 are in none of the tags or branches read.

**How the layer works (FML 1.6.4, S148).**
- **Discovery.** A class is a ModLoader mod if it extends `BaseMod` (also through a subclass seen earlier in the same jar) and its name matches `mod_...`; a class that matches the name but is not yet known to be a `BaseMod` is remembered and reconsidered; a `BaseMod` with another name is remembered as a base type; a class in the package `net.minecraft.src` outside the Minecraft jar gets a loud warning to move it.
- **Identity.** The mod id is the simple class name (for example `mod_Foo`); sorting rules come from the mod's `getPriorities` string.
- **Configuration.** `<configDir>/<modId>.cfg` is read as a properties file; static fields marked `@MLProp` (found by bytecode scan, class `net.minecraft.src.MLProp` or `MLProp`) become typed properties; unsupported types throw `IllegalArgumentException`; a non-static field is skipped with an info line; the file is written only if some property needs it, and an old file with no properties is renamed to a backup.
- **Lifecycle mapping.** `constructMod`: loads the class, configures properties, registers the mod as a network mod (or injects a dummy network handler for a plain `BaseMod`), instantiates it with the public no-argument constructor and injects `@SidedProxy` fields; `preInit`: registers game and GUI tick handlers (client side), a world generator, a fuel handler, a crafting handler, a pickup handler, a chat listener and a connection handler built from the mod; `init`: calls `load()`; `postInit`: calls `modsLoaded()`; load complete: `ModLoaderHelper.finishModLoading`; server starting: registers the commands the mod collected.
- **Surface.** `BaseModProxy` has 21 methods (`modsLoaded`, `load`, `getName`, `getPriorities`, `getVersion`, `doTickInGUI`, `doTickInGame`, `generateSurface`, `generateNether`, `addFuel`, `takenFromCrafting`, `takenFromFurnace`, `onClientLogout`, `onClientLogin`, `serverDisconnect`, `serverConnect`, `receiveCustomPacket`, `clientChat`, `onItemPickup`, `serverCustomPayload`, `serverChat`); `ModLoaderHelper` has helpers for ticks, GUI, entity trackers, village trades, commands, world generation, fuel, crafting, pickup, chat and connections. FML ships its own `ModLoader`, `BaseMod` and `MLProp` classes in the client, server and Bukkit source sets (the client `ModLoader` has 979 lines and 80 public static methods, the server one 71).
- **Era table consequence (proposal, not decided).** The evidence suggests that E1b (FML as its own loader with `@Mod`) starts at Minecraft 1.2.5 and E1a (Forge on top of a real ModLoader) covers only the early Forge versions for Minecraft 1.0.0 and 1.1. The era table of section 2 is VESSEL's own grouping and is left unchanged until Shiroz decides; the proposal does not change what must be built (one adapter for ModLoader-based mods, one for FML), only where the line between the two eras falls.

### 38.2 Row 30: the E2 launch flow, `ITweaker`, `@SidedProxy` and `FMLInterModComms`

**LaunchWrapper (S150).** `Launch.main` builds a `LaunchClassLoader` from the class path URLs, then parses `--version`, `--gameDir`, `--assetsDir` and `--tweakClass` (default `VanillaTweaker`; other options are kept as non-options). It puts three entries on a shared map, `Launch.blackboard`: `TweakClasses` (the names still to load), `ArgumentList` and `Tweaks` (the instances). A loop then repeats while there are names left: each new name is skipped with a warning if already visited, its package is excluded from the class loader's transformation, it is instantiated through the launch class loader, and the first one becomes the **primary tweaker**; every instantiated tweaker is then asked `acceptOptions(args, gameDir, assetsDir, profile)` and `injectIntoClassLoader(classLoader)` and moves to the final list. A tweaker may push more names onto `TweakClasses`, which is how tweakers cascade. When no names are left, every tweaker's `getLaunchArguments()` is appended to the argument list, and the primary tweaker's `getLaunchTarget()` names the class whose `main` is invoked by reflection; any exception is logged as "Unable to launch" and ends the process with exit code 1. `ITweaker` has exactly those four methods. The library also holds `IClassTransformer`, `IClassNameTransformer`, `LogWrapper`, three vanilla tweakers and their injectors; the inside of `LaunchClassLoader` was not read.

**FML's tweakers (S151, 1.12.x).** `FMLTweaker` (client): its constructor sets the IPv4-stack property if unset and installs `FMLSecurityManager` (a failure aborts the start); `acceptOptions` turns the arguments into a map on the blackboard key `launchArgs`, keeps stand-alone arguments, defaults `--version`, `--gameDir` and `--assetsDir`, logs in through `Yggdrasil` and stores a copy as `forgeLaunchArgs`; `injectIntoClassLoader` calls `FMLLaunchHandler.configureForClientLaunch` and `appendCoreMods`; the launch target is `net.minecraft.client.main.Main` after the arguments `--modListFile` and `--mods` are removed; `injectCascadingTweak` appends a name to `TweakClasses`. `FMLServerTweaker` has the target `net.minecraft.server.MinecraftServer` and calls `configureForServerLaunch`. `FMLLaunchHandler`'s constructor excludes from class loading the packages `net.minecraftforge.fml.relauncher.`, `net.minecraftforge.classloading.`, Apache commons, http and maven, Guava, ASM and LZMA, and from transformation three FML packages; setup then builds `FMLInjectionData`, redirects standard output and error into the log, sets up the library manager and calls `CoreModManager.handleLaunch`. `handleLaunch` detects a deobfuscated environment by trying to load the bytes of `net.minecraft.world.World`, cascades `FMLInjectionAndSortingTweaker`, registers `PatchingTransformer`, loads the two root plugins (`FMLCorePlugin`, `FMLForgePlugin`), reads `fml.coreMods.load` and scans the mods folder. In a scanned jar the manifest key `TweakClass` (with an optional integer `TweakOrder`) pushes a cascading tweak and the jar is not treated as a mod jar; `ModType` (default `FML`) other than FML makes FML skip the jar; `FMLCorePlugin` marks a coremod; `FMLCorePluginContainsFMLMod` makes FML warn that mods should be in a separate jar. `FMLInjectionAndSortingTweaker` adds itself at index 0 of `Tweaks` plus a wrapper per coremod, sorts the list in its `acceptOptions` (itself first, coremod wrappers by their sort index; the rule for other tweakers was cut off in the read) and adds `TerminalTweaker`. `FMLDeobfTweaker` registers the transformers (`DeobfuscationTransformer`, the coremod access transformers, `ModAccessTransformer`, and four patching transformers for item stacks, item blocks, special item blocks and potion effects) and then loads `Loader` and calls `injectData` and `instance()` as a check of the Minecraft version; it throws if used as a primary tweaker. `ServerLaunchWrapper` checks that the LaunchWrapper and ASM classes exist (else it prints a message and exits with 1) and sets the server log configuration; the rest of it was not read. History: FML `1.5.1` has no `ITweaker` class; `1.6.4` has four (sorting, FML client, deobfuscation and the coremod wrapper); `1.7.10` and `1.8` have five (adding `TerminalTweaker`).

**`@SidedProxy` (S151, S152).** `ProxyInjector.inject(mod, asmData, side, languageAdapter)` runs in the mod's construction step (in `FMLModContainer` for `@Mod` mods and in `ModLoaderModContainer` for ModLoader mods). For each field with the annotation: the owner is the annotation's `modId`, or the one `@Mod` class in the same data, otherwise a big warning and the field is skipped; fields owned by another mod are skipped; the proxy class is `clientSide` or `serverSide` according to the **physical** side, and if that string is empty the code uses the nested class `<holder>$ClientProxy` or `<holder>$ServerProxy`; for the Java adapter the field must be static and its type must accept the proxy (otherwise a `LoaderException`). The 1.12.x docs say the same about the side (single-player still gets the client proxy) and add that the proxy classes keep side-only code out of common classes; the default nested class names are in the code only.

**`FMLInterModComms` (S151, S152).** `sendMessage(modId, key, value)` takes an NBT compound, an item stack, a resource location or a string, and `sendFunctionMessage` takes a class name that the receiver loads as a `Function`; `sendRuntimeMessage` and `sendRuntimeFunctionMessage` are for after start-up; `fetchRuntimeMessages(mod)` removes and returns the pending messages of that mod. A startup message is stored only if a mod is active at that moment and the target mod is loaded (otherwise `false`), and the return value is `true` only while the post-initialization state has not been reached. `Loader#initializeMods` distributes the initialization state, moves to post-initialization, distributes `IMCEvent` (so every receiver sees its messages **before** its post-init handler), then post-initialization. An `IMCMessage` has a key, the sender container and typed getters. The docs name IMC only in two lines of "Loading Stages" (send in init, process in `IMCEvent`); there is no IMC page.

### 38.3 Row 34: threading per era

**Packets (S148, S151, S152).**
- **E1b (FML 1.7.10):** `SimpleChannelHandlerWrapper#channelRead0` calls the handler's `onMessage` directly and writes the reply in the same call, so the handler runs on the channel's thread (the network thread); the class has no hand-off.
- **E2 (1.8.9, 1.12.x):** the same code; since 1.8 `FMLCommonHandler#getWorldThread(INetHandler)` returns an `IThreadListener`. The 1.12.x docs say packets are handled on the network thread by default since 1.8 and show `IThreadListener#addScheduledTask`, with the listener taken from `Minecraft` (client) or `WorldServer` (server).
- **E3 (1.16.x), E4a (1.20.1), E4b (1.21.1):** `Context#enqueueWork(Runnable)` (in `NetworkEvent` up to 1.20.1, in `CustomPayloadEvent` at 1.21.1) returns a `CompletableFuture<Void>`, takes the executor from `LogicalSidedProvider.WORKQUEUE` for the receiving side, calls `submitAsync` if the thread differs and otherwise runs the work at once; a code comment says Minecraft sometimes delays tasks even on the right thread. The 1.21.x docs still say packets are handled on the network thread.
- **E5 (26.3):** the same hand-off (sections 34 and 36.3).

**Capabilities.**
- **E1b:** the capability classes were not looked for in 1.7.10 (the entity-data mechanism there is in the next block).
- **E2:** `Capability`, `CapabilityManager`, `CapabilityDispatcher`, `CapabilityInject` and two interfaces already exist in 1.8.9; `injectCapabilities(ASMDataTable)` is called from `Loader` (1.8.9 and 1.12.x); in 1.12.x `ForgeEventFactory.gatherCapabilities` is called from the patches of `TileEntity`, `Chunk`, `World`, `ItemStack` and `Village` (the `Entity` patch was not listed); the package has no `synchronized`, lock, concurrent map or volatile (word search: 0).
- **E3 (1.16.x):** `CapabilityProvider` appears; `injectCapabilities(List<ModFileScanData>)` is called from `ModLoader`; `CapabilityManager` synchronizes on its provider map and has a volatile callbacks map; `LazyOptional` resolves its value with double-checked locking.
- **E4a, E4b (1.20.1, 1.21.1):** `RegisterCapabilitiesEvent` (a mod-bus event) is created inside `CapabilityManager.injectCapabilities`, which runs in the loading state `INJECT_CAPABILITIES` (`ForgeStatesProvider`); `CapabilityManager` synchronizes on the provider map and on each capability when registering; `Capability#addListener` is `synchronized`.
- **E5 (26.3):** **no `RegisterCapabilitiesEvent` exists anywhere in the tree** (word search of sources, patches, tests and Markdown); capabilities are registered only by scanning the annotation `@AutoRegisterCapability`; `injectCapabilities()` takes no argument; the state `INJECT_CAPABILITIES` comes after `CREATE_REGISTRIES` and before `UNFREEZE_DATA`; the same three synchronized points and the same locking in `LazyOptional`.
- **Result:** in every era with capabilities, the registration side is guarded by locks, and nothing in the capability package guards the capability **instances** (providers, dispatcher, storage). Neither the 1.12.x docs nor the 1.21.x docs have a statement about threads on their capability or data-storage pages (word search for "thread").

**Entity data.**
- **E1b (1.7.10):** `IExtendedEntityProperties` objects live in a plain `HashMap` field of `Entity`; the `EntityConstructing` event is posted from the constructor and each property is then initialised; the properties are saved and loaded in loops over the map; no locks. **`registerExtendedProperties` renames a repeated identifier by appending a counter** (`name1`, `name2`, ...), logs at info level and returns the identifier actually used; a null identifier or null properties are refused with a warning and the empty string is returned.
- **E2:** 1.8.9 has `IExtendedEntityProperties` and capabilities; 1.12.x no longer has the interface (although its docs still have a page for it); the 1.12.x patch of `EntityDataManager` makes `defineId` log a warning when it is called for a class that is not the entity's own (the same change as at 26.3, section 36.1).
- **E3 to E5:** synched entity data and Forge's data serializers are described for 26.3 in section 36.1; the per-era patches of the synched-data class for 1.16.x to 1.21.1 were not read. The vanilla classes (`DataWatcher`, `EntityDataManager`, `SynchedEntityData`) cannot be read here, so any lock inside them is unknown.

### 38.4 What this means for VESSEL

Evidenced:
- **ModLoader mods are discoverable by rule:** a `BaseMod` subclass named `mod_...`; the id is the simple class name; the config file is `<modId>.cfg`; the lifecycle maps to construct, `load()` at init and `modsLoaded()` at post-init. Support exists in FML 1.2.5 to 1.6.4 and in none of 1.7 and later (38.1).
- **Forge tags `1.0.0` to `1.3.2` need a real ModLoader underneath;** they are not a loader.
- **An E2 mod jar is never started through LaunchWrapper by VESSEL:** the tweaker chain is a start-up mechanism whose effects are class transformations (coremods, access transformers) and a few arguments (design consequence; the transformations themselves are in section 12.4).
- **The packet handler of E1b and E2 mods runs on the network thread** (38.3).

Inference (marked, not tested): on a server loader a `@SidedProxy` field must always receive the `serverSide` class (or `$ServerProxy`), and VESSEL's adapter must move E2 packet handlers that were written for `addScheduledTask` onto the server thread; the same adapter would have to guard E1b handlers that assume nothing about threads. The proposal to move the E1a/E1b line to Minecraft 1.2.5 waits for Shiroz.

### 38.5 Effect on the row count

Row 30 moves from NOT STARTED to DONE (docs, code of 1.12.x and LaunchWrapper; no class B source applies). Row 34 moves from NOT STARTED to DONE with the finding that no thread rule is documented for capabilities and entity data and that the code facts per era are those of 38.3. Row 31 moves from NOT STARTED to PARTIAL: the source conflict is resolved and the discovery, identity, configuration and lifecycle rules are read, but the bodies of the helper classes and of the `ModLoader` shim are not. The count goes from 25 DONE, 9 PARTIAL, 3 NOT STARTED to 27 DONE, 10 PARTIAL, 0 NOT STARTED: (27 + 0.5 x 10) / 37 = 86.5%, reported as about 86%. No row uses the "documented limit" label.

### 38.6 Still missing

**[Update 2026-10-08 (v0.4.55): the helper bodies and the shim of row 31 are read in 40.3; Forge for Minecraft 1.2.1 to 1.2.4 is answered by the history in 40.4.]**

For row 31: the bodies of `ModLoaderHelper`, `BaseModTicker`, the network, GUI, village-trade, fuel, crafting, pickup, chat and connection helpers; the `ModLoader` shim method by method (80 and 71 static methods); the property file format of `ModLoader` itself as shipped by Risugami (no source read); Forge for Minecraft 1.2.1 to 1.2.4; the Minecraft version of the tags listed in 38.1 as unread. For row 30: the inside of `LaunchClassLoader`, the rest of `ServerLaunchWrapper`, `FMLInjectionData`, `Yggdrasil`, the sort rule for non-FML tweakers, and the same flow in `1.8.9` to `1.11.x` compared with `1.12.x`. For row 34: the per-era synched-data patches, the lock inside the vanilla data classes, the `Entity` patch of 1.12.x for `gatherCapabilities`, the dispatch thread inside `SimpleChannel` (E3 to E4b) and `MessageContext` (E2).

---

## 39. Rendering hooks, recipes, loot tables, the scheduler and items: leftovers (pass 25, v0.4.54, 2026-10-07)

Pass 25 takes the PARTIAL rows 16 (items), 20 (scheduler), 21 (rendering hooks), 22 (recipes) and 23 (loot tables) of the 37-row list, and records decision 34 (the E1a/E1b line, answered by Shiroz on 2026-10-06). Dates come from the container clock (UTC). Evidence classes as in the sources table (A official or source code, B credible secondary, C community or excerpt).

### 39.0 What was read

- **Forge docs history (S153).** A blob-less clone of `MinecraftForge/Documentation` with all 15 branches and 502 commits, used only to ask which commits ever touched a path; plus the 1.21.x docs tree of pass 23 (head `ac05e12aae903703cb8a9908a153623ce476a509`) read in full for items, recipes (custom, non-datapack) and loot tables.
- **Minecraft Wiki (S154, class C).** "Items model definition", read in full (revision `oldid=3779697`, last edited 2026-09-16; its navigation names 26.3 as the latest Java release).
- **Forge branches by raw file (S155).** `ConditionalRecipe.java` on the branches `1.20.1`, `1.20.4`, `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.8`, `1.21.10`, `1.21.11` (and `26.1`, `26.2` for its registration), and the `conditional` registration in `ForgeMod.java` on the branches from `1.21.1` to `26.2`; there is no `1.21.6` branch.
- **Third-party excerpts (S156, class C, not counted).** `mappings.dev` pages for `ItemModelUtils` (only the three builder kinds) and a CraftEngine wiki page that repeats the range-dispatch property list.
- **Code (S132, S151):** the 26.3 tree of passes 22 and 23. **Class B:** NeoForged docs (S144), pages named per row below.

### 39.1 Row 21: rendering hooks (DONE, "documented limit")

**The one missing docs page never existed.** The navigation file of the Forge docs lists "Model Extensions: Introduction" at `rendering/modelextensions/index.md`, and that file has never been written: the path was touched by **no commit in any of the 15 branches (502 commits)**; the navigation line and the other four pages of the folder came in one commit (`23ba528`, 2023-05-15, "Document Forge extensions of the model JSON format (#493)"); the file's raw address answers 404 on the branches `1.21.x`, `1.20.x`, `1.20.1`, `1.19.x`, `1.19.2`, `1.18.x`, `1.17.x` and `1.16.x`. This is the proof required by decision 30 (b) (dead link, no source anywhere): row 21 counts as DONE with the label **documented limit**.

**Custom geometry loader API at 26.3 (S132).**
- `IGeometryLoader` has one method, `read(JsonObject, JsonDeserializationContext)`, which returns a vanilla `UnbakedGeometry`.
- `GeometryLoaderManager` holds an immutable map `Identifier -> IGeometryLoader`, built once by posting `ModelEvent.RegisterGeometryLoaders` (called from a patch of `ModelManager`). `register(id, loader)` **throws `IllegalArgumentException` ("Geometry loader already registered") for a repeated id**, the same hard-stop shape as decisions 8 and 17.
- A model JSON selects a loader with the key `loader`; no key means the default loader; an unknown name throws a `JsonParseException` that lists the registered loaders (`ForgeHooksClient`).
- Stock loaders registered by `ClientForgeMod`: `forge:empty`, `forge:obj`, `forge:fluid_container`. **There is no `composite` loader at 26.3**; NeoForged documents `neoforge:composite`, `neoforge:empty` and `neoforge:obj`, and two more kinds that Forge 26.3 was not looked up for, block state model loaders and block state definition loaders (S144).
- `ModelContext` (a record, internal) implements `IGeometryBakingContext` (`isGui3d`, `useBlockLight`, `useAmbientOcclusion`, `getTransforms`, `getRootTransform`, `getRenderTypeHint`, `getRenderTypeFastHint`, `isComponentVisible`, `getRenderType`, `getRenderTypeFast`); the Forge data (`ForgeBlockModelData`) is merged along the parent chain until it is complete. Named render types are a separate registration (for example `forge:item_unlit`).

**Vanilla item model definitions (S154, class C).** The files are `assets/<namespace>/items/<id>.json`, selected by the component `item_model`; added in 1.21.4. Model object types: `model`, `composite`, `condition`, `select`, `range_dispatch`, `empty`, `bundle/selected_item`, `special`. **Boolean properties (13):** `broken`, `bundle/has_selected_item`, `carried`, `component`, `damaged`, `extended_view`, `fishing_rod/cast`, `has_component`, `keybind_down`, `selected`, `using_item`, `view_entity`, `custom_model_data`. **Select properties (10):** `block_state`, `charge_type`, `component`, `context_dimension`, `context_entity_type`, `display_context`, `local_time`, `main_hand`, `trim_material`, `custom_model_data`. **Numeric properties (10):** `bundle/fullness`, `compass`, `cooldown`, `count`, `crossbow/pull`, `damage`, `time`, `use_cycle`, `use_duration`, `custom_model_data`. **Tint sources:** `constant`, `dye`, `firework`, `grass`, `potion`, `team`, `custom_model_data` (and `map_color`, removed in a 26.3 snapshot). **Special models:** `banner`, `bell`, `book`, `chest`, `conduit`, `copper_golem_statue`, `decorated_pot`, `end_cube`, `head`, `player_head`, `shield`, `shulker_box`, `trident` (`bed`, `standing_sign` and `hanging_sign` were removed in 26.2). Display contexts: `none`, `thirdperson_lefthand`, `thirdperson_righthand`, `firstperson_lefthand`, `firstperson_righthand`, `head`, `gui`, `ground`, `fixed`, `on_shelf`. Root fields: `hand_animation_on_swap`, `oversized_in_gui`, `swap_animation_scale`, `model`; every type may carry a `transformation` (added in 26.1). The NeoForged page tells the reader to look the property classes up in vanilla code rather than listing them; the wiki is the only complete list read, and it is a community source.

### 39.2 Row 22: recipes (DONE)

**Docs (S153).** The "Custom Recipes" page of the 1.21.x docs is written for the older API: a recipe is a `Recipe<Container>` (`matches`, `assemble`), a `RecipeType` and a `RecipeSerializer` with `fromJson`, `toNetwork` and `fromNetwork`; non-item logic goes through `RecipeManager#getAllRecipesFor`; recipes become a `FinishedRecipe` for datagen. The "Non-Datapack Recipes" page: brewing recipes by `BrewingRecipeRegistry#addRecipe` in `FMLCommonSetupEvent`, **inside `enqueueWork` because the method is not thread-safe**, or by an `IBrewingRecipe`; anvil results by `AnvilUpdateEvent` (output, level cost, material cost); loom patterns by registering a `BannerPattern` (in the tag `minecraft:no_item_required` to appear without an item).

**Docs against 26.3 (S132).** `AnvilUpdateEvent` exists; **`BrewingRecipeRegistry` and `IBrewingRecipe` do not exist in `src`, `patches` or the old tests** at 26.3; the recipe API shape is the vanilla one with `RecipeInput`, a `RecipeSerializer` built from a `MapCodec` and a `StreamCodec`, and `RecipeOutput` for datagen. Recorded as "docs older than code" (decision 27). NeoForged's page for 26.1 (S144) matches the code side: a `RecipeInput`, the recipe class, recipe book information (groups, book categories, search categories), placement info, slot displays, recipe display, `RecipeType`, `RecipeSerializer`, and a worked crafting mechanic; it says `ShapedRecipePattern#setCraftingSize` is not thread-safe and must be wrapped in `enqueueWork`.

**`forge:conditional` after 1.21.3 (S155).** The serializer is registered under the same path, `conditional`, in `ForgeMod` on **every** branch checked from 1.21.1 to 26.3, so the id never changed. The class changed shape: on `1.21.3` to `1.21.11` `SERIALZIER` (the spelling used in the code) is an anonymous `RecipeSerializer<Recipe<?>>` with its own `streamCodec()`, and an inner `Wrapper implements Recipe<CraftingInput>` whose `getType` throws; at 26.3 `ConditionalRecipe implements Recipe<RecipeInput>` and the serializer is `new RecipeSerializer<>(MapCodec, StreamCodec)`; its JSON carries the conditions plus a `recipe` object read with `Recipe.DIRECT_CODEC`, the **stream codec throws `UnsupportedOperationException`** (a conditional recipe is resolved when data loads and is never sent to a client), and the builder allows one main condition, one pending condition at a time and refuses a recipe added with no condition.

### 39.3 Row 23: loot tables (DONE)

**Docs (S153).** The latest 1.21.x page (read in full) describes `LootDataResolver#getLootTable`, `LootParams`, `LootContext`, the three roll methods, and the Forge additions: `LootTableLoadEvent` (cancelling loads an empty table; do not change drops there, use global loot modifiers), pool names (an unnamed pool is `custom#<hash code>`), `LootingLevelEvent`, optional killer-entity parameters for the `chest` and `fishing` parameter sets, multi-item smelting for the smelt function, and the conditions `forge:loot_table_id` and `forge:can_tool_perform_action`.

**Code at 26.3 (S132), docs against code.**
- `LootTableLoadEvent` is a cancellable event with `getName`, `getTable`, `setTable`. It is fired by `ForgeEventFactory#onLoadLootTable`, which returns null when a listener cancels and otherwise the table the listeners left; the only caller is `ForgeHooks#onJsonDataParsed(codec, id, value)`, a generic hook that the patches of `SimpleJsonResourceReloadListener` and `ResourceManagerRegistryLoadTask` call after any JSON element is parsed and which acts only when the codec is `LootTable.DIRECT_CODEC` (it also stores the table id in the table). The docs say a cancelled event loads an empty table; what the two patched callers do with the null result was not read.
- `LootPool` has the optional field `name` (not written back when it starts with `custom#`) and a Forge condition field, plus `CONDITIONAL_CODEC` used by `LootTable` for its `pools`; `LootTable` routes the non-raw roll methods through the global loot modifiers and marks the raw ones `@Deprecated`.
- `LootContext` carries `queriedLootTableId` and `getLootingModifier()` (which calls `ForgeHooks.getLootingLevel`); `EnchantedCountIncreaseFunction` and the random-chance-with-enchanted-bonus condition use it for the looting enchantment; `LootParams.Builder` has a copy constructor.
- The `chest` and `fishing` parameter sets get an optional `ATTACKING_ENTITY` (the docs say `KILLER_ENTITY`: renamed upstream).
- Conditions registered by `ForgeMod`: `loot_table_id` and `can_tool_perform_action`. **No patch of the smelt function exists** among the 10 loot patches, so the multi-item smelting claim of the docs was not confirmed at 26.3.
- Global loot modifiers: `LootModifierManager` reads the layered list `forge:loot_modifiers/global_loot_modifiers.json` (key `entries`, a `replace` flag clears earlier layers; modifiers not listed are disabled) and the modifier files from the folder `loot_modifiers`; a file that does not decode is logged as a warning and skipped. Other classes: `IGlobalLootModifier`, `LootModifier`, `LootTableIdCondition`, `LootingLevelEvent`.

**Class B: NeoForged loot pages (S144; headings, introductions and key terms read).** Same structure (entries, pools, number providers, parameters and parameter sets, context, rolling, datagen sub-providers); GLMs are stacking rather than last-load-wins, the file is `data/<namespace>/loot_modifiers/<path>.json`, the built-in modifier is `neoforge:add_table`, and the conditions are `neoforge:loot_table_id` and `neoforge:can_item_perform_ability` (Forge: `forge:can_tool_perform_action` with a tool action). The pages read do not mention a loot-table load event.

### 39.4 Row 20: the scheduler (DONE)

The missing class B text for 26.x exists in the NeoForged 26.1 blocks page (S144): scheduled ticks are created with `Level#scheduleTick(BlockPos, Block, int)` (the `int` is the delay) and delivered to `BlockBehaviour#tick`; random ticks need `BlockBehaviour.Properties#randomTicks()` and run for a number of random blocks per chunk per tick set by the game rule `randomTickSpeed` (default 3); weather ticking (`handlePrecipitation`) is server-only, only while it rains, with a 1 in 16 chance; `animateTick` is client-only and runs every frame. The NeoForged networking pages say `enqueueWork` returns a `CompletableFuture`, and that an exception not handled on that future is swallowed without notice. The Fabric docs have no page on ticking or scheduling (a word search of `develop/` found nothing). In the Forge 26.3 tree, patches of nine block classes call into scheduling or random ticking (`NetherWartBlock`, `SweetBerryBushBlock`, `StemBlock`, `GrowingPlantHeadBlock`, `LiquidBlock`, `CocoaBlock`, `BambooStalkBlock`, `VineBlock`, `SaplingBlock`). With the Forge side of 36.3 this completes the row; the inside of the vanilla tick lists (`LevelTicks`) is a vanilla class and cannot be read here.

### 39.5 Row 16: items (still PARTIAL, narrower)

**Docs (S153).** The 1.21.x docs have exactly two item pages (the navigation file lists only "Introduction" and "BlockEntityWithoutLevelRenderer"): item properties (`requiredFeatures`, `durability`, `stacksTo`, `setNoRepair`, `craftRemainder`), creative tabs through `BuildCreativeModeTabContentsEvent`, custom tabs, and the BEWLR through `IClientItemExtensions#getCustomRenderer` (a class that, per the rows already read, does not exist at 26.3). **There is no Forge docs page on item components or on use behavior**; that part of the row is a finding, not a missing read.

**Code at 26.3 (S132).** `IForgeItem` has 48 default hook methods (for example `onItemUseFirst`, `onStopUsing`, `canContinueUsing`, `onLeftClickEntity`, `getCraftingRemainder`, `damageItem`, `onDestroyed`, `inventoryTick`, `canPerformAction`, `initCapabilities`, `getBurnTime`) and `IForgeItemStack` forwards 35 of them; the item events are `ItemEvent`, `ItemExpireEvent` and `ItemTossEvent`. `ForgeMod` adds no data component of its own, but the sealed event `GatherComponentsEvent` with its subclass `Item` lets any mod add default components to an item: the patched `Item` composes `ForgeHooks#gatherItemComponents(item, builtInComponents)` into the item's built components, and a listener calls `register(componentType, value)` (the event also gives `getOriginalComponentMap` and `getOwner`).

**Class B (S144, headings and introductions only).** NeoForged 26.1 documents `ItemStackTemplate` and `ItemInstance` (not in the Forge 26.3 names read), data components (`DataComponentType`, component maps, default components on items), the `Consumable` component with `ItemUseAnimation` and `ConsumeEffect`, and a click pipeline (left click: feature flags, attack charge check, `InputEvent.InteractionKeyMappingTriggered`, then per target; right click: `Item#useOn` and `Item#use`).

**Why the row stays PARTIAL.** The bodies of those NeoForged pages (the use pipeline in full) and how Forge's hook methods are called from the patched `Item` and `ItemStack` at 26.3 (apart from the component hook above) were not read.

### 39.6 Decision 34 (the E1a/E1b line)

Shiroz agreed on 2026-10-06 to move the line between E1a and E1b from Minecraft 1.3.2 to **1.2.5**. The era table of section 2 now reads: **E1a = 1.1 to 1.2.4 (3 versions, Forge on top of a real ModLoader)** and **E1b = 1.2.5 to 1.7.10 (20 versions, FML with its own ModLoader-compatibility layer until 1.6.4)**; the total stays at 78 versions. Forge builds for Minecraft 1.2.5 were assumed to be FML-based because the first FML tags (`v2.0` to `v2.9`) are for 1.2.5. **[Update 2026-10-08 (v0.4.55): checked against the full Forge history and contradicted, see 40.4: the Forge conf update for 1.2.5 (2012-04-06) is ModLoader-based and older than FML 2.0 (2012-05-10); FML joined Forge only in July 2012, for 1.3.x. The line stays at 1.2.5 as decided; the E1b adapter must also load FML-2.x mods for 1.2.5.]**

### 39.7 Effect on the row count

Rows 20, 21, 22 and 23 move from PARTIAL to DONE; row 21 carries the label "documented limit" (the only such row). Row 16 stays PARTIAL. The count goes from 27 DONE, 10 PARTIAL, 0 NOT STARTED to 31 DONE (one with the label), 6 PARTIAL, 0 NOT STARTED: (31 + 0.5 x 6) / 37 = 91.9%, reported as about 92%.

### 39.8 Still missing

For row 16: the full NeoForged pages on items, data components, interactions and consumables; the calls of Forge's item hooks in the patched `Item` and `ItemStack`; the vanilla item component list. For row 21: the NeoForged block state model loaders and block state definition loaders against Forge 26.3; the vanilla property classes themselves (the wiki list is class C). For row 22: the NeoForged page bodies beyond the headings. For row 23: the bodies of the NeoForged loot pages; the vanilla loot code. For decision 34: a Forge build for Minecraft 1.2.5 to confirm that it is FML-based.

---

## 40. Loader mechanisms, networking at 26.3, the ModLoader layer, the early Forge history, build numbers and items (pass 26, v0.4.55, 2026-10-08)

Pass 26 takes the last six PARTIAL rows of the 37-row list: 2 (build numbers per version), 10 (networking by era), 16 (items), 29 (era-to-source fit), 31 (the ModLoader compatibility layer) and 32 (loader mechanisms). Dates come from the container clock (UTC). Evidence classes as in the sources table (A official or source code, B credible secondary, C community or excerpt). Two scope decisions that Claude made on its own authority are flagged in 40.5, 40.6 and in decision 35 of the status file; Shiroz can reverse them.

### 40.0 What was read

- **Forge 26.3 loader and network code (S132, S158):** the modules `fmlcore` (45 Java files), `fmlloader` (65), `javafmllanguage` (5) and `forge-transformers`, and the whole `network` package.
- **Forge branches by raw file (S159):** `CoreModFile.java` on `1.20.1`, `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.8`, `1.21.10`, `1.21.11`, `26.1`, `26.2`, `26.3`; `JarInJarDependencyLocator.java` on `1.20.1`, `1.21.1`, `1.21.5`, `1.21.10`, `1.21.11`, `26.1`, `26.2`, `26.3`.
- **FML 1.6.4 (S148):** the remaining bodies of the package `cpw.mods.fml.common.modloader`, and the public API of the shim classes `ModLoader` and `BaseMod`.
- **Forge repository history (S157):** a blob-less clone with all branches (**11,821 commits, 45 remote branches, the first commit dated 2011-07-12**), searched by path and by date.
- **Forge Maven metadata (S160):** `maven-metadata.xml` of `net.minecraftforge:forge`, fetched on 2026-10-08; the fetch returned the entries from `1.21-51.0.33` down to `1.10.2-12.18.3.2281` and was cut there.
- **NeoForged docs (S161, class B):** `items/interactions.md` (the left-click pipeline in full, the right-click pipeline in full, the result types) and `items/datacomponents.md` (headings and the introduction).

### 40.1 Row 32: loader mechanisms

- **`fml.toml` (`FMLConfig`).** Read by `ModDirTransformerDiscoverer`, i.e. before any mod discovery; the default file is copied from `/META-INF/defaultfmlconfig.toml` on first use. Fourteen options: `earlyWindowControl` (default true), `maxThreads` (-1), `versionCheck` (true), `defaultConfigPath` ("defaultconfigs"), `disableOptimizedDFU` (true), `earlyWindowProvider` ("fmlearlywindow"), `earlyWindowWidth` (854), `earlyWindowHeight` (480), `earlyWindowFBScale` (1), `earlyWindowMaximized` (false), `earlyWindowSkipGLVersions` (a list), `earlyWindowSquir` (false), `earlyWindowShowCPU` (false), `earlyWindowLogHelpMessage` (true). On a server only `maxThreads`, `versionCheck` and `defaultConfigPath` matter; the rest belong to the client start-up window.
- **Early discovery.** `ModDirTransformerDiscoverer` scans `mods/` (depth 1) for jars whose module descriptor `provides` `ITransformationService`, `IModLocator` or `IDependencyLocator`, and offers them as early services; it also records on the launch blackboard whether the argument list starts a Mixin run in the development environment (`isMixinEnabledInDev`). A mod jar can therefore still ship a ModLauncher transformation service or a locator.
- **Mixin.** `mods.toml` has **no `[[mixins]]` table** in the 26.3 loader (word search of `fmlcore`, `fmlloader`, `javafmllanguage`), and the older branches `1.16.x`, `1.20.1`, `1.21.1` have none either. The only Mixin-related code in the loader is in `JarInJarDependencyLocator`: it reads the jar manifest attributes for Mixin configurations and, if any loaded mod has them, adds a synthetic dependency on MixinExtras. **That synthetic dependency exists from the branch `1.21.10` on** (its two lines are absent on `1.20.1`, `1.21.1` and `1.21.5`).
- **JavaScript coremods.** E3 and E4 had them: `CoreModFile` (with the coremods library behind `ICoreModProvider`) is present on `1.16.x`, `1.20.1`, `1.21.1` and on every checked branch through `1.21.10` (HTTP 200), **absent from `1.21.11`, `26.1`, `26.2` and `26.3`** (404), and `coremods.json`, a JavaScript engine, `ICoreMod` and `IFMLLoadingPlugin` appear nowhere in the 26.3 tree. The replacement is Java: the module `forge-transformers` has a `MethodRedirector` (it turns a call of `finalizeSpawn` into a static call of `ForgeEventFactory.onFinalizeSpawn`, with the target list in the resource `coremods/finalize_spawn_targets.json`) and a `FieldToMethodTransformer`, both ModLauncher transformers built on the `coremods` API library.
- **Docs.** The Forge docs (1.21.x and 1.12.x lines) have **no page on coremods, Mixin or `fml.toml`**; the only hits are passing mentions in the contribution guidelines. For row 32 the docs part is a finding, as for rows 19 and 25.

### 40.2 Row 10: networking at 26.3

- **Docs (S153).** The 1.21.x docs have two network pages (an index and "SimpleImpl"); both describe the older `NetworkRegistry.newSimpleChannel(name, version supplier, predicates)` API with `SimpleChannel#registerMessage`; that API is **not** the one at 26.3 (docs older than code, decision 27).
- **Channel API at 26.3.** `ChannelBuilder.named(Identifier)` with `networkProtocolVersion(int)`, `optional`, `optionalServer`, `optionalClient`, `acceptedVersions(VersionTest)` and three kinds of channel (`simpleChannel`, `eventNetworkChannel`, `payloadChannel`); a simple channel is declared per phase (`login`, `configuration`, `play`), per direction (`serverbound`, `clientbound`, `bidirectional`) with `add(Class, StreamCodec, handler)`. `ConnectionType` (modded, vanilla and others) drives which rules apply; `NetworkRegistry` holds the channels and the accept rules.
- **Forge's own channels.** `forge:login` (login phase, one serverbound wrapper) and `forge:handshake` (configuration phase: `Acknowledge` serverbound; `ModVersions` and `ChannelVersions` both ways; `RegistryList`, `RegistryData`, `ConfigData` and `MismatchData` clientbound); the play-phase channel carries `SpawnEntity` and the container-open packet. In the source the play channel is the same object as the configuration channel, marked with a TODO.
- **Configuration-phase tasks.** `ForgeNetworkConfigurationHandler` queues, in this order, `RegisterChannelsTask` (sends `minecraft:register` for every known channel), `ModVersionsTask`, `ChannelVersionsTask`, `SyncRegistriesTask` and `SyncConfigTask`; `NetworkRegistry#validateChannels` builds a `NetworkMismatchData` (version mismatches and missing channels, both directions) and the clientbound packet `MismatchData` carries such a result (what the client does with it was not read).
- **Vanilla clients.** `NetworkFilters` registers **only** `forge:vanilla_filter`; its `VanillaConnectionNetworkFilter` rewrites the clientbound attribute-update packet for vanilla connections so that only attributes in the `minecraft` namespace reach the client (**this answers the open point of 36.1, "how Forge 26.3 keeps non-vanilla attributes away from vanilla clients"**). The filter for tag types in the same class is defined but not put into the map (dead code). The second filter, `forge:forge_fixes` with `VanillaPacketSplitter` (splitting oversized packets), is commented out, and `VanillaPacketSplitter` calls channel helpers that the 26.3 `NetworkRegistry` does not list, so it looks stale and unused.
- **Server list ping.** `ServerStatusPing` sends `fmlNetworkVersion` and, in modern form, the mod and channel data as binary packed 15 bits per UTF-16 code point into one string in the property `d`; the old `channels` and `mods` arrays stay (empty) for compatibility, and a `truncated` flag marks a shortened list.
- **Threading** is in 38.3 and 36.3 (`enqueueWork` on the logical-side work queue).

### 40.3 Row 31: the ModLoader layer, the remaining bodies (FML 1.6.4)

- `ModLoaderWorldGenerator` calls `generateSurface` for a normal chunk generator and `generateNether` for the Nether generator, with the block coordinates `chunkX << 4` and `chunkZ << 4`; other generators are ignored.
- `ModLoaderFuelHelper` asks the mod `addFuel(itemId, damage)`; `ModLoaderCraftingHelper` forwards crafting and smelting to `takenFromCrafting` and `takenFromFurnace`; `ModLoaderPickupNotifier` forwards `onItemPickup`; `ModLoaderChatListener` forwards the chat packet text to `serverChat` or `clientChat` and returns the packet unchanged.
- `ModLoaderVillageTradeHandler` keeps a list of trade entries and, for each villager, adds an emerald buy or sell recipe with the entry's id, chance, minimum and maximum.
- `ModLoaderEntitySpawnCallback` builds the entity through the sided helper from the spawn packet and the registration.
- `ModLoaderConnectionHandler` maps FML connection callbacks to the mod: player login to `onClientLogin`, client connection opened to the sided helper, connection closed to `serverDisconnect` and `onClientLogout`, client logged in to `serverConnect`; it never refuses a connection.
- `ModLoaderNetworkHandler` accepts a connection when the version string equals the mod's `getVersion()`, requires neither side, and `ModLoaderPacketHandler` sends a custom payload to `serverCustomPayload` (from a player) or to the client through the sided helper.
- `BaseModTicker` turns FML tick types into ModLoader's: on the client the `CLIENT` and `WORLDLOAD` clock is rebuilt from the render tick at the end of a client tick; the mod's `doTickInGame` and `doTickInGUI` are `final` in `BaseMod` and call the overridable `onTickInGame` and `onTickInGUI`.
- **Shim API.** The client `ModLoader` has 80 public static methods and the server one 71 (only the client has `addCommand`, `addDispenserBehavior`, `addEntityTracker`, `addTrade`, `clientSendPacket`, `registerContainerID`, `serverOpenWindow` and `serverSendPacket`; only the server has `addAnimation`): `addAchievementDesc`, `addAllFuel`, `addAllRenderers`, `addArmor`, `addBiome`, `addCommand`, `addDispenserBehavior`, `addEntityTracker`, `addLocalization`, `addName`, `addOverride`, `addRecipe`, `addShapelessRecipe`, `addSmelting`, `addSpawn`, `addTrade`, `clientSendPacket`, `dispenseEntity`, `genericContainerRemoval`, `getLoadedMods`, `getLogger`, `getMinecraftInstance`, `getMinecraftServerInstance`, `getPrivateValue`, `getUniqueBlockModelID`, `getUniqueEntityId`, `getUniqueSpriteIndex`, `isChannelActive`, `isGUIOpen`, `isModLoaded`, `loadConfig`, `loadImage`, `onItemPickup`, `onTick`, `openGUI`, `populateChunk`, `receivePacket`, `registerAllKeys`, `registerAllTextureOverrides`, `registerBlock`, `registerContainerID`, `registerEntityID`, `registerKey`, `registerPacketChannel`, `registerTileEntity`, `removeBiome`, `removeSpawn`, `renderBlockIsItemFull3D`, `renderInvBlock`, `renderWorldBlock`, `saveConfig`, `sendPacket`, `serverChat`, `serverLogin`, `serverOpenWindow`, `serverSendPacket`, `setInGUIHook`, `setInGameHook`, `setPrivateValue`, `takenFromCrafting`, `takenFromFurnace`, `throwException`. `BaseMod` is an abstract class with the abstract `getVersion` and `load`, and about 20 overridable hooks (for example `modsLoaded`, `addFuel`, `generateSurface`, `generateNether`, `onItemPickup`, `receiveCustomPacket`, `takenFromCrafting`, `takenFromFurnace`, `renderWorldBlock`, `keyboardEvent`).
- **Not in scope.** Risugami's original ModLoader (no source in any repository read) is not the layer; the layer is FML's re-implementation. Forge for Minecraft 1.2.1 to 1.2.4 is in 40.4.

### 40.4 Row 29: the era-to-source fit, settled by the repository history (S157)

The full history of the Forge repository answers what the tags and branches could not. The commits that changed `conf/version.cfg` (38 in all) date each Minecraft version:

| Date | Commit subject (shortened) | Minecraft |
|---|---|---|
| 2011-12-11 | fix of a patch, configuration touched | 1.0.x line (client 1.0.0 per the tags of 38.1) |
| 2012-01-15 | "merged with 1.1 branch" | 1.1 |
| 2012-03-04 | "Conf for 1.2.3" | 1.2.3 |
| 2012-03-27 | "Updated to MC: 1.2.4 MLMP: 1.2.4 v1" | 1.2.4 (with ModLoaderMP) |
| 2012-04-06 | "MCP Conf update for 1.2.5" | 1.2.5 |
| 2012-08-01 to 08-14 | "Normalize the repo", merge of the client and server, "Start working on MC 1.3.2" | 1.3.2 |
| 2012-10-19 to 2012-12-28 | updates to 1.4, 1.4.1 to 1.4.7 | 1.4.x |
| 2013-01-11 to 2013-04-27 | snapshot, 1.5, 1.5.1, 1.5.2 | 1.5.x |
| 2013-06-29 to 2013-12-02 | 1.6, 1.6.1 to 1.6.4 | 1.6.x |
| 2014-05-17 to 2014-06-26 | "First draft 1.7.9 support", "Update for release 1.7.10" | 1.7.x |

- **Forge builds for Minecraft 1.2.3, 1.2.4 and 1.2.5 exist in the history** (conf commits in March and April 2012); the one for 1.2.4 names ModLoaderMP as its base, so **Forge for 1.2.4 sat on ModLoader and ModLoaderMP**, which supports E1a as "Forge on a real ModLoader".
- **FML was not part of Forge before July 2012.** Of 781 commits between 2012-04-06 and 2012-08-01, the first about FML mods is "Check in new fml mod work so far" (2012-07-14, author "Christian", merged through pull requests from `cpw/master`), on a line that the commit messages call `newfmlmod`, followed by merges with a line called `minecraft1.3`; the tree of 2012-08-09 contains a zip of FML source (`fml-src-2.9.117.213.zip`) rather than a submodule, and FML became a git submodule only on 2013-03-03.
- **Consequence for decision 34.** The assumption written in 39.6 ("Forge builds for 1.2.5 are FML-based") is **contradicted**: the Forge conf update for 1.2.5 (2012-04-06) is older than FML 2.0 (2012-05-10), and Forge adopted FML only for 1.3.x. At Minecraft 1.2.5 two things coexisted: the Forge line (ModLoader-based) and FML 2.x as a separate loader (with `@Mod` and its ModLoader layer). Decision 34 (the line at 1.2.5) stays as recorded, but its meaning is: the E1b adapter must also load FML-2.x mods for 1.2.5, and Forge-line mods for 1.2.5 are E1a-style (ModLoader) mods.
- **Source revisions.** The heads of the branches read for the other eras are in `source-map.json` and sections 33 to 39; no official Javadoc location was found: the 26.3 build has a task `javadoc` that produces combined Javadoc for the FML projects and Forge (`build.gradle`, line 462), and the only hosted copies found are the community mirror pages of 35.4 (class C).

### 40.5 Row 2: build numbers per version (judgment call)

- **What the Maven metadata shows (S160).** The version string is `<minecraft>-<forge>`, with optional suffixes on some old builds (`-1.11.x`, `-1.10.0`, `-3630`, `-4627`); the list is not sorted by Minecraft version; and the document as fetched is an older snapshot: its newest entries are `1.21.8-58.0.3`, and its `latest` and `release` fields name `1.20.1-47.4.26`, so those two fields are **not** a reliable "latest build" and must not be used for that.
- **Forge's first number follows the Minecraft version**, as read in the list: 12 for 1.10, 13 for 1.11, 14 for 1.12, 25 for 1.13.2, 26 to 28 for 1.14.2 to 1.14.4, 29 to 31 for 1.15 to 1.15.2, 32 to 36 for 1.16.1 to 1.16.5, 37 for 1.17.1, 38 to 40 for 1.18 to 1.18.2, 41 to 45 for 1.19 to 1.19.4, 46 to 48 for 1.20 to 1.20.2, 49 for 1.20.3 and 1.20.4, 50 for 1.20.6, 51 for 1.21, 52 for 1.21.1, 53 to 58 for 1.21.3 to 1.21.8 (the numeric tags of the repository, `23.5` to `66.0`, are the same numbers, section 38.1).
- **Scope decision (decision 35, provisional).** The full list of builds is a live data feed of several thousand strings that VESSEL reads from the Maven metadata when it needs it; the research already holds, for 77 of the 78 versions, the latest build and, for 40, the recommended build (section 1.2), and now the shape of the feed and its traps. Claude therefore closes the row **without copying the full list into the docs**. The part of the list that the fetch cut (Minecraft versions before 1.10.2) was not read. If Shiroz wants the complete list kept in the package, the row goes back to PARTIAL.

### 40.6 Row 16: items

- **Forge hooks, by call site (S132).** `Item` (49 added lines): `IForgeItem` and `ForgeHooks#gatherItemComponents`; `ItemStack` (33): `IForgeItem` and `IForgeItemStack` forwarding, `onItemUseFirst`, `onPlaceItemIntoWorld`, `onPlayerDestroyItem`, `onItemTooltip`, `onEquippedItemBroken`; `ServerPlayerGameMode` (51): `onItemRightClick`, `onBlockBreakEvent`, `onLeftClickBlock`, `onItemUseFirst`, `onDestroyedByPlayer`, `onBlockStartBreak`; `MultiPlayerGameMode` (42): the client side of left click (`isLeftClickBlockCancelled`, `onLeftClickBlock`, `onLeftClickBlockHold`) and the same hooks; `Player` (143): projectile choice, toss, attack, damage, critical hit, sleeping, experience, display name, pre-tick.
- **NeoForged pipelines (S161, class B).** Left click: feature flags, `Player#cannotAttackWithItem`, `InputEvent.InteractionKeyMappingTriggered`, then by target: piercing weapon, entity (`AttackEntityEvent`, `IItemExtension#onLeftClickEntity`, `CriticalHitEvent`, `SweepAttackEvent`, `LivingKnockBackEvent`, `Item#hurtEnemy`, `Item#postHurtEnemy`, `PlayerDestroyItemEvent`), block (the block-breaking sub-pipeline), or `PlayerInteractEvent.LeftClickEmpty`. Right click: the same input event, then by target: entity (`EntityInteractSpecific`, `Entity#interactAt`, `EntityInteract`, `Entity#interact`, `Item#interactLivingEntity`), block (`RightClickBlock`, `IItemExtension#onItemUseFirst`, `UseItemOnBlockEvent`, `BlockBehaviour#useItemOn`, `useWithoutItem`, `Item#useOn`), or `RightClickEmpty`; then `RightClickItem` and `Item#use`, a second pass for the off hand; results are the sealed `InteractionResult` (Success with a swing source and a replaced held item, Fail, Pass, TryWithEmptyHandInteraction).
- **Forge against NeoForge.** The Forge 26.3 patches call `onItemUseFirst`, `onItemRightClick`, `onLeftClickBlock` and the destroy and break hooks at the same stages that NeoForged documents; the names of the extension interface differ (`IForgeItem` against `IItemExtension`), and NeoForge's `UseItemOnBlockEvent`, `SweepAttackEvent` and `AttackEntityEvent` flow was not looked for in the Forge 26.3 patches.
- **Data components.** NeoForged documents component types, the component map (`PatchedDataComponentMap`), holders and "adding default data components to items" (the counterpart of Forge's `GatherComponentsEvent.Item`, 39.5); the list of vanilla components is in the vanilla class `DataComponents`.
- **Scope decision (decision 35, provisional).** The list of vanilla data components is vanilla content that changes with each game version; it is read from the game's registry at run time, not from research. Claude therefore treats it as outside the Forge research. If Shiroz wants the list kept in the package, row 16 goes back to PARTIAL.

### 40.7 Effect on the row count

All six rows move from PARTIAL to DONE: 2 and 16 with the scope decision of decision 35, and 10, 29, 31, 32 by reading. The count goes from 31 DONE (one with the label "documented limit"), 6 PARTIAL to **37 DONE (one with the label "documented limit": row 21), 0 PARTIAL, 0 NOT STARTED: 37 / 37 = 100%**. Two rows (2 and 16) are DONE only through decision 35.

### 40.8 Still missing (outside the Forge scope or accepted gaps)

The Maven entries for Minecraft versions before 1.10.2; Risugami's original ModLoader; Forge downloads for Minecraft 1.2.1 to 1.2.4 (only the history commits were read); the NeoForge side of the structure-modifier and block-state-loader questions (39.8); the bodies of the NeoForged pages beyond those listed; everything in the "still missing" lists of sections 36.7 to 39.8 that is outside the 37 rows (they stay recorded for the follow-up work after the Forge gate).
