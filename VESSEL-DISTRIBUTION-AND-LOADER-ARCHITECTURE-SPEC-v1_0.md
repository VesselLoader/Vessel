# VESSEL — Distribution, Installer, Bootstrap & Loader Architecture Supplement

**Specification type:** Supplemental Master Engineering Specification
**Project:** VESSEL — **Virtualized Ecosystem Shared Server & Engine Loader**
**Version:** 1.0.0
**Date:** 2026-10-07
**Status:** Proposed implementation contract for the next engineering stage
**Audience:** VESSEL core maintainers, contributors, implementation AIs, release automation
**Primary rule:** No implementation work may advance past the Forge gate until Forge research and Forge adapter work are explicitly marked 100% complete and independently verified.

---

## 0. Purpose and scope

This document fills the architectural boundary that was still missing between the existing VESSEL architecture/research package and a production-like loader distribution model.

It defines the system that makes VESSEL install and operate like a conventional Minecraft loader while preserving VESSEL's universal-runtime vision.

This document specifically defines:

- VESSEL Bootstrap and Loader entrypoint behavior.
- Fresh installation and existing-server installation.
- The exact first action when a VESSEL server JAR is launched.
- Automatic VESSEL system update before normal server startup.
- Logging and user-visible restart behavior after an update.
- Safe update, rollback and recovery semantics.
- Separation between Bootstrap, Runtime, Loader Core and Source Loader Adapters.
- Version manifests, release channels and compatibility metadata.
- GitHub Pages as static website/manifest hosting.
- GitHub Releases as binary distribution.
- GitHub Actions as the build/test/release pipeline.
- Offline installation and offline startup behavior.
- Security, integrity verification and update trust boundaries.
- Contributor workflow, ownership and code-approval rules.
- Repository and directory layout.
- Future migration from GitHub-only distribution to CDN/custom infrastructure without redesigning VESSEL Loader Core.

This document does not redefine already-final architecture decisions unless an explicit section below says that it is a refinement or superseding rule.

---

# 1. Canonical project identity

## 1.1 Full name

**VESSEL = Virtualized Ecosystem Shared Server & Engine Loader**

The expansion above is the canonical full project name.

## 1.2 Product identity

VESSEL is a universal Minecraft server/runtime and compatibility platform designed to host modules originating from multiple source loader ecosystems through a common runtime.

The word "Loader" in the product name refers to VESSEL's role as the host/runtime loading layer. VESSEL is not merely a conversion utility and is not to be described as only a compatibility patch layer.

## 1.3 Product layers

VESSEL must be treated as four strongly separated product layers:

```text
User-facing distribution
    VESSEL Installer / website / releases
                ↓
Bootstrap layer
    vessel-bootstrap / stable entry JAR
                ↓
Loader Runtime
    VESSEL Loader Core
                ↓
Compatibility layer
    Source Loader Adapters
    ├── forge
    ├── fabric
    ├── quilt
    ├── neoforge
    └── plugins / Bukkit family
                ↓
Minecraft + vanilla runtime
```

The boundaries are mandatory. Installer behavior must not leak into loader semantics. Loader Core must not know how GitHub is hosted. Source adapters must not know how releases are distributed.

---

# 2. Non-negotiable implementation gates

## 2.1 Forge-first gate

**Forge is the first and mandatory completed loader.**

No work on a new source loader may be considered started merely because its research folder or placeholder adapter exists.

The required order is:

```text
Forge research completion
        ↓
Forge architecture review
        ↓
VESSEL Loader Core minimum implementation
        ↓
Forge Adapter implementation
        ↓
Forge runtime integration
        ↓
Forge end-to-end compatibility test matrix
        ↓
Forge = 100% gate
        ↓
ONLY NOW may the next loader begin
```

## 2.2 What "Forge = 100%" means

Forge does not reach 100% merely by changing a percentage in the status file.

A Forge gate may be marked **100% COMPLETE** only when all of these are true:

1. Every currently PARTIAL Forge research row is closed or explicitly documented as a proven documented limit under the existing status rules.
2. The Forge version registry and version/source mapping inputs required by the adapter are present.
3. Metadata parsing/normalization is implemented for every supported Forge era covered by the declared scope.
4. Forge lifecycle, registries, events, threading, networking, persistent data, transformers/coremods and gameplay API contracts required by VESSEL are implemented or intentionally represented by a documented capability boundary.
5. Source-name-space handling required by the official-mapping requirement is implemented for Forge.
6. Forge module discovery, dependency resolution, lifecycle execution and teardown are operational.
7. The Forge adapter can boot at least one representative module on every declared supported architectural era that has an executable test target.
8. Forge-specific failure modes produce deterministic VESSEL diagnostics.
9. Automated regression tests pass.
10. The status file is updated with evidence and the gate is explicitly signed off by the project owner.

## 2.3 Hard lock

An implementation AI MUST refuse to start a new loader adapter if the canonical status source does not report:

```text
Forge Gate: PASS
Forge Research: 100%
Forge Adapter: COMPLETE
Forge Runtime E2E: PASS
```

A task request that conflicts with this order must be treated as deferred work, not as permission to bypass the gate.

---

# 3. Terminology

| Term | Meaning |
|---|---|
| VESSEL | Virtualized Ecosystem Shared Server & Engine Loader |
| Bootstrap | Small, stable process entry component responsible for selecting and starting the runtime |
| Loader Core | VESSEL's own universal runtime loader and module orchestration engine |
| Source Loader | The ecosystem from which a module originates, e.g. Forge/Fabric/Quilt/NeoForge/Plugins |
| Adapter | VESSEL implementation translating one source loader's lifecycle/metadata/API contracts into Loader Core contracts |
| Module | A loadable source artifact such as a mod or plugin |
| Runtime | The actual executing VESSEL core and its loaded adapters/modules |
| Distribution | Installer, website, manifests, Releases, update channels and delivery mechanisms |
| Manifest | Signed/verified metadata describing versions, artifacts, compatibility and update policy |
| System Update | Update of VESSEL-owned components only; never an arbitrary user mod update |
| Game Version | Minecraft version being hosted |
| VESSEL Version | VESSEL's independent release version |
| Adapter Version | Source-loader integration implementation version |
| Channel | Stable, Beta, Nightly, or future channels |
| Install Root | Folder containing a VESSEL installation |
| Server Root | Minecraft server data/world/config directory tree |
| Restart-required update | An update that cannot safely replace the currently loaded runtime in place |
| Gate | Hard status barrier that must pass before work proceeds |

