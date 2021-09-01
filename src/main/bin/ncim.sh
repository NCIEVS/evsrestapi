#!/bin/sh -f

config=1
while [[ "$#" -gt 0 ]]; do case $1 in
  --noconfig) config=0;;
  *) arr=( "${arr[@]}" "$1" );;
esac; shift; done


if [ ${#arr[@]} -ne 1 ]; then
  echo "Usage: $0 [--noconfig] <dir>"
  echo "  e.g. $0 /data/evs/ncim"
  echo "  e.g. $0 --noconfig /data/evs/ncim"
  exit 1
fi

dir=${arr[0]}
terminology=ncim

echo "--------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "--------------------------------------------------"
echo "terminology = $terminology"
echo "dir = $dir"
echo ""
#set -e

# Setup configuration
echo "  Setup configuration"
if [[ $config -eq 1 ]]; then
    APP_HOME=/local/content/evsrestapi
    CONFIG_DIR=${APP_HOME}/${APP_NAME}/config
    CONFIG_ENV_FILE=${CONFIG_DIR}/setenv.sh
    echo "    config = $CONFIG_ENV_FILE"
    . $CONFIG_ENV_FILE
elif [[ -z $STARDOG_HOST ]]; then
    echo "ERROR: STARDOG_HOST is not set"
    exit 1
elif [[ -z $STARDOG_PORT ]]; then
    echo "ERROR: STARDOG_PORT is not set"
    exit 1
elif [[ -z $STARDOG_USERNAME ]]; then
    echo "ERROR: STARDOG_USERNAME is not set"
    exit 1
elif [[ -z $STARDOG_PASSWORD ]]; then
    echo "ERROR: STARDOG_PASSWORD is not set"
    exit 1
elif [[ -z $ES_SCHEME ]]; then
    echo "ERROR: ES_SCHEME is not set"
    exit 1
elif [[ -z $ES_HOST ]]; then
    echo "ERROR: ES_HOST is not set"
    exit 1
elif [[ -z $ES_PORT ]]; then
    echo "ERROR: ES_PORT is not set"
    exit 1
fi

# set the max number of fields higher
# we can probably remove this when we figure a better answer
echo "  Set index.mapping.total_fields.limit = 5000"  
curl -s -X PUT "$ES_SCHEME://$ES_HOST:$ES_PORT/evs_metadata/_settings" \
        -H "Content-type: application/json" -d '{ "index.mapping.total_fields.limit": 5000 }' >> /dev/null
if [[ $? -ne 0 ]]; then
    echo "ERROR: unexpected error setting index.mapping.total_fields in evs_metadata"
    exit 1
fi

# Handle the local setup
echo ""
export PATH="/usr/local/jdk1.8/bin/:$PATH"
local=""
jar="../lib/evsrestapi.jar"
if [[ $config -eq 0 ]]; then
    local="-Dspring.profiles.active=local"
    jar=build/libs/`ls build/libs/ | grep evsrestapi | grep jar | head -1`
fi
export EVS_SERVER_PORT="8083"

# Run reindexing process (choose a port other than the one that it runs on)
echo "  Generate indexes"
echo "java $local -jar $jar --terminology $terminology -d $dir --forceDeleteIndex"
java $local -jar $jar --terminology $terminology -d $dir --forceDeleteIndex
if [[ $? -ne 0 ]]; then
    echo "ERROR: unexpected error building indexes"
    exit 1
fi

echo "  Remove old version indexes"
version=`grep umls.release.name $dir/release.dat | perl -pe 's/.*=//; s/\r//;'`
curl -s $ES_SCHEME://$ES_HOST:$ES_PORT/_cat/indices | perl -pe 's/^.* open ([^ ]+).*/$1/' | grep -v $version | grep ${terminology}_ > /tmp/x.$$
for i in `cat /tmp/x.$$`; do
    echo "    delete $i"
    curl -s -X DELETE https://$ES_HOST:$ES_PORT/$i
    if [[ $? -ne 0 ]]; then
        echo "ERROR: problem deleting https://$ES_HOST:$ES_PORT/$i"
        exit 1
    fi
    curl -s -X DELETE https://$ES_HOST:$ES_PORT/evs_metadata/_doc/$i
    if [[ $? -ne 0 ]]; then
        echo "ERROR: problem deleting https://$ES_HOST:$ES_PORT/evs_metadata/_doc/$i"
        exit 1
    fi
done
/bin/rm -f /tmp/x.$$

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
