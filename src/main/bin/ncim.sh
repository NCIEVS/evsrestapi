#!/bin/bash -f
#
# This is the wrapper script for deployment that uses NCIM to load
# various terminologies.  This may be called with parameters due to
# legacy Jenkins setup, but it ignores the parameters.
#

if [[ $# -ne 0 ]] && [ "x$1" != "x--download" ]; then
  echo "Usage: $0"
  echo "  Call without parameters, you are likely trying to call ncim-part.sh"
  exit 1
fi

echo "--------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "--------------------------------------------------"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo "  dir = $DIR"

echo "Wait for 8 seconds to start ..."
sleep 8

echo "Load ncim (download)"
$DIR/ncim-part.sh --download --keep > /tmp/$$.log 2>&1 
if [[ $? -ne 0 ]]; then
    cat /tmp/$$.log | sed 's/^/    /'
    echo "ERROR: loading ncim"
    exit 1
fi

# Keep LNC out of this for now.
for t in MDR ICD10CM ICD9CM LNC SNOMEDCT_US RADLEX PDQ ICD10 HL7V3.0; do
    # show memory usage
    free
    # Keep the NCIM folder around while we run
    echo "Load $t (from downloaded data)"
    $DIR/ncim-part.sh ./NCIM/META --keep --terminology $t
    if [[ $? -ne 0 ]]; then
        echo "ERROR: loading $t"
        exit 1
    fi
    # no ncim terminologies have content qa yet
    # cd src/main/bin
    # mkdir -p postman_content_qa
    # mac doesn't support bash 4 >:(
    # lowercase_t=$(echo "$t" | tr '[:upper:]' '[:lower:]')
    # ./postman.sh "${t_tolower}" > "postman_content_qa/${t_tolower}_postman_content_qa.txt"
    # cd ../../..
done

echo "Cleanup"
/bin/rm -rf ./NCIM /tmp/$$.log

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
