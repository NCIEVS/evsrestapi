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

The temporary deployment and local target is **OpenSearch 1.3.20**. Spring Data OpenSearch
**3.1.4** is the Java integration library version, not the server version. Upstream supports
OpenSearch 2.x and 3.x; our temporary 1.x workaround does not change that support matrix. See the upstream
[compatibility matrix](https://github.com/opensearch-project/spring-data-opensearch/#opensearch-3x--2x-client-libraries).

**Temporary 1.3 workaround:** the newer Java client sends `cluster_manager_timeout`, which
1.3.20 rejects. Following Termhub, `OpensearchLegacyCompatibilityInterceptor` is always registered
and rewrites that query parameter to `master_timeout`; there is no configuration toggle.
This lets Java 25/Boot 4 deploy before the OpenSearch server upgrade. It changes no request
bodies or other parameters and applies to both API and loader clients using this configuration.

### Compatibility checks

`OpenSearchLegacyCompatibilityInterceptorTest` exercises parameter boundaries and real loopback
HTTP requests through the configured client without requiring OpenSearch. For additional verification,
start a disposable OpenSearch 1.3.20 server bound to loopback and pass its published port:

```bash
LEGACY_OPENSEARCH_TEST_PORT=9202 ./gradlew test --tests '*OpenSearchLegacyCompatibilityIntegrationTest'
```

This opt-in test verifies the server is 1.3.20, creates a uniquely named test index, exercises mappings,
bulk indexing, reads, search, updates, refresh, and delete-by-query, and deletes that index afterward.
It does not run unless the environment variable is set. Use a disposable server for this check.

In a terminal, run the following to have an opensearch instance running on the background without security.

    docker run --rm -d -p 9201:9200 -p 9600:9600 -e "discovery.type=single-node" \
      -v "$OS_DIR":/usr/share/opensearch/data \
      -e DISABLE_SECURITY_PLUGIN=true \
      --name opensearch-node -d opensearchproject/opensearch:1.3.20

For 1.3.20, `$OS_DIR` must point to existing compatible 1.x storage or a new, empty directory.
**Do not reuse storage already opened by OpenSearch 2.x/3.x.** Downgrading the Docker image
does not downgrade its data. Preserve newer storage separately and use a compatible backup or
reload into fresh 1.x storage if returning from 2.18.0. This is a downgrade restriction, distinct
from the forward-upgrade guidance below.

## Upgrading existing data

### Moving to OpenSearch 2.18.0

1. Back up data/configuration, review compatibility, and upgrade the server using the procedure
   below. For Docker, the target image is `opensearchproject/opensearch:2.18.0`.
2. Once all target environments are upgraded, remove the legacy interceptor registration from
   `OpensearchConfiguration`, the interceptor class, and its legacy-only tests. Update the
   documented server target and Docker example to 2.18.0. Keep the current Java client libraries;
   no client-library downgrade was made for the workaround, so none needs to be reversed.
3. Rebuild and run the suite against 2.18.0 with `TEST_EMAIL_DISABLED=true make test`, including
   API/FHIR search and loader index-management checks, before deploying the rebuilt application.

Upgrading **1.3.20 to 2.18.0 does not inherently require reloading all indexes**. Compatible
indexes and the existing Docker data volume can be retained through the supported upgrade
procedure. Back up cluster data and configuration, check index/plugin compatibility and breaking
changes, and follow the [upgrade guidance](https://docs.opensearch.org/latest/migrate-or-upgrade/rolling-upgrade/).
Indexes inherited from older Elasticsearch releases may need separate reindexing.

Fresh storage and a test-data reload are an alternative for local testing. Keep old storage
separate if needed for recovery; never start two containers against the same data directory.

A **3.x** upgrade is different: do not mount the 1.x directory directly into a 3.x container.
The staged path goes through 2.19.x, and indexes created before 2.x must be reindexed before
upgrading to 3.x. A fresh cluster and reload is another option.

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
