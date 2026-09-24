#!/usr/bin/env bash
# Generate Android launcher icons from a master PNG.
#
# Usage:
#   ./make_icons.sh <master.png> [background_hex] [app_module_path]

set -euo pipefail

MASTER="${1:-}"
BG_HEX="${2:-#2D1B4E}"
APP_PATH="${3:-.}"

if [[ -z "$MASTER" || ! -f "$MASTER" ]]; then
    echo "Usage: $0 <master.png> [background_hex] [app_module_path]"
    exit 1
fi

RES_DIR="$APP_PATH/src/main/res"
if [[ ! -d "$APP_PATH" ]]; then
    echo "ERROR: app module not found at: $APP_PATH"
    exit 1
fi

if command -v magick >/dev/null 2>&1; then
    IM="magick"
elif command -v convert >/dev/null 2>&1; then
    IM="convert"
else
    echo "ERROR: ImageMagick not found."
    exit 1
fi

echo "==> Using: $IM"
echo "==> Master: $MASTER"
echo "==> Background: $BG_HEX"
echo "==> Res dir: $RES_DIR"

echo "==> Master size: $($IM identify -format '%wx%h' "$MASTER" 2>/dev/null)"

# Legacy icons
declare -A LEGACY_SIZES=(
    [mipmap-mdpi]=48
    [mipmap-hdpi]=72
    [mipmap-xhdpi]=96
    [mipmap-xxhdpi]=144
    [mipmap-xxxhdpi]=192
)

for dir in "${!LEGACY_SIZES[@]}"; do
    size="${LEGACY_SIZES[$dir]}"
    mkdir -p "$RES_DIR/$dir"
    $IM "$MASTER" -resize "${size}x${size}" "$RES_DIR/$dir/ic_launcher.png"
    $IM "$MASTER" -resize "${size}x${size}" "$RES_DIR/$dir/ic_launcher_round.png"
    echo "  -> $dir/ic_launcher.png (${size}x${size})"
done

# Adaptive foreground (432x432 canvas, 288x288 safe zone)
FG_SIZE=432
SAFE_SIZE=288
mkdir -p "$RES_DIR/drawable"
$IM "$MASTER" -resize "${SAFE_SIZE}x${SAFE_SIZE}" \
    -background none -gravity center -extent "${FG_SIZE}x${FG_SIZE}" \
    "$RES_DIR/drawable/ic_launcher_foreground.png"
echo "  -> drawable/ic_launcher_foreground.png"

# Adaptive icon XML
mkdir -p "$RES_DIR/mipmap-anydpi-v26"
cat > "$RES_DIR/mipmap-anydpi-v26/ic_launcher.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
EOF
cp "$RES_DIR/mipmap-anydpi-v26/ic_launcher.xml" \
   "$RES_DIR/mipmap-anydpi-v26/ic_launcher_round.xml"
echo "  -> mipmap-anydpi-v26/ic_launcher.xml"

# Background color
mkdir -p "$RES_DIR/values"
cat > "$RES_DIR/values/ic_launcher_background.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">$BG_HEX</color>
</resources>
EOF
echo "  -> values/ic_launcher_background.xml ($BG_HEX)"

# Play Store 512
$IM "$MASTER" -resize 512x512 "$APP_PATH/../playstore-icon-512.png"
echo "  -> ../playstore-icon-512.png"

echo ""
echo "==> DONE."
