# VESSEL: Startup Build Stage & Version Model

**Document version:** 0.1.3
**Status:** Draft, approved by Shiroz. Loader-specific detail will be added as each phase is researched.

---

## 0. Scope

This document describes how VESSEL starts up, and in particular how it builds the
render bundle (resource pack, data pack, texture pack and Bedrock add-on) for every
Minecraft version and loader combination that the content on the server needs.

It replaces an earlier design where the build happened lazily when the first player
joined with a version nobody had built yet. That design is gone. The build now happens
once, at startup, before the world loads and before the port opens.

It updates section `# 414. SERVER STARTUP ORDER` of the original specification (Full
Master Engineering Specification v3). The changed part is shown in section 2.

Related document: `identity-and-collision.md` (module identity, loader classification,
duplicate handling).

---

## 1. VESSEL's own versioning

VESSEL has its own version number (for example `0.0.1`, `0.0.2`), completely separate
from any Minecraft version. It works the same way as a mod loader such as Fabric Loader,
whose version number says nothing about which Minecraft versions it can run.

- VESSEL is not aimed at one fixed Minecraft version.
- By default it must be able to run on every Minecraft version it supports. The
  supported range comes from the Version Registry (spec Phase 1): every version the
  upstream loader still officially maintains (CURRENT_STABLE, CURRENT_BETA/SNAPSHOT,
  MAINTAINED, LEGACY_SUPPORTED), excluding anything upstream has declared EOL or
  UNSUPPORTED.

---

## 2. Where the Startup Build Stage sits in the startup order

```
 1. config load
 2. filesystem init
 3. version detection
 4. platform detection
 5. source scanning (mods/, plugins/, packs)
 6. pack scanning
 7. dependency graph resolution
    + module identity and collision checks (see identity-and-collision.md)
 8. module analysis (mixin and transform detection)
 9. UIR construction
10. registry bootstrap
--------------------------------------------------------------------
11. STARTUP BUILD STAGE (new)
    - builds the render bundle (resource pack + data pack + texture pack +
      Bedrock add-on) for every (Minecraft version x source loader)
      combination that the content installed on THIS server needs
    - it does not build for every version VESSEL could theoretically support,
      only for versions that installed mods, plugins or packs actually need
    - in spirit it is like the "loading plugins" phase of a normal Bukkit or
      Paper server: the world is NOT loaded, the port is NOT open, and no
      mod or plugin is enabled while this stage runs
--------------------------------------------------------------------
12. target adapter registration
13. network init
14. world load
15. port open (the server starts accepting connections)
16. per player: resource pack, data pack and add-on transfer (loading screen,
    see section 7)
```

The stage sits after steps 9-10 (VESSEL has to analyze every mod, plugin and mixin
before it knows what to build) and before steps 12-15 (no world, port or network yet).

Identity and registry collisions are detected at step 7, so the set of valid modules is
fixed before any build starts. A build is never thrown away halfway because of a
collision.

### 2.1 What the build stage may rely on (added 2026-09-29, v0.1.2)

Decided on 2026-09-29 (Shiroz delegated the choice, the option below was picked as the best fit):

- **Registries are complete before the stage starts.** Step 10 (registry bootstrap) finishes every
  module's registration, with no world, no port and no network. The stage then works from a snapshot
  of the finished registries. Data conditions such as `item_exists` and `tag_empty`
  (`docs/upstream/forge/research.md` section 20.2) are evaluated against that snapshot.
- **VESSEL merges tags itself** with the documented algorithm (`replace`, `required`, `remove`,
  research.md section 20.4) when it needs merged tags for a condition or for the bundle, instead of
  waiting for a world-time data reload.
- **Mod code keeps running on the server** for behavior that is code, not data: global loot modifiers,
  custom recipe matching, custom ingredients and code-only recipes (brewing, anvil, loom). Only the part
  a player's client can see or download goes into the bundle.
- **Condition identity:** a Forge-source condition such as `forge:mod_loaded` matches only modules whose
  source loader is `forge` (agreed by Shiroz on 2026-09-29, see `identity-and-collision.md`).

