#!/usr/bin/env bash
set -eu
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
mode=--dry-run
mode_selected=false
source=content/menus/awal-2026.json
while [[ $# -gt 0 ]]; do
    case "$1" in
        --dry-run|--apply)
            if $mode_selected; then echo 'Pilih satu mode impor saja.' >&2; exit 2; fi
            mode="$1"; mode_selected=true; shift ;;
        --source)
            if [[ $# -lt 2 || -z "$2" || "$2" == --* ]]; then
                echo '--source memerlukan path JSON paket.' >&2; exit 2
            fi
            source="$2"; shift 2 ;;
        *)
            echo 'Gunakan: bash impor-menu.sh [--dry-run|--apply] [--source path-paket.json]' >&2
            exit 2 ;;
    esac
done
# Provisioning stays in the launcher; this command never starts/seeds a database.
python3 scripts/prepare-menus.py --source "$source"
if [[ -z "${MENU_ADMIN_EMAIL:-}" ]]; then read -r -p 'Email admin: ' MENU_ADMIN_EMAIL; export MENU_ADMIN_EMAIL; fi
if [[ -z "${MENU_ADMIN_PASSWORD:-}" ]]; then read -rs -p 'Password admin: ' MENU_ADMIN_PASSWORD; echo; export MENU_ADMIN_PASSWORD; fi
if [[ "$mode" == --apply ]]; then
    bash gradlew :Backend2:importMenus -PapplyMenus --no-daemon
else
    bash gradlew :Backend2:importMenus --no-daemon
fi
unset MENU_ADMIN_PASSWORD
