#!/usr/bin/env bash
# Publish a signed release, or a clearly marked TEST prerelease if upload
# signing is unavailable. A successful build alone never proves release safety.
set -euo pipefail

: "${VOJS_VERSION:?Set release version (e.g. 1.0.50)}"
: "${VOJS_RELEASE_TARGET:?Set tested git commit}"
: "${VOJS_SIGNING_READY:?State whether production signing was verified}"
if [[ "${VOJS_QA_GATE_PASSED:-false}" != true ]]; then
  echo "::error::Post-merge QA has not passed. Release publication is blocked."
  exit 1
fi
if [[ ! "$VOJS_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ||
   [[ ! "$VOJS_RELEASE_TARGET" =~ ^[0-9a-f]{40}$ ]]; then
  echo "::error::Invalid version or release commit."
  exit 1
fi
if [[ "$VOJS_SIGNING_READY" != true && "$VOJS_SIGNING_READY" != false ]]; then
  echo "::error::Unknown signing state."
  exit 1
fi

DEBUG="app/build/outputs/apk/debug/app-debug.apk"
AAB="app/build/outputs/bundle/release/app-release.aab"
if [[ "$VOJS_SIGNING_READY" == true ]]; then
  TAG="v$VOJS_VERSION"
  APK="app/build/outputs/apk/release/app-release.apk"
  APK_NAME="VOJS-$TAG-release.apk"
  AAB_NAME="VOJS-$TAG-release.aab"
  TITLE="VOJS $TAG"
  NOTES="Verified Android production-signing configuration: APK/AAB are signed and all required GitHub post-merge checks passed. Google Play Console review, Data safety and production HTTPS verification remain separate obligations."
  FLAGS=()
else
  TAG="v$VOJS_VERSION-test"
  APK="app/build/outputs/apk/release/app-release-unsigned.apk"
  APK_NAME="VOJS-$TAG-unsigned.apk"
  AAB_NAME="VOJS-$TAG-unsigned.aab"
  TITLE="VOJS $TAG — test build (NOT Google Play ready)"
  NOTES="TEST PRERELEASE — NOT FOR GOOGLE PLAY. The debug APK is signed with an Android debug key and can be installed for testing. The release APK and AAB are UNSIGNED, cannot be used to update an installed production app and must not be uploaded to Google Play. Configure the private production upload signing key for a signed release. All required GitHub post-merge checks passed; this does NOT imply Play Console approval."
  FLAGS=(--prerelease)
fi

for file in "$DEBUG" "$APK" "$AAB"; do
  if [[ ! -s "$file" ]]; then
    echo "::error::Required APK/AAB is missing or empty: $file"
    exit 1
  fi
done
mkdir -p dist
cp "$DEBUG" "dist/VOJS-$TAG-debug.apk"
cp "$APK" "dist/$APK_NAME"
cp "$AAB" "dist/$AAB_NAME"
printf '%s\n' "$NOTES" > dist/RELEASE-NOTES.md
(
  cd dist
  sha256sum "VOJS-$TAG-debug.apk" "$APK_NAME" "$AAB_NAME" > SHA256SUMS
)
gh release create "$TAG" \
  "dist/VOJS-$TAG-debug.apk" "dist/$APK_NAME" "dist/$AAB_NAME" \
  "dist/SHA256SUMS" \
  --target "$VOJS_RELEASE_TARGET" --title "$TITLE" \
  --notes-file dist/RELEASE-NOTES.md "${FLAGS[@]}"
printf 'Published %s\n' "$TAG"
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  printf '### Published %s\n\n%s\n' "$TAG" "$NOTES" >> "$GITHUB_STEP_SUMMARY"
fi
