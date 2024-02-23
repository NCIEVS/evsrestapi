# EVSRESTAPI - STARDOG SETUP

Information on downloading and using stardog with EVSRESTAPI.

## Running Stardog Locally

* Initial setup (create a volume to store data/license)

      docker pull stardog/stardog:latest
      
      # Windows volume seems to have an "fsync" issue, try using a "local docker volume"
      docker volume create --name stardog-home2 -d local

* Using an existing stardog license (in $dir). Make sure your license file is called `stardog-license-key.bin` and is in the $dir directory on you rlocal machine.

      dir=c:/Users/carlsenbr/eclipse-workspace/data/
      docker run -it --entrypoint "/bin/bash" -v "$dir":/data -v stardog-home2:/var/opt/stardog stardog/stardog
      [root@0b9fbb0b90ba bin]# cp /data/stardog-license-key.bin /var/opt/stardog
      [root@0b9fbb0b90ba bin]# exit


* Obtaining Stardog Licence Locally - [see Quick Start Guide](https://www.stardog.com/docs/#_quick_start_guide)

      # get license
      docker run -it --entrypoint "/bin/bash" -v stardog-home2:/var/opt/stardog stardog/stardog
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin license request --force
      ... answer questions, provide email - bcarlsen+stardog@westcoastinformatics.com ...
      [root@0b9fbb0b90ba bin]# exit

  * At this point, the license should be in docker volume "stardog-home2" and be properly remounted with the license intact
  * NOTE: this step only needs to be run once (until license expires)

* Loading NCIt ThesaurusInferred.owl (after license is setup).  Make sure the local volume being mounted is the one that contains the ThesaurusInferred.owl file.

      dir=c:/Users/carlsenbr/eclipse-workspace/data/
      docker run -it --entrypoint "/bin/bash" -p 5820:5820 -v "$dir":/data -v stardog-home2:/var/opt/stardog stardog/stardog
      [root@0b9fbb0b90ba bin]# export STARDOG_SERVER_JAVA_ARGS="-Xmx4g -Xms3g -XX:MaxDirectMemorySize=4g"
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin server start
      
      # Check if the db already exists
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin db list
      
      # If not, create it
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin db create -n NCIT2
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog data add --named-graph http://NCI_T NCIT2 /data/ThesaurusInferred.owl
      [root@0b9fbb0b90ba bin]# /opt/stardog/bin/stardog-admin db optimize -n NCIT2

* Running Stardog Locally (after data is loaded)

      dir=c:/Users/carlsenbr/eclipse-workspace/data/
      docker run -d --name=stardog_evs --rm -p 5820:5820 -v "$dir":/data -v stardog-home2:/var/opt/stardog -e STARDOG_SERVER_JAVA_ARGS="-Xmx4g -Xms3g -XX:MaxDirectMemorySize=4g" stardog/stardog

* Log into a Running Stardog Container

      docker exec -it <container_id, e.g. 3c29d72babc2> /bin/bash

* Running a sparql query (assumes stardog running on localhost and curl and jq are installed)

Start by configuring your environment

```
STARDOG_HOST=localhost
STARDOG_PORT=5820
STARDOG_USERNAME=admin
STARDOG_PASSWORD=admin
STARDOG_URL=http://${STARDOG_HOST}:${STARDOG_PORT}
STARDOG_DB=NCIT2
```

Next, put your query into a file.  Make sure to include prefixes and the correct graph name.

```
cat > query.txt << EOF
PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl> 
PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>
PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX dc:<http://purl.org/dc/elements/1.1/>
PREFIX oboInOwl:<http://www.geneontology.org/formats/oboInOwl#>
PREFIX xml:<http://www.w3.org/2001/XMLSchema#>
SELECT ?conceptCode ?conceptLabel
{ GRAPH <http://NCI_T_monthly> 
    { 
        ?concept a owl:Class . 
        OPTIONAL { ?concept :P108 ?conceptLabel } .
        ?concept :NHC0 ?conceptCode
    }
}
ORDER BY ?conceptCode
EOF
```

Then, run the query.

```
q=`cat query.txt`
curl -v -g -u "${STARDOG_USERNAME}:$STARDOG_PASSWORD" "$STARDOG_URL/NCIT2/query" --data-urlencode "query=$q" -H "Accept: application/sparql-results+json" | jq
```
