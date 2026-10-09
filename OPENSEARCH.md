# EVSRESTAPI - OPENSEARCH SETUP

Information on downloading and using OPENSEARCH with EVSRESTAPI.

Verify the $OS_DIR variable points to the correct path set up in previous steps. If not, set it now.

Examples:

- Bash (Git Bash / WSL): `export OS_DIR="$dir/opensearch/data"`
- Windows CMD: `set OS_DIR=%dir%\\opensearch\\data`

See https://github.com/opensearch-project/spring-data-opensearch for setting up the project

NOTE: the examples below use 9201 as a port so as to avoid conflicting with other elasticsearch implementations running on 9200.
When operating this tool, `export ES_PORT=9201` when using the configuration below.

## Running OpenSearch Locally

Spring Data OpenSearch 3.x requires an OpenSearch 2.x or 3.x server. OpenSearch 1.x is no longer
supported by this application. Use a new data directory when replacing a 1.x server, then reload
the test data; a 1.x data directory cannot be mounted directly into a newer major version.

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
curl -X PUT $ES/_cluster/settings -H "Content-type: application/json" \
  -d '{  "persistent": { "index.refresh_interval": "60s" } }
```

A call like this in AWS needs to be done from a machine with an IAM role.

```
{
  "Effect": "Allow",
  "Principal": {
    "AWS": "arn:aws:iam::<YOUR_AWS_ACCOUNT_ID>:user/<YOUR_IAM_USERNAME>"
  },
  "Action": [
    "es:ESHttpPut",
    "es:admin/cluster/settings/update"
  ],
  "Resource": "arn:aws:es:<YOUR_AWS_REGION>:<YOUR_AWS_ACCOUNT_ID>:domain/<YOUR_DOMAIN_NAME>/*"
}
```

## References

* Documentation - https://opensearch.org/docs/latest/
* Docker info - https://hub.docker.com/r/opensearchproject/opensearch
* Tags - https://hub.docker.com/r/opensearchproject/opensearch/tags
