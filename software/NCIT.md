# EVSRESTAPI - NCIT Maintenance Tasks

Information on using evsrestapi-util for maintenance tasks.

## Computing SPARQL queries for roles/subclasses

The roles and subclasses queries in sparql-queries.properties
may need to change from time to time as patterns within NCIt OWL
change over time.  Periodically (e.g. quarterly/bi-annually),
it is desirable to recompute the roles and subclasses sparql queries
to understand whether new patterns need to be supported.

### Steps

1. Build software/evsapi-util to create the evsrestapi-util.jar file (requires `ant` installation)


2. Generate roles

java -d64 -Xms512m -Xmx4g -classpath evsrestapi-util.jar gov.nih.nci.evs.restapi.util.SPARQLQueryGenerator /path/to//ThesaurusInferred.owl  roles

Use the output to update all "roles" entries in `sparql-queries.properties`)

3. Generate subclasses

java -d64 -Xms512m -Xmx4g -classpath evsrestapi-util.jar gov.nih.nci.evs.restapi.util.SPARQLQueryGenerator /path/to//ThesaurusInferred.owl  subclasses

Use the output to update all "hierarchy" entries in `sparql-queries.properties`)

