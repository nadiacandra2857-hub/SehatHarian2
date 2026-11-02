#!/usr/bin/env bash
set -eu
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
source ./scripts/database.sh
bash ./gradlew :Backend2:createAdmin --no-daemon
