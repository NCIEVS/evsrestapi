# EVSRESTAPI - OPENSEARCH SETUP

Information on downloading and using OPENSEARCH with EVSRESTAPI.

Verify the $OS_DIR variable points to the correct path set up in previous steps. If not, set it now: </br>`export set
OS_DIR=c:$dir/opensearch/data`

See https://github.com/opensearch-project/spring-data-opensearch for setting up the project

## Running Opensearch 1.3.x Locally

In a terminal, run the following to have an opensearch instance running on the background without security.

    docker run --rm -d -p 9201:9200 -p 9600:9600 -e "discovery.type=single-node" \
      -v "$OS_DIR":/usr/share/opensearch/data \
      -e DISABLE_SECURITY_PLUGIN=true \
      --name opensearch-node -d opensearchproject/opensearch:1.3.19

## Running Opensearch 2.x.x Locally

In a terminal, run the following to have an opensearch instance running on the background without security.

    docker run --rm -d -p 9201:9200 -p 9600:9600 -e "discovery.type=single-node" \
      -v "$OS_DIR":/usr/share/opensearch/data \
      -e DISABLE_SECURITY_PLUGIN=true \
      --name opensearch-node -d opensearchproject/opensearch:2.18.0
  
## References

* Documentation - https://opensearch.org/docs/latest/
* Docker info - https://hub.docker.com/r/opensearchproject/opensearch
* Tags - https://hub.docker.com/r/opensearchproject/opensearch/tags
