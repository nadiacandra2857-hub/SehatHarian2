#!/usr/bin/env bash
# TEST ONLY: no database startup/provisioning. Requires local RepDB preparation.
set -eu
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
export DB_HOST=127.0.0.1 DB_PORT=1
export SEHATHARIAN_REPDB_DIR="$PWD/.runtime/repdb"
python3 scripts/verify-source-tests.py --output /tmp/ui3-check
python3 scripts/build-ui3-harness.py
bash gradlew --offline -I scripts/visual/ui3-jpro.init.gradle :Frontend2:jproRun --no-daemon
