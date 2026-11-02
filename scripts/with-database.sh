#!/usr/bin/env bash
set -eu
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
source ./scripts/database.sh
printf '%s\n' 'SEHATHARIAN_DATABASE_READY'
# Parent JVM holds stdin open; EOF stops the server through database.sh's EXIT trap.
cat >/dev/null
