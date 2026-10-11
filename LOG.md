# VESSEL Documentation Change Log

Records every change to the VESSEL documentation set. Each entry is tied to the version
of the file that changed. Every file bumps its own version on any change, however small.

---

## 2026-10-11 (pass 29: the repository README)

Dates come from the container clock (UTC; checked with `date -u` at 2026-10-11 10:27 UTC). No research pass. Shiroz asked for a "nice" README for the repository and answered the questions: English only, honest about the state, minimal style, no logo (none designed), licence shown as not chosen, README kept in the package and updated after every completed part.
**Done:** wrote `repo-starter/README.md` v0.1.0 (title block with three plain badges, what VESSEL is, goals, one Mermaid diagram, project status table with the 23-phase table in a collapsible block, planned support, documentation links, contributing, credits, licence, disclaimer) and a builder, `tools/build_readme.py`, with `tools/README.template.md`, so that every number in the README comes from the tables of the status file. The old name USML is not mentioned. No outside source was used; the contents come from the status file, the decisions in it and the original specification.
**Numbers (script):** unchanged: 37 of 37 Forge rows DONE; stage A 2 DONE, 8 PARTIAL, 16 NOT STARTED = 23.1%; 145 / 23 = 6.3%.
**Mistakes found while working:** (1) my first README draft listed the phase table with a column "Progress" and "Forge Adapter 100%", which would have told readers that the Forge adapter is finished; the 100% is the research part only. I caught it while checking the status file, replaced the column by "Status" and "Estimate", and added the sentence that the estimates cover research and design only; (2) the first draft also said "matches status v1.0.50" while the status file was still v1.0.49; the builder now reads the version from the status file; (3) the Mermaid diagram and the shields.io badges were **not rendered** in this environment (no browser, and the badge service is outside the allowed network), so their look is unchecked.

### `repo-starter/README.md` (new)
- **v0.1.0**, matches status v1.0.50.

### `tools/` (new)
- `build_readme.py`, `README.template.md`.

### `MASTER-SPEC-STATUS.md`
- **v1.0.50:** new files in the tree (section 7); a new working rule about the README (section 9); the answers recorded (section 9); update notes in sections 5 and 8; "Where this stopped" extended.

### Files not changed
Everything else in the package.

### Package delivered with this version
`MASTER-SPEC-STATUS.md`, `repo-starter/README.md` (both the same as inside the zip) and `VESSEL-docs.zip`, in the same message as the report of pass 29.

---

## 2026-10-10 (pass A2: Sinytra Connector study, rows 6 to 10)

The pass started on 2026-10-10 and was finished on 2026-10-11. Dates come from the container clock (UTC; checked with `date -u` at 2026-10-11 02:46 UTC). Second pass of stage A (reference research, no gate: decision 36). Shiroz asked again to postpone creating the GitHub organization and asked that Claude make him do it after this pass: recorded as a **firm reminder** in section 8 of the status file (Claude does not start pass A3 until he says it is done or says he wants to skip it again).
**Sources used** (rated in `docs/upstream/sinytra/research.md` section 13): the shallow clones of pass A1 were reused (no new web search; no GitHub API call). Files read in full or in large part are listed as `SY15` (Connector `26.1.x`) and `SY16` (Adapter `26.1.x`); the same-named classes of the `1.21.x` and `1.20.1` branches were compared with normalized `diff` (imports and blank lines ignored), and the `ForgeModPackageFilter` class of `1.21.x` was read.
**Result:** rows 9 and 10 are `DONE` (their depth is medium); rows 6, 7 and 8 are `PARTIAL` (each names what is unread). Stage A count after A2: 2 DONE, 8 PARTIAL, 16 NOT STARTED of 26 = 23.1% (script, section 4.4); the project figure is unchanged (37 of 37 Forge rows DONE; 145 / 23 = 6.3%).
**Corrections to the handoff's starting notes** (research.md section 0, rows 11 to 16 of its table): `ModuleLayerMigrator` is, on `26.1.x`, a candidate locator that moves Brigadier and Authlib off the system class loader (on older branches it moves a named module from the boot layer to the game layer); the environment stripper removes members and interfaces, not whole classes; the safeguard is a one-time startup check with an off switch; the `ClassProcessor` services exist as such only on `26.1.x`.
**Mistakes and limits found while working:** (1) my first `diff` script used a shell feature (process substitution) that the default shell does not support and failed; it was rewritten for bash and the output of the failed run was discarded; (2) my first draft of the `research.md` header said the heads were "checked again" in a muddled sentence; it was replaced by a plain statement that the A1 clones were reused; (3) `research.md` was **renumbered**: rows 6 to 10 are sections 6 to 10 and the old sections 6, 7 and 8 became 11, 12 and 13, so the entry of pass A1 below refers to the old numbers ("section 7" there means the present section 12); (4) several classes are named but unread (section 12 of the research file), notably the resolvers and processors of the Adapter library (about 9,000 lines) and the bodies of the older service classes.

### `docs/upstream/sinytra/`
- `research.md` **v0.2.0**: new sections 6 (Mixin handling), 7 (the Adapter library), 8 (runtime class and method processors), 9 (split packages and JPMS), 10 (environment stripping); six more corrections in section 0; sources `SY15` and `SY16`; sections 11 to 13 rewritten.
- `source-map.json` **v0.2.0**: read scopes updated.
- `design-lessons-for-vessel.md` **v0.2.0**: lessons L14 to L21 and open questions 5 to 7.

### `MASTER-SPEC-STATUS.md`
- **v1.0.49:** rows 6 to 10 of the stage A table (4.4) and its count; update notes in sections 5 and 8 (with the firm reminder); section 7 shows the new versions; "Where this stopped" extended.

### Files not changed
Everything else in the package.

### Package delivered with this version
`MASTER-SPEC-STATUS.md` (the same file as inside the zip) and `VESSEL-docs.zip` (the whole package), in the same message as the report of pass A2.

---

## 2026-10-10 (pass A1: Sinytra Connector study, rows 1 to 5)

Dates come from the container clock (UTC; checked with `date -u` at 2026-10-10 09:29 UTC; the passes of 2026-10-09 ran earlier on the same clock). First pass of stage A (reference research, no gate: decision 36). Shiroz chose option B for GitHub (a free organization owned by his existing account) and will create it later; recorded as a reminder, no file depends on it yet.
**Sources used** (all rated in `docs/upstream/sinytra/research.md` section 8): shallow clones (depth 1) of `Sinytra/Connector` branches `26.1.x`, `1.21.x`, `1.20.1`, `Sinytra/Adapter` `26.1.x`, `ForgifiedFabricLoader` `26.1.x`, `1.21.x`, `1.20.1`, `Launchpad` `26.2.x`, `ConnectorExtras` `26.1.x`, `ForgifiedFabricAPI` `26.2.x`; `git ls-remote` of the six repositories; three web searches (organization page text, Connector page text, documentation site excerpts). The GitHub REST API call failed on a rate limit and gave no facts.
**Result:** rows 1 to 5 are all `PARTIAL` (each names what is unread); stage A count 0 DONE, 5 PARTIAL, 21 NOT STARTED of 26 = 9.6% (script, section 4.4 of the status file); the project figure is unchanged (37 of 37 Forge rows DONE; 145 / 23 = 6.3%).
**Corrections to the handoff's starting notes** (research.md section 0): the `1.20.1` branch of Connector targets MinecraftForge, not NeoForge; the heads of ForgifiedFabricAPI `26.2.x` and Launchpad `master` moved since the notes were written and Launchpad has two new branches; the remap target is SRG on `1.20.1` and Mojang names on `1.21.x`; the mapping data is a file bundled in the Forgified Fabric Loader jar; the exact suffix of the id normalization is `_nojpms`.
**Mistakes and limits found while working:** (1) the first working assumption in my notes, taken from the handoff, was that Connector runs only on NeoForge; reading the `1.20.1` build files corrected it before anything was written into the package; (2) the organization's repository list could not be enumerated (API rate limit), so row 1 stays PARTIAL and the extra repositories (for example `FAPI-Mojmaps`) are named from second-hand pages only; (3) several classes of rows 2 to 5 were listed or compared by key lines, not read in full; `research.md` section 7 names each of them.

### `docs/upstream/sinytra/` (new)
- `research.md` **v0.1.0**: rows 1 to 5 (repository map, mod detection, metadata rewriting, the jar pipeline and cache, remapping), corrections, sources `SY1` to `SY14`.
- `source-map.json` **v0.1.0**: repositories, branches, heads, tag counts, licences, what was read.
- `design-lessons-for-vessel.md` **v0.1.0**: thirteen lessons and four open questions (module-id aliasing against decision 29; where Intermediary names come from; Launchpad licence consequences; the `26.1.x` dependency check).

### `MASTER-SPEC-STATUS.md`
- **v1.0.48:** new section 4.4 (the 26-row stage A table and its count); update notes in sections 5 and 8; the GitHub choice recorded and the earlier "personal account" line marked superseded in place (section 9); section 7 shows the new folder; "Where this stopped" extended.

### Files not changed
Everything else in the package (research v0.4.55 of Forge, source-map v0.1.10, identity v0.1.12, drafts README v0.1.1, original v3 and its README v0.1.0).

### Package delivered with this version
`MASTER-SPEC-STATUS.md` (the same file as inside the zip) and `VESSEL-docs.zip` (the whole package), in the same message as the report of pass A1.

---

## 2026-10-09 (pass 28: the original v3 specification saved, confirmations recorded)

