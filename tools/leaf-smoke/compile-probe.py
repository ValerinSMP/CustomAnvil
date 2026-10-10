#!/usr/bin/env python3
import subprocess, shutil, json, sys
from pathlib import Path
project = Path(__file__).resolve().parents[2]
runtime = Path(sys.argv[1])
base = Path(sys.argv[2])
out = project / "build/leaf-probe"
out.mkdir(parents=True, exist_ok=True)
jars = [runtime / "versions/26.2/leaf-26.2.jar",
        project / "build/libs/CustomAnvil-2.2.0-valerin.1-offline.jar"]
jars += sorted((runtime / "libraries").rglob("*.jar"))
for row in json.loads((base / "personal-transfer-manifest.json").read_text()):
    if row["name"] in ["eco", "libreforge", "EcoEnchants", "EcoItems"]:
        jars.append(base / row["archive_path"])
cp = ":".join(map(str, jars))
result = subprocess.run(["/usr/lib/jvm/java-25-openjdk-amd64/bin/javac", "-classpath", cp,
                "-d", str(out), str(project / "tools/leaf-smoke/LeafAnvilProbe.java")])
if result.returncode: sys.exit(result.returncode)
shutil.copy2(project / "tools/leaf-smoke/plugin.yml", out / "plugin.yml")
subprocess.run(["/usr/lib/jvm/java-25-openjdk-amd64/bin/jar", "cf",
                str(project / "build/LeafAnvilProbe.jar"), "-C", str(out), "."], check=True)
