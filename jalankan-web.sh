#!/usr/bin/env bash
set -eu
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
# Protect a running instance before provisioning or JPro's automatic PID replacement.
if [[ -r Frontend2/RUNNING_PID ]]; then
    read -r sehatharian_running_pid < Frontend2/RUNNING_PID || true
    if [[ "${sehatharian_running_pid:-}" =~ ^[0-9]+$ ]] && kill -0 "$sehatharian_running_pid" 2>/dev/null; then
        printf '%s\n' 'JPro masih berjalan di direktori ini. Gunakan salinan folder terpisah untuk instance paralel; proses lama dipertahankan.' >&2
        exit 1
    fi
fi
source scripts/database.sh
bash gradlew :Backend2:setupDatabase --no-daemon
if [[ -r datashetmakanan/tempolife-foods.csv ]]; then
    python3 scripts/prepare-foods.py
    bash gradlew :Backend2:importFoods --no-daemon
fi
printf '%s\n' "Buka http://localhost:${JPRO_PORT:-8088} — pertahankan terminal ini selama demo."
bash gradlew -I scripts/demo/web.init.gradle :Frontend2:jproRun --no-daemon
