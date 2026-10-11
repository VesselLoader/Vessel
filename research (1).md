# Upstream Research: Sinytra Connector (reference study, stage A)

**Document version:** 0.2.0
**Research date:** 2026-10-09 to 2026-10-10 (pass A1) and 2026-10-10 (pass A2; the clones of pass A1 were reused, so the branch heads are those of 2026-10-09)
**Research status:** `PARTIAL` (passes A1 and A2 cover rows 1 to 10 of the stage A table in section 4.4 of `MASTER-SPEC-STATUS.md`: rows 9 and 10 are complete, rows 1 to 8 are `PARTIAL`; rows 11 to 26 are not started; section 12 lists what is still unread)
**Platform:** Sinytra Connector and its companion repositories. This is **reference research**, not a VESSEL source loader: no row of this file belongs to a loader table, and no gate applies to it (decision 36).
**Companion files:** `source-map.json` (repositories, branches, heads, licences, as of the run date) and `design-lessons-for-vessel.md` (what the findings mean for VESSEL's design)

How to read this file:
- Every claim carries a source id from section 13 (`SY1` and so on). Source authority is rated A (the project's own repositories and sites), B (documentation or pages of the project read second-hand through a search result), C (third-party pages).
- Class, method, property, file and annotation names are reproduced exactly. Explanatory prose is paraphrased. No code is copied into this package (licences: section 1.5).
- "Evidenced" means a source read in this pass shows it. "Inference" means Claude concluded it. "Proposal" means it is VESSEL design thinking. "Unverified" means seen only weakly or not at all.
- Branch heads move. Every head, version and date below is "as of 2026-10-09".

---

## 0. Corrections to the handoff starting notes (read first)

The handoff prompt (`handoff/NEW-CHAT-PROMPT-STAGE-2.md`, appendix A) listed unverified starting notes. Checked in pass A1:

| # | Starting note | Verdict | Evidence |
|---|---|---|---|
| 1 | Connector runs Fabric mods on NeoForge, "not Forge" | **Incomplete.** True for branches `1.21.x` and `26.1.x`. The branch `1.20.1` targets **MinecraftForge** 1.20.1 (its README says it runs Fabric mods on MinecraftForge; it builds with ForgeGradle and pins Forge 47.4.6) | SY3 (`README.md`, `build.gradle.kts`, `gradle.properties`), SY2, SY1 |
| 2 | Head commits listed in the table | **Mostly confirmed, two moved.** Connector (all three branches), Adapter `26.1.x` and ConnectorExtras `26.1.x` match. ForgifiedFabricAPI `26.2.x` moved to `fb52e7cd925f...` (2026-10-09). Launchpad `master` moved to `029e026d3c...` and two new branches exist, `26.1.x` and `26.2.x` | SY9 |
| 3 | Branch `26.1.x` has no remap step because the game is no longer obfuscated | **Confirmed in code.** The transformer module of `26.1.x` has no renaming transformer, and the loader's mapping resolver returns every name unchanged. (That Minecraft 26.1 is unobfuscated comes from decisions 32 and 33, not re-derived here.) | SY1, SY5 |
| 4 | Mapping data comes from the Minecraft artifact the loader provides (marked "to verify") | **Different.** On `1.20.1` and `1.21.x` the mapping data is a file `mappings.tsrg` shipped **inside the Forgified Fabric Loader jar**, built at that project's build time from Mojang's mapping file plus Fabric's intermediary mapping | SY5 |
| 5 | `ConnectorLocator` is an `IDependencyLocator` with the lowest priority | **Confirmed for `26.1.x`** (`LOWEST_SYSTEM_PRIORITY`) | SY1 |
| 6 | `ConnectorModFileReader` marks Fabric jars as "stubs" | **Confirmed for `26.1.x` only.** On `1.20.1` and `1.21.x` there is no such reader; Connector scans the `mods` directory itself | SY1, SY2, SY3 |
| 7 | The metadata rewrite normalizes the id (`-` to `_`), avoids JPMS keywords with a suffix, records the original id in `provides`, drops the MixinExtras entrypoint, adds dependency aliases and strips the patch part of the Fabric API version | **Confirmed for `26.1.x`**, with the exact suffix `_nojpms` (section 3). The jar's placeholder `neoforge.mods.toml` is also removed | SY1 |
| 8 | The pipeline also converts class tweakers and access wideners, patches Mixin through the Adapter library, redirects accessors and strips the client or server environment | **Partly read.** Signature stripping, Mixin patching and accessor redirect are evidenced as separate pipeline steps; class tweaker handling and environment stripping are named in the code but not yet read (rows 6, 10, 20) | SY1 |
| 9 | `1.20.1` and `1.21.x` rename Intermediary to "the NeoForge runtime names" | **Corrected.** `1.21.x` renames Intermediary to **Mojang** names; `1.20.1` renames Intermediary to **SRG** names (Forge's runtime namespace) | SY2, SY3 (`JarTransformer.java`, constants `SOURCE_NAMESPACE` and `OBF_NAMESPACE`) |
| 10 | Published limits (hybrid servers, no backport before 1.20.1, no OptiFine) | **Not checked in this pass** (row 22) | none |
| 11 | A safeguard detects Mixin errors | **Confirmed.** `MixinTransformSafeguard` is a one-time startup check controlled by the config key `enableMixinSafeguard`; it fails the load with a message that lists the failing Mixin methods (section 6.5) | SY15 |
| 12 | `ModuleLayerMigrator` handles the module layer | **Refined.** On `26.1.x` it is a candidate locator that moves two libraries (Brigadier and Authlib) from the system class loader into the game's module layer through private JDK access; on `1.20.1` and `1.21.x` the same name moves a named module from the boot layer to the game layer (section 9.4) | SY15, SY2, SY3 |
| 13 | The pipeline strips the client or server environment | **Refined.** Only methods, fields and interfaces marked with Fabric's environment annotations are removed (with their lambdas); no class-level removal was seen (section 10) | SY15 |
| 14 | The pipeline merges split packages for JPMS | **Confirmed**, with the exact rule: the first jar found owns a split package and the others give only their files of that package (section 9.2) | SY15 |
| 15 | `ClassProcessor` services transform classes at run time | **Confirmed for `26.1.x` only.** `1.20.1` and `1.21.x` use ModLauncher-style services instead (section 8.4) | SY15, SY2, SY3 |
| 16 | The Mixin patches come from the Adapter library | **Confirmed**, and a small hand-written patch table in Connector complements the generated ones (sections 6.2 and 7.6) | SY15, SY16 |

---

## 1. Repository map (row 1)

### 1.1 The repositories

All are under `github.com/Sinytra/`. Heads from `git ls-remote` on 2026-10-09 (SY9); content read from shallow clones (SY1 to SY8).

| Repository | What it is (evidenced) | Branches (head, first 10 characters) | Tags | Licence |
|---|---|---|---|---|
| `Connector` | The compatibility layer: a mod that discovers Fabric mods, transforms their jars and hands them to the host loader | `1.20.1` 7f68ac0229, `1.21.x` 5a2f668ad4, `26.1.x` 67b3cc5827, `dev` a6b562095c, `master` f2e4253619 | 72 | MIT, "Copyright (c) 2023 Su5eD" (all three read branches); README states it "will always remain" MIT (SY10) |
| `Adapter` | Library of data-driven and generated Mixin patches that Connector applies (its build pins appear in `1.20.1` as `versionAdapter` and others) | `1.20.1` 31326ec2f1, `1.21.x` 38d8594954, `26.1.x` 60fdfcdfc5 | 4 | MIT, "Copyright (c) 2023 Sinytra" |
| `ForgifiedFabricLoader` | Implementation of the Fabric Loader API on top of the host loader (`net.fabricmc.loader.impl.*` classes, Apache-2.0 headers by FabricMC) | `1.19.4` e3578dfe70, `1.20.1` fa2e368c07, `1.20.4` 7517663b85, `1.20.6` 701622cf1a, `1.21.x` 29b610cb73, `26.1.x` 41d9514089 | 93 | Apache-2.0 |
| `ForgifiedFabricAPI` | Fabric API ported to the host loader (large; 27 MB shallow clone of `26.2.x`) | `1.19.4` 55138c20e3, `1.20.1` f5fd869edf, `1.20.4` f77bd0ed26, `1.21` 2650520041, `1.21.1` c2cb2ae0db, `26.1.2` ac8f2d3188, `26.2.x` fb52e7cd92, `master` cae711a673, `neo/1.20.6` 833049b448, `old/1.20.6` f4695025db | 1376 | Apache-2.0 |
| `ConnectorExtras` | Add-on for third-party API integration (README of Connector: "improved compatibility with third-party libraries and APIs") | `1.20.1` 5c93103072, `1.21.x` 9e10ee3a81, `26.1.x` 0f36aae25d | 28 | MIT, "Copyright (c) 2023 Su5eD" |
| `Launchpad` | Reads `fabric.mod.json`, runs entrypoints and provides the factory that turns a transformed jar into a mod file (the Connector code calls `org.sinytra.launchpad.api.FabricModFactory` and `Launchpad`) | `26.1.x` cacb837dd5, `26.2.x` e3c9899aa8, `master` 029e026d3c | 26 | `LICENSE` file is the GPL v3 text; its README says GPL v3 **with the Classpath Exception v2.0** |

The `moddedmc.wiki` documentation tells users to install Connector together with Forgified Fabric API and Launchpad (SY11).

### 1.2 Versions and the host loader per branch

| Connector branch | `versionConnector` | Host loader | Build system | Evidence |
|---|---|---|---|---|
| `1.20.1` | `1.0.0-beta.50` | **MinecraftForge** (Forge 47.4.6 pinned; Forge JarJar; Mixin gradle plugin; fabric loader shipped as a dummy "lang" jar via shadow) | ForgeGradle 6.0 to 6.2 | SY3 |
| `1.21.x` | `2.0.0-beta.18` | **NeoForge** | ModDevGradle | SY2 |
| `26.1.x` | `3.0.0-beta.6` | **NeoForge** | ModDevGradle | SY1 |

`publishBranch` is `master` for `1.20.1`, `1.21.x` for `1.21.x` and `26.1.x` for `26.1.x` (SY1 to SY3). The line to Minecraft version mapping is one branch per Minecraft line, with the host loader switching between the first and second generation (section 2.4).

### 1.3 Layout inside Connector

- **`1.20.1`:** one Gradle project; all code under `src/main/java/org/sinytra/connector/` with sub-packages `loader`, `locator`, `service`, `service/hacks`, `transformer`, `transformer/jar`, `transformer/patch` (about 6100 lines of Java) (SY3).
- **`1.21.x` and `26.1.x`:** root project `connector` plus a subproject `transformer`. The root project has `src/main` (locator, services, utilities: the code that runs inside the loader) and `src/mod` (the packaged mod's own mixins and compatibility code). The `transformer` project holds the jar transformation framework (`api`, `transform`, `plugin`, `jar`, `patch`). `26.1.x` adds a `src/runner` source set in `transformer` with a command-line runner and "probe" classes (`discovery`, `runtime`, `cli`) (SY1, SY2).
- Core loader-side code of `26.1.x` is small: 2177 lines in 17 Java files (SY1).

### 1.4 Continuous integration seen

`build.yml`, `probe.yml`, `release.yml` in `1.21.x` and `26.1.x`; `build.yml`, `release.yml` in `1.20.1` (SY1 to SY3). Contents not read (row 24).

### 1.5 Licence consequences (first reading; full analysis in row 25)

- Connector, Adapter and ConnectorExtras are MIT. Forgified Fabric Loader and Forgified Fabric API are Apache-2.0. Launchpad is GPL v3 with a Classpath Exception (the exception is stated in the README, not in the `LICENSE` file) (SY1 to SY8).
- Rule used in this package: read and paraphrase; never copy code or text of these projects into VESSEL docs or code without a separate licence decision. Whether VESSEL may embed or link Launchpad code is **not decided here**.

### 1.6 What else the organization has (second-hand)

A search result of the organization page (SY13) shows "Showing 10 of 20 repositories" and a third-party mirror (SY12) shows "10 of 22". Names seen: `Connector`, `Wiki`, `ForgifiedFabricAPI`, `ConnectorExtras`, `Adapter`, `MultiLoader-Template`, `ItemAssetExporterMod`, `WikiToolkit`, `CommunityWikiDocs`, `ForgifiedFabricLoader`, `.github`, `FAPI-Mojmaps`, `Launchpad` (the last through the Connector docs, SY11). `FAPI-Mojmaps` is **unexamined** and may matter for rows 12 and 5. The GitHub API call that would list all repositories failed on a rate limit (shared address, 60 requests per hour), so the full list is **unverified**.

---

## 2. Mod detection (row 2)

### 2.1 Two generations of discovery

| Branch | Host | How Fabric jars are found | Stub or in-memory metadata |
|---|---|---|---|
| `1.20.1` | Forge 47 | `ConnectorLocator` (an `IDependencyLocator`, extends `AbstractJarFileModProvider`) scans for Fabric jars **itself**: `scanModsDir`, `scanClasspath`, `scanFromArguments`, all minus the paths Forge's own `ModDirTransformerDiscoverer.allExcluded()` already claims (SY3) | Builds a Forge `ModFile` from the transformed jar with Forge metadata created in memory from the Fabric metadata (section 3.2) |
| `1.21.x` | NeoForge | `FabricModsDiscoverer.scanFabricMods()` (a static scan with the same three sources) called from `ConnectorLocator` (an `IDependencyLocator`) (SY2) | Same idea: `IModFile.create(..., FabricModMetadataParser.createForgeMetadata(...))` |
| `26.1.x` | NeoForge | **Inside FML's reader pipeline.** `ConnectorModFileReader` (an `IModFileReader`, priority 500, "before both Launchpad and builtin readers") turns every Fabric jar into a **stub mod file**; `ConnectorLocator` (an `IDependencyLocator`, lowest system priority) later transforms the stubs and replaces them (SY1) | Stub: a `StubModFileInfo` record that carries the Fabric `LoaderModMetadata` and nothing else |

The shift in `26.1.x` means Fabric jars no longer need a private directory scan: FML's own discovery offers every jar to the readers, the Connector reader claims the Fabric ones, and the later locator step converts them (inference from SY1 and the comparison with SY2).

### 2.2 `26.1.x` in detail (evidenced, SY1)

Services registered in `META-INF/services` of the loader-side code: `IModLanguageLoader` (`ConnectorPrelaunch`), `IDependencyLocator` (`ConnectorLocator`), `IModFileCandidateLocator` (`ModuleLayerMigrator`), `IModFileReader` (`ConnectorModFileReader`), and three `ClassProcessor` classes (`ConnectorClassProcessor`, `ConnectorMethodProcessor`, `FabricASMClassGenerator`; rows 8 and 9).

**The reader (`ConnectorModFileReader`):**
1. Does nothing unless `ConnectorUtil.SHOULD_ENABLE` is true. That flag is true only when the class `org.sinytra.launchpad.api.Launchpad` loads and `FabricModFactory.createModFile` is callable. Without Launchpad, Connector stays inactive.
2. When enabled, its constructor installs log markers (system property `connector.logging.markers`), a crash-report header, a fork-join thread factory, creates a `MixinFacade`, and tells the Fabric Loader implementation to ignore the configured `hiddenMods` and to apply the configured `globalModAliases`.
3. `read(jar, attributes)` returns nothing unless the jar contains `fabric.mod.json`.
4. It also returns nothing for a jar that is already a NeoForge mod: the jar manifest has the mod-type attribute, or `META-INF/neoforge.mods.toml` exists **without** the property `properties.connector:placeholder` (a placeholder TOML is the convention that lets a Fabric mod pose as a NeoForge mod for other tools; a TOML that cannot be parsed is treated as a real NeoForge mod).
5. It parses the metadata with Fabric Loader's own `ModMetadataParser`, then skips the mod if its id is in `ConnectorUtil.DISABLED_MODS` or if its custom value `Constants.ENABLE_LAUNCHPAD` is true. The second rule hands such jars to Launchpad's own reader (section 3.1 explains which jars carry that marker).
6. Otherwise it returns `IModFile.create(jar, manifestParser, Type.MOD, attributes)` whose info object is `StubModFileInfo`: no mod infos, no language loaders, not a resource or data pack, license string `"unknown"`, file properties `{ "fabric:metadata": <the Fabric metadata> }`.

**The locator (`ConnectorLocator.scanMods`):**
1. Same guard on `SHOULD_ENABLE`; initializes the transformer plugin manager.
2. Reads the list of already located files from the pipeline object **by reflection** (private field `loadedFiles`).
3. Splits that list into real NeoForge mods and stubs. Builds the set of already loaded mod ids (libraries excluded).
4. Keeps only stubs that "should load": the Fabric metadata says it loads in the current environment (client or server), and no native mod with the **normalized** id (`-` replaced by `_`) is already loaded.
5. Picks the Minecraft version from FML's version info, or from the file of the `minecraft` mod.
6. Groups the stubs by Fabric mod id; when several files share an id it keeps the **highest version, and among equal versions the lowest nesting level** (a file inside another jar is nested). The unique name of a candidate is the chain of parent file names joined with `$`.
7. Duplicate handling against FML: a Fabric mod whose id is already loaded by FML is dropped (logged as "Ignoring duplicate mod"). If the id is held by a **library** and the Fabric version is newer, the Fabric mod wins and the other files are added to an "ignored" list because FML's `UniqueModListBuilder` will exclude them; that list is also passed to the split-package step.
8. Transformed output goes through the cache (section 4) with the file paths of all non-stub loaded mods as the rename library classpath.
9. Jars whose audit shows failing Mixins go to `MixinTransformSafeguard.prepare`; then `SplitPackageMerger.mergeSplitPackages` (row 9) makes the result module-safe.
10. For each result it calls Launchpad's `FabricModFactory.createModFile(contents, attributes, type)`. The type is `MOD`, or for generated library jars `LIBRARY` (if a library with that module name already came from a parent library) or `GAMELIBRARY`.
11. Adds the results to the pipeline, calls `ConnectorEarlyLoader.init`, adds the **generated adapter mixins jar** (`adapter/adapter_generated_mixins.jar` in the cache) as an extra path, and removes the stubs from the located list.
12. Errors: `ModLoadingException` passes through; anything else is logged, a startup message "CONNECTOR LOCATOR ERROR" is added and a `ModLoadingException` with "Fabric mod discovery failed" is thrown.

### 2.3 `1.20.1` and `1.21.x`: what is different (evidenced, SY2, SY3)

- **Placeholder rule:** a Forge or NeoForge mod file whose properties contain `connector:placeholder` is ignored as a real mod, and on `1.20.1` its version is forced to `0.0` so FML's duplicate resolution prefers the Fabric mod. In `FabricModsDiscoverer.isFabricModJar` (`1.21.x`) a jar with a `mods.toml` is skipped as a Fabric candidate unless that TOML is a placeholder.
- **Classpath scan** (jars carrying `fabric.mod.json` on the development class path) runs only when the game is **not** in production mode (`1.21.x`).
- **Extra locations:** the system property `connector.additionalModLocations` (comma-separated files or directories).
- **Nested jars:** `1.20.1` calls `discoverNestedJarsRecursive` with the metadata's nested jar list and a temporary directory under Connector's folder; `1.21.x` has `EmbeddedDependencies` and uses the loader's JarJar dependency locator on a temporary file (row 19 will read these).
- **Dependency check before transforming:** both branches call `DependencyResolver.resolveDependencies`, which checks that required dependencies exist and removes side-only mods. `26.1.x` has no such class in its locator folder (its check is replaced by the `loadsInEnvironment` test and by the loader's own resolution; **unverified**).
- **Early registration:** `1.20.1` has `ConnectorEarlyLocator` (reflects into Forge's discoverer to reach its list of dependency locators); `1.21.x` has `ConnectorEarlyLocatorBootstrap` (an `IModFileCandidateLocator`). Their exact roles were not read (section 12).

### 2.4 Summary of the discovery design

1. Detection key: presence of `fabric.mod.json`, and absence of a real native mod descriptor.
2. A **placeholder** descriptor is the agreed marker for "this jar is a Fabric mod pretending to be native".
3. Conflicts between a Fabric mod and a native mod of the same id are resolved by **version and nesting**, with a hard rule that native mods win unless a library holds the id.
4. The environment filter uses the Fabric metadata's `environment` field.

---

## 3. Rewriting Fabric metadata (row 3)

### 3.1 `26.1.x`: the `fabric.mod.json` of the output jar is rewritten (evidenced, SY1)

`FabricMetadataTransformer` is a transformer in the per-jar pipeline. It looks at two entries only:
- **`fabric.mod.json`** is parsed and rewritten (pretty-printed). On any error the entry is logged and kept unchanged.
- **`META-INF/neoforge.mods.toml`** is **deleted** from the output (the placeholder TOML must not survive).

Changes made to the JSON:
1. **Id normalization:** `-` becomes `_` (reason given in the code: JPMS module names). If the result is a Java reserved keyword, the suffix `_nojpms` is added. `version` is written back unchanged.
2. **Aliasing:** when the id changed, the new id is removed from `provides` if present and the **original id is added to `provides`**, so other mods can still depend on the original id.
3. **Entrypoints:** the two MixinExtras bootstrap entrypoint strings (`com.llamalad7.mixinextras.MixinExtrasBootstrap` and its `::init` form) are removed. Reason in the code: MixinExtras starts itself from its own mixin config plugin, and a second start at the game layer causes a duplicate class definition.
4. **Dependencies:** for every dependency id that is neither an already loaded mod id nor the id of a Fabric mod about to be loaded, and that has an alternative in the configured alias table, the dependency is replaced by the alternative with version `*`. If `fabric-api` is a dependency, its version predicate loses the patch component (pattern: major.minor.patch... becomes major.minor; text after a `+` is kept).
5. **Markers:** the custom section receives two boolean markers: `connector:active` (`TransformerUtil.METADATA_MARKER`) and `launchpad:compatible` (`TransformerUtil.LAUNCHPAD_MARKER`). The second string is identical to `Constants.ENABLE_LAUNCHPAD` in Launchpad's API (SY6), which is what `ConnectorModFileReader` tests in step 5 of section 2.2; so a transformed jar is skipped by Connector's reader and left to Launchpad's reader. (That Launchpad's reader then claims it was not read in this pass.)

Not done here (other steps): jar signature removal (`JarSignatureStripper`), Mixin patching, accessor redirects. Class tweaker conversion is handled by the class-node transformer registered in the builtin plugin (`FieldToMethodTransformer` receives `getClassTweaker()`); its details are for row 20.

How Connector reads the original metadata (evidenced, SY1): `MetadataReader.readModMetadata` calls the Fabric Loader's own `ModMetadataParser.parseMetadata`; a missing file warns and returns null; a malformed one throws. `readJarMetadata` gathers the mixin configs declared per environment, adds undeclared files named `*.mixins.json` or `mixins.*.json`, and reads from each config its `refmap`, `package` and class lists. A jar is treated as a **generated library** when its custom value `fabric-loom:generated` is true and the manifest attribute `Fabric-Loom-Remap` is not `"true"`. Generated jars get only the metadata rewrite and signature stripping.

### 3.2 `1.20.1` and `1.21.x`: native-style metadata built in memory (evidenced, SY2, SY3)

On these branches the Fabric JSON is **not** the thing the host loader reads. `ConnectorModMetadataParser.createForgeMetadata` (`1.20.1`) and `FabricModMetadataParser.createForgeMetadata` (`1.21.x`) build an in-memory NightConfig document shaped like a `mods.toml` and wrap it in the host's mod file info. Its content:
- `modLoader`: `lowcodefml` for generated library jars, otherwise `javafml`; `loaderVersion`: `[0, )`.
- `license`: the joined Fabric licenses, or `All Rights Reserved` if none.
- `properties`: the Fabric metadata object and the Connector marker; `modproperties.<id>`: the Fabric custom values.
- One `mods` entry: `modId`, `version` (only if it matches a digit-first pattern; otherwise the host assigns its default and a warning is logged), `displayName`, `description`, `logoFile` (from the icon), `modUrl` and `displayURL` (from homepage, else source, else the first contact entry, each only if a valid URL), `issueTrackerURL`, `updateJSONURL` (from the custom value `forgeUpdateJSONURL`), `authors` and `credits`.
- `displayTest`: `IGNORE_ALL_VERSION` for client-environment mods, `IGNORE_SERVER_VERSION` for server-environment mods.
- `1.21.x` additionally adds the `mixins` list (visible Mixin configs).

### 3.3 `1.20.1`: generated entry class and pack metadata (evidenced, SY3)

`ModMetadataGenerator` (a per-jar transformer) **adds** to the transformed jar: a generated class named `org/sinytra/generated/<modid>_<random5>/Entrypoint_<modid>` annotated with Forge's `@Mod("<modid>")` and a default constructor, so that FML treats the jar as a mod with code; and a `pack.mcmeta` (pack format 15, description "<modid> resources, generated by Connector") when the jar has none (a comment in the code says to remove it in 1.20.5).

### 3.4 What this means (inference)

Three different techniques appear for one task, "make the host loader see a Fabric jar as a mod": a **stub** that keeps the Fabric metadata untouched (`26.1.x` reader), a **rewritten Fabric JSON** that Launchpad reads (`26.1.x` transform), and a **synthetic native descriptor** built at run time (`1.20.1`, `1.21.x`). VESSEL's IR approach avoids needing any host descriptor, but the **rules** (id normalization, alias table, MixinExtras entrypoint removal, environment filter) are the same problems VESSEL must solve (proposal; see `design-lessons-for-vessel.md`).

---

## 4. The jar transformation pipeline and its cache (row 4)

### 4.1 Overview of one run (`26.1.x`, evidenced, SY1)

1. **Candidates** are built by the locator (section 2.2): each has an input file, an output path in the cache directory, the parsed metadata and a cache record.
2. **Cache check** per jar (section 4.3). Jars that are up to date are reused. **Every** input jar, up to date or not, is added to the class path used by the transformer, so that class lookups can see all mods.
3. For the jars that need work, **one transformer instance** is created: it collects the package names of all libraries, builds a class provider over the library paths (wrapped by the early coremod transformer when the `neoforge.coremods` library is present), installs that provider as the Mixin service's bytecode source (through reflection into the loader's private Mixin classes), loads a **clean (vanilla) game jar** for class lookup, and creates the generated-adapter front end.
4. **One thread per jar** (a fixed pool as large as the number of jars, threads named `TX <file name>`), with a **one hour** overall timeout. Progress is reported through the loader's startup progress bar.
5. **Per jar:** a generated library jar gets only the metadata transformer and the signature stripper. Any other jar gets the full chain (4.2), run by NeoForged's rename tool (package `net.neoforged.art`; assets and data prefixes `assets/` and `data/` are not scanned as classes), then a finalization step (4.2, last item).
6. After all jars: the generated adapter Mixin jar is saved, the audit report is written, the shared transformer state (the global bytecode loader and Mixin's static class-info cache) is reset.

### 4.2 The transformer chain and its plugin framework

- **Plugin SPI:** `TransformerPlugin` (name, `registerPatches`, `registerJarTransformers`), loaded through `PluginManager`, which uses `ServiceLoader.load` (evidenced; the built-in plugin is declared in the transformer module's `META-INF/services`; configuration and API are row 17). The built-in plugin is `connector:builtin`.
- **Registration with ordering:** each jar transformer is registered with a name and optional "runs before" and "runs after" sets plus an ordering hint. Names must be unique (duplicate names throw). The sorted order is a topological sort of the declared constraints; targets that are not present are ignored (so a plugin can order itself against transformers that may not exist); ties break by ordering hint, then by name.
- **Built-in transformer ids:** `connector:signature_stripper`, `connector:mod_metadata`, `connector:analysis`, `connector:patches`, `connector:access_redirect`.
- **Built-in chain (`26.1.x`):** signature stripper; metadata transformer; the analysis step (a `FieldToMethodTransformer` that receives the class tweaker, and a `ClassAnalysingTransformer`) registered before the patches; the **Mixin patch transformer** at the late ordering position; the accessor redirect transformer after the patches.
- **Patches:** three groups registered at three priorities (highest system priority, higher system priority, default priority) from `MixinPatches` and the accessor redirect transformer; they come from the Adapter library (row 7).
- **Per jar, before the run:** the refmaps named by the Mixin configs are read; a patch environment is created from the refmap holder, the clean class lookup, the Mixin class lookup, the Fabric Mixin compatibility level and the per-jar audit trail.
- **Finalization:** after the rename tool wrote the output, the patch transformer's `finalize` step receives the root of the output archive, the Mixin config names, the refmap files and the refmaps that were changed ("dirty") and writes them back. Errors from Mixins that appear in no config file are silenced in the audit trail.

### 4.3 The cache (evidenced, SY1)

- **Where:** `<game directory>/.cache/connector/`. Transformed jars are `<unique name>.jar`; the generated adapter jar is `adapter/adapter_generated_mixins.jar`; the audit report is `patch_audit.txt`; downloaded clean game jars are in `minecraft/minecraft-<version>-<side>.jar`.
- **Record:** next to each output `X.jar` a text file `X.jar.input` holds `<cache version>,<SHA-256 of the whole input jar>`.
- **Up to date** only if the record exists, the output exists and the text equals the new value. A mismatch deletes both files; a missing record deletes the output. The record is written **after** a successful transform.
- **Cache version:** `__dev__` outside production; otherwise the Connector jar's implementation version plus `,` plus the side. If the version cannot be read, an error is logged and a **random** five-letter string is used so that the cache never matches by accident.
- **Switch:** system property `connector.cache.enabled`; anything other than `true` disables caching.

### 4.4 The clean game jar

`GameCodeRetriever` downloads the **vanilla game jar of the exact Minecraft version and side** from Mojang's launcher manifest (`launchermeta.mojang.com/mc/game/version_manifest_v2.json`, then the per-version manifest's `downloads.client` or `downloads.server` url) into the cache, under the same cache-version rule. It is the class source for Mixin patch analysis ("clean" means not patched by the host loader) (SY1). This is the same pattern as VESSEL's decision 33 (download at run time, cache, do not bundle); see `design-lessons-for-vessel.md`.

### 4.5 Differences in `1.20.1` and `1.21.x` (partly read)

- **Chain order on `1.21.x`** (read, SY2): signature stripper; analysis (field-to-method with class tweaker and the Intermediary mapping, **reflection renaming**, class analysis); **optimized renaming**; Mixin patch transformer; accessor redirect; **refmap remapper**; and an **access widener transformer** only when the jar does not contain an access transformer. `26.1.x` has none of the four bold steps.
- **Locator class names and the jar transformer** differ in structure (`JarTransformer` in `1.20.1` is 283 lines and carries the namespace constants; the `1.20.1` and `1.21.x` `JarTransformer` classes were **not** read in full, section 12).
- **`1.20.1` has no plugin framework** (no `plugin` or `api` package in the file list; the transformer classes are called directly) (inference from the file list, SY3).

---

## 5. Remapping and the end of remapping (row 5)

### 5.1 Result per branch (evidenced)

| Branch | Names in Fabric mods | Names the host runs | Mapping source | Evidence |
|---|---|---|---|---|
| `1.20.1` (Forge 47) | Intermediary (`SOURCE_NAMESPACE = "intermediary"`) | **SRG** (`OBF_NAMESPACE = "srg"`) | `mappings.tsrg` in the Forgified Fabric Loader jar (default content stated as OFFICIAL, SRG, INTERMEDIARY) | SY3, SY5 |
| `1.21.x` (NeoForge) | Intermediary | **Mojang** names (`OBF_NAMESPACE = "mojang"`) | `mappings.tsrg` in the Forgified Fabric Loader jar, built as OFFICIAL, INTERMEDIARY, MOJANG | SY2, SY5 |
| `26.1.x` (NeoForge) | none needed | none needed | none: the loader's mapping resolver has only the runtime namespace and returns every class, field and method name unchanged | SY1, SY5 |

Fabric mods must be mapped to Intermediary for Connector to process them (Connector README, SY10). The manifest attribute `Fabric-Mapping-Namespace` is checked: a value other than Intermediary logs an error that only Intermediary is supported (SY2, SY3).

### 5.2 Where the mapping data comes from (`1.21.x`, evidenced, SY5)

The Forgified Fabric Loader's build contains a task `GenerateMergedMappingsTask`. Inputs: the Yarn mapping jar's `mappings/mappings.tiny` (which supplies the Intermediary names) and a Mojang mapping file. The task merges them, drops entries with no Mojang name, completes missing names and writes the result in TSRG2 form. At run time `MappingResolverImpl` loads the resource `/mappings.tsrg` from the loader jar and answers `getMap(from, to)` and `getCurrentMap(from)` (to the runtime namespace). **Unverified:** whether the published loader jars really contain this file, and how Mojang's mapping licence is handled for it. VESSEL's decision 33 forbids bundling; this is a design difference.

### 5.3 What the renamer changes (`1.21.x`, partly read, SY2)

- **Classes, fields, methods and descriptors** through NeoForged's renaming transformer (the project's `OptimizedRenamingTransformer` extends it) with a remapper aware of Mixin targets (it first reads which classes a Mixin class targets, so the hierarchy of the target is taken into account).
- **Strings that name members** (reflection calls and similar), through a post-processing step over class annotations, method annotations, `LDC` constants, `invokedynamic` bootstrap arguments and the bootstrap handle, and constant field values. It uses a flat name table (`IntermediateMapping`) built once per namespace from names with the Intermediary prefixes (`net/minecraft/class_`, `field_`, `method_`, `comp_`); a name that has two different targets is stored under name plus descriptor instead.
- **Mixin annotations:** when the jar has **no refmap**, or an annotation says `remap = false`, annotation values are mapped directly; otherwise the refmap carries the mapping. Regex-style method selectors (`desc=` form) have the class name replaced.
- **Refmaps:** the refmap files named by the Mixin configs are remapped (method references, field references and class names, owner and descriptor included) and the environment key `named:intermediary` is replaced by `mojang`; a refmap that is not in the mod jar is searched in the other jars on the class path.
- **Ambiguity:** when several Intermediary names map to one Mojang name and a method overrides more than one such parent, self-calls on `this` are switched to a super call and duplicate same-name methods in the class are renamed with the suffix `$connector_disabled$<n>`.

### 5.4 Meaning of `26.1.x` (evidenced facts, inference on cause)

The renaming transformer, the refmap remapper, the Intermediary table, the reflection renamer and the access widener transformer are all **absent** from the `26.1.x` transformer module, and the Fabric Loader mapping resolver is an identity. The cause is the unobfuscated game (decisions 32 and 33); the code evidence is only the absence and the identity (inference).

---

## 6. Mixin handling (row 6)

### 6.1 Where it sits (evidenced, SY15)

Mixin handling is one step of the per-jar chain of section 4: the transformer `MixinPatchTransformer` (id `connector:patches`) runs after the analysis step and before the accessor redirect. It uses the Adapter library (section 7) and the refmap data of section 5.3, and it is the only part of the chain that changes Mixin classes.

### 6.2 What it does to each class

For **every class entry** of the jar (not only the classes listed in a Mixin config) the transformer reads the class and checks for the `@Mixin` annotation (class-retention). The comment in the code explains why: some mods generate their Mixin configs at run time (the Andromeda mod is named), so the config files cannot be trusted as the list.
- **A Mixin class** is given to the Adapter's `Patcher` (section 7.3), built with the dynamic class patches and the method transformers of section 7.5 plus the patch list that the plugin framework collected (the three priority patches, the accessor-redirect patches and the default patches of section 7.6).
- **Any other class** goes through two class transformers: the environment stripper (section 10) and `FieldTypeUsageTransformer`.
- A class is written back only when a result other than "pass" came out; the writer is `InheritingClassWriter`, with frame computation only when a patch asked for it.
- **Generated Mixin classes:** the patch environment can create new Mixin classes (when a patch extracts a Mixin into another class). Those are patched too and added to the jar as extra class entries.

### 6.3 After the run: the finalization step

`finalize` receives the root of the output archive. For each Mixin config file that exists and names a `package`, the generated Mixin classes inside that package are **added to the `mixins` array of the config** (the JSON is rewritten, pretty-printed); if the config's refmap was changed ("dirty"), the refmap file is rewritten too. Then every file in `META-INF/services` loses the lines that name classes which no longer exist in the jar (a file left empty is deleted); this is the cleanup after environment stripping removed classes or members.

### 6.4 Refmaps on `26.1.x` (no remapping)

`RefmapStorage.processRefmaps` reads each named refmap from the jar with Gson, skips missing ones with a warning, and merges the rest. `ConnectorRefmapHolder` is the Adapter's `RefmapHolder` implementation: `remap(class, reference)` looks the reference up (spaces removed) in the merged mappings under the class name (dot or slash form) and returns the reference unchanged when absent; `copyEntries(from, to)` copies refmap entries from one Mixin class name to another (used when a patch moves a Mixin) and records which refmap files became dirty. No names are translated on this branch; compare section 5.3 for `1.21.x`.

### 6.5 The safeguard

`MixinTransformSafeguard.prepare(mods)` stores the list of jars whose audit shows failing Mixins, or only logs a warning when the config key `enableMixinSafeguard` is false. `trigger()` runs once, from `ConnectorPrelaunch.version()` (section 8.2); when the list is not empty it throws a loading error whose text counts the incompatible Fabric mods, warns that Connector cannot guarantee a stable environment, says the one-time check can be switched off with `enableMixinSafeguard`, and lists for each mod file the failing Mixins as `Class#method`, taken from the audit trail (SY15).

### 6.6 Runtime integration

`ConnectorModFileReader` creates NeoForge's own `MixinFacade` (`net.neoforged.fml.loading.mixin`); the Fabric Mixin compatibility level of a mod comes from `FabricMixinBootstrap.getMixinCompat(metadata)` of the Forgified Fabric Loader. Connector's `26.1.x` loader-side code does not ship Mixin itself (to be confirmed in rows 11 and 18).

### 6.7 Differences between branches (found by comparing the files, SY2, SY3, SY15)

| Class | `1.21.x` against `26.1.x` | `1.20.1` against `1.21.x` |
|---|---|---|
| `MixinPatchTransformer` | 23 changed lines. `1.21.x` keeps the priority and default patch lists as static fields, accepts extra patches from the caller (order: priority, extra, default) and has an optional filter so that only a given set of Mixin classes is patched; `26.1.x` receives one merged list from the plugin framework and has no filter. `finalize` is the same except for the refmap type name | 123 changed lines; written against an older Adapter API (not analyzed further) |
| `MixinTransformSafeguard` | 20 changed lines. `1.21.x` throws straight from the locator, adds the startup message "INCOMPATIBLE FABRIC MOD FOUND", tells the user to restart the game and throws a priority loading exception; `26.1.x` stores the list and throws later from `ConnectorPrelaunch` | not compared |
| `ConnectorRefmapHolder` | 10 changed lines, only type names and `replaceAll` against `replace` | not compared |
| `FabricMixinBootstrap` | `1.21.x` carries a copy of a Fabric Loader class at a pinned commit, with Fabric's Apache-2.0 header; `26.1.x` has none in Connector (the Forgified Fabric Loader provides it) | not compared |

### 6.8 Not read

The Mixin classes of Connector's own mod (`src/mod`, 22 classes: registries, networking, items, client; they belong to rows 13 to 16); `FieldTypeUsageTransformer`; the audit trail internals; `1.20.1`'s Mixin patch transformer beyond the line count.

---

## 7. The Adapter library (row 7)

### 7.1 What it is (evidenced, SY16)

`Sinytra/Adapter` is MIT-licensed and, as its organization description says, holds "data-driven, generated mixin patches for Connector" (SY13). Branch `26.1.x` has no README. Modules: **`core`** (164 Java files: `analysis` 21 files, `env` 34, `patch` 68, `transform` 24, `types` 9, `util` 8), **`runtime`** (5 files: a Mixin config plugin `AdapterMixinPlugin` and a family of custom injectors for an "instanceof value" injection) and **`test`** (test projects for Minecraft 1.21.1 and 26.1.2). It builds against Minecraft 26.1.2 and NeoForge 26.1.2.77 (its `gradle.properties`).

### 7.2 The idea (inference from the code in 7.3 and 7.4)

A Fabric mod's Mixin methods were written against the **vanilla** game. The host loader changes vanilla classes. The library therefore compares the vanilla ("clean") class lookup with the host-modified ("dirty") lookup and, for each Mixin method whose injection no longer fits, tries to build a corrected injection.

### 7.3 `Patcher.process(ClassNode)` (evidenced)

1. `MixinParser.prepareMixinClass` yields the class target (the classes the Mixin targets).
2. Class-level transformers run first.
3. The Mixin methods are parsed (parsing is split in two because the class transformers may change methods).
4. For each Mixin method with a known target method: a `MixinContext` is built; a mutable configuration is made from the Mixin type's template merged with the parsed properties; the audit trail prepares the method; then four phases: **EARLY** transformers, `preProcess` of the Mixin type (failure skips the method), **LOADED** transformers, validation of the configuration (invalid skips the method), then, with `maxLocals` and `maxStack` temporarily set to 999 for frame analysis, **VALIDATED** transformers.
5. `context.run()` runs deferred work; `MixinGroups` and the audit trail close the class. The result is one of "pass", "apply" or "compute frames".

### 7.4 `PipelineMethodTransformer` (the VALIDATED phase)

- It finds the target method in the **clean** lookup (none: pass) and in the **dirty** lookup. If no manual patch matches and the injection still resolves in the dirty target (valid slice, the Mixin type can inject, the target has injection instructions or constants), it passes.
- Otherwise it builds a recipe: a clean configuration and a child "dirty" configuration, then runs the **resolvers** in order, the manual patch resolver first. A resolver returns success (merge into the dirty configuration), replace (use its configuration and stop) or fail (skip the Mixin).
- It runs the Mixin type's `postProcess`, validates the dirty configuration, and runs the **processors** (fail skips, finalize stops). A stale parameter debug table is dropped when the parameter count changed. The audit records "none" first and "full" on success.
- The packages hold many resolvers and processors (target method, injection point, parameter differences, ordinal resolvers, "extract mixin", WrapOperation helpers): about 6,300 lines in `patch` and 2,900 in `analysis`. They were **not** read.

### 7.5 Dynamic patches (evidenced)

Class patches: `DynamicAnonClassIndexPatch`, `DynamicAnonymousShadowFieldTypePatch`, `FieldTypeUsageTransformer`. Method transformers by phase: `LocalCaptureUpgradeTransformer` (EARLY), `FieldAccessorTypeTransformer` (LOADED), `PipelineMethodTransformer` with the patch list (VALIDATED).

### 7.6 Connector's hand-written patch table (`MixinPatches`, `26.1.x`)

A patch names a vanilla class and method, an injection point and what to change (target, injection point, Mixin type, "extract the Mixin into another class", disable).
- **Priority patches (3):** `ItemStack.useOn` (retarget to `connector_useOn`, injection point replaced by return); `Minecraft.<init>` (Fabric's client start hook moved to a `Thread.currentThread()` call); `net.minecraft.server.Main.main` (Fabric's server start hook moved to NeoForge's `ServerModLoader.load`).
- **Default patches (8 groups):** `KeyMapping` `set`, `click` and `resetMapping` (the last turns `Map.put` redirects into `KeyMappingLookup.put` with a custom body change); `PistonStructureResolver.isSticky` and `LivingEntity.goDownInWater` (retarget and **extract** the Mixin into NeoForge extension interfaces); `SnowGolem`, `Sheep` and `MushroomCow` `mobInteract` (**disabled**: NeoForge moved the behaviour out with no replacement, per the code comment); rendering patches for `PowderSnowBlock` and `ItemInHandRenderer`.
- Several carry the comment that they "should be automatic". So the table is a short list of hard-coded exceptions; the rest comes from the generated analysis.

### 7.7 The runtime part

`AdapterMixinPlugin` and the `ModifyInstanceofValue` injector family are loaded with the patched mods (the generated adapter Mixin jar of section 2.2 step 11 is the other runtime piece). Read only by file name.

### 7.8 Not read

The resolvers, processors and analysis code in detail; the `1.20.1` and `1.21.x` branches of Adapter; how the generated adapter Mixin jar is produced (`BytecodeFixerUpperFrontend`); `MockMixinRuntime`; the test projects.

---

## 8. Runtime class and method processors (row 8)

### 8.1 `26.1.x`: three FML class-processor services (evidenced, SY15)

All three are registered as FML `ClassProcessor` services (section 2.2) and reshape **host or vanilla classes** to what Fabric mods expect, or let Fabric helper libraries work.

| Service | Targets | Change |
|---|---|---|
| `ConnectorClassProcessor` (simple class processor; id `connector:class_coremod`; runs after frame computation and before Mixin) | `net.minecraft.client.KeyMapping` | inserts a private static final `Map` field `MAP` at the index of the existing one (a Connector issue is cited) |
| | `net.minecraft.world.item.CreativeModeTab` | adds the vanilla-style constructor, calling the NeoForge constructor with default values |
| | `net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor` | makes the field `consumer` public final |
| | `ParticleEngine.providers`, `BlockColors.blockColors`, `ItemColors.itemColors` (client) | adds a synthetic field with the original's access minus `final` (public if the original is missing), so a Connector Mixin can assign it |
| `ConnectorMethodProcessor` (simple method processor; id `connector:method_coremod`) | `EffectsInInventory.extractRenderState` | after the stream `collect`, inserts a natural-order sort of the list |
| | `LivingEntity.forceAddEffect(MobEffectInstance, Entity)` | widens the scope of a local variable and initializes it to null so a Mixin local capture works |
| | FabricASM plugin classes (two package families, `com.chocohead.mm` and `me.shedaniel.mm`): `Plugin.fishAddURL`, `Plugin$1.generate`, `EnumSubclasser.defineAnonymousSubclass` | replace or prefix the method body to call the three helpers of the generator below |
| `FabricASMClassGenerator` (class processor; id `connector:fabric_asm`) | classes in the packages `com.chocohead.gen.mixin` and `me.shedaniel.gen.mixin` | declares those packages as generated, loads the class bytes from the URLs the FabricASM hook collected (frames recomputed); helpers: `fishAddURL` (collects URLs), `flattenMixinClass` (`/` to `_`), `permitEnumSubclass` (adds an anonymous class to the enum's permitted subclasses) |

The targets are hard-coded class and method names of one Minecraft line (`26.1.x`); nothing is selected by version inside the file.

### 8.2 `ConnectorPrelaunch`

An `IModLanguageLoader` named `connector:prelaunch` that is **not** used to load mods (`loadMod` throws). Its `version()` method runs, once, `MixinTransformSafeguard.trigger()` and `ConnectorEarlyLoader.finalizeCache()`. It is a lifecycle hook that exploits the moment FML asks language loaders for their version (inference).

### 8.3 Other service classes of `26.1.x`

`ConnectorForkJoinThreadFactory` (46 lines; installed by the reader's constructor) was not read; `ModuleLayerMigrator` is in section 9.4.

### 8.4 `1.20.1` and `1.21.x`: ModLauncher-style services (names and sizes only)

`ConnectorLoaderService` implements ModLauncher's `ITransformationService` (141 lines); `ConnectorPreLaunchPlugin` implements `ILaunchPluginService` (60); `DummyVirtualJar` implements `SecureJar` (102); `FabricMixinBootstrap` (148; a copy of a Fabric Loader class at a pinned commit); `hacks/FabricASMFixer` (119), `hacks/LenientRuntimeEnumExtender` (36; extends the loader's `RuntimeEnumExtender` and tolerates field flags that a Mixin changed), `hacks/ModuleLayerMigrator` (163), `hacks/ConnectorForkJoinThreadFactory` (46); `1.20.1` adds `ServiceProviderInheritanceWorkaround`. Their bodies were **not** read.

### 8.5 Meaning (inference)

The class-transform extension point of the host changed (ModLauncher services up to `1.21.x`, FML class processors on `26.1.x`) while the set of compatibility shims (FabricASM, enum extension, module layer) carried over. A VESSEL adapter must therefore treat "how to register a class or method transform in the host" as an **era-specific** API, but the list of compatibility problems as era-independent (proposal; see `design-lessons-for-vessel.md`).

---

## 9. Split packages and JPMS (row 9)

### 9.1 The problem (inference)

The host loads mods as named Java modules, and two named modules may not contain the same package; Fabric mods do not follow that rule, so Connector must make jars module-safe before the host sees them. Libraries are exempt because they live in unnamed modules; that is why the merger skips jars of type `LIBRARY`.

### 9.2 The rule (`SplitPackageMerger.mergeSplitPackages`, `26.1.x`, evidenced, SY15)

1. For every non-library input jar, scan its packages (`JarModuleInfo.scanModulePackages`) and build the map package to jars.
2. A package found in more than one jar is **split**. The **first jar in discovery order owns it**; every other jar is added to the owner as an extra path filtered to that package only (its package folder, its files and root resources), and the same package is excluded from the other jar.
3. Packages that **already exist** are excluded from the Fabric jars: those of already loaded mod files (except files FML will drop as duplicates) and those of the unnamed modules along the class loader chain.
4. Each affected jar receives a package filter (`PackageTracker`) combined with a base filter that drops `META-INF/versions`; unaffected jars pass through with only the base filter (none for libraries).
5. A check requires the number of output paths to equal the number of input paths.

### 9.3 `PackageTracker` and its licence header

`PackageTracker(Set<String> packages)` accepts a path unless its package is in the excluded set; it always accepts `META-INF/`, root resources and directory entries. The file carries the **GNU LGPL v3 header of cpw's BootstrapLauncher** and a comment linking the source lines it derives from, although the repository is MIT (a fact for row 25, not a legal conclusion).

### 9.4 `ModuleLayerMigrator`

- **`26.1.x`** (a `IModFileCandidateLocator`, 205 lines): at class-load time it fetches, through `ConnectorUtil.TRUSTED_LOOKUP` (an `Unsafe`-based trusted lookup), private members of the JDK's built-in class loader and its `URLClassPath`; if that fails the migrator disables itself and logs. When used, it moves **two libraries, Brigadier and Authlib**, off the system class loader: it finds the jar of a known class, removes its URL from the class path structures (closing and evicting matching loaders), and adds the same jar to the discovery pipeline as a game library so it can be transformed. A failure after a library was removed is fatal.
- **`1.20.1` and `1.21.x`** (`hacks/`, 163 lines on `1.21.x`): `moveModule(name)` moves a **named module** from ModLauncher's boot layer to the game layer: it replaces the old module's data provider with an empty one, empties the descriptor's package set through a private field, builds a new jar for the game layer, and has an `addReads` helper using `Module.implAddReads`.

### 9.5 Differences between branches (read as differences of the files, SY2, SY3, SY15)

- **`SplitPackageMerger`, `1.20.1` to `1.21.x`** (29 changed lines): `1.21.x` adds the base filter that drops `META-INF/versions` and combines it with the package filter by AND; the package sets come from the module descriptor; filter types change from a two-argument predicate to the union file system's path filter; metadata type moved out of `JarTransformer`.
- **`1.21.x` to `26.1.x`** (139 changed lines): the same algorithm over new types (`JarContents`, `JarModuleInfo`, an input record with the file type so that **libraries are skipped**, jars keyed by path or checksum). One real change: the "existing packages" check used the packages of the modules in ModLauncher's **boot and service layers** on `1.21.x`; `26.1.x` uses the packages of the **unnamed modules along the class loader chain** of FML's current loader.
- **`PackageTracker`:** 4 to 6 changed lines across branches (only types).
- **`ForgeModPackageFilter` (`1.20.1` and `1.21.x` only; 22 changed lines apart):** the comment says some packages Connector ships (mainly from the Fabric loader) are also included, unrelocated, by other Forge mods. Relocating them would mean rewriting every use in Fabric mods and could break reflection, so the "slightly hacky" solution is to **filter those packages out of the other mods' jars**: it takes the packages of Connector's own module, intersects them with each other FML mod file's packages, and installs a path filter (a `PackageTracker`) into that jar's union file system through private fields, then forces the jar to recompute its packages. **It has no counterpart on `26.1.x`** (the file does not exist there); whether the problem disappeared or is solved elsewhere is unverified.
- **`ModuleLayerMigrator`:** on `1.20.1` the class defines its own `Unsafe`-based trusted lookup and **throws** when a module cannot be made transformable; on `1.21.x` that lookup moved to `ConnectorUtil`, the new data provider is created **before** the old module's packages are emptied, a failure is logged and `null` is returned, and an `addReads` helper is new. `26.1.x` is a different mechanism (section 9.4).

---

## 10. Stripping the client or server environment (row 10)

### 10.1 What is removed (evidenced, SY15)

`EnvironmentStripperTransformer` implements the Adapter's class-transformer interface and is built with the current environment type. On the **class-retention** annotations of a class it:
1. removes **methods** annotated with Fabric's `@Environment` for the other side, together with the synthetic `lambda$` methods they reach through `invokedynamic` handles (followed transitively);
2. removes **fields** annotated the same way;
3. removes the **interfaces** named by `@EnvironmentInterface` or `@EnvironmentInterfaces` for the other side.

The code comment says the annotations are processed ahead of time "to avoid class resolution issues leading to CNFEs". An annotation whose value array does not have the expected shape raises an error.

### 10.2 Where it runs and what it does not do

It runs inside `MixinPatchTransformer` for every class that is **not** a Mixin class (section 6.2). It does **not** remove whole classes marked for the other side, and the mod-level environment filter is a separate test in the locator (section 2.2 step 4). A search of the cloned Connector, Adapter, Launchpad and Forgified Fabric Loader sources for the environment annotation classes found no other code that acts on them (the Forgified Fabric Loader only defines the annotations); Connector and Launchpad use the environment type for the mod-level filter and for entrypoints. So class-level removal does not exist in these repositories (annotated classes that are never loaded on the other side simply stay in the jar).

### 10.3 Differences between branches

`1.21.x` and `26.1.x`: identical (0 changed lines). `1.20.1`: 25 changed lines, caused by an older Adapter API (`ClassTransform`, `Patch.Result`) and a static current-environment field read from the Fabric loader instead of a constructor argument (SY3, SY15).

---

## 11. What rows 1 to 10 mean for VESSEL (first reading)

Written out in `design-lessons-for-vessel.md` (version 0.2.0). In one line each:
1. The host loader changes per Minecraft line (Forge for 1.20.1, NeoForge afterwards); VESSEL's era model already has to cover both.
2. "Make a foreign jar look native" has three proven techniques; the shared parts are the rules, not the descriptors.
3. A per-jar transform with a content-hash cache, a cache-version key and a clean game jar downloaded at run time is a working pattern.
4. Remapping is a **version-era feature**: needed on 1.20.1 and 1.21.x, gone on 26.1.x.
5. Mixin compatibility is handled by comparing **vanilla against host-modified** classes and rewriting only the injections that no longer fit, plus a short hand-written exception table; failures are reported once at startup, with an off switch.
6. The host-side "class transform" registration API changed between eras while the list of compatibility problems (module layers, FabricASM, enum extension) stayed.
7. Split packages are resolved by a written ownership rule; the problem exists because of the host's module system.

---

## 12. What is still unread inside rows 1 to 10 (why they are `PARTIAL`)

- **Row 1:** the full list of the organization's repositories (`FAPI-Mojmaps`, the template and tooling repositories); the content of branches not cloned (Adapter `1.20.1` and `1.21.x`, ConnectorExtras `1.20.1` and `1.21.x`, Launchpad `26.1.x` and `master`, ForgifiedFabricLoader `1.19.4`, `1.20.4`, `1.20.6`); release and tag naming (row 24).
- **Row 2:** `DependencyResolver`, `EmbeddedDependencies`, `ConnectorEarlyLocator`, `ConnectorEarlyLocatorBootstrap`, `ConnectorEarlyLoader`; Launchpad's own reader and `FabricModFactory` (row 11).
- **Row 3:** `ConnectorLoaderModMetadata` and `ConnectorFabricModMetadata`; `FabricModMetadataParser` of `1.21.x` was compared by key lines only.
- **Row 4:** `JarTransformer` of `1.20.1` and `1.21.x`; `TransformerUtil` beyond the cache and keyword parts; `EarlyCoremodTransformer`; the `runner` source set of `26.1.x`.
- **Row 5:** the `1.20.1` renamer files line by line; `ReflectionRenamingTransformer`; `AccessWidenerTransformer`; the second half of `OptimizedRenamingTransformer`.
- **Row 6:** `FieldTypeUsageTransformer`; the audit trail internals; the Mixin patch transformer of `1.20.1` beyond its line count; `FabricMixinBootstrap` of `1.21.x` (seen only by header and comment).
- **Row 7:** the resolvers, processors and analysis code (about 9,000 lines); the `1.20.1` and `1.21.x` branches; how the generated adapter Mixin jar is produced; the `runtime` and `test` modules in detail.
- **Row 8:** `ConnectorForkJoinThreadFactory`; the bodies of the `1.20.1` and `1.21.x` service classes (section 8.4).
- **Row 9:** complete at the medium depth: nothing left unread that the row names (the exact `26.1.x` equivalent of `ForgeModPackageFilter` is unverified, section 9.5).
- **Row 10:** complete at the medium depth.

---

## 13. Sources

| Id | Rating | Read as | Source | Used for |
|---|---|---|---|---|
| SY1 | A | read | `Sinytra/Connector` branch `26.1.x` at commit `67b3cc58278696ddc8c6ae6515a0de52266258ab` (2026-09-01): `README.md`, `LICENSE`, `gradle.properties`, `settings.gradle.kts`, `build.gradle.kts` (greps), loader-side Java (`locator/*`, `service/*` names, `util/ConnectorUtil.java`, `util/GameCodeRetriever.java`), `transformer` module (`jar/JarTransformer.java`, `jar/JarTransformInstance.java`, `jar/MetadataReader.java`, `jar/FabricModFileMetadata.java`, `transform/FabricMetadataTransformer.java`, `transform/TransformerUtil.java`, `plugin/BuiltinConnectorPlugin.java`, `plugin/TransformerRegistrarImpl.java`, `api/TransformerIds.java`, `TransformerEnvironment.java`) | Sections 1 to 5 |
| SY2 | A | read in part | `Sinytra/Connector` branch `1.21.x` at `5a2f668ad492c1586c279a197423f5a1d1a16509` (2026-09-26): `README.md`, `LICENSE`, `gradle.properties`, file lists, `locator/FabricModsDiscoverer.java`, `locator/ConnectorLocator.java` (key parts), `locator/FabricModMetadataParser.java` (key lines), `transformer/jar/JarTransformInstance.java`, `jar/IntermediateMapping.java`, `transform/OptimizedRenamingTransformer.java` (first 130 lines and structure), `transform/MappingAwareReferenceMapper.java`, `transform/RefmapRemapper.java` | Sections 2, 3, 4.5, 5 |
| SY3 | A | read in part | `Sinytra/Connector` branch `1.20.1` at `7f68ac02291fde986119a3f5cab85436bed5c350` (2026-09-13): `README.md`, `LICENSE`, `gradle.properties`, `build.gradle.kts` (greps), file list, `locator/ConnectorLocator.java` (lines 62 to 160), `locator/ConnectorModMetadataParser.java`, `transformer/ModMetadataGenerator.java`, greps of `transformer/jar/JarTransformer.java` and `JarTransformInstance.java` | Sections 1.2, 2, 3, 5 |
| SY4 | A | read in part | `Sinytra/Adapter` branch `26.1.x` at `60fdfcdfc5dc04c5911c2b674a55f6f5bd3beeab` (2026-08-14): `LICENSE` only | Section 1 |
| SY5 | A | read in part | `Sinytra/ForgifiedFabricLoader` branches `26.1.x` (`41d95140890c0e333d7b6e69ae33005fe98067e0`, 2026-10-09), `1.21.x` (`29b610cb7394e5702a6b99b5081218186acc8c6f`, 2026-07-04), `1.20.1` (`fa2e368c07f538cc09b127a6f399c67c4147fbd0`, 2026-07-04): `LICENSE`, `MappingResolverImpl.java` (all three), `build.gradle.kts` of `1.21.x` (lines 205 to 290) | Sections 1, 5 |
| SY6 | A | read in part | `Sinytra/Launchpad` branch `26.2.x` at `e3c9899aa888c5e8685a4a709718744b7b52babb` (2026-10-09): `LICENSE`, `README.md` licence section, `api/Constants.java` (`ENABLE_LAUNCHPAD`), grep of `impl/FabricModMetadata.java` | Sections 1, 3 |
| SY7 | A | read in part | `Sinytra/ForgifiedFabricAPI` branch `26.2.x` at `fb52e7cd925fc0b3e7a9426ce291da28cbfa35d1` (2026-10-09): `LICENSE`; size | Section 1 |
| SY8 | A | read in part | `Sinytra/ConnectorExtras` branch `26.1.x` at `0f36aae25d17ef9d07f010e9fbfe24a543c04e60` (2026-09-13): `LICENSE` | Section 1 |
| SY9 | A | run | `git ls-remote --heads` and `--tags` of the six repositories, 2026-10-09 | Section 1 |
| SY10 | B | excerpt | The `github.com/Sinytra/Connector` page text as returned by a web search (2026-10-09): README statements (runs Fabric mods on NeoForge; MIT "will always remain"; mods must be mapped to Intermediary) | Sections 0, 1, 5 |
| SY11 | B | excerpt | `moddedmc.wiki/en/project/connector/latest/docs` and its introduction page, through web search (2026-10-09): install Connector, Forgified Fabric API and Launchpad | Section 1 |
| SY12 | C | excerpt | A third-party mirror of the Sinytra organization page (`gitblind.noratr.app/Sinytra`): pinned repositories, "10 of 22 repositories" | Section 1.6 |
| SY13 | B | excerpt | `github.com/Sinytra` organization page text through web search: repository names, "10 of 20 repositories" | Section 1.6 |
| SY14 | A | attempted, failed | GitHub REST API `orgs/Sinytra/repos` (rate limit exceeded); no fact taken from it | Section 1.6 |
| SY15 | A | read | `Sinytra/Connector` branch `26.1.x` at `67b3cc58278696ddc8c6ae6515a0de52266258ab`, pass A2: `service/coremod/ConnectorClassProcessor.java`, `ConnectorMethodProcessor.java`, `FabricASMClassGenerator.java`, `service/ConnectorPrelaunch.java`, `service/ModuleLayerMigrator.java`, `locator/filter/SplitPackageMerger.java`, `locator/filter/PackageTracker.java`, `locator/MixinTransformSafeguard.java`, transformer module: `transform/MixinPatchTransformer.java`, `transform/MixinPatches.java`, `patch/EnvironmentStripperTransformer.java`, `patch/RefmapStorage.java`, `patch/ConnectorRefmapHolder.java`; plus normalized `diff` of the same-named files of `1.21.x` and `1.20.1`, and file lists of `1.21.x` and `1.20.1` service folders | Sections 6, 8, 9, 10 |
| SY16 | A | read in part | `Sinytra/Adapter` branch `26.1.x` at `60fdfcdfc5dc04c5911c2b674a55f6f5bd3beeab`, pass A2: `settings.gradle.kts`, `gradle.properties`, package and file-size survey, `patch/Patcher.java`, `patch/DynamicPatches.java`, `transform/PipelineMethodTransformer.java`, listing of `transform/` and `runtime/` | Section 7 |
