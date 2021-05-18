#!/bin/sh -f

echo "--------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "--------------------------------------------------"
set -e

# Setup configuration
echo "  Setup configuration"
APP_HOME=/local/content/evsrestapi
CONFIG_DIR=${APP_HOME}/${APP_NAME}/config
CONFIG_ENV_FILE=${CONFIG_DIR}/setenv.sh
echo "    config = $CONFIG_ENV_FILE"
. $CONFIG_ENV_FILE

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
version=`curl -s -g -u "${STARDOG_USERNAME}:$STARDOG_PASSWORD" http://${STARDOG_HOST}:${STARDOG_PORT}/${STARDOG_DB}/query  --data-urlencode "$query" -H "Accept: application/sparql-results+json" | perl -ne 's/.*"version".*"value":"(\d[\d\.abcde]+)".*/$1/; print "$_\n";' | sort | tail -1`
/bin/rm -f /tmp/x.$$.txt

echo "    version = $version"
if [[ -z $version ]]; then
    echo "ERROR: version is unknown"
    exit 1
fi

# Run reindexing process (choose a port other than the one that it runs on)
echo "  Generate indexes"
export EVS_SERVER_PORT="8082"
/usr/local/jdk1.8/bin/java -jar ../lib/evsrestapi.jar --terminology ncit_$version --realTime --forceDeleteIndex | sed 's/^/    /'

# Set the indexes to have a larger max_result_window
echo "  Set max result window to 150000"
fv=`echo $version | perl -pe 's/\.//;'`
curl -X PUT "$ES_SCHEME://$ES_HOST:$ES_PORT/concept_ncit_$fv/_settings" \
  -H "Content-type: application/json" -d '{ "index" : { "max_result_window" : 150000 } }'

# The part to remove the old version indexes for things no longer in stardog
# is unnecessary as it is implemented by the reindexing process.  To see an example
# of the code, see ncim.sh

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
