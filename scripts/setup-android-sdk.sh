#!/usr/bin/env bash
# Installs the Android SDK pieces this project needs into ~/android-sdk (idempotent) and
# writes local.properties. Safe to run twice: the second run changes nothing.
set -euo pipefail

SDK="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
PLATFORM="platforms;android-37.0"
BUILD_TOOLS="build-tools;36.0.0"
TOOLS_BIN="$SDK/cmdline-tools/latest/bin"

retry() { # retry <attempts> <cmd...>: the proxy and Maven hosts sometimes answer 429
  local n=$1; shift
  for ((i = 1; i <= n; i++)); do "$@" && return 0; sleep $((i * 3)); done
  return 1
}

mkdir -p "$SDK"

if [[ ! -x "$TOOLS_BIN/sdkmanager" ]]; then
  echo "Installing Android command-line tools..."
  build=$(retry 3 curl -fsS --max-time 60 https://dl.google.com/android/repository/repository2-3.xml \
    | grep -oE 'commandlinetools-linux-[0-9]+_latest\.zip' | grep -oE '[0-9]+' | sort -n | tail -1)
  [[ -n "$build" ]] || { echo "Could not find the cmdline-tools build number" >&2; exit 1; }
  tmp=$(mktemp -d)
  retry 3 curl -fsS --max-time 300 -o "$tmp/tools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-${build}_latest.zip"
  unzip -q "$tmp/tools.zip" -d "$tmp/x"
  mkdir -p "$SDK/cmdline-tools"
  mv "$tmp/x/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "${tmp:?}"
fi

installed=$("$TOOLS_BIN/sdkmanager" --sdk_root="$SDK" --list_installed 2>/dev/null || true)
missing=()
for pkg in "$PLATFORM" "$BUILD_TOOLS" "platform-tools"; do
  # sdkmanager prints packages with ';' in --list_installed on some versions and '/' on others
  alt="${pkg//;//}"
  if ! grep -qE "^\s*(${pkg}|${alt})\s" <<<"$installed"; then missing+=("$pkg"); fi
done

if ((${#missing[@]})); then
  echo "Accepting licences and installing: ${missing[*]}"
  yes | "$TOOLS_BIN/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null 2>&1 || true
  retry 3 "$TOOLS_BIN/sdkmanager" --sdk_root="$SDK" "${missing[@]}"
else
  echo "SDK already complete."
fi

cd "$(dirname "$0")/.."
line="sdk.dir=$SDK"
if ! grep -qxF "$line" local.properties 2>/dev/null; then echo "$line" > local.properties; fi
echo "Done. sdk.dir=$SDK"
