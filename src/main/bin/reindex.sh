#!/bin/bash -f
#
# This script reconciles the elasticsearch indexes against what is loded
# into stardog.  The --noconfig flag is for running in the dev environment
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
  echo "  e.g. $0 --noconfig --history ../data/UnitTestData/cumulative_history_21.06e.txt"
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

# Setup configuration
setup_configuration() {
  echo "  Setup configuration"
  if [[ $config -eq 1 ]]; then
      APP_HOME=/local/content/evsrestapi
      CONFIG_DIR=${APP_HOME}/${APP_NAME}/config
      CONFIG_ENV_FILE=${CONFIG_DIR}/setenv.sh
      echo "    config = $CONFIG_ENV_FILE"
    if [[ -e $CONFIG_ENV_FILE ]]; then
      echo "    config = $CONFIG_ENV_FILE"
      . $CONFIG_ENV_FILE
    else
          echo "ERROR: $CONFIG_ENV_FILE does not exist or has a problem"
          echo "       consider using --noconfig (if working in dev environment)"
          exit 1
      fi
  fi
}

setup_configuration
l_graph_db_type=${GRAPH_DB_TYPE:-"stardog"}
l_graph_db_port=${GRAPH_DB_PORT:-"5820"}
validate_setup() {
  if [[ -n "$GRAPH_DB_USERNAME" ]]; then
    l_graph_db_username="$GRAPH_DB_USERNAME"
  elif [[ -n "$STARDOG_USERNAME" ]]; then
    l_graph_db_username="$STARDOG_USERNAME"
  else
    echo "Error: Both GRAPH_DB_USERNAME and STARDOG_USERNAME are not set."
    exit 1
  fi
      if [[ -n "$GRAPH_DB_PASSWORD" ]]; then
    l_graph_db_password="$GRAPH_DB_PASSWORD"
  elif [[ -n "$STARDOG_PASSWORD" ]]; then
    l_graph_db_password="$STARDOG_PASSWORD"
  else
    echo "Error: Both GRAPH_DB_PASSWORD and STARDOG_PASSWORD are not set."
    exit 1
  fi
  if [[ -n "$GRAPH_DB_HOST" ]]; then
    l_graph_db_host="$GRAPH_DB_HOST"
  elif [[ -n "$STARDOG_HOST" ]]; then
    l_graph_db_host="$STARDOG_HOST"
  else
    echo "Error: Both GRAPH_DB_HOST and STARDOG_HOST are not set."
    exit 1
  fi
  if [[ -n "$GRAPH_DB_PORT" ]]; then
    l_graph_db_port="$GRAPH_DB_PORT"
  elif [[ -n "$STARDOG_PORT" ]]; then
    l_graph_db_port="$STARDOG_PORT"
  else
    echo "Error: Both GRAPH_DB_PORT and STARDOG_PORT are not set."
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
}
validate_setup
echo "    GRAPH_DB_TYPE = $l_graph_db_type"
echo "    GRAPH_DB_PORT = $l_graph_db_port"
if [[ $force -eq 1 ]]; then
    echo "  force = 1"
elif [[ $ES_CLEAN == "true" ]]; then
    echo "  force = 1 (ES_CLEAN=true)"
    force=1
fi

metadata_config_url=${CONFIG_BASE_URI:-"https://raw.githubusercontent.com/NCIEVS/evsrestapi-operations/main/config/metadata"}

get_databases(){
  if [[ $l_graph_db_type == "stardog" ]]; then
    curl -s -g -u "${l_graph_db_username}:$l_graph_db_password" \
        "http://${l_graph_db_host}:${l_graph_db_port}/admin/databases" |\
        python3 "$DIR/get_databases.py" "$GRAPH_DB_TYPE" > /tmp/db.$$.txt
  elif [[ $l_graph_db_type == "jena" ]]; then
    curl -s -g "http://${l_graph_db_host}:${l_graph_db_port}/$/server" |\
        python3 "$DIR/get_databases.py" "$GRAPH_DB_TYPE" > /tmp/db.$$.txt
  fi
  if [[ $? -ne 0 ]]; then
      echo "ERROR: unexpected problem listing databases"
      exit 1
  fi

  echo "  databases = " `cat /tmp/db.$$.txt`

  ct=`cat /tmp/db.$$.txt | wc -l`
  if [[ $ct -eq 0 ]]; then
      echo "ERROR: no graph databases, this is unexpected"
      exit 1
  fi
}
get_databases
# Open a new file descriptor that redirects to stdout:
exec 3>&1

