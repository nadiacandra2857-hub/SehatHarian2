#!/usr/bin/env bash
set -eu
rombak_dir=$(mktemp -d /tmp/sehatharian-rombak-db-XXXXXX)
printf '%s\n' "$rombak_dir" > /tmp/sehatharian-rombak-db-path
mariadb-install-db --no-defaults --datadir="$rombak_dir" --auth-root-authentication-method=normal --skip-test-db > "$rombak_dir/install.log" 2>&1
exec mariadbd --no-defaults --datadir="$rombak_dir" --socket="$rombak_dir/server.sock" --pid-file="$rombak_dir/server.pid" --log-error="$rombak_dir/server.log" --bind-address=127.0.0.1 --port=13317
