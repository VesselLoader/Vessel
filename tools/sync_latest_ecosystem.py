#!/usr/bin/env python3
"""
VESSEL Ecosystem Auto-Sync Tool (v1.0.0)
Automatically queries the latest official releases of:
1. Minecraft Dedicated Server (Mojang Piston Meta API `latest.release`)
2. GeyserMC (Geyser Download API v2 `versions/latest/builds/latest`)
3. Spark Profiler (Modrinth API v2 `project/spark/version`)

Updates `docs/vessel-update-manifest.json` whenever VESSEL's source code is updated.
"""

import json
import pathlib
import urllib.request

MOJANG_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
GEYSER_LATEST_URL = "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest"
SPARK_LATEST_URL = "https://api.modrinth.com/v2/project/spark/version"

DEFAULT_MC = "1.21.4"
DEFAULT_GEYSER = "2.6.1-b756"
DEFAULT_SPARK = "1.10.119"


def fetch_json(url: str):
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "VESSEL-Ecosystem-AutoSync/2.3"}
    )
    with urllib.request.urlopen(req, timeout=8) as resp:
        return json.loads(resp.read().decode("utf-8"))


def resolve_latest_versions():
    mc_ver = DEFAULT_MC
    geyser_ver = DEFAULT_GEYSER
    spark_ver = DEFAULT_SPARK

    try:
        mojang_data = fetch_json(MOJANG_MANIFEST_URL)
        latest_rel = mojang_data.get("latest", {}).get("release", "")
        if latest_rel:
            mc_ver = str(latest_rel).strip()
    except Exception:
        pass

    try:
        geyser_data = fetch_json(GEYSER_LATEST_URL)
        g_ver = geyser_data.get("version", "")
        g_build = geyser_data.get("build", "")
        if g_ver and g_build:
            geyser_ver = f"{g_ver}-b{g_build}"
        elif g_ver:
            geyser_ver = str(g_ver).strip()
    except Exception:
        pass

    try:
        spark_data = fetch_json(SPARK_LATEST_URL)
        if isinstance(spark_data, list) and len(spark_data) > 0:
            s_ver = spark_data[0].get("version_number", "")
            if s_ver:
                spark_ver = str(s_ver).strip()
    except Exception:
        pass

    return mc_ver, geyser_ver, spark_ver


def main():
    root = pathlib.Path(__file__).resolve().parent.parent
    docs_dir = root / "docs"
    docs_dir.mkdir(parents=True, exist_ok=True)
    manifest_path = docs_dir / "vessel-update-manifest.json"

    revision = 1046
    if manifest_path.exists():
        try:
            existing = json.loads(manifest_path.read_text(encoding="utf-8"))
            revision = int(existing.get("revisionNumber", 1046))
        except Exception:
            revision = 1046

    mc_ver, geyser_ver, spark_ver = resolve_latest_versions()
    version_code = f"VESSEL-2.3.0-rev{revision}+mc{mc_ver}-geyser{geyser_ver}-spark{spark_ver}"

    manifest_payload = {
        "schemaVersion": 1,
        "vesselVersion": "2.3.0",
        "revisionNumber": revision,
        "versionCode": version_code,
        "channel": "release",
        "updateIntervalSeconds": 3600,
        "restartRequiredForKernelPatches": True,
        "preservesAllUserData": True,
        "ecosystemDefaults": {
            "minecraftDefaultLatestRelease": mc_ver,
            "geyserLatestVersion": geyser_ver,
            "sparkLatestVersion": spark_ver
        },
        "credits": {
            "geyser": {
                "name": "GeyserMC (Geyser & Floodgate)",
                "authors": "GeyserMC Team & Contributors",
                "license": "MIT License",
                "url": "https://geysermc.org"
            },
            "spark": {
                "name": "spark (Performance Profiler)",
                "authors": "Lucko (lucko) & Contributors",
                "license": "GPL-3.0 (Standalone Profiler Agent/Plugin Integration)",
                "url": "https://spark.lucko.me"
            }
        }
    }

    manifest_path.write_text(
        json.dumps(manifest_payload, indent=2) + "\n",
        encoding="utf-8"
    )
    print(f"[VESSEL Sync] Updated {manifest_path}: {version_code}")


if __name__ == "__main__":
    main()
