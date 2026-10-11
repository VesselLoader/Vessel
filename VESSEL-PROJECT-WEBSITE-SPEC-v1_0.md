# VESSEL — Project Website & Web Experience Specification

**Specification type:** Complete public website specification
**Project:** VESSEL — **Virtualized Ecosystem Shared Server & Engine Loader**
**Version:** 1.0.0
**Date:** 2026-10-07
**Status:** Design contract for implementation
**Hosting target:** GitHub Pages initially
**Binary distribution:** GitHub Releases initially
**Primary design goal:** Make VESSEL look and feel like a serious, modern, ambitious open-source infrastructure project rather than a plain GitHub README wrapped in HTML.

---

# 0. Design brief

VESSEL's website must communicate three things within the first few seconds:

1. **This is a real Minecraft loader/runtime project.**
2. **The technology is unusually ambitious.**
3. **Anyone interested can use it, inspect it, test it and contribute.**

The site should feel contemporary, cinematic and technical without becoming a generic gaming landing page.

The UI references provided by the project owner are used for principles only, not copied visually or textually:

- `https://spdmteam.com/index` — strong app-like presentation and impactful hero treatment.
- `https://khoindvn.io.vn/` — dense but navigable service/link structure, language selector, clear sections and a strong personal/project identity.

The final VESSEL site must be its own visual identity.

---

# 1. Brand identity

## 1.1 Full name

**VESSEL**

**Virtualized Ecosystem Shared Server & Engine Loader**

## 1.2 Brand promise

Suggested concept:

> One runtime. Many ecosystems. One place to run them.

Alternative supporting language may vary by page, but the core message should emphasize **unification** rather than simply "compatibility".

## 1.3 Visual personality

The visual language should be:

```text
modern
technical
premium
slightly futuristic
Minecraft-aware
open-source
confident
clean
```

It must not feel:

```text
generic Bootstrap template
corporate SaaS dashboard
plain documentation portal
Minecraft-server store
crypto/AI hype site
```

---

# 2. Site-wide visual system

## 2.1 Layout

Use a responsive content grid with a strong central maximum width.

Desktop:

```text
┌───────────────────────────────────────────────────────────┐
│ logo   navigation                 GitHub   Download       │
├───────────────────────────────────────────────────────────┤
│                                                           │
│                    HERO / VISUAL                          │
│                                                           │
├───────────────────────────────────────────────────────────┤
│ feature cards                                             │
├───────────────────────────────────────────────────────────┤
│ technical sections                                        │
├───────────────────────────────────────────────────────────┤
│ contributors / community                                  │
├───────────────────────────────────────────────────────────┤
│ footer                                                    │
└───────────────────────────────────────────────────────────┘
```

Mobile navigation becomes a compact drawer/menu without losing the main Download action.

## 2.2 Corners

Use a contemporary rounded visual language.

Recommended:

- cards: medium-large radius;
- buttons: pill or rounded rectangle;
- inputs: rounded;
- hero containers: large radius;
- avoid raw square boxes except where a technical/code aesthetic is intentional.

## 2.3 Depth

Use depth sparingly:

- soft shadows;
- layered translucent panels;
- subtle glow around primary actions;
- low-noise gradients;
- animated background particles/lines only where they support the technology aesthetic.

## 2.4 Typography

Use a modern sans-serif for general text and a monospace face for code/version/technical metadata.

The typography hierarchy must be dramatic:

```text
H1 → very large
H2 → strong section marker
H3 → concise feature title
body → comfortable line length
metadata → small and technical
```

## 2.5 Color architecture

Do not lock the exact palette too early.

Use semantic tokens:

```text
--bg-primary
--bg-secondary
--surface
--surface-strong
--text-primary
--text-secondary
--accent
--accent-strong
--success
--warning
--danger
--border
```

The accent should visually connect with VESSEL's identity and Minecraft's technical aesthetic, but should not look like a default Mojang palette.

## 2.6 Animation principles

Animation must feel premium, not distracting.

Allowed:

