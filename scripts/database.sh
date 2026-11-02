#!/usr/bin/env bash
# Dipanggil dengan source oleh peluncur desktop.
export DB_HOST="${DB_HOST:-127.0.0.1}"
export DB_PORT="${DB_PORT:-13306}"
export DB_NAME="${DB_NAME:-sehatharian_db}"
export DB_USER="${DB_USER:-root}"
export DB_PASSWORD="${DB_PASSWORD:-}"

# Konfigurasi server eksternal tidak dikelola oleh peluncur.
if [[ "$DB_HOST" != 127.0.0.1 || "$DB_PORT" != 13306 ]]; then
    return
fi
for tool in mariadbd mariadb-install-db mariadb-admin; do
    command -v "$tool" >/dev/null || { echo "Program $tool tidak tersedia. MariaDB diperlukan untuk database lokal." >&2; exit 1; }
done
DB_DIR="$PWD/.runtime/database"
mkdir -p "$DB_DIR"
# Socket pendek menghindari batas panjang path socket Unix.
DB_SOCKET="/tmp/sehatharian-${UID}-$(printf '%s' "$PWD" | cksum | cut -d ' ' -f 1).sock"
if mariadb-admin --no-defaults --socket="$DB_SOCKET" -u root ping >/dev/null 2>&1; then
    return
fi
if [[ ! -d "$DB_DIR/mysql" ]]; then
    echo 'Menyiapkan penyimpanan database lokal (hanya pertama kali)...'
    mariadb-install-db --no-defaults --datadir="$DB_DIR" --auth-root-authentication-method=normal --skip-test-db >"$DB_DIR/install.log" 2>&1 || { cat "$DB_DIR/install.log" >&2; exit 1; }
fi
echo 'Menyalakan database SehatHarian...'
mariadbd --no-defaults --datadir="$DB_DIR" --socket="$DB_SOCKET" \
    --pid-file="$DB_DIR/server.pid" --log-error="$DB_DIR/server.log" \
    --bind-address=127.0.0.1 --port="$DB_PORT" &
DB_PROCESS=$!
stop_database() {
    kill "$DB_PROCESS" 2>/dev/null || true
    wait "$DB_PROCESS" 2>/dev/null || true
}
trap stop_database EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
for ((attempt=0; attempt<60; attempt++)); do
    if mariadb-admin --no-defaults --socket="$DB_SOCKET" -u root ping >/dev/null 2>&1; then
        return
    fi
    if ! kill -0 "$DB_PROCESS" 2>/dev/null; then
        cat "$DB_DIR/server.log" >&2
        exit 1
    fi
    sleep 0.5
done
echo "Database belum siap. Periksa $DB_DIR/server.log" >&2
exit 1
