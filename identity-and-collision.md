# VESSEL: Module Identity, Loader Classification & Collision Rules

**Document version:** 0.1.12
**Status:** Draft, approved by Shiroz
**Supersedes:** spec v3 sections #408-#409, #487-#491 (identity and duplicates), and every
use of `data/mods/<id>/` in the original layout (see section 3)
**v0.1.2 change (2026-09-29):** section 5.1 now follows Shiroz's decision that VESSEL scans the whole
module set for duplicate registry keys BEFORE anything loads, and refuses to start if it finds one.
The earlier "runtime pass may catch it after a build has started" behavior is marked superseded
(kept in place for history). This also removes the wording conflict with `startup-build-stage.md`
section 2, which already said a build is never thrown away halfway because of a collision.
**v0.1.1 change:** section 5.1 corrected against Forge evidence (Forge itself silently
overrides duplicate registry keys; VESSEL's hard stop is a deliberate, stricter choice,
checked per registry and partly only detectable at runtime). See
`docs/upstream/forge/research.md` section 6.

**v0.1.11 change (2026-10-05):** new section 5.5: entity selector tokens and argument-type classes also count as registry keys (decision 31, answered "yes" by Shiroz on 2026-10-05); one new open item in section 9. Source: `docs/upstream/forge/research.md` section 36.2.

**v0.1.12 change (2026-10-06):** section 5.4 gets one more verified behavior of an old era: the 1.7.10 entity extended properties rename a repeated identifier instead of failing. Source: `docs/upstream/forge/research.md` section 38.3.

---

## 1. What a module's identity is made of

In VESSEL a module is identified by a pair, never by an ID alone:

```
(source loader, module id)
```

Examples: `(fabric, foo)`, `(forge, foo)`, `(plugins, foo)`.

