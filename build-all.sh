#!/usr/bin/env bash
# Build every Doodly module → jars land in each module's build/libs/
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}java"

echo "Java:"
$JAVA_BIN -version 2>&1 | head -3

MODULES=(DoodlyCore PlayerEsp BaseFinder ChestEsp FreeCam)
OUT="$ROOT/dist"
mkdir -p "$OUT"

for m in "${MODULES[@]}"; do
  dir="$ROOT/$m"
  if [[ ! -d "$dir" ]]; then
    echo "skip missing $m"
    continue
  fi
  if [[ ! -x "$dir/gradlew" ]]; then
    echo "skip $m (no gradlew)"
    continue
  fi
  echo ""
  echo "======== building $m ========"
  (
    cd "$dir"
    ./gradlew build --no-daemon -q
  )
  # copy non-sources / non-dev jars
  shopt -s nullglob
  for jar in "$dir"/build/libs/*.jar; do
    base="$(basename "$jar")"
    case "$base" in
      *-sources.jar|*-dev.jar|*dev-shadow*) continue ;;
    esac
    cp -f "$jar" "$OUT/"
    echo "  → dist/$base"
  done
done

echo ""
echo "Done. Installable jars:"
ls -lh "$OUT"
echo ""
echo "Copy everything in dist/ into .minecraft/mods/"
echo "Need: Fabric Loader + Fabric API + Cloth Config for your MC version."
echo "In game: Right Shift opens Doodly Core menu."
