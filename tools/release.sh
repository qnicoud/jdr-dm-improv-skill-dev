#!/usr/bin/env bash
# Build, sign and publish a DM Gym release on GitHub.
#
# Usage: DMGYM_KEYSTORE=~/path/key.jks tools/release.sh [options]
#
#   --dry-run          build and sign only: no tag push, no GitHub release
#   --skip-checks      skip `./gradlew check` (assembleRelease still runs)
#   --draft            create the GitHub release as a draft
#   --yes              do not ask for confirmation before publishing
#   --notes-file FILE  release notes (default: commit subjects since the previous tag)
#
# The APK is always built from tag v<versionName> in a temporary worktree.
# If the tag does not exist yet, it is created on HEAD (clean main, in sync with origin).
#
# Environment:
#   DMGYM_KEYSTORE     keystore path (required)
#   DMGYM_KEY_ALIAS    key alias (optional when the keystore holds a single key)
#   DMGYM_CERT_SHA256  expected signing certificate SHA-256 (hex, colons optional);
#                      when set, an APK signed with another key is refused
#   ANDROID_HOME       Android SDK (default: ~/Android/Sdk)
#   DMGYM_BUILD_TOOLS  build-tools version (default: 36.0.0)
#   JAVA_HOME          JDK used by Gradle (default: /usr/lib/jvm/java-21-openjdk)
#
# Passwords are asked by apksigner itself: they never go through this script.
# The keystore must never be committed (`*.jks` and `*.keystore` are git-ignored).
set -euo pipefail

DRY_RUN=0
SKIP_CHECKS=0
DRAFT=0
ASSUME_YES=0
NOTES_FILE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1 ;;
    --skip-checks) SKIP_CHECKS=1 ;;
    --draft) DRAFT=1 ;;
    --yes) ASSUME_YES=1 ;;
    --notes-file) NOTES_FILE="$(realpath "${2:?--notes-file needs a path}")"; shift ;;
    -h|--help) sed -n '2,25p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
  shift
done

die() { echo "Error: $*" >&2; exit 1; }
step() { printf '\n==> %s\n' "$*"; }

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
BT="$SDK/build-tools/${DMGYM_BUILD_TOOLS:-36.0.0}"
ZIPALIGN="$BT/zipalign"
APKSIGNER="$BT/apksigner"
export ANDROID_HOME="$SDK"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}"
export PATH="$JAVA_HOME/bin:$PATH"

step "Preflight"
KEYSTORE="${DMGYM_KEYSTORE:-}"
[[ -n "$KEYSTORE" ]] || die "set DMGYM_KEYSTORE to your keystore path"
[[ -f "$KEYSTORE" ]] || die "keystore not found: $KEYSTORE"
command -v gh >/dev/null || die "gh (GitHub CLI) is not installed"
[[ -x "$ZIPALIGN" && -x "$APKSIGNER" ]] || die "zipalign/apksigner not found in $BT"
[[ -x "$JAVA_HOME/bin/java" ]] || die "no JDK in JAVA_HOME=$JAVA_HOME"
[[ -z "$NOTES_FILE" || -f "$NOTES_FILE" ]] || die "notes file not found: $NOTES_FILE"
if [[ $DRY_RUN -eq 0 ]]; then
  gh auth status >/dev/null 2>&1 || die "gh is not authenticated (run: gh auth login)"
fi

VERSION="$(sed -n 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"\(.*\)".*/\1/p' app/build.gradle.kts | head -1)"
[[ -n "$VERSION" ]] || die "versionName not found in app/build.gradle.kts"
TAG="v$VERSION"

git fetch --quiet origin --tags
if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
  TAG_IS_NEW=0
  echo "Version $VERSION: building existing tag $TAG ($(git rev-list -n1 "$TAG" | cut -c1-7))"
