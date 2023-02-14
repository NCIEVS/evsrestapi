#!/bin/sh -f
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
$DIR/ncim-part.sh --download --keep | sed 's/^/    /'
if [[ $? -ne 0 ]]; then
    echo "ERROR: loading ncim"
    exit 1
fi

for t in MDR ICD10CM ICD9CM LNC SNOMEDCT_US; do

    echo "Load $t (from downloaded data)"
    $DIR/ncim-part.sh ./NCIM/META --terminology $t | sed 's/^/    /'
    if [[ $? -ne 0 ]]; then
        echo "ERROR: loading $t"
        exit 1
    fi
done

echo "Cleanup"
/bin/rm -rf ./NCIM

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