Wording conflict to settle: section 2 above says "no mod or plugin is enabled while this stage runs".
Running a module's registration code at step 10 is not the same as enabling it (no plugin `onEnable`,
no gameplay event handlers, no scheduler), and this note relies on that difference. The earlier text did
not say whether step 10 runs mod registration code or derives registries from the UIR alone, so this is a
new requirement, not a restatement. It is listed in section 10.

---

## 3. First-run flow

```
                     +---------------------------+
                     |    VESSEL server start    |
                     +-------------+-------------+
                                   |
                     +-------------v-------------+
                     | Was --vessel-version      |
                     | passed on the command     |
                     | line?                     |
                     +------+-------------+------+
                        YES |             | NO
                            |             |
          +-----------------v--+      +---v----------------------------+
          | Skip the sentinel  |      | Does vessel-first-run-ack.txt  |
          | file and the       |      | exist next to the jar?         |
          | 45-second warning  |      +---+------------------------+---+
          | completely. Build  |      YES |                        | NO
          | only the versions  |          |                        |
          | listed in the flag |   +------v---------+   +----------v-----------+
          +---------+----------+   | Build now, for |   | Print the warning    |
                    |              | every version  |   | (English), hold for  |
                    |              | the content    |   | exactly 45 seconds,  |
                    |              | needs. No      |   | then:                |
                    |              | questions      |   |  - write the         |
                    |              +------+---------+   |    sentinel file     |
                    |                     |             |    (with timestamp)  |
                    |                     |             |  - EXIT the server   |
                    |                     |             |    (nothing built)   |
                    |                     |             +----------------------+
                    +---------------------+
                                |
                   +------------v-------------+
                   | Continue with steps      |
                   | 12-16 from section 2     |
                   +--------------------------+
```

If the "no sentinel file" branch is taken, the server exits after the 45 seconds and
does not build anything. The admin has to start it again. On that next start the
sentinel file exists, so the server goes straight into the full build without asking.

### 3.1 The 45-second warning

The wording is natural and friendly, not robotic. This applies to all VESSEL logs:

```
[VESSEL] Heads up: this is the first time VESSEL is starting on this server.

By default, VESSEL builds a complete render bundle (resource packs, data packs,
texture packs and Bedrock add-ons) for every Minecraft version your installed
mods and plugins actually need, legacy versions included. This is a genuinely
heavy one-time job. On modest hardware it can take several hours, and in
extreme cases (many legacy versions, a large number of mods and plugins) it can
stretch into a day or more.

If you would rather limit this to specific versions, stop the server now and
start it again with:

    --vessel-version="26.2"
    --vessel-version="26.2","26.1","1.18.1"      (comma-separated for several)

If you do nothing, VESSEL will remember that you have seen this message and
start the full build automatically the next time you launch the server.

Continuing in 45 seconds. This run will exit without building anything.
```

After exactly 45 seconds VESSEL writes the sentinel file, prints
`[VESSEL] Noted. Start the server again whenever you are ready to build.`, and exits
with exit code `0`. This is intended behavior, not an error.

**Fixed rule:** this warning always appears exactly once, on the very first run. It
cannot be turned off or configured in any way. No setting in `vessel-config/` can
skip it, because `vessel-config/` does not exist yet before that first run. Only the
duration of similar warnings elsewhere in the future could be configurable. It never
applies to this first-run event itself.

---

## 4. Sentinel file

| Property | Value |
|---|---|
| Name | `vessel-first-run-ack.txt` |
| Location | Same folder as the server jar (server root) |
| Content | One line: the ISO-8601 timestamp of the first run |
| Purpose | Marks that the 45-second warning has already been shown. Only its existence is checked, never its content |

---

## 5. The `--vessel-version` flag

```
--vessel-version="26.2"
--vessel-version="26.2","26.1","1.18.1"
```

- Accepts one or several versions, separated by commas.
- When present, it always skips the sentinel file and the 45-second warning, on every
  run, including the very first.
