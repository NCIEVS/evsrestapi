#!/bin/sh -f

config=1
download=0
rm=1
help=0
while [[ "$#" -gt 0 ]]; do case $1 in
  --noconfig) config=0;;
  --download) download=1;;
  --terminology) terminology="$2"; shift;;
  --keep) rm=0;;
  --help) help=1;;
  *) arr=( "${arr[@]}" "$1" );;
esac; shift; done

ok=0
if [ ${#arr[@]} -eq 1 ]; then
  ok=1
elif [ ${#arr[@]} -eq 0 ] && [ $download -eq 1 ]; then
  ok=1
fi
if [ $ok -eq 0 ] || [ $help -eq 1 ]; then
  echo "Usage: $0 [--noconfig] [--download] [<dir>]"
  echo "Usage:    [--terminology <terminology, e.g. MDR>]"
  echo "  e.g. $0 /data/evs/ncim"
  echo "  e.g. $0 --noconfig /data/evs/ncim"
  echo "  e.g. $0 --download"
  echo "  e.g. $0 --noconfig --download"
  echo "  e.g. $0 --noconfig --download --keep"
  echo "  e.g. $0 --noconfig --download --terminology MDR"
  exit 1
fi

if [ ${#arr[@]} -eq 1 ]; then
  dir=${arr[0]}
fi
if [[ -z $terminology ]]; then
    terminology=ncim
fi

# Set download dir if not set (regardless of mode)
if [[ -z $DOWNLOAD_DIR ]]; then
	export DOWNLOAD_DIR=.
fi

echo "--------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "--------------------------------------------------"
echo "terminology = $terminology"
echo "config = $config"
echo "remove dir = $rm"
if [[ $download -eq 1 ]]; then
  echo "download = $DOWNLOAD_DIR"
else
  echo "dir = $dir"
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
    echo "A" | unzip $DOWNLOAD_DIR/Metathesaurus.RRF.zip -d $DOWNLOAD_DIR/NCIM "META/*" -x "*MRX*" > /tmp/x.$$ 2>&1
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
export PATH="/usr/local/corretto-jdk11/bin/:$PATH"
local=""
jar="../lib/evsrestapi.jar"
if [[ $config -eq 0 ]]; then
    local="-Dspring.profiles.active=local"
    jar=build/libs/`ls build/libs/ | grep evsrestapi | grep jar | head -1`
fi
export EVS_SERVER_PORT="8083"

# Compute version
lcterm=`echo $terminology | perl -ne 'print lc($_);'`
if [[ $terminology == "ncim" ]]; then
    version=`grep umls.release.name $dir/release.dat | perl -pe 's/.*=//; s/\r//;'`
else
    version=`perl -ne '@_=split/\|/; print "$_[6]\n" if $_[0] && $_[3] eq "'$terminology'";' $dir/MRSAB.RRF`
fi

## check whether this index exists already, and if so skip indexing call
echo "  Check whether indexes exist already"
ct1=`curl -s $ES_SCHEME://$ES_HOST:$ES_PORT/evs_metadata/_doc/concept_${lcterm}_${version} | grep '"found":false' | wc -l`
ct2=`curl -s $ES_SCHEME://$ES_HOST:$ES_PORT/_cat/indices | grep concept | grep ${lcterm} | grep ${version} | wc -l`
if [[ $ct1 -eq 0 ]] && [[ $ct2 -eq 1 ]]; then
    echo "    SKIP - indexes found"
    skip=1
else
    echo "    REINDEX - indexes not found"
    skip=0
fi

# Run reindexing process (choose a port other than the one that it runs on)
if [[ $skip -eq 0 ]]; then
    echo "  Generate indexes"
    # need to override this setting to make sure it's not too big
    export NCI_EVS_BULK_LOAD_INDEX_BATCH_SIZE=1000
    echo "java $local -Xmx3572M -jar $jar --terminology $terminology -d $dir --forceDeleteIndex"
    java $local -Xmx3572M -jar $jar --terminology $terminology -d $dir --forceDeleteIndex
    if [[ $? -ne 0 ]]; then
        echo "ERROR: unexpected error building indexes"
        exit 1
    fi
    
    # Set the indexes to have a larger max_result_window
    echo "  Set max result window to 250000 for concept_${lcterm}_${version}"
    curl -s -X PUT "$ES_SCHEME://$ES_HOST:$ES_PORT/concept_${lcterm}_${version}/_settings" \
         -H "Content-type: application/json" -d '{ "index" : { "max_result_window" : 250000 } }' >> /dev/null
    if [[ $? -ne 0 ]]; then
        echo "ERROR: unexpected error setting max_result_window"
        exit 1
    fi
fi

# compute maxVersions from config
echo "  Remove older versions indexes ($terminology $version)"
maxVersions=1
curl -s $ES_SCHEME://$ES_HOST:$ES_PORT/evs_metadata/_doc/concept_${lcterm}_${version} > /tmp/x.$$
if [[ `grep -c maxVersions /tmp/x.$$` -gt 0 ]]; then
  maxVersions=`curl -s $ES_SCHEME://$ES_HOST:$ES_PORT/evs_metadata/_doc/concept_${lcterm}_${version} | perl -pe 's/.*maxVersions"\s*\:\s*(\d+),.*/$1/'`
fi
echo "    maxVersions = $maxVersions"

# get concept indexes for this terminology and sort by version (which should sort earlier versions at the top of the file)
curl -s $ES_SCHEME://$ES_HOST:$ES_PORT/_cat/indices |\
   perl -pe 's/^.* open ([^ ]+).*/$1/; s/\r//;' | grep concept | grep -i ${terminology}_ | sort > /tmp/x.$$
ct=`cat /tmp/x.$$ | wc -l`
ct=$(($ct - $maxVersions))
if [[ $ct -lt 0 ]]; then
    ct=0
fi

# Remove the top $ct versions (which may be zero)
for i in `cat /tmp/x.$$ | head -$ct`; do
    lv=`echo $i | perl -pe 's/.*'${terminology}'_//i;'`
    # string compare versions
    if [ "$lv" \> "$version" ]; then
        echo "    skip $lv - later than $version"
        continue
    fi

    echo "    delete $i"
    curl -s -X DELETE $ES_SCHEME://$ES_HOST:$ES_PORT/$i > /tmp/x.$$
    if [[ $? -ne 0 ]]; then
        cat /tmp/x.$$ | sed 's/^/    /'
        echo "ERROR: problem deleting $ES_SCHEME://$ES_HOST:$ES_PORT/$i"
        exit 1
    fi

    i2=`echo $i | perl -pe 's/concept/evs_object/;'`
    echo "    delete $i2"
    curl -s -X DELETE $ES_SCHEME://$ES_HOST:$ES_PORT/$i2 > /tmp/x.$$
    if [[ $? -ne 0 ]]; then
        cat /tmp/x.$$ | sed 's/^/    /'
        echo "ERROR: problem deleting $ES_SCHEME://$ES_HOST:$ES_PORT/$i2"
        exit 1
    fi

    echo "    delete evs_metadata entry for $i"
    curl -s -X DELETE $ES_SCHEME://$ES_HOST:$ES_PORT/evs_metadata/_doc/$i > /tmp/x.$$
    if [[ $? -ne 0 ]]; then
        cat /tmp/x.$$ | sed 's/^/    /'
        echo "ERROR: problem deleting $ES_SCHEME://$ES_HOST:$ES_PORT/evs_metadata/_doc/$i"
         exit 1
    fi

done

echo "  Cleanup"
/bin/rm -rf /tmp/x.$$ $DOWNLOAD_DIR/Metathesaurus.RRF.zip
if [[ $rm -eq 1 ]]; then
    echo "    remove $DOWNLOAD_DIR/NCIM"
    /bin/rm -rf $DOWNLOAD_DIR/NCIM
else
    echo "    keep $DOWNLOAD_DIR/NCIM"
fi

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