get_ignored_sources(){
  if [[ -z $metadata_config_url ]]; then
    echo "METADATA_CONFIG_URL not set" 1>&3
    echo ""
  else
    curl -s -g -f "$metadata_config_url/ignore-source.txt" -o /tmp/is.$$.txt
  if [[ $? -ne 0 ]]; then
      echo "Failed to download ignore-source.txt using curl, trying as a local file..." 1>&3
      cp "$metadata_config_url/ignore-source.txt" /tmp/is.$$.txt
      if [[ $? -ne 0 ]]; then
          echo "ERROR: unable to obtain ignore-source.txt. Assuming no source URLs to ignore" 1>&3
          echo "$metadata_config_url/ignore-source.txt" 1>&3
      fi
  fi

    if [ -f "/tmp/is.$$.txt" ]; then
      echo $(cat "/tmp/is.$$.txt" | awk -vORS=">,<" '{ print $1 }' | sed 's/,<$//' | sed 's/^/</')
    else
      echo ""
    fi
  fi
}
ignored_sources=$(get_ignored_sources)
echo "Ignored source URLs:${ignored_sources}"

# Prep query to read all version info
echo "  Lookup terminology, version info in graph db"
get_graph_query(){
if [ -n "$ignored_sources" ];then
cat > /tmp/x.$$.txt << EOF
query=PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX dc:<http://purl.org/dc/elements/1.1/>
PREFIX xml:<http://www.w3.org/2001/XMLSchema>
select distinct ?source ?graphName ?version where {
  graph ?graphName {
    {
      ?source a owl:Ontology .
      ?source owl:versionInfo ?version .
      FILTER (?source NOT IN ($ignored_sources))
    }
    UNION
    {
      ?source a owl:Ontology .
      ?source owl:versionIRI ?version .
      FILTER NOT EXISTS { ?source owl:versionInfo ?versionInfo } .
      FILTER (?source NOT IN ($ignored_sources))
    }
  }
}
EOF
else
cat > /tmp/x.$$.txt << EOF
query=PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX dc:<http://purl.org/dc/elements/1.1/>
PREFIX xml:<http://www.w3.org/2001/XMLSchema>
select distinct ?source ?graphName ?version where {
  graph ?graphName {
    {
      ?source a owl:Ontology .
      ?source owl:versionInfo ?version .
    }
    UNION
    {
      ?source a owl:Ontology .
      ?source owl:versionIRI ?version .
      FILTER NOT EXISTS { ?source owl:versionInfo ?versionInfo } .
    }
  }
}
EOF
fi
query=$(cat /tmp/x.$$.txt)
}
get_graph_query
# Run the query against each of the databases
get_graphs(){
  /bin/rm -f /tmp/y.$$.txt
  touch /tmp/y.$$.txt
  for db in `cat /tmp/db.$$.txt`; do
      curl -s -g -u "${l_graph_db_username}:$l_graph_db_password" \
          http://${l_graph_db_host}:${l_graph_db_port}/$db/query \
          --data-urlencode "$query" -H "Accept: application/sparql-results+json" |\
          $jq | python3 "$DIR/get_graphs.py" "$db" >> /tmp/y.$$.txt
      if [[ $? -ne 0 ]]; then
          echo "ERROR: unexpected problem obtaining $db versions from stardog"
          exit 1
      fi
  done
  # Sort by version then reverse by DB (NCIT2 goes before CTRP)
  # this is because we need "monthly" to be indexed from the "monthlyDb"
  # defined in ncit.json
  # NOTE: version isn't cleaned up here so from where versionIRI is still an IRI
  sort -t\| -k 1,1 -k 2,2r -o /tmp/y.$$.txt /tmp/y.$$.txt
  cat /tmp/y.$$.txt | sed 's/^/    version = /;'
}
get_graphs

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
export PATH="/usr/local/corretto-jdk17/bin:$PATH"
# Handle the local setup
local=""
jar="../lib/evsrestapi.jar"
if [[ $config -eq 0 ]]; then
    local="-Dspring.profiles.active=local"
    jar=build/libs/`ls build/libs/ | grep evsrestapi | grep jar | head -1`
fi

get_terminology(){
  lower_terminology=$(basename "$1" | sed 's/.owl//g; s/Ontology//; s/-//;' | tr '[:upper:]' '[:lower:]')
  if [[ $lower_terminology =~ "thesaurus" ]]; then
    echo "ncit"
  else
    #lower_terminology=$(basename "$1" | sed 's/.owl//g; s/Ontology//; s/-//;' | tr '[:upper:]' '[:lower:]')
    IFS='_' read -r -a array <<<"$lower_terminology"
    echo $array
  fi
}

