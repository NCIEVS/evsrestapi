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
  echo "  e.g. $0 ncim 202102"
  exit 1
fi

terminology=${arr[0]}
version=${arr[1]}

echo "--------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "--------------------------------------------------"
echo "terminology = $terminology"
echo "verion = $version"
echo ""

echo "  Remove indexes for $terminology $version"
curl -s $ES_SCHEME://$ES_HOST:$ES_PORT/_cat/indices | perl -pe 's/^.* open ([^ ]+).*/$1/; s/\r//;' |\
  grep $version | grep ${terminology}_ | cat > /tmp/x.$$
if [[ $? -ne 0 ]]; then
    echo "ERROR: unexpected error looking up indices for $terminology $version"
    exit 1
fi

ct=`cat /tmp/x.$$ | wc -l`
if [[ $ct -eq 0 ]]; then
    echo "    NO indexes to delete"
fi
for i in `cat /tmp/x.$$`; do
    echo "    delete $i"
    curl -s -X DELETE $ES_SCHEME://$ES_HOST:$ES_PORT/$i > /tmp/x.$$
    if [[ $? -ne 0 ]]; then
        echo "ERROR: unexpected error removing index $i"
        exit 1
    fi
done

echo "  Remove ${terminology}_$version from evs_metadata"
curl -s -X DELETE $ES_SCHEME://$ES_HOST:$ES_PORT/evs_metadata/_doc/concept_${terminology}_$version > /tmp/x.$$
if [[ $? -ne 0 ]]; then
    echo "ERROR: unexpected error removing concept_${terminology}_$version from evs_metadata index"
    exit 1
fi
ct=`grep 'not_found' /tmp/x.$$ | wc -l`
if [[ $ct -ne 0 ]]; then
    echo "    NO matching evs_metadata entry"
fi

# cleanup
/bin/rm -f /tmp/x.$$

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