---

# 4. Product behavior: VESSEL must feel like a normal loader

## 4.1 Target user experience

A normal user should be able to think:

> Download VESSEL. Install it for my Minecraft server. Run the VESSEL server JAR. VESSEL checks itself, updates if necessary, then starts the server.

The user should not need to understand:

- which internal component is Bootstrap versus Runtime;
- how GitHub Releases are structured;
- which manifest file is used;
- where update metadata is hosted;
- how adapter bundles are cached;
- which GitHub workflow built the artifact.

Those details are implementation concerns.

## 4.2 Conventional-loader installation model

VESSEL MUST support at minimum:

### Fresh server installation

```text
VESSEL Installer
  → choose Minecraft version
  → choose installation type
  → choose server folder
  → download required files
  → verify integrity
  → create server runtime layout
  → create launch scripts
```

### Existing server installation

```text
Existing Minecraft Server
  → select Install VESSEL
  → detect game version
  → validate server layout
  → preserve user data
  → add VESSEL files
  → generate/update launch entrypoint
```

The installer must never silently delete a user's mods, configs, worlds, server properties or unknown files.

---

# 5. Runtime component boundary

## 5.1 VESSEL Bootstrap

Bootstrap is the stable entrypoint that users invoke.

Responsibilities:

- Locate the installation root.
- Read the bootstrap configuration.
- Determine the selected VESSEL release/channel.
- Verify the current bootstrap/runtime metadata.
- Perform system update before normal server startup.
- Prepare the runtime classpath/process invocation.
- Emit update/startup diagnostics.
- Exit cleanly after an update that requires restart.
- Start Loader Core only after update checks pass.

Bootstrap must not implement loader-specific APIs.

## 5.2 VESSEL Loader Core

Loader Core owns:

- module discovery;
- module identity;
- dependency graph;
- classloading boundary;
- transformation pipeline orchestration;
- lifecycle orchestration;
- universal registry view;
- event and callback abstraction;
- source-loader adapter dispatch;
- module sandbox/virtual filesystem policy;
- VESSEL configuration;
- startup state machine;
- logging conventions;
- failure categorization;
- capability negotiation.

Loader Core MUST NOT contain GitHub-specific URLs or GitHub API logic.

## 5.3 Source Loader Adapter

Each adapter owns source-loader semantics.

Example:

```text
ForgeAdapter
  ├── version/era selector
  ├── metadata reader
  ├── dependency semantics
  ├── lifecycle mapping
  ├── registry mapping
  ├── event mapping
  ├── transform integration
  ├── config semantics
  ├── resource/data semantics
  └── compatibility exceptions
```

Adapter code may consume Loader Core services but may not bypass Loader Core's process ownership and security boundaries.

## 5.4 Runtime payload

Runtime payloads may be split physically into multiple artifacts even when presented to users as one VESSEL installation.

Recommended structure:

```text
vessel/
├── bootstrap/
├── runtime/
│   └── <vessel-version>/
├── adapters/
│   ├── forge/
│   ├── fabric/
│   ├── quilt/
│   ├── neoforge/
│   └── plugins/
├── mappings/
├── libraries/
├── cache/
├── manifests/
└── state/
```

The user-facing server JAR may be a small bootstrap executable while the runtime remains versioned and separately replaceable.

---

# 6. Canonical server launch sequence

## 6.1 Absolute first action

Whenever a VESSEL server installation is launched through the official VESSEL entrypoint, **the update state machine is the first VESSEL-controlled operation before normal Minecraft server initialization.**

The server must not:

- bind the normal Minecraft port;
- open a world;
- begin module runtime;
- initialize source-loader gameplay state;
- start player acceptance;

until the update preflight has reached a safe terminal state.

## 6.2 Complete boot state machine

```text
BOOT
 ↓
LOCATE_INSTALLATION
 ↓
READ_BOOTSTRAP_CONFIG
 ↓
VALIDATE_LOCAL_STATE
 ↓
LOAD_LOCAL_MANIFEST
 ↓
CHECK_NETWORK_POLICY
 ↓
FETCH_REMOTE_MANIFEST
 ↓
VERIFY_MANIFEST_INTEGRITY
 ↓
COMPARE_VESSEL_SYSTEM_VERSION
 ↓
NO UPDATE? ───────────────→ PREPARE_RUNTIME
       |
       YES
       ↓
DISPLAY_UPDATE_HEADER
       ↓
DOWNLOAD_UPDATE_PAYLOADS
       ↓
VERIFY_ARTIFACT_HASHES
       ↓
STAGE_UPDATE
       ↓
COMMIT_UPDATE_ATOMICALLY
       ↓
UPDATE_LOCAL_STATE
       ↓
WRITE_SUCCESS_LOG
       ↓
RESTART_REQUIRED
       ↓
EXIT CLEANLY

PREPARE_RUNTIME
 ↓
LOAD VESSEL LOADER CORE
 ↓
DISCOVER MODULES
 ↓
RUN COLLISION/REGISTRY PRE-SCAN
 ↓
RESOLVE DEPENDENCIES
 ↓
SELECT SOURCE ADAPTERS
 ↓
REGISTER/INITIALIZE MODULES
 ↓
STARTUP BUILD STAGE
 ↓
START MINECRAFT SERVER
```

## 6.3 Update-before-runtime rule

An update is considered a **system update**, not a world or module operation.

The bootstrap must update VESSEL-owned artifacts before loading the current runtime.

Never update a loaded classpath in place.

---

# 7. Why restart is mandatory after an update

