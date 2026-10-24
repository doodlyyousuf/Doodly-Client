#!/usr/bin/env bash
# Build every Doodly module and collect jars into ONE folder: dist/
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/dist"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}java"

echo "Java:"
$JAVA_BIN -version 2>&1 | head -3 || true

MODULES=(DoodlyCore PlayerEsp BaseFinder ChestEsp FreeCam)

rm -rf "$OUT"
mkdir -p "$OUT"

ok=0
fail=0

for m in "${MODULES[@]}"; do
  dir="$ROOT/$m"
  if [[ ! -d "$dir" ]]; then
    echo "skip missing $m"
    continue
  fi
  if [[ ! -f "$dir/gradlew" ]]; then
    echo "skip $m (no gradlew)"
    continue
  fi
  chmod +x "$dir/gradlew" 2>/dev/null || true

  echo ""
  echo "======== building $m ========"
  # Skip spotless — missing codestyle files must not fail the jar
  if (
    cd "$dir"
    ./gradlew build --no-daemon -x spotlessJava -x spotlessCheck -x spotlessApply
  ); then
    found=0
    shopt -s nullglob
    for jar in "$dir"/build/libs/*.jar; do
      base="$(basename "$jar")"
      case "$base" in
        *-sources.jar|*-dev.jar|*-dev-shadow.jar|*javadoc*) continue ;;
      esac
      cp -f "$jar" "$OUT/"
      echo "  → dist/$base"
      found=1
    done
    shopt -u nullglob
    if [[ "$found" -eq 1 ]]; then
      ok=$((ok + 1))
    else
      echo "  (build ok but no jar in build/libs/)"
      fail=$((fail + 1))
    fi
  else
    echo "  BUILD FAILED: $m"
    fail=$((fail + 1))
  fi
done

echo ""
echo "========================================"
echo " All installable jars are here:"
echo "   $OUT"
echo "========================================"
ls -lh "$OUT" 2>/dev/null || true
echo ""
echo "Modules built OK: $ok  |  failed: $fail"
echo "Copy every .jar from dist/ into .minecraft/mods/"
