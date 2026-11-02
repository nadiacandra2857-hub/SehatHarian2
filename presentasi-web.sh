#!/usr/bin/env bash
set -eu
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
export DB_HOST=127.0.0.1 DB_PORT=13306 DB_NAME=sehatharian_demo_db
export SEHATHARIAN_REPDB_DIR="$PWD/.runtime/repdb"
source scripts/database.sh
python3 scripts/demo/prepare.py --verify
printf '%s\n' 'PRESENTASI DATA SINTETIS — buka http://localhost:8088'
bash gradlew --offline -I scripts/demo/web.init.gradle :Frontend2:jproRun --no-daemon