- soft hero reveal;
- card hover lift;
- background parallax;
- subtle gradient movement;
- version/status count-up;
- scroll reveal;
- tab transitions;
- compatibility filtering transitions.

Do not:

- animate every element;
- use flashing effects;
- block content behind splash animations;
- require high-end GPUs to read the site.

Respect reduced-motion preferences.

---

# 3. Global navigation

Desktop navigation:

```text
VESSEL

Overview
Download
Compatibility
Docs
Releases
Roadmap
Contribute

[ GitHub ] [ Download ]
```

Optional secondary links:

```text
Discord
Issues
Discussions
Security
```

The Download button remains the visually strongest navigation action.

## 3.1 Sticky header

The header should become slightly more compact after scrolling.

It may use:

- translucent background;
- backdrop blur where supported;
- thin border;
- compact logo lockup.

Do not make it visually heavy.

---

# 4. Website route map

The public site should include these primary pages/routes:

```text
/
/download
/compatibility
/docs
/docs/getting-started
/docs/architecture
/docs/configuration
/releases
/roadmap
/contribute
/community
/security
/faq
/about
/status
```

Legal/supporting pages:

```text
/privacy
/terms
/licenses
/credits
```

Some routes may render as generated/static pages instead of a fully dynamic application, but each route is treated as an independent design/content contract below.

---

# 5. HOME — root project page

## 5.1 Objective

The root page is the project's flagship presentation.

It should make someone who has never heard of VESSEL understand the idea without reading a technical specification.

## 5.2 Hero

Hero composition:

```text
small eyebrow
VESSEL
large headline
supporting paragraph
primary CTA
secondary CTA
compatibility/status strip
large visual
```

Suggested headline direction:

> **Minecraft, without the loader walls.**

Supporting copy direction:

> VESSEL is a universal server and engine loader designed to bring multiple Minecraft ecosystems into one runtime.

Do not claim full universal compatibility before that is actually true. Use language such as "designed to", "working toward", or show exact compatibility status.

Primary button:

**Download VESSEL**

Secondary:

**Explore the architecture**

Tertiary link:

**View source on GitHub**

## 5.3 Hero visual

The visual should be a high-quality technical illustration, not a stock photo.

Concept:

```text
        FABRIC ─────┐
        FORGE ──────┤
        QUILT ──────┼──→ VESSEL CORE → MINECRAFT
        NEOFORGE ───┤
        PLUGINS ────┘
```

Visually, show multiple colored/animated streams entering a central VESSEL symbol and emerging as one stable runtime pipeline.

A future implementation may use a generated SVG/WebGL/CSS animation, but the visual should still work as a static screenshot.

## 5.4 Trust strip

Immediately after the hero:

```text
Open Source
GitHub
Community Contributions
Versioned Releases
Compatibility Research
```

Use actual data, not invented statistics.

## 5.5 Core feature cards

Recommended initial cards:

### One runtime
Multiple source ecosystems are handled underneath a unified VESSEL runtime.

### Independent versioning
VESSEL has its own version lifecycle instead of pretending to be a Minecraft version.

### Built for compatibility
The architecture explicitly separates source-loader behavior from the universal core.

### Self-updating
The official server entrypoint can verify and update VESSEL-owned runtime components before startup.

### Open to contributors
Contributions are welcome and reviewed before merge.

### Deep compatibility work
The project studies loader behavior across historical eras rather than only recent API snapshots.

## 5.6 Architecture section

Use a large visual pipeline:

```text
Installer
   ↓
Bootstrap
   ↓
VESSEL Loader Core
   ↓
Source Loader Adapters
   ↓
Minecraft Server
```

Each node should be clickable.

## 5.7 "How it works" section

Three/four large numbered steps:

```text
01 Install
02 Launch
03 VESSEL checks itself
04 Runtime starts
```

Add a small animated console preview showing the update process.

## 5.8 Forge-first progress section

Important honesty requirement:

Display current progress as an actual project status, not marketing fluff.

Example layout:

```text
Current foundation
Forge research
██████████████████░░ 91.9%

Next gate
Forge → 100%

After the gate
Next source loader
```

The exact number must be generated from the canonical status file rather than manually typed into the website.

