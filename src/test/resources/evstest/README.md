# EVS Test Terminology

A small fake terminology for testing the query nodes end to end: load it into the graph database,
index it with the real graph loader, then search the result. Each concept is there for a specific
behavior; the comments in `EvsTest.ttl` say which.

| File | What it is |
|---|---|
| `EvsTest.ttl` | The terminology, in Turtle. The ontology IRI `http://evs.example/fake/EvsTest.owl` names it `evstest`; version `26.11a`. |
| `evstest.json` | Its metadata: which properties hold the code, name, synonyms, status, subsets, and so on. The loader reads it from `CONFIG_BASE_URI`. |
| `evstest.html` | Welcome text. The loader fails without it. |
| `ignore-source.txt` | Graphs `reindex.sh` skips. Needed when `CONFIG_BASE_URI` points at this directory. |

## Loading and indexing

Run from the project root, with Fuseki and OpenSearch running and the usual dev environment
variables set (`GRAPH_DB_HOST`, `GRAPH_DB_PORT`, `GRAPH_DB_USERNAME`, `GRAPH_DB_PASSWORD`,
`ES_SCHEME`, `ES_HOST`, `ES_PORT`), and the jar built (`make build`).

1. Load the Turtle into the `NCIT2` database as the graph `http://EvsTest`. `PUT` replaces the
   graph, so rerunning it doesn't duplicate triples (`devreset.sh` uses `POST`, which appends).

   ```bash
   curl -u "$GRAPH_DB_USERNAME:$GRAPH_DB_PASSWORD" -X PUT -H 'Content-Type: text/turtle' -T src/test/resources/evstest/EvsTest.ttl "http://$GRAPH_DB_HOST:$GRAPH_DB_PORT/NCIT2/data?graph=http://EvsTest"
   ```

2. Index it, reading the metadata from this directory. `--terminology evstest` limits the run to
   this terminology, and `--forceDeleteIndex` (set by the script) rebuilds the index each time.

   ```bash
   CONFIG_BASE_URI="file://$PWD/src/test/resources/evstest" src/main/bin/reindex.sh --noconfig --terminology evstest
   ```

   This creates the concept index `concept_evstest_2611a`.

3. Check it loaded:

   ```bash
   curl -s "http://localhost:8082/api/v1/concept/evstest/FK100?include=full"
   ```

To remove it, delete the graph and the index:

```bash
curl -u "$GRAPH_DB_USERNAME:$GRAPH_DB_PASSWORD" -X DELETE "http://$GRAPH_DB_HOST:$GRAPH_DB_PORT/NCIT2/data?graph=http://EvsTest"
```

```bash
curl -X DELETE "$ES_SCHEME://$ES_HOST:$ES_PORT/concept_evstest_2611a"
```

## Not checked yet

These steps follow `devreset.sh` and `reindex.sh` but haven't been run. In particular,
`reindex.sh` ends by running `--terminology reconcile --skipLoad` across every loaded terminology,
and with `CONFIG_BASE_URI` pointing here, only `evstest`'s metadata can be found.
