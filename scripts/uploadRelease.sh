#!/bin/bash

# Created by l1ving on 17/02/20
#
# ONLY USED IN AUTOMATED BUILDS
#
# Usage: "./uploadRelease.sh <major or empty> <branch or commit> <version> <jar name> <changelog>"

__scripts="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$__scripts/utils.sh" # include check_var
source "$__scripts/../../.profile"

check_var "2" "$2" || exit $?
check_var "3" "$3" || exit $?
#check_var "KAMI_REPO_NIGHTLY" "$KAMI_REPO_NIGHTLY" || exit $?
check_var "GITHUB_TOKEN" "$GITHUB_TOKEN" || exit $?
check_var "KAMI_DIR" "$KAMI_DIR" || exit $?

cd "$KAMI_DIR" || exit $?
BRANCH="$(git symbolic-ref -q HEAD | sed "s/^refs\/heads\///g")"
REPO="$KAMI_REPO"

check_var "git symbolic-ref -q HEAD" "$BRANCH" || exit $?

# Create release
# shellcheck disable=SC2001
CHANGELOG="$(echo "$5" | sed "s/[\"';]//g")"
curl -s -H "Authorization: token $GITHUB_TOKEN" -X POST --data "$(generate_release_data "$KAMI_OWNER" "$REPO" "$3" "$BRANCH" "$3" "" "false" "false")" "https://api.github.com/repos/$KAMI_OWNER/$REPO/releases" || exit $?

# Upload jar to release
"$__scripts/uploadReleaseAsset.sh" github_api_token="$GITHUB_TOKEN" owner="$KAMI_OWNER" repo="$REPO" tag="$3" filename="$KAMI_DIR/build/libs/$4"

echo '{ 
  "stable": 
  {
    "name": "'$3'",
    "url": "https://github.com/'"$KAMI_OWNER/$REPO/releases/download/$3/NECRON-$3.jar"'"
  }
}' > "latest.json"
"$_d"/scripts/incrementVersion.sh "$VERSION"

git commit -m 'update version' ./latest.json ./src/main/java/me/zeroeightsix/kami/NecronClient.java ./gradle.properties
git push