## 5.9 Compatibility statement

Do not say:

> "All mods work."

Instead:

> Compatibility is measured, versioned and documented. Check the compatibility explorer for the exact module/version result.

## 5.10 Community section

Headline:

> **Build the runtime with us.**

Copy:

> VESSEL is open to contributors across research, code, testing, documentation and tooling. Every change still goes through project-owner review before it reaches the protected production branches.

CTA:

**Read the contributor guide**

## 5.11 Footer preview

Root page ends with a strong footer:

```text
VESSEL
Virtualized Ecosystem Shared Server & Engine Loader

Overview • Download • Compatibility • Docs • Releases • Contribute

GitHub • Community • Security

Open source. Built in public.
```

---

# 6. DOWNLOAD PAGE

## 6.1 Objective

The page must feel like a professional loader download center.

## 6.2 Top area

```text
Download VESSEL

Choose how you want to install it.

[ Server ] [ Client – future ] [ Tools ]
```

## 6.3 Version selector

Controls:

```text
VESSEL channel
[ Stable ▼ ]

Minecraft version
[ 1.21.11 ▼ ]

Installation type
[ Existing server ▼ ]
```

Show resolved compatibility underneath.

## 6.4 Download cards

Primary:

```text
VESSEL Installer
Latest Stable
[ Download Installer ]
SHA-256: …
```

Advanced:

```text
Server Bootstrap
Runtime Package
Offline Bundle
Checksums
```

## 6.5 Install instructions

Tabs:

```text
Fresh server
Existing server
Command line
Offline
```

## 6.6 Update explanation

A prominent visual:

```text
You install once.
VESSEL keeps its own runtime current.
```

Then explain the update/restart behavior honestly.

---

# 7. COMPATIBILITY EXPLORER

## 7.1 Objective

This is one of the most important technical pages and should become a major community asset.

## 7.2 Filters

```text
Source loader
Minecraft version
Module type
VESSEL version
Compatibility status
Search
```

## 7.3 Status visualization

Use compact cards/table:

```text
Mod / Plugin
Source
Version
Minecraft
VESSEL
Status
Reason
```

Status chips:

```text
✓ Compatible
△ Warning
◐ Partial
× Incompatible
? Unknown
```

## 7.4 Detail page

Each module may have:

- compatibility timeline;
- tested versions;
- detected dependencies;
- known issues;
- last test timestamp;
- relevant issue links;
- required VESSEL capability.

## 7.5 Honesty rule

Never fabricate a compatibility percentage from incomplete data.

Unknown is a valid status.

---

# 8. DOCS LANDING

## 8.1 Objective

The documentation portal should be attractive but functional.

Layout:

```text
Docs
├── Getting Started
├── Installation
├── Configuration
├── Architecture
├── Loader Adapters
├── Compatibility
├── Troubleshooting
├── Development
└── Reference
```

## 8.2 Search

Search should be prominent on desktop and mobile.

## 8.3 Version selector

Because VESSEL is versioned independently, docs must support selecting a VESSEL version.

---

# 9. GETTING STARTED PAGE

Required sections:

```text
What is VESSEL?
Requirements
Install
Run
First startup
Auto-update behavior
Adding modules
Checking compatibility
Troubleshooting
```

A first-time user should reach a running server with as little reading as possible.

---

# 10. ARCHITECTURE DOCS

This page should visually explain:

```text
Bootstrap
Runtime
Loader Core
Adapters
Build Stage
Virtual Filesystem
Module identity
Registry rules
Transformers
```

Use expandable diagrams for complex internals.

Link directly to the detailed engineering specifications where appropriate.

---

# 11. CONFIGURATION DOCS

The docs should show:

```text
vessel-config/
```

and explain which settings are:

- Bootstrap settings;
- distribution settings;
- Loader Core settings;
- adapter-specific settings.

Avoid mixing source-loader-specific configuration into generic VESSEL documentation.

---

# 12. RELEASES PAGE

## 12.1 Objective

A serious release history.

## 12.2 Layout

```text
Releases

Stable
1.0.0
Oct 2026

Beta
...

Nightly
...
```

