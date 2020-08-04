#!/bin/sh -f

# Prep query to read all version info
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
version=`curl -v -g -u "${STARDOG_USERNAME}:$STARDOG_PASSWORD" http://${STARDOG_HOST}:${STARDOG_PORT}/${STARDOG_DB}/query  --data-urlencode "$query" -H "Accept: application/sparql-results+json"  | python -m json.tool | perl -ne 's/^ +"value": "//; s/".*//; print if /^20/' | sort | tail -1`
/bin/rm -f /tmp/x.$$.txt

# Run reindexing process
export EVS_SERVER_PORT="8082"
/usr/local/jdk1.8/bin/java -jar evsrestapi/lib/evsrestapi.jar --terminology ncit_$version --realTime --forceDeleteIndex
