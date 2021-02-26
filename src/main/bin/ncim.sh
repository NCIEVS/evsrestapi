#!/bin/sh -f

terminology=ncim
echo "--------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "--------------------------------------------------"
echo "terminology = $terminology"
set -e

# Setup configuration
echo "  Setup configuration"
APP_HOME=/local/content/evsrestapi
CONFIG_DIR=${APP_HOME}/${APP_NAME}/config
CONFIG_ENV_FILE=${CONFIG_DIR}/setenv.sh
echo "    config = $CONFIG_EVS_FILE"
. $CONFIG_ENV_FILE
echo "dir = $DATA_DIR"

if [[ -z $DATA_DIR ]]; then
    echo "ERROR: DATA_DIR must be set"
    exit 1
fi
if [[ ! -e $DATA_DIR ]]; then
    echo "ERROR: DATA_DIR must exist"
    exit 1
fi

# Run reindexing process (choose a port other than the one that it runs on)
echo "  Generate indexes"
export EVS_SERVER_PORT="8082"
/usr/local/jdk1.8/bin/java -jar ../lib/evsrestapi.jar --terminology $terminology -d $DATA_DIR --forceDeleteIndex | sed 's/^/    /'

echo "  Remove old version indexes = $ES_CLEAN"
if [[ $ES_CLEAN == "true" ]]; then
    fv=`echo $version | perl -pe 's/\.//;'`
    curl -s $ES_SCHEME://$ES_HOST:$ES_PORT/_cat/indices | perl -pe 's/^.* open ([^ ]+).*/$1/' | grep -v $version | grep -v $fv | grep ${terminology}_ > /tmp/x.$$
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
fi

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