Each release card:

- version;
- channel;
- Minecraft compatibility;
- key changes;
- compatibility notes;
- download CTA;
- source commit;
- checksum link.

## 12.3 Release detail

Use a split layout:

```text
Release info | Download
              Files
              Checksums
              Compatibility
```

---

# 13. ROADMAP PAGE

The roadmap must show the project's ambition while remaining honest about status.

Suggested visualization:

```text
01 Forge foundation          CURRENT
02 Loader Core               CURRENT/NEXT
03 Forge 100% gate           REQUIRED
04 Next source loader        LOCKED
05 Universal render/data     FUTURE
06 Legacy migration          FUTURE
07 Bedrock integration       FUTURE
```

Every roadmap item should link to its status/source when practical.

Do not display fictional delivery dates.

---

# 14. CONTRIBUTE PAGE

## 14.1 Main message

> **You don't need to be a core maintainer to help build VESSEL.**

## 14.2 Contribution areas

Cards:

```text
Code
Research
Compatibility Testing
Documentation
Website
Tooling
Bug Reports
```

## 14.3 Governance statement

Clearly state:

> Contributions are welcome. Submission does not guarantee compensation, employment, contract work or payment. Protected production changes require project-owner approval before merge.

## 14.4 PR workflow visual

```text
Fork → Branch → Implement → CI → Review → Changes → Approval → Merge
```

## 14.5 Good first issue

Surface a generated list of beginner-friendly tasks from GitHub labels.

---

# 15. COMMUNITY PAGE

The page may aggregate:

- GitHub Discussions;
- issues;
- Discord/community chat if available;
- contributor recognition;
- announcements.

Do not require an account on the website itself.

---

# 16. SECURITY PAGE

Sections:

```text
Security policy
Report a vulnerability
Update integrity
Artifact verification
Supported release channels
Past advisories
```

Never publish sensitive vulnerability details before coordinated disclosure.

The website should link to the repository's official security mechanism where applicable.

---

# 17. FAQ PAGE

Required questions:

### What is VESSEL?

Universal server/engine loader project.

### Is VESSEL a Forge fork?

No. Forge is the first deep compatibility target; VESSEL has a separate core/runtime architecture.

### Does VESSEL replace Minecraft?

No. It acts as the loader/runtime around the Minecraft server environment.

### Will every mod work?

No automatic promise. Compatibility is version- and behavior-specific and is tested/documented.

### Does VESSEL update every mod?

No. Core self-update covers VESSEL-owned components by default.

### Why does the server restart after an update?

Because the running JVM must not hot-swap VESSEL's own loaded runtime classes.

### Why GitHub Pages?

It provides a free static site for the public project while GitHub Releases handles binaries.

### Can I contribute?

Yes. All useful contributions are welcome, subject to project review and approval.

### Is contributor payment guaranteed?

No. The project does not promise compensation unless separately agreed in writing.

---

# 18. STATUS PAGE

## 18.1 Objective

Provide real project transparency without pretending the roadmap is more complete than it is.

## 18.2 Main cards

```text
Overall Project
Forge
Fabric
NeoForge
Quilt
Bukkit
Infrastructure
Website
```

The first release may show:

```text
Forge research: 91.9%
Overall roadmap: ~6.0%
```

when those values are still current in the canonical status source.

## 18.3 Update source

Status values must be generated from the canonical status document/data whenever practical.

---

# 19. ABOUT PAGE

Keep it personal but technical.

Sections:

```text
Why VESSEL exists
The problem
The approach
The current stage
The long-term vision
Project owner
```

Avoid exaggerated claims like "the first universal Minecraft loader ever" unless independently verified.

---

# 20. PRIVACY PAGE

Initial policy can be minimal because the site should not require analytics or personal tracking.

Explain:

- no account required;
- no personal data required for downloads;
- GitHub itself may process requests under its own policies;
- external community links may have their own policies.

---

# 21. TERMS PAGE

Explain acceptable use, limitations and project-distribution boundaries.

Do not present the site as an employment or commercial service.

---

