# Design lessons for VESSEL from the Sinytra Connector study

**Document version:** 0.2.0
**Date:** 2026-10-10 (passes A1 and A2; grows with every pass of stage A)
**Status:** `PARTIAL`: only the lessons of rows 1 to 10 are here. The study itself is `research.md` in this folder; this file says what to take from it.

How to read this file: every lesson names the finding it comes from (a section of `research.md`). Everything here is a **proposal** or an **open question**, never a decision; decisions go into section 2 of `MASTER-SPEC-STATUS.md`. VESSEL differs from Connector at the root: Connector transforms Fabric mods in place so they run on a Forge or NeoForge host; VESSEL, as decided, converts modules into a shared IR and runs them on its own server software. Row 26 will compare the two models in full; Shiroz's working hypothesis ("VESSEL is basically Connector with more loaders") is kept until then.

---

## 1. Lessons

**L1. The host loader changes at a Minecraft line, and the project's branches follow it** (`research.md` 1.2).
Connector's `1.20.1` branch runs on Forge; its `1.21.x` and `26.1.x` branches run on NeoForge. VESSEL's era model (decision 34) already separates Forge eras; the lesson is that "which loader family hosts a Fabric mod" is itself era-dependent. Proposal: the capability profile of an era names the loader family that was dominant for server mods in that era, so the source adapters for Fabric can be tested against the loader the era's players used.

**L2. A foreign jar is recognized by one file, and a marker lets a jar be "both"** (`research.md` 2.2, 2.3).
Detection is the presence of `fabric.mod.json` plus the absence of a real native descriptor; a placeholder descriptor (a property in the native descriptor) is the agreed marker for "this is a Fabric mod posing as native". Proposal: VESSEL's discovery should classify a jar by the set of descriptors it carries (module detection key per loader) and record, not guess, when a jar carries two (compare identity rules in `docs/architecture/identity-and-collision.md`).

**L3. Duplicate resolution is a written rule, not an accident** (`research.md` 2.2 steps 6 and 7).
Rule used: highest version wins; among equal versions the **lowest nesting level** wins; a native mod of the same normalized id beats the Fabric one unless a library holds the id and the Fabric version is newer. Proposal: adopt "version, then nesting, then native-first" as the starting rule for VESSEL's duplicate module ids (v3 point #490 says a duplicate id is a solver conflict; this gives the solver a tie-break to propose, still subject to the admin).

**L4. Module id normalization and aliasing exist for a runtime reason** (`research.md` 3.1).
Connector rewrites the id (`-` to `_`, a suffix for Java reserved words) because the host loader uses Java module names, and keeps the original id resolvable through `provides`. VESSEL runs on its own server software, so the Java-module reason may not apply, but **the alias-table and the dependency-alias idea do** (a config table mapping known pairs of ids, for example `cloth_config` to `cloth-config2`). **Open question for the identity file:** decision 29 says a namespaced registry id is never merged by alias; module ids are a different space, but the identity file must state that module-id aliasing (admin-controlled) does not weaken decision 29.

**L5. Entrypoint filtering is needed for libraries that start themselves** (`research.md` 3.1, item 3).
MixinExtras' bootstrap entrypoint must not run twice. Proposal: VESSEL keeps a small "never run" list of entrypoints with the reason stored next to each (as v3 point #389 asks, no hard-coding of module ids in the core: the list lives in a config or a converter registry).

**L6. Make-it-native has three techniques; VESSEL needs none of the host-descriptor ones, but needs their rules** (`research.md` 3.4).
Stub reader (`26.1.x`), rewritten Fabric JSON (`26.1.x`), synthetic native descriptor built in memory (`1.20.1`, `1.21.x`), including a generated `@Mod` class on Forge. For VESSEL's Forge adapter on 1.20.1 and earlier eras the generated-entrypoint trick is relevant knowledge: it shows what FML needs to treat a jar as a mod with code. For the IR approach the lesson is only that **environment, version validity and URL fields of the Fabric metadata need validation rules** (a version that does not start with a digit, an invalid URL).

**L7. A per-jar transform should be cached by content hash plus a version key, with a sidecar record written last** (`research.md` 4.3).
The sidecar holds cache-version plus SHA-256 of the input; it is written only after success; a missing version key forces a miss with a random value. This matches v3 points #370 and #374 (conversion cache key and invalidation). Proposal: adopt the same "sidecar written last" rule, which gives crash safety for free, and include in the key what v3 #370 lists.

**L8. Download the clean game jar at run time and cache it** (`research.md` 4.4).
Connector fetches the vanilla jar of the exact version and side from Mojang's launcher manifest and caches it. This is the same pattern as decision 33 (mapping data downloaded from Mojang at run time, cached, not bundled), applied to the game code. Proposal: VESSEL's "clean class lookup" for any patch analysis uses the same fetch-and-cache service as the mapping data, with the same admin override (a file the server owner provides).

**L9. Transformer ordering by declared constraints, not by list position** (`research.md` 4.2).
Registrations name what they run before or after; missing targets are ignored; ties break by hint then name; duplicate names are an error. Proposal: VESSEL's converter and compiler stages (v3 #263 to #265) use the same registration style, so a specialized converter can order itself relative to generic ones without editing the core (v3 #389 and #390).

**L10. Remapping is a version-era capability** (`research.md` 5.1, 5.4).
Needed on 1.20.1 (Intermediary to SRG) and 1.21.x (Intermediary to Mojang), absent on 26.1.x (identity). Proposal: the era profile has a field "Fabric mods carry Intermediary names: yes or no" and a field "runtime names: SRG, Mojang or the game's own"; the remap stage runs only when the first is yes. This follows decisions 32 and 33.