## 7.1 Design rule

VESSEL must not hot-replace its own loaded runtime classes or core JARs during normal server execution.

Reason:

- classes may already be loaded;
- classloaders may retain old bytecode;
- native libraries may be locked;
- transformers may have cached state;
- adapters may retain static references;
- Minecraft runtime state may depend on the old implementation.

Therefore:

```text
Update detected
    ↓
Download
    ↓
Verify
    ↓
Stage
    ↓
Commit files
    ↓
STOP
    ↓
User launches again
    ↓
New runtime starts
```

## 7.2 User-facing message

The message must be explicit:

```text
VESSEL SYSTEM UPDATE COMPLETED

The new VESSEL runtime has been installed successfully.
A restart is required before the updated runtime can be loaded.

VESSEL is shutting down now.
Please start the server again to continue with the new version.
```

Do not claim that the new runtime is active when the current process still has the previous runtime loaded.

---

# 8. Console and restart behavior

## 8.1 Console logging

All update operations must be visible in the console.

Minimum stages:

```text
[VESSEL] Checking for system updates...
[VESSEL] Current system version: x.y.z
[VESSEL] Update channel: stable
[VESSEL] Checking remote release metadata...
[VESSEL] Update available: a.b.c
[VESSEL] Downloading runtime package...
[VESSEL] Verifying SHA-256...
[VESSEL] Verification passed.
[VESSEL] Installing update...
[VESSEL] Update installed successfully.
[VESSEL] Restart required.
[VESSEL] Shutting down.
[VESSEL] Please start the server again to load VESSEL a.b.c.
```

## 8.2 Color policy

Preserve the existing project rule:

- White: normal output.
- Yellow: warning.
- Red: error.

Update progress should remain visually readable when colors are disabled.

## 8.3 Console closing

The phrase "close console" is interpreted safely as:

- VESSEL must terminate its process after a successful restart-required update.
- When launched from an official desktop launcher/start script that owns a dedicated console window, that parent launcher may close the window after VESSEL exits.
- When the user started `java -jar` from an existing terminal, VESSEL MUST NOT attempt to close the user's terminal application; it simply exits.
- The console-close behavior must therefore never rely on unsafe OS-specific hacks.

## 8.4 Exit codes

Recommended codes:

| Code | Meaning |
|---:|---|
| 0 | Clean exit; includes successful update followed by required restart |
| 1 | Generic startup/runtime failure |
| 2 | Configuration error |
| 3 | Network/update metadata failure where safe fallback is impossible |
| 4 | Artifact integrity verification failure |
| 5 | Update installation/rollback failure |
| 6 | Dependency/module resolution failure |
| 7 | Compatibility/runtime mapping failure |

The exact numeric values may be refined, but the categories must remain distinct.

---

# 9. Update policy

## 9.1 What is auto-updated

Auto-update is limited to VESSEL-owned system artifacts unless a future explicit feature says otherwise.

Eligible components:

- Bootstrap;
- Loader Core;
- installed source-loader adapters;
- VESSEL-shipped helper libraries;
- mapping metadata required by the installed VESSEL release, subject to licensing and the existing mapping policy;
- VESSEL-generated manifest/index files.

## 9.2 What is NOT auto-updated by default

Never silently update:

- user-installed mods;
- user-installed plugins;
- world files;
- server configs;
- user scripts;
- resource packs not owned by VESSEL;
- third-party content.

A future separate "dependency update" system may exist, but it must be opt-in and distinct from the core self-update mechanism.

## 9.3 Frequency

Default behavior:

- every official server launch checks once;
- cache a recently-verified manifest for a short configurable offline grace period;
- do not perform an infinite retry loop;
- one launch performs one update decision;
- if the update succeeds and requires restart, exit;
- next launch checks again.

## 9.4 Network failure

If the remote manifest cannot be reached:

1. If the current installed VESSEL runtime is valid and policy allows offline startup, print a yellow warning and continue.
2. If a security policy requires a fresh manifest for a critical security release, fail closed with a clear message.
3. Never delete the working runtime because a remote server is unavailable.

The default stable channel should favor availability while still verifying any artifact it downloads.

---

# 10. Release channels

Required initial channels:

```text
stable
beta
nightly
```

## 10.1 Stable

For ordinary users.

Rules:

- only owner-approved releases;
- full automated test suite;
- release notes required;
- compatibility status required;
- artifact hashes/signatures required by the release contract;
- bootstrap update may consume stable only when the local policy selects stable.

## 10.2 Beta

For users who intentionally opt in.

Rules:

- may contain incomplete future features;
- still must be installable and recoverable;
- compatibility warnings must be explicit;
- never present beta as stable.

## 10.3 Nightly

For development/testing.

Rules:

- generated automatically from a selected branch/commit;
- no stability guarantee;
- clear date/commit identifier;
- opt-in only.

---

# 11. Manifest architecture

## 11.1 Design principle

The installer and bootstrap must speak a **distribution-neutral manifest protocol**.

The first backend is GitHub, but the manifest schema MUST NOT contain GitHub-specific semantics in the core data model.

Bad:

```json
{"github_release_id": 12345}
```

Good:

```json
{
  "artifact": {
    "id": "vessel-runtime",
    "version": "1.0.0",
    "channel": "stable",
    "download": {
      "url": "https://...",
      "sha256": "...",
      "size": 123456
    }
  }
}
```

## 11.2 Manifest envelope

Recommended schema:

```json
{
  "schema": 1,
  "generated_at": "2026-10-07T00:00:00Z",
  "project": "vessel",
  "product": {
    "name": "VESSEL",
    "full_name": "Virtualized Ecosystem Shared Server & Engine Loader"
  },
  "channels": {
    "stable": {...},
    "beta": {...},
    "nightly": {...}
  }
}
```

## 11.3 Release object