# 22. LICENSES / CREDITS PAGE

List:

- VESSEL license;
- third-party libraries;
- fonts/icons if used;
- Minecraft attribution where required;
- external project references;
- UI inspirations/reference acknowledgements where legally and socially appropriate.

Never imply endorsement by Minecraft, Mojang or loader projects unless such endorsement exists.

---

# 23. GLOBAL FOOTER

Every page:

```text
VESSEL
Virtualized Ecosystem Shared Server & Engine Loader

Explore
Overview
Download
Compatibility
Docs
Releases
Roadmap

Community
Contribute
GitHub
Discussions

Project
Security
FAQ
Licenses
Privacy
Terms

© VESSEL Project
Open source, built in public.
```

Keep the footer visually rich but not gigantic.

---

# 24. Download CTA system

Every high-intent page should contain one clearly identifiable download action.

CTA variants:

```text
Download VESSEL
Install for Server
Get Latest Stable
View Releases
```

Never hide the installer behind multiple nested menus.

---

# 25. GitHub integration

The site may consume public GitHub data for:

- release version;
- open issues;
- contributor count;
- latest release date;
- roadmap issues;
- discussions.

For critical product data, prefer generated static data over client-side API calls so the website remains functional during GitHub API rate limits.

---

# 26. Generated data strategy

A GitHub Action may generate:

```text
/data/releases.json
/data/compatibility.json
/data/status.json
/data/contributors.json
```

The frontend uses those files as its primary public data source.

That produces a static site that does not need a backend server.

---

# 27. Localization

English is the canonical project language.

The current project documents explicitly use English for configs, comments, docs and logs. The website should therefore use English as the canonical source language.

Optional future locales may include Vietnamese first, followed by other community-supported languages.

Do not make translations overwrite the English source.

Recommended:

```text
/en/
/vi/
```

Use translation files rather than duplicating markup when possible.

---

# 28. Responsive design

The site must work at minimum on:

```text
360px mobile
768px tablet
1024px laptop
1440px desktop
1920px wide desktop
```

Critical buttons must remain reachable without horizontal scrolling.

Compatibility tables on mobile should convert to cards or horizontally scroll within an isolated container.

---

# 29. Accessibility

Required:

- semantic HTML;
- visible keyboard focus;
- color is never the only status indicator;
- sufficient contrast;
- reduced-motion support;
- alt text for meaningful imagery;
- accessible navigation drawer;
- form labels;
- accessible release/download controls.

---

# 30. Performance

The website must remain fast despite the premium visual design.

Rules:

- compress images;
- use SVG for icons/diagrams where appropriate;
- avoid shipping giant background videos;
- lazy-load non-critical media;
- do not preload everything;
- split non-critical JavaScript;
- keep compatibility data paginated/filterable;
- keep Pages deployment comfortably below the documented 1 GB site ceiling.

The website itself should consume a tiny fraction of GitHub Pages' current 100 GB/month soft bandwidth budget; large files belong on Releases/CDN.

---

# 31. SEO / social previews

Each major page needs:

```text
<title>
<meta description>
canonical URL
Open Graph title
Open Graph description
Open Graph image
Twitter/X card metadata
```

Home OG image should be a premium VESSEL branded composition.

## 31.1 Suggested home title

> VESSEL — Virtualized Ecosystem Shared Server & Engine Loader

## 31.2 Suggested home description

> A universal Minecraft server and engine loader designed to bring multiple loader ecosystems into one runtime.

---

# 32. Visual storytelling system

The website should repeatedly use one visual metaphor:

```text
Many ecosystems
       ↓
   VESSEL CORE
       ↓
One runtime
```

This can appear as:

- hero animation;
- architecture diagrams;
- compatibility visualization;
- installer flow;
- update console animation;
- roadmap progression.

Consistency makes the brand memorable.

---

# 33. Console simulation component

Create a reusable browser component resembling a real VESSEL console.

Example states:

```text
Checking for system updates...
Update available.
Downloading...
Verifying...
Installing...
Restart required.
Starting VESSEL Loader...
Forge adapter loaded.
Server ready.
```

