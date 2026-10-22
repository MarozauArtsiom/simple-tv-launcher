#!/usr/bin/env bash
# ./gradlew with retries: Maven Central and the plugin portal answer HTTP 429 to cloud IPs.
# Usage: scripts/gradlew-retry.sh <gradle args...>   (env RETRIES, default 6)
set -u
cd "$(dirname "$0")/.."
log=$(mktemp)
for ((i = 1; i <= ${RETRIES:-6}; i++)); do
  ./gradlew --console=plain "$@" 2>&1 | grep -v "^Picked up" | tee "$log"
  [[ ${PIPESTATUS[0]} -eq 0 ]] && exit 0
  grep -q "429\|Too Many Requests\|Could not resolve\|Could not GET\|Could not HEAD" "$log" || exit 1
  echo ">>> dependency download throttled, retry $i after $((i * 20))s"; sleep $((i * 20))
done
exit 1
