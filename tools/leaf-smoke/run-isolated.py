#!/usr/bin/env python3
import fcntl, hashlib, json, os, shutil, subprocess, tempfile, zipfile, sys
from pathlib import Path
project = Path(__file__).resolve().parents[2]
art = Path(sys.argv[3])
base = Path(sys.argv[2])
cached = Path(sys.argv[1])
lock = open("/run/lock/valerin-development-build.lock", "a")
fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
assert int(next(x.split()[1] for x in Path("/proc/meminfo").read_text().splitlines()
                if x.startswith("MemAvailable:"))) > 6*1024*1024
assert hashlib.sha256((cached / "leaf.jar").read_bytes()).hexdigest() == "38d0ad83fa9c874859f4dc62fec76b852d5bd82be16d603b879bf304a70f3481"
manifest = json.loads((base / "personal-transfer-manifest.json").read_text())
rows = {r["name"]: r for r in manifest}
results = []
for eco in [False, True]:
    folder = Path(tempfile.mkdtemp(prefix="valerin-customanvil-leaf262-"))
    os.chmod(folder, 0o755)
    for name in ["libraries", "versions", "cache"]:
        shutil.copytree(cached / name, folder / name)
    shutil.copy2(cached / "leaf.jar", folder / "leaf.jar")
    for name in ["plugins", "tmp", "home"]: (folder / name).mkdir()
    for name in ["CustomAnvil-2.2.0-valerin.2-offline.jar"]:
        shutil.copy2(project / "build/libs" / name, folder / "plugins" / name)
    shutil.copy2(project / "build/LeafAnvilProbe.jar", folder / "plugins/LeafAnvilProbe.jar")
    # Public defaults embedded in the installed 1.16.3 binary, with synthetic overrides.
    config = folder / "plugins/CustomAnvil"; config.mkdir()
    with zipfile.ZipFile(base / rows["CustomAnvil"]["archive_path"]) as z:
        for name in ["config.yml", "enchant_conflict.yml", "item_groups.yml",
                     "unit_repair_item.yml", "custom_recipes.yml"]:
            (config / name).write_bytes(z.read(name))
    text = (config / "config.yml").read_text()
    import re
    for key, value in {"limit_repair_value":"250", "limit_repair_cost":"true",
                       "replace_too_expensive":"true", "reset_result_repair_cost":"true",
                       "reset_result_repair_cost_value":"0", "item_rename_cost":"1", "allow_color_code":"false",
                       "configVersion":"1.15.6", "lowMinecraftVersion":"1.21.11",
                       "force_protocolib":"false", "metric_type":"disabled",
                       "metric_collect_errors":"false",
                       "check_for_update":"false"}.items():
        pattern = r"(?m)^" + re.escape(key) + r":.*$"
        if re.search(pattern, text):
            text = re.sub(pattern, key + ": " + value, text)
        else:
            text += "\n" + key + ": " + value + "\n"
    text += "\nvalerin_fixture: preserve\n"
    (config / "config.yml").write_text(text)
    before = art / ("fixture-eco" if eco else "fixture-native"); before.mkdir(exist_ok=True)
    for name in config.iterdir(): shutil.copy2(name, before / name.name)
    if eco:
        for name in ["eco", "libreforge", "EcoEnchants", "EcoItems"]:
            row = rows[name]; jar = base / row["archive_path"]
            assert hashlib.sha256(jar.read_bytes()).hexdigest() == row["test_sha256"]
            shutil.copy2(jar, folder / "plugins" / (name + ".jar"))
    shutil.copy2(project / "tools/leaf-smoke/worker.py", folder / "worker.py")
    (folder / "eula.txt").write_text("eula=true\n")
    (folder / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25692\nonline-mode=false\nenable-query=false\n"
        "enable-rcon=false\nlevel-name=probe_world\nlevel-type=minecraft:flat\n"
        "generator-settings={\"biome\":\"minecraft:plains\",\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1},{\"block\":\"minecraft:dirt\",\"height\":2},{\"block\":\"minecraft:grass_block\",\"height\":1}]}\n"
        "generate-structures=false\nspawn-protection=0\nview-distance=2\nsimulation-distance=2\n"
        "max-players=1\n")
    for parent, dirs, files in os.walk(folder):
        os.chown(parent, 65534, 65534)
        for name in files: os.chown(Path(parent) / name, 65534, 65534)
    unit = "valerin-anvil-smoke-" + str(os.getpid()) + ("-eco" if eco else "-native")
    props = ["MemoryMax=4G", "MemorySwapMax=0", "CPUQuota=200%", "CPUWeight=10", "IOWeight=10",
             "TasksMax=256", "RuntimeMaxSec=300", "TimeoutStopSec=20", "KillMode=control-group",
             "User=nobody", "Group=nogroup", "ProtectHome=yes", "ProtectSystem=strict",
             "NoNewPrivileges=yes", "PrivateDevices=yes", "PrivateNetwork=yes",
             "WorkingDirectory=" + str(folder), "ReadWritePaths=" + str(folder)]
    args = ["systemd-run", "--quiet", "--wait", "--pipe", "--collect", "--unit=" + unit]
    for prop in props: args += ["--property=" + prop]
    args += ["/usr/bin/env", "-i", "PATH=/usr/bin:/bin", "HOME=" + str(folder),
             "/usr/bin/nice", "-n", "15", "/usr/bin/ionice", "-c", "3",
             "/usr/bin/python3", str(folder / "worker.py"), str(eco).lower()]
    print("START", unit, folder, flush=True)
    with (art / (unit + ".log")).open("w") as log:
        run = subprocess.run(args, stdout=log, stderr=subprocess.STDOUT, timeout=330)
    result = {"eco":eco, "runtime": str(folder), "executor_exit": run.returncode, "unit":unit}
    if (folder / "worker-result.json").exists():
        result.update(json.loads((folder / "worker-result.json").read_text()))
    result["unit_active_after"] = subprocess.run(["systemctl", "is-active", unit],
        capture_output=True, text=True).stdout.strip()
    dest = art / ("smoke-eco" if eco else "smoke-native"); dest.mkdir(exist_ok=True)
    for name in ["console.log", "worker-result.json", "probe-result.txt"]:
        if (folder / name).exists(): shutil.copy2(folder / name, dest / name)
    shutil.copytree(config, dest / "migrated-config", dirs_exist_ok=True)
    print("RESULT", json.dumps(result), flush=True)
    results.append(result)
(art / "smoke-results.json").write_text(json.dumps(results, indent=2))

if not all(r.get("executor_exit") == 0 and r.get("ready") and
           r.get("probe", "").startswith("PASS\n") and
           r.get("root_visible") is False and r.get("unit_active_after") == "inactive"
           for r in results):
    sys.exit(1)