```json
{
  "version": "1.0.0",
  "channel": "stable",
  "published_at": "2026-10-07T00:00:00Z",
  "minimum_bootstrap": "1.0.0",
  "minecraft": {
    "supported": ["1.21.11"]
  },
  "artifacts": [
    {
      "id": "vessel-bootstrap",
      "kind": "bootstrap",
      "url": "...",
      "sha256": "...",
      "bytes": 123456
    },
    {
      "id": "vessel-runtime",
      "kind": "runtime",
      "url": "...",
      "sha256": "...",
      "bytes": 2345678
    }
  ]
}
```

## 11.4 Compatibility metadata

The manifest must be capable of representing:

- VESSEL version;
- Minecraft version range;
- source loader support;
- adapter version;
- minimum Java version;
- architecture/OS requirements if ever needed;
- required companion artifacts;
- known incompatibilities;
- release channel;
- update severity;
- deprecation/replacement information.

---

# 12. GitHub Pages architecture

## 12.1 Role of Pages

GitHub Pages is the **presentation and lightweight metadata layer**.

It hosts:

- the public VESSEL website;
- documentation landing pages;
- compatibility views generated as static data;
- release summaries;
- contributor information;
- security/legal pages;
- small JSON manifests if they remain within the website deployment model.

It must NOT become a binary file dump.

## 12.2 Current GitHub Pages limits to design around

GitHub currently documents these Pages limits:

- published site maximum: 1 GB;
- source repository recommended limit: 1 GB;
- soft bandwidth limit: 100 GB/month;
- deployment timeout: 10 minutes;
- soft build limit: 10 builds/hour, except that this build-frequency limit does not apply when publishing through a custom GitHub Actions workflow;
- rate limiting may occur;
- one user or organization site per GitHub account.

GitHub also states that Pages is not intended or allowed to be used as a free hosting service primarily intended to run an online business, e-commerce site or SaaS. A public open-source project site and project documentation should therefore remain the intended use case; binary distribution should use Releases rather than abusing Pages as a file CDN. [Research source: GitHub Pages limits, checked 2026-10-07.] 

## 12.3 Bandwidth strategy

The site must keep heavy assets off Pages.

Do not place these on Pages:

- installer JARs;
- runtime JARs;
- large Minecraft libraries;
- generated server distributions;
- large media archives;
- backup ZIPs.

Use GitHub Releases or a future CDN for these.

## 12.4 Pages repository layout

Recommended:

```text
vessel-site/
├── src/
├── public/
│   ├── assets/
│   ├── icons/
│   └── media/
├── data/
│   ├── manifest/
│   ├── compatibility/
│   └── releases/
├── docs/
├── scripts/
└── .github/workflows/
```

The exact framework is intentionally not fixed here. The website must export to static files and remain hostable by Pages.

---

# 13. GitHub Releases architecture

## 13.1 Role

GitHub Releases is the canonical initial binary distribution service.

GitHub currently documents up to 1,000 release assets per release, a per-file limit of under 2 GiB, and no total release size or bandwidth-usage limit. [Research source: GitHub Releases documentation, checked 2026-10-07.] 

This makes Releases a substantially better home for VESSEL binary payloads than Pages.

## 13.2 Release asset naming

Use deterministic names:

```text
vessel-bootstrap-1.0.0.jar
vessel-installer-1.0.0.jar
vessel-runtime-1.0.0.jar
vessel-runtime-1.0.0.zip
vessel-server-template-1.21.11-1.0.0.zip
checksums-sha256.txt
release-manifest.json
```

## 13.3 Stable direct-link strategy

The website may link to release assets directly.

GitHub supports stable URLs for the latest release and direct download URLs for named assets. These URLs should be wrapped behind the website's distribution configuration rather than hardcoded into Loader Core. [Research source: GitHub "Linking to releases", checked 2026-10-07.] 

---

# 14. GitHub Actions architecture

## 14.1 Role

GitHub Actions performs:

- compilation;
- unit tests;
- integration tests;
- compatibility tests;
- artifact packaging;
- checksums/signatures;
- release creation;
- manifest generation;
- Pages deployment;
- release notes generation.

## 14.2 Cost assumptions

For a public repository, GitHub currently states that standard GitHub-hosted runners are free and unlimited. Private-repository usage is subject to included minute/storage quotas. GitHub documents 20 concurrent standard jobs for the Free tier and 2,000 monthly minutes as a Free-account allowance for the general quota table, while public-repository standard-runner usage is stated as free and unlimited. The architecture should therefore keep the VESSEL production repository public unless a strong reason requires otherwise. [Research source: GitHub Actions runner/billing documentation, checked 2026-10-07.] 

## 14.3 Pipeline

```text
push/PR
  ↓
static analysis
  ↓
unit tests
  ↓
Forge compatibility tests
  ↓
artifact build
  ↓
package validation
  ↓
checksum/signature generation
  ↓
release candidate
  ↓
owner approval
  ↓
GitHub Release
  ↓
manifest generation
  ↓
Pages deployment
```

## 14.4 Mandatory owner approval

The project owner retains final authority over release promotion.

Automatic CI may say:

```text
PASS
```

but it MUST NOT automatically promote an arbitrary build to Stable without the configured owner approval step.

---

# 15. Installer architecture

## 15.1 Installer responsibilities

Installer owns:

- selecting a Minecraft version;
- selecting server/client mode when supported;
- selecting VESSEL channel;
- selecting VESSEL version;
- selecting installation directory;
- detecting existing installations;
- creating or migrating VESSEL layout;
- downloading required artifacts;
- verifying artifacts;
- generating launch scripts;
- producing an installation report.

Installer does not own runtime update logic after installation. That belongs to Bootstrap.

## 15.2 Installer modes

Required modes:

```text
Fresh Installation
Existing Server Installation
Repair Installation
Upgrade Installation
Offline Installation
```

## 15.3 Fresh installation flow

```text
Welcome
 ↓
Minecraft version
 ↓
Server/client target
 ↓
VESSEL channel/version
 ↓
Java requirement check
 ↓
Destination selection
 ↓
Disk space check
 ↓
Dependency plan
 ↓
Download
 ↓
Verify
 ↓
Install
 ↓
Create launch scripts
 ↓
Final validation
 ↓
Finish
```

