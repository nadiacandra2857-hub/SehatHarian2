#!/usr/bin/env bash
set -eu
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
export DB_HOST="${DB_HOST:-127.0.0.1}" DB_PORT="${DB_PORT:-13306}" DB_NAME="${SEHATHARIAN_PRESENTATION_TEST_DB:-sehatharian_demo_db}"
export SEHATHARIAN_REPDB_DIR="${SEHATHARIAN_REPDB_DIR:-$PWD/.runtime/repdb}"
# Runtime assets are intentionally not distributed in Git; prepare before web startup.
if [[ ! -r "$SEHATHARIAN_REPDB_DIR/catalog.xml" ]]; then
    python3 scripts/prepare-repdb.py --limit 24 --output "$SEHATHARIAN_REPDB_DIR"
fi
if [[ -n "${SEHATHARIAN_PRESENTATION_TEST_DB:-}" ]]; then
    [[ "$DB_NAME" == sehatharian_test_* ]] || { echo 'Uji tulis wajib memakai sehatharian_test_*.' >&2; exit 1; }
fi
# Presentation never creates a fresh database implicitly.
if [[ "${SEHATHARIAN_DEMO_EXTERNAL:-0}" != 1 && ! -d ".runtime/database/$DB_NAME" ]]; then
    printf '%s\n' 'Database demo belum tersedia pada checkout ini. Gunakan server demo yang sudah disiapkan; jangan menjalankan seed pengguna.' >&2
    exit 1
fi
if [[ "${SEHATHARIAN_DEMO_EXTERNAL:-0}" != 1 ]]; then
    source scripts/database.sh
fi
if [[ -z "${SEHATHARIAN_PRESENTATION_TEST_DB:-}" ]]; then
    python3 scripts/demo/prepare.py --verify
else
    printf 'UJI TERISOLASI — database %s; tanpa seed/bootstrap\n' "$DB_NAME"
fi
printf '%s\n' 'PRESENTASI DATA SINTETIS — buka http://localhost:8088'
bash gradlew --offline -I scripts/demo/web.init.gradle :Frontend2:jproRun --no-daemon
