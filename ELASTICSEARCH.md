# EVSRESTAPI - ELASTICSEARCH SETUP

Information on downloading and using ELASTICSEARCH with EVSRESTAPI.

## Running Elasticsearch Locally

In a terminal, run the following to have an elasticsearch instance running. Keep this window open to keep the server running.

      docker pull docker.elastic.co/elasticsearch/elasticsearch:6.7.0
      # Choose a directory for your elasticsearch data to live
      dir=c:/Users/carlsenbr/eclipse-workspace/data/elasticsearch
      docker run -d --name=es_evs --rm -p 9200:9200 -v "$dir":/usr/share/elasticsearch/data  -e "discovery.type=single-node" -e ES_JAVA_OPTS="-Xms1g -Xmx5g" docker.elastic.co/elasticsearch/elasticsearch:6.7.0

