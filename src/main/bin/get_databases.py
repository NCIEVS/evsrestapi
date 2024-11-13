import json
import sys

if len(sys.argv) > 1:
    GRAPH_DB_TYPE = sys.argv[1]
else:
    raise Exception("Expecting graph db type as a script argument")

data = json.load(sys.stdin)

if GRAPH_DB_TYPE.lower() == "stardog":
    for db in data["databases"]:
        print(db)
elif GRAPH_DB_TYPE.lower() == "jena":
    for db in data["datasets"]:
        print(db["ds.name"].replace("/", ""))
else:
    raise Exception("Unknown graph DB")
