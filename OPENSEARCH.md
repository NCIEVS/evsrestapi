# EVSRESTAPI - OPENSEARCH SETUP

Information on downloading and using OPENSEARCH with EVSRESTAPI.

Verify the $OS_DIR variable points to the correct path set up in previous steps. If not, set it now: </br>`export set
OS_DIR=c:$dir/opensearch/data`

See https://github.com/opensearch-project/spring-data-opensearch for setting up the project

NOTE: the examples below use 9201 as a port so as to avoid conflicting with other elasticsearch implementations running on 9200.
When operating this tool, `export ES_PORT=9201` when using the configuration below.

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

## Configure refresh interval globally

To prevent "too many requests" issue from happening while loader is running, it may be useful
to set the refresh interval higher.  Making it higher than 5s requires review of loader
code to ensure it is not trying to read back data earlier than 5s after a required prior
indexing operation completes (Review `Thread.sleep` calls in the code).

```
curl -X PUT "$ES/_settings" -H 'Content-Type: application/json' \
  -d '{ "index": { "refresh_interval": "5s" } }'
```

## References

* Documentation - https://opensearch.org/docs/latest/
* Docker info - https://hub.docker.com/r/opensearchproject/opensearch
* Tags - https://hub.docker.com/r/opensearchproject/opensearch/tags