## 15.4 Existing server safety

Before modification:

- detect world folder;
- detect existing VESSEL folder;
- detect Forge/Fabric/other loader installations;
- detect server JARs;
- detect Java version;
- calculate a file plan.

The installer must show a summary of files it will add, modify and preserve.

## 15.5 Backup

Upgrade operations should offer or automatically create a minimal bootstrap-state backup containing:

```text
vessel/state/
vessel/bootstrap/
vessel/manifests/
vessel/config/
```

World backups are outside the installer core unless explicitly requested.

---

# 16. VESSEL server JAR contract

## 16.1 User-visible entrypoint

The normal server invocation is conceptually:

```text
java -jar vessel-server.jar
```

The physical filename may change, but there must be one clear official entrypoint.

## 16.2 Entry JAR stability

The entry JAR should remain compatible across ordinary patch updates wherever practical.

Its primary role is bootstrap, not the entire runtime.

## 16.3 Arguments

Required conceptual arguments:

```text
--vessel-channel=stable
--vessel-version=latest
--vessel-offline
--vessel-no-update
--vessel-repair
--vessel-version=<exact>
```

These must not weaken safety unintentionally.

Examples:

- `--vessel-offline` may force offline mode if the local runtime is already valid.
- `--vessel-no-update` may skip a normal update check only for a consciously chosen developer/admin workflow and must print a yellow warning.
- `--vessel-repair` runs repair validation.
- Existing version-scoping semantics in the project status document remain authoritative and must be reconciled with this naming before implementation.

---

# 17. Update transaction design

## 17.1 Staging directory

Use:

```text
vessel/state/update/
├── download/
├── staging/
├── verified/
├── backup/
└── journal.json
```

## 17.2 Transaction journal

The journal must describe:

```json
{
  "transaction": "...",
  "from_version": "1.0.0",
  "to_version": "1.0.1",
  "state": "verified|staged|committed|rolled_back",
  "artifacts": [
    {
      "id": "vessel-runtime",
      "expected_sha256": "...",
      "actual_sha256": "..."
    }
  ]
}
```

## 17.3 Atomicity

Never replace an active runtime file until every replacement file has been downloaded and verified.

Preferred sequence:

```text
download all
 ↓
verify all
 ↓
prepare full staging tree
 ↓
fsync/flush where supported
 ↓
commit directory/version switch
 ↓
write committed state
```

If the commit fails:

```text
restore previous runtime reference
mark transaction failed
leave diagnostic journal
exit non-zero
```

## 17.4 Rollback

At least the previous known-good runtime must remain recoverable.

Never remove the previous runtime before the new runtime is successfully verified.

---

# 18. Runtime selection model

Instead of rewriting the user's main entry JAR on every update, use a stable pointer/state file.

Example:

```text
vessel/runtime/
├── 1.0.0/
├── 1.0.1/
└── current.txt
```

`current.txt` contains:

```text
1.0.1
```

Bootstrap reads the pointer and launches the selected runtime.

This significantly reduces update risk and makes rollback simple.

---

# 19. Security and integrity

## 19.1 Transport

All remote metadata and artifact downloads must use HTTPS.

Plain HTTP URLs are invalid for production release metadata.

## 19.2 Artifact integrity

Every official artifact must have a SHA-256 digest.

The installer/bootstrap computes the downloaded digest and compares it before installation.

## 19.3 Stronger trust model

The architecture should reserve fields for a future cryptographic signature system:

```json
{
  "sha256": "...",
  "signature": {
    "algorithm": "...",
    "public_key_id": "...",
    "value": "..."
  }
}
```

Do not make GitHub account identity itself the artifact trust model.

## 19.4 Manifest authenticity

The bootstrap should verify the manifest integrity/authenticity according to the selected trust policy before accepting update instructions.

A future move from GitHub Pages to a custom CDN must not require changing the trust model.

## 19.5 Never execute unverified downloads

Downloaded artifacts must be:

1. downloaded;
2. stored in staging;
3. hash/signature verified;
4. checked against expected type/size;
5. only then installed/executed.

---

# 20. Offline mode

## 20.1 Offline startup

When no network is available:

```text
Bootstrap
 ↓
read local manifest/state
 ↓
validate current runtime
 ↓
if valid → continue
if invalid → clear repair/error
```

## 20.2 Offline install

The installer must support a future offline package such as:

```text
VESSEL-Offline-Bundle-<version>.zip
```

It should contain everything legally distributable and necessary for the selected configuration.

Mojang-owned data that cannot legally be redistributed must follow the existing VESSEL mapping/data policy and must not be silently bundled against license restrictions.

---

# 21. Failure policy

## 21.1 Manifest fetch failure

Safe runtime exists → continue with warning unless strict security policy says otherwise.

## 21.2 Manifest verification failure

Do not trust the remote update instructions.

Continue offline only if local policy permits and local runtime is valid.

## 21.3 Artifact hash mismatch

Abort installation.

Do not activate the artifact.

Keep diagnostic information.

## 21.4 Partial download

Delete or quarantine only the partial download.

Never overwrite the active runtime with a partial file.

## 21.5 Disk full

Abort before activation when possible.

Retain previous working runtime.

## 21.6 Runtime fails after an update

Bootstrap must detect an update-start failure when practical and expose rollback.

Example:

```text
Previous runtime: 1.0.0
Attempted runtime: 1.0.1
Startup validation: FAILED

Rollback available.

[VESSEL] Restoring last known-good runtime...
```

---

# 22. Logging contract

Every update should have a unique transaction identifier.

Example:

```text
[VESSEL] Update transaction: UPD-20261007-00017
```

Logs must distinguish:

```text
DISCOVERY
DOWNLOAD
VERIFY
STAGE
COMMIT
ROLLBACK
START
```

Detailed diagnostic files should live under:

```text
vessel/logs/
```

Do not expose secrets or authorization tokens in logs.

---

# 23. Source-loader adapter boundary

## 23.1 Adapter registration

