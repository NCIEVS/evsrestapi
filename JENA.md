# EVSRESTAPI - JENA/FUSEKI SETUP

Information on using Apache jena/fuseki with EVSRESTAPI.

## Build and run a local docker image

Run the following command to build the Jena image:

```bash
cd docker/fuseki
docker build -t evsrestapi/fuseki:5.1.0  .
```

### Running a Jena/Fuseki Container 

Start the container with the following command. 
Note: you need the mount path to a local directory ($dir) of your choice to persist the data.

```bash
dir=c:/Users/carlsenbr/eclipse-workspace/data/fuseki
docker run -d --name=jena_evs --rm -p "3030:3030" -v"$dir":/opt/fuseki/run/databases evsrestapi/fuseki:5.1.0
```

### Running a sparql query (assumes jena/fuseki running on localhost and curl and jq are installed)

Start by configuring your environment

```
GRAPH_DB_HOST=localhost
GRAPH_DB_PORT=3030
GRAPH_DB_USERNAME=admin
GRAPH_DB_PASSWORD=admin
GRAPH_DB_URL=http://${GRAPH_DB_HOST}:${GRAPH_DB_PORT}
GRAPH_DB_DB=NCIT2
```

Next, put your query into a file.  Make sure to include prefixes and the correct graph name.

```
cat > query.txt << EOF
PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#> 
PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>
PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX dc:<http://purl.org/dc/elements/1.1/>
PREFIX oboInOwl:<http://www.geneontology.org/formats/oboInOwl#>
PREFIX xml:<http://www.w3.org/2001/XMLSchema#>
SELECT ?code
{ GRAPH <http://NCI_T_monthly> 
    { 
      ?x a owl:Class . 
      ?x :NHC0 ?code .
      ?x :P108 "Melanoma"
    }
}
ORDER BY ?conceptCode
EOF
```

Then, run the query.

```
q=`cat query.txt`
curl -v -g -u "${GRAPH_DB_USERNAME}:$GRAPH_DB_PASSWORD" "$GRAPH_DB_URL/NCIT2/query" --data-urlencode "query=$q" -H "Accept: application/sparql-results+json" | jq
```