This component should use real project text from the specification and must never fabricate success for unsupported features.

---

# 34. Interactive architecture component

Create a diagram where clicking:

```text
Bootstrap
Loader Core
Forge Adapter
Runtime
Build Stage
```

opens a short explanation panel.

Desktop can use hover/click.

Mobile uses tap.

The diagram must still render as plain information when JavaScript is unavailable.

---

# 35. Project progress visualization

Instead of generic progress bars everywhere, use a mixture of:

- phase cards;
- vertical roadmap timeline;
- status chips;
- numerical research progress;
- compatibility test count when real data exists.

Never invent usage/download numbers.

---

# 36. Contribution visual identity

Contributor page should celebrate people without suggesting paid employment.

Example:

```text
Built by one owner.
Improved by everyone who contributes.

Code      Research      Testing      Docs      Design
```

Then display contributor names/avatars from verified Git history where permitted.

---

# 37. Release-state visual identity

Stable releases:

```text
STABLE
```

Beta:

```text
BETA
```

Nightly:

```text
NIGHTLY
```

Use distinct semantic indicators while preserving the core brand.

---

# 38. No fake enterprise language

Avoid phrases such as:

- enterprise-grade;
- guaranteed compatibility;
- zero configuration for every case;
- production ready everywhere;
- 100% compatible;
- officially supported by Mojang.

The website's strength should come from technical transparency.

---

# 39. No fake metrics

Do not invent:

- download counts;
- number of supported mods;
- contributor counts;
- test pass rates;
- uptime;
- user counts.

Only display values derived from actual project data.

---

# 40. GitHub Pages constraints reflected in website design

The website is intentionally static-first because GitHub currently limits published Pages sites to 1 GB and applies a soft 100 GB/month bandwidth limit. GitHub also documents rate limiting and deployment timeout constraints.

Therefore:

```text
HTML/CSS/JS/static JSON/images
        → GitHub Pages

JAR/ZIP/binary assets
        → GitHub Releases
```

This separation is a foundational design rule.

---

# 41. Page loading states

Every data-driven page needs:

```text
Loading
Loaded
Empty
Error
Offline/stale
```

For example:

```text
Compatibility data unavailable.
Showing the latest cached dataset.
```

Do not leave blank screens.

---

# 42. Error pages

Create branded:

```text
404
500/static failure fallback
Data unavailable
Release unavailable
```

Example 404 concept:

> This route fell outside the vessel's current chart.

CTA:

**Return home**

---

# 43. Announcement banner system

Optional top-of-site banner for:

- new stable release;
- major compatibility milestone;
- security advisory;
- contributor event.

It should be dismissible and not intrusive.

---

# 44. Search architecture

The docs search should eventually index:

- page titles;
- section titles;
- code names;
- loader names;
- Minecraft versions;
- common errors.

Search results should show breadcrumbs and a relevant excerpt.

---

# 45. Code presentation

Use syntax-highlighted code blocks with:

- language label;
- copy button;
- line wrap toggle where useful;
- horizontal scroll fallback.

Avoid enormous dense blocks on marketing pages.

---

# 46. Mobile navigation design

Mobile header:

```text
VESSEL       ☰
```

Open drawer:

```text
Overview
Download
Compatibility
Docs
Releases
Roadmap
Contribute

GitHub
```

The Download CTA should remain prominent inside the drawer.

---

# 47. Home page section order

Recommended final root page order:

```text
1. Header
2. Hero
3. Trust/identity strip
4. Why VESSEL exists
5. Architecture visualization
6. Key capabilities
7. Live/update console demo
8. Forge-first progress/status
9. Compatibility explorer preview
10. Roadmap
11. Contributor invitation
12. GitHub/source CTA
13. Footer
```

This is the default unless usability testing proves otherwise.

---

# 48. Download page section order

```text
1. Header
2. Version/channel selector
3. Primary installer card
4. Advanced downloads
5. Installation steps
6. Auto-update explanation
7. Integrity/checksum explanation
8. Offline installation
9. FAQ
10. Footer
```

---

# 49. Compatibility page section order

