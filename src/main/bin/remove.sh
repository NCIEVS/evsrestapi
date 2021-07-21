#!/bin/sh -f
#
# Used to remove indexes for a particular terminology/version.
#
help=0
while [[ "$#" -gt 0 ]]; do case $1 in
  --help) help=1;;
  *) arr=( "${arr[@]}" "$1" );;
esac; shift; done

if [ ${#arr[@]} -ne 2 ]; then
  echo "Usage: $0 <terminology> <version>"
  echo "  e.g. $0 ncit 20.09d"
  exit 1
fi

terminology=${arr[0]}
version=${arr[1]}

echo "--------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "--------------------------------------------------"
# Stop on any error
set -e

echo "  Remove index for $terminology $version"
curl -s $ES_SCHEME://$ES_HOST:$ES_PORT/_cat/indices | perl -pe 's/^.* open ([^ ]+).*/$1/' |\
   grep $version | grep ${terminology}_ > /tmp/x.$$
for i in `cat /tmp/x.$$`; do
    echo "    delete $i"
    curl -s -X DELETE https://$ES_HOST:$ES_PORT/$i
    #curl -s -X DELETE https://$ES_HOST:$ES_PORT/evs_metadata/_doc/$i
    curl -s -X DELETE https://$ES_HOST:$ES_PORT/evs_metadata/$i
done

/bin/rm -f /tmp/x.$$

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