The source loader is one of: `fabric`, `quilt`, `forge`, `neoforge`, `plugins`.
The module id is the id from the module's own metadata, never the JAR file name
(spec v3 #489 still applies: never assume filename equals id).

Two modules with the same id but a different source loader are two different
modules with two different identities. They never share a data folder, a cache
entry or a render bundle.

---

## 2. Classifying the source loader

### 2.1 Fabric versus Quilt

Quilt Loader can load Fabric mods, but Fabric Loader cannot understand Quilt mods.
For that reason the classification follows the API the mod's bytecode really calls,
not just the metadata file. Rules are applied in this order:

| # | What the bytecode does | Classified as |
|---|---|---|
| 1 | Calls any Quilt-specific API (`org.quiltmc.*`), with or without Fabric API calls | `quilt` |
| 2 | Calls only Fabric API (`net.fabricmc.*`) | `fabric` |
| 3 | Calls neither (talks to Minecraft only through Mixin) | Fall back to metadata: `quilt.mod.json` means `quilt`, `fabric.mod.json` means `fabric` |
| 4 | Calls neither and has both metadata files | `quilt` (a Quilt loader can read both) |
| 5 | Calls neither and has no known metadata file | Reported as `UNKNOWN_LOADER`, see section 6 |

Rule 1 is the important one: a mod that calls both Quilt and Fabric API is a Quilt
mod, because only Quilt can run something that understands both.

### 2.2 Forge versus NeoForge

Classified by the metadata file and the API package it calls:

- `META-INF/neoforge.mods.toml` or calls into `net.neoforged.*` means `neoforge`.
- `META-INF/mods.toml` or calls into `net.minecraftforge.*` means `forge`.
- If a JAR has both, it is classified once per loader and reported as a multi-loader
  JAR (two module identities from one file). It is never silently merged.

### 2.3 Plugins

Anything with `plugin.yml` or `paper-plugin.yml` is `plugins`. Bukkit, Spigot, Paper,
Folia, Canvas and Purpur plugins all share the `plugins` source loader, because they
are one plugin family. The target platform (Paper, Folia, and so on) is recorded as a
separate attribute, not as part of the identity.

### 2.4 Classification is done statically

The classifier reads bytecode and metadata only. It never runs constructors, static
initializers or `onEnable` (spec v3 #180 still applies).

---

### 2.2 Forge's own "maybe not a Forge mod" test (evidence, added 2026-09-30, v0.1.6)

Forge 26.3's `ModFileInfo` (`docs/upstream/forge/research.md` section 26.3) treats a file as "might not be a Forge mod" when
its loader is `javafml`, its loader version range has restrictions, and its loader version text equals the constant
`MAYBE_NOT_JAVAFML_VER` (source comment: `[2,)`). `clientSideOnly = true` makes it definitely a Forge mod. VESSEL's
classification (section 2.1, decision 9) can use this as one more signal when a file declares `javafml` but may belong to another
loader. This is evidence only. No rule is decided, and other Forge eras may differ.

## 3. Data layout (replaces `data/mods/`)

The old shared `data/mods/<id>/` folder is removed. Module data is split per source loader:

```
data/
├── fabric/<id>/
├── quilt/<id>/
├── forge/<id>/
├── neoforge/<id>/
├── plugins/<id>/
├── shared/<namespace>/
├── worlds/<world-id>/
│   ├── fabric/<id>/
│   ├── quilt/<id>/
│   ├── forge/<id>/
│   ├── neoforge/<id>/
│   ├── plugins/<id>/
│   └── shared/
├── players/<uuid>/
└── server/
```

The same split applies to every module-scoped root that used to have a `mods/` branch:

```
cache/<loader>/<id>/
generated/<loader>/<id>/
logs/<loader>/<id>/
temp/<loader>/<id>/
libraries/<loader>/<id>/
backups/<loader>/<id>/
reports/crashes/<loader>/<id>/
```

where `<loader>` is one of `fabric`, `quilt`, `forge`, `neoforge`, `plugins`.

### 3.1 Consequence of splitting by loader

Data does not follow a mod when it moves between loaders. If an admin replaces a Forge
mod with its NeoForge build, the old data stays in `data/forge/<id>/` and the new
module starts with an empty `data/neoforge/<id>/`. This is accepted by design.
The migration tools from spec v3 #60-#63 can copy data across by hand.

---

## 4. Same id, different loader: both run

If two modules share an id but not a loader (for example the plugin `worldedit` and
the Fabric mod `worldedit`), the default behavior is to load and run both.

- Each one gets its own data folder, cache entry, log folder and render bundle.
- A yellow warning is printed to the console once per collision at startup, and the
  same information is written to `reports/identity-collisions.json`.
- This is documented as a usage caveat, not a bug.
- Load order between them is: mods first, plugins second. The order only decides
  who loads first. It does not disable anything.

Example console output:

```
[VESSEL] Heads up: a plugin and a Fabric mod are both called "worldedit".
[VESSEL] Both will run. Their data is kept apart (data/plugins/worldedit and
[VESSEL] data/fabric/worldedit), but if they touch the same in-game content,
[VESSEL] you may see odd behavior. Remove one of them if that is not what you want.
```

(Printed in yellow. See section 7 for the color rules.)

Two modules with the same id **and** the same loader (for example two JAR versions of
the same Fabric mod) are not the same case. That one is a hard duplicate: see section 5.2.

---

## 5. Hard collisions

### 5.1 Same registry key: the server stops

If two modules register the same key **in the same registry** (for example both
register `foo:machine` in the block registry), the server stops. The check is
per registry: a block and an item can share the name `foo:thing` without conflict,
because they live in different registries. This matches how Forge's own registries
work (see `docs/upstream/forge/research.md` section 6): Forge does not stop the game
on a duplicate key, it silently lets the second registration override the first.
VESSEL is deliberately stricter than that, because a silent override is a common
source of hard-to-diagnose bugs.

**Decision of 2026-09-29 (Shiroz), which supersedes the two-pass behavior described further down
in this section:** VESSEL scans the whole module set for duplicate registry keys before anything
loads. If any duplicate exists, VESSEL prints the error and does not start at all: no partial
loading, no world, no port, no render bundle build. Only when the scan finds no duplicate does
loading begin, followed by the creation of render bundles for players who join. There is no
runtime "second chance" pass that can interrupt a build that already started.

**Decided by Shiroz on 2026-09-29 (option C): the up-front scan has two steps.**

1. **Static scan.** VESSEL reads every module's JAR without running any mod code and collects every
   registry key it can read directly (metadata and code that writes the name out in full).
2. **Isolated dry run.** For each module that shows signs of building registry names in code (for
   example `DeferredRegister` names that are assembled or computed), VESSEL runs only that module's
   registration step in an isolated environment. No world is loaded, no port is opened, no
   render bundle is built. VESSEL records the keys produced and adds them to the same duplicate check.

If either step finds a duplicate, VESSEL prints the error and does not start. Both steps finish
before the Startup Build Stage.

**Console reporting is required, not optional.** The scan must tell the admin clearly what it is doing
(white normal, yellow warning, red error, same colors as section 7). Required lines:
- start and end of the static scan, with counts (modules scanned, keys found);
- the list of modules sent to the dry run and, for each, the reason it was flagged;
- progress of the dry run, one line per module (started, finished, keys found, time taken);
- any module whose dry run failed or could not be isolated (yellow, naming the module and the reason);
- the final result: either "no duplicate registry keys found" or the red duplicate error from
  the example below.

Example (wording is a draft, the meaning is what is fixed):

```
[VESSEL] Checking registry names before loading anything...
[VESSEL] Step 1 of 2: reading 42 modules without running them. Found 3,118 registry names.
[VESSEL] Step 2 of 2: 3 modules build names in code, so I will test-run just their registration.
[VESSEL]   forge "foo" (mods/foo-1.4.0.jar): names are assembled in code
[VESSEL]   forge "foo": test run finished in 1.2s, 87 more names found.
[VESSEL]   fabric "bar": could not be test-run safely (it needs the game world). Skipping it.   <- yellow
[VESSEL] No duplicate registry names found. Loading can begin.
```

Open technical points (not designed yet): what exactly counts as a "sign" that a module builds names
in code, how the isolated environment is built per loader (Forge, Fabric, NeoForge, Quilt, plugins), and
what VESSEL does with a module whose dry run cannot be made safe (the example above only warns and skips
it, which reopens the runtime-only case for that module; not decided). Listed in section 9.

- [Two-pass description, partly SUPERSEDED on 2026-09-29, kept for history] Checked in two passes, because not every registry key can be seen ahead of time:
  - A best-effort **static** pass at the dependency graph stage (startup step 7),
    before the Startup Build Stage begins, catches whatever keys can be read directly
    from metadata and straightforward code.
  - A **runtime** check when each loader actually fills its registries (for Forge,
    when `RegisterEvent` fires) catches keys built dynamically, for example through
    `DeferredRegister` with a computed or assembled name, which the static pass may
    miss entirely. **[SUPERSEDED 2026-09-29: the runtime pass no longer exists as a separate stage; the up-front scan must find these keys before loading.]**
- Whichever pass finds the collision first stops the server: the console prints a red
  error naming the registry, the key, both modules and both JAR paths, and tells the
  admin to remove one of them. The process exits with a non-zero exit code.
- If the runtime pass is the one that catches it, this happens later than step 7,
  possibly after the Startup Build Stage has already started or finished for that
  version combination. That partially-done build is discarded, following the same
  rule as an interrupted build (see `startup-build-stage.md` section 6). **[SUPERSEDED 2026-09-29: with the up-front scan, a collision can no longer be found after a build has started.]**
- There is no feature isolation, no automatic renaming and no alias workaround for
  this case. The admin removes one module and starts again.

Example console output:

```
[VESSEL] Cannot start: two modules registered the same block, "foo:machine".
[VESSEL]   First:  fabric "foo"   (mods/foo-fabric-1.2.0.jar)
[VESSEL]   Second: plugins "foo"  (plugins/Foo-3.0.jar)
[VESSEL] Only one of them can own that name. Remove one of the two jars and start again.
```

(Printed in red.)

### 5.2 Same loader and same id: the server stops

Two active modules with the same source loader and the same id (two versions of one
mod, or a renamed copy) are also a hard duplicate. The server stops with a red error
listing both JAR paths, and does not overwrite or pick one silently (spec v3 #490-#491).

### 5.3 What is not a hard collision

The following do not stop the server:

- Same id, different loader (section 4): warning only.
- Same asset namespace (for example both produce `assets/foo/...`): the two are merged
  using the per-type rules from spec v3 #129, and the winner and loser are recorded in
  the pack source map. A yellow warning is printed.
- Same permission node or command name: reported as a yellow warning. The one that
  registers second follows the platform's own rule for duplicates.

Data already saved in a world or player file is never overwritten because of a
collision. If a collision could touch saved data, a backup and a migration manifest are
required first (spec v3 #63 and #410).

---

### 5.2 Data conditions and module identity (added 2026-09-29, v0.1.4)

Forge data files can carry conditions such as `forge:mod_loaded` that name a bare module id
(`docs/upstream/forge/research.md` section 20.2). Because a plugin and a mod with the same id can both run
(section 4), a bare id is ambiguous. **Decided by Shiroz on 2026-09-29:** a condition written in Forge's
format matches only modules whose source loader is `forge`. The same principle (match within the
condition's own loader) is the default for the other loaders' conditions, subject to each loader's own
research. Not yet checked: how Fabric, NeoForge and Quilt name their equivalent conditions.

### 5.3 Ids registered outside `RegisterEvent` also count as registry keys (added 2026-09-29, v0.1.5)

**Decided by Shiroz on 2026-09-29 (answer "yes" to the question in `docs/upstream/forge/research.md` 22.4):** for the
duplicate rule of section 5, these ids count as registry keys, exactly like ids registered through `RegisterEvent` or
`DeferredRegister`:
- advancement trigger ids added with `CriteriaTriggers#register`;
- ingredient serializer ids and condition serializer ids added with `CraftingHelper#register`.

The same key twice in the same kind of registry is therefore a hard stop with the red error of section 5, and VESSEL does
not start (decision 17).

New requirement this creates (not designed): the Forge docs place these calls in `FMLCommonSetupEvent` (through
`enqueueWork`), which runs **after** the registration step. Section 5.1's static scan and isolated dry run and decision 19's
"registries complete at step 10" must therefore also cover those setup-time calls, either by finding the calls statically
or by letting the dry run and step 10 run the enqueued setup work that makes them. Which of the two is used, and how a dry
run keeps that work from touching the world, is listed in section 9. The docs do not say what the game itself does on a
duplicate id for these calls (not read), so VESSEL's rule is stricter by choice, not a copy of upstream behavior.

**Verification and scope note, 2026-09-30 (v0.1.7, `docs/upstream/forge/research.md` section 27).** Checked against the Forge 26.3 source: condition codecs and
ingredient serializers are ordinary registries (`forge:condition_codecs`, `forge:ingredient_serializers`) from the 1.20.4 branch on, and
`CraftingHelper` no longer exists from 1.21.1 on. So this section's special rule is needed only for Forge 1.20.1 and earlier. From 1.20.4 on these ids are
plain registry keys and sections 5 and 5.1 already cover them. For trigger ids in 26.x no Forge registration code was found, so that part is unresolved.
The decision itself (ids count as registry keys) is unchanged.
Follow-up of 2026-09-30 (v0.1.8, `research.md` section 28.5): Forge 26.3 has no trigger registry and no trigger registration code of its own, so for trigger ids VESSEL has to apply this rule itself and cannot
rely on Forge's registry classes. Also from section 28.4: only `forge:entity_data_serializers` and `forge:display_contexts` refuse overrides in 26.3.

**Follow-up of 2026-10-03 (v0.1.9, `research.md` section 34.3; supersedes the "unresolved" and "no trigger registry" statements above for trigger ids from the 1.20.4 branch on).** The earlier search missed the Forge test module. From the 1.20.4 head on, a mod registers a trigger through a `DeferredRegister` on the vanilla registry `BuiltInRegistries.TRIGGER_TYPES`, so trigger ids are entries of a vanilla registry that goes through `RegisterEvent`. Sections 5 and 5.1 therefore see them like any other registry key, and the special rule of this section (calls to `CriteriaTriggers#register`) is needed only for Forge 1.20.1 and earlier, the same scope as for the serializer ids. Forge adds no override rule and no duplicate rule of its own for that registry; what vanilla does on a duplicate trigger key was not read, so for trigger ids too the hard stop is VESSEL's own rule. The decision itself (ids count as registry keys) is unchanged.

**Namespaces are never aliased (added 2026-10-04, v0.1.10, decision 29 of the status file).** Ids are compared as full `namespace:path` strings. `forge:x`, `c:x`, `fabric:x` and `neoforge:x` are four different ids, for registry keys, tags and every other namespaced id; VESSEL merges and looks up only entries whose full id is identical. This applies to tags too: a tag moved from `forge:` to `c:` in a newer Forge version (`research.md` 35.2) is a different tag, not a renamed one, and it is not mapped back.

### 5.4 What Forge itself does on a duplicate key (verified in the 26.3 source, added 2026-09-30, v0.1.7)

In `ForgeRegistry#add` a different object under an existing name **replaces** the old one when the registry allows overrides, and that is the default
(`RegistryBuilder`). The override is logged only at debug level, and Forge remembers the old entry with its owner namespace. Only registries that call
`disableOverrides()` throw (two in the tree). The same object registered twice for one name only logs a warning. Inside one `DeferredRegister` a repeated
name throws. VESSEL's hard stop (section 5) is therefore deliberately stricter than Forge, confirming the claim recorded under decision 8. This is the
Forge 26.3 behavior only.

**Older eras (added 2026-10-03, v0.1.9, `research.md` section 34.4).** The same structure is in the `1.12.x` head, where an override is logged only when a debug flag is on; the same messages are found by text search in `1.16.x`, `1.18.x`, `1.19.x` and `1.20.1`. In E1 (`1.7.10`) a different object under an existing name **throws** (`IllegalArgumentException`, "has been registered twice"): Forge itself refuses a duplicate name there and replaces only through substitution aliases (`GameRegistry#addSubstitutionAlias`, with `ExistingSubstitutionException` and `IncompatibleSubstitutionException`). So the statement that VESSEL is stricter than Forge holds from the `1.12.x` head on, and for E1 VESSEL's hard stop matches what Forge does. The `1.8.9`, `1.10.x` and `1.11.x` heads were checked by message counts only.

**Per-entity extended properties in 1.7.10 (added 2026-10-06, v0.1.12, `research.md` section 38.3).** `Entity#registerExtendedProperties(identifier, properties)` (E1b, `IExtendedEntityProperties`) does not fail on a repeated identifier: it appends a counter (`name1`, `name2`, ...), logs at info level and returns the identifier it actually used; a null identifier or null properties are refused with a warning and an empty string. The keys live in a plain map inside each entity instance, not in a registry, so this is not a registry-key collision under section 5; it is recorded here because it is the one place where an old Forge era renames a duplicate by itself. How VESSEL treats the renamed key when it reads saved data of such mods is not decided.

### 5.5 Entity selector tokens and argument-type classes also count as registry keys (added 2026-10-05, v0.1.11, decision 31)

Two more places in Forge 26.3 take a name or key outside `RegisterEvent` and replace an earlier entry without any message (`research.md` section 36.2):
`EntitySelectorManager.register(token, type)` (the `@token` selectors of commands; a static map; only the five vanilla tokens `p`, `a`, `r`, `s`, `e`, an empty token
and characters not allowed in an unquoted string are refused) and `ArgumentTypeInfos.registerByClass(Class, info)` (a plain put keyed by the Java argument class,
used by Forge's own `enum` and `modid` argument types and by any mod that adds an argument type). Shiroz answered "yes" on 2026-10-05 to treating both like
registry keys: **two modules that register the same selector token, or an argument-type info for the same argument class, are a hard collision under sections 5
and 5.1**; the server does not start and the report names both modules.

- The token is the key. `@p`, `@a`, `@r`, `@s` and `@e` are reserved by vanilla; Forge already refuses them, and VESSEL reports them in the same way.
- For argument types the Java class is the key, not the registry id. The ids of argument type infos are ordinary registry keys and follow section 5.1 already.
- Like the other setup-time registrations (section 5.3), both calls happen in code, so they are found by the static scan and the isolated dry run; how exactly is still an open item (section 9).
- Open: whether the same argument class shipped in two different library jars (shaded copies) counts as one class or two. The call sites of the selector hook in the vanilla parser were not read.
- Cross-loader: whether NeoForge, Fabric and Quilt have the same two key spaces is a question for their own passes.

## 6. When the loader cannot be decided

If the classifier cannot decide (section 2.1 rule 5, or a JAR with no readable metadata):

- The module is reported as `UNKNOWN_LOADER` in `reports/`.
- It is placed in quarantine (spec v3 #380) and not loaded.
- A yellow warning names the JAR and says which files were missing.

---

## 7. Console colors

| Level | Color | Used for |
|---|---|---|
| Normal | White | Regular progress and loading messages |
| Warning | Yellow | Collisions that do not stop the server, skipped or quarantined modules |
| Error | Red | Hard collisions and anything that stops the server |

Log wording follows the project rule: natural and readable, never stiff or robotic.
Everything in the project, including config files and their comments, is written in
English.

---

## 8. Effect on the Startup Build Stage

The build cache key now includes the source loader, so two modules with the same id
but different loaders can never share a cache entry or a render bundle:

```
cache key = sourceHash + sourceLoader + moduleId + targetMinecraftVersion
            + client type + converterVersion + configHash
```

Collision detection (sections 4 and 5) runs at startup step 7, before the Startup Build
Stage at step 11, so the set of modules is fixed before any build starts and a build is
never thrown away halfway because of a collision.

---

## 9. Open items

- Exact JSON shape of `reports/identity-collisions.json`.
- Whether the `vessel-config/` file that controls collision behavior gets its own file
  or lives in an existing one.
- Setup-time registrations (trigger ids, ingredient and condition serializer ids) and how the scan and step 10 cover them
  (section 5.3).
- Selector tokens and argument-type classes as registry keys (section 5.5): shaded copies of one argument class, and how the scan finds the two calls.
- Design details of the up-front duplicate scan (option C, decided 2026-09-29): what counts as a
  "sign" of names built in code, how the isolated dry run works for each loader, and what happens to a
  module whose dry run cannot be made safe.
- Full rewrite of the affected spec v3 sections (#10, #13, #66, #142-#146, #550,
  #601-#602) with the new per-loader layout: to be done in the master spec rewrite.