- The build is limited to exactly the listed versions. No other version is touched.

---

## 6. Interrupted builds (Ctrl+C during a build)

- A combination that was **in the middle of building** when the server stopped is
  thrown away completely and rebuilt from scratch on the next start. VESSEL never tries
  to resume from a half-finished state, so a cut-off build can never leave a corrupted
  artifact behind.
- A combination that had **already finished building** is kept and not rebuilt.

---

## 7. What the per-player loading screen does now

In the old design the loading screen also covered the case where a player was first to
join with a version nobody had built yet. That case no longer exists: every needed
version combination is built before the world and port open.

The loading screen is still required, but its job is smaller: it works like the normal
vanilla resource pack loading screen. The player has to finish downloading and loading
the already-built resource pack, data pack and add-on before entering the world. There
is no "join and wait for a build" situation anymore.

---

## 8. Logging

- Build progress is printed to the console by default while the Startup Build Stage
  runs, for example:
  ```
  [VESSEL] Building render bundle: fabric x 26.2, going through 14 mods...
  ```
- It can be switched on or off in `vessel-config/build.yml` (see section 9).
- All VESSEL log text, not only this stage, reads naturally and avoids stiff, robotic wording.
- Colors: normal output is white, warnings are yellow, errors are red.

---

## 9. The `vessel-config/` folder

Location: the server root, next to `mods/`, `plugins/` and `config/`. It is not nested
inside `config/`.

```
vessel-config/
+-- build.yml         # Startup Build Stage: progress logging on/off, build worker
|                     # count, priority queue order, cleanup of cancelled builds
+-- versions.yml      # Version Registry overrides, for example limiting how far
|                     # back LEGACY_SUPPORTED goes
+-- first-run.yml     # duration of similar warnings in the future (does NOT affect
|                     # the very first run, see section 3.1)
+-- filesystem.yml    # carried over from spec v3 #546
+-- packs.yml         # carried over from spec v3 #547
+-- ignore.yml        # carried over from spec v3 #548
+-- geyser.yml        # bundled Geyser on/off, detection override for an external Geyser
+-- identity.yml      # behavior for duplicate ids (see identity-and-collision.md)
+-- defaults/
|   +-- forge/        # default config files of Forge mods (decision 26 of the status file)
+-- overrides/
    +-- <loader>/<id>/...   # per-module overrides (spec v3 #545), now split by loader
```

This folder replaces the config files that spec v3 placed under `config/`
(`universal-loader.yml`, `packs.yml`, `ignore.yml`, `filesystem.yml`, `versions.yml`,
`config/overrides/<module>/`). Everything an admin can change about VESSEL itself now
lives in this one folder.

All files are written in English with friendly, plain comments.

The `defaults/` folder was added on 2026-10-01 (decision 26 of the status file). Forge copies a default config file from its `defaultconfigs` folder into the config folder when a mod's config file is first created; VESSEL applies the same rule with `vessel-config/defaults/forge/` as the source folder. Which files go there and how the copy is wired into the startup flow is not designed yet (section 10).

---

## 10. Open items

- Layout and wiring of `vessel-config/defaults/<loader>/` (added 2026-10-01): only `forge` is decided so far; the other loaders get their own subfolder when their research is done, and the first-creation copy still has to be placed in the startup flow.
- Full field list, types and defaults for every file in `vessel-config/`. Only the file
  names exist so far.
- A full technical spec of the Build Worker Pool: default worker count, the formula
  based on CPU cores, and how a build job is split into steps that can yield.
- The full server directory tree and a startup execution flow with concrete function
  and module names. Shiroz asked for both in an easy-to-find place, but whether they go
  in this file or in separate files is not yet decided.
- Step 10 must run each module's registration code (decision of 2026-09-29, section 2.1). Not yet
  designed: what "enabled" means per loader, how registration code is kept from touching the world, and
  what happens if a module's registration needs world state (the same problem as the dry-run skip in
  `identity-and-collision.md` section 5.1).
