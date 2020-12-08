#!/bin/sh

USER=xxxxroot
PASSWORD=xxxxx

version=`unzip -p /local/content/downloads/ThesaurusInferred_forTS.zip "*ThesaurusInferred_forTS.owl" |\
   grep '<owl:versionInfo>' | perl -pe 's/.*<owl:versionInfo>//; s/<\/owl:versionInfo>//'`

# Use something like this to find versions to remove - and remove by graph instead of --all
# ./stardog query execute NCIT2 "select distinct ?graphName where { graph ?graphName {?s ?p ?o} }" | grep http | grep -v $version

graph=http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus$version.owl
./stardog data remove --all CTRP -u $USER -p $PASSWORD
./stardog data add CTRP -g $graph /local/content/downloads/ThesaurusInferred_forTS.zip -u $USER -p $PASSWORD

./stardog data remove --all NCIT2 -u $USER -p $PASSWORD
./stardog data add NCIT2 -g $graph /local/content/downloads/ThesaurusInferred_forTS.zip -u $USER -p $PASSWORD
