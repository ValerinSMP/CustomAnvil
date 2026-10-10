#!/usr/bin/env bash
set -euo pipefail
project=$(cd "$(dirname "$0")/.." && pwd)
exec /root/development/tools/run-personal-build.sh "$project" \
    env JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=2" \
    ./gradlew --no-daemon --no-parallel --max-workers=1 --console=plain \
    -Ppaperweight.experimental.genSources=false -Psubprojects.reobfnms= \
    test shadowJar offlineJar