for x in `cat /tmp/y.$$.txt`; do
    echo "  Check indexes for $x"
    version=`echo $x | cut -d\| -f 1 | perl -pe 's#.*/([\d-]+)/[a-zA-Z]+.owl#$1#;'`
    cv=`echo $version | tr '[:upper:]' '[:lower:]' | perl -pe 's/[\.\-]//g;'`
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
	
        # Prep dir
        /bin/rm -rf $DIR/NCIT_HISTORY
        mkdir $DIR/NCIT_HISTORY
        cd $DIR/NCIT_HISTORY

        # Download file (try 5 times)
        for i in {1..5}; do 

        	echo "  Download latest NCIt History: attempt $i"
        	# Use the upload directory because this is where we can control it from
        	url=https://evs.nci.nih.gov/ftp1/upload/cumulative_history_$version.zip
            echo "    url = $url"
            curl -w "\n%{http_code}" -s -o cumulative_history_$version.zip $url > /tmp/x.$$
            if [[ $? -ne 0 ]]; then
                echo "ERROR: problem downloading NCIt history (trying again $i)"
            elif [[ `tail -1 /tmp/x.$$` -eq 404 ]]; then
                echo "ERROR: url does not exist, bail out"
                break;
            else

                echo "  Unpack NCIt history"
                unzip cumulative_history_$version.zip > /tmp/x.$$ 2>&1
                if [[ $? -ne 0 ]]; then
                    cat /tmp/x.$$
                    echo "ERROR: problem unpacking cumulative_history_$version.zip"
                    break
                fi

                # Set historyFile for later steps    
                historyFile=$DIR/NCIT_HISTORY/cumulative_history_$version.txt
                echo "    historyFile = $DIR/NCIT_HISTORY/cumulative_history_$version.txt"
                break
            fi
        done

        # cd back out
        cd - > /dev/null 2> /dev/null
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
    export STARDOG_DB=$db
    export EVS_SERVER_PORT="8083"
    
    if [[ $exists -eq 1 ]] && [[ $force -eq 0 ]]; then
        echo "    FOUND indexes for $term $version"
        
        if [[ $term == $pt ]]; then
            echo "    SKIP RECONCILE $term stale indexes and update flags"
            continue
        fi
        
        # Stale indexes are automatically cleaned up by the indexing process
        # It checks against stardog and reconciles everything and updates latest flags
        # regardless of whether there was new data
        echo "    RECONCILE $term stale indexes and update flags"
        export EVS_SERVER_PORT="8083"
        java $local -XX:+ExitOnOutOfMemoryError -jar $jar --terminology ${term} --skipConcepts --skipMetadata > /tmp/x.$$.log 2>&1 
        if [[ $? -ne 0 ]]; then
            cat /tmp/x.$$.log | sed 's/^/    /'
            echo "ERROR: unexpected error building indexes"
            exit 1
        fi
        /bin/rm -rf /tmp/x.$$.log
        
    else
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
        echo "    Generate indexes for $STARDOG_DB ${term} $version"
        
        # Set the history clause for "ncit"
        historyClause=""
        if [[ "$term" == "ncit" ]] && [[ $historyFile ]]; then
        	historyClause=" -d $historyFile"
        fi

        echo "    java $local -Xm4096M -jar $jar --terminology ${term}_$version --realTime --forceDeleteIndex $historyClause"
        java $local -XX:+ExitOnOutOfMemoryError -Xmx4096M -jar $jar --terminology "${term}_$version" --realTime --forceDeleteIndex $historyClause
        if [[ $? -ne 0 ]]; then
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
        ./postman.sh ${term}
 
    fi
	
	# Delete download directory for history file if it exists
	if [[ -e $DIR/NCIT_HISTORY ]]; then
        /bin/rm -rf $DIR/NCIT_HISTORY
	fi
    
    
    
    # track previous version, if next one is the same, don't index again.
    pv=$cv
    pt=$term
done

# Reconcile mappings after loading terminologies
export EVS_SERVER_PORT="8083"
echo "    Generate mapping indexes"
echo "      java $local -Xm4096M -jar $jar --terminology mapping"
java $local -XX:+ExitOnOutOfMemoryError -Xmx4096M -jar $jar --terminology mapping
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
/bin/rm -f /tmp/[xy].$$.txt /tmp/db.$$.txt /tmp/x.$$

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
