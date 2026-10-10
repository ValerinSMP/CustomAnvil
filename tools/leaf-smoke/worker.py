#!/usr/bin/env python3
import os, subprocess, selectors, time, json, sys
from pathlib import Path
folder = Path.cwd()
start = time.monotonic()
command = ["/usr/lib/jvm/java-25-openjdk-amd64/bin/java", "-Xms256m", "-Xmx2G",
           "-XX:ActiveProcessorCount=2", "-Duser.home=" + str(folder / "home"),
           "-Djava.io.tmpdir=" + str(folder / "tmp"), "-Dpaper.disableStartupVersionCheck=true",
           "-Dvalerin.probe.eco=" + sys.argv[1], "-jar", "leaf.jar", "--nogui"]
proc = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                        stderr=subprocess.STDOUT)
selector = selectors.DefaultSelector()
selector.register(proc.stdout, selectors.EVENT_READ)
ready = False
stopping = None
pending = ""
with (folder / "console.log").open("w") as log:
    while proc.poll() is None:
        for key, _ in selector.select(timeout=0.5):
            chunk = os.read(key.fileobj.fileno(), 65536).decode(errors="replace")
            log.write(chunk); log.flush()
            pending = (pending + chunk)[-16384:]
            if "Done (" in pending and "For help" in pending: ready = True
        if stopping is None and ((folder / "probe-result.txt").exists() or time.monotonic() - start > 240):
            stopping = time.monotonic()
            try: proc.stdin.write(b"stop\n"); proc.stdin.flush()
            except BrokenPipeError: pass
        if stopping is not None and time.monotonic() - stopping > 25:
            proc.terminate()
            try: proc.wait(timeout=10)
            except subprocess.TimeoutExpired: proc.kill(); proc.wait()
    log.write(proc.stdout.read().decode(errors="replace"))
result = {"exit_code": proc.returncode, "ready": ready, "seconds": round(time.monotonic()-start, 1),
          "probe": (folder / "probe-result.txt").read_text() if (folder / "probe-result.txt").exists() else "MISSING",
          "root_visible": os.path.exists("/root/development")}
(folder / "worker-result.json").write_text(json.dumps(result, indent=2))