```text
1. Header
2. Search + filters
3. Summary
4. Results table/cards
5. Selected-module detail
6. Compatibility timeline
7. Known issues
8. Contribute a test result
9. Footer
```

---

# 50. Contributor page section order

```text
1. Hero
2. Why contribute
3. Areas of contribution
4. PR workflow
5. Owner-approval rule
6. Code standards
7. Research workflow
8. Current good-first tasks
9. Community links
10. Footer
```

---

# 51. Architecture page section order

```text
1. Loader overview
2. Bootstrap boundary
3. Loader Core
4. Source Loader Adapters
5. Module identity
6. Virtual filesystem
7. Startup Build Stage
8. Rendering/data compilation
9. Update/distribution architecture
10. Future expansion
```

---

# 52. Status page data model

Suggested generated object:

```json
{
  "generated_at": "...",
  "project": {
    "overall_estimate": 6.0
  },
  "gates": {
    "forge": {
      "research": 91.9,
      "adapter": "pending",
      "runtime_e2e": "pending",
      "gate": "locked"
    }
  }
}
```

Never hand-edit the live progress values if a canonical source can generate them.

---

# 53. GitHub repository links

All primary links should use stable repository-relative configuration.

Central configuration should define:

```text
repo URL
issues URL
discussions URL
releases URL
docs URL
security URL
```

No hardcoded duplicate URLs across dozens of components.

---

# 54. No backend requirement

Version 1 of the website must not require:

- VPS;
- database;
- API server;
- PHP hosting;
- Node server in production;
- persistent worker.

Build-time generation is preferred.

---

# 55. Future custom domain

The site should be deployable first at:

```text
<github-user-or-org>.github.io/<project>/
```

and later at:

```text
vessel.<custom-domain>
```

without changing internal routes.

GitHub Pages supports custom domains, including subdomains and apex domains. Domain verification should be configured before DNS to reduce takeover risk. [Research source: GitHub custom-domain documentation, checked 2026-10-07.]

---

# 56. Website deployment pipeline

```text
content/source change
 ↓
GitHub Actions
 ↓
lint/build
 ↓
link validation
 ↓
accessibility checks
 ↓
asset size checks
 ↓
status/release data generation
 ↓
static site build
 ↓
Pages deployment
```

Do not make a deployment fail because GitHub's release API is temporarily unavailable if the last generated release data remains valid.

---

# 57. Asset policy

Keep large visual media optimized.

Preferred:

```text
SVG → icons/logos/diagrams
WebP/AVIF → raster images
PNG → transparency/technical screenshots only when necessary
```

Do not package binary releases into the website's `public/` directory.

---

# 58. Logo system

Required logo variants:

```text
Primary lockup
VESSEL wordmark
Icon-only
Monochrome
Dark background
Light background
Favicon
Social/OG image
```

The icon should work at very small sizes and should not depend on tiny text.

---

# 59. Hero artwork direction

A premium illustration should communicate:

```text
multiple loader streams
        ↓
central VESSEL core
        ↓
stable Minecraft runtime
```

Avoid a literal screenshot collage of Forge/Fabric logos as the main hero.

Logos may appear later in factual ecosystem sections where trademark attribution is appropriate.

---

# 60. Tone of voice

Use:

```text
confident
technical
honest
forward-looking
welcoming
```

Not:

```text
aggressive
corporate
overhyped
childish
fake-official
```

Example:

> Designed to unify Minecraft loader ecosystems under one runtime.

Better than:

> THE ULTIMATE 100% COMPATIBLE LOADER THAT RUNS EVERYTHING!!!

---

# 61. Contributor acknowledgment

The site should include a contributor wall/list generated from Git history when possible.

Categories may include:

```text
Core
Research
Compatibility
Docs
Website
Tooling
```

Do not rank people by perceived importance unless the project governance explicitly defines such roles.

---

# 62. Project owner visibility

Because VESSEL is currently maintained by one project owner, the site may say so clearly.

Example:

> VESSEL is currently led by a single project owner and maintained openly in collaboration with community contributors.

This should not make the project look amateur; it should communicate where final architectural authority resides.

