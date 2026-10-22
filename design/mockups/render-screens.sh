#!/usr/bin/env bash
# Renders the batch-2 screens in three representative themes (dark, light, cartoon) to design/mockups/screens/.
# Usage: design/mockups/render-screens.sh [theme ...]   (default: midnight sky valley)
set -euo pipefail
cd "$(dirname "$0")/../.."
R=.claude/skills/tv-ui-design/scripts/render-mockup.cjs
themes=("$@"); [[ ${#themes[@]} -eq 0 ]] && themes=(midnight sky valley)
for t in "${themes[@]}"; do
  for p in home-empty-favs app-menu favourites-move weather-search settings-background settings-hidden \
           settings-about settings-schedule settings-hdmi iphone-hint \
           "settings-usb:list" "settings-usb:nopermission" "settings-usb:installing" "settings-usb:done"; do
    page=${p%%:*}; state=""; [[ "$p" == *:* ]] && state="&state=${p##*:}"
    out="design/mockups/screens/${p//:/-}-$t.png"
    node $R "design/mockups/$page.html?t=$t$state" "$out" >/dev/null
  done
done
echo "rendered ${#themes[@]} theme(s)"
