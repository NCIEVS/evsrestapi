#!/bin/bash -f
#
# This script produces audit reports by querying Elasticsearch for evs_audit data.
#

config=1
help=0
arr=()
output_fmt="tsv"
output_to_file=0

while [[ "$#" -gt 0 ]]; do
  case $1 in
  -h | --help) help=1 ;;
  -n | --noconfig)
    config=0
    ncflag="--noconfig"
    ;;
  -v | --tsv)
    output_to_file=1
    output_fmt="tsv"
    ;;
  -c | --csv)
    output_to_file=1
    output_fmt="csv"
    ;;
  -r | --recent)
    recent=1
    ;;
  -T | --terminology)
    shift
    terminology=$1
    ;;
  *) arr+=("$1") ;;
  esac
  shift
done

print_help() {
  echo "Audit Script - Query Elasticsearch for audit data"
  echo ""
  echo "Usage: $0 [options] <report_type> [terminology]"
  echo ""
  echo "Report Types (Required):"
  echo "  load                Terminology loading and indexing metrics"
  echo "  error               Audit records with ERROR log level"
  echo "  warning             Audit records with WARN log level"
  echo "  all                 All audit records"
  echo ""
  echo "Options:"
  echo "  -h, --help          Show this help message"
  echo "  -r, --recent        Limit results to the last 24 hours"
  echo "  -n, --noconfig      Skip sourcing /local/content/evsrestapi/config/setenv.sh"
  echo "  -T, --terminology   Filter by terminology (can also be 2nd positional argument)"
  echo ""
  echo "Output Control (Default is console):"
  echo "  -c, --csv           Generate a CSV file in audit_reports/"
  echo "  -v, --tsv           Generate a TSV file in audit_reports/"
  echo ""
  echo "Examples:"
  echo "  $0 load                      # Load report to console"
  echo "  $0 --csv load ncit           # CSV report for NCIt"
  echo "  $0 -r -c all                 # Recent records for all types in CSV"
  echo "  $0 all -T medrt              # Use explicit flag for terminology"
  exit 1
}

if [[ $help -eq 1 || ${#arr[@]} -eq 0 ]]; then
  print_help
fi

report_type=${arr[0]}
# Use positional terminology ONLY if not already set by -T flag
terminology=${terminology:-${arr[1]}}


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
    # Use -f to fail on HTTP errors and -S to show error on stderr
    if ! curl -s -S -f -G "$ES/evs_audit/_search" --data-urlencode "size=1000" --data-urlencode "q=$q"; then
      echo "ERROR: Elasticsearch query failed." >&2
      exit 1
    fi
}


# Output handling
if [[ $output_to_file -eq 1 ]]; then
    mkdir -p audit_reports
    timestamp=$(date +"%Y%m%d_%H%M%S")
    output_file="audit_reports/${report_type}_${timestamp}.${output_fmt}"
    exec > "$output_file"
    # Note: We use tsv internally for jq then convert to csv if needed
fi

case $report_type in
  load)
    header="Terminology\tVersion\tElapsed Time\tCount\tDate"
    q="process:(MetaOpensearchLoadServiceImpl OR MetaSourceOpensearchLoadServiceImpl OR LoaderServiceImpl OR GraphOpensearchLoadServiceImpl) AND NOT elapsedTime:0"
    filter='def f: . as $ms | if $ms < 1000 then ($ms|tostring)+"ms" else ($ms/1000|floor) as $s | ($s/3600|floor) as $h | (($s%3600)/60|floor) as $m | ($s%60) as $sec | (if $h>0 then ($h|tostring)+"h " else "" end) + (if $m>0 or $h>0 then ($m|tostring)+"m " else "" end) + ($sec|tostring)+"s" end; .hits.hits[] | [._source.terminology, ._source.version, (._source.elapsedTime | f), ._source.count, (._source.date // ._source.startDate)] | @tsv'
    ;;
  error)
    header="Terminology\tVersion\tProcess\tDetails\tDate"
    q="logLevel:ERROR"
    filter='.hits.hits[] | [._source.terminology, ._source.version, ._source.process, ._source.details, ._source.date] | @tsv'
    ;;
  warning)
    header="Terminology\tVersion\tProcess\tDetails\tDate"
    q="logLevel:WARN"
    filter='.hits.hits[] | [._source.terminology, ._source.version, ._source.process, ._source.details, ._source.date] | @tsv'
    ;;
  all)
    header="Type\tTerminology\tVersion\tProcess\tLogLevel\tDetails\tCount\tElapsed\tDate"
    q="*:*"
    filter='def f: . as $ms | if $ms < 1000 then ($ms|tostring)+"ms" else ($ms/1000|floor) as $s | ($s/3600|floor) as $h | (($s%3600)/60|floor) as $m | ($s%60) as $sec | (if $h>0 then ($h|tostring)+"h " else "" end) + (if $m>0 or $h>0 then ($m|tostring)+"m " else "" end) + ($sec|tostring)+"s" end; .hits.hits[] | [._source.type, ._source.terminology, ._source.version, ._source.process, ._source.logLevel, ._source.details, ._source.count, (._source.elapsedTime | f), (._source.date // ._source.startDate)] | @tsv'
    ;;
  *)
    echo "ERROR: Unknown report type: $report_type"
    print_help
    ;;
esac
if [[ -n $terminology ]]; then
    q="$q AND terminology:$terminology"
fi


# recent flag restricts only to audits that happened in the last 24 hours
if [[ $recent -eq 1 ]]; then
    start_date=$(date -d '24 hours ago' +%Y-%m-%dT%H:%M:%SZ)
    end_date=$(date +%Y-%m-%dT%H:%M:%SZ)
    q="$q AND (date:[$start_date TO $end_date] OR startDate:[$start_date TO $end_date])"
fi

data=$(query_es "$q" | jq -r "$filter")

# Final output with format conversion if needed
if [[ -n "$data" ]]; then
    if [[ "$output_fmt" == "csv" ]]; then
        echo -e "$header" | sed 's/\t/,/g'
        echo -e "$data" | sed 's/\t/,/g'
    else
        echo -e "$header"
        echo -e "$data"
    fi
else
    echo "No records found for report: $report_type${terminology:+ for terminology: $terminology}${recent:+ (last 24 hours)}"
fi

if [[ $output_to_file -eq 1 ]]; then
    # Close file and notify on stderr so it shows in console
    exec >&2
    echo "Report generated: $output_file"
fi
