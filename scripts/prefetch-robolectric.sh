#!/usr/bin/env bash
# Robolectric downloads its android-all jar from Maven Central without retrying, and Maven
# Central answers HTTP 429 to cloud IPs. This fetches the jar with retries into ~/.m2, where
# Robolectric looks first. Usage: scripts/prefetch-robolectric.sh <android-all-instrumented version>
# (the version is printed in the test failure: "Failed to fetch maven artifact ...:<version>").
set -euo pipefail
ver="${1:?version, e.g. 16-robolectric-13921718-i7}"
art=android-all-instrumented
dir="$HOME/.m2/repository/org/robolectric/$art/$ver"
file="$dir/$art-$ver.jar"
[[ -s "$file" ]] && { echo "already present: $file"; exit 0; }
mkdir -p "$dir"
for ((i = 1; i <= 12; i++)); do
  code=$(curl -sS -o "$file.part" -w '%{http_code}' --max-time 300 \
    "https://repo1.maven.org/maven2/org/robolectric/$art/$ver/$art-$ver.jar" || true)
  if [[ "$code" == 200 ]]; then mv "$file.part" "$file"; echo "fetched $file"; exit 0; fi
  echo "attempt $i: HTTP $code, waiting $((i * 15))s"; sleep $((i * 15))
done
rm -f "$file.part"; echo "giving up" >&2; exit 1
