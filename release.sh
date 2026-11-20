#!/bin/bash
# Публикация релиза VAGDroid на GitHub (использует git credential для токена)
set -e
cd "$(dirname "$0")"

REPO="ArtemDit/VAGDroid"
TAG="v1.0"
APK="app/build/outputs/apk/release/app-release.apk"

TOKEN=$(printf "protocol=https\nhost=github.com\n\n" | git credential fill | grep '^password=' | cut -d= -f2)
if [ -z "$TOKEN" ]; then echo "Нет токена GitHub"; exit 1; fi

# 1. Создать релиз
echo "Создаю релиз $TAG..."
RESP=$(curl -sS -X POST "https://api.github.com/repos/$REPO/releases" \
  -H "Authorization: token $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -d "{\"tag_name\":\"$TAG\",\"name\":\"VAGDroid 1.0\",\"body\":\"VAG-COM 311.2 для Android: KW1281/KWP2000, KKL USB/BT-адаптеры. См. README.md\",\"draft\":false,\"prerelease\":false}")
UPLOAD_URL=$(echo "$RESP" | grep -o '"upload_url": *"[^"]*' | cut -d'"' -f4)
if [ -z "$UPLOAD_URL" ]; then echo "Ошибка создания релиза:"; echo "$RESP" | head -20; exit 1; fi
UPLOAD_URL="${UPLOAD_URL%\{*}"

# 2. Загрузить APK
echo "Загружаю APK..."
curl -sS -X POST "$UPLOAD_URL?name=VAGDroid-1.0.apk" \
  -H "Authorization: token $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary "@$APK" | grep -o '"browser_download_url": *"[^"]*' | cut -d'"' -f4

echo "Готово."
