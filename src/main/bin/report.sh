#!/bin/bash
# Compatibility entry point for one coordinated release.
DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$DIR/ops-forward.sh" "report.sh" "$@"