Adapters register capabilities rather than directly patching arbitrary Loader Core state.

Conceptual interface:

```text
SourceLoaderAdapter
├── identity()
├── supportedVersions()
├── detect(module)
├── readMetadata(module)
├── resolveDependencies(moduleSet)
├── prepareEnvironment(context)
├── register(context)
├── load(context)
├── start(context)
├── stop(context)
├── transformPlan(context)
├── resourcePlan(context)
└── diagnostics()
```

The exact programming-language API remains an implementation decision, but these responsibilities must remain separate.

## 23.2 Era-aware adapters

Forge and other historical loaders must not be implemented as one giant if/else file where every version changes behavior inline.

Prefer:

```text
ForgeAdapter
 ├── EraRegistry
 ├── E1a
 ├── E1b
 ├── E2
 ├── E3
 ├── E4a
 ├── E4b
 └── E5
```

and version-specific capability maps.

This follows the existing VESSEL research approach, which already models Forge historically by eras and has researched 78 Forge/Minecraft versions. 

---

# 24. Module identity and collision contract

The existing architecture decision remains authoritative:

```text
module identity = (source loader, module id)
```

Therefore:

```text
(forge, worldedit)
(fabric, worldedit)
```

are distinct module identities and both may load.

But:

```text
(forge, worldedit)
(forge, worldedit)
```

is a hard collision.

Likewise, same registry key in the same registry is a hard collision under the VESSEL policy.

The distribution/installer layer must never change this rule.

---

# 25. Startup Build Stage boundary

The existing Startup Build Stage decision remains authoritative.

VESSEL builds needed render/data/resource/Bedrock bundles before world load and port open.

The update system occurs **before** this stage.

Therefore:

```text
System update
 ↓
Runtime load
 ↓
Module discovery/scan
 ↓
Registry completion
 ↓
Startup Build Stage
 ↓
world/network startup
```

The update mechanism must never begin halfway through Startup Build Stage.

---

# 26. First-ever startup interaction

The existing 45-second first-ever warning remains authoritative.

This is a separate policy from update behavior.

Recommended order:

```text
process launch
 ↓
bootstrap update check
 ↓
if first-ever startup and no bypass flag:
    show required warning
    wait 45 seconds
    exit without build
else:
    proceed
```

If the current project decision requires the warning before network work, the dedicated Startup Build Stage file takes precedence and this sequence must be reconciled there before implementation.

The important architectural rule is that the first-run policy is a Bootstrap policy and must not be implemented inside a Forge adapter.

---

# 27. GitHub repository architecture

Recommended organization:

```text
VESSEL/
├── .github/
│   ├── workflows/
│   ├── ISSUE_TEMPLATE/
│   ├── pull_request_template.md
│   └── CODEOWNERS
├── loader/
│   ├── bootstrap/
│   ├── core/
│   ├── api/
│   └── adapters/
├── adapters/
│   ├── forge/
│   ├── fabric/
│   ├── quilt/
│   ├── neoforge/
│   └── plugins/
├── distribution/
│   ├── installer/
│   ├── manifest-schema/
│   └── release-tools/
├── tests/
│   ├── unit/
│   ├── integration/
│   ├── compatibility/
│   └── fixtures/
├── docs/
├── website/
└── tools/
```

The exact folder naming can differ, but dependency direction must remain:

```text
website → distribution metadata
installer → distribution metadata
bootstrap → distribution metadata
bootstrap → runtime
runtime → loader core
loader core → adapters
adapters → source-loader semantics
```

No reverse dependency from Core to Website/GitHub.

---

# 28. Website/data contract

The website should consume the same generated release/compatibility data that the installer understands, but it must not be the authoritative executable logic.

Example:

```text
GitHub Release/tag
      ↓
release generator
      ↓
release-manifest.json
      ├── website
      └── bootstrap/installer
```

This prevents the website from becoming a separately maintained copy of release metadata.

---

# 29. Project website architecture

The full website specification is maintained separately in:

**`VESSEL-PROJECT-WEBSITE-SPEC-v1.0.md`**

That document defines:

- global visual system;
- responsive layout;
- all public pages;
- navigation;
- project home page;
- download/install page;
- compatibility explorer;
- documentation;
- releases/changelog;
- roadmap;
- contributors;
- governance;
- security;
- FAQ;
- legal/disclaimer;
- status page;
- localization architecture;
- accessibility;
- performance;
- SEO/open-graph metadata;
- GitHub integration.

---

# 30. Contributor model

## 30.1 Philosophy

VESSEL should explicitly welcome contributors.

The public message should be:

> Contributions are welcome. Contributors are not promised compensation unless a separate written agreement exists.

No public page may imply employment, guaranteed payment, or a revenue-sharing arrangement unless one actually exists.

## 30.2 Owner approval rule

Every contributor is welcome to submit code.

However:

> **No contribution is merged into the protected production branches without final review and approval by the project owner or an explicitly delegated maintainer.**

This is a governance rule, not a statement about contributor quality.

## 30.3 Pull request flow

```text
Issue / idea
 ↓
Contributor implementation
 ↓
PR
 ↓
CI
 ↓
Automated tests
 ↓
Owner/maintainer review
 ↓
Requested changes (if any)
 ↓
Approval
 ↓
Merge
```

## 30.4 Protected branches

At minimum:

```text
main
release/*
```

Stable releases should originate from reviewed commits/tags.

## 30.5 CODEOWNERS

Loader-sensitive directories should require explicit owner review.

Especially:

```text
loader/core/
adapters/forge/
distribution/
.github/workflows/
```

---

# 31. Compatibility database

The architecture should generate a compatibility index over time.

Concept:

```text
module
source loader
module version
Minecraft version
VESSEL version
status
failure reason
required adapter capability
```

Statuses:

```text
COMPATIBLE
COMPATIBLE_WITH_WARNING
PARTIAL
INCOMPATIBLE
UNKNOWN
NOT_TESTED
```

The compatibility website may display this data, while automated tests continuously update machine-readable records.

---

