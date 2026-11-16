#!/bin/bash
# Compatibility bootstrap for one coordinated release; also used by make devreset.
# Usage: ops-forward.sh [--build-loader] [--quiet] [script [args...]]
OPS_FORWARD_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
build_loader=0
quiet=0
while [[ "${1:-}" == --build-loader || "${1:-}" == --quiet ]]; do
  if [[ "$1" == --build-loader ]]; then build_loader=1; else quiet=1; fi
  shift
done
script=${1:-}
[[ $# -eq 0 ]] || shift
no_config=0
for arg in "$@"; do [[ "$arg" != --noconfig ]] || no_config=1; done
if [[ $no_config == 0 ]]; then
  app_home="${APP_HOME:-/local/content/evsrestapi}"
  config_file="$app_home/config/setenv.sh"
  if [[ -n "${APP_NAME:-}" && -f "$app_home/$APP_NAME/config/setenv.sh" ]]; then
    config_file="$app_home/$APP_NAME/config/setenv.sh"
  fi
  if [[ -f "$config_file" ]]; then
    case "$script" in
      # These always used only the caller's environment; read just the operations location.
      devreset.sh | load.sh)
        [[ -n "${EVS_OPS_HOME:-}" ]] ||
          EVS_OPS_HOME="$(source "$config_file" >/dev/null 2>&1; printf '%s' "${EVS_OPS_HOME:-}")"
        ;;
      *)
        source "$config_file" || exit 1
        export OPS_CONFIG_ENV_FILE="$config_file"
        ;;
    esac
  fi
fi
# Without EVS_OPS_HOME, use a checkout next to this one, then the standard installation.
if [[ -z "${EVS_OPS_HOME:-}" ]]; then
  for candidate in "$OPS_FORWARD_DIR/../../../../evsrestapi-operations" /local/content/evsrestapi-operations; do
    if [[ -f "$candidate/bin/ops-common.sh" ]]; then
      EVS_OPS_HOME="$(cd "$candidate" && pwd)"
      break
    fi
  done
fi
export EVS_OPS_HOME
if [[ ! -f "${EVS_OPS_HOME:-}/bin/${script:-ops-common.sh}" ]]; then
  echo "ERROR: evsrestapi-operations not found${EVS_OPS_HOME:+ at $EVS_OPS_HOME}." \
    "Set EVS_OPS_HOME in setenv.sh, or clone evsrestapi-operations next to this checkout." >&2
  exit 1
fi
if [[ $build_loader == 1 && -f "$EVS_OPS_HOME/web/gradlew" ]]; then
  source "$EVS_OPS_HOME/bin/ops-common.sh"
  bash "$EVS_OPS_HOME/web/gradlew" -p "$EVS_OPS_HOME/web" loaderJar || exit $?
fi
[[ -n "$script" ]] || exit 0
[[ $quiet == 1 ]] || echo "Deprecated: use $EVS_OPS_HOME/bin/$script" >&2
exec "${OPS_BASH:-bash}" "$EVS_OPS_HOME/bin/$script" "$@"
