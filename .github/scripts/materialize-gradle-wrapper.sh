#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WRAPPER_DIR="$ROOT_DIR/gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"
BASE64_PAYLOAD="$WRAPPER_DIR/gradle-wrapper.jar.base64"
CHECKSUM_FILE="$WRAPPER_DIR/gradle-wrapper.jar.sha256"

if [[ -f "$WRAPPER_JAR" ]]; then
  exit 0
fi

if [[ ! -r "$BASE64_PAYLOAD" ]]; then
  echo "Gradle wrapper payload not found at $BASE64_PAYLOAD" >&2
  exit 1
fi

if tmpdir=$(mktemp -d 2>/dev/null); then
  :
else
  tmpdir="$WRAPPER_DIR/.gradle-wrapper-tmp"
  rm -rf "$tmpdir"
  mkdir -p "$tmpdir"
fi
trap 'rm -rf "$tmpdir"' EXIT

tmpjar="$tmpdir/gradle-wrapper.jar"
decoded=false

if command -v base64 >/dev/null 2>&1; then
  if base64 --decode "$BASE64_PAYLOAD" >"$tmpjar"; then
    decoded=true
  else
    echo "base64 command failed while decoding Gradle wrapper" >&2
  fi
fi

if [[ "$decoded" == false ]]; then
  for py in python3 python; do
    if command -v "$py" >/dev/null 2>&1; then
      "$py" - "$BASE64_PAYLOAD" "$tmpjar" <<'PYCODE'
import base64
import sys

if len(sys.argv) != 3:
    raise SystemExit("usage: decode_base64 <source> <target>")

source, target = sys.argv[1:]
with open(source, "rb") as src, open(target, "wb") as dst:
    dst.write(base64.b64decode(src.read()))
PYCODE
      if [[ $? -eq 0 ]]; then
        decoded=true
        break
      fi
    fi
  done
fi

if [[ "$decoded" == false ]]; then
  echo "Unable to decode Gradle wrapper payload; install base64 or Python" >&2
  exit 1
fi

if [[ -r "$CHECKSUM_FILE" ]]; then
  expected=$(tr -d ' \r\n' < "$CHECKSUM_FILE")
  if command -v sha256sum >/dev/null 2>&1; then
    actual=$(sha256sum "$tmpjar" | awk '{print $1}')
  elif command -v shasum >/dev/null 2>&1; then
    actual=$(shasum -a 256 "$tmpjar" | awk '{print $1}')
  else
    echo "sha256sum or shasum not found; skipping Gradle wrapper checksum verification" >&2
    actual="$expected"
  fi

  if [[ "$expected" != "$actual" ]]; then
    echo "Gradle wrapper payload checksum mismatch (expected $expected, got $actual)" >&2
    exit 1
  fi
fi

mkdir -p "$WRAPPER_DIR"
mv "$tmpjar" "$WRAPPER_JAR"
echo "Gradle wrapper JAR materialized at $WRAPPER_JAR"