# 32. Testing strategy for distribution

## 32.1 Bootstrap tests

Test:

- fresh install;
- no update;
- update available;
- failed manifest;
- invalid manifest;
- hash mismatch;
- disk full;
- interrupted download;
- interrupted commit;
- rollback;
- offline startup;
- `--vessel-offline`;
- restart-required exit;
- old bootstrap + new runtime;
- new bootstrap + old runtime rejection if unsupported.

## 32.2 Installer tests

Test across supported OSes where possible:

- Windows;
- Linux;
- macOS where practical.

Test fresh/existing/repair/upgrade/offline cases.

## 32.3 Integration tests

A test fixture should launch:

```text
vessel-server.jar
```

and assert:

1. update check occurred before normal server startup;
2. update completed when required;
3. process exited instead of loading old runtime;
4. second invocation loaded the new runtime;
5. log sequence is correct.

---

# 33. Update UX details

The update experience should look authoritative, not like an unrelated updater.

Example:

```text
────────────────────────────────────────────────────────
 VESSEL  •  Virtualized Ecosystem Shared Server & Engine Loader
────────────────────────────────────────────────────────

 System Update

 Current      1.0.0
 Available    1.0.1
 Channel      Stable

 [VESSEL] Downloading runtime ............... 42%
 [VESSEL] Verifying artifact ................ PASS
 [VESSEL] Installing update ................. PASS

 Update completed successfully.

 A restart is required to load VESSEL 1.0.1.

 VESSEL is shutting down now.
 Start the server again to continue.
────────────────────────────────────────────────────────
```

The CLI must still work when ANSI color is unavailable.

---

# 34. Update frequency and abuse protection

The bootstrap must not hammer GitHub Pages or Releases.

Requirements:

- one manifest request per startup by default;
- HTTP caching headers should be respected;
- local last-check time may suppress duplicate checks during repeated rapid launches if policy allows;
- failed requests use bounded backoff;
- no tight polling loops;
- no update loop after a restart-required exit;
- failed update must not recursively relaunch itself forever.

---

# 35. Future CDN migration

The initial infrastructure is intentionally:

```text
GitHub Pages + GitHub Releases + GitHub Actions
```

But the distribution abstraction must support a future:

```text
VESSEL website
       ↓
CDN / object storage
       ↓
regional mirrors
```

without changing:

- Installer UI;
- Bootstrap state machine;
- Loader Core;
- adapter APIs;
- installation layout.

Only the distribution provider implementation/manifest URLs should change.

---

# 36. No website dependency for runtime

VESSEL server startup must never require the visual website to be online.

The runtime depends on:

```text
Bootstrap
Local manifest/state
Optional remote manifest
Downloaded runtime artifacts
```

It does NOT depend on:

```text
Home page HTML
JavaScript bundles
analytics
images
CSS
```

This is a core reliability boundary.

---

# 37. No analytics requirement

The initial VESSEL website does not require user tracking, cookies or analytics.

Installer/bootstrap should not report individual user telemetry by default.

Download counts may be obtained from release infrastructure without installing a tracking system into the runtime.

Any future telemetry must be explicitly designed, opt-in where appropriate, documented and privacy-reviewed.

---

# 38. Versioning

VESSEL versioning is independent of Minecraft version.

Example:

```text
VESSEL 1.4.0
Minecraft 1.21.11
Forge adapter 1.3.0
```

Do not encode Minecraft version as VESSEL's product version.

The existing decision that VESSEL versions itself independently of Minecraft remains authoritative.

---

# 39. Compatibility warning policy

The existing release policy is retained:

- newer module content than VESSEL knows → warning;
- module calling an unmapped function → error/crash with the reason;
- each VESSEL release updates support knowledge for all loaders it already supports.

This policy applies at runtime and must also be represented in website compatibility UI.

---

# 40. Installer/runtime migration boundaries

The installer may migrate:

- bootstrap layout;
- VESSEL-owned state;
- VESSEL config layout;
- launch scripts generated by VESSEL.

The installer must not automatically rewrite arbitrary user config/data unless a module-specific migration contract explicitly exists.

---

# 41. Future client support

Server mode is the initial priority.

The architecture may support client installation later without changing the core distribution protocol:

```text
Installer
  ├── Server
  └── Client
```

Client-specific loader/runtime behavior must remain behind separate adapter/capability boundaries.

---

# 42. Future external source-loader installation

When VESSEL supports multiple source loaders, the installer may expose:

```text
Install VESSEL
Minecraft 1.21.11

Source ecosystems available:
  ✓ Forge
  ✓ Fabric
  ✓ Quilt
  ✓ NeoForge
  ✓ Bukkit/Plugins
```

But the initial installation should not force every adapter onto the user.

Only required VESSEL-owned components should be downloaded for the selected configuration.

---

# 43. Minimal default installation layout

Recommended final user-visible layout:

```text
MyServer/
├── vessel-server.jar
├── start.bat
├── start.sh
├── eula.txt
├── server.properties
├── world/
├── mods/
├── plugins/
├── config/
├── vessel-config/
└── vessel/
    ├── bootstrap/
    ├── runtime/
    ├── adapters/
    ├── manifests/
    ├── state/
    ├── cache/
    ├── logs/
    └── libraries/
```

User data stays visually familiar. VESSEL-owned state is isolated.

---

# 44. Upgrade scenario example

Starting installation:

```text
vessel-server.jar
VESSEL Runtime 1.0.0
```

User launches:

```text
java -jar vessel-server.jar
```

Bootstrap:

```text
checks manifest
finds 1.0.1
```

It downloads and verifies:

```text
vessel-runtime-1.0.1.jar
```

Then:

```text
install 1.0.1
set current.txt = 1.0.1
log success
exit 0
```

The next launch:

```text
bootstrap reads 1.0.1
no update
starts runtime 1.0.1
```

The old 1.0.0 runtime remains available for rollback until retention policy removes it.

---

# 45. Developer mode

Developers must have a way to bypass the public release backend without modifying production code.

Example concept:

