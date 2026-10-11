# VESSEL — Master Specification Status

**Document version:** 1.0.50
**Date:** 2026-10-09 (first written 2026-09-29; the version number above is the current one)
**Purpose:** A single, honest map of the whole VESSEL project: what is done, what is in
progress, and what is planned, with pointers to the detailed file for each item. This
file does not replace the original 625-point Full Master Engineering Specification v3
that Shiroz provided — it tracks status against it and against the architecture
decisions made since. It also fully supersedes the original document wherever the two
disagree (see section 2).

**How to use this file:** start here, then open the specific file named next to the
item you care about. Do not assume anything not explicitly marked DONE is finished.

---

## 1. What "done" means here

- **DONE (with evidence)**: written into a file in this package, backed by cited
  official or credible sources, or an explicit decision Shiroz made in chat.
- **PARTIAL**: some real, cited content exists, but the file itself says what is
  still missing. Treat as a foundation, not a finished spec.
- **NOT STARTED**: no research or design has happened yet. Nothing should be assumed
  about it beyond what the original v3 document already said.

No percentage in this file is a feeling. Every estimate is a count against a fixed
list of sub-items, shown next to the number.

---

## 2. Architecture decisions that supersede the original v3 spec

These are firm, chat-approved changes. Anywhere the original v3 spec (sections
referenced below) conflicts with these, **these win**.

