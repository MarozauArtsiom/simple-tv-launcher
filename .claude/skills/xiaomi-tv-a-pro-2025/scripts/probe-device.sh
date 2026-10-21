#!/usr/bin/env bash
# Collects the facts this project depends on from the real TV and writes them to docs/device/.
# Run from the repo root on a machine whose adb can reach the TV:
#   adb connect <tv-ip>:5555
#   .claude/skills/xiaomi-tv-a-pro-2025/scripts/probe-device.sh [adb-serial]
# No -e/pipefail: a grep with no match must not abort the probe.
set -u

serial="${1:-}"
adb_cmd=(adb)
[[ -n "$serial" ]] && adb_cmd+=(-s "$serial")
tv() { "${adb_cmd[@]}" shell "$@" 2>&1 | tr -d '\r'; }

out="docs/device"
mkdir -p "$out"

"${adb_cmd[@]}" get-state >/dev/null || { echo "adb device not reachable" >&2; exit 1; }

{
  echo "# Device profile (probed $(date -u +%Y-%m-%dT%H:%MZ))"
  echo
  echo "## Build"
  for p in ro.product.manufacturer ro.product.brand ro.product.model ro.product.name ro.product.device \
           ro.build.version.release ro.build.version.sdk ro.build.version.security_patch \
           ro.build.fingerprint ro.board.platform ro.hardware ro.product.cpu.abilist \
           ro.config.low_ram ro.sf.lcd_density dalvik.vm.heapgrowthlimit dalvik.vm.heapsize \
           ro.opengles.version ro.hardware.egl; do
    printf -- '- `%s`: %s\n' "$p" "$(tv getprop "$p")"
  done
  echo
  echo "## Display"
  echo '```'
  tv wm size
  tv wm density
  tv dumpsys display | grep -E "mBaseDisplayInfo|supportedModes|hdrCapabilities|mActiveColorMode" | head -10
  echo '```'
  echo
  echo "## Memory"
  echo '```'
  tv cat /proc/meminfo | head -5
  echo '```'
  echo
  echo "## Home"
  echo '```'
  tv cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME
  echo "--- HOME role holders:"
  tv cmd role get-role-holders android.app.role.HOME
  echo '```'
  echo
  echo "## HDMI passthrough handler"
  echo '```'
  tv cmd package query-activities --brief -a android.intent.action.VIEW -d content://android.media.tv/passthrough/x
  echo '```'
  echo
  echo "## Features"
  echo '```'
  tv pm list features | grep -iE "leanback|live_tv|hdmi|touchscreen|vulkan|opengl" | sort
  echo '```'
} > "$out/profile.md"

tv pm list packages -s | sed 's/^package://' | sort > "$out/packages-system.txt"
tv pm list packages -3 | sed 's/^package://' | sort > "$out/packages-user.txt"
tv dumpsys tv_input > "$out/tv-input.txt"

echo "Wrote $out/profile.md, packages-system.txt, packages-user.txt, tv-input.txt"
echo "Next: run 'adb shell getevent -lt', press every remote button, and record keycodes in $out/remote.md"
