# Draft specifications (not authoritative)

**Document version:** 0.1.1
**Date:** 2026-10-09

This folder holds two specifications that Shiroz wrote and uploaded on 2026-10-07. They are kept here **unchanged**, as the starting point for the next stage of work. Shiroz said that they are only early drafts, not the versions that will be applied: they are to be consulted where useful and **upgraded** (see `handoff/NEW-CHAT-PROMPT-STAGE-2.md`, sections 4 and 5).

| File | Lines | Contents | Status |
|---|---|---|---|
| `VESSEL-DISTRIBUTION-AND-LOADER-ARCHITECTURE-SPEC-v1_0.md` | 2,183 | Distribution, bootstrap, installer, update transaction, manifest, GitHub Pages/Releases/Actions, security, loader core and adapter boundary; 52 sections and 2 appendices | Draft (version 1.0 in the file name) |
| `VESSEL-PROJECT-WEBSITE-SPEC-v1_0.md` | 1,934 | Public website and web experience specification; 72 numbered sections; the file calls itself a "design contract for implementation" | Draft (Shiroz treats it as a draft) |

Known tension with this package (to be resolved in the next stage): the loader draft's "Forge gate" (sections 2.1 to 2.3 and 49) counts the Forge adapter, runtime and end-to-end tests inside "Forge = 100%", while the order of work that Shiroz fixed on 2026-10-07 finishes Forge research first and then studies Sinytra Connector, the website and the other loaders. The status file records the research side only: the Forge research is 100% complete. **[Resolved in principle on 2026-10-09 (README v0.1.1): decision 36 of `MASTER-SPEC-STATUS.md` splits the gate into a research gate and an implementation gate for every loader (section 3.1 of the status file). The draft text itself stays unchanged here and is upgraded in a later pass.]**

These files are not part of the research status counts and are not listed in the status file's counts.