else
  TAG_IS_NEW=1
  [[ "$(git branch --show-current)" == "main" ]] || die "tag $TAG does not exist: run from main to create it"
  [[ -z "$(git status --porcelain)" ]] || die "working tree is not clean"
  git fetch --quiet origin main
  [[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/main)" ]] \
    || die "main is not in sync with origin/main (push or pull first)"
  echo "Version $VERSION: tag $TAG will be created on HEAD ($(git rev-parse --short HEAD))"
fi

if [[ $DRY_RUN -eq 0 ]] && gh release view "$TAG" >/dev/null 2>&1; then
  die "GitHub release $TAG already exists; bump versionCode/versionName first"
fi

WORK="$(mktemp -d)"
SRC="$WORK/src"
cleanup() {
  git -C "$ROOT" worktree remove --force "$SRC" >/dev/null 2>&1 || true
  rm -rf "$WORK"
}
trap cleanup EXIT
git worktree add --quiet --detach "$SRC" "$([[ $TAG_IS_NEW -eq 1 ]] && echo HEAD || echo "$TAG")"

step "Build"
TASKS=(assembleRelease)
[[ $SKIP_CHECKS -eq 1 ]] || TASKS=(check assembleRelease)
(cd "$SRC" && ./gradlew --console=plain --no-configuration-cache "${TASKS[@]}")
UNSIGNED="$SRC/app/build/outputs/apk/release/app-release-unsigned.apk"
[[ -f "$UNSIGNED" ]] || die "unsigned APK not found: $UNSIGNED"

step "Align and sign"
DIST="$ROOT/dist"
mkdir -p "$DIST"
APK="$DIST/dmgym-$VERSION.apk"
"$ZIPALIGN" -p -f 4 "$UNSIGNED" "$WORK/aligned.apk"
SIGN_ARGS=(--ks "$KEYSTORE")
[[ -z "${DMGYM_KEY_ALIAS:-}" ]] || SIGN_ARGS+=(--ks-key-alias "$DMGYM_KEY_ALIAS")
"$APKSIGNER" sign "${SIGN_ARGS[@]}" --out "$APK" "$WORK/aligned.apk"
rm -f "$APK.idsig"

step "Verify"
CERTS="$("$APKSIGNER" verify --print-certs "$APK")"
echo "$CERTS"
CERT_SHA="$(sed -n 's/.*certificate SHA-256 digest: \([0-9a-fA-F]*\).*/\1/p' <<<"$CERTS" | head -1 | tr 'A-F' 'a-f')"
[[ -n "$CERT_SHA" ]] || die "could not read the signing certificate digest"
if [[ -n "${DMGYM_CERT_SHA256:-}" ]]; then
  EXPECTED="$(tr -d ':[:space:]' <<<"$DMGYM_CERT_SHA256" | tr 'A-F' 'a-f')"
  if [[ "$CERT_SHA" != "$EXPECTED" ]]; then
    rm -f "$APK"
    die "signing certificate does not match DMGYM_CERT_SHA256: installed apps could not be updated"
  fi
  echo "Signing certificate matches DMGYM_CERT_SHA256."
else
  echo "Tip: export DMGYM_CERT_SHA256=$CERT_SHA to refuse APKs signed with another key."
fi
(cd "$DIST" && sha256sum "$(basename "$APK")" > "$(basename "$APK").sha256")
echo "Signed APK: $APK"

if [[ $DRY_RUN -eq 1 ]]; then
  step "Dry run: nothing pushed or published"
  exit 0
fi

step "Release notes"
if [[ -z "$NOTES_FILE" ]]; then
  NOTES_FILE="$WORK/notes.md"
  TARGET="$([[ $TAG_IS_NEW -eq 1 ]] && echo HEAD || echo "$TAG")"
  PREVIOUS="$(git describe --tags --abbrev=0 "$TARGET^" 2>/dev/null || true)"
  {
    if [[ -n "$PREVIOUS" ]]; then
      echo "Changes since $PREVIOUS:"
      echo
      git log --no-merges --pretty='- %s' "$PREVIOUS..$TARGET"
      echo
    fi
    echo "Signing certificate SHA-256: \`$CERT_SHA\`"
    echo
    echo "An update keeps your data only if it is signed with the same key as the installed app."
  } > "$NOTES_FILE"
fi
cat "$NOTES_FILE"

if [[ $ASSUME_YES -eq 0 ]]; then
  printf '\nPublish %s on GitHub%s? [y/N] ' "$TAG" \
    "$([[ $TAG_IS_NEW -eq 1 ]] && echo ' (the tag will be created and pushed)')"
  read -r answer
  [[ "$answer" == [yY] ]] || die "aborted"
fi

step "Publish"
[[ $TAG_IS_NEW -eq 0 ]] || git tag -a "$TAG" -m "DM Gym $VERSION"
git push --quiet origin "refs/tags/$TAG"
RELEASE_ARGS=(--title "DM Gym $VERSION" --notes-file "$NOTES_FILE" --verify-tag)
[[ $DRAFT -eq 0 ]] || RELEASE_ARGS+=(--draft)
[[ "$VERSION" != *-* ]] || RELEASE_ARGS+=(--prerelease)
gh release create "$TAG" "$APK" "$APK.sha256" "${RELEASE_ARGS[@]}"
echo "Published: $(gh release view "$TAG" --json url --jq .url)"
