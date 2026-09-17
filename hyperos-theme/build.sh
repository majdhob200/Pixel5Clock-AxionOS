#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC="$ROOT/theme-src"
DIST="$ROOT/dist"
TMP="$ROOT/.build"

rm -rf "$DIST" "$TMP"
mkdir -p "$DIST" "$TMP/lockscreen" "$TMP/mtz"

# Build extensionless `lockscreen` module as a ZIP archive.
(
  cd "$SRC/lockscreen"
  zip -q -r "$TMP/lockscreen.zip" advance
)
mv "$TMP/lockscreen.zip" "$TMP/mtz/lockscreen"

cp "$SRC/description.xml" "$TMP/mtz/description.xml"
if [ -d "$SRC/preview" ]; then
  cp -r "$SRC/preview" "$TMP/mtz/preview"
fi

# MTZ itself is a ZIP container with root entries directly inside it.
(
  cd "$TMP/mtz"
  zip -q -r "$DIST/PixelStackedClock-HyperOS3-v0.1.mtz" .
)

echo "Built: $DIST/PixelStackedClock-HyperOS3-v0.1.mtz"
unzip -l "$DIST/PixelStackedClock-HyperOS3-v0.1.mtz"
