#!/bin/bash

# Created by l1ving on 17/11/20
#
# echos the current version.
#
# Usage: "./version.sh <major or empty> <simple or empty>"
#
# Major overrides Simple
# Version spec for major:  R.MM.01
# Version spec for beta:   R.MM.DD-hash
# Version spec for simple: R.MM.DD
#
# Example beta: 1.11.17-58a47a2f

__utils="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/utils.sh"
source "$__utils"

check_git || exit $?

version=$(<version.txt)

CUR_HASH="-"$(git log --pretty=%h -1) # for the -hash
CUR_R="beta."$version    # Current year - 2019

echo "$CUR_R$CUR_HASH"