---

# 63. Governance page / section

Future page or subsection should define:

- project owner;
- maintainers/delegates;
- protected branches;
- code review requirements;
- release authority;
- security authority;
- contributor expectations;
- behavior standards.

The key rule remains:

**All contributors are welcome; production code still requires owner/maintainer approval.**

---

# 64. No compensation promises

The website may say:

> VESSEL is open to contributors. Contributions are voluntary unless separately agreed. The project does not promise payment or employment for contributions.

Do not use ambiguous language such as "join the team" if it could reasonably imply employment.

---

# 65. Project credibility system

Credibility should come from:

- public source;
- visible releases;
- reproducible build information;
- transparent compatibility data;
- real roadmap status;
- documented architecture;
- active issue tracking;
- clear contributor policy.

The website should not rely on fake testimonials, fake user counts or fabricated download numbers.

---

# 66. Integration with the auto-update system

The Download page should explain:

```text
You install VESSEL once.

Every normal server launch:
1. VESSEL checks its system manifest.
2. If a VESSEL update exists, it downloads and verifies it.
3. VESSEL installs the update safely.
4. VESSEL exits.
5. You launch the server again.
6. The updated runtime starts.
```

This should be displayed with a polished console animation.

---

# 67. Compatibility status honesty

The site must mirror the master status language:

```text
DONE
PARTIAL
NOT STARTED
```

when showing development work.

For user compatibility results, use:

```text
Compatible
Compatible with warning
Partial
Incompatible
Unknown
Not tested
```

Do not blur these two concepts.

---

# 68. Documentation source integrity

If a page describes a technical behavior, link to:

- VESSEL architecture spec;
- relevant loader research;
- source reference;
- test result.

Where a behavior is still an architectural decision rather than implemented functionality, label it as planned/specification.

---

# 69. Browser support

Target current evergreen desktop/mobile browsers.

Graceful degradation should preserve:

- navigation;
- release downloads;
- docs reading;
- compatibility table basics.

---

# 70. Final page experience test

Before the website is considered complete, test a new visitor who knows nothing about VESSEL.

Within approximately 30 seconds they should be able to answer:

```text
What is VESSEL?
Why does it exist?
Can I download it?
What works today?
How do I contribute?
Where is the source?
```

If the user cannot answer those six questions, the home page is not ready.

---

# 71. Final visual bar

The website should feel closer to:

```text
modern developer infrastructure product
        +
Minecraft technical ecosystem
        +
open-source project transparency
```

than to:

```text
plain documentation
```

The project should look ambitious enough that a visitor immediately understands why a universal Minecraft runtime is worth building.

---

# Appendix A — Current infrastructure facts used in this website spec

GitHub currently documents that GitHub Pages sites may be no larger than 1 GB and have a soft bandwidth limit of 100 GB/month, with rate limits and deployment-time constraints; GitHub recommends using other GitHub features such as Releases or a CDN when a Pages site grows beyond appropriate usage.

GitHub Releases currently allow up to 1,000 assets per release, with each release file under 2 GiB and no total release-size or bandwidth-usage limit documented in the Releases page.

GitHub Pages supports custom domains, so the initial `github.io` site can later move to a project-owned domain without changing the underlying information architecture.

The site is therefore intentionally designed around:

```text
GitHub Pages → static website + small generated metadata
GitHub Releases → installers + runtimes + binary packages
GitHub Actions → build/test/generate/deploy
```

---

# Appendix B — Implementation-AI instructions

1. Build the site from this specification; do not clone the UI of the reference websites.
2. Keep all marketing claims tied to real VESSEL status.
3. Use generated data for release/status values when practical.
4. Do not ship large binary payloads through Pages.
5. Keep the website backend-free in the initial release.
6. Preserve a future custom-domain migration path.
7. Keep Download prominent but never deceptive.
8. Every route must have a mobile layout.
9. Keep animations subtle and accessible.
10. Never invent project metrics.
11. Contributions are welcome, but protected production branches require owner/maintainer approval.
12. Do not imply contributor compensation unless an explicit agreement exists.

**End of specification.**
