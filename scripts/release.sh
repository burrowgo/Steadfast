#!/usr/bin/env bash
# Steadfast — Release Helper Script
# Usage: ./scripts/release.sh 0.7.2
#        ./scripts/release.sh v0.7.2

set -euo pipefail

if [ $# -eq 0 ]; then
  echo "Usage: $0 <version> (e.g. 0.7.2 or v0.7.2)"
  exit 1
fi

RAW_VERSION="$1"
CLEAN_VERSION="${RAW_VERSION#v}"
TAG="v$CLEAN_VERSION"

echo "=================================================="
echo " Preparing Steadfast Release: $TAG"
echo "=================================================="

# 1. Ensure working directory is clean
if [ -n "$(git status --porcelain)" ]; then
  echo "❌ Error: Working tree has uncommitted changes. Please commit or stash them first."
  git status -s
  exit 1
fi

# 2. Ensure current branch is master/main
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "Current branch: $CURRENT_BRANCH"

# 3. Check if tag already exists locally or remotely
if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "❌ Error: Tag '$TAG' already exists locally."
  exit 1
fi

if git ls-remote --tags origin "$TAG" | grep -q "$TAG"; then
  echo "❌ Error: Tag '$TAG' already exists on remote origin."
  exit 1
fi

# 4. Run tests and lint checks locally
echo "Running local unit tests & lint..."
JAVA_HOME="${JAVA_HOME:-/home/sangharsha/.sdkman/candidates/java/21.0.12+1.1-tem}"
PATH="$JAVA_HOME/bin:$PATH" ./gradlew testDebugUnitTest lintDebug

# 5. Create annotated git tag
echo "Creating git tag $TAG..."
git tag -a "$TAG" -m "Steadfast Release $TAG"

# 6. Push commit and tag to origin
echo "Pushing $CURRENT_BRANCH and tag $TAG to origin..."
git push origin "$CURRENT_BRANCH"
git push origin "$TAG"

echo "=================================================="
echo " ✅ Successfully pushed $TAG!"
echo " GitHub Actions is now building and publishing:"
echo "   • steadfast-$TAG-release.apk"
echo "   • steadfast-$TAG-debug.apk"
echo "   • steadfast-$TAG-release.aab"
echo "   • Checksums and Release notes"
echo "=================================================="
