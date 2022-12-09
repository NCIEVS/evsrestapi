#!/bin/bash -f
#
# This script generates a load report for a specified stardog graph
# into stardog.  The --noconfig flag is for running in the dev environment
# where the setenv.sh file does not exist.  The --list flag is used to
# list terminology/version combinations in stardog
#
config=1
help=0
list=0
while [[ "$#" -gt 0 ]]; do case $1 in
  --noconfig) config=0;;
  --help) help=1;;
  --list) list=1;;
  *) arr=( "${arr[@]}" "$1" );;
esac; shift; done

if [ $list -eq 1 ]; then
  # noop
  touch /tmp/x.$$
elif [ ${#arr[@]} -ne 3 ] || [ $help -eq 1 ]; then
  echo "Usage: $0 [--noconfig] [--list] <database> <terminology> <version>"
  echo "  e.g. $0"
  echo "  e.g. $0 --noconfig"
  echo "  e.g. $0 --noconfig --list"
  echo "  e.g. $0 --noconfig NCIT2 hgnc 202209"
  echo "  e.g. $0 --noconfig NCIT2 chebi 213"
  exit 1
fi

db=${arr[0]}
terminology=${arr[1]}
version=${arr[2]}

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
if [ $list -eq 1 ]; then
    echo "LIST mode"
else
    echo "db = $db"
    echo "terminology = $terminology"
    echo "version = $version"
fi

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
        echo "       consider using --noconfig (if working in dev environment)"
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
fi

echo "    stardog = http://${STARDOG_HOST}:${STARDOG_PORT}"
echo ""

curl -s -g -u "${STARDOG_USERNAME}:$STARDOG_PASSWORD" \
    "http://${STARDOG_HOST}:${STARDOG_PORT}/admin/databases" |\
    $jq | perl -ne 's/\r//; $x=0 if /\]/; if ($x) { s/.* "//; s/",?$//; print "$_"; }; 
                    $x=1 if/\[/;' > /tmp/db.$$.txt
if [[ $? -ne 0 ]]; then
    echo "ERROR: unexpected problem listing databases"
    exit 1
fi

echo "  databases = " `cat /tmp/db.$$.txt`
ct=`cat /tmp/db.$$.txt | wc -l`
if [[ $ct -eq 0 ]]; then
    echo "ERROR: no stardog databases, this is unexpected"
    exit 1
fi


# Prep query to read all version info
echo "  Lookup terminology, version info for graphs in stardog"
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
      ?source owl:versionInfo ?version
    }
    UNION
    {
      ?source a owl:Ontology .
      ?source owl:versionIRI ?version .
      FILTER NOT EXISTS { ?source owl:versionInfo ?versionInfo }
    }
  }
}
EOF
query=`cat /tmp/x.$$.txt`

# Run the query against each of the databases
/bin/rm -f /tmp/y.$$.txt
touch /tmp/y.$$.txt
for d in `cat /tmp/db.$$.txt`; do
    curl -s -g -u "${STARDOG_USERNAME}:$STARDOG_PASSWORD" \
        http://${STARDOG_HOST}:${STARDOG_PORT}/$d/query \
        --data-urlencode "$query" -H "Accept: application/sparql-results+json" |\
        $jq | perl -ne '
            chop; $x="version" if /"version"/; 
            $x="source" if /"source"/; 
            $x=0 if /\}/; 
            if ($x && /"value"/) { 
                s/.* "//; s/".*//;
                ${$x} = $_;                
                print "$version|'$d'|$source\n" if $x eq "version"; 
            } ' >> /tmp/y.$$.txt
    if [[ $? -ne 0 ]]; then
        echo "ERROR: unexpected problem obtaining $d versions from stardog"
        exit 1
    fi    
done

# Sort by version then reverse by DB (NCIT2 goes before CTRP)
# this is because we need "monthly" to be indexed from the "monthlyDb"
# defined in ncit.json
/bin/sort -t\| -k 1,1 -k 2,2r -o /tmp/y.$$.txt /tmp/y.$$.txt


if [ $list -eq 1 ]; then

    echo "  List stardog graphs"
    for x in `cat /tmp/y.$$.txt`; do
        version=`echo $x | cut -d\| -f 1 | perl -pe 's#.*/(\d+)/[a-zA-Z]+.owl#$1#;'`
        cv=`echo $version | perl -pe 's/\.//;'`
        db=`echo $x | cut -d\| -f 2`
        uri=`echo $x | cut -d\| -f 3`
        term=`echo $uri | perl -pe 's/.*Thesaurus.owl/ncit/; s/.*obo\/go.owl/go/; s/.*\/HGNC.owl/hgnc/; s/.*\/chebi.owl/chebi/'`
        echo "    $db $term $version"
    done
    exit 0
    
else
    # Verify db/termionlogy/version is valid
    passed=0
    for x in `cat /tmp/y.$$.txt`; do
        v=`echo $x | cut -d\| -f 1 | perl -pe 's#.*/(\d+)/[a-zA-Z]+.owl#$1#;'`
        d=`echo $x | cut -d\| -f 2`
        uri=`echo $x | cut -d\| -f 3`
        t=`echo $uri | perl -pe 's/.*Thesaurus.owl/ncit/; s/.*obo\/go.owl/go/; s/.*\/HGNC.owl/hgnc/; s/.*\/chebi.owl/chebi/'`
        if [ $v == $version ] && [ $t == $terminology ] && [ $d == $db ]; then
            passed=1
        fi	
    done
    if [ $passed -eq 0 ]; then
        echo "ERROR: $db $terminology $version not found (try using --list)"
        exit 1
    fi

    export PATH="/usr/local/corretto-jdk11/:$PATH"
    # Handle the local setup
    local=""
    jar="../lib/evsrestapi.jar"
    if [[ $config -eq 0 ]]; then
        local="-Dspring.profiles.active=local"
        jar=build/libs/`ls build/libs/ | grep evsrestapi | grep jar | head -1`
    fi

    # Generate report
    echo "  Generate report for $db $terminology $version...`/bin/date`"
    export STARDOG_DB=$db
    export EVS_SERVER_PORT="8083"
    echo "java $local -Xmx4096M -jar $jar --terminology ${terminology}_$version --report" | sed 's/^/      /'
    java $local -Xmx4096M -jar $jar --terminology ${terminology}_$version --report
    if [[ $? -ne 0 ]]; then
        echo "ERROR: unexpected error building indexes"
        exit 1
    fi

fi

# Cleanup
/bin/rm -f /tmp/[xy].$$.txt /tmp/db.$$.txt /tmp/x.$$

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
