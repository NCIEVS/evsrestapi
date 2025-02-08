import json
import sys

db = sys.argv[1]
data = json.load(sys.stdin)

bindings = data["results"]["bindings"]

for binding in bindings:
    version = binding["version"]["value"]
    graph_name = binding["graphName"]["value"]
    source = binding["source"]["value"]
    print("|".join((version, db, graph_name, source)))