```text
VESSEL_DEV_DISTRIBUTION_ROOT
VESSEL_DEV_MANIFEST
VESSEL_DEV_CHANNEL
```

This must be disabled by default in production distributions.

---

# 46. Release promotion model

```text
commit
 ↓
CI
 ↓
development artifact
 ↓
nightly
 ↓
owner validation
 ↓
beta
 ↓
owner validation
 ↓
stable
```

The same source commit may produce multiple channel artifacts/metadata records, but promotion should preserve traceability to the exact source commit.

---

# 47. Auditability

Every stable artifact must be traceable to:

- source commit;
- release tag;
- CI workflow run;
- tests executed;
- artifact hash;
- owner approval;
- release notes.

The website may expose a simplified version of this information.

---

# 48. Legal and licensing boundary

The project may distribute VESSEL-owned binaries and open-source dependencies according to their licenses.

Minecraft/JARs, mapping files and third-party loader binaries must be handled according to their applicable licenses and the existing VESSEL research decisions.

The installer must distinguish:

```text
VESSEL-owned artifact
Third-party dependency
User-provided artifact
```

and must not silently imply ownership of third-party software.

---

# 49. Implementation order

The next engineering order is fixed:

### Stage A — Finish Forge research

Close all remaining Forge research gaps.

### Stage B — Freeze Forge contract

Turn the research into explicit adapter capability specifications.

### Stage C — Implement Loader Core minimum

Implement the minimum universal runtime required to execute Forge.

### Stage D — Implement Forge Adapter

Implement the complete Forge source-loader boundary.

### Stage E — Make Forge run end-to-end

A real Minecraft server must start through `vessel-server.jar` using Forge-origin modules.

### Stage F — Add distribution system

Implement Bootstrap, manifest, update transaction, installer and GitHub integration around the working Forge path.

### Stage G — Forge gate review

Only after end-to-end validation:

```text
FORGE = 100% / PASS
```

### Stage H — Next loader

Only then begin the next adapter/research phase according to the master plan.

---

# 50. Explicit anti-scope-creep rules

The following are not allowed without an architecture change review:

1. Putting GitHub API logic inside Loader Core.
2. Making Loader Core download its own arbitrary third-party modules.
3. Hot-swapping core runtime classes during a live server.
4. Updating user mods as part of VESSEL self-update.
5. Requiring the public website to be online for server execution.
6. Making Pages host large release binaries.
7. Starting a new loader before the Forge gate passes.
8. Allowing contributors to merge protected runtime changes without owner approval.
9. Rewriting user files without an explicit migration contract.
10. Coupling the manifest schema to GitHub-specific concepts.

---

# 51. Definition of done for this architecture

This supplement is considered implemented only when there is a working proof for all of the following:

```text
[ ] VESSEL server JAR launches through Bootstrap
[ ] Bootstrap reads local install state
[ ] Bootstrap checks remote manifest
[ ] Bootstrap detects update
[ ] Bootstrap downloads update
[ ] Bootstrap verifies update
[ ] Bootstrap installs atomically
[ ] Bootstrap exits cleanly after update
[ ] User restarts server
[ ] New runtime loads
[ ] Offline startup works
[ ] Failed update preserves previous runtime
[ ] Rollback works
[ ] Installer performs fresh install
[ ] Installer performs existing-server install
[ ] GitHub Release artifacts are produced by CI
[ ] Pages hosts website and lightweight metadata only
[ ] Website consumes generated release metadata
[ ] No Loader Core code depends on GitHub
[ ] Forge gate remains enforced
[ ] Owner approval gates stable releases
```

---

# 52. Final architectural principle

VESSEL should feel simple from the outside and be highly modular inside.

The intended final experience is:

```text
Download VESSEL
      ↓
Install VESSEL
      ↓
Run vessel-server.jar
      ↓
VESSEL checks itself
      ↓
VESSEL updates itself if needed
      ↓
[VESSEL] update complete → restart required
      ↓
Run again
      ↓
VESSEL Loader starts
      ↓
Source loaders are adapted underneath
      ↓
Minecraft server runs
```

The website, installer and GitHub infrastructure are replaceable distribution plumbing.

The Loader Core is the permanent technical product.

The Source Loader Adapters are the compatibility boundary.

Forge is the first implementation and a hard gate.

Everything else must grow only after that foundation is proven.

---

# Appendix A — Current-source facts used in this supplement

The current VESSEL status package says:

- VESSEL's source-loader identities are `fabric`, `quilt`, `forge`, `neoforge`, and `plugins`.
- VESSEL versions itself independently of Minecraft.
- VESSEL's intended version scope covers every version of every supported loader from its first release onward.
- Forge is currently the deepest research phase at approximately 91.9% with 31 DONE, 6 PARTIAL and 0 NOT STARTED among its fixed 37 research rows.
- The entire 23-phase VESSEL plan is currently estimated at about 6.0% because most other phases have not been started.
- Existing architecture already defines a Startup Build Stage before world load/port open, a `vessel-config/` folder, per-loader data separation and strict collision policy.
- Existing project policy says to finish one loader fully before moving to the next.

These facts are derived from `MASTER-SPEC-STATUS.md` version 1.0.43 and its referenced package files. The original 625-point Full Master Engineering Specification v3 is not currently included in the package and must remain the authoritative source for points not superseded by the current status/decision record.

---

# Appendix B — Implementation-AI instructions

When an AI is given this file:

1. Read the existing `MASTER-SPEC-STATUS.md` first.
2. Read `LOG.md` next.
3. Read the relevant architecture/research files.
4. Treat this supplement as the distribution/runtime boundary specification.
5. Do not mark a feature DONE because a stub exists.
6. Do not begin the next loader until Forge passes the hard gate.
7. When a requirement conflicts with an older document, follow the newest explicit decision/status rule and record the reconciliation.
8. Never invent missing evidence.
9. Keep all project implementation text in English, while communication with the project owner may remain Vietnamese.
10. Every architecture-affecting change must be recorded in the status/change log.

**End of specification.**
