# VESSEL Third-Party Open-Source Credits & Attributions

VESSEL integrates and auto-provisions the latest official releases of **GeyserMC** and **spark** to provide out-of-the-box Bedrock Edition crossplay and low-overhead server performance profiling on the latest official **Minecraft (`latest.release`)**. Full credit and gratitude belong to their respective authors and contributors:

---

## 1. GeyserMC (`Geyser` & `Floodgate`)
- **Integrated Channel:** Latest Official Release (`https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest`)
- **Authors:** GeyserMC Team & Contributors
- **License:** MIT License
- **Official Website & Repository:** [https://geysermc.org](https://geysermc.org) | [https://github.com/GeyserMC/Geyser](https://github.com/GeyserMC/Geyser)
- **Role in VESSEL:** Enables seamless Minecraft: Bedrock Edition client connections to VESSEL Java Edition servers with protocol translation, Xbox Live / Floodgate authentication, and automatic custom block/item resource pack mapping.

---

## 2. spark (`lucko/spark` Performance Profiler)
- **Integrated Channel:** Latest Official Release (`https://api.modrinth.com/v2/project/spark/version`)
- **Authors:** Lucko (`lucko`) & Contributors
- **License:** GPL-3.0 (Standalone Profiler Agent/Plugin Integration)
- **Official Website & Repository:** [https://spark.lucko.me](https://spark.lucko.me) | [https://github.com/lucko/spark](https://github.com/lucko/spark)
- **Role in VESSEL:** Provides production-ready async CPU sampling, MSPT tick duration histograms, GC pressure monitoring, and heap summary diagnostics alongside VESSEL's internal microkernel telemetry.

---

## 3. Minecraft Dedicated Server (`Mojang Studios`)
- **Target Default Release:** `latest.release` via Mojang Piston Meta API (`https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`)
- **Copyright:** Copyright (c) Mojang AB / Microsoft Corporation. Do not distribute proprietary Mojang assets outside official EULA terms (`eula=true`).