Dates come from the container clock (UTC; checked with `date -u` at 2026-10-09 22:30 UTC). No research pass; research files, source map, version index and identity file are unchanged (research v0.4.55, source-map v0.1.10, identity v0.1.12). Sources used outside the package: the current GitHub documentation pages on GitHub Pages (plans, publishing source) and on code owners, read only to write the set-up guide given in the report; nothing from them was copied into the package.
**Recorded:** Shiroz answered "ok" to the written definition of decision 36 and to the repository recommendations, and asked to be guided through creating the account and the repository (the guide is in the report of this pass, not in a file); RG(Forge) stays unsigned. Shiroz found and pasted the original Full Master Engineering Specification v3 and asked for it to be saved as markdown in the package: it is now `docs/original/USML-FULL-MASTER-ENGINEERING-SPECIFICATION-v3.md` (saved unchanged, with a README).
**Checks (script):** the saved v3 has 626 numbered headings (0 to 625), in order, none missing or duplicated, 9,444 lines, 116,104 bytes, valid UTF-8, balanced code fences; the 23 phase names of v3 point #614 equal the 23 names in section 3 of the status file; all 17 distinct v3 point references cited in the status file (as written there) exist as headings, and their topics were compared by eye with what the status says (the topics agree). Numbers recomputed from the status file: 37 rows, 37 DONE = 100%; 145 / 23 = 6.3%. Nothing moved.
Mistakes found while working: (1) the first pass of the text writing damaged the character in three occurrences of "Ví dụ" (points #30, #32 and #43; the "í" lost its first byte, so the file was not valid UTF-8); the script that validated the file found them, they were repaired and the file validated again; this is also stated in `docs/original/README.md`; (2) the file is a transcription of pasted text, not a byte-for-byte copy of a source file (none exists), which the README states; (3) none else that reached the package.

### `MASTER-SPEC-STATUS.md`
- **v1.0.47:** confirmation marks added in place to decision 36 and to the repository recommendations; the statements that v3 is not in the package are marked superseded in place (sections 7 and 9) and item 1 of the open list notes that the re-audit can now be done from the package; the file tree shows `docs/original/`; update notes in sections 5 and 8; "Where this stopped" extended.

### `docs/original/` (new)
- `USML-FULL-MASTER-ENGINEERING-SPECIFICATION-v3.md`: the original text, saved unchanged.
- `README.md` **v0.1.0**: provenance, how to treat it, the checks run, the honest limits.

### Files not changed
Everything else in the package, including `docs/drafts/README.md` (v0.1.1) and `handoff/NEW-CHAT-PROMPT-STAGE-2.md` (v1.0).

### Package delivered with this version
`MASTER-SPEC-STATUS.md` (the same file as inside the zip) and `VESSEL-docs.zip` (the whole package), in the same message as the report of pass 28.

---

## 2026-10-09 (pass 27, setup of the new chat: decision 36, approvals, answers)

Dates come from the container clock (UTC; checked with `date -u` at 2026-10-09 09:59 UTC). No research pass; no research file, source map, version index or identity file changed (research stays at v0.4.55, source-map v0.1.10, identity v0.1.12). The only outside lookup was one shallow web search for a name collision of "vessel" (used for a recommendation, not a source of the package).
Shiroz answered the questions of the new chat (the prompt `handoff/NEW-CHAT-PROMPT-STAGE-2.md`, section 6). **Recorded:** decision 35 and the refinement of decision 34 are approved; the split of the "Forge gate" was delegated to Claude and is **decision 36** (a research gate RG(X) and an implementation gate IG(X) for every loader, with what each kind of work needs; section 3.1 of the status file holds the gate table); **RG(Forge) is not signed** (he answered "chưa"), so research of the other loaders (stage C) stays closed, while stages A and B are not gated; the parameters of stage A, the answers about the website and the repository, the order of the other loaders, the pass size and the `date -u` rule are in sections 8 and 9 of the status file. Claude's recommendations for the repository (one monorepo called `vessel`, a site at `<account>.github.io/vessel/`, no domain before the first public release, `CODEOWNERS` without required reviews until a second maintainer exists) are marked as recommendations not yet confirmed. MIT is recorded as Shiroz's tentative opinion, with the condition that it holds only if VESSEL embeds or links no copyleft code (to be checked at row 25 and in stage C).
Numbers recomputed by a script from the files: the table of 4.1 has 37 rows, 37 DONE, 0 PARTIAL, 0 NOT STARTED = 100%; the 23 phase percentages add up to 145, 145 / 23 = 6.3%. Nothing moved.
Mistakes found while writing: (1) the first run of the edit script stopped on a failed assertion because the anchor sentence in section 8 began with a capital letter that the script had written in lower case; nothing had been written yet, and the script was corrected and run again; (2) none else that reached the package.

### `MASTER-SPEC-STATUS.md`
- **v1.0.46:** decision 36 added (section 2); approval marks added in place to decisions 34 (refinement) and 35 and to rows 2 and 16 of 4.1 (their status and the count are unchanged); new section 3.1 (gate table); update notes in sections 5 and 8 (the stage A parameters); an "Answered on 2026-10-09" paragraph and two new working rules in section 9 (the `date -u` line in every report; versions and logs for code and configuration files); the "waiting review" sentences marked superseded in place; section 7 shows the README at v0.1.1; "Where this stopped" extended.

### `docs/drafts/README.md`
- **v0.1.1:** the "known tension" about the Forge gate is marked resolved in principle by decision 36 (the two draft specifications themselves are unchanged).

### Files not changed
`docs/upstream/forge/research.md` (v0.4.55), `source-map.json` (v0.1.10), `version-index.json` (v0.1.3), `metadata-inventory.json` (v0.1.0), `vanilla-metadata-table.json` (v0.1.3), `docs/upstream/fabric/research.md` (v0.1.0), `docs/architecture/identity-and-collision.md` (v0.1.12), `docs/architecture/startup-build-stage.md` (v0.1.3), `handoff/NEW-CHAT-PROMPT-STAGE-2.md` (v1.0, kept as the historical prompt), and the two draft specifications.

### Package delivered with this version
`MASTER-SPEC-STATUS.md` (the same file as inside the zip) and `VESSEL-docs.zip` (the whole package), in the same message as the report of pass 27.

---

## 2026-10-09 (hand-off: input prompt for the new chat)

Dates come from the container clock (UTC). No research pass; no research file, source map or identity file changed (research stays at v0.4.55, source-map v0.1.10, identity v0.1.12).
Shiroz asked for the input prompt after the Forge research reached 100%. **Written: `handoff/NEW-CHAT-PROMPT-STAGE-2.md`** (Vietnamese; also delivered as a separate file): the order of work he fixed (stage A: deep study of the architecture of Sinytra Connector, reference only; stage B: the project website, no repository yet; stage C: all other loaders), the working rules carried over from the Forge stage, the questions that must be asked first, the conflict between the loader draft's "Forge gate" (which counts the Forge adapter and end-to-end tests as part of "Forge = 100%") and his order of work (to be resolved as a new decision 36 in the new chat), the policy for upgrading the two drafts (including the mapping policy of decision 33), and an appendix of **unverified** starting notes on Sinytra Connector (repositories, branches, head commits and licences as seen on 2026-10-07 and 2026-10-08).
The two draft specifications he uploaded on 2026-10-07 are copied **unchanged** into `docs/drafts/` with a new README (version 0.1.0); they are outside the research counts.
Choices made by Claude because Shiroz did not answer the questions about the shape of the prompt (one prompt or several, language, whether to embed the drafts): one prompt with three stages; Vietnamese for the prompt and for the new chat's replies, English for the docs (as before); the drafts are kept in the zip and the originals are to be attached too if possible.
Facts checked before writing: the drafts' sizes and section counts (2,183 lines, 52 sections and 2 appendices; 1,934 lines, 72 sections); the wording of the loader draft's Forge gate (sections 2.1 to 2.3 and 49); the head commits and licence files of the Sinytra clones.
Mistakes found while writing: none that reached the package; the first reading of the website draft's header ("Design contract for implementation") was added to the prompt so that the new chat does not mistake it for a final contract.

### `MASTER-SPEC-STATUS.md`
- **v1.0.45:** section 7 lists the new folders and the prompt; an update note in section 8; "Where this stopped" extended; no count, decision or percentage changed.

### New files
- `handoff/NEW-CHAT-PROMPT-STAGE-2.md` (version 1.0 in its header).
- `docs/drafts/README.md` (v0.1.0) and the two draft specifications, unchanged.

### Package delivered with this version
`NEW-CHAT-PROMPT-STAGE-2.md` (the same file as inside the zip), `MASTER-SPEC-STATUS.md` and `VESSEL-docs.zip` (the whole package), in the same message as the report.

---

## 2026-10-08 (pass 26, Forge): the last six rows; the Forge research reaches 100%; decision 35

Dates come from the container clock (UTC; the pass ran from 2026-10-07 22:32 to 2026-10-08 22:56, with an interruption for a side study, see below). New this pass: the full Forge repository history (blob-less clone, 11,821 commits), the Forge 26.3 loader and network code, raw files of Forge branches for the coremod and Mixin checks, the Maven metadata, and NeoForged item pages (sources S157 to S161).
**Order of work clarified by Shiroz (2026-10-07):** in this chat only the Forge research is finished to 100%; then a complete input prompt is written for a new chat, where first the architecture of Sinytra Connector is studied in depth, then the website (no repository exists yet), then all other loaders. Two draft specifications uploaded by Shiroz (distribution and loader architecture; website) are drafts only and are to be upgraded in the new chat. **Before that clarification Claude started a Sinytra Connector study (repositories Connector, Adapter, Launchpad, ForgifiedFabricAPI, ConnectorExtras cloned and read in part) and stopped it on Shiroz's instruction; nothing from it was written into the package** (the notes will go into the new-chat prompt as starting points, marked as unverified).
Result: rows 32, 10, 31, 29, 2 and 16 PARTIAL to DONE. **The Forge count reaches 100% (37 DONE of 37, 0 PARTIAL, 0 NOT STARTED, recomputed by a script from the table)**; the overall estimate moves from 6.0% to 6.3% (the 23 phase percentages add up to 145, checked by a script). One DONE row carries the label "documented limit" (row 21); rows 2 and 16 are DONE only through **decision 35**, a provisional scope decision made by Claude that Shiroz can reverse (reversing one gives 98.6%, both 97.3%).
Findings worth remembering: the full Forge history dates the builds for Minecraft 1.1, 1.2.3, 1.2.4 (on ModLoaderMP) and 1.2.5 and shows that FML joined Forge only in July 2012, so the assumption written for decision 34 is contradicted (the line stays at 1.2.5; the E1b adapter must also load FML-2.x mods for 1.2.5); JavaScript coremods end at the branch 1.21.10; `mods.toml` has no `[[mixins]]` table; at 26.3 a filter keeps non-`minecraft` attributes away from vanilla connections; the Maven metadata as fetched was an older snapshot, with a `latest` field that names an old build.
Mistakes found while writing: (1) the first draft of the networking text gave the configuration-phase task order wrongly and said a mismatch refuses the connection; the code gives `RegisterChannelsTask` first, and only shows that a mismatch result is sent to the client; corrected before it was written; (2) the draft of the shim list said the client and server sets differ only by a few animation and key methods; a name comparison gave eight client-only names and one server-only name, and the text was corrected; (3) the draft of 40.4 named a branch and a commit author as facts; the commit messages only call the line `newfmlmod`, so the text now says so; (4) the first version of the count sentence in 4.1 and in decision 35 said "36 of 37" for reversing one part and used a wrong phrase for reversing both; fixed before the package was built; (5) the Maven metadata fetch ignored the token limit I set and returned a very long text; no data was lost, but the fetch should be asked for with a path that is smaller.

### `docs/upstream/forge/research.md`
- **v0.4.55:** new section 40 (40.0 to 40.8); sources S157 to S161 added; header, research dates and status line updated; dated notes added at 36.7, 38.6, 39.6 and in the era note of section 2 (the refinement of decision 34); pointer added in section 15 item 11. Size 379912 bytes (was 358111).

### `docs/upstream/forge/source-map.json`
- **v0.1.10:** a `pass26_history` entry (history facts, conf commits per Minecraft version, FML entry dates, coremod and MixinExtras branch limits, Maven fetch facts); the file stays valid JSON.

### `MASTER-SPEC-STATUS.md`
- **v1.0.44:** decision 35 added and decision 34 refined; rows 2, 10, 16, 29, 31 and 32 rewritten in 4.1; the pass 26 paragraph and a count-history row added; phase 7 set to 100% (research part) and the overall estimate to about 6.3%; the update of 2026-10-08 added to section 5; sections 7, 8 and 9 and "Where this stopped" brought up to date; two tooling notes (background history clone); the source range is now S1 to S161.

### Files not changed
`docs/architecture/identity-and-collision.md` stays at v0.1.12.

### Package delivered with this version
`MASTER-SPEC-STATUS.md` (the same file as inside the zip) and `VESSEL-docs.zip` (the whole package), in the same message as the report of pass 26.

---

## 2026-10-07 (pass 25, Forge): leftovers of rendering hooks, recipes, loot tables, the scheduler and items; decision 34

Dates come from the container clock (UTC; the pass ran from 2026-10-06 22:29 to 2026-10-07 03:27). New this pass: a blob-less clone of the Forge docs repository with all branches (to ask about file history), the Minecraft Wiki page on item model definitions, raw files of Forge branches 1.20.1 to 26.2 (sources S153 to S156); the docs trees of pass 23 were reused.
Shiroz answered "yes" on 2026-10-06 to the proposal of pass 24: the line between E1a and E1b moves to Minecraft 1.2.5. That is decision 34; the era table of `research.md` section 2 is changed (E1a 1.1 to 1.2.4, 3 versions; E1b 1.2.5 to 1.7.10, 20 versions; total 78). The plan of decision 30 was re-planned: pass 25 took the gameplay leftovers 16, 20, 21, 22, 23; pass 26 takes 2, 10, 16, 29, 31, 32.
Result: rows 20, 21, 22 and 23 PARTIAL to DONE; row 16 stays PARTIAL with a narrower gap. **Row 21 is the first row with the label "documented limit"** (the Forge docs page "Model Extensions: Introduction" never existed: listed in the navigation, never touched by any of 502 commits on 15 branches, 404 on eight branches). The Forge count moves from 86.5% to 91.9% ((31 + 0.5 x 6) / 37; 31 DONE, one with the label, 6 PARTIAL, 0 NOT STARTED, recomputed by a script from the table), the overall estimate from 5.7% to 6.0% (the 23 phase percentages add up to 137, checked by a script).
Findings worth remembering: at 26.3 a repeated geometry loader id throws; `forge:conditional` keeps its id on every branch from 1.21.1 to 26.3 but changes shape at 26.3 and is never sent over the network; `BrewingRecipeRegistry` and `IBrewingRecipe` do not exist at 26.3; the loot-table load event is fired from a generic hook that runs after any JSON element is parsed; Forge has an event that adds default data components to items; the vanilla property lists for item model definitions come from a community wiki (class C), the only complete list found.
Mistakes found while writing: (1) the draft of section 39 said the `conditional` registration was checked on the 1.20.x branches; only the class file was checked there, and the text was corrected before it was written into the package; (2) the draft described the loot-table load event as fired directly by the event factory; a word search for the caller (the first search had used a wrong path to the patches) found the generic JSON hook, and the text was corrected; (3) the draft counted eight block patches that call scheduling or random ticking; the search had been cut at eight, the full count is nine; (4) the first attempt to ask GitHub's API about the docs history was rate-limited and returned nothing; the answer came from a blob-less clone instead; (5) one status-file edit pattern from the previous pass used the wrong wording of a sentence and had to be fixed before the third run, as recorded in the pass 23 entry (no new failure of that kind this pass).

### `docs/upstream/forge/research.md`
- **v0.4.54:** new section 39 (39.0 to 39.8); the era table rows E1a and E1b rewritten and a dated note added (decision 34); sources S153 to S156 added; header, research dates and status line updated; pointer added in section 15 item 11. Size 358111 bytes (was 340157).

### `docs/upstream/forge/source-map.json`
- **v0.1.9:** a `pass25_clones` entry (docs history facts, raw-branch checks, wiki revision); the file stays valid JSON.

### `MASTER-SPEC-STATUS.md`
- **v1.0.43:** decision 34 added and the plan of decision 30 re-planned; rows 16, 20, 21, 22 and 23 rewritten in 4.1; the pass 25 paragraph and a count-history row added; the era model rows noted; phase 7 set to about 92% and the overall estimate to about 6.0%; the update of 2026-10-07 added to section 5; sections 7, 8 and 9 and "Where this stopped" brought up to date; a tooling note on proving that a page never existed; the source range is now S1 to S156.

### Files not changed
`docs/architecture/identity-and-collision.md` stays at v0.1.12 (the repeated-id stop of geometry loaders matches the existing hard-stop rule and needed no new section; it is recorded in `research.md` 39.1).

### Package delivered with this version
`MASTER-SPEC-STATUS.md` (the same file as inside the zip) and `VESSEL-docs.zip` (the whole package), in the same message as the report of pass 25.

---

## 2026-10-06 (pass 24, Forge): ModLoader compatibility, E2 launch flow, threading per era

Dates come from the container clock (UTC; the pass ran on 2026-10-06). New this pass: the FML repository (branches and tags), the old Forge tags `1.0.0` to `1.3.2`, Mojang's LaunchWrapper repository, six more Forge branches and the Forge 1.12.x docs (sources S148 to S152). Old tags were read without cloning whole histories (blob-less fetches, see the tooling notes of the status file).
Result: row 30 (E2 launch flow) and row 34 (threading per era) NOT STARTED to DONE, row 31 (ModLoader compatibility) NOT STARTED to PARTIAL. No row is NOT STARTED any more. The Forge count moves from 79.7% to 86.5% ((27 + 0.5 x 10) / 37; 27 DONE, 10 PARTIAL, 0 NOT STARTED, recomputed by a script from the table), the overall estimate from 5.4% to 5.7% (the 23 phase percentages add up to 131, checked by a script). No row uses the "documented limit" label.
Findings worth remembering: the section 2 conflict about the end of ModLoader support is not a conflict (FML 1.6.4 loads ModLoader mods, FML 1.7.10 ignores them); FML with `@Mod` and the ModLoader layer already exists in the tags for Minecraft 1.2.5 (May 2012), while the early Forge tags `1.0.0` to `1.3.2` (for Minecraft 1.0.0 and 1.1) are a patch set that needs a real ModLoader underneath; at 26.3 `RegisterCapabilitiesEvent` does not exist; 1.7.10 entity extended properties rename a repeated identifier by appending a counter. A proposal waits for Shiroz: move the E1a/E1b line to Minecraft 1.2.5 (the era table is unchanged until he decides).
Mistakes found while writing: (1) the draft of section 38 gave the number of methods of `BaseModProxy` twice in different ways (17, then 21); a recount from the file gave 21 and the text was corrected before it was written into the package; (2) section 2 and the status file had stated the end of ModLoader support as unresolved and the early Forge tags as a compatibility layer; both are now annotated with dated notes pointing to section 38 instead of rewriting the old text; (3) the first reading of the tag list suggested the numeric tags `23.5` to `66.0` might be early Forge builds; their dates (2018 to 2026) show they are the modern versions.

### `docs/upstream/forge/research.md`
- **v0.4.53:** new section 38 (ModLoader compatibility, E2 launch flow, threading per era; 38.0 to 38.6); sources S148 to S152 added; header, research dates and status line updated; dated notes added in section 2 (two places) and in section 15 items 3, 4 and 10. Size 340157 bytes (was 315204).

### `docs/upstream/forge/source-map.json`
- **v0.1.8:** a `pass24_clones` entry (Forge branches, FML branches and tags read, old Forge tags with their Minecraft versions, LaunchWrapper, docs 1.12.x); the file stays valid JSON.

### `docs/architecture/identity-and-collision.md`
- **v0.1.12:** one paragraph added at the end of section 5.4 (1.7.10 extended properties rename a repeated identifier; not a registry key) and a change note in the header.

### `MASTER-SPEC-STATUS.md`
- **v1.0.42:** rows 30, 31 and 34 rewritten in 4.1; the pass 24 paragraph and a count-history row added; phase 7 set to about 86% and the overall estimate to about 5.7%; the update of 2026-10-06 added to section 5 (with the waiting proposal); sections 7, 8 and 9 and "Where this stopped" brought up to date; a tooling note on blob-less fetches; the source range is now S1 to S152. No new numbered decision.

### Package delivered with this version
`MASTER-SPEC-STATUS.md` (the same file as inside the zip) and `VESSEL-docs.zip` (the whole package), in the same message as the report of pass 24.

---

## 2026-10-05 (pass 23, Forge): worldgen, data generation, source of official-name data; decision 33

Dates come from the container clock (UTC; the pass ran on 2026-10-05). The 26.3 clone is the same head as in passes 19 to 22 (`939fb13d`). New this pass: the docs of Forge, NeoForged and Fabric were read from their GitHub repositories (markdown source) and MCPConfig was cloned for Forge's own mapping notes (sources S143 to S147).
Shiroz answered the question of the pass 22 report on 2026-10-05: choose it myself, preferably from Mojang, consult more sources, data from jars is acceptable. That is decision 33 (order of sources; Mojang's file is downloaded at run time and cached, never shipped; offline servers load the module only if the owner supplies the data, otherwise it is not loaded, with a message).
Result: rows 25 (worldgen) and 26 (data generation) NOT STARTED to DONE under decision 28. The Forge count moves from 74.3% to 79.7% ((25 + 0.5 x 9) / 37; 25 DONE, 9 PARTIAL, 3 NOT STARTED, recomputed by a script from the table), the overall estimate from 5.2% to 5.4% (the 23 phase percentages add up to 125, checked by a script). No row uses the "documented limit" label.
Findings worth remembering: the Forge docs have no worldgen page at all; the Forge datagen docs describe classes that are not in the 26.3 tree (the model generator package, `DatapackBuiltinEntriesProvider`, `ForgeAdvancementProvider`); `GatherDataEvent` is created only by the datagen loader; the reference copy of Mojang's mapping licence named in Forge Gradle's warning is gone (404, and not in the MCPConfig tree), so its wording is known only from log excerpts.
Mistakes found while writing: (1) the draft of section 37 said `common/world` had "13 files, 7 in detail" and that Forge's own datagen adds "eight tag providers"; checking against the code gave 12 files (and a smaller detail list) and seven tag providers, and the text was corrected before it was written into the package; (2) the first two runs of the script that updates the status file stopped at an assertion (a sentence that occurs twice, and a pattern that used the wrong wording of a sentence I wrote in v1.0.40); the script writes only at its end, so nothing wrong was written, and the third run was verified by recounting the table; (3) the first run of the script that writes this very entry failed on a string-format error, so the first zip was built without it; the zip was rebuilt after this entry was written, and the new zip was checked to contain it; (4) the Javadoc of `ForgeBiomeModifiers` gives type names that differ from the registered ones (`forge:removefeatures`, and `forge:add_spawns` for the removal of spawns); the registered names are used and the difference is recorded in `research.md` 37.1.

### `docs/upstream/forge/research.md`
- **v0.4.52:** new section 37 (worldgen, data generation, the source of official-name data; 37.0 to 37.6); sources S143 to S147 added; header, research dates and status line updated; pointers added in section 15 item 11, the docs map of section 19 (Data Generation now read) and 36.7. Size 315204 bytes (was 288639).

### `docs/upstream/forge/source-map.json`
- **v0.1.7:** a `pass23_clones` entry for the docs repositories and MCPConfig used in this pass (the file stays valid JSON).

### `MASTER-SPEC-STATUS.md`
- **v1.0.41:** decision 33 added to section 2 and decision 32 pointed at it; rows 25 and 26 rewritten in 4.1; the pass 23 paragraph and a count-history row added; phase 7 set to about 80% and the overall estimate to about 5.4%; the update of 2026-10-05 added to section 5 (the question of pass 22 is answered, no question waits); sections 7, 8 and 9 and "Where this stopped" brought up to date; a tooling note on reading docs from repositories; the source range is now S1 to S147.

### Files not changed
`docs/architecture/identity-and-collision.md` stays at v0.1.11 (no new key space was found; the provider-name rule of 37.2 is a dev-time rule inside one generator).

### Package delivered with this version
`MASTER-SPEC-STATUS.md` (the same file as inside the zip) and `VESSEL-docs.zip` (the whole package), in the same message as the report of pass 23.

---

## 2026-10-05 (pass 22, Forge): entities, commands, scheduler, Mojang's official names; decisions 30 to 32

Dates come from the container clock (UTC). The research of this pass ran from 2026-10-04 (clock 12:19) to 2026-10-05 (clock 09:30). The 26.3 clone is the same head as in passes 19 to 21 (`939fb13d`).
Shiroz delegated three choices on 2026-10-04 (how many rows per pass, how to count a row whose last part cannot be obtained, which class B source per row); they are decision 30 with the reasoning written down. On 2026-10-05 he answered "yes" to the question of the pass 22 report (selector tokens and argument-type classes are registry keys, decision 31) and added a requirement: mods that use Mojang's official mappings must load (decision 32).
Result: rows 17 (entities) and 19 (commands) NOT STARTED to DONE, row 20 (scheduler) NOT STARTED to PARTIAL, all under decision 28. The Forge count moves from 67.6% to 74.3% ((23 + 0.5 x 9) / 37; 23 DONE, 9 PARTIAL, 5 NOT STARTED, recomputed by a script from the table), the overall estimate from 4.9% to 5.2% (the 23 phase percentages add up to 119, checked by a script).
Mistakes found while writing this entry: (1) a first word search limited to `src/main` made it look as if `SpawnPlacementRegisterEvent` was never fired; it is fired from a patch, so searches must cover `patches/` too (recorded in `research.md` 36.0 and in the tooling notes of the status file); (2) the "Research status" line of `research.md` stopped at pass 18; the phrases for passes 19 to 21 were added now; (3) the report of the previous message gave the row counts before they were written; the script recount of the written table gives the same numbers, so nothing had to be corrected; (4) section 35.4 said no Javadoc mirror page was opened; search extracts of four `TickEvent` pages were read in this pass and a dated note was added there instead of changing the old text.

### `docs/upstream/forge/research.md`
- **v0.4.51:** new section 36 (entities, commands, the scheduler, Mojang's official names; 36.0 to 36.7); sources S132 to S142 added; header, research dates and the status line updated; pointers added in section 15 item 11, the docs map of section 19 (Synchronizing Entities now read) and 35.4. Size 288639 bytes (was 262750).

### `docs/upstream/forge/source-map.json`
- **v0.1.6:** a `pass22_clones` entry for the 26.3 head used in this pass (the file stays valid JSON).

### `docs/architecture/identity-and-collision.md`
- **v0.1.11:** new section 5.5 (selector tokens and argument-type classes as registry keys, decision 31) and one new open item in section 9.

### `MASTER-SPEC-STATUS.md`
- **v1.0.40:** decisions 30, 31 and 32 added to section 2; rows 17, 19 and 20 rewritten in 4.1; the pass 22 paragraph and a count-history row added; phase 7 set to about 74% and the overall estimate to about 5.2%; the update of 2026-10-05 added to section 5 with one waiting question (the source of the data for translating official names on obfuscated versions); sections 7, 8 and 9 and "Where this stopped" brought up to date; the source range is now S1 to S142.

### Package delivered with this version
`MASTER-SPEC-STATUS.md` (the same file as inside the zip) and `VESSEL-docs.zip` (the whole package), in the same message as the report of pass 22.

---

## 2026-10-04 (handoff package for a new chat; decision 29)

Shiroz answered the four open questions left after pass 21 and asked for the work to be packaged like his first message, because he continues in a new chat. No research pass was run in this version; the count stays at 67.6% (overall 4.9%).
His answers: (1) the order of the remaining rows is free as long as every row is covered; (2) the PayBot cookbook is reference only and is not copied into the package; (3) he does not know whether the runtime names of Forge 1.21.x are SRG or official, so that check stays an open research gap; (4) namespaced ids are never aliased across namespaces (`forge:x`, `c:x`, `fabric:x` and `neoforge:x` are completely different names), recorded as decision 29.
Mistakes found while writing this entry: my reading of "in any order" first covered only the eight NOT STARTED rows; the eight PARTIAL rows are open too, so "completeness" means all 16 rows not yet DONE, and the status file now says so. Two edit scripts stopped on an error before writing (a missing function argument and a wrong anchor); each time the files were checked and the edit applied again, so no half-written state was left, except that `research.md` v0.4.50 was written one step before the other files.

### `docs/upstream/forge/research.md`
- **v0.4.50:** the open question about aliasing `forge:` and `c:` tags in 35.6 marked as answered in place (decision 29).

### `docs/architecture/identity-and-collision.md`
- **v0.1.10:** a paragraph in section 5.3 stating that namespaced ids are never aliased across namespaces, tags included.

### `MASTER-SPEC-STATUS.md`
- **v1.0.39:** decision 29 added to section 2; the answers of 2026-10-04 recorded in sections 5 and 9; section 8 now says the order is free and has starting notes for the eight NOT STARTED rows and a pointer to the eight PARTIAL rows; section 9 rules extended (full zip after each pass, passes 19 to 21 done, the cookbook stays out of the package), the question list emptied and "where this stopped" rewritten; section 7 names the new versions of `research.md` and `identity-and-collision.md`.

### Package delivered with this version
`MASTER-SPEC-STATUS.md` (the same file as inside the zip), `VESSEL-docs.zip` (the whole package) and `NEW-CHAT-PROMPT.md` (text to paste as the first message of the new chat).

---

## 2026-10-04 (pass 21, Forge): conditional data formats, tags list, rest of the vanilla metadata table, Javadoc inventory

Dates come from the container clock (UTC) and file times. The research of this pass ran from 2026-10-03 to 2026-10-04. Shiroz did not answer the two questions left open after pass 20; the pass did not depend on them and they stay open (status file, section 9), together with two new ones (tag aliasing and the order of the not-started rows).
Result: no row of the 37-row count changed (67.6%, overall 4.9%). This is the third pass in a row without a change, because passes 19 to 21 closed leftovers inside PARTIAL rows.
One correction made during the pass: in `vanilla-metadata-table.json` v0.1.1 I had explained the era of the Bukkit snapshot with block names from memory and a guess about a rebase; v0.1.2 replaced that with what the Forge sources show (double plant, log2 and stained glass are absent from the 1.6.4 head and present from the 1.7.2 head) and marks the rebase explanation as not checked.

### `docs/upstream/forge/research.md`
- **v0.4.48:** added section 35 (conditional data of 1.20.4 and 1.21.1, which gives four formats by era instead of three; the tags list from the code of 12 heads; the rest of the vanilla metadata table; the Javadoc inventory; source revision per era with a tag probe; what this means for VESSEL; what is missing); update notes added to 25.6, 28.2, 28.7, 29.3, 31.2 and 31.6; the research dates of the header now name pass 21.
- **v0.4.49:** sources S123 to S131 added to the sources table.

### `docs/upstream/forge/vanilla-metadata-table.json`
- **v0.1.1:** new kinds `cocoa`, `end_portal_frame`, `vine`, `powered_and_detector_rail` and `huge_mushroom`; the comparator got a `mode_and_power` field (`unknown`); new sources named in the file; counts recomputed (55 fields: 33 corroborated, 15 single source, 3 convention unresolved, 2 conflict, 2 unknown).
- **v0.1.2:** the first entry of `known_source_problems` corrected as described above.
- **v0.1.3:** found by the consistency check of this pass: the two fields of the `slab` kind (from pass 17) named a source `minecraft_data_1.8` that is not defined in `sources`; they now name the defined source `minecraft_data_1.7_and_1.8`. No value changed.

### `docs/upstream/forge/source-map.json`
- **v0.1.5:** new key `pass21_clones` (the Bukkit head, the Forge branches `1.6`, `v164-pregradle` and `mc172`, and 15 tags with date and subject). Compared with a backup: only `document_version` and `pass21_clones` differ.

### `MASTER-SPEC-STATUS.md`
- **v1.0.37:** rows 22 and 29 re-worded (both stay PARTIAL); a pass 21 paragraph in 4.1 and a row in 4.3; versions and size in sections 3 and 7; a pass 21 update in section 5; sections 8 and 9 (what is still unread, passes 19 to 21 done, source numbering S1 to S131, where this stopped, four open questions).
- **v1.0.38:** section 7 now names `vanilla-metadata-table.json` v0.1.3.

---

## 2026-10-03 (pass 20, Forge): threading, trigger registration, duplicate keys before 1.19.2, the same checks on more branches

Dates in this entry come from the container clock (UTC) and from file times. The research of this pass ran from 2026-10-02 (clones at 10:49 UTC) to 2026-10-03. The same file times show that pass 19 was written between 2026-10-01 (clone at 14:20 UTC) and 2026-10-02 (docs at 04:25 UTC), so its entries below, dated 2026-10-01, name the day it started; they were left as written.
Shiroz answered "both" on the question of what counts as DONE for the not-started gameplay API rows (decision 28) and approved the defaults of the previous turn ("Ok": full zip after each pass, fixing the two stale places). On 2026-10-03 he also supplied a PayBot mapping cookbook as a reference only (source S122, not added to the package).
Result: no row of the 37-row count changed (67.6%, overall 4.9%).

Corrections found while working (all in the status file):
- The date of decision 28 and of the v1.0.34 update paragraph was written as 2026-10-01; the file times show 2026-10-02. Corrected in v1.0.35 with the old value mentioned in place.
- Decision 28 and section 8 of v1.0.34 listed the old-era rows as "30 and 31" and left out row 34 (the memory note and the question asked to Shiroz had all three). Row 34 restored in v1.0.35.
- The earlier claim of 28.5 that Forge has no trigger registration code came from a search that did not cover the `forge-tests` module; superseded in place in `research.md` 28.5 and 28.6 and in decision 20.

### `docs/upstream/forge/research.md`
- **v0.4.46:** added section 34 (threading and listener order in the loading stages of 26.3; the same loading code on older heads; trigger registration; duplicate registry keys before 1.19.2; the four checks of section 32 on more heads; the reference document of S122; what this means for VESSEL; what is missing); update notes added to 26.8, 27.3, 27.6, 28.5, 28.6, 28.7, 32.1 and 32.3; the research dates of the header now name pass 20.
- **v0.4.47:** sources S119 to S122 added to the sources table.

### `docs/upstream/forge/source-map.json`
- **v0.1.4:** new key `pass20_clones` with the heads of the 20 branches read in pass 20 (1.7.10, 1.8.9, 1.10.x, 1.11.x, 1.12.x, 1.16.x, 1.18.x, 1.19.x, 1.20.1, 1.20.4, 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1, 26.2, 26.3). Compared with a backup: only `document_version` and `pass20_clones` differ.

### `docs/architecture/identity-and-collision.md`
- **v0.1.9:** section 5.3 got a follow-up (trigger ids are entries of a vanilla registry from the 1.20.4 head on, so the special rule is needed only up to 1.20.1); section 5.4 got the older eras (1.12.x same structure; in 1.7.10 Forge throws on a duplicate name).

### `MASTER-SPEC-STATUS.md`
- **v1.0.35:** the two corrections above; the date line moved to 2026-10-03.
- **v1.0.36:** pass 20 results: decisions 8 and 20 got era precisions (the old text stays, marked superseded in part), rows 6, 8, 10 and 34 re-worded (no row category changed), a pass 20 paragraph in 4.1 and a row in 4.3, versions and size in sections 3 and 7, a pass 20 update in section 5 (including the two statements of the reference document that do not match the Forge trees), sections 8 and 9 (what is still unread, two new rules, source numbering S1 to S122, where this stopped, two open questions).

---

## 2026-10-01 (pass 19, Forge): client and item side at 26.3, plus decisions 26 and 27

Shiroz answered the questions asked before the pass: default configs live in `vessel-config/defaults/forge/` (option A, decision 26); no decisions were missing from the docs; the remaining Forge work is split into three passes (option A); conflicting sources are settled with a reputable source and otherwise marked as a conflict with the reason (decision 27); the version and log rules stand.
His answer "B + A" about the not-started gameplay API rows is ambiguous and is recorded as an open question, with the stricter reading applied meanwhile (status file, section 9). His one-word "Đúng" to the docs-and-log question was taken as confirming the rules and as agreement to fix the two stale places in the status file.
The pass itself checked what replaced item overrides, item properties and item renderers at 26.x, and read the model extension pages against the 26.3 code. No row of the 37-row count changed (67.6%, overall 4.9%).

### `docs/upstream/forge/research.md`
- **v0.4.42:** added section 33 (what replaced item overrides, properties and renderers; the model extension pages against the 26.3 code; seen in passing; implications for VESSEL; what is missing); update notes added to sections 23.7, 25.6, 29 and 32.3; the research dates in the header now name pass 19.
- **v0.4.43:** the Part Visibility page was fetched in full (it had first been seen only as search extracts); the code path of `ForgeBlockModelData#merge` is described with its caller and the child and parent order; the "still missing" list of 33.5 was corrected. The first attempt to apply this edit stopped on a script error before writing; the file was checked unchanged (`cmp`) and the edit was applied again.
- **v0.4.44:** sources S113 to S118 added to the sources table.
- **v0.4.45:** the open question about default configs in section 29.2 marked as answered in place (decision 26).

### `docs/upstream/forge/source-map.json`
- **v0.1.3:** new entry `clones_read."26.3_pass19"` (head `939fb13d3b96`, 2026-09-30), because the head of branch 26.3 moved after the clone of pass 18. Keys and values were compared with a backup: only `document_version` and `clones_read` differ.

### `docs/architecture/startup-build-stage.md`
- **v0.1.3:** `vessel-config/defaults/forge/` added to the folder tree of section 9 with a short explanation, and one bullet added to the open items of section 10 (decision 26).

### `MASTER-SPEC-STATUS.md`
- **v1.0.31:** decisions 26 (default configs location) and 27 (conflicting sources) added to section 2; the three places in section 5 that still said "waiting for Shiroz" about the default configs were marked as answered in place; new update paragraph in section 5.
- **v1.0.32:** stale text marked in place: the date line, the heading and source line of 4.1, and the pass 5 result paragraph of 4.1 (marked superseded; it was not deleted).
- **v1.0.33:** pass 19 results: rows 16 and 21 re-worded (both stay PARTIAL), a pass 19 paragraph in 4.1 and a row in 4.3 (count unchanged), version numbers and size in section 3 and section 7, a pass 19 update in section 5, section 8 (what is still unread, and the three-pass plan), section 9 (two new rules, the open question about "B + A", source numbering S1 to S118, where this stopped).

---

## 2026-10-01 (handoff preparation)

Shiroz asked whether to move to a new chat. Before answering, the package was checked for consistency: file versions match the file list in section 7 of the status file, decisions are numbered 1 to 25 without gaps,
the four JSON files parse, the LOG is newest-first, and the sources table has no duplicate numbers (S69 is intentionally unused, noted below).

### `MASTER-SPEC-STATUS.md`
- **v1.0.30:** new section 9 (handoff notes for a new session): reading order, Shiroz's working rules, how percentages are counted, the one open question, tooling lessons, process mistakes to avoid, the note on S69, the
  note that the original v3 specification is not in the package, and where the work stopped. No row, decision or percentage changed.

---

## 2026-10-01 (pass 18, Forge): the four unchecked register rows

The session crossed midnight, so this and later entries use 2026-10-01. Checked the 26.3 tree for recipes (crafting grid size), tinting, the client item API and the `@Mod` constructor rule with event stages.

### `docs/upstream/forge/research.md`
- **v0.4.40:** added section 32 (the four rows, side findings, implications for VESSEL, what is missing); the register in section 29 got an update note.
- **v0.4.41:** source S112 added, header updated. File size 193945 to 199898 bytes.

### `MASTER-SPEC-STATUS.md`
- **v1.0.29:** rows for lifecycle, rendering hooks, items and recipes re-worded; **no category change**, so the Forge count stays 67.6% (about 68%) and the overall estimate about 4.9%. Count history, file list, next-step text and section 5 updated.
- Process note: pass 17 was written and zipped but its report was not sent before this pass started; its report goes out in the same reply as this one.

---

## 2026-09-30 (pass 17, Forge): the vanilla metadata table (decision 25)

Shiroz chose option B with option C as reference only (a value from such a cache is used only if proven by another source). I did not download the third-party cache because its own description lists
Mojang's client, server and merged JARs. As independent references I cloned `Bukkit/Bukkit` (shallow) and read 17 classes of `org.bukkit.material`, and compared them with Forge 1.7.10's `RotationHelper` and the wiki.

### `docs/upstream/forge/vanilla-metadata-table.json` (new, v0.1.0)
- Five confidence levels, the rule for option C, 25 entries (of which 24 with fields), per-field confidence counts {'corroborated': 29, 'single_source': 9, 'convention_unresolved': 2, 'conflict': 2}, known source problems.

### `docs/upstream/forge/research.md`
- **v0.4.38:** section 30.4 records the decision; new section 31 (31.1 why the cache was not downloaded, 31.2 the Bukkit API reference, 31.3 agreements and disagreements, 31.4 the rule for the table, 31.5 status, 31.6 what is missing).
- **v0.4.39:** sources S110 and S111 added, header updated. File size 187097 to 193945 bytes.

### `MASTER-SPEC-STATUS.md`
- **v1.0.28:** decision 25 added, file list (new JSON), next-step text and section 5 updated. No row of the 37-row list changed (the Forge count stays 67.6%, overall about 4.9%).
- Caveat recorded: the Bukkit snapshot's commit is dated 2013-07-07 but its pom says 1.7.10, so it may be older than 1.7.10.
- The question about where default configs live is still unanswered by Shiroz; it was not assumed.

---

## 2026-09-30 (pass 16, Forge): pre-1.8 vanilla block metadata

Read: the Minecraft Wiki pre-flattening data values page (fetched, cut off after the torches section), the `variations` of minecraft-data 1.7 and 1.8, and again the Forge 1.7.10 `RotationHelper`
table. No decompiled vanilla code was read: decompiling needs Mojang's game files, which are not reachable here, and a third-party cache of decompiled sources was deliberately not used.

### `docs/upstream/forge/metadata-inventory.json` (new, v0.1.0)
- 60 block and item kinds with metadata range, meaning and a category, value tables for 10 groups, three conflicts between sources, and an explicit list of what is not covered.

### `docs/upstream/forge/research.md`
- **v0.4.36:** added section 30 (30.1 sources and limits, 30.2 what the summary table shows, 30.3 conflicts, 30.4 decision needed with three options, 30.5 implications, 30.6 what is missing).
- **v0.4.37:** sources S107 to S109 added, header updated. File size 179706 to 187097 bytes.

### `MASTER-SPEC-STATUS.md`
- **v1.0.26:** file list (new JSON), next-step text and section 5 updated with one open question. **No row of the 37-row list changed**, because the metadata layer is not one of its rows, so the Forge
  count stays 67.6% and the overall estimate about 4.9%.
- Correction made right after writing: `MASTER-SPEC-STATUS.md` section 5 first said the inventory has 64 kinds; the actual count is 60 (fixed in v1.0.27). Section 30 and the JSON file were correct.
- Correction to an earlier statement: section 26.2 said the minecraft-data 1.7 files list display names only. That is still true, but the pass showed the "1.7" directory also contains post-1.7 content, so it
  must not be used as a 1.7.10 reference. Added in 30.1 and 30.3.
- Process note: the previous pass (15) was written and zipped, but the message reporting it was not sent before pass 16 started. Its report is in the same reply as pass 16's.

---

## 2026-09-30 (pass 15, Forge): re-verification register

Shiroz asked to redo the already researched parts before moving on to the pre-1.8 vanilla code. Searched the 26.3 tree for 26 earlier topics (sides, event buses, lifecycle, configs,
capabilities, networking, update checker, access transformers, ModLauncher, Mixin, loot modifiers, loot conditions, conditional data, ingredients, tags, model loaders, block entities, registries,
mods.toml keys, triggers, creative tabs, plus four topics that were not checked).

### `docs/upstream/forge/research.md`
- **v0.4.33:** added section 29 (the register with 13 confirmed, 1 partly confirmed, 7 changed, 1 absent, 4 not checked; answers to earlier open items; implications for VESSEL).
- **v0.4.34:** source S106 added, header updated. File size 169734 to 179607 bytes.

### `MASTER-SPEC-STATUS.md`
- **v1.0.24:** rows `defaultconfigs` (to DONE) and `[[mixins]]` (to PARTIAL) changed, rows for sides and events re-worded, Forge phase 67.6% (reported about 68%), overall about 4.9%, count history, file list,
  next-step text and section 5 updated (one new question for Shiroz about where default configs live).
- Correction right after writing: section 29.3 named the "not checked" rows as 24 to 27; they are rows 23 to 26. Fixed in `research.md` v0.4.35 and `MASTER-SPEC-STATUS.md` v1.0.25 (file list version and size only).
- Honesty note: "confirmed" in the register means the name or value matched, not that the behavior was read in depth. The register is a wide, shallow check, and the four "not checked" rows show what is still only documentation-based.

---

## 2026-09-30 (pass 14, Forge): conditional data, tags and registry flags as coded

Read in the 26.3 tree: `ICondition`, `ConditionCodec`, `ForgeHooks` (conditional loading, parsed-data hook), patches for `SimpleJsonResourceReloadListener`, `LootPool`,
`TagFile`, `TagLoader`, and the registry builders in `GameData`. Probed three files on 13 branch heads.

### `docs/upstream/forge/source-map.json`
- **v0.1.2:** added the conditional format probe (`forge:condition` from 1.20.4, `forge:conditional` mention in `ForgeHooks` from 1.21.3).

### `docs/upstream/forge/research.md`
- **v0.4.31:** added section 28 (28.1 correction about the dead `CraftingHelper` line, 28.2 conditional data as coded, 28.3 tags, 28.4 registry flags, 28.5 triggers,
  28.6 implications, 28.7 what is missing). Two statements of section 27 annotated in place, and the open item about `forge:conditions` marked answered.
- **v0.4.32:** sources S104 and S105 added, header updated. File size 161856 to 169734 bytes.

### `docs/architecture/identity-and-collision.md`
- **v0.1.8:** follow-up to section 5.3: no Forge trigger registry in 26.3, which registries refuse overrides.

### `MASTER-SPEC-STATUS.md`
- **v1.0.23:** decision 20 row annotated, rows 22 and 24 re-worded (**no category change**, Forge stays 63.5%, overall about 4.7%), count history, file list,
  next-step text and section 5 updated.
- **Corrections of my own earlier statements:** (1) pass 13 (sections 26.4 and 27.2) reported a live call to `CraftingHelper` in `JsonCodecProvider`; it is in a comment. (2) Section 20.2
  (docs) said conditional loading applies only to recipes and advancements; in 26.3 it applies to all JSON reload listeners. (3) The docs format of conditional data described in
  section 20.2 is only the pre-1.20.4 format.

---

## 2026-09-30 (pass 13, Forge): docs checked against the code

Shiroz's answers of 2026-09-30 (pre-1.8 metadata layer agreed; "re-verify 26.x claims against the code, like the other places") started this pass.
Read: Forge 26.3 registry classes, `ForgeMod`, `ForgeRegistry`, `RegistryBuilder`, `GameData`, `DeferredRegister`, `NBTIngredient`, the `fmlloader` key
readers, and probed `ForgeRegistries.java` and `CraftingHelper.java` on 13 branch heads.

### `docs/upstream/forge/source-map.json`
- **v0.1.1:** added the registry probe (condition and ingredient registry present from 1.20.4, `CraftingHelper.java` gone from 1.21.1).

### `docs/upstream/forge/research.md`
- **v0.4.28:** added section 27 (27.1 eleven Forge registries, 27.2 conditions and ingredients, 27.3 duplicate handling in Forge 26.3, 27.4 other
  checks, 27.5 implications for VESSEL, 27.6 what is missing).
- **v0.4.29:** sources S101 to S103 added, header updated. File size 151647 to 161478 bytes.

### `docs/architecture/identity-and-collision.md`
- **v0.1.7:** scope and verification note in section 5.3, new section 5.4 (what Forge itself does on a duplicate key).

### Correction made right after (same pass)
- **`docs/upstream/forge/research.md` v0.4.30:** the `mods.toml` key comparison in section 27.4 had counted my own new sections 26 and 27 as "already known". Recomputed
  against sections 1 to 25 only: 25 of 28 keys were already written as code spans, unknown: showAsDataPack, accessTransformers, mods. Sentence replaced in place.
- **`MASTER-SPEC-STATUS.md` v1.0.22:** file list version and file size updated to match.

### `MASTER-SPEC-STATUS.md`
- **v1.0.21:** decisions 23 and 24 added, decision 8 and 20 rows annotated, rows 5, 6 and 8 of the 37-row table re-worded (**no category change**, so the
  Forge count stays 63.5% and overall stays about 4.7%), count history, file list, next-step text and section 5 updated.
- Correction to earlier text: section 26.7 of `research.md` and the v1.0.20 status note said decision 20 "rests on an API that may not exist under that
  name in 26.3". The check shows more exactly: the API existed up to branch 1.20.4 and was replaced by ordinary registries.

---

## 2026-09-30 (pass 12, Forge): first pass from source code

Read from the Forge repository: branch and tag lists (git ls-remote), a shallow clone of branch 1.7.10 and of branch 26.3, raw build
files of 13 branches. Supporting sources: PrismarineJS/minecraft-data (1.7 and 1.8 blocks), Minecraft Wiki pre-flattening pages
(excerpts). The date rolled over to 2026-09-30 during the session, so this entry and later ones use that date.

### `docs/upstream/forge/source-map.json` (new, v0.1.0)
- Machine-readable list of 44 branches with head commits, 131 tag names, EventBus version per branch head, and the two clones read.

### `docs/upstream/forge/research.md`
- **v0.4.26:** added section 26 (26.1 repository map, 26.2 metadata in 1.7.10 including the RotationHelper table, 26.3 the 26.3 tree
  and pack loading, 26.4 docs tested against code, 26.5 EventBus per branch, 26.6 `authorList`, 26.7 implications, 26.8 what is missing).
- **v0.4.27:** annotations in 20.1 and 25.4 (both open points now resolved by source), sources S95 to S100 added, header updated. File
  size 135134 to 151647 bytes.

### `docs/architecture/identity-and-collision.md`
- **v0.1.6:** new section 2.2 (Forge's own "maybe not a Forge mod" test, evidence only).

### `MASTER-SPEC-STATUS.md`
- **v1.0.20:** rows 29, 33, 36, 37 changed and rows 9 and 18 re-worded, Forge phase 63.5% (reported about 64%), overall about
  4.7%, count history, file list (new `source-map.json`), next-step text and section 5 updated.
- Honesty note: the jump from 55.4% to 63.5% is mostly two small single-fact rows (36 and 37) and the pack-loading row. The
  fixed list weights every row the same, so the number overstates how much of the hard work is done.
- Correction to an earlier claim: pass 11 said the block entity docs text "may lag Forge's code". Pass 12 confirms it does (section 26.4).

---

## 2026-09-29 (pass 11 follow-up): simple/complex line accepted, grey case decided

- **`docs/upstream/forge/research.md` v0.4.25:** new section 25.7 (accepted rule, grey case decided by Claude on delegation with the reasoning
  written out); 24.5 and 25.5 annotated in place.
- **`MASTER-SPEC-STATUS.md` v1.0.19:** decision 22 added, section 5 updated (also records that the source for the pre-1.8 metadata layer is
  Forge source code first).
- Note: the grey-case choice is Claude's proposal accepted by delegation, not a design Shiroz wrote out.

---

## 2026-09-29 (pass 11, Forge): block states, texture tinting, recipes introduction, renderer check

Read in full from the "latest" docs line: Block States, Texture Tinting, Recipes introduction, BlockEntityRenderer (checked
against the 1.19.x page read in pass 10). Nothing from earlier versions was deleted.

### `docs/upstream/forge/research.md`
- **v0.4.23:** added section 25 (25.1 block states, 25.2 texture tinting, 25.3 recipes introduction, 25.4 renderer check,
  25.5 implications for VESSEL, 25.6 what is missing). Section 24 caveat annotated in place: Forge's own "latest" renderer page
  has the same content as the 1.19.x page, so the "probably wrong" remark is only partly right.
- **v0.4.24:** section 19 rows (Blocks, Block Entities, Resources client, Resources server) and section 24.6 annotated,
  sources S91 to S94 added, header updated. File size 124730 to 133271 bytes.

### `MASTER-SPEC-STATUS.md`
- **v1.0.18:** rows 15 (blocks) and 18 (block entities) from PARTIAL to DONE **for the docs scope only**, status text of rows 21
  and 22 updated, Forge phase 55.4% (reported about 55%), overall about 4.3%. Count history, file list, next-step
  text and section 5 updated.
- Correction note: in pass 10 I told the reader that the block entity renderer names were probably wrong for 1.21.x. Reading
  Forge's own "latest" page shows the old shape, so that was too strong; the redesign is documented only by NeoForge and Fabric.

---

## 2026-09-29 (pass 10, Forge): client-side renderers and item model state

Read in full from the **1.19.x** docs line: BlockEntityRenderer, BlockEntityWithoutLevelRenderer, Item Overrides, Item
Properties. Excerpts only: community wiki BER pages, NeoForge and Fabric BER pages (which show a redesigned renderer).
Nothing from earlier versions was deleted.

### `docs/upstream/forge/research.md`
- **v0.4.21:** added section 24 (24.1 BER, 24.2 BEWLR, 24.3 item overrides, 24.4 item properties, 24.5 implications for VESSEL
  plus a proposal for the simple/complex line, 24.6 what is missing).
- **v0.4.22:** section 19 rows (Block Entities, Items, Rendering, Resources client) and section 23.7 annotated, sources S85 to
  S90 added, header updated. File size 117122 to 124730 bytes.

### `MASTER-SPEC-STATUS.md`
- **v1.0.17:** status text of rows 16, 18 and 21 updated, but **no row changed category**, so the Forge count stays at
  52.7% (reported about 53%) and overall stays about 4.3%. Count history, file list, next-step text and section 5 updated
  (one proposal waiting for Shiroz).

---

## 2026-09-29 (pass 9 follow-up): model conversion policy decided

- **`docs/upstream/forge/research.md` v0.4.20:** section 23.6 records option C (simple models to vanilla JSON, complex models to the Display/Model backend).
- **`MASTER-SPEC-STATUS.md` v1.0.16:** decision 21 added, section 5 updated, file list version updated.

---

## 2026-09-29 (pass 9, Forge): block entities and model loaders

Read in full: block entities page and model loaders introduction (docs line labeled "latest", 1.20.x-era code names).
Excerpts only: model extension keys, baked model, Fabric 26.2 and NeoForge comparison pages, an older community wiki.
Nothing from earlier versions was deleted.

### `docs/upstream/forge/research.md`
- **v0.4.18:** added section 23 (23.1 block entities, 23.2 model loaders, 23.3 extension keys, 23.4 baked model, 23.5
  comparison excerpts, 23.6 implications for VESSEL with evidence and inference apart, 23.7 what is missing).
- **v0.4.19:** section 19 rows (Block Entities, Rendering) and section 20.7 annotated, sources S77 to S84 added, header
  updated. File size 106448 to 116966 bytes.

### `MASTER-SPEC-STATUS.md`
- **v1.0.15:** rows 18 (block entities) and 21 (rendering hooks) from not started to PARTIAL, Forge phase 52.7%
  (reported about 53%), overall about 4.3%, count history, file list and next-step text updated, new open
  question added to section 5.

---

## 2026-09-29 (pass 8 follow-up): setup-time ids count as registry keys

- **`docs/architecture/identity-and-collision.md` v0.1.5:** new section 5.3 (trigger ids and ingredient or condition
  serializer ids count as registry keys), new requirement about covering setup-time calls, section 9 extended.
- **`docs/upstream/forge/research.md` v0.4.17:** the question in 22.4 marked answered.
- **`MASTER-SPEC-STATUS.md` v1.0.14:** decision 20 added, section 5 updated, file list versions updated.

---

## 2026-09-29 (pass 8, Forge): advancements and items

Read in full: advancements page (docs line labeled "latest", code names look JSON-era), items introduction (1.21.x
docs). Excerpts only: Architectury 1.20 migration notes, 1.18.x tags page, NeoForge advancements pages. Nothing from
earlier versions was deleted.

### `docs/upstream/forge/research.md`
- **v0.4.15:** added section 22 (22.1 advancements, 22.2 items, 22.3 excerpt-only facts, 22.4 implications for VESSEL
  with one new question, 22.5 what is missing).
- **v0.4.16:** section 19 rows (Items, Resources server) and section 20.7 annotated, sources S72 to S76 added, header
  updated. File size 98333 to 106391 bytes.

### `MASTER-SPEC-STATUS.md`
- **v1.0.13:** row 16 (items) from not started to PARTIAL, Forge phase 50.0% (reported about 50%), overall about
  4.1%, count history, file list and next-step text updated, new open question added to section 5.
  Advancements were read too but have no row in the fixed 37-row list, so they do not change the percentage.

---

## 2026-09-29 (pass 7, Forge): loot tables

Read in full: the loot tables page of the **1.20.x** docs (the 1.21.x page could not be fetched, so every API
name is unverified for 1.21.x and 26.x). Excerpts only: datagen loot table page (latest), 1.12.x loot tables,
NeoForge 1.20.6 loot tables. Nothing from earlier versions was deleted.

### `docs/upstream/forge/research.md`
- **v0.4.13:** added section 21 (how a table is found and run, Forge additions, excerpt-only facts,
  implications for VESSEL with evidence and inference kept apart, what is still missing).
- **v0.4.14:** section 19 row and section 20.7 annotated, sources S68, S70, S71 added (S69 is not used;
  the datagen loot excerpt is in S67), header updated. File size 91927 to 98333 bytes.

### `MASTER-SPEC-STATUS.md`
- **v1.0.12:** row 23 (loot tables) from not started to PARTIAL, Forge phase 48.6% (reported about
  49%), overall about 4.1%, count history and file list updated.
- Process note: the first attempt to write this status version failed on a formatting error in my script
  (a percent sign inside a format string). research.md v0.4.14 had already been written; the status and
  this entry were written on the second attempt.

---

## 2026-09-29 (pass 6 follow-up): condition identity and registry availability decided

- **`docs/architecture/startup-build-stage.md` v0.1.2:** new section 2.1 (registries complete before the
  stage, VESSEL merges tags, mod code keeps running for loot modifiers and code-only recipes), one new
  open item in section 10, and a flagged wording conflict with the older "no mod is enabled" sentence.
- **`docs/architecture/identity-and-collision.md` v0.1.4:** new section 5.2 (Forge-source conditions
  match only `forge` modules).
- **`docs/upstream/forge/research.md` v0.4.12:** section 20.2 questions A and B marked answered.
- **`MASTER-SPEC-STATUS.md` v1.0.11:** decisions 18 and 19 added, file list versions and section 5 updated.
- Note: decision 19 was delegated ("pick the most suitable option"), so it is Claude's proposal accepted
  by Shiroz's delegation, not a design Shiroz wrote out.

---

## 2026-09-29 (pass 6, Forge): data packs, resource packs, conditions, loot modifiers, tags, recipes

Read in full (official 1.21.x docs): resources/server intro, conditionally-loaded data, global loot
modifiers, tags, ingredients, resources/client intro. Seen only as search excerpts: custom recipes,
non-datapack recipes, model loader transform, datagen pages. No 26.x docs exist, so all of it is
unverified for 26.x. Nothing from earlier versions was deleted.

### `docs/upstream/forge/research.md`
- **v0.4.9:** added section 20 (20.1 pack locations, 20.2 conditional data, 20.3 global loot modifiers,
  20.4 tags, 20.5 recipes and ingredients, 20.6 excerpt-only fragments, 20.7 what is still missing),
  with implications for VESSEL kept apart from evidence, and two open questions.
- **v0.4.10:** section 19 table rows for "Resources, client", "Resources, server" and "Rendering"
  updated in place with what was read.
- **v0.4.11:** sources S58 to S67 added, header research date and status updated. File size
  78200 bytes before pass 6, 91449 bytes now.

### `MASTER-SPEC-STATUS.md`
- **v1.0.9:** rows 22 (recipes), 24 (tags) and 33 (resources and packs) of the 37-row table changed
  (PARTIAL, DONE, PARTIAL). Forge phase 47.3% (reported about 47%), overall about 4.0%. Added
  section 4.3 (count history). Also corrected the file size: the earlier "about 77 KB" had been an
  estimate that was never measured.
- **v1.0.10:** file list versions updated, two new open questions added to section 5, next-step text
  rewritten to the pages still unread.
- Process note: writing these two status versions failed twice on my own script checks (first it also
  matched the decisions table when counting rows, then it looked for an outdated file-list line). Nothing
  was half-written each time. A zip built in between held research.md v0.4.11 with status v1.0.8 and was
  replaced by the final zip.

---

## 2026-09-29 (pass 5 follow-up 3): duplicate scan method decided

- **`docs/architecture/identity-and-collision.md` v0.1.3:** section 5.1 now specifies the two-step scan
  (static scan, then isolated dry run for modules that build names in code) and a required list of
  console messages with a draft example. Open details moved to section 9.
- **`docs/upstream/forge/research.md` v0.4.8:** section 15 note updated to point to the decision.
- **`MASTER-SPEC-STATUS.md` v1.0.8:** decision 17 and section 5 updated. Also fixed the file list versions.

---

## 2026-09-29 (pass 5 follow-up 2): version-source rule decided

- **`docs/upstream/forge/research.md` v0.4.7:** section 18.2 changed from "proposal" to a decided rule
  (option A: read all sources, take the highest build, yellow warning when they differ).
- **`MASTER-SPEC-STATUS.md` v1.0.7:** section 5 updated to match. The runtime-registry-key scan design
  remains open.

---

## 2026-09-29 (pass 5 follow-up): answers to the four questions

- **`docs/upstream/forge/research.md` v0.4.6:** section 18.3 now records that a build with Latest but no
  Recommended (not stable) only produces a yellow warning, never a block. Old wording ("not decided")
  replaced because it was no longer true.
- **`MASTER-SPEC-STATUS.md` v1.0.6:** decision 16 and section 5 updated to match, plus a note that the
  Forge version-source disagreement rule and the runtime-registry-key scan design are still open (both
  answered "not clear").

---

## 2026-09-29 (pass 5, Forge): research, status and identity updates

Every part below is one version step of one file, in the order it was made. Nothing from
earlier versions was deleted. Superseded text is marked in place.

### `docs/upstream/forge/version-index.json`
- **v0.1.1:** era labels aligned with `research.md` section 2. The file used `E0` (4 versions:
  1.1, 1.2.3, 1.2.4, 1.2.5) and `E1` (19 versions), but `research.md` had already replaced them with
  `E1a` and `E1b`. Relabeled `E0` to `E1a` and `E1` to `E1b`.
- **v0.1.2:** added `promotions` (latest and recommended) for every version, from Forge's official
  `promotions_slim.json` (source S50, fetched 2026-09-29). 77 of 78 versions have a latest build,
  40 have a recommended build, `1.7.10_pre4` has no entry. Older `builds_seen` values were kept.
- **v0.1.3:** 26.3 got builds 66.0.4, 66.0.5, 66.0.8 (index page excerpt) and the 66.0.0 to 66.0.5
  changelog lines, plus `index_page_latest_seen: 66.0.8`. The promotions file still said 66.0.6, so
  the two official sources disagree (see `research.md` 18.2).

### `docs/upstream/forge/research.md`
- **v0.4.1:** added section 1.2a: the promotions file, 31 of 78 known builds became 77 of 78, and a
  correction: 1.21.11 was recorded as 61.0.3 from an old cached page, the promotions file gives
  61.2.1 latest and 61.2.0 recommended. Nine other versions also had older builds recorded than now.
- **v0.4.2:** added section 13a: the 26.3 changelog (66.0.0 to 66.0.5) and what it implies (Forge
  patches Minecraft classes, so key on the Forge build, not only the Minecraft version).
- **v0.4.3:** added sections 18 (version registry evidence: promotions format, lists of versions
  with and without a recommended build, the 26.3 disagreement, the stability decision, mod
  versioning convention with Maven `ComparableVersion`, update checker statuses) and 19 (the full
  official 1.21.x docs page list as a reading checklist, and confirmation that no 26.x docs exist).
- **v0.4.4:** section 15 items 1, 2, 8 and 9 annotated as resolved or partly resolved (old text kept),
  Shiroz's two answers recorded, sources S50 to S57 added, header status updated.
- **v0.4.5:** fixed an error made in v0.4.3: section 18.1 said latest equals recommended for 16
  versions, the correct count is 12 (checked by script). Both version lists in 18.1 were also
  verified by script (40 with recommended, 37 latest only).

### `docs/architecture/identity-and-collision.md`
- **v0.1.2:** section 5.1 records Shiroz's decision that the whole module set is scanned for duplicate
  registry keys before anything loads and a duplicate means no startup. The earlier two-pass
  (static plus runtime) behavior is marked superseded but kept. New open item in section 9: how the
  scan finds keys that only exist at runtime. This also removes a conflict with
  `startup-build-stage.md` section 2, which already said a build is never discarded halfway
  because of a collision.

### `MASTER-SPEC-STATUS.md`
- **v1.0.1:** added decisions 16 (Latest-without-Recommended is not stable) and 17 (up-front scan for
  duplicate registry keys, no startup on a duplicate). Decision 14 got Shiroz's own example of the
  "update every loader on every release" rule.
- **v1.0.2:** Forge phase changed from about 28% to about 42%, overall from about 3.2% to about
  3.8%, research file size updated.
- **v1.0.3:** section 4 rebuilt as a fixed 37-row list with a stated scoring method. The old table
  is kept as 4.2. Correction recorded: the old text said "13 of 19 items DONE" and "about 28%",
  but 13 of 19 is about 68%, so the 28% was not a count-based number. Blocks were also regraded
  from DONE to PARTIAL (the old note said "first slice only").
- **v1.0.4:** section 5 got the answers to open items 2 and 3 and four new open points.
- **v1.0.5:** file list versions updated, next step rewritten (Forge first, then other loaders in
  phase order, with the exact pages to read next).

---

## 2026-09-29: `MASTER-SPEC-STATUS.md` v1.0.0 (new)

**Change type:** New top-level status document, requested by Shiroz as a full package
(spec + zip) to close out this conversation before continuing in a fresh one.

**What it does:** a single map of the whole project against the original v3's own
23-phase plan, split into three honest categories (DONE with evidence, PARTIAL with an
exact list of what's missing, NOT STARTED). Lists all 15 architecture decisions made so
far that supersede specific v3 sections, with a pointer to the file backing each one.
Gives an itemized Forge breakdown (13 of ~19 tracked items done, ~28%) explaining where
the phase-7 percentage comes from, an overall estimate of roughly 3-4% across all 23
phases, and a short list of decisions made in chat that are real but do not have a
dedicated file yet (Geyser bundling, network sync, compatibility/release policy).

**Note on the original v3 document:** it is not included as a separate file in the
delivered package, since it was pasted as chat text at the start of this project and
was never saved as a file in this environment's outputs. MASTER-SPEC-STATUS.md section
2 records every point where it has been superseded; everything else in it still stands.

**Package delivered:** a zip containing this file, LOG.md, and every docs/ file listed
in MASTER-SPEC-STATUS.md section 7.

---

## 2026-09-28 (6): `docs/upstream/forge/research.md` v0.4.0

**Change type:** Added ModLauncher's transformation-service mechanism (E3+) and a first pass on
the gameplay API (block registration). One numbering fix applied mid-edit (a section heading was
briefly lost and restored; final structure verified section by section).

**New content:**
- Full `ITransformationService` / `ITransformer` / `ServiceLoader` mechanism for ModLauncher
  (E3 onward), read in full from a dedicated technical walkthrough: discovery via
  `META-INF/services/...ITransformationService`, the ASM-tree-based transformer model (versus
  LaunchWrapper's raw-bytecode model), vote-based target selection, and how Forge itself can host
  a ModLauncher service jar directly in `mods/` from Forge 1.13.2-25.0.216 onward.
- Five real Forge startup logs spanning 2020 to 2024 compared side by side to show the built-in
  launch-plugin set is not fixed: `slf4jfixer` and `capability_token_subclass` are newer additions
  not present in the earliest logs read.
- A first slice of the gameplay API: block registration (`DeferredRegister`/`RegistryObject`)
  confirmed identical across three official Forge documentation snapshots (1.18.x, 1.20.x,
  1.21.x). Found, by direct comparison with NeoForge's own docs, that Forge and NeoForge have
  diverged again on this specific API (`RegistryObject` vs `DeferredBlock`), reinforcing that they
  need fully separate adapters.

**Still open for Forge:** no official docs exist for 26.x at all, 47/78 versions still lack a
build number, E1a/E2 LaunchWrapper internals beyond coremods, `defaultconfigs` copy behavior, and
nearly the entire rest of the gameplay API (items, entities, block entities, commands, scheduler,
rendering, recipes, loot tables, tags, worldgen, datagen).

---

## 2026-09-28 (5): `docs/upstream/forge/research.md` v0.3.0

**Change type:** Deepened the earliest Forge eras and the pre-ModLauncher coremod mechanism.
Two corrections applied to v0.2.0's own content.

**Corrections:**
- v0.2.0 speculated Forge might have pre-1.1 (Beta-era) builds. Independent evidence now states
  plainly Forge's first release was for Minecraft 1.1 in 2011. There is no older Forge. The "E0"
  era is removed; 1.1-1.2.5 is now era E1a (a VESSEL-proposed split, not an official era name).
- v0.2.0's source material had conflated two different things under "mcmod.info": the real,
  official mcmod.info (always a bare JSON array, one object per mod) versus an unrelated,
  unofficial "mod list" JSON shape (an object with modListVersion/modList) used by some
  launchers for displaying installed mods. Only the bare-array form is Forge's actual metadata
  file; this is now stated explicitly so the two are never confused going forward.

**New content:**
- Full `mcmod.info` field table, confirmed identically across 1.7.10, 1.10.2, 1.12.x and 1.13.x
  official docs plus real-world forum examples. Most important finding: `requiredMods` crashes
  the game if missing but does NOT affect load order by itself; `dependencies`/`dependants`
  control order only and silently do nothing if the listed mod is absent; getting both mandatory
  AND ordered requires listing a modid in both fields at once. A 2015 Forge proposal to replace
  this format with an npm-style schema was never implemented.
- Full pre-ModLauncher coremod mechanism (`IFMLLoadingPlugin`, `IClassTransformer`,
  `FMLCorePlugin` manifest attribute, `FMLCorePluginContainsFMLMod`), confirmed across four
  independent Javadoc mirrors (1.7.10, 1.9.4, 1.10.2, 1.11.2) plus two worked tutorials. Recorded
  the self-exclusion hazard a coremod's transformer package has to respect.

**Consequences for VESSEL noted in the file:** the compatibility layer must reproduce the exact
crash-vs-silent-noop split of `requiredMods` vs `dependencies`, and the bytecode analyzer needs a
distinct E1b/E2 coremod path separate from the later ModLauncher transformation services.

**Still open for Forge (file section 15, trimmed of resolved items):** no official docs exist for
26.x, 47/78 versions still lack a build number, `ITweaker`/LaunchWrapper internals beyond
coremods, ModLauncher services in E3+, `defaultconfigs` copy behavior, and the entire gameplay
API surface remain unresearched.

---

## 2026-09-28 (4): `docs/upstream/forge/research.md` v0.2.0, `docs/upstream/forge/version-index.json` v0.1.0 (new), `docs/architecture/identity-and-collision.md` v0.1.1

**Change type:** Forge research deepened significantly; one correction applied to an earlier document.

**Forge research v0.2.0 additions over v0.1.0:**
- Full official version index read in full (not excerpt): 78 Minecraft versions from 1.1 to
  26.3. Stored as structured data in version-index.json, grouped into 7 proposed eras
  (E0 ModLoader-era Forge, E1 FML/cpw, E2 net.minecraftforge.fml, E3 ModLauncher/mods.toml,
  E4a 1.17-1.20.1, E4b 1.20.2-1.21.11, E5 the unobfuscated 26.x line).
- Build numbers found for 31 of the 78 versions, cited per version.
- Mod metadata for the old `@Mod` + `mcmod.info` system (E1/E2), including the dependency
  string grammar, alongside the already-known `mods.toml` (E3+).
- Registries: read in full from the official docs. Correction: Forge silently overrides a
  duplicate registry key instead of stopping. Also covers per-registry uniqueness, dynamic
  (datapack) registries, and MissingMappingsEvent for save migration.
- Sides (Dist, DistExecutor, @OnlyIn, @SidedProxy), events (two buses, EventBus 6 vs the
  unreleased-version EventBus 7 rewrite), networking by era (SimpleNetworkWrapper to
  SimpleChannel to ChannelBuilder), persistent data by era (capabilities, SavedData).
- Access transformer file format read in full: syntax, +f/-f, SRG names pre-26.1 vs plain
  Mojang names from 26.1 onward.
- Direct primary evidence: the official 26.2 changelog, read in full, listing 13 real
  engineering changes build by build.
- A third-party ecosystem project (Architectury API) confirmed to have actually dropped
  Forge support for 26.x, refining (not reversing) the earlier rejection of that claim.

**Correction applied to `identity-and-collision.md` (now v0.1.1):**
- Section 5.1 rewritten: the duplicate-registry-key stop is now explicitly stated to be a
  VESSEL design choice that is stricter than Forge's own (silently-overriding) behavior.
- Added that the check must be per-registry, not global.
- Added that detection is a static pass plus a necessary runtime pass, since dynamically
  built registry keys (through DeferredRegister) are not always visible statically, and
  that a runtime-caught duplicate discards any in-progress build for that version like an
  interrupted build.

**Still open for Forge (see file section 15):** no official docs exist for 26.x (all API
detail confirmed only up to 1.21.x), 47 of 78 versions still have no build number, E0/E1
loader internals, ModLauncher/coremod services, resources/packs, `defaultconfigs` copy
behavior, and the entire gameplay API surface (blocks, items, entities, commands,
scheduler, rendering, worldgen) are not yet researched.

---

## 2026-09-28 (3): `docs/upstream/forge/research.md` v0.1.0 and `docs/upstream/fabric/research.md` v0.1.0 (new)

**Change type:** Two new research files. This is the first research output in the set.

**Forge (status PARTIAL):**
- Versions: Forge 62.0.9 (26.1), 63.0.2 (26.1.1), 64.1.3 with recommended 64.1.0
  (26.1.2), 65.1.3 with recommended 65.1.0 (26.2), 66.0.6 (26.3, no recommended build).
  The 26.x rows come from search excerpts of the official download pages, not a full read.
- A third-party claim that Forge dropped Minecraft 26.x was checked and rejected.
- Java requirement per Minecraft version from the official README.
- `mods.toml` schema, entrypoints (`javafml`, `lowcodefml`), lifecycle events with their
  threading, and the CLIENT / COMMON / SERVER config types, all from the official docs.
- Findings that affect the master spec: `lowcodefml` JARs carry packs only, one JAR can
  hold several mods, Forge and NeoForge need different version parsers, `SERVER` configs
  are read too late for the Startup Build Stage to depend on, and registry key collisions
  cannot be detected purely statically.
- Main gap: the official docs have no Minecraft 26.x pages, so every API name is only
  confirmed for 1.19 to 1.21.

**Fabric and Minecraft core (status PARTIAL, versions only):**
- Minecraft 26.3 is a full release since 2026-09-15, 26.4 is in development since
  2026-09-22.
- Fabric Loader 0.19.5 (2026-08-28) recommended for 26.3.
- Pack format facts moved out of chat into a file, including the `min_format` /
  `max_format` change from 1.21.9.
- Recorded optimal cache split: pack outputs keyed by pack format, logic outputs keyed by
  Minecraft version. Not yet in the master spec.
- Fabric API inventory, `fabric.mod.json`, lifecycle and Mixin details are not done.

---

## 2026-09-28 (2): `docs/architecture/identity-and-collision.md` v0.1.0 (new)

**Change type:** New file.

**What it covers:**
- A module's identity is now the pair (source loader, module id), never the id alone.
  Source loaders: `fabric`, `quilt`, `forge`, `neoforge`, `plugins`.
- Fabric versus Quilt is decided by the API the bytecode calls: any Quilt-specific API
  call means `quilt` (even when Fabric API is also called), only Fabric API means
  `fabric`, and metadata is the fallback when neither is called.
- Forge versus NeoForge by metadata file and API package. Plugin family shares one
  `plugins` loader, with the target platform kept as a separate attribute.
- Module data is split per loader (`data/fabric/`, `data/quilt/`, `data/forge/`,
  `data/neoforge/`, `data/plugins/`), replacing `data/mods/`. The same split applies to
  cache, generated, logs, temp, libraries, backups and crash reports. Render bundles
  and the build cache key follow the same split.
- Same id with a different loader: both run, yellow warning, documented as a usage
  caveat and not a bug. Replaces the earlier "mod wins, plugin is skipped" rule.
- Same registry key: the server stops with a red error naming both modules and both
  JAR paths. Detected at startup step 7, before the Startup Build Stage.
- Same loader and same id: also a hard stop.
- Console colors: white normal, yellow warning, red error.

**Accepted consequence:** data does not follow a mod when it moves between loaders
(for example Forge to NeoForge). Migration is manual or done through the migration tools.

**Still open:** JSON shape of `reports/identity-collisions.json`, where collision
settings live in `vessel-config/`, and the rewrite of the affected spec v3 sections.

---

## 2026-09-28 (2): `docs/architecture/startup-build-stage.md` v0.1.1

**Change type:** Full rewrite in English, plus content updates.

**What changed:**
- Whole document translated to English (project rule: everything is English, only chat
  with Shiroz is Vietnamese).
- Startup step 7 now includes module identity and collision checks, so the module set is
  fixed before the build starts.
- Config folder listing gained `identity.yml`, and per-module overrides are now split by
  loader (`overrides/<loader>/<id>/`).
- Logging section now states the console colors.
- Added a pointer to `identity-and-collision.md`.

---

## 2026-09-28 (1): `docs/architecture/startup-build-stage.md` v0.1.0 (new)

**Change type:** New file (later rewritten as v0.1.1, see above).

**What it covered:**
- The Startup Build Stage: the render bundle (resource pack, data pack, texture pack,
  Bedrock add-on) is built at server startup, before the world loads, the port opens or
  any mod or plugin is enabled. It replaced an earlier lazy build-on-first-join design.
- VESSEL versions itself independently of Minecraft, like Fabric Loader.
- First-run flow: `--vessel-version` flag, sentinel file `vessel-first-run-ack.txt`,
  a mandatory non-configurable 45-second warning on the very first run, then exit.
- Interrupted build policy: a build cut off halfway is discarded and redone, finished
  builds are kept.
- Narrower role for the per-player loading screen.
- The `vessel-config/` folder, at the server root.

---

## Research status

| Platform | Status | File |
|---|---|---|
| Minecraft core and pack formats | PARTIAL (versions and pack format history done) | docs/upstream/fabric/research.md |
| Fabric | PARTIAL (versions only) | docs/upstream/fabric/research.md |
| Forge | PARTIAL (metadata, lifecycle, config, version registry mechanics done; most of the gameplay API still open) | docs/upstream/forge/research.md |
| NeoForge | not started (config read timing seen only as a comparison) | |
| Quilt | not started | |
| Bukkit, Spigot, Paper, Folia, Canvas, Purpur | not started | |
| Bedrock and Geyser | not started | |

Next in phase order: NeoForge, then Quilt, then the Bukkit family, then Bedrock and Geyser.
Fabric still needs a full API pass, and Forge still needs the items in section 9 of its file.
