#!/bin/bash -f
#
# This script reconciles the elasticsearch indexes against what is loded
# into graph db.  The --noconfig flag is for running in the dev environment
# where the setenv.sh file does not exist.  The --force flag is used
# to recompute indexes that already exist rather than skipping them.
#
config=1
force=0
historyFileOverride=
while [[ "$#" -gt 0 ]]; do case $1 in
  --noconfig) config=0;;
  --force) force=1;;
  --history) historyFileOverride=$2; shift;;
  *) arr=( "${arr[@]}" "$1" );;
esac; shift; done

if [ ${#arr[@]} -ne 0 ]; then
  echo "Usage: $0 [--noconfig] [--force] [--history <history file>]"
  echo "  e.g. $0"
  echo "  e.g. $0 --noconfig"
  echo "  e.g. $0 --force"
  echo "  e.g. $0 --noconfig --history ../data/UnitTestData/NCIT/cumulative_history_25.12e.txt"
  exit 1
fi

# Set up ability to format json
jq --help >> /dev/null 2>&1
if [[ $? -eq 0 ]]; then
    jq="jq ."
else
    jq="python -m json.tool"
fi
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

echo "--------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "--------------------------------------------------"
echo "DIR = $DIR"
if [[ $historyFileOverride ]]; then
    echo "historyFileOverride = $historyFileOverride"
fi
echo ""

#---------------------------------------------------------
# Check configuration setup
#---------------------------------------------------------
echo "  Setup configuration"
if [[ $config -eq 1 ]]; then
        APP_HOME=/local/content/evsrestapi
        CONFIG_DIR=${APP_HOME}/${APP_NAME}/config
        CONFIG_ENV_FILE=${CONFIG_DIR}/setenv.sh
        echo "    config = $CONFIG_ENV_FILE"
    if [[ -e $CONFIG_ENV_FILE ]]; then
        . $CONFIG_ENV_FILE
    else
        echo "ERROR: $CONFIG_ENV_FILE does not exist or has a problem"
        echo "       consider using --noconfig (if working in dev environment)"
        exit 1
    fi
fi

# Set variables
l_graph_db_port=${GRAPH_DB_PORT:-"3030"}
metadata_config_url=${CONFIG_BASE_URI:-"https://raw.githubusercontent.com/NCIEVS/evsrestapi-operations/main/config/metadata"}

# Check config
if [[ -n "$GRAPH_DB_USERNAME" ]]; then
    l_graph_db_username="$GRAPH_DB_USERNAME"
else
    echo "Error: GRAPH_DB_USERNAME is not set."
    exit 1
fi
if [[ -n "$GRAPH_DB_PASSWORD" ]]; then
    l_graph_db_password="$GRAPH_DB_PASSWORD"
else
    echo "Error: GRAPH_DB_PASSWORD is not set."
    exit 1
fi
if [[ -n "$GRAPH_DB_HOST" ]]; then
    l_graph_db_host="$GRAPH_DB_HOST"
else
    echo "Error: GRAPH_DB_HOST is not set."
    exit 1
fi
if [[ -n "$GRAPH_DB_PORT" ]]; then
    l_graph_db_port="$GRAPH_DB_PORT"
else
    echo "Error: GRAPH_DB_PORT is not set."
    exit 1
fi
if [[ -z $ES_SCHEME ]]; then
      echo "ERROR: ES_SCHEME is not set"
      exit 1
elif [[ -z $ES_HOST ]]; then
      echo "ERROR: ES_HOST is not set"
      exit 1
elif [[ -z $ES_PORT ]]; then
      echo "ERROR: ES_PORT is not set"
      exit 1
fi
if [[ -z $metadata_config_url ]]; then
    echo "METADATA_CONFIG_URL not set"
    exit 1
fi

# Report configuration  
if [[ $force -eq 1 ]]; then
    echo "  force = 1"
fi

# Setup java environment
export PATH="/usr/local/corretto-jdk17/bin:$PATH"
# Handle the local setup
local=""
jar="../lib/evsrestapi.jar"
if [[ $config -eq 0 ]]; then
    local="-Dspring.profiles.active=local"
    jar=build/libs/`ls build/libs/ | grep evsrestapi | grep jar | head -1`
fi

#---------------------------------------------------------
# Declare Subroutines
#---------------------------------------------------------

# Set up a file handle so that subroutines an log to stdout
# For constructs like x=$(function_call) where we want to log
# within function call, put 1>&3 at the end of those commands
exec 3>&1

# check status
check_status() {
    local retval=$1
    local message=$2
    if [ $retval -ne 0 ]; then
      cat /tmp/x.$$
      echo ""
      echo "$message"
      exit 1
    fi
}
# check status
check_http_status() {
    retval=$1
    message=$2
    status=`tail -1 /tmp/x.$$`
    if [ $status -ne $retval ]; then
      echo ""
      perl -pe 's/'$status'$//' /tmp/x.$$ | sed 's/^/    /'
      echo "$message (returned $status)"
      exit 1
    fi
}

# Lookup databases and put into a temp file db.$$.txt
get_databases(){
	echo "    Get databases - /tmp/db.$$.txt ...`/bin/date`"
  # Hardcode database names for now 
  # TODO: this needs to make use of the new config index
  cat > /tmp/db.$$.txt << EOF
CTRP
NCIT2
EOF

}

# Gather ignored sources.  These are graphs that are loaded into Jena
# as a by-product of the thing we actually care about (e.g. duo).  
# Put values into a temp file is.$$.txt
# This function sets the "$ignored_sources" variable
get_ignored_sources() {
	echo "    Get ignored sources ...`/bin/date`"
    curl -s -g -f "$metadata_config_url/ignore-source.txt" -o /tmp/is.$$.txt
    if [[ $? -ne 0 ]]; then
      echo "      Failed to download ignore-source.txt using curl, trying as a local file..."
      cp "${metadata_config_url/file:\/\//}/ignore-source.txt" /tmp/is.$$.txt
      if [[ $? -ne 0 ]]; then
          echo "ERROR: unable to obtain ignore-source.txt. Assuming no source URLs to ignore"
          echo "$metadata_config_url/ignore-source.txt"
      fi
    fi

    if [ -f "/tmp/is.$$.txt" ]; then
      ignored_sources=$(cat "/tmp/is.$$.txt" | awk -vORS=">,<" '{ print $1 }' | sed 's/,<$//' | sed 's/^/</')
    else
      ignored_sources=""
    fi
    echo "      ignored source URLs = ${ignored_sources}"

}

# Build the query to find all graphs
# This function sets the "$graph_query" variable
get_graph_query() {
	echo "    Get graph query ...`/bin/date`"
	clause=""
    if [ -n "$ignored_sources" ]; then
        clause="      FILTER (?source NOT IN ($ignored_sources))"
    fi

    # NOTE: the query must return ?source, ?graphName, and ?version with those names
    cat > /tmp/x.$$.txt << EOF
query=PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX dc:<http://purl.org/dc/elements/1.1/>
PREFIX xml:<http://www.w3.org/2001/XMLSchema>
SELECT DISTINCT ?source ?graphName (STR(?safeVersion) AS ?version) WHERE {
  graph ?graphName {
    {
      ?source a owl:Ontology .
      ?source owl:versionInfo ?x_version .      
    $clause }
    UNION
    {
      ?source a owl:Ontology .
      ?source owl:versionIRI ?x_version .
      FILTER NOT EXISTS { ?source owl:versionInfo ?versionInfo } .
    $clause }

    BIND (
        IF(
            isURI(?x_version),
            ?x_version,
            IF(
                DATATYPE(?x_version) = xsd:decimal,
                xsd:integer(?x_version),
                ?x_version
            )
        ) AS ?safeVersion
    )
  }
}
EOF

    graph_query=$(cat /tmp/x.$$.txt)

}


# Run the query against each of the databases
# Collect results into a temp file y.$$.txt
get_graphs() {
    echo "    Lookup terminology, version info in graph db - /tmp/y.$$.txt ...`/bin/date`"
    /bin/rm -f /tmp/y.$$.txt
    touch /tmp/y.$$.txt
    # this was put back to perl because we don't have python3 on the evsrestapi machines
    for db in `cat /tmp/db.$$.txt`; do
        curl -w "\n%{http_code}"  -s -g -u "${l_graph_db_username}:$l_graph_db_password" \
          http://${l_graph_db_host}:${l_graph_db_port}/$db/query \
          --data-urlencode "$graph_query" -H "Accept: application/sparql-results+json" 2> /dev/null > /tmp/x.$$
        check_status $? "GET /$db/query failed to get graphs"
        check_http_status 200 "GET /$db/query expecting 200"
        sed '$d' /tmp/x.$$ | $jq | perl -ne '
            chop; $x="version" if /"version"/; 
            $x="source" if /"source"/; 
            $x=0 if /\}/; 
            if ($x && /"value"/) { 
                s/.* "//; s/".*//;
                ${$x} = $_;                
                print "$version|'$db'|$source\n" if $x eq "version"; 
            } ' >> /tmp/y.$$.txt
    done
    # Sort by version then reverse by DB (NCIT2 goes before CTRP)
    # this is because we need "monthly" to be indexed from the "monthlyDb"
    # defined in ncit.json
    # NOTE: version isn't cleaned up here so from where versionIRI is still an IRI
    sort -t\| -k 1,1 -k 2,2r -o /tmp/y.$$.txt /tmp/y.$$.txt
    cat /tmp/y.$$.txt | sed 's/^/      version = /;'
}

# set the max number of fields higher
# we can probably remove this when we figure a better answer
set_total_fields_limit_5000() {
    echo "    Set index.mapping.total_fields.limit = 5000 ...`/bin/date`"  
    curl -s -X PUT "$ES_SCHEME://$ES_HOST:$ES_PORT/evs_metadata/_settings" \
        -H "Content-type: application/json" -d '{ "index.mapping.total_fields.limit": 5000 }' >> /dev/null
    if [[ $? -ne 0 ]]; then
        echo "ERROR: unexpected error setting index.mapping.total_fields in evs_metadata"
        exit 1
    fi
}

# Takes a terminology graph URL and returns the terminology value
get_terminology() {
    lower_terminology=$(basename "$1" | sed 's/.owl//g; s/Ontology//; s/-//;' | tr '[:upper:]' '[:lower:]')
    if [[ $lower_terminology =~ "thesaurus" ]]; then
        echo "ncit"
    else
        #lower_terminology=$(basename "$1" | sed 's/.owl//g; s/Ontology//; s/-//;' | tr '[:upper:]' '[:lower:]')
        IFS='_' read -r -a array <<<"$lower_terminology"
        echo $array
    fi
}

# Takes a raw version and cleans it up for use
get_version() {
    local ver="$1"
    echo $ver | cut -d\| -f 1 | perl -ne 's#.*/([\d-]+)/[a-zA-Z]+.owl#$1#; print lc($_)'
}

# Attempts to download and unpack available history information for specified version
download_ncit_history_helper() {
    local ver="$1"
    success=0
    for i in {1..5}; do 
        echo "    Download NCIt History version $ver: attempt $i"
        url="https://evs.nci.nih.gov/ftp1/NCI_Thesaurus/cumulative_history_$ver.zip"
        echo "      url = $url"
        
        curl -w "\n%{http_code}" -s -o cumulative_history_$ver.zip "$url" > /tmp/x.$$
        # if curl command fails then try again
        if [[ $? -ne 0 ]]; then
            echo "      ERROR: problem downloading NCIt history (trying again $i)"
        # if status code is not 200, then bail
        elif [[ $(tail -1 /tmp/x.$$) -ne 200 ]]; then
            echo "      ERROR: unexpected status code downloading NCIt history = "$(tail -1 /tmp/x.$$)
            break
        else
            echo "    Unpack NCIt history"
            unzip "cumulative_history_$ver.zip" > /tmp/x.$$ 2>&1
            if [[ $? -ne 0 ]]; then
                cat /tmp/x.$$
                echo "ERROR: problem unpacking cumulative_history_$ver.zip"
                break
            fi

            historyFile="$DIR/NCIT_HISTORY/cumulative_history_$ver.txt"

            if [[ -f "$historyFile" ]]; then
                echo "    historyFile = $historyFile"
                success=1
            else
                echo "ERROR: expected file $historyFile not found"
            fi
            break
        fi
    done
}

# Download and unpack NCIt history.  Uses helper method to iterate and try
# multiple attempts and multiple approaches.
download_ncit_history() {
  # Prep dir
  /bin/rm -rf $DIR/NCIT_HISTORY
  mkdir $DIR/NCIT_HISTORY
  cd $DIR/NCIT_HISTORY

  # Download file (try 5 times)
  success=0
  download_ncit_history_helper "$version"

  # try to get previous version of the history file
  if [[ $success -eq 0 ]]; then
      echo "    Initial version $version failed. Fetching latest version from API..."

      # get server port for local vs deployed environment
      serverPort=8080
      if [[ $config -eq 0 ]]; then
          serverPort=8082
      fi

      # This script runs on the same server as the API
      response=$(curl -s -X 'GET' \
        "http://localhost:${serverPort}/api/v1/metadata/terminologies?latest=true&tag=monthly&terminology=ncit" \
        -H 'accept: application/json')
      if [[ $? -ne 0 ]]; then
          echo "ERROR: Failed to get latest terminology from http://localhost:${serverPort}/api/v1/metadata/terminologies?latest=true&tag=monthly&terminology=ncit"
          if [[ $serverPort -eq 8082 ]]; then
              echo "  Setting default history version on local to 25.12e"
              prev_version="25.12e"
          else
              echo "  Failed to find terminology version on non-local server, exiting"
          fi
          cd - > /dev/null
          return 1

      fi
      echo "      response = $response"

      if [[ -z "${prev_version}" ]]; then
          echo "  prev_version is not set to a default, trying to parse response"
          # Parse the response to get the previous version
          if ! command -v jq &> /dev/null; then
              echo "jq is not installed, using grep and perl as fallback"
              prev_version=$(echo "$response" | grep '"version"' | perl -pe 's/.*"version":"//; s/".*//; ')
          else
              prev_version=$(echo "$response" | jq -r '.[] | .version')
          fi
      fi
      echo "    Previous monthly version of ncit: $prev_version"
            
      if [[ -z "$prev_version" ]]; then
          echo "    Unable to find a previous monthly version of ncit"
      # done looking
      else 
          echo "    Trying again with prev_version = $prev_version"
          download_and_unpack "$prev_version"
      fi
  fi

  # cd back out
  cd - > /dev/null
  return 0
}

# For "rdf" or "rrf", get the indexed terminologies
get_indexed_terminologies() {
    local type=$1	
    echo $(curl -s "$ES_SCHEME://$ES_HOST:$ES_PORT/evs_metadata/_search?size=1000" | jq -r '.hits.hits[]._source.terminology | select(.metadata.loader == "'"$type"'") | .terminologyVersion' | sed 's/^/concept_/')
}

# Remove unused indexes
# Things loaded via RDF in the indexes that are no longer in graphdb
remove_unused_indexes() {

    echo "    Remove rdf terminologies no longer in graphdb ...`/bin/date`"
    # Get all currently indexed terminologies
    rdf_terms=$(get_indexed_terminologies "rdf")
    echo "      rdf = $rdf_terms"

    # Get all valid terminology keys from the currently loaded graph db triples
    graphdb_terms=$(cut -d'|' -f1,3 /tmp/y.$$.txt | while IFS='|' read -r version iri; do
        term=$(get_terminology "$iri")
        version=$(get_version "$version")
        echo -n " concept_${term}_${version}"
done)
    echo "      graphdb = $graphdb_terms"

    # Remove indexes not found in triple store
    for index in $rdf_terms; do
        keep=0
        for v in $graphdb_terms; do
            if [[ "$index" == "$v" ]]; then
                keep=1
                break
            fi
        done
        if [[ $keep -eq 0 ]]; then
            echo "      remove $index"
            #curl -s -X DELETE "$ES_SCHEME://$ES_HOST:$ES_PORT/$index" > /dev/null
        fi
    done	
	
}

#---------------------------------------------------------
# Perform Reindex Operations
#---------------------------------------------------------

get_databases
get_ignored_sources
get_graph_query
get_graphs
set_total_fields_limit_5000
remove_unused_indexes

# For each DB|version, check whether indexes already exist for that version
echo ""
for x in `cat /tmp/y.$$.txt`; do
    echo "  Check indexes for $x"
    version=`echo $x | cut -d\| -f 1 | perl -pe 's#.*/([\d-]+)/[a-zA-Z]+.owl#$1#;'`
    cv=`echo $version | perl -ne 's/[\.\-]//g; print lc($_)'`
    db=`echo $x | cut -d\| -f 2`
    uri=`echo $x | cut -d\| -f 3`
    term=$(get_terminology "$uri")

    # if previous version and current version match, then skip
    # this is a monthly that's in both NCIT2 and CTRP databases
    if [[ $cv == $pv ]] && [[ $term == $pt ]]; then
        echo "    SEEN $cv for $pt, continue"
        continue
    fi

    exists=1
	
    # Use override history file if specified
    historyFile=""
    if [[ "$term" == "ncit" ]] && [[ $historyFileOverride ]]; then
        historyFile=$historyFileOverride

    # Otherwise, download if ncit
    elif [[ "$term" == "ncit" ]]; then
        download_ncit_history
	fi
	
    for y in `echo "evs_metadata concept_${term}_$cv evs_object_${term}_$cv"`; do

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

        if [[ $y == "evs_metadata" ]]; then
            echo "  Checking for existence of concept_${term}_$cv"
            # Check whether the entry for this terminology exists        
            if [[ `curl -s "${ES_SCHEME}://${ES_HOST}:${ES_PORT}/evs_metadata/_doc/concept_${term}_$cv" | grep '"found":false' | wc -l` -eq 1 ]]; then
                echo "    MISSING evs_metadata entry for concept_${term}_$cv"
                exists=0
            fi
        fi
    done

    # Set up environment
    export GRAPH_DB=$db
    export EVS_SERVER_PORT="8083"

    # Set the history clause for "ncit"
    historyClause=""
    if [[ "$term" == "ncit" ]] && [[ $historyFile ]]; then
      historyClause=" -d $historyFile"
    fi
    
    if [[ $exists -eq 0 ]] || [[ $force -eq 1 ]]; then

        if [[ $exists -eq 1 ]] && [[ $force -eq 1 ]]; then
            echo "    FOUND indexes for $term $version, force reindex anyway"        

            # Remove if this already exists
            version=`echo $cv | perl -pe 's/.*_//;'`
            echo "    Remove indexes for $term $version"
            $DIR/remove.sh $term $version > /tmp/x.$$ 2>&1
            if [[ $? -ne 0 ]]; then
                cat /tmp/x.$$ | sed 's/^/    /'
                echo "ERROR: removing $term $version indexes"
                exit 1
            fi
        fi

        # Run reindexing process (choose a port other than the one that it runs on)
        echo "    Generate indexes for $GRAPH_DB ${term} $version"
        
        # Set the history clause for "ncit"
        historyClause=""
        if [[ "$term" == "ncit" ]] && [[ $historyFile ]]; then
            historyClause=" -d $historyFile"
        fi

        echo "    java --add-opens=java.base/java.io=ALL-UNNAMED $local -Xm4096M -jar $jar --terminology ${term}_$version --realTime --forceDeleteIndex $historyClause"
        java --add-opens=java.base/java.io=ALL-UNNAMED $local -XX:+ExitOnOutOfMemoryError -Xmx4096M -jar $jar --terminology "${term}_$version" --realTime --forceDeleteIndex $historyClause
        if [[ $? -ne 0 ]]; then
            echo "pwd = `pwd`"
            echo "ERROR: unexpected error building indexes"
            exit 1
        fi

        # Unset history file once done being used

        # Set the indexes to have a larger max_result_window
        echo "    Set max result window to 250000 for concept_${term}_$cv"
        curl -s -X PUT "$ES_SCHEME://$ES_HOST:$ES_PORT/concept_${term}_$cv/_settings" \
              -H "Content-type: application/json" -d '{ "index" : { "max_result_window" : 250000 } }' >> /dev/null
        if [[ $? -ne 0 ]]; then
            echo "ERROR: unexpected error setting max_result_window"
            exit 1
        fi

        ### This needs more work to run in local, dev, qa, stage, prod
        ### main issues are API_URL setting and the requirement to use npm
        ### which may not be installed or configured
            ## get directory of reindex.sh
            #ORIG_DIR=$(pwd)
            #REINDEX_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
            #cd "$REINDEX_DIR"
            #mkdir -p "$REINDEX_DIR/postman_content_qa"
            #"$REINDEX_DIR/postman.sh" "${term}" > "$REINDEX_DIR/postman_content_qa/${term}_postman_content_qa.txt"
            #POSTMAN_EXIT=$?
            #if [ $POSTMAN_EXIT -ne 0 ]; then
            #    echo "Error: postman.sh failed with exit code $POSTMAN_EXIT"
            #    exit $POSTMAN_EXIT
            #fi
            #cd "$ORIG_DIR"
          
        # Delete download directory for history file if it exists
        if [[ -e $DIR/NCIT_HISTORY ]]; then
            /bin/rm -rf $DIR/NCIT_HISTORY
        fi
        
        
        
        # track previous version, if next one is the same, don't index again.
        pv=$cv
        pt=$term
    fi
done

# Stale indexes are automatically cleaned up by the indexing process
# It checks against graph db and reconciles everything and updates latest flags
# regardless of whether there was new data
echo "    RECONCILE ALL stale indexes and update flags"
export EVS_SERVER_PORT="8083"
java --add-opens=java.base/java.io=ALL-UNNAMED $local -XX:+ExitOnOutOfMemoryError -jar $jar --terminology reconcile --skipLoad
if [[ $? -ne 0 ]]; then
    echo "ERROR: unexpected error reconciling indexes"
    exit 1
fi
/bin/rm -rf /tmp/x.$$.log

# Reconcile mappings after loading terminologies
export EVS_SERVER_PORT="8083"
echo "    Generate mapping indexes"
echo "      java --add-opens=java.base/java.io=ALL-UNNAMED $local -Xmx4096M -jar $jar --terminology mapping"
java --add-opens=java.base/java.io=ALL-UNNAMED $local -XX:+ExitOnOutOfMemoryError -Xmx4096M -jar $jar --terminology mapping
if [[ $? -ne 0 ]]; then
    echo "ERROR: unexpected error building mapping indexes"
    exit 1
fi

# Verify that max_result_window on evs_mappings is set to 2500000
if [[ `curl -s "$ES_SCHEME://$ES_HOST:$ES_PORT/evs_mappings/_settings" | grep -c max_result_window` -eq 0 ]]; then
    # Set the indexes to have a larger max_result_window
    echo "  Set max result window to 250000 for evs_mappings"
    curl -s -X PUT "$ES_SCHEME://$ES_HOST:$ES_PORT/evs_mappings/_settings" \
         -H "Content-type: application/json" -d '{ "index" : { "max_result_window" : 250000 } }' >> /dev/null
    if [[ $? -ne 0 ]]; then
        echo "ERROR: unexpected error setting max_result_window for evs_mappings"
        exit 1
    fi
fi

# Cleanup
/bin/rm -f /tmp/[xy].$$.txt /tmp/db.$$.txt /tmp/is.$$.txt /tmp/x.$$

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
