# EVSRESTAPI - ELASTICSEARCH SETUP

Information on downloading and using ELASTICSEARCH with EVSRESTAPI.
Verify the $ES_DIR variable points to the correct path set up in previous steps. If not, set that variable now. 

## Running Elasticsearch Locally

In a terminal, run the following to have an elasticsearch instance running. Keep this window open to keep the server running.

      docker pull docker.elastic.co/elasticsearch/elasticsearch:6.7.0

      docker run -d --name=es_evs --rm -p 9200:9200 -v "$ES_DIR":/usr/share/elasticsearch/data  -e "discovery.type=single-node" -e ES_JAVA_OPTS="-Xms1g -Xmx5g" docker.elastic.co/elasticsearch/elasticsearch:6.7.0

