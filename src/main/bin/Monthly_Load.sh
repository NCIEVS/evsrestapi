#!/bin/sh

USER=xxxxroot
PASSWORD=xxxxx
dir=../data/UnitTestData

export JAVA_HOME=/usr/local/jdk1.8
./stardog data remove --all  NCIEVS -u $USER -p $PASSWORD
./stardog data remove --all  CTRP   -u $USER -p $PASSWORD
./stardog data add NCIEVS -g http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl /local/content/triplestore/stardog/bin/ThesaurusInferred_forTS.owl -u $USER -p $PASSWORD
./stardog data add NCIEVS -g http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.rdf /local/content/triplestore/stardog/bin/ThesaurusInferred_forTS.rdf -u $USER -p $PASSWORD
./stardog data add CTRP -g http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl   /local/content/triplestore/stardog/bin/ThesaurusInferred_forTS.owl -u $USER -p $PASSWORD
./stardog data add CTRP -g http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.rdf   /local/content/triplestore/stardog/bin/ThesaurusInferred_forTS.rdf -u $USER -p $PASSWORD

# Optimize stardog databases
./stardog-admin db optimize -n CTRP
./stardog-admin db optimize -n NCIEVS

# Trigger downstream jobs
rm /admfs/triplestore/*
cp ./qa_ready_monthly /admfs/triplestore/qa_ready


#--------
# Load NCIt - NCI Thesaurus
#

version=`unzip -p /local/content/downloads/ThesaurusInferred_forTS.zip "*ThesaurusInferred_forTS.owl" |\
   grep '<owl:versionInfo>' | perl -pe 's/.*<owl:versionInfo>//; s/<\/owl:versionInfo>//'`

# Use something like this to find versions to remove - and remove by graph instead of --all
# ./stardog query execute NCIT2 "select distinct ?graphName where { graph ?graphName {?s ?p ?o} }" | grep http | grep -v $version

graph=http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus$version.owl
./stardog data remove --all CTRP -u $USER -p $PASSWORD
./stardog data add CTRP -g $graph /local/content/downloads/ThesaurusInferred_forTS.zip -u $USER -p $PASSWORD

./stardog data remove --all NCIT2 -u $USER -p $PASSWORD
./stardog data add NCIT2 -g $graph /local/content/downloads/ThesaurusInferred_forTS.zip -u $USER -p $PASSWORD

#--------
# Load GO - Gene Ontology
#

echo "  Set GO version and graph"
version=`grep 'data-version' $dir/GO/go.obo | perl -pe 's/.*\: //; s/releases\///;'`
graph=http://purl.obolibrary.org/obo/go${version}.owl
echo"    version = $version"
echo"    graph = $graph"

# Clean and load stardog
echo "  Remove stardog databases and load monthly/weekly"
cat > $dir/x.sh << EOF
#!/bin/bash
echo "    load data"
/opt/stardog/bin/stardog data add --named-graph http://GO_monthly NCIT2 /data/UnitTestData/GO/go.owl | sed 's/^/     /'
echo "    optimize databases"
/opt/stardog/bin/stardog-admin db optimize -n NCIT2 | sed 's/^/      /'
EOF
chmod 755 $dir/x.sh
chmod ag+rwx $dir
pid=`docker ps | grep stardog/stardog | cut -f 1 -d\  `
# note: //data is required for gitbash
docker exec -it $pid //data/UnitTestData/x.sh
if [[ $? -ne 0 ]]; then
    echo "ERROR: problem loading stardog"
    exit 1
fi
/bin/rm -f $dir/x.sh


