#!/usr/bin/env bash
# Prints the newest *stable* version of every library and plugin in docs/tech-stack.md,
# straight from Google Maven and Maven Central. Run it before scaffolding or upgrading,
# then update docs/tech-stack.md and gradle/libs.versions.toml together.
set -u

G=https://dl.google.com/dl/android/maven2
C=https://repo.maven.apache.org/maven2

newest_stable() { # $1 = repo base, $2 = group/artifact path
  local body=""
  for _ in 1 2 3; do # Maven Central sometimes answers 429; retry
    body=$(curl -fsS --max-time 30 "$1/$2/maven-metadata.xml" 2>/dev/null) && break
    sleep 3
  done
  printf '%s' "$body" | python3 -c '
import re, sys
vs = re.findall(r"<version>([^<]+)</version>", sys.stdin.read())
stable = [v for v in vs if not re.search(r"(?i)alpha|beta|rc|dev|eap|snapshot|-M\d", v)]
key = lambda v: [int(p) if p.isdigit() else -1 for p in re.split(r"[.\-]", v)]
print(sorted(stable, key=key)[-1] if stable else "?")'
}

row() { printf '%-28s %s\n' "$1" "$(newest_stable "$2" "$3")"; }

echo "# Newest stable versions ($(date -u +%Y-%m-%d))"
row agp                    $G com/android/tools/build/gradle
row kotlin                 $C org/jetbrains/kotlin/kotlin-gradle-plugin
row compose-bom            $G androidx/compose/compose-bom
row tv-material            $G androidx/tv/tv-material
row activity-compose       $G androidx/activity/activity-compose
row lifecycle              $G androidx/lifecycle/lifecycle-runtime-compose
row core-ktx               $G androidx/core/core-ktx
row datastore              $G androidx/datastore/datastore-preferences
row palette                $G androidx/palette/palette-ktx
row profileinstaller       $G androidx/profileinstaller/profileinstaller
row benchmark-baselineprofile $G androidx/benchmark/benchmark-macro-junit4
row uiautomator            $G androidx/test/uiautomator/uiautomator
row androidx-test-junit    $G androidx/test/ext/junit
row coroutines             $C org/jetbrains/kotlinx/kotlinx-coroutines-android
row serialization-json     $C org/jetbrains/kotlinx/kotlinx-serialization-json
row robolectric            $C org/robolectric/robolectric
row roborazzi              $C io/github/takahirom/roborazzi/roborazzi
row junit4                 $C junit/junit
printf '%-28s %s\n' gradle "$(curl -fsS --max-time 20 https://services.gradle.org/versions/current | python3 -c 'import json,sys; print(json.load(sys.stdin)["version"])' 2>/dev/null || echo '?')"
