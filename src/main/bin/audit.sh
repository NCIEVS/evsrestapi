#!/bin/bash -f
#
# This script produces audit reports by querying Elasticsearch for evs_audit data.
#

config=1
help=0
arr=()

while [[ "$#" -gt 0 ]]; do
  case $1 in
  --help) help=1 ;;
  --noconfig)
    config=0
    ncflag="--noconfig"
    ;;
  *) arr+=("$1") ;;
  esac
  shift
done

print_help(){
  echo "Usage: $0 [--noconfig] [--help] <report: load|error|warning> [terminology]"
  echo "  e.g. $0 load"
  echo "  e.g. $0 error ncit"
  echo "  e.g. $0 warning"
  exit 1
}

if [[ $help -eq 1 || ${#arr[@]} -eq 0 ]]; then
  print_help
fi

report_type=${arr[0]}
terminology=${arr[1]}

setup_configuration() {
  if [[ $config -eq 1 ]]; then
    # Set directory of this script so we can call relative scripts
    DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
    if [[ "$DIR" == /cygdrive/* ]]; then DIR=$(echo "$DIR" | sed 's|^/cygdrive/\([a-zA-Z]\)/\(.*\)|\1:/\2|'); fi
    APP_HOME="${APP_HOME:-/local/content/evsrestapi}"
    CONFIG_DIR=${APP_HOME}/config
    CONFIG_ENV_FILE=${CONFIG_DIR}/setenv.sh
    if [[ -e $CONFIG_ENV_FILE ]]; then
      . $CONFIG_ENV_FILE
    else
      echo "    ERROR: $CONFIG_ENV_FILE does not exist, consider using --noconfig"
      exit 1
    fi
  fi
}

validate_setup() {
  if [[ -z "$ES_SCHEME" || -z "$ES_HOST" || -z "$ES_PORT" ]]; then
    # Try default if not set
    ES_SCHEME=${ES_SCHEME:-http}
    ES_HOST=${ES_HOST:-localhost}
    ES_PORT=${ES_PORT:-9201}
  fi
  ES="${ES_SCHEME}://${ES_HOST}:${ES_PORT}"
}

# Verify jq installed
jq --help >>/dev/null 2>&1
if [[ $? -ne 0 ]]; then
  echo "ERROR: jq is required for this script."
  exit 1
fi

setup_configuration
validate_setup

query_es() {
    local q=$1
    curl -s -G "$ES/evs_audit/_search" --data-urlencode "size=1000" --data-urlencode "q=$q"
}

case $report_type in
  load)
    printf "Terminology\tVersion\tElapsed Time (ms)\tCount\tDate\n"
    q="process:(MetaOpensearchLoadServiceImpl OR MetaSourceOpensearchLoadServiceImpl OR LoaderServiceImpl) AND NOT elapsedTime:0"
    if [[ -n $terminology ]]; then
        q="$q AND terminology:$terminology"
    fi
    query_es "$q" | jq -r '.hits.hits[] | [._source.terminology, ._source.version, ._source.elapsedTime, ._source.count, (._source.date // ._source.startDate)] | @tsv'
    ;;
  error)
    printf "Terminology\tVersion\tProcess\tDetails\tDate\n"
    q="logLevel:ERROR"
    if [[ -n $terminology ]]; then
        q="$q AND terminology:$terminology"
    fi
    query_es "$q" | jq -r '.hits.hits[] | [._source.terminology, ._source.version, ._source.process, ._source.details, ._source.date] | @tsv'
    ;;
  warning)
    printf "Terminology\tVersion\tProcess\tDetails\tDate\n"
    q="logLevel:WARN"
    if [[ -n $terminology ]]; then
        q="$q AND terminology:$terminology"
    fi
    query_es "$q" | jq -r '.hits.hits[] | [._source.terminology, ._source.version, ._source.process, ._source.details, ._source.date] | @tsv'
    ;;
  *)
    echo "ERROR: Unknown report type: $report_type"
    print_help
    ;;
esac