| # | Decision | Replaces v3 section(s) | Detail file |
|---|---|---|---|
| 1 | VESSEL builds every needed render bundle (resource pack, data pack, texture pack, Bedrock addon) as its own **Startup Build Stage**, eagerly, before world load and port open — not lazily on first player join | #414 (Server Startup Order) | `docs/architecture/startup-build-stage.md` |
| 2 | First-ever startup with no CLI flag: a mandatory, non-configurable 45-second English warning, then the process exits without building. Later startups build automatically. A `--vessel-version="26.2"` flag (repeatable/comma-list) always skips this and scopes the build | none in v3 (new mechanism) | `docs/architecture/startup-build-stage.md` |
| 3 | An interrupted build discards only the in-progress version combination; finished combinations are kept | none in v3 (new mechanism) | `docs/architecture/startup-build-stage.md` |
| 4 | New `vessel-config/` folder, sibling to server root, holds every VESSEL-specific setting, replacing the scattered `config/universal-loader.yml` etc. from v3 | #545 (config files) | `docs/architecture/startup-build-stage.md` section 9 |
| 5 | Module identity is the pair **(source loader, module id)**, not just id. Source loaders: `fabric`, `quilt`, `forge`, `neoforge`, `plugins` | #487-#491 | `docs/architecture/identity-and-collision.md` |
| 6 | Module data is split **per source loader**: `data/fabric/<id>/`, `data/forge/<id>/`, etc., replacing the shared `data/mods/<id>/`. Same split applies to cache, generated, logs, temp, libraries, backups, crash reports | #10, #13, #65-#66, #142-#150, #550, #601-#602 (every use of `data/mods/`) | `docs/architecture/identity-and-collision.md` section 3 — **full spec re-audit for this is still open, see section 5 below** |
| 7 | Same id, different loader (e.g. a plugin and a mod both called `worldedit`): **both load and run**, yellow console warning, documented as a usage caveat, not an error | #490 (used to be a conflict) | `docs/architecture/identity-and-collision.md` section 4 |
| 8 | Same id **and** same loader, or same key in the same registry: **hard stop**, red error naming both modules and both JAR paths, non-zero exit. This is confirmed to be **stricter than Forge's own behavior** (Forge silently overrides on a duplicate registry key from the 1.12.x head on; in 1.7.10 Forge itself throws on a duplicate name, so there VESSEL is not stricter: `research.md` 34.4, `identity-and-collision.md` 5.4 v0.1.9) — a deliberate VESSEL choice | #490-#491 | `docs/architecture/identity-and-collision.md` section 5, corrected in v0.1.1 against `docs/upstream/forge/research.md` section 6, and re-verified against the Forge 26.3 source on 2026-09-30 (`research.md` section 27.3) |
| 9 | Fabric vs. Quilt classification is by which API the mod's **bytecode actually calls**, not just metadata: any Quilt-specific API call means `quilt`, even if Fabric API is also called | none in v3 (new mechanism) | `docs/architecture/identity-and-collision.md` section 2.1 |
| 10 | Geyser is bundled inside VESSEL's own distribution (latest official release channel only, never beta/alpha), auto-refreshed on every VESSEL release; VESSEL auto-detects an externally-installed Geyser (plugin/mod id match, then Bedrock port already bound) and disables its own bundled copy; the bundled Geyser's files stay in Geyser's own native paths, an explicit exception to VESSEL's virtual-filesystem rules | #257-#258 (Geyser integration) | Chat decision only, not yet in a dedicated file — **planned**, see section 6 |
| 11 | Network sync for a shared dynamic object (e.g. a moving contraption): one broadcast packet per state change to every tracking player, plus delta compression, plus a one-time full-state packet for a late-joining tracker. Never recomputed per player | #207-#208, #252-#253 | Chat decision only, not yet in a dedicated file — **planned**, see section 6 |
| 12 | VESSEL versions itself independently of any Minecraft version, the same way Fabric Loader does | none in v3 (new framing) | `docs/architecture/startup-build-stage.md` section 1 |
| 13 | Version support scope: **every version of every loader from its very first release onward**, including very old ones (confirmed example: Forge exists from Minecraft 1.1, 78 official versions total) — wider than v3's original "currently maintained only" framing | #2, #4 (Version Status Model) | `docs/upstream/forge/research.md` section 1 |
| 14 | A mod built for a newer version than VESSEL knows about: warning. A mod calling a function VESSEL has not yet mapped: error and crash stating the reason. Every VESSEL release updates source support for ALL loaders it supports, and that update is recorded in the docs. Example given by Shiroz on 2026-09-29: if a loader had version A at the previous VESSEL release and version B has appeared since, the new release must add B | none in v3 (new policy) | Chat decision only, not yet in a dedicated file — **planned**, see section 6 |
| 15 | Whole project (configs, comments, docs, logs) is written in English; only chat with Shiroz is Vietnamese. Console colors: white normal, yellow warning, red error | none in v3 (new policy) | Applied throughout every file in this package |
| 16 | A Latest-without-Recommended loader build (Forge 26.3 today) is **not** stable in the Version Registry | none in v3 (new policy) | `docs/upstream/forge/research.md` section 18.3 (decided 2026-09-29: "not stable" only prints a yellow warning, it never blocks) |
| 17 | Duplicate registry keys: scan the WHOLE module set before anything loads. Any duplicate means an error and no startup at all. Only when there is none does loading begin, then render bundles are created for joining players. Supersedes the "runtime pass" part of decision 8 | #490-#491 | `docs/architecture/identity-and-collision.md` section 5.1 (v0.1.3). Scan method decided 2026-09-29: static scan plus an isolated dry run for modules that build names in code, with required console reporting |
| 18 | A Forge-source data condition (for example `forge:mod_loaded`) matches only modules whose source loader is `forge`; same principle is the default for other loaders | none in v3 (new mechanism) | `docs/architecture/identity-and-collision.md` section 5.2 (v0.1.4) |
| 19 | Registries are complete before the Startup Build Stage (step 10 runs each module's registration, no world, no port); VESSEL merges tags itself; mod code keeps running on the server for loot modifiers and code-only recipes; only client-visible parts go into the bundle | #414 refinement | `docs/architecture/startup-build-stage.md` section 2.1 (v0.1.2). Shiroz delegated this choice on 2026-09-29 |
| 20 | Ids registered outside `RegisterEvent` (advancement trigger ids via `CriteriaTriggers#register`, ingredient and condition serializer ids via `CraftingHelper#register`) count as registry keys for the duplicate rule of decision 8 and decision 17 | none in v3 (new mechanism) | `docs/architecture/identity-and-collision.md` section 5.3 (v0.1.7). Scope note of 2026-09-30: for condition and ingredient serializer ids this special case exists only up to Forge 1.20.1; from the 1.20.4 branch on they are ordinary registry keys (`research.md` 27.2); trigger ids: Forge 26.3 has no trigger registry or registration code, so VESSEL must apply the rule itself (`research.md` 28.5) **[Superseded in part 2026-10-03: from the 1.20.4 head on trigger ids are entries of the vanilla registry `TRIGGER_TYPES`, filled through `RegisterEvent`, so the special case is needed only up to 1.20.1; Forge adds no duplicate rule for that registry, so the rule is still VESSEL's (`research.md` 34.3, `identity-and-collision.md` 5.3 v0.1.9)]** |
| 21 | Client-side custom models (for example models from Forge's OBJ loader): simple ones are converted into vanilla JSON models, complex ones go through the Display/Model backend (phase 16). The simple/complex boundary is not defined yet | none in v3 (new policy) | `docs/upstream/forge/research.md` section 23.6 (v0.4.20) |
| 22 | Simple/complex line for client-side models (decision 21 detail): complex = needs code at draw time or per-item/per-position state (block entity renderers, item renderers, item override resolution, item-property models, color tint handlers) goes to the Display/Model backend; simple = static shapes that vanilla JSON holds exactly, converted to JSON; grey case (static model vanilla JSON cannot hold exactly) also goes to the backend, never approximated silently, and if the backend cannot hold it a best-effort approximation is allowed only with a yellow warning naming the model. The grey-case choice was delegated to Claude | none in v3 (new policy) | `docs/upstream/forge/research.md` sections 24.5 and 25.7 (v0.4.25) |
| 23 | Pre-1.8 metadata layer (Forge 1.1 to 1.7.10, decision 13 versions): metadata of vanilla blocks is decoded using what the vanilla code and Forge source define; metadata of mod blocks stays an **opaque integer** and is not converted to properties | none in v3 (new policy) | `docs/upstream/forge/research.md` sections 26.2 and 26.7 |
| 24 | Working practice: claims about 26.x taken from documentation are re-verified against the 26.x source code before VESSEL relies on them (a verification practice asked for by Shiroz on 2026-09-30, not a rule that code always overrides docs) | none in v3 (new policy) | `docs/upstream/forge/research.md` section 27 |
| 25 | Vanilla part of the pre-1.8 metadata layer: option B, a data-driven table shipped with VESSEL with a per-field confidence level (`corroborated`, `single_source`, `convention_unresolved`, `conflict`, `unknown`); only `corroborated` fields are converted, `single_source` converts with a yellow warning, the rest stay opaque like mod blocks. Third-party decompiled caches (option C) are reference only: a value is used only if another independent source proves it | none in v3 (new policy) | `docs/upstream/forge/research.md` sections 30.4 and 31, `docs/upstream/forge/vanilla-metadata-table.json` |
| 26 | Default configs of Forge mods: they are kept in `vessel-config/defaults/forge/`, and VESSEL applies Forge's copy-on-first-creation rule (a default file is copied into the config folder when the mod's config file is first created, as Forge does with its `defaultconfigs` folder). Claude's proposal, accepted by Shiroz on 2026-10-01 as option A. The layout and wiring inside that folder are not designed yet | none in v3 (new policy) | `docs/upstream/forge/research.md` section 29.2 and `docs/architecture/startup-build-stage.md` section 9 |
| 27 | Working practice for conflicting sources (Shiroz, 2026-10-01, about the old Forge eras but stated generally): look for a reputable source that can settle the conflict; if it still conflicts, mark it as a conflict and write down the reason; never guess. Same spirit as the `conflict` confidence level of decision 25 | none in v3 (new policy) | this file, section 9 (rules) |
| 28 | What counts as DONE for the not-started gameplay API rows whose vanilla code cannot be read (17 entities, 19 commands, 20 scheduler, 25 worldgen, 26 data generation). Shiroz answered "both" on 2026-10-02 (the first version of this row said 2026-10-01, a one-day error found from the file times and corrected in v1.0.35): a row needs the docs, the Forge-side 26.3 code **and** a source for the vanilla part (for example NeoForged or Fabric docs, evidence class B), so that vanilla internals are not left as a silent gap; and these rows are worked only after passes 19 to 21 (section 8), together with the old-era rows 30, 31 and 34 (the first version of this row left out row 34 by mistake; corrected in v1.0.35) | none in v3 (new policy) | this file, sections 4.1, 8 and 9 |
| 29 | Namespaced ids are never aliased across namespaces. `forge:x`, `c:x`, `fabric:x` and `neoforge:x` are completely different names (Shiroz, 2026-10-04, who gave the loader names as an example of the principle). VESSEL's tag merge (decision 19), registry scan and every other id lookup merge only entries with the identical full id; a tag that moved from `forge:` to `c:` in a newer Forge version is a different tag | none in v3 (new policy) | `docs/architecture/identity-and-collision.md` section 5.3 (v0.1.10), `research.md` 35.2 and 35.6 |
| 30 | Method for the 16 rows that are not DONE. Shiroz delegated all three choices on 2026-10-04 ("Giao Claude chon cach chia hop ly va ghi ly do"); the reasoning below is Claude's. (a) **Grouping:** pass 22 = rows 17, 19, 20; pass 23 = rows 25 and 26; pass 24 = rows 30, 31 and 34; passes 25 and 26 = the PARTIAL rows (re-planned in v1.0.43: pass 25 = 16, 20, 21, 22, 23, which finished 20, 21, 22 and 23 and left 16; pass 26 = 2, 10, 16, 29, 31, 32). The rows of a group share their sources (the 26.3 tree with NeoForged or Fabric docs; the old Forge branches; leftover reads), so each pass can be finished and reported whole. It is a plan, not a promise. (b) **Parts that cannot be obtained:** the fixed 37-row list and its rule stay (a row moves only when its whole scope is covered). Only a part **proven** unobtainable (dead link, no branch, sources still conflicting after a search for a reputable source, decision 27) lets a row count as DONE with the label "documented limit", worth 1.0, with the proof and the reason written in the row and such rows counted separately in every report; no row uses the label yet. Reason: otherwise a row with a permanently missing part could never reach DONE, while a silent DONE would hide the gap. (c) **Class B source per row:** NeoForged docs by default (same design lineage as Forge); Fabric docs when NeoForged has no page on the topic or Fabric states the vanilla part more clearly; the choice and its reason are written in the section of each row | none in v3 (new policy) | this file, sections 4.1, 4.3 and 8; `research.md` 36.0 |
| 31 | Entity selector tokens (`EntitySelectorManager.register`) and argument-type classes (`ArgumentTypeInfos.registerByClass`) count as registry keys for the duplicate rule of decisions 8, 17 and 20: two modules that register the same token, or an info for the same argument class, stop the server. Shiroz answered "co" on 2026-10-05 to the question of the pass 22 report; Forge itself overwrites both silently | none in v3 (new mechanism) | `docs/architecture/identity-and-collision.md` section 5.5 (v0.1.11), `research.md` 36.2 |
| 32 | A module whose code uses Mojang's official names must be loadable (Shiroz, 2026-10-05: any mod that uses the official Mojang mapping must also load). Recorded as a **requirement for all loaders**; the design is open: how VESSEL detects the name space of a module; which eras need translation (official names exist for 1.14.4 and later; Mojang announced on 2025-10-29 that new versions ship without obfuscation, so (inference) official names are the run-time names there); where the translation data comes from (answered by decision 33); and whether the requirement covers all loaders now or only Forge in phase 7 | none known in v3 (the v3 text is not in the package) | `research.md` 36.4, 36.7 and 37.3; section 5 updates of v1.0.40 and v1.0.41 |
| 33 | Source of the data for translating Mojang's official names (the follow-up of decision 32). Shiroz delegated the choice on 2026-10-05, asking that it preferably come from Mojang, that other sources be consulted, and that data taken from jars is acceptable; the reasoning is Claude's. Order: (1) unobfuscated versions (26.1 and later): no translation, official names are the run-time names; (2) obfuscated versions (1.14.4 to 1.21.11): Mojang's own mapping file for the exact game version, downloaded from the address in the version JSON on first need, kept in VESSEL's cache folder and **never shipped inside the VESSEL package** (the licence notices, known only from log excerpts, allow development use but not redistributing the complete unmodified file; the reference copy of the licence is gone, so its current wording is not verified); (3) modules in another name space on obfuscated versions (SRG, Intermediary, MCP, Yarn) need that name space's data as a second input, with the obfuscated names as the common key (design inference; SRG data: `joined.tsrg` per version in MCPConfig; Intermediary data not read); (4) data found in jars VESSEL already holds may fill gaps and help detect a module's name space but never overrides Mojang's file; a disagreement is a conflict under decision 27; (5) offline servers: the owner may supply the file in a named folder, checked against the game version; without data a module that needs translation is **not loaded, with a clear message**, never skipped silently. Open: name-space detection per module, hash and size fields of the version JSON entries, Intermediary file format and licence, Quilt, and the run-time names of Forge and NeoForge mods on obfuscated versions | none in v3 (new policy) | `research.md` 37.3, `docs/upstream/forge/source-map.json` (v0.1.10) |
| 34 | The line between the eras E1a and E1b moves from Minecraft 1.3.2 to **1.2.5** (Shiroz agreed on 2026-10-06 to the proposal of pass 24). E1a = Forge on top of a real ModLoader, Minecraft 1.1 to 1.2.4 (3 versions); E1b = FML with its own ModLoader-compatibility layer, 1.2.5 to 1.7.10 (20 versions); the total stays 78. Evidence: the FML tags `v2.0` to `v2.9` are for 1.2.5 and already contain `@Mod` and the ModLoader layer; the early Forge tags are a patch set over a real ModLoader. The assumption that Forge builds for 1.2.5 are FML-based was checked in pass 26 and is **contradicted**: the Forge line for 1.2.5 (conf update 2012-04-06) is ModLoader-based and older than FML 2.0 (2012-05-10), and FML joined Forge only in July 2012, for 1.3.x. The line stays at 1.2.5 as decided; the E1b adapter must also load FML-2.x mods for 1.2.5, while Forge-line mods for 1.2.5 are E1a-style (ModLoader) mods (`research.md` 40.4) **[The refinement was approved by Shiroz on 2026-10-09 (v1.0.46): the line stays at 1.2.5.]** | none in v3 (the era model is VESSEL's own grouping) | `research.md` section 2 (rows E1a and E1b), 38.1 and 39.6 |
| 35 | **Provisional, made by Claude on its own authority in pass 26; Shiroz can reverse either part.** (a) Row 2 (build numbers per version): the full list of Forge builds is a live data feed that VESSEL reads from the Maven metadata when needed, so it is not copied into the docs; the research keeps the latest and recommended builds, the shape of the feed and its traps (unsorted, suffixes, stale `latest` and `release`), and the rule that the first number of a Forge version follows the Minecraft version. (b) Row 16 (items): the list of vanilla data components is vanilla content read from the game's registry at run time, so it is outside the Forge research. Reversing one part returns that row to PARTIAL (count 36.5 of 37 = 98.6%); reversing both gives 36 of 37 = 97.3% **[Approved by Shiroz on 2026-10-09 ("Như vậy ok rồi", v1.0.46): no longer provisional; the count of 37 of 37 stands and rows 2 and 16 stay DONE.]** | none | `research.md` 40.5 and 40.6 |
| 36 | **Delegated to Claude by Shiroz on 2026-10-09** ("pick what is best for the loader, easy to upgrade and repair later, consider it carefully"); the reasoning is Claude's. The loader draft's single "Forge gate" (sections 2.1 to 2.3 and 49, and rule 7 of section 50) is split into two kinds of gate and is defined **for every loader X**, not only for Forge. **RG(X), the research gate:** the row table of X is at 100% (the label "documented limit" is allowed), every provisional decision it rests on is approved, and Shiroz has signed. **IG(X), the implementation gate:** for Forge the minimum Loader Core, the adapter, the runtime, the end-to-end tests and the regression tests, signed by Shiroz; for any other loader its adapter, runtime and tests. What each kind of work needs: specifications, the website specification, repository starter files and reference research such as the study of Sinytra Connector are never gated; research of a loader other than Forge needs RG(Forge) PASS and still follows his rule of finishing one loader before the next; writing an adapter or runtime code for a loader X other than Forge needs RG(X) PASS and IG(Forge) PASS; the Forge adapter and runtime need RG(Forge) PASS and follow the draft order (minimum Loader Core, adapter, runtime, end-to-end tests, distribution). The state of every gate is recomputed by a script from the row tables, never typed; a gate whose basis is reversed (a provisional decision or a count) becomes REOPENED until it is counted again. Reasons: one template serves every future loader (a new loader adds a row to the gate table, not new wording to the specification); the numbers cannot drift from the tables; a reversed provisional decision reopens its gate by rule instead of by memory; Sinytra is reference research and should come before the minimum Loader Core, because what it teaches can change the Core's boundary. The draft text is not changed here; the upgraded loader specification will carry the rule. **State on 2026-10-09: RG(Forge) is not signed (Shiroz answered "chưa"), IG(Forge) is not started; Shiroz's approval of the written definition is requested in the report of pass 27 and not yet given.** **[Shiroz answered "ok" to the written definition on 2026-10-09 (v1.0.47): the definition stands; RG(Forge) stays unsigned.]** | Forge gate, sections 2.1 to 2.3, 49 and 50 (rule 7) of the loader draft in `docs/drafts/` (a draft, not authoritative) | this file, section 3.1 |

---

## 3. Status by the original 23-phase plan (v3 section #614)

Each phase's percentage is this package's own honest estimate, explained in the note.
This is not a v3 percentage — v3 never assigned numbers to its own phases.

| Phase | Status | % | Note | Detail file |
|---|---|---|---|---|
| 1. Core + Version Registry | PARTIAL | 15% | Forge (deep) + Fabric (versions only) + Minecraft core versions and pack-format history done. NeoForge, Quilt, Bukkit family, Bedrock/Geyser not started | `docs/upstream/forge/research.md`, `docs/upstream/fabric/research.md` |
| 2. Universal Module Metadata | PARTIAL | 5% | Only Forge's two metadata systems (`@Mod`+`mcmod.info`, `mods.toml`) documented in prose. No cross-loader schema or parser designed | `docs/upstream/forge/research.md` sections 3, 3.1 |
| 3. Virtual Filesystem | PARTIAL | 10% | Per-loader directory split decided and written. No path interceptor, no resolver design, no code | `docs/architecture/identity-and-collision.md` section 3 |
| 4. Paper/Bukkit Plugin Adapter | NOT STARTED | 0% | | |
| 5. Plugin Data/Config Compatibility | NOT STARTED | 0% | | |
| 6. Fabric Adapter | PARTIAL | 5% | Only version numbers known (Loader 0.19.5, Minecraft 26.1-26.4). No API inventory | `docs/upstream/fabric/research.md` |
| 7. Forge Adapter | DONE (research part) | 100% | By far the deepest phase. Counted on a fixed 37-row list (section 4; pass 6 raised it from about 42% to about 47%, pass 7 to about 49%, pass 8 to about 50%, pass 9 to about 53%, pass 11 to about 55%, pass 12 to about 64%, pass 15 to about 68%; passes 16 to 21 left it unchanged; pass 22 to about 74%; pass 23 to about 80%; pass 24 to about 86%; pass 25 to about 92%; pass 26 to 100%); the old 28% did not match its own item count | `docs/upstream/forge/research.md` (v0.4.55), `docs/upstream/forge/version-index.json` (v0.1.3), `docs/upstream/forge/source-map.json` (v0.1.10), `docs/upstream/forge/metadata-inventory.json` (v0.1.0), `docs/upstream/forge/vanilla-metadata-table.json` (v0.1.0) |
| 8. NeoForge Adapter | NOT STARTED | 2% | Only glimpsed once for a config-timing comparison, no dedicated research | |
| 9. Quilt Adapter | NOT STARTED | 0% | | |
| 10. Folia Scheduler/Threading | NOT STARTED | 0% | | |
| 11. Canvas/Purpur | NOT STARTED | 0% | | |
| 12. UIR Core | NOT STARTED | 0% | Still v3's original design, not re-verified or implemented | |
| 13. Block/Item/Entity abstraction | NOT STARTED | 0% | Forge's own block API is documented (phase 7), but the universal abstraction across loaders is not designed | |
| 14. Resource Pack Compiler | PARTIAL | 3% | Pack format history researched (including the `min_format`/`max_format` change from 1.21.9). No compiler design | `docs/upstream/fabric/research.md` section 2 |
| 15. Datapack Compiler | NOT STARTED | 0% | | |
| 16. Display/Model/Animation backend | NOT STARTED | 0% | | |
| 17. Physics | NOT STARTED | 0% | | |
| 18. Dynamic Scene/Contraptions | NOT STARTED | 0% | Network sync principle decided (section 2, item 11), nothing else | |
| 19. Bytecode conversion | NOT STARTED | 0% | Forge's own bytecode/transform mechanisms are documented (phase 7), but the universal conversion engine is not designed | |
| 20. Mixin/Transformer semantic conversion | PARTIAL | 5% | Forge-side Mixin and access-transformer facts gathered. No cross-loader conversion design | `docs/upstream/forge/research.md` section 12 |
| 21. Geyser/Bedrock | NOT STARTED | 0% | Only the bundling policy decided (section 2, item 10), no Bedrock-side research at all | |
| 22. Advanced migration | NOT STARTED | 0% | | |
| 23. Full compatibility test matrix | NOT STARTED | 0% | | |

**Overall estimate: roughly 5% of the full 23-phase plan (about 6.3% after pass 26: the 23 phase percentages add up to 145, and 145 / 23 = 6.3; it was 6.0% after pass 25 (137 / 23), 5.7% after pass 24 (131 / 23), 5.4% after pass 23 (125 / 23), 5.2% after pass 22 (119 / 23) and 4.9% after pass 15 and unchanged after passes 16 to 21 (113 / 23); it was 4.7% after pass 12 (109 / 23) and unchanged after passes 13 and 14; it was 4.3% after pass 11 (100 / 23) and after pass 10 and pass 9 (98 / 23), 4.1% after pass 8, 4.1% after pass 7, 4.0% after pass 6, 3.8% after the pass 5 recount and 3.2% on the old 28% figure)**, equal-weighted. This
number is low because the denominator is the entire project, most of which (physics,
rendering, UIR, Bedrock, testing) has not been touched — not because the work done so
far is small in itself. Forge's own research file alone is now about 380 KB (measured: 379912 bytes at v0.4.55; it was 358111 bytes at v0.4.54, 340157 bytes at v0.4.53, 315204 bytes at v0.4.52, 288639 bytes at v0.4.51, 262794 bytes at v0.4.50, 262639 bytes at v0.4.49, it was 243906 bytes at v0.4.47, 218959 bytes at v0.4.45 and 199898 bytes at v0.4.41; the earlier "about 77 KB" was an estimate that was not measured).


### 3.1 Gate table (decision 36, added in v1.0.46)

Two kinds of gate exist for every loader X. **RG(X)**, the research gate: the row table of X is at 100% (the label "documented limit" is allowed), every provisional decision it rests on is approved, and Shiroz has signed. **IG(X)**, the implementation gate: for Forge the minimum Loader Core, the adapter, the runtime, the end-to-end tests and the regression tests, signed by Shiroz; for any other loader its adapter, runtime and tests. What each kind of work needs is in decision 36 (section 2). The states below are recomputed from the row tables by a script, never typed; a gate whose basis is reversed (a provisional decision or a count) is set to REOPENED until it is counted again.

| Loader | RG (research gate) | IG (implementation gate) | Note |
|---|---|---|---|
| Forge | NOT SIGNED: 37 of 37 rows DONE (script, 2026-10-09); decision 35 and the refinement of decision 34 approved on 2026-10-09; Shiroz answered "chưa" to the sign-off | NOT STARTED | research of the other loaders (stage C) waits for this signature |
| Fabric | NOT STARTED (phase 6: version numbers only) | LOCKED (needs RG(Fabric) and IG(Forge)) | |
| NeoForge | NOT STARTED (no dedicated research) | LOCKED | |
| Quilt | NOT STARTED | LOCKED | |
| Bukkit family (Spigot, Paper, Folia, Canvas, Purpur) | NOT STARTED | LOCKED | |
| Bedrock / Geyser | NOT STARTED | LOCKED | |

Reference work that no gate locks: the study of Sinytra Connector (stage A of the new chat) and the website specification (stage B).

---

## 4. Forge phase breakdown (the deepest phase, itemized)

### 4.1 Current count (the table is kept current; this heading and the first result paragraph date from v1.0.3, 2026-09-29)

Source: `docs/upstream/forge/research.md` v0.4.5 (the version when this list was fixed; the current version is in section 7). Method, fixed and stated: DONE = 1 point, PARTIAL =
0.5, anything else = 0, divided by the 37 rows below. The list is the 19 rows of the old table,
with the one lumped "gameplay API" row split into its 11 named parts (rows 16 to 26), plus
8 rows taken from section 15 of `research.md` that the old table did not show. Extending the list
lowers earlier percentages on purpose.

| # | Item | Status |
|---|---|---|
| 1 | Official version index (78 Minecraft versions, 1.1-26.3) | DONE |
| 2 | Build numbers per version | DONE through decision 35 (provisional when written, approved by Shiroz on 2026-10-09): the latest build for 77 of the 78 versions and the recommended build for 40 are recorded, with the shape of the Maven metadata and its traps (unsorted, suffixes, a stale `latest` and `release`) and the rule that the first number of a Forge version follows the Minecraft version; the full list of builds is a live feed that VESSEL reads when needed and is not copied into the docs (sections 1.2 and 40.5 of research.md); the Maven entries before 1.10.2 were not read |
| 3 | Era model (E1a/E1b/E2/E3/E4a/E4b/E5) | DONE, boundary uncertainty noted; the E1a/E1b line fixed at 1.2.5 by decision 34 |
| 4 | Old metadata (`@Mod` + `mcmod.info`) | DONE |
| 5 | `mods.toml` key list | DONE for the 1.19 to 1.21.x docs; the key set the Forge 26.3 loader reads was also extracted from source (section 27.4 of research.md), the file-level versus per-mod split was not read |
| 6 | Lifecycle events and threading (E1/E2/E3+) | DONE; lifecycle event class names and the nine `ModLoadingStage` values verified in the 26.3 source, the `@Mod` constructor rule verified for every branch head from 1.18.x to 26.3 (context or no argument; no-argument only at 1.18.x); threading and listener order now read in the 26.3 code (six parallel stages on a `ForkJoinPool`, `enqueueWork` queues run one by one on the loader thread, registration sequential in mod order) with the era boundaries of dispatch ordering and of the mod bus type; the loading code of 1.12.x and 1.17.x, the merged order of the state providers and the client loader thread were not read (sections 27.4, 32 and 34 of research.md) |
| 7 | Sides | DONE for the docs; in 26.3 `DistExecutor` does not exist and `@OnlyIn` in mod classes throws, so the docs describe only older versions (section 29 row 2 of research.md) |
| 8 | Registries, incl. Forge-vs-VESSEL duplicate difference | DONE; the duplicate-key behavior of Forge was re-verified in the 26.3 source (overrides allowed by default, debug log only; section 27.3 of research.md) and now also read for 1.12.x (same structure) and 1.7.10 (a duplicate name throws, overrides only through substitution aliases; section 34.4); the 1.8.9, 1.10.x and 1.11.x heads were checked by message counts only |
| 9 | Events (two buses, EventBus 6 vs 7) | DONE; EventBus 7 first appears at the head of branch 1.21.8; 26.3 uses `BusGroup` and `getModBusGroup()` instead of `getModEventBus()` (sections 26.5 and 29 of research.md) |
| 10 | Networking by era | DONE: networking read for every era (sections 8, 34, 36.3, 38.3 and 40.2 of research.md); at 26.3 the channel builder API, Forge's own channels, the configuration-phase tasks in order, the filter that keeps non-`minecraft` attributes away from vanilla connections, and the server list ping; the Forge docs describe the older channel API |
| 11 | Persistent data (capabilities, `SavedData`) | DONE for pre-26.x |
| 12 | Access transformers | DONE |
| 13 | Pre-ModLauncher coremods (`IFMLLoadingPlugin`) | DONE |
| 14 | ModLauncher `ITransformationService`/`ITransformer` | DONE |
| 15 | Gameplay API: blocks | DONE for the docs scope (Blocks introduction and Block States both read in full from the "latest" docs; unverified for 26.x; sections 16 and 25.1 of research.md) |
| 16 | Gameplay API: items | DONE through decision 35 (provisional when written, approved by Shiroz on 2026-10-09) for the vanilla component list: the Forge 1.21.x docs have two item pages (read) and none on components or use behavior (a finding); the 26.3 hooks read by call site (`Item`, `ItemStack`, both game modes, `Player`), `IForgeItem` and `IForgeItemStack`, the component-gathering event; the NeoForged left-click and right-click pipelines read in full and the component page by headings (class B); the list of vanilla data components is read from the game's registry at run time (sections 22.2, 24, 32, 33, 39.5 and 40.6 of research.md) |
| 17 | Gameplay API: entities | DONE for the docs scope plus the 26.3 source and class B text (decision 28): "Synchronizing Entities" (the only entity page of the Forge docs) read in full from the 1.21.x docs; the 26.3 code for entity types, spawn data, synched data and serializers, attributes, spawn placements, join and leave events and part entities read; NeoForged 26.1 entity pages read in full as class B; the 1.21.x docs are out of date on `defineSynchedData` (section 36.1 of research.md). Not read: the bodies of the 55 living and player event classes (listed by name), renderers belong to row 21, capabilities to row 34 |
| 18 | Gameplay API: block entities | DONE for the docs scope (Block Entities introduction and BlockEntityRenderer read in full from the "latest" docs); the 26.3 source shows the docs are stale: `ValueInput` based loading and a render-state renderer; sections 23.1, 24.1, 25.4 and 26.4 of research.md |
| 19 | Gameplay API: commands | DONE (decision 28: docs, code, class B): no commands page exists in the Forge docs (a finding, like row 28); the 26.3 code read (registration event, execution hook, vanilla-client filter of mod argument types, selectors, client commands, permission API); Fabric 26.2 command guides read in full as class B (section 36.2). Not read: the call sites of the selector hook and the bodies of the `/forge` subcommands |
| 20 | Gameplay API: scheduler | DONE (decision 28: docs, code, class B): no scheduler page exists in the Forge docs (a finding); the 26.3 mechanisms read (tick events, logical-side work queue, `TickTask`, nine block patches that call scheduling or random ticking); NeoForged 26.1 blocks and networking pages read as class B for scheduled ticks, random ticks and `enqueueWork`; the vanilla tick lists are vanilla classes and cannot be read here (sections 36.3 and 39.4 of research.md) |
| 21 | Gameplay API: rendering hooks | DONE with the label **documented limit** (decision 30 b): the "Model Extensions: Introduction" page never existed (it is listed in the docs navigation, no commit in any of the 15 branches and 502 commits ever touched it, its address answers 404 on eight branches; proof in section 39.1 of research.md); everything else read: all model extension, loader and client model pages, the 26.3 custom geometry loader API (`IGeometryLoader`, registration that throws on a repeated id, stock loaders), and the vanilla item model definition lists (a class C wiki page); sections 23, 24, 25.2, 32, 33 and 39.1 of research.md |
| 22 | Gameplay API: recipes | DONE (decision 28: docs, code, class B): introduction, ingredients, conditional, custom and non-datapack recipe pages read in full (the docs are older than the 26.3 code: the brewing registry and `IBrewingRecipe` do not exist there); `forge:conditional` is registered as `conditional` on every branch from 1.21.1 to 26.3 and changed shape at 26.3 (sections 28.2, 35.1 and 39.2 of research.md); NeoForged custom recipes page read by headings (class B) |
| 23 | Gameplay API: loot tables | DONE (decision 28: docs, code, class B): the latest 1.21.x page read in full; the 26.3 code read (the load event and its only caller, pool and table patches, the looting hook, parameter sets, the global loot modifier manager, conditions); NeoForged loot pages by headings and key terms; the multi-item smelting claim of the docs was not confirmed at 26.3 (sections 21 and 39.3 of research.md) |
| 24 | Gameplay API: tags | DONE for the 1.21.x docs; the tag `remove` extension and insertion-ordered merge were checked in the 26.3 source (removals are always optional at runtime; section 28.3 of research.md); Tags List page not read |
| 25 | Gameplay API: worldgen | DONE (decision 28: docs, code, class B): no worldgen or biome-modifier page exists in the Forge docs (a finding, like rows 19 and 28; full-text search of the docs tree); the 26.3 code read (biome and structure modifier contract, when and how they run, registries and data folder, five stock biome modifiers, one stock structure modifier, four holder set types, the `Biome` and `Structure` patches and 12 smaller worldgen patches); NeoForged "Biome Modifiers" read as class B (NeoForge has nine stock types against Forge's five, other names and folder; no class B text on structure modifiers). Not read: the builder classes for spawn, climate, effects and structure settings (sections 37.1 and 37.6) |
| 26 | Gameplay API: data generation | DONE (decision 28: docs, code, class B): all ten Forge datagen docs pages read in full from the docs repository; the 26.3 code read (event, command line, run flow, duplicate provider names, `RegistryDataBuilder`, providers, patches); NeoForged "Data Generation" section read in full as class B. The docs are older than the 26.3 code: the model generator package, `DatapackBuiltinEntriesProvider` and `ForgeAdvancementProvider` they describe are not in the 26.3 tree (section 37.2). Not read: tag provider bodies, `ForgeRecipeProvider#create`, `ConditionalRecipe`, per-topic NeoForged and Fabric datagen pages (section 37.6) |
| 27 | `defaultconfigs` copy behavior | DONE (answered from the 26.3 source: the default file is copied on first creation of a config file, path from `defaultConfigPath`; the call site was not read; section 29 row 4 of research.md) |
| 28 | Official docs for 26.x | DONE as a finding: confirmed absent on 2026-09-29 (docs offer 1.21.x, 1.20.x, 1.20.1, 1.19.x only) |
| 29 | Javadoc inventory and source revision per era | DONE: the era-to-source fit is settled by the full Forge history (11,821 commits, 45 branches): conf commits for Minecraft 1.1, 1.2.3, 1.2.4 (on ModLoaderMP), 1.2.5, 1.3.2 up to 1.7.10, and the entry of FML into Forge in July 2012; heads for every later era are in `source-map.json`; the Javadoc inventory is class C only, no official hosted Javadoc was found (the 26.3 build has a `javadoc` task) (section 40.4 of research.md) |
| 30 | E2 LaunchWrapper flow beyond coremods, `ITweaker`, `@SidedProxy`, `FMLInterModComms` | DONE (docs, code; no class B source applies): the LaunchWrapper `Launch` flow and `ITweaker` read from Mojang's `LegacyLauncher`; FML 1.12.x tweakers, `FMLLaunchHandler`, `CoreModManager` (cascading tweaks, `TweakClass` and `TweakOrder`, sorting), `ProxyInjector` (`@SidedProxy`) and `FMLInterModComms` with its place in `Loader#initializeMods` read; the 1.12.x docs "Sides" and "Loading Stages" read in full (section 38.2 of research.md). Not read: the inside of `LaunchClassLoader`, the rest of `ServerLaunchWrapper`, the same flow in 1.8.9 to 1.11.x (section 38.6) |
| 31 | E1a ModLoader-compatibility layer (exact behavior) | DONE: the source conflict is resolved (support through FML 1.6.4, absent from 1.7.10); discovery, identity, configuration, lifecycle, every helper class and the 80 and 71 methods of the shim `ModLoader` (client and server) read in FML 1.6.4; the early Forge tags and history are a patch set over a real ModLoader (sections 38.1 and 40.3 of research.md); Risugami's original ModLoader has no source in any repository read and is not the layer |
| 32 | `mods.toml` `[[mixins]]`, JS coremods, `fml.toml`, E3+ coremod compatibility path | DONE: the `fml.toml` options (14), early discovery of transformation services in `mods/`, no `[[mixins]]` table in `mods.toml` (Mixin only through jar manifest attributes, with a synthetic MixinExtras dependency from the branch 1.21.10), JavaScript coremods through the branch 1.21.10 and absent from 1.21.11 and later, with Java transformers in `forge-transformers` at 26.3; the Forge docs have no page on these (a finding) (section 40.1 of research.md) |
| 33 | Resources and packs for mods (`showAsResourcePack`, `pack.mcmeta`, conditional loading, global loot modifiers) | DONE for the docs scope plus the 26.3 source (resources and data intro, conditional loading and global loot modifiers from docs; `showAsResourcePack`, `showAsDataPack`, `pack.mcmeta` handling from the 26.3 source; older versions unverified; sections 20 and 26.3 of research.md) |
| 34 | Threading rules per era (capabilities, entity data, packets) | DONE (docs, code; no class B source applies): packets per era (network-thread handlers in E1b and E2, `IThreadListener` from 1.8, `enqueueWork` in E3 to E5), capabilities per era (registration locked, instances not guarded, `RegisterCapabilitiesEvent` absent at 26.3), entity data per era (1.7.10 `IExtendedEntityProperties`, renaming of repeated identifiers); finding: no thread rule is documented for capabilities and entity data (section 38.3). Not read: per-era synched-data patches, vanilla data classes, dispatch thread inside `SimpleChannel` (section 38.6) |
| 35 | Version registry mechanics (promotions file, recommended/latest, update checker, versioning convention) | DONE (added to the list on 2026-09-29; 26.3 source conflict noted in research.md 18.2) |
| 36 | `mcmod.info` `authors` versus `authorList` | DONE (answered from the 1.7.10 source: the field is `authorList`, there is no `authors` field; section 26.6 of research.md) |
| 37 | Forge `strength()` versus `destroyTime()`/`explosionResistance()` naming | DONE (both names exist in the 26.3 source at different levels: `strength` is the builder call, `destroyTime` and `explosionResistance` are fields; section 26.4 of research.md) |

**[Superseded: this is the count after pass 5, kept as history; the current count is the last paragraph of this section and the last row of 4.3.]** Result: 15 DONE, 5 PARTIAL, 17 not done or not verified, so
(15 + 0.5 x 5) / 37 = **47.3%, reported as about 47%**. Pass 6 changed three rows
(22 recipes to PARTIAL, 24 tags to DONE, 33 resources and packs to PARTIAL) and moved the count from
41.9% to 47.3%.

Pass 7 changed one row (23 loot tables, from not started to PARTIAL) and moved the count from 47.3% to
48.6%: (15 + 0.5 x 6) / 37 = **48.6%, reported as about 49%**.

Pass 8 changed one row (16 items, from not started to PARTIAL) and moved the count from 48.6% to 50.0%:
(15 + 0.5 x 7) / 37 = **50.0%, reported as about 50%**. Advancements were also read in pass 8, but
the fixed list has no advancements row, so that work does not move the percentage. The list is kept fixed on
purpose so that percentages stay comparable.

Pass 9 changed two rows (18 block entities and 21 rendering hooks, both from not started to PARTIAL) and moved the count from 50.0% to
52.7%: (15 + 0.5 x 9) / 37 = **52.7%, reported as about 53%**.

Pass 10 changed no row category, so the count stays at 52.7% (15 DONE, 9 PARTIAL). It did add real content: the
block entity renderer, the item renderer, item overrides and item properties were read (from the 1.19.x docs), but each of
rows 16, 18 and 21 still has unread parts, and the block entity renderer turned out to be redesigned in newer versions. The
rule stays fixed on purpose: a row moves only when its whole scope is covered.

Pass 11 changed two rows: 15 (blocks) and 18 (block entities) went from PARTIAL to DONE, and the count moved from 52.7% to
55.4% (17 DONE, 7 PARTIAL): (17 + 0.5 x 7) / 37 = **55.4%, reported as about 55%**. These two rows
count as DONE **for the docs scope only**: every page Forge's docs devote to them was read in full. It does not mean the whole
gameplay API of blocks or block entities is known. The block entity docs text is 1.20.x-era and may lag Forge's code, and a
redesigned renderer exists in NeoForge and Fabric, so 26.x is unverified for both rows.

Pass 12 (first pass from source code) changed five rows: 29 (Javadoc and source revision per era) from not started to PARTIAL, and 33
(resources and packs), 36 (`authors` versus `authorList`), 37 (`strength`) plus row 9 text. Rows 33, 36 and 37 became DONE, row 18
stayed DONE with updated text. The count moved from 55.4% to 63.5% (20 DONE, 7 PARTIAL): (20 + 0.5 x 7) / 37 =
**63.5%, reported as about 64%**. Two of the three new DONE rows (36 and 37) are small single-fact checks that the fixed list
weights like any other row, so this jump is larger than the amount of knowledge it reflects. The big items (gameplay API beyond
the docs pages, entities, commands, worldgen, scheduler, the metadata layer) are still untouched.

Pass 13 (checking the docs against the code) changed **no row category**: rows 5, 6 and 8 got updated text only. The count stays at 63.5%
(20 DONE, 7 PARTIAL) and the overall estimate stays at about 4.7%. The pass still changed what is known: it verified decision 8
against the code and narrowed decision 20's scope (`research.md` section 27).

Pass 14 also changed **no row category** (rows 22 and 24 got updated text). The count stays at 63.5% and the overall estimate at about 4.7%.

Pass 15 (the re-verification register) changed two rows: 26 (`defaultconfigs` copy behavior) from not verified to DONE, and the `[[mixins]]` row from not started to PARTIAL. Two more rows (sides, events) only got new text.
The count moved from 63.5% to 67.6% (21 DONE, 8 PARTIAL): (21 + 0.5 x 8) / 37 = **67.6%, reported as about 68%**. As in pass 12, one of these is a small single-fact row that the fixed list weights like any other.

Passes 16, 17 and 18 changed **no row category** (pass 18 only re-worded rows for lifecycle, rendering, items and recipes). The count stays at 67.6% and the overall estimate at about 4.9%.

Pass 19 (client and item side at 26.3) changed **no row category** either: rows 16 and 21 got new text and moved closer to DONE, but each still has an unread part (see their rows). The count stays at 67.6% (21 DONE, 8 PARTIAL) and the overall estimate at about 4.9%. Row 21 is the closest: what keeps it PARTIAL is one docs page that cannot be read (dead link), the custom geometry loader API read by names only, and the vanilla property lists.

Pass 20 (threading, trigger registration, duplicate keys before 1.19.2, the four checks on more branches) changed **no row category** either. It re-worded rows 6, 8, 10 and 34; row 6 lost its "threading unread" caveat for 26.3, and row 34 stays NOT STARTED because capabilities and entity data were not touched. The count stays at 67.6% (21 DONE, 8 PARTIAL, 8 NOT STARTED) and the overall estimate at about 4.9%.

Pass 21 (conditional data formats, tags list, rest of the vanilla metadata table, Javadoc inventory) changed **no row category** either: row 22 now has the conditional format for every era and row 29 has the Javadoc inventory, but each keeps an unread part (see their rows). The count stays at 67.6%. This is the third pass in a row without a change in the count, because passes 19 to 21 closed leftovers inside PARTIAL rows; the rows that can move the count are the NOT STARTED rows 17, 19, 20, 25, 26 (decision 28) and 30, 31, 34, which come next.

Pass 22 (entities, commands, scheduler; `research.md` section 36) changed three rows: 17 and 19 from NOT STARTED to DONE and 20 from NOT STARTED to PARTIAL, all under decision 28. The count moves from 67.6% to 74.3%: (23 + 0.5 x 9) / 37 = **74.3%, reported as about 74%** (23 DONE, 9 PARTIAL, 5 NOT STARTED: rows 25, 26, 30, 31 and 34). Row 19 counts as DONE because Forge has no commands page (a finding, as for row 28), the 26.3 code was read and Fabric's guides serve as class B; row 20 stays PARTIAL because its vanilla side has only excerpts. No row uses the "documented limit" label of decision 30. The overall estimate moves from about 4.9% to about 5.2% (119 / 23).

Pass 23 (worldgen and data generation; `research.md` section 37) moved rows 25 and 26 from NOT STARTED to DONE under decision 28. The count moves from 74.3% to 79.7%: (25 + 0.5 x 9) / 37 = **79.7%, reported as about 80%** (25 DONE, 9 PARTIAL, 3 NOT STARTED: rows 30, 31 and 34). Row 25 counts as DONE because Forge has no worldgen page (a finding, as for rows 19 and 28), the 26.3 code was read and NeoForged's page serves as class B; row 26 has all ten Forge pages read in full. No row uses the "documented limit" label of decision 30. The overall estimate moves from about 5.2% to about 5.4% (125 / 23).

Pass 24 (ModLoader compatibility, E2 launch flow, threading per era; `research.md` section 38) changed three rows: 30 and 34 from NOT STARTED to DONE and 31 from NOT STARTED to PARTIAL. The count moves from 79.7% to 86.5%: (27 + 0.5 x 10) / 37 = **86.5%, reported as about 86%** (27 DONE, 10 PARTIAL, 0 NOT STARTED). No row is NOT STARTED any more; only PARTIAL rows can still move the count. Row 31 stays PARTIAL because the helper bodies and the `ModLoader` shim are unread; rows 30 and 34 have no class B source because the features exist only in Forge, FML and LaunchWrapper. No row uses the "documented limit" label of decision 30. The overall estimate moves from about 5.4% to about 5.7% (131 / 23).

Pass 25 (leftovers of rendering hooks, recipes, loot tables, the scheduler and items; `research.md` section 39) moved rows 20, 21, 22 and 23 from PARTIAL to DONE; row 16 stays PARTIAL with a narrower gap. The count moves from 86.5% to 91.9%: (31 + 0.5 x 6) / 37 = **91.9%, reported as about 92%** (31 DONE, 6 PARTIAL, 0 NOT STARTED). **One DONE row carries the label "documented limit": row 21** (its introduction page never existed, proven in 39.1); it is the first row to use the label of decision 30 (b), and every later count reports such rows separately (31 DONE = 30 complete + 1 with the label). The overall estimate moves from about 5.7% to about 6.0% (137 / 23).

Pass 26 (loader mechanisms, networking at 26.3, the ModLoader layer, the early Forge history, build numbers and items; `research.md` section 40) moved the last six PARTIAL rows (2, 10, 16, 29, 31, 32) to DONE. The count moves from 91.9% to **100%: 37 DONE of 37, 0 PARTIAL, 0 NOT STARTED**. **One DONE row carries the label "documented limit" (row 21), and two rows (2 and 16) are DONE only through decision 35**, a provisional scope decision that Claude made and Shiroz can reverse (reversing one gives 98.6%, reversing both gives (35 + 0.5 x 2) / 37 = 97.3%). The overall estimate moves from about 6.0% to about 6.3% (145 / 23). With this the research part of the Forge gate is complete; the implementation part of phase 7 (adapters) has not started.

### 4.3 Count history

| Version | Basis | Forge phase |
|---|---|---|
| v1.0.0 | old 19-row table, text said 13 of 19 DONE and "about 28%" (the 28% was not derived from the count) | 28% (not count-based) |
| v1.0.3 | new fixed 37-row list, after pass 5 | 41.9%, reported ~42% |
| v1.0.9 | same 37 rows, after pass 6 | 47.3%, reported ~47% |
| v1.0.12 | same 37 rows, after pass 7 | 48.6%, reported ~49% |
| v1.0.13 | same 37 rows, after pass 8 | 50.0%, reported ~50% |
| v1.0.15 | same 37 rows, after pass 9 | 52.7%, reported ~53% |
| v1.0.17 | same 37 rows, after pass 10 | 52.7%, reported ~53% (unchanged, see 4.1) |
| v1.0.18 | same 37 rows, after pass 11 | 55.4%, reported ~55% |
| v1.0.20 | same 37 rows, after pass 12 (source code) | 63.5%, reported ~64% |
| v1.0.21 | same 37 rows, after pass 13 (docs checked against code) | 63.5%, reported ~64% (unchanged, see 4.1) |
| v1.0.23 | same 37 rows, after pass 14 (conditions, tags, registry flags) | 63.5%, reported ~64% (unchanged, see 4.1) |
| v1.0.24 | same 37 rows, after pass 15 (re-verification register) | 67.6%, reported ~68% |
| v1.0.29 | same 37 rows, after pass 18 (four unchecked rows) | 67.6%, reported ~68% (unchanged since pass 15) |
| v1.0.33 | same 37 rows, after pass 19 (client and item side at 26.3) | 67.6%, reported ~68% (unchanged since pass 15) |
| v1.0.36 | same 37 rows, after pass 20 (threading, triggers, duplicate keys, more branches) | 67.6%, reported ~68% (unchanged since pass 15) |
| v1.0.37 | same 37 rows, after pass 21 (conditional data, tags list, metadata table, Javadoc inventory) | 67.6%, reported ~68% (unchanged since pass 15) |
| v1.0.40 | same 37 rows, after pass 22 (entities, commands, scheduler) | 74.3%, reported ~74% (was 67.6%) |
| v1.0.41 | same 37 rows, after pass 23 (worldgen, data generation) | 79.7%, reported ~80% (was 74.3%) |
| v1.0.42 | same 37 rows, after pass 24 (ModLoader compatibility, E2 launch flow, threading) | 86.5%, reported ~86% (was 79.7%) |
| v1.0.43 | same 37 rows, after pass 25 (leftovers of five gameplay rows; one DONE row with the label "documented limit") | 91.9%, reported ~92% (was 86.5%) |
| v1.0.44 | same 37 rows, after pass 26 (the last six PARTIAL rows; two of them DONE through decision 35) | 100% (was 91.9%) |

### 4.2 Previous breakdown (v1.0.0, kept for history)
Source: `docs/upstream/forge/research.md` v0.4.0. Rough weight per item, all equal:

| Item | Status |
|---|---|
| Official version index (78 Minecraft versions, 1.1-26.3) | DONE |
| Build numbers per version | PARTIAL (31 of 78) |
| Era model (E1a/E1b/E2/E3/E4a/E4b/E5) | DONE, with noted uncertainty on the exact E4a/E4b boundary (the E1a/E1b line was fixed at 1.2.5 by decision 34) |
| Old metadata (`@Mod` + `mcmod.info`), including `requiredMods`/`dependencies` semantics | DONE |
| `mods.toml` (E3+) key list | DONE for 1.19-1.21.x docs; unverified for 26.x |
| Lifecycle events and threading | DONE for E1/E2/E3+; unverified for 26.x |
| Sides (`Dist`, `DistExecutor`, `@OnlyIn`) | DONE |
| Registries, including the Forge-vs-VESSEL duplicate-handling difference | DONE |
| Events (two buses, EventBus 6 vs. an unreleased-version EventBus 7) | DONE, EventBus 7's actual introduction version unverified |
| Networking by era | PARTIAL, 26.x unverified |
| Persistent data (capabilities, `SavedData`) | DONE for pre-26.x |
| Access transformers | DONE |
| Pre-ModLauncher coremods (`IFMLLoadingPlugin`) | DONE |
| ModLauncher's `ITransformationService`/`ITransformer` | DONE |
| Gameplay API: blocks | DONE (first slice only) |
| Gameplay API: items, entities, block entities, commands, scheduler, rendering, recipes, loot tables, tags, worldgen, datagen | NOT STARTED |
| `defaultconfigs` copy behavior | NOT VERIFIED |
| Official docs for 26.x | DO NOT EXIST YET (confirmed absent, not just unread) |
| Javadoc inventory, source repo revisions per era | NOT STARTED |

Out of roughly 19 tracked items, 13 are DONE, 2 PARTIAL, 4 NOT STARTED/UNVERIFIED —
about 28% by item count, which is where the phase-7 estimate in section 3 comes from.

Correction note (v1.0.3): the sentence above says 13 of 19 items were DONE and calls that "about
28%", but 13 of 19 is about 68%. The 28% figure was never derived from that count and was wrong as
a count-based number. The recount in 4.1 replaces it.

### 4.4 Stage A table: study of Sinytra Connector (added in v1.0.48)

Reference research (decision 36: no gate applies). The rows are the 26 starting rows of `handoff/NEW-CHAT-PROMPT-STAGE-2.md` appendix A, kept as they are (Shiroz, 2026-10-09). **Counting rule:** DONE = 1, PARTIAL = 0.5, counted by a script from this table, separate from the 23 phases. **DONE** only when every branch the row names was read to the row's depth (deep for rows 2 to 6, 8, 11 and 13 to 16; medium for the others) and nothing named is left unread; otherwise PARTIAL with the missing parts named. Files: `docs/upstream/sinytra/research.md`, `source-map.json`, `design-lessons-for-vessel.md`.

| # | What the row asks | Depth | Status | Pass | Done and missing |
|---|---|---|---|---|---|
| 1 | Map of repositories, branches and licences | medium | PARTIAL | A1 | Done: six repositories, all branch heads and tag counts (`git ls-remote`), licences read, host loader per branch (1.20.1 = Forge, 1.21.x and 26.1.x = NeoForge), layout. Missing: full organization list (API rate limit; `FAPI-Mojmaps` unexamined), content of branches not cloned. |
| 2 | Mod detection (locator, reader, stub) | deep | PARTIAL | A1 | Done: `26.1.x` reader and locator in full; `1.21.x` scanner and locator; `1.20.1` locator head. Missing: `DependencyResolver`, `EmbeddedDependencies`, early locators, `ConnectorEarlyLoader`, Launchpad's reader. |
| 3 | Rewriting Fabric metadata | deep | PARTIAL | A1 | Done: `26.1.x` JSON rewrite and `MetadataReader` in full; `1.20.1` and `1.21.x` in-memory native metadata; `1.20.1` generated entry class. Missing: Connector's own `LoaderModMetadata` classes. |
| 4 | Jar transformation pipeline and cache | deep | PARTIAL | A1 | Done: `26.1.x` `JarTransformer`, `JarTransformInstance`, plugin framework, cache, clean game jar; `1.21.x` chain order. Missing: `JarTransformer` of `1.20.1` and `1.21.x`, `EarlyCoremodTransformer`, the `runner` source set. |
| 5 | Remap (1.20.1, 1.21.x) and the end of remap in 26.1 | deep | PARTIAL | A1 | Done: namespaces per branch (SRG, Mojang, none), mapping data source in the Forgified Fabric Loader, `IntermediateMapping`, refmap remapper, first part of the renamer. Missing: second half of the renamer, `ReflectionRenamingTransformer`, `1.20.1` renamer line by line. |
| 6 | Mixin (refmap, safeguard, patch) | deep | PARTIAL | A2 | Done: `26.1.x` Mixin patch transformer, finalization, refmap storage and holder, safeguard, runtime facade; differences to `1.21.x` and `1.20.1` by diff. Missing: `FieldTypeUsageTransformer`, audit trail internals, `1.20.1` patch transformer body, `FabricMixinBootstrap` of `1.21.x`. |
| 7 | The Adapter library | medium | PARTIAL | A2 | Done: structure and size of the library, `Patcher`, `PipelineMethodTransformer`, dynamic patches, Connector's hand-written patch table. Missing: resolvers, processors and analysis code (about 9,000 lines), other branches, generated adapter jar production. |
| 8 | Runtime `ClassProcessor` services | deep | PARTIAL | A2 | Done: all three `26.1.x` processors and `ConnectorPrelaunch`; older service classes by name and size. Missing: `ConnectorForkJoinThreadFactory`, bodies of the `1.20.1` and `1.21.x` ModLauncher-style services. |
| 9 | Split packages and JPMS | medium | DONE | A2 | `26.1.x` merger, tracker and migrator read in full; `1.20.1` and `1.21.x` read as differences; `ForgeModPackageFilter` read; the `26.1.x` equivalent of that filter is unverified (named in research 9.5). |
| 10 | Stripping client and server environment | medium | DONE | A2 | Stripper read in full; identical on `1.21.x` and `26.1.x`, `1.20.1` differs by API only; no other code acts on the environment annotations (searched). |
| 11 | Entrypoints and Fabric Loader emulation (Launchpad, Forgified Fabric Loader) | deep | NOT STARTED |  |  |
| 12 | Structure and method of Forgified Fabric API | medium | NOT STARTED |  |  |
| 13 | Registry bridge | deep | NOT STARTED |  |  |
| 14 | Event bridge | deep | NOT STARTED |  |  |
| 15 | Networking and payload bridge | deep | NOT STARTED |  |  |
| 16 | Data, resources and load conditions | deep | NOT STARTED |  |  |
| 17 | Configuration and the plugin API | medium | NOT STARTED |  |  |
| 18 | Dependencies, aliases and versions | medium | NOT STARTED |  |  |
| 19 | Nested jars | medium | NOT STARTED |  |  |
| 20 | Class tweaker and access widener to access transformer | medium | NOT STARTED |  |  |
| 21 | Enum extension and fluid type | medium | NOT STARTED |  |  |
| 22 | Published limits (hybrid servers and others) | medium | NOT STARTED |  |  |
| 23 | Error diagnostics | medium | NOT STARTED |  |  |
| 24 | Tests, CI and the version model | medium | NOT STARTED |  |  |
| 25 | Licence consequences for VESSEL (MIT, Apache-2.0, GPL with classpath exception) | medium | NOT STARTED |  |  |
| 26 | Comparison with VESSEL's IR approach | medium | NOT STARTED |  |  |

**Count after pass A2 (2026-10-11, script):** 2 DONE, 8 PARTIAL, 16 NOT STARTED of 26 rows = 23.1% (after pass A1: 0 DONE, 5 PARTIAL, 21 NOT STARTED = 9.6%). The overall project figure (145 / 23 = 6.3%) is unchanged: reference research is not one of the 23 phases.

---

## 5. Known gaps that need a decision or more work before the next loader

These are flagged inside the detail files, repeated here so they are not missed:

1. **Full spec re-audit for the per-loader data split** (`data/fabric/`, `data/forge/`,
   etc.) has not been done against the original v3 document's every use of
   `data/mods/` — only the principle is written down in
   `docs/architecture/identity-and-collision.md`.
2. **Registry-collision detection cannot be purely static** for Forge (keys built via
   `DeferredRegister` with computed names may only be visible at runtime, after the
   Startup Build Stage may have already begun). Shiroz has not yet confirmed whether a
   runtime-caught duplicate discarding an in-progress build is acceptable.
3. **Is a "Latest" build with no "Recommended" build (Forge 26.3 today) treated as
   stable by VESSEL's Version Registry?** Not yet decided.
4. Whether the `mcmod.info` field `authors` (seen once, low authority) is real or a
   typo in that one source, against three higher-authority sources showing
   `authorList`.

**Update 2026-09-29 (v1.0.4):**
- Item 2 is answered by decision 17: the whole module set is scanned up front and any duplicate
  registry key stops VESSEL before loading. New open point that follows from it: how that scan
  learns keys that only exist at runtime (computed `DeferredRegister` names). No design yet.
- Item 3 is answered by decision 16: Latest-without-Recommended is not stable. New open point:
  applied literally, 37 of the 77 versions in Forge's promotions file are then "not stable",
  including old ones (1.1, 1.2.x, 1.3.2, 1.4.x) and 26.1, 26.1.1, 26.3. Behavior decided later the same
  day (v1.0.6): a yellow warning only, no blocking.
- Item 1 (full spec re-audit for `data/<loader>/`) and item 4 (`mcmod.info` `authors`) are still open. **[v1.0.47: the v3 text is now in `docs/original/`, so item 1 can be done from the package; it stays a later pass, not part of the Sinytra study.]**
- New: on 2026-09-29 Forge's promotions file said 26.3 latest is 66.0.6 while the official index page
  excerpt said 66.0.8. Rule decided by Shiroz the same day (v1.0.7): read promotions, maven and the
  download page, take the highest build, print a yellow warning naming the differing values
  (`research.md` section 18.2).

**Update 2026-09-29 (v1.0.6):** Shiroz answered the four questions from pass 5. (1) Not stable means
warning only. (4) The reading order for the next pass was accepted. (2) How to resolve disagreeing
Forge version sources (26.3: 66.0.6 versus 66.0.8) and (3) how the up-front duplicate scan finds keys
that only exist at runtime were answered "not clear", so both stay **open** and are to be re-explained.

**Update 2026-09-29 (v1.0.7):** Shiroz chose option A for the version-source disagreement (highest build
wins, yellow warning shown). Only the design of the up-front duplicate scan for keys that exist only at
runtime is still open (answered "still not clear" after a second explanation).

**Update 2026-09-29 (v1.0.8):** the last open question of pass 5 is answered. Shiroz chose option C for
the up-front duplicate scan (static scan, then an isolated dry run for modules that build registry names in
code) and requires clear console reporting. The remaining open items are the design details of that scan
(section 9 of `identity-and-collision.md`). Forge pass 6 (resources, recipes, loot tables, tags and the
rest of the reading list) was started next.

**Update 2026-09-29 (v1.0.10, pass 6):** two new open questions came out of the conditional-data and
loot-modifier pages (details in `research.md` 20.2 and 20.3). Not answered yet:
- `forge:mod_loaded` takes a bare id. With decision 7 a plugin and a mod can share an id. Proposal: a
  Forge-source condition matches only modules whose source loader is `forge`.
- Data conditions such as `item_exists` and `tag_empty`, and global loot modifiers, need finished
  registries or running mod code. How the Startup Build Stage gets complete registries and tags is not
  designed, and loot modifiers cannot be reduced to plain data.

**Update 2026-09-29 (v1.0.11):** both pass 6 questions are answered: decision 18 (condition identity, agreed)
and decision 19 (registries complete before the build stage, delegated to Claude). Decision 19 needs one
thing settled later: the older text says no mod is "enabled" during the stage, so it has to be spelled out
that running registration code is not enabling (`startup-build-stage.md` sections 2.1 and 10). Still open:
how registration code is kept from touching the world.

**Update 2026-09-29 (v1.0.13, pass 8):** one new question for Shiroz, not answered yet (`research.md` 22.4): trigger ids
added with `CriteriaTriggers#register` and ingredient or condition serializers added with `CraftingHelper#register` are not
filled through Forge's `RegisterEvent`. Should VESSEL count their ids as registry keys for the duplicate rule (decision 8)?
Also noted: the advancements page of Forge's docs has a 1.21.x menu but JSON-era code names, so its API is unverified for 1.21.x.

**Update 2026-09-29 (v1.0.14):** the pass 8 question is answered "yes": decision 20. It creates one new design
requirement: these ids are registered in `FMLCommonSetupEvent`, after the registration step, so the duplicate scan and
step 10 (decision 19) must also cover setup-time calls (`identity-and-collision.md` sections 5.3 and 9).

**Update 2026-09-29 (v1.0.15, pass 9):** one new open question for Shiroz (`research.md` 23.6): model loaders run on the
client, so a vanilla or Bedrock client cannot run them and the compiler must turn those models into a format the target
understands before the player joins. OBJ models may allow shapes that vanilla JSON models cannot hold, so conversion may lose
detail. What VESSEL does then is not decided. Also noted: the block entity method names on Forge's docs are 1.20.x-era and
newer docs show different signatures, so they are unverified for 1.21.x and 26.x.

**Update 2026-09-29 (v1.0.16):** the pass 9 question is answered: decision 21 (option C, simple models to vanilla JSON,
complex models to the Display/Model backend). Still open: the rule that separates simple from complex, and what happens
to a model that the chosen route cannot represent.

**Update 2026-09-29 (v1.0.17, pass 10):** one proposal waits for Shiroz (`research.md` 24.5): the line between "simple" and
"complex" models in decision 21. Proposed: anything that needs code at draw time or per-item state (block entity renderers,
item renderers, item override resolution, models tied to an item property) is complex and goes to the Display/Model backend;
static shapes with no such code are simple and go to vanilla JSON. A static OBJ model too detailed for vanilla JSON is the grey
case, still open. Also noted: the block entity renderer API was redesigned in newer versions, so its Forge docs text is
unverified for 1.21.x and 26.x.

**Update 2026-09-29 (v1.0.18, pass 11):** the pending proposal for the simple/complex line (decision 21) gets one proposed
addition (`research.md` 25.5): color tint handlers (`BlockColor`, `ItemColor`) depend on position or state and are client code,
so they would join the "complex" list. Still waiting for Shiroz. New open point from the block state page: in Minecraft 1.7 and
earlier, block state was a numeric metadata value, and decision 13 means VESSEL needs a metadata-to-property layer for those
versions; the docs give no mapping. Also clarified: Forge's own "latest" block entity renderer page shows the old shape, so the
redesigned renderer appears only in NeoForge and Fabric docs (`research.md` 25.4).

**Update 2026-09-29 (v1.0.19):** the pending proposal is accepted and the grey case decided: decision 22. The exact "fits vanilla JSON
exactly" test is still open and depends on researching the vanilla model format (phase 14). Also decided: the source for the pre-1.8
metadata-to-property layer (decision 13 versions) is Forge's source code first, plus other sources; research on it starts in the next pass.

**Update 2026-09-30 (v1.0.20, pass 12, first pass from source code):**
- The metadata-to-property layer for versions before 1.8 cannot be built from Forge source alone: Forge knows only the orientation
  bits of about 22 vanilla block kinds (`research.md` 26.2), and mod blocks keep their meaning in their own code. Open question
  for Shiroz below.
- For 26.x the Forge documentation is behind the code in at least three places (block entity data methods, block entity renderer,
  `CraftingHelper`). Claude's inference, not a decision: for 26.x, source code is the reference and the docs are a hint.
- Decision 20 (`CraftingHelper#register` ids count as registry keys) rests on an API that was not found by name in the 26.3 tree. It
  must be re-checked against the 26.3 code before implementation.
- Answered by source: `mcmod.info` uses `authorList`, not `authors` (open item 4 above is closed); EventBus 7 first appears at the
  `1.21.8` branch head (open item about EventBus is narrowed); `showAsResourcePack` exists and so does a `showAsDataPack` key that the
  docs pages read do not mention.

**Update 2026-09-30 (v1.0.21, pass 13):** two answers recorded (decisions 23 and 24): the pre-1.8 metadata layer keeps mod-block metadata as an opaque integer,
and 26.x claims from documentation are re-verified against the source. The re-verification changed the picture in three places:
- Decision 8 is **confirmed** by the 26.3 source (Forge allows overrides by default and logs them only at debug level).
- Decision 20 has a **version scope**: condition and ingredient serializer ids are ordinary registry keys from the 1.20.4 branch on, so the
  `CraftingHelper` special case applies only to 1.20.1 and earlier. No Forge-side trigger registration code exists in 26.3, so the trigger part
  is unresolved for 26.x.
- The Forge registry list is version dependent (eleven Forge-specific registries in 26.3, `research.md` 27.1), so the up-front duplicate scan needs a
  per-version registry list.

**Update 2026-09-30 (v1.0.23, pass 14):** three more open points closed from the 26.3 source, and one earlier statement corrected:
- The `CraftingHelper` reference in `JsonCodecProvider` is inside a commented-out block, so the leftover reference I reported in pass 13 is dead code.
- The conditional data format has **three versions by era** (up to 1.20.1, 1.20.4 to 1.21.1, 1.21.3 and later). The docs describe only the oldest. In 26.3 conditions apply to every
  JSON reload listener and to loot pools, using the keys `forge:condition` and `forge:conditional` (`research.md` 28.2).
- Only `forge:entity_data_serializers` and `forge:display_contexts` refuse overrides in 26.3.
- Trigger ids: Forge 26.3 has no trigger registry, so VESSEL must apply decision 20 to triggers itself.

**Update 2026-09-30 (v1.0.24, pass 15, re-verification register):** 26 earlier topics were checked against the 26.3 code (`research.md` section 29): see the verdict counts
there. Main changes for design: `DistExecutor` does not exist and `@OnlyIn` in mod classes throws in 26.3; the event bus API changed (`getModBusGroup`); `defaultconfigs` is a
copy-on-first-creation folder relative to the game directory; `[[mixins]]` is not read from `mods.toml` (manifest attributes are used); the model loader list is era dependent.
New open question for Shiroz: Forge's `defaultconfigs` folder is relative to the game directory and is copied into a config file when that file is first created. Under decision 6 (data
split per loader) and decision 4 (`vessel-config/`), **where should VESSEL keep the default configs for Forge mods?** Proposal, not decided: `vessel-config/defaults/forge/`, with the same copy-on-first-creation rule. **[Answered 2026-10-01 (v1.0.31): decision 26.]**

**Update 2026-09-30 (v1.0.26, pass 16, pre-1.8 vanilla metadata):** the pass found an inventory of 60 block and item kinds (the first version of this line said 64, a miscount; the JSON file and `research.md` section 30 say 60) and a few exact tables, but **no version-accurate
decode table for 1.1 to 1.7.10** (`research.md` section 30): the wiki describes 1.12.2, and the minecraft-data directory named 1.7 contains later content. Three conflicts between sources were found
(anvil, torch, dataset naming). Decompiled vanilla code was not read. **Open question for Shiroz:** how should the vanilla part of the metadata layer get its exact values?
Option A: Shiroz decompiles locally and uploads the vanilla block classes. Option B: VESSEL ships a data-driven table with a per-block confidence flag and treats `unknown` as opaque (same rule as mod blocks).
Option C: use a third-party cache of decompiled sources (not recommended here). Claude's recommendation, not a decision: B now, A later for the blocks that matter.

**Update 2026-09-30 (v1.0.28, pass 17, vanilla metadata table):** decision 25 recorded (option B with option C as reference only). The third-party cache was **not** downloaded because it bundles
Mojang's game JARs; the Bukkit API (`org.bukkit.material`, GPL v3, commit dated 2013-07-07) and the Forge 1.7.10 source were used as independent references instead (`research.md` section 31,
`vanilla-metadata-table.json`). The table has per-field confidence levels; conflicts that stay opaque: skull, anvil; unresolved naming convention: pumpkin, trapdoor. Still waiting for Shiroz:
where VESSEL keeps the default configs of Forge mods (Claude's proposal, not decided: `vessel-config/defaults/forge/` with copy-on-first-creation). **[Answered 2026-10-01 (v1.0.31): decision 26.]**

**Update 2026-10-01 (v1.0.29, pass 18):** the four unchecked register rows were checked against the 26.3 source (`research.md` section 32). Main changes for design: in 26.3 block tint is registered as
`BlockTintSource` lists (no item tint event in that file); `ItemProperties`, `ItemOverrides` and `BlockEntityWithoutLevelRenderer` are not in Forge's tree and `IClientItemExtensions` has no custom renderer hook;
a mod constructor can take only the loading context or nothing. Decision 22's principle stands, but its list of API names must be kept per era. Still waiting for Shiroz: where VESSEL keeps the default configs
of Forge mods (Claude's proposal, not decided: `vessel-config/defaults/forge/` with copy-on-first-creation). **[Answered 2026-10-01 (v1.0.31): decision 26.]**

**Update 2026-10-01 (v1.0.31):** Shiroz answered the open question of the default configs: option A, `vessel-config/defaults/forge/` with Forge's copy-on-first-creation rule (decision 26). In the same answers he set the rule for conflicting sources (decision 27), chose the three-pass split for the remaining Forge work (section 8) and confirmed the version and log rules (section 9). No pass was run in this version; the pass 19 results follow in v1.0.33.

**Update 2026-10-01 (v1.0.33, pass 19):** the client and item side was checked against the 26.3 source and the docs (`research.md` section 33). Main changes for design: in 26.x the item model is **data** (client item definitions at `assets/<namespace>/items/<id>.json`, chosen by the `ITEM_MODEL` component) instead of Java overrides, item properties and item renderers; Forge 26.3 has **no event** for registering custom item model types, properties, tint sources or special model renderers (NeoForge has, class B), so a mod-defined one is complex under decision 22; the model JSON extension keys differ from the docs (face data is reduced to `ambient_occlusion` and `calculate_normals`, `render_type_fast` is new, the alias is spelled `post-rotation`, the loaders `forge:composite` and `forge:item_layers` are not registered, there is no `BakedModel`); `ResourceLocation` is gone and `Identifier` takes its place. Two probable defects in Forge's own code (visibility merge, modded display transforms) are recorded as readings only. Decision 22's principle stands; its list of API names needs a 26.x column, which is kept in `research.md` section 33.4 and is not yet written into decision 22. No row changed.

**Update 2026-10-03 (v1.0.36, pass 20):** results in `research.md` section 34. For design: (1) **loading stages:** in 26.3 registration is sequential (registry by registry, mod by mod in sorted order, on the loader thread), while the setup events run in parallel on a `ForkJoinPool` with no cross-mod order before the 1.21.3 head, and `enqueueWork` queues run afterwards one by one on the loader thread; the `@Mod` constructor with a context appears between the 1.18.x and 1.19.x heads and the mod bus changes from `IEventBus` to `BusGroup` between 1.21.5 and 1.21.8; (2) **trigger ids:** from the 1.20.4 head on they are entries of the vanilla registry `TRIGGER_TYPES` filled through `RegisterEvent`, so the special rule of decision 20 is needed only up to 1.20.1; the earlier "unresolved" came from a search that missed the Forge test module; (3) **duplicate keys:** Forge overrides from the 1.12.x head on, but in 1.7.10 it throws on a duplicate name, so the "stricter than Forge" claim of decision 8 holds only from 1.12.x on; (4) **era boundaries** of the docs-era item API (gone at the 1.21.4 head, where client item definitions first appear), `CreateSpecialBlockRendererEvent` (only the 1.21.4 to 1.21.11 heads) and block tint sources (from 26.1); (5) **names:** Forge's access transformers use SRG names from 1.18.x to 1.21.11 and official names from 26.1, and `ResourceLocation` is called `Identifier` from the 1.21.11 head on. No row changed.
Shiroz also supplied a PayBot mapping cookbook as a reference on 2026-10-03 (source S122, class C, reference only, not part of this package). Two of its statements do not match the Forge trees: "Mojmap for Forge 1.20.6+" (the access transformers say SRG until 1.21.11) and `ResourceLocation` as the Mojmap name through 26.2 (it is `Identifier` from 1.21.11). Section 34.6 has the details and what was not checked.

**Update 2026-10-04 (v1.0.37, pass 21):** results in `research.md` section 35. For design: (1) **conditional data** has four formats by era (up to 1.20.1 a `conditions` list; 1.20.4 `forge:condition` with inline recipe entries; 1.21.1 `forge:condition` with a separate `recipe` key; 1.21.3 and later `forge:condition` plus the root array `forge:conditional` on every JSON listener), and the condition ids are stable from 1.20.4 on; (2) **tags:** all Forge tags are in the namespace `forge` up to the 1.20.4 head, 334 of 515 are in `c` at 1.21.1, and 492 of 498 are in `c` at 26.3, so VESSEL's own tag merging (decision 19) sees `forge:x` and `c:x` as different names (open design question 3 below; **answered 2026-10-04: decision 29**); (3) **metadata table** (decision 25) grew from 42 to 55 fields (33 corroborated, 15 single source, 3 convention unresolved, 2 conflict, 2 unknown): cocoa facing, vine flag bits, rail shape and the mushroom stem value are corroborated and convert; the mushroom cap faces stay opaque because the Bukkit class disagrees with Forge, the wiki and the Spigot Javadoc; the comparator mode and power bits are still unknown; (4) the **Bukkit snapshot** is not older than the 1.7.2 era, and the Forge side of the table is the same code in 1.6.4, 1.7.2 and 1.7.10; (5) **Javadoc** exists only as third-party mirrors, none for 1.21.1 and later, so source code stays the reference; (6) the old Forge **tags** `1.0.0` to `1.3.2` are Forge's own version numbers (dated 2011-07-31 to 2012-01-29), which leaves the E1a count of the era table untestable by tag name. No row changed.

**Update 2026-10-11 (v1.0.50, pass 29, the repository README):** no research pass; no row, count or percentage changed (script: 37 of 37 Forge rows DONE; stage A 2 DONE, 8 PARTIAL, 16 NOT STARTED = 23.1%; 145 / 23 = 6.3%). New: `repo-starter/README.md` v0.1.0 and the two files of `tools/` that rebuild it. Pass A3 still waits for the creation of the GitHub organization (section 8).

**Update 2026-10-10 (v1.0.49, pass A2, Sinytra Connector rows 6 to 10):** second pass of stage A. Stage A count: 2 DONE, 8 PARTIAL, 16 NOT STARTED of 26 = 23.1% (section 4.4); overall project figure unchanged (145 / 23 = 6.3%). Main findings: Mixin compatibility works by comparing the vanilla game against the host-modified game and rewriting only the injections that no longer fit (the Adapter library), plus a short hand-written exception table and a one-time startup safeguard; on `26.1.x` three FML class and method processors reshape vanilla and host classes and support the FabricASM libraries, while `1.20.1` and `1.21.x` use ModLauncher-style services; split packages are resolved by an ownership rule (first jar found owns it) and a migrator that moves Brigadier and Authlib into the game layer through private JDK access; the environment stripper removes members and interfaces only, never whole classes. `research.md` was renumbered (rows 6 to 10 became sections 6 to 10; the old sections 6 to 8 became 11 to 13). Files: `docs/upstream/sinytra/` (research v0.2.0, source-map v0.2.0, design lessons v0.2.0).

**Update 2026-10-10 (v1.0.48, pass A1, Sinytra Connector rows 1 to 5):** first pass of stage A. Stage A count: 0 DONE, 5 PARTIAL, 21 NOT STARTED of 26 = 9.6% (section 4.4); overall project figure unchanged (145 / 23 = 6.3%). Main findings: Connector's `1.20.1` branch runs on **MinecraftForge** (the handoff note said NeoForge only); discovery moved from a private directory scan (`1.20.1`, `1.21.x`) to FML's reader pipeline with stub mod files (`26.1.x`); remapping is Intermediary to SRG on `1.20.1`, Intermediary to Mojang on `1.21.x`, and absent on `26.1.x`; the mapping data is bundled in the Forgified Fabric Loader jar (a difference from decision 33); the cache and the clean game jar download follow patterns close to VESSEL's own decisions. Files: `docs/upstream/sinytra/`.

**Update 2026-10-09 (v1.0.47, pass 28, the original v3 saved and the confirmations recorded):** no research pass; no row, count or percentage changed (script: 37 DONE of 37 = 100%; 145 / 23 = 6.3). The original 625-point specification v3 is saved unchanged in `docs/original/` (626 numbered headings, 0 to 625, none missing; the 23 phases of #614 and every v3 point number cited in this file were checked against it and agree); Shiroz answered "ok" to the written definition of decision 36 and to the repository recommendations.

**Update 2026-10-09 (v1.0.46, pass 27, setup of the new chat):** no research pass; no row, count or percentage changed (script: 37 DONE of 37 = 100%; the 23 phases add up to 145, 145 / 23 = 6.3). Shiroz answered the questions of the new chat: decision 35 and the refinement of decision 34 are **approved**; the split of the Forge gate was delegated to Claude (decision 36); RG(Forge) is **not signed yet**; the other answers are in section 9 ("Answered on 2026-10-09") and the parameters of the next stage in section 8 (update v1.0.46).

**Update 2026-10-08 (v1.0.44, pass 26):** results in `research.md` section 40; **the Forge research reaches 100% of the 37 rows** (one row with the label "documented limit", two rows through decision 35). Main findings: (1) the full Forge history shows Forge builds for Minecraft 1.1, 1.2.3, 1.2.4 (on ModLoaderMP) and 1.2.5 and that FML joined Forge only in July 2012, so the assumption behind decision 34 is contradicted; the line stays at 1.2.5 but the E1b adapter must also load FML-2.x mods for 1.2.5; (2) JavaScript coremods end at the branch 1.21.10, `mods.toml` has no `[[mixins]]` table, and `fml.toml` has 14 options; (3) at 26.3 a filter keeps non-`minecraft` attributes away from vanilla connections, and the server list ping sends binary data in one property; (4) the Forge build numbers follow the Minecraft version, and the Maven metadata can be stale. **Review requested from Shiroz (not blocking):** decision 35 (rows 2 and 16 closed by scope decisions) and the refinement of decision 34. **Next step (agreed): write the input prompt for the new chat** (Sinytra Connector architecture first, then the website, then the other loaders).

**Update 2026-10-07 (v1.0.43, pass 25):** results in `research.md` section 39. Shiroz accepted the proposal of v1.0.42 on 2026-10-06: the line between E1a and E1b moves to Minecraft 1.2.5 (decision 34; the era table of research.md section 2 is changed). Main findings: (1) row 21: the Forge docs page "Model Extensions: Introduction" never existed (proof from the full docs history), so row 21 is the first row with the label "documented limit"; (2) at 26.3 a repeated geometry loader id throws, like the other registry-key stops; (3) `forge:conditional` keeps its id on every branch from 1.21.1 to 26.3 but changes shape at 26.3 and is never sent over the network; (4) the loot-table load event is fired from a generic hook that runs after any JSON element is parsed, and `BrewingRecipeRegistry` and `IBrewingRecipe` do not exist at 26.3; (5) Forge has an event for adding default data components to items (`GatherComponentsEvent.Item`); (6) the Forge docs are about a year older than the code for items, recipes and loot, so docs-against-code differences keep repeating. No question is waiting for Shiroz.

**Update 2026-10-06 (v1.0.42, pass 24):** results in `research.md` section 38. Main findings: (1) the section 2 conflict about the end of ModLoader support is not a conflict: FML 1.6.4 loads ModLoader mods and FML 1.7.10 ignores them; (2) FML with `@Mod` and a ModLoader layer starts with the tags for Minecraft 1.2.5 (May 2012), and the early Forge tags `1.0.0` to `1.3.2` (for Minecraft 1.0.0 and 1.1) are a patch set that needs a real ModLoader underneath; (3) the E2 start is a chain of LaunchWrapper tweakers; `@SidedProxy` picks the class by the physical side; IMC messages are delivered before the post-init handlers; (4) E1b and E2 packet handlers run on the network thread, E3 to E5 have `enqueueWork`; capability registration is locked but capability instances are not, and no thread rule is documented for capabilities and entity data; (5) at 26.3 `RegisterCapabilitiesEvent` does not exist (capabilities are registered by annotation scan only). **One proposal waits for Shiroz:** move the line between E1a and E1b from Minecraft 1.3.2 to 1.2.5 and describe E1a as "Forge on top of a real ModLoader" (Minecraft 1.0.0 and 1.1) and E1b as "FML, with its own ModLoader layer" (from 1.2.5). The era table is unchanged until Shiroz decides; the proposal does not change what must be built.

**Update 2026-10-05 (v1.0.41, pass 23):** results in `research.md` section 37. Shiroz delegated the question of pass 22 (where the data for translating Mojang's official names comes from) and added the wishes "preferably from Mojang, consult more sources, data from jars is acceptable": decision 33 records the answer (download Mojang's file at run time and cache it, never ship it; second inputs for other name spaces; jar data only fills gaps; offline owner may supply the file; otherwise the module is not loaded, with a message). A class B source (Fabric docs) now confirms that 26.1 is the first unobfuscated release and 1.21.11 the last obfuscated one. For design: (a) **datagen never runs at server run time** (`GatherDataEvent` is created only by the datagen loader), so only the generated files matter; (b) **biome and structure modifiers are datapack data** with a serializer registry; Forge and NeoForge differ in type names, folder and number of stock types (5 against 9), so cross-loader conversion needs a name map; (c) **the Forge docs are about a year older than the 26.3 code** for datagen (classes they describe are gone), so docs-against-code differences are expected to repeat. No question is waiting for Shiroz.

**Update 2026-10-05 (v1.0.40, pass 22):** results in `research.md` section 36. Shiroz answered "co" to the question of the pass 22 report: entity selector tokens and argument-type classes are registry keys (decision 31); and he added a requirement: mods that use Mojang's official mappings must load (decision 32). For design: (a) **commands:** Forge cuts every command branch below a mod-namespace argument type from the tree it sends to a vanilla connection; VESSEL's tree for vanilla and Bedrock-side players needs the same rule; (b) **entities:** mod entities with extra spawn data use a Forge-only packet (inference: a vanilla client cannot read it) and every saved entity carries the keys `CanUpdate`, `ForgeCaps` and `ForgeData`; (c) **scheduler:** Forge has no scheduler API, only tick events, the logical-side work queue and vanilla scheduled ticks, which matters for phase 10 (Folia); (d) **names:** the open points of decision 32 are in `research.md` 36.7. **[Answered in v1.0.41: delegated, see decision 33 and the update above.]**

**Update 2026-10-04 (v1.0.39, handoff to a new chat):** Shiroz answered the four open questions of v1.0.37: (1) the order of the remaining not-started rows is free as long as every row is covered; (2) the PayBot cookbook stays reference only and is **not** added to the package; (3) he does not know whether the runtime names of Forge 1.21.x mods are SRG or official, so the jar check of section 34.6 stays undone and the point stays an open research gap (it blocks nothing now); (4) tags and every other namespaced id are never aliased across namespaces (decision 29). He is continuing in a new chat and asked for the package in the same form as his first message: this status file, `VESSEL-docs.zip` and a prompt text. No research pass was run in this version.

**Update 2026-10-02 (v1.0.34, date corrected in v1.0.35):** Shiroz resolved the "B + A" question: option "both" (decision 28). Nothing is waiting for him now. Pass 20 results follow in v1.0.35.

---

## 6. Decisions made in chat but not yet written into a dedicated file

These are real, final decisions (see section 2, items 10, 11, 14) that currently only
exist in this status file and in Shiroz's memory record. They should get their own
file in a future session:

- Geyser bundling and auto-detection policy → proposed file:
  `docs/architecture/geyser-bundling.md`
- Network sync model for shared dynamic objects → proposed file:
  `docs/architecture/network-sync.md`
- Cross-release compatibility policy (warn on newer content, crash with reason on
  unmapped calls, update-all-loaders-per-release rule) → proposed file:
  `docs/architecture/compatibility-and-release-policy.md`

---

## 7. Files in this package

```
MASTER-SPEC-STATUS.md                          <- this file
LOG.md                                         <- full dated change history, every edit
handoff/
└── NEW-CHAT-PROMPT-STAGE-2.md                 <- the input prompt for the next chat (Sinytra Connector study, website, other loaders)
repo-starter/
└── README.md                                  <- the README of the GitHub repository (v0.1.0), rebuilt after every pass
tools/
├── build_readme.py                            <- rebuilds repo-starter/README.md from this file; every number comes from the tables
└── README.template.md                         <- the README text with placeholders
docs/
├── architecture/
│   ├── startup-build-stage.md   (v0.1.3)      <- startup flow, build stage, vessel-config/
│   └── identity-and-collision.md (v0.1.12)    <- module identity, loader classification, collisions
├── original/                                  <- the original USML v3 specification (625 points), saved unchanged, with a README
│   ├── README.md                (v0.1.0)
│   └── USML-FULL-MASTER-ENGINEERING-SPECIFICATION-v3.md
├── drafts/                                    <- Shiroz's two draft specifications, unchanged, with a README (not authoritative)
│   ├── README.md                (v0.1.1)
│   ├── VESSEL-DISTRIBUTION-AND-LOADER-ARCHITECTURE-SPEC-v1_0.md
│   └── VESSEL-PROJECT-WEBSITE-SPEC-v1_0.md
└── upstream/
    ├── fabric/
    │   └── research.md          (v0.1.0)      <- Fabric + Minecraft core versions, pack format
    ├── sinytra/                               <- stage A reference study of Sinytra Connector (not a loader; no gate)
    │   ├── research.md          (v0.2.0)      <- rows 1 to 10 so far
    │   ├── source-map.json      (v0.2.0)      <- repositories, branches, heads, licences, what was read
    │   └── design-lessons-for-vessel.md (v0.2.0) <- what to take from it (21 lessons so far), with open questions
    └── forge/
        ├── research.md          (v0.4.55)     <- the deep Forge research file, ~380 KB
        ├── version-index.json   (v0.1.3)      <- all 78 official Forge/Minecraft versions, with latest/recommended builds
        ├── source-map.json      (v0.1.10)      <- branches, tags, head commits and EventBus versions read from the Forge repository
        ├── metadata-inventory.json (v0.1.0)   <- what block metadata meant before the flattening (partial, with source conflicts)
        └── vanilla-metadata-table.json (v0.1.3) <- design seed of the vanilla metadata decode table with confidence levels (30 kinds, 55 fields)
```

The original Full Master Engineering Specification v3 (625 numbered points) that
Shiroz provided at the start of this project is not included as a separate file in
this package — it was supplied as pasted text at the start of the conversation and is
not a file this session produced. Section 2 of this document lists every point where
the decisions made since then override it; everything in v3 not mentioned there still
stands as originally written. **[Superseded 2026-10-09 (v1.0.47): the text is now in the package as `docs/original/USML-FULL-MASTER-ENGINEERING-SPECIFICATION-v3.md`, saved unchanged from the text Shiroz pasted; see `docs/original/README.md`.]**

---

## 8. Recommended next step

Per Shiroz's own rule ("finish one loader fully before moving to the next"), the
Forge gaps in section 4 (mainly: the rest of the gameplay API, and confirming whether
any 26.x-specific documentation exists yet) should be closed before starting NeoForge
research. A fresh conversation continuing this work should:

1. Read this file first, then `LOG.md` for the newest entries.
2. Read `docs/upstream/forge/research.md` section 15 ("Still missing for Forge") for
   the exact remaining list.
3. Continue research there. Shiroz confirmed on 2026-09-29 to finish Forge first and then move
   through the other loaders in phase order. Next pages to read (section 19 of `research.md`):
   still unread after pass 21: the wiki page after the torches section (rows for cocoa, vine, rails, comparator and end portal frame; the comparator bits, the end portal frame eye bit and the activator rail), the small leftovers of pass 19 (the Model Extensions
   introduction page, the custom geometry loader API, the vanilla property lists) of pass 20 (section 34.8: the loading code of 1.12.x and 1.17.x, the thread of the client loader, a jar-level check of runtime names, the duplicate handling code of the 1.8.9, 1.10.x and 1.11.x heads) and of pass 21 (section 35.7: the `forge:conditional` recipe serializer at 1.21.3 and later, the "Tags List" docs page, whether Forge aliases `forge:` and `c:` tags, the mapping of the old tags to Minecraft versions). `research.md` sections 20.7, 21.5, 22.5, 23.7, 24.6, 25.6, 26.8, 27.6, 28.7, 29.3, 31.6, 32.3, 33.5, 34.8 and 35.7 have the exact lists.
4. Shiroz chose on 2026-10-01 to split what is left into three passes, one per chat turn, with a percentage report before the next pass starts: **pass 19** (client and item side at 26.3, done in v1.0.33); **pass 20** (threading of
   `enqueueWork` and `ParallelDispatchEvent`, the advancement trigger registry, the registry check for eras before 1.19.2, and the four checks on the branches 1.20.4, 1.21.1 and 1.21.8; done in v1.0.36); **pass 21** (conditional data formats of
   1.20.4 and 1.21.1, the tags list, the Javadoc inventory, and the rest of the vanilla metadata table; done in v1.0.37). After these three, which are now done, come the not-started rows 17, 19, 20, 25 and 26 (decision 28: docs, Forge-side code and a class B source for the vanilla part) and the old-era
   rows 30, 31 and 34 (**in any order, as long as every row gets covered**, Shiroz 2026-10-04; the gameplay API rows first is only a suggestion because they are the largest part of the count) (conflicting sources follow decision 27; row 34 had been left out by mistake in v1.0.34 and was restored in v1.0.35).
5. Starting notes for the eight not-started rows (written for the next session; they list pointers found so far, not results). The eight PARTIAL rows (2, 10, 16, 21, 22, 23, 29, 32) are also still open: each row text in 4.1 says what is missing, and "completeness" in Shiroz's answer means all 16 rows, not only these eight:
   - **Row 17 entities, 19 commands, 20 scheduler, 25 worldgen, 26 data generation:** each needs three things (decision 28): the docs, the Forge-side 26.3 code, and a class B source for the vanilla part (for example NeoForged or Fabric docs). The docs map (`research.md` section 19) lists Forge pages for Data Generation (ten pages, none read) and for Synchronizing Entities (not read); there is **no official page for commands** at all, so row 19 starts from code and class B text. Already seen in the 26.3 tree: `DatagenModLoader`, the registries for biome modifier serializers, structure modifier serializers and holder set types (`ForgeRegistries`, sections 28.4 and 34), and `forge:entity_data_serializers`.
   - **Row 30 (E2 flow: `ITweaker`, `@SidedProxy`, `FMLInterModComms`, LaunchWrapper):** the heads of `1.8.9`, `1.10.x`, `1.11.x` and `1.12.x` are in the source map and can be cloned again; Javadoc mirrors for 1.8.9 to 1.12.2 exist (class C, `research.md` 35.4).
   - **Row 31 (E1a ModLoader compatibility):** the sources conflict (`research.md` section 2); follow decision 27 (look for a reputable source, else mark `conflict` with the reason). The old Forge tags `1.0.0` to `1.3.2` are Forge's own version numbers, not Minecraft versions (`research.md` 35.5).
   - **Row 34 (threading per era: capabilities, entity data, packets):** lifecycle threading and the 26.3 packet hand-off are done (`research.md` section 34); capabilities (the state `INJECT_CAPABILITIES` exists at 26.3) and entity data are untouched.


**Update v1.0.40 (pass 22 done):** rows 17 and 19 are DONE and row 20 is PARTIAL (section 4.1, `research.md` 36). By the plan of decision 30 the rest is: pass 23 = rows 25 and 26; pass 24 = rows 30, 31 and 34; passes 25 and 26 = the nine PARTIAL rows (2, 10, 16, 20, 21, 22, 23, 29, 32). The starting notes of item 5 for rows 17, 19 and 20 are used up; those for 25, 26, 30, 31 and 34 still stand.


**Update v1.0.41 (pass 23 done):** rows 25 and 26 are DONE (section 4.1, `research.md` 37). By the plan of decision 30 the rest is: pass 24 = rows 30, 31 and 34 (the old Forge branches); passes 25 and 26 = the nine PARTIAL rows (2, 10, 16, 20, 21, 22, 23, 29, 32). The starting notes of item 5 for rows 25 and 26 are used up; those for 30, 31 and 34 still stand.


**Update v1.0.42 (pass 24 done):** rows 30 and 34 are DONE and row 31 is PARTIAL (section 4.1, `research.md` 38). No row is NOT STARTED now. By the plan of decision 30 the rest is passes 25 and 26: the ten PARTIAL rows 2, 10, 16, 20, 21, 22, 23, 29, 31 and 32 (what keeps each one PARTIAL is in its row text in 4.1 and in the "still missing" lists of `research.md`). The starting notes of item 5 for rows 30, 31 and 34 are used up.


**Update v1.0.43 (pass 25 done):** rows 20, 21, 22 and 23 are DONE (row 21 with the label "documented limit") and row 16 stays PARTIAL (section 4.1, `research.md` 39). Six PARTIAL rows remain: 2, 10, 16, 29, 31, 32; by the plan of decision 30 they make up pass 26 (the last planned pass). What keeps each one PARTIAL is in its row text in 4.1 and in the "still missing" lists of `research.md` (row 2: build history per version; row 10: networking at 26.x; row 16: 39.8; row 29: the era-to-branch fit of E1a and early E1b, which the pass 24 findings help to settle; row 31: 38.6; row 32: `mods.toml` mixins, JS coremods, `fml.toml`).


**Update v1.0.44 (pass 26 done):** all 37 rows are DONE (section 4.1, `research.md` 40); the Forge research is complete (one row with the label "documented limit", two through decision 35). By the order Shiroz gave, the next step is to write the input prompt for the new chat; in the new chat the first job is a deep study of the architecture of Sinytra Connector, then the website (no repository exists yet), then all other loaders (the Forge gate of the loader specification is respected: Sinytra is reference research, not a loader adapter).


**Update v1.0.45 (prompt written):** the input prompt for the new chat is `handoff/NEW-CHAT-PROMPT-STAGE-2.md` (also delivered as a separate file). It carries the order of work Shiroz fixed on 2026-10-07 (stage A: a deep study of the architecture of Sinytra Connector, as reference only; stage B: the project website, no repository exists yet; stage C: all other loaders), the working rules, the questions that must be asked first, the conflict between the loader draft's "Forge gate" and that order, and unverified starting notes on Sinytra. The two draft specifications are in `docs/drafts/` unchanged.

**Update v1.0.46 (pass 27, setup of the new chat, 2026-10-09):** decision 36 (the gate split, section 3.1) is recorded; decision 35 and the refinement of decision 34 are approved; RG(Forge) is not signed, so research of other loaders (stage C) stays closed until it is, while stages A and B are not gated. **Stage A parameters (Shiroz answered "ok" to the defaults on 2026-10-09):** the study of Sinytra Connector is reference only; Shiroz's framing is that VESSEL is basically Sinytra Connector with more loaders, kept as a working hypothesis, and row 26 compares the two models (Connector runs Fabric mods inside a NeoForge host; VESSEL as decided converts modules to a shared IR on its own server software) without deciding anything in stage A; (a) the 26-row starting table of `handoff/NEW-CHAT-PROMPT-STAGE-2.md` appendix A is kept as it is and becomes the stage A table (DONE = 1, PARTIAL = 0.5, counted by a script, separate from the 23 phases); (b) depth: deep for rows 2 to 6, 8, 11 and 13 to 16, medium for the rest; (c) branches: `26.1.x` and `1.20.1` of Connector first (the first has no remap, the second has), the others only as differences; (d) reading only: code, docs, git history and light static analysis, no builds (the shell's allowed network list has no Maven Central or Gradle plugin portal; Claude's inference, not tested); (e) the design lessons go into their own file `docs/upstream/sinytra/design-lessons-for-vessel.md` and the upgraded loader specification only takes short decisions and points to it; (f) licences: read and paraphrase, never copy code; row 25 records the consequences for VESSEL. Pass size 5 to 6 rows (5 + 5 + 5 + 5 + 6 = 26), every report with the `date -u` line. **Next:** pass A1 = rows 1 to 5 (the map of repositories, branches and licences; mod detection; rewriting of Fabric metadata; the jar transformation pipeline and its cache; remapping).

**Update v1.0.47 (pass 28, 2026-10-09):** (1) the written definition of decision 36 and the repository recommendations (one monorepo `vessel`, a project site at `<account>.github.io/vessel/` with a configurable base path, no domain before the first public release, a `CODEOWNERS` file without required reviews until a second maintainer exists) are confirmed by Shiroz with "ok"; (2) the original v3 specification is saved in `docs/original/` so that Shiroz is not asked for it again; (3) Shiroz asked to be guided through creating the dedicated GitHub account and the repository; Claude gave the manual steps in the report of pass 28, checked against the current GitHub documentation (Pages, code owners): GitHub Pages on the free plan needs a **public** repository; the publishing source is set to GitHub Actions in the repository settings; a `CODEOWNERS` file may sit in `.github/`, the root or `docs/` of the branch; the repository starts without a `LICENSE` file until the licence question (MIT, tentative) is checked; the starter files (workflow, `CODEOWNERS`, README, licence once decided) belong to stage B and are not created yet. The next pass is still **A1** (rows 1 to 5 of the stage A table).

**Update v1.0.48 (pass A1, 2026-10-10):** stage A started; see section 4.4 for the table and section 5 for the findings. **Reminder to repeat in every report until done:** Shiroz chose option B on 2026-10-09 (a free GitHub **organization** owned by his existing account, with the repository `vessel` inside) and will create it later because he is busy; the organization name and the repository are still to be created and reported to Claude. **Next:** pass A2 = rows 6 to 10 (Mixin, the Adapter library, the runtime `ClassProcessor` services, split packages and JPMS, environment stripping).

**Update v1.0.49 (pass A2, 2026-10-10):** rows 6 to 10 of stage A done (rows 9 and 10 DONE, rows 6 to 8 PARTIAL); see section 4.4. **Firm reminder (Shiroz asked on 2026-10-10 to be made to do it after this pass):** create the free GitHub organization and the repository `vessel` inside it, and tell Claude the organization name, **before pass A3 starts**; Claude does not start A3 until Shiroz says it is done or says explicitly that he wants to skip it again. **Next:** pass A3 = rows 11 to 15 (entrypoints and Fabric Loader emulation, the structure of Forgified Fabric API, the registry bridge, the event bridge, the networking bridge).

**Update v1.0.50 (pass 29, README, 2026-10-11):** the README exists and is rebuilt by script from this file. The firm reminder of v1.0.49 is unchanged: **create the free GitHub organization and the repository `vessel` before pass A3**; once the organization name is known, the README and the starter files can use it (the README uses only relative links today). From the next pass on, every pass ends with: update this file, run `tools/build_readme.py`, log, deliver.

---

## 9. Handoff notes for a new session (added 2026-10-01, v1.0.30; updated in v1.0.45)

Written so that a fresh conversation can continue without the previous chat. This section describes how to work; the decisions themselves are in section 2 and the open questions in section 5.

**What to read, in this order:** this file, then `LOG.md` (newest entries first), then `docs/upstream/forge/research.md` section 15 and the "still missing" lists of sections 20 to 32 (the exact list is
in section 8 above), then `docs/architecture/identity-and-collision.md` section 9 and `docs/architecture/startup-build-stage.md` section 10 (open design points). Older progress lines that a memory
store may hold (for example "Forge research v0.4.0, 31 of 78 builds") are **historical**; this file and the package are authoritative.

**Rules Shiroz set for this work (all still in force):**
- Chat with Shiroz in Vietnamese; everything written into files (docs, configs, logs) in English.
- Before starting anything, ask detailed questions until sure; when he delegates a choice, pick the most reasonable option, write the reasoning down, and say that it was delegated.
- Every edit of a file, however small, bumps that file's version (one part per change) and is recorded in `LOG.md`. Earlier findings are never deleted; superseded text is marked in place.
- After each research pass, report progress as a percentage. The Forge number is counted on the **fixed 37-row list in section 4.1** (DONE = 1, PARTIAL = 0.5), so a row moves only when its whole scope is covered; the overall number
  is the sum of the 23 phase percentages divided by 23 (currently 145 / 23, about 6.3%). Passes that change no row say so honestly.
- Finish one loader fully before the next. Order after Forge: NeoForge, Quilt, the Bukkit family (Spigot, Paper, Folia, Canvas, Purpur), Bedrock and Geyser, as in the phase list of section 3.
- Claims about 26.x taken from documentation are re-verified against the 26.x source code before VESSEL relies on them (decision 24). Evidence classes in `research.md`: A official or source code, B credible secondary, C community or excerpt.
- When sources conflict, look for a reputable source that can settle it; if it still conflicts, mark it as a conflict and write down the reason (decision 27).
- A reference document supplied by Shiroz in the chat (for example the PayBot mapping cookbook, S122) is evidence class C and reference only, and it is **not** copied into this package (Shiroz, 2026-10-04); where it disagrees with the Forge source, the source wins (decision 24) and the difference is recorded (section 34.6 of `research.md`).
- Dates in the logs: from pass 20 on they come from the container clock (UTC) and are checked against file times; two one-day errors in v1.0.34 were found that way and corrected in v1.0.35.
- Forge research runs in passes, one per chat turn, with a percentage report before the next pass starts (Shiroz, 2026-10-01); passes 19 to 26 are done, and the remaining rows (not-started and PARTIAL) may be taken in any order as long as every row is covered (Shiroz, 2026-10-04); the plan of decision 30 is only a plan.
- After every pass, deliver the whole `VESSEL-docs.zip` (not only the changed files) together with the report, in the same message (Shiroz, "Ok", 2026-10-02).
- Every report carries the container clock line (`date -u`) (Shiroz, 2026-10-09).
- Files other than the docs (code, configuration, workflows; from stage B on) are also versioned inside the file where its format allows, and every change is logged in `LOG.md` (Shiroz, 2026-10-09: "có nhé, lưu log"; the exact shape of the in-file log was not specified, so this is Claude's reading).
- The repository README (`repo-starter/README.md`) is part of the package and is **updated after every pass that completes or changes something** (Shiroz, 2026-10-11: "mỗi lần xong phần gì thì NÊN sửa lại readme, nên bỏ cả readme trong zip"): run `python3 tools/build_readme.py . <readme version>` after the status file is updated (all numbers are read from the tables), bump the README version, and log it in `LOG.md`. The README stays honest: research and design progress is never shown as implementation, and every statement about the future is marked as planned.

**Answered on 2026-10-01:** where VESSEL keeps the default configs of Forge mods (decision 26).

**Answered on 2026-10-02 (decision 28):** what counts as DONE for the not-started gameplay API rows: the docs, the Forge-side 26.3 code **and** a source for the vanilla part.

**Answered on 2026-10-04:** the order of the remaining rows is free (completeness matters); the PayBot cookbook is reference only and stays out of the package; tags and all namespaced ids are never aliased across namespaces (decision 29); and he does not know whether Forge 1.21.x runtime names are SRG or official, so that stays an **open research gap** (section 34.6: the access transformers say SRG until 1.21.11 and official names from 26.1; the runtime names need a jar or another source; it blocks nothing now). **[Superseded 2026-10-08: no question blocks work; a review is requested for decision 35 and for the refinement of decision 34 (section 5 update of v1.0.44); everything else was answered, see decisions 33 and 34.]**
Everything else listed in section 5 as "open" is a design detail or a research gap, not a pending answer.

**Answered on 2026-10-05:** selector tokens and argument-type classes are registry keys (decision 31); mods that use Mojang's official mappings must load (decision 32, design open). The question of pass 22 (where VESSEL gets the data to translate official names) was delegated and answered with decision 33 (v1.0.41). The proposal of v1.0.42 (the E1a/E1b line) was accepted as decision 34 in v1.0.43. **A non-blocking review waits for Shiroz (decision 35 and the refinement of decision 34, v1.0.44).** **[Superseded 2026-10-09 (v1.0.46): both approved.]**

**Answered on 2026-10-09 (new chat, v1.0.46):** (1) the split of the Forge gate was delegated to Claude (decision 36); (2) the study of Sinytra Connector is reference only, and the defaults for stage A (section 8, update v1.0.46) were accepted with "ok"; (3) decision 35 and the refinement of decision 34 are approved; (4) website and repository: the old name USML is not mentioned on the website; the project is published from Shiroz's own personal GitHub account (he will create a dedicated account later; its name is not known yet) and moves to an organization if contributors appear **[Superseded 2026-10-09 (v1.0.48): Shiroz chose option B, a free organization owned by his existing account, created later; the reason for the change is GitHub's rule of one free account per person]**; the repository name was left to Claude, who **recommended** one monorepo called `vessel` (loader, adapters, distribution, tests, docs and website, as in section 27 of the loader draft), a project site first at `<account>.github.io/vessel/` with a configurable base path, no domain bought before the first public release, and a `CODEOWNERS` file with Shiroz as owner without required reviews until a second maintainer exists (**recommendations, not yet confirmed by Shiroz**) **[Confirmed with "ok" on 2026-10-09 (v1.0.47); Shiroz asked to be guided through creating the account and the repository.]**; no domain yet; website content is approved by hand by him, `CODEOWNERS` possible; no licence chosen yet, he thinks MIT is reasonable (tentative: it holds only if VESSEL embeds or links no copyleft code; to be checked at row 25 and in stage C before any `LICENSE` file); the language of the website stays English with Vietnamese later (default accepted); (5) the order of the other loaders stays as in the phase list (NeoForge, Quilt, the Bukkit family, Bedrock and Geyser); where Fabric fits is decided at the start of stage C; (6) passes as before with the `date -u` line; (7) logs are kept for every file; (8) the Forge research gate: not signed yet ("chưa"); (9) Shiroz does not remember the original Full Master Engineering Specification v3; it was pasted as text at the start of the first conversation (section 7) and is still not in the package, and it is not needed for stages A and B. **[Superseded the same day (v1.0.47): Shiroz found it, pasted it and asked for it to be saved in the package; it is in `docs/original/`.]** Nothing else is waiting for him; the open items are the written definition of decision 36, the sign-off of RG(Forge), and the confirmation of the recommendations in (4). **[Update v1.0.47: only the sign-off of RG(Forge) is still open.]**

**Answered on 2026-10-11 (README, v1.0.50):** Shiroz asked for a README for the repository; English only; honest about the state; minimal style (no banner and no logo, because no logo has been designed yet; three plain status badges, one diagram, short tables); the licence is shown as "not chosen" (he accepted this; Claude explained that parts of the surrounding ecosystem use copyleft licences, so MIT cannot be promised before the check of row 25); the old name USML is not mentioned (default); no contact or chat links (none given). The README is kept in the package and updated after every pass (rule in section 9).

**What was learned about the tooling (to save time):**
- Network from the shell reaches `github.com` (git clone, `git ls-remote`), `raw.githubusercontent.com` (single files) and package registries. The GitHub API is rate limited for the shared address; use git instead.
- `web_fetch` only accepts URLs that appeared in a search result or in a fetch result still in the current context; navigation links inside an older, cleared result are refused. Search first, then fetch the result link.
- Mojang's game files are not reachable, so no vanilla source can be decompiled in this environment (see `research.md` section 30 and 31).
- Shallow clones (`--depth 1`) give no history. Branch heads move, so every value is "as of the run date".
- In the Forge 26.3 tree most changes to vanilla classes are diff files in `patches/`, not code in `src/`; a word search must cover both (a pass 22 mistake, `research.md` 36.0).
- The full history of a large repository can be cloned in the background without file contents (`nohup timeout 900 git clone --filter=blob:none --no-checkout <url> dir > log &`, then poll the log); path searches (`git log --all -- <path>`) and date windows then run on the commit graph only (pass 26, `research.md` 40.4).
- The full history of a small repository can be searched with a blob-less clone (`git clone --filter=blob:none --no-checkout`), then `git log --all -- <path>` and `git log --all -S<text> -- <file>` show whether and when a path or a line ever existed; this is how a "page that does not exist" is proven (pass 25, `research.md` 39.1).
- Old tags and branches can be read without cloning a whole history: `git init`, add the remote, set `remote.origin.promisor` to true and `remote.origin.partialclonefilter` to `blob:none`, then `git fetch --depth 1 --filter=blob:none origin refs/tags/X:refs/tags/X`; `git log -1`, `git ls-tree` and `git show X:path` then fetch only what they need (pass 24, `research.md` 38.0).
- The docs of Forge, NeoForged and Fabric can be read from their GitHub repositories (markdown source) instead of the web pages: `MinecraftForge/Documentation` (one branch per version), `neoforged/documentation` (`docs/` is the current line) and `FabricMC/fabric-docs`. It is quicker, has no navigation noise and allows a full-text search of a docs tree (pass 23, `research.md` 37.0).
- Work in a scratch copy of the package, keep a backup of each file before an edit, measure file sizes instead of estimating them, and validate JSON files after every edit.

**Process mistakes made before, to avoid repeating:** two passes were written and zipped but their report was not sent before the next pass began; several counts were wrong when typed by hand (decisions table matched by a row-counting
script, 64 instead of 60 kinds, 16 instead of 12 equal builds, a table row off by one). Each was caught by re-checking with a script and recorded in `LOG.md`. Recompute numbers from the data and report each pass before starting the next.

**Source numbering:** the sources table of `research.md` runs S1 to S161 and S69 is intentionally unused (a loot table search result that was not needed).

**The original specification:** the Full Master Engineering Specification v3 (625 points) is **not** in this package (section 7). **[Superseded 2026-10-09 (v1.0.47): it is now in the package as `docs/original/USML-FULL-MASTER-ENGINEERING-SPECIFICATION-v3.md`; Shiroz does not need to attach it again.]** If the next session is meant to write the final, complete master specification, Shiroz must attach the v3 text again, because
the final document has to merge v3 with every decision and research finding of this package.

**Where this stopped:** the Forge research is complete: 37 DONE of 37 rows (100%), of which one row has the label "documented limit" (row 21) and two (rows 2 and 16) are DONE through decision 35, a provisional scope decision that Shiroz can reverse **[approved on 2026-10-09, v1.0.46]**; overall about 6.3% (145 / 23); 35 decisions recorded **[36 from v1.0.46]**; research file at v0.4.55; last pass was pass 26 (the last six PARTIAL rows); passes 19 to 26 are done. Next, as Shiroz ordered: (1) write the complete input prompt for the new chat, built on the two draft specifications he uploaded (the distribution and loader architecture spec and the website spec; both are drafts, not the versions to apply) and on this package; (2) in the new chat: a deep study of the architecture of Sinytra Connector, then the website (no repository exists yet), then all other loaders. A non-blocking review waits for Shiroz: decision 35 and the refinement of decision 34. **[Superseded 2026-10-09 (v1.0.46): both approved.]** Delivered with the report of pass 26 as this status file and `VESSEL-docs.zip`. **Update v1.0.45:** the input prompt for the new chat is written (`handoff/NEW-CHAT-PROMPT-STAGE-2.md`); the two draft specifications are kept unchanged in `docs/drafts/`. **Update v1.0.46:** decision 36 recorded, decisions 34 (refinement) and 35 approved, RG(Forge) not signed; next is pass A1 of the Sinytra study (rows 1 to 5); no count changed. **Update v1.0.47:** the original v3 is saved in `docs/original/`; the definition of decision 36 and the repository recommendations are confirmed; only the sign-off of RG(Forge) is open; next is pass A1; no count changed. **Update v1.0.48:** pass A1 of the Sinytra study done (rows 1 to 5, all PARTIAL, stage A at 9.6%); next is pass A2 (rows 6 to 10); the creation of the GitHub organization and repository is Shiroz's to do later (remind in every report); no project count changed. **Update v1.0.49:** pass A2 of the Sinytra study done (rows 6 to 10; stage A at 23.1%); next is pass A3 (rows 11 to 15), after Shiroz creates the GitHub organization and repository (firm reminder, section 8); no project count changed. **Update v1.0.50:** `repo-starter/README.md` v0.1.0 added with its builder script; the README is rebuilt after every pass; the GitHub organization is still to be created before pass A3; no count changed.
