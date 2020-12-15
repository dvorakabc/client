#!/bin/bash

# Created by l1ving on 17/02/20
#
# ONLY USED IN AUTOMATED BUILDS
#
# Usage: "./runAutomatedRelease.sh <major or empty>"

_d="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."
source "$_d/scripts/utils.sh"
source $_d/../.profile

check_var "KAMI_DIR" "$KAMI_DIR" || exit $?
#check_var "KAMI_MIRROR_DIR" "$KAMI_MIRROR_DIR" || exit $?
check_var "KAMI_REPO" "$KAMI_REPO" || exit $?
#check_var "KAMI_REPO_NIGHTLY" "$KAMI_REPO_NIGHTLY" || exit $?
check_var "KAMI_OWNER" "$KAMI_OWNER" || exit $?

# Safely update repository
#cd "$KAMI_DIR" || exit $?
#git reset --hard HEAD
#check_git || exit $?
#OLD_COMMIT=$(git log --pretty=%h -1)

#git reset --hard origin/master || exit $?
#git pull || exit $?
#git submodule update --init --recursive || exit $?

# Update mirror
#cd "$KAMI_MIRROR_DIR" || exit $?
#git reset --hard master || exit $?
#git pull "$KAMI_DIR" || exit $?
#git submodule update --init --recursive || exit $?
#git push --force origin master || exit $?

cd "$KAMI_DIR" || exit $?

# Set some variables, run scripts
HEAD=$(git log --pretty=%h -1)
#CHANGELOG="$("$_d"/scripts/changelog.sh "$OLD_COMMIT")" || exit $?
VERSION="$("$_d"/scripts/version.sh)" || exit $?
#VERSION_MAJOR="$("$_d"/scripts/version.sh "major")" || exit $?
"$_d"/scripts/bumpVersion.sh "$1" || exit $?
JAR_NAME="$("$_d"/scripts/buildJarSafe.sh)" || exit $?

"$_d"/scripts/uploadRelease.sh "$1" "$HEAD" "$VERSION" "$JAR_NAME" || exit $?
