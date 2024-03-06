# EVSRESTAPI - ELASTICSEARCH SETUP

Information on downloading and using ELASTICSEARCH with EVSRESTAPI.

Verify the $ES_DIR variable points to the correct path set up in previous steps. If not, set it now: </br>`export set
ES_DIR=c:$dir/elasticsearch/data`

## Running Elasticsearch Locally

In a terminal, run the following to have an elasticsearch instance running. Keep this window open to keep the server running.

    docker network create elasticsearch  
    docker pull docker.elastic.co/elasticsearch/elasticsearch:7.12.1
    docker run -d --name=es_evs --net elasticsearch --rm -p $ES_PORT:9200 -v "$ES_DIR":/usr/share/elasticsearch/data -e "xpack.security.enabled=false" -e "discovery.type=single-node" -e ES_JAVA_OPTS="-Xms1g -Xmx5g" docker.elastic.co/elasticsearch/elasticsearch:7.12.1