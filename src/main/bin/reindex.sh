#!/bin/bash -f
#
# This script reconciles the elasticsearch indexes against what is loded
# into stardog.  The --noconfig flag is for running in the dev environment
# where the setenv.sh file does not exist.  The --force flag is used
# to recompute indexes that already exist rather than skipping them.
#
config=1
force=0
while [[ "$#" -gt 0 ]]; do case $1 in
  --noconfig) config=0;;
  --force) force=1;;
  *) arr=( "${arr[@]}" "$1" );;
esac; shift; done

if [ ${#arr[@]} -ne 0 ]; then
  echo "Usage: $0 [--noconfig] [--force]"
  echo "  e.g. $0"
  echo "  e.g. $0 --noconfig"
  echo "  e.g. $0 --force"
  exit 1
fi

# Set up ability to format json
jq --help >> /dev/null 2>&1
if [[ $? -eq 0 ]]; then
    jq="jq ."
else
    jq="python -m json.tool"
fi

echo "--------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "--------------------------------------------------"
set -e

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

if [[ $force -eq 1 ]]; then
    echo "  force = 1"
elif [[ $ES_CLEAN == "true" ]]; then
    echo "  force = 1 (ES_CLEAN=true)"
    force=1
fi

curl -s -g -u "${STARDOG_USERNAME}:$STARDOG_PASSWORD" \
    "http://${STARDOG_HOST}:${STARDOG_PORT}/admin/databases" |\
    $jq | perl -ne 's/\r//; $x=0 if /\]/; if ($x) { s/.* "//; s/",?$//; print "$_"; }; 
                    $x=1 if/\[/;' > /tmp/db.$$.txt
if [[ $? -ne 0 ]]; then
    echo "ERROR: unexpected problem listing databases"
    exit 1
fi

echo "  databases = " `cat /tmp/db.$$.txt`

# Prep query to read all version info
echo "  Lookup version info for latest terminology in stardog"
cat > /tmp/x.$$.txt << EOF
query=PREFIX owl:<http://www.w3.org/2002/07/owl#> 
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> 
PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> 
PREFIX dc:<http://purl.org/dc/elements/1.1/> 
PREFIX xml:<http://www.w3.org/2001/XMLSchema>
select ?graphName ?version where {
  graph ?graphName {
    ?source a owl:Ontology .
    ?source owl:versionInfo ?version .
    ?source dc:date ?date .
    ?source rdfs:comment ?comment .
  }
}
EOF
query=`cat /tmp/x.$$.txt`

# Run the query against each of the databases
/bin/rm -f /tmp/y.$$.txt
touch /tmp/y.$$.txt
for db in `cat /tmp/db.$$.txt`; do
    curl -s -g -u "${STARDOG_USERNAME}:$STARDOG_PASSWORD" \
        http://${STARDOG_HOST}:${STARDOG_PORT}/$db/query \
        --data-urlencode "$query" -H "Accept: application/sparql-results+json" |\
        $jq | perl -ne 'chop; $x=1 if /"version"/; $x=0 if /\}/; if ($x && /"value"/) { 
            s/.* "//; s/".*//; print "$_|'$db'\n"; } ' >> /tmp/y.$$.txt
    if [[ $? -ne 0 ]]; then
        echo "ERROR: unexpected problem obtaining $db versions from stardog"
        exit 1
    fi    
done

# Sort by version then reverse by DB (NCIT2 goes before CTRP)
# this is because we need "monthly" to be indexed from the "monthlyDb"
# defined in ncit.json
/bin/sort -t\| -k 1,1 -k 2,2r -o /tmp/y.$$.txt /tmp/y.$$.txt
cat /tmp/y.$$.txt | sed 's/^/    version = /;'

if [[ $ES_CLEAN == "true" ]]; then
    echo "  Remove and recreate evs_metadata index"
    curl -s -X DELETE "$ES_SCHEME://$ES_HOST:$ES_PORT/evs_metadata" >> /dev/null
    if [[ $? -ne 0 ]]; then
        echo "ERROR: unexpected error deleting evs_metadata index"
        exit 1
    fi
    curl -s -X PUT "$ES_SCHEME://$ES_HOST:$ES_PORT/evs_metadata" >> /dev/null
    if [[ $? -ne 0 ]]; then
        echo "ERROR: unexpected error creating evs_metadata index"
        exit 1
    fi
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

# For each DB|version, check whether indexes already exist for that version
echo ""
export PATH="/usr/local/jdk1.8/bin/:$PATH"
# Handle the local setup
local=""
jar="../lib/evsrestapi.jar"
if [[ $config -eq 0 ]]; then
    local="-Dspring.profiles.active=local"
    jar=build/libs/`ls build/libs/ | grep evsrestapi | grep jar | head -1`
fi
export EVS_SERVER_PORT="8083"
for x in `cat /tmp/y.$$.txt`; do
    echo "  Check indexes for $x"
    version=`echo $x | cut -d\| -f 1`
    cv=`echo $version | perl -pe 's/\.//;'`
    db=`echo $x | cut -d\| -f 2`

    # if previous version and current version match, then skip
    # this is a monthly that's in both NCIT2 and CTRP databases
    if [[ $cv == $pv ]]; then
        echo "    SEEN $cv, continue"
        continue
    fi

    exists=1
    for y in `echo "evs_metadata concept_ncit_$cv evs_object_ncit_$cv"`; do

        # Check for index
        curl -s -o /tmp/x.$$.txt ${ES_SCHEME}://${ES_HOST}:${ES_PORT}/_cat/indices    
        if [[ $? -ne 0 ]]; then
            echo "ERROR: unexpected problem attempting to list indexes"
            exit 1
        fi
        # handle the no indexes case
        ct=`grep $y /tmp/x.$$.txt | wc -l`
        if [[ $ct -eq 0 ]]; then
            echo "    MISSING $y index"
            exists=0
        fi
    done
    
    if [[ $exists -eq 1 ]] && [[ $force -eq 0 ]]; then
        echo "    FOUND indexes for $version, continue"
    else
        if [[ $exists -eq 1 ]] && [[ $force -eq 1 ]]; then
            echo "    FOUND indexes for $version, force reindex anyway"        
        fi

        # Run reindexing process (choose a port other than the one that it runs on)
        export STARDOG_DB=$db
        export EVS_SERVER_PORT="8083"
        echo "    Generate indexes for $STARDOG_DB $version"

        echo "java $local -jar $jar --terminology ncit_$version --realTime --forceDeleteIndex" | sed 's/^/      /'
        java $local -jar $jar --terminology ncit_$version --realTime --forceDeleteIndex
        if [[ $? -ne 0 ]]; then
            echo "ERROR: unexpected error building indexes"
            exit 1
        fi

        # Set the indexes to have a larger max_result_window
        echo "    Set max result window to 150000 for concept_ncit_$cv"
        curl -s -X PUT "$ES_SCHEME://$ES_HOST:$ES_PORT/concept_ncit_$cv/_settings" \
             -H "Content-type: application/json" -d '{ "index" : { "max_result_window" : 150000 } }' >> /dev/null
        if [[ $? -ne 0 ]]; then
            echo "ERROR: unexpected error setting max_result_window"
            exit 1
        fi

    fi

    # track previous version, if next one is the same, don't index again.
    pv=$cv
done

# Stale indexes are automatically cleaned up by the indexing process
# It checks against stardog and reconciles everything and updates latest flags
# regardless of whether there was new data
echo "  Reconcile stale indexes and update flags"
echo "    java $local -jar $jar --terminology ncit --skip-load"
java $local -jar $jar --terminology ncit --skip-load
if [[ $? -ne 0 ]]; then
    echo "ERROR: unexpected error building indexes"
    exit 1
fi

# Cleanup
/bin/rm -f /tmp/[xy].$$.txt /tmp/db.$$.txt

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
