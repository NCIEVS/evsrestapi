#!/bin/sh -f

config=1
download=0
while [[ "$#" -gt 0 ]]; do case $1 in
  --noconfig) config=0;;
  --download) download=1;;
  *) arr=( "${arr[@]}" "$1" );;
esac; shift; done

ok=0
if [ ${#arr[@]} -eq 1 ]; then
  ok=1
elif [ ${#arr[@]} -eq 0 ] && [ $download -eq 1 ]; then
  ok=1
fi
if [ $ok -eq 0 ]; then
  echo "Usage: $0 [--noconfig] [--download] [<dir>]"
  echo "  e.g. $0 /data/evs/ncim"
  echo "  e.g. $0 --noconfig /data/evs/ncim"
  echo "  e.g. $0 --download"
  echo "  e.g. $0 --noconfig --download"
  exit 1
fi

if [ ${#arr[@]} -ne 1 ]; then
  dir=${arr[0]}
fi
terminology=ncim

echo "--------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "--------------------------------------------------"
echo "terminology = $terminology"
echo "dir = $dir"
echo "config = $config"
if [[ $download -eq 1 ]]; then
  echo "download = $DOWNLOAD_DIR"
else
  echo "download = $download"
fi
echo ""

# Setup configuration
echo "  Setup configuration"
if [[ $config -eq 1 ]]; then
    APP_HOME=/local/content/evsrestapi
    CONFIG_DIR=${APP_HOME}/${APP_NAME}/config
    CONFIG_ENV_FILE=${CONFIG_DIR}/setenv.sh
    echo "    config = $CONFIG_ENV_FILE"
    . $CONFIG_ENV_FILE
    if [[ $? -ne 0 ]]; then
        echo "ERROR: $CONFIG_ENV_FILE does not exist or has a problem"
        exit 1
    fi
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
elif [[ -z $DOWNLOAD_DIR ]]; then
	export DOWNLOAD_DIR=.
fi

# Check if downloading NCIM data
if [[ $download -eq 1 ]]; then
 
    if [[ ! -e $DOWNLOAD_DIR ]]; then
        echo "ERROR: \$DOWNLOAD_DIR does not exist = $DOWNLOAD_DIR"
        exit 1
    fi

    echo "  Cleanup download directory"
    /bin/rm -rf $DOWNLOAD_DIR/Metathesaurus.RRF.zip $DOWNLOAD_DIR/NCIM
	if [[ $? -ne 0 ]]; then
	    echo "ERROR: problem cleaning up \$DOWNLOAD_DIR = $DOWNLOAD_DIR"
	    exit 1
	fi
	mkdir $DOWNLOAD_DIR/NCIM
    
    url=https://evs.nci.nih.gov/sites/default/files/assets/metathesaurus/Metathesaurus.RRF.zip
	echo "  Download latest NCI Metathesaurus"
    echo "    url = $url"
    curl -o $DOWNLOAD_DIR/Metathesaurus.RRF.zip $url
	if [[ $? -ne 0 ]]; then
	    echo "ERROR: problem downloading metathesaurus"
	    exit 1
	fi
    
    echo "  Unpack NCI Metathesaurus"
    echo "A" | unzip $DOWNLOAD_DIR/Metathesaurus.RRF.zip -d $DOWNLOAD_DIR/NCIM > /tmp/x.$$ 2>&1
	if [[ $? -ne 0 ]]; then
	    cat /tmp/x.$$
	    echo "ERROR: problem unpacking $DOWNLOAD_DIR/Metathesaurus.RRF.zip"
	    exit 1
	fi

    # Set $dir for later steps    
    dir=$DOWNLOAD_DIR/NCIM/META

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

echo "  Remove any older versions indexes"
version=`grep umls.release.name $dir/release.dat | perl -pe 's/.*=//; s/\r//;'`
curl -s $ES_SCHEME://$ES_HOST:$ES_PORT/_cat/indices |\
   perl -pe 's/^.* open ([^ ]+).*/$1/; s/\r//;' | grep -v $version | grep ${terminology}_ > /tmp/x.$$
for i in `cat /tmp/x.$$`; do    
    lv=`echo $i | perl -pe 's/.*_//;'`
    if [[ $lv -ge $version ]]; then
        echo "    skip $lv - later than $version"
        continue
    fi

    echo "    delete $i"
    curl -s -X DELETE https://$ES_HOST:$ES_PORT/$i
    if [[ $? -ne 0 ]]; then
        echo "ERROR: problem deleting https://$ES_HOST:$ES_PORT/$i"
        exit 1
    fi

    # do this if it starts with "concept_"
    if [[ $i =~ ^concept_.* ]]; then
        curl -s -X DELETE https://$ES_HOST:$ES_PORT/evs_metadata/_doc/$i
        if [[ $? -ne 0 ]]; then
            echo "ERROR: problem deleting https://$ES_HOST:$ES_PORT/evs_metadata/_doc/$i"
            exit 1
        fi
    fi
done

echo "  Cleanup"
/bin/rm -rf /tmp/x.$$ $DOWNLOAD_DIR/NCIM $DOWNLOAD_DIR/Metathesaurus.RRF.zip

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
