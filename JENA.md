# EVSRESTAPI - JENA SETUP

Information on using Apache jena/fuseki with EVSRESTAPI.

## Build and run a local docker image

Run the following command to build the Jena image:

```bash
cd docker/fuseki
docker build -t evsrestapi/fuseki:5.1.0  .
```

### Running Jena Locally (after data is loaded)

Start the container with the following command. 
Note: you need the src mount path to a local directory of your choice to persist the data.

```bash
dir=c:/Users/carlsenbr/eclipse-workspace/data/fuseki
docker run -d --name=jena_evs --rm -p "3030:3030" -v"$dir":/opt/fuseki/run/databases evsrestapi/fuseki:5.1.0
```