**L11. Remapping touches more than classes** (`research.md` 5.3).
Strings in annotations, constants, `invokedynamic` arguments, refmaps (with the environment key swapped) and reflection strings all needed work in Connector's renamer, including a special case for ambiguous overrides. Proposal: VESSEL's mapping layer must list these as separate cases in its test matrix (v3 #403 to #406 already ask for descriptor mapping; the string cases are not named there).

**L12. The mapping data is built, not just downloaded** (`research.md` 5.2).
The Forgified Fabric Loader merges Mojang's mapping file with Fabric's Intermediary at build time and ships the result in its jar. VESSEL's decision 33 forbids bundling Mojang data. **Open question for stage C (Fabric research) and the distribution spec:** where does VESSEL get the **Intermediary** names for Fabric mods of the eras that need them, under what licence, and is the merge done at run time from two downloaded sources?

**L13. Do not rely on host internals more than necessary** (`research.md` 2.2, 4.1; evidence of reflection and private lookups).
Connector reads a private field of the discovery pipeline by reflection, builds a private class-info provider through a method handle, clears Mixin's static cache through a private variable handle, and uses `Unsafe` to get a trusted lookup (`ConnectorUtil`). These are fragile across host versions. Proposal: since VESSEL owns the server software, its Forge adapter should reach FML through its documented extension points wherever one exists, and keep each unavoidable private access in one named, versioned shim so an upstream change fails loudly and in one place.

**L14. Compatibility shims for host and vanilla classes are hard-coded per Minecraft line** (`research.md` 8.1).
Connector's class and method processors name exact vanilla and host classes (`KeyMapping`, `CreativeModeTab`, `LivingEntity.forceAddEffect`, and others) and exact fixes. Nothing selects them by version inside the code; the branch is the version. Proposal: VESSEL keeps such shims as **data keyed by era** (a table in the adapter of that era, one test per entry, each entry with the reason it exists), so an era's shims can be reviewed and removed as a unit.

**L15. The core of Mixin compatibility is a comparison of vanilla against the host-modified game** (`research.md` 7.2 to 7.4).
The Adapter library finds the injections that no longer fit, builds a corrected configuration with resolvers and processors, and falls back to a short hand-written exception table (`research.md` 7.6). VESSEL's v3 text (points #167 to #171) asks for Mixin **intent** to be turned into a semantic hook, not copied. The Adapter's analysis is the nearest prior art for extracting that intent. **Open question:** may VESSEL reuse or port Adapter code (MIT), and does VESSEL even have a "host-modified" game to compare against (its own server software may be the only game)?

**L16. Fail loudly once, with an off switch** (`research.md` 6.5).
The safeguard stops the start with a readable list of the failing Mixins and says how to disable the check. This is the strict end of v3 points #520 (an unsupported feature never disappears silently) and #385 (admin overrides). Proposal: VESSEL's default for a module with a failing conversion is a report and a reduced feature set (v3 #304, #608), with a **strict mode** that behaves like this safeguard.

**L17. Keep an audit report as a file, with class and method** (`research.md` 4.1, 6.5).
`patch_audit.txt` plus the in-game list match v3 point #310. Proposal: adopt the shape "module, class, method, reason" for every unsupported finding.

**L18. Split-package handling is a consequence of the host's module system** (`research.md` 9.1, 9.2).
The first jar found owns a package and the others give only their files of it; packages already present on the class path or in loaded mods are removed from the Fabric jars; a hack filters Connector's own packages out of other Forge mods on older branches. If VESSEL gives every module its own class loader (v3 points #289 to #293), splits **between modules** are legal and the rule is needed only for duplicates **inside one module** (merged jars, nested jars). **Open question:** does VESSEL's Forge adapter, which must run Forge mods on FML-style infrastructure, inherit the module constraint?

**L19. Private JDK access: disable yourself early, fail hard only after the point of no return** (`research.md` 9.4).
The `26.1.x` migrator turns itself off if its private lookups fail, and throws only after a library was already removed from the system class loader. Proposal: any VESSEL shim that needs private access follows the same two-stage rule and is listed in one place (extends L13).

**L20. Stripping the other side's members needs three companion rules** (`research.md` 6.3, 10.1).
Remove members and interfaces by annotation, also remove the synthetic lambda methods they reach, and remove `META-INF/services` lines whose class no longer exists. Whole annotated classes are not removed. Proposal: VESSEL's side-split (v3 points #354 to #358) uses the same three rules for server conversion, and adds class-level removal where the class is client-only (the study found none in Connector).

**L21. Record where borrowed code comes from** (`research.md` 9.3, 6.7).
Connector's repository contains files derived from other projects, each with its original header (an LGPL v3 header on `PackageTracker`, Fabric's Apache-2.0 header and a pinned commit link on `FabricMixinBootstrap`). Proposal: VESSEL records the provenance and licence of every borrowed snippet in a table (v3 point #183 asks for licence storage for modules; this is the same discipline for VESSEL's own sources).

---

## 2. Open questions collected so far

1. Module-id aliasing versus decision 29 (L4): identity file.
2. Where VESSEL gets Intermediary names and under what licence (L12): stage C Fabric research and the distribution spec.
3. Whether Launchpad's code may be embedded, linked or only studied (GPL v3 with Classpath Exception): row 25.
4. What replaces `DependencyResolver` on `26.1.x` (`research.md` 2.3): row 18.
5. May VESSEL reuse or port Adapter code, and what would it compare against (L15): rows 7 and 25.
6. Does VESSEL's own runtime inherit the module constraint that forces split-package handling (L18): the Forge research of the adapter design.
7. Which mechanism replaces `ForgeModPackageFilter` on `26.1.x` (`research.md` 9.5): row 18 or 11.
