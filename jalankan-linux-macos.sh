#!/usr/bin/env bash
set -eu
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
printf '%s\n' 'SehatHarian - aplikasi desktop' 'Memerlukan JDK 27 dan internet untuk unduhan dependency pertama.'
if ! command -v java >/dev/null 2>&1; then
    printf '%s\n' 'Java tidak ditemukan. Pasang JDK 27 dan atur JAVA_HOME serta PATH.' >&2
    exit 1
fi
exec bash ./gradlew :Frontend2:run --console=plain "$@"
