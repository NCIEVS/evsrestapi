#!/bin/bash

# Start the fuseki-server (foreground runner, backgrounded here)
export JAVA_OPTIONS="-Xmx8000M"
export DEBUG=1
./fuseki-server &
FUSEKI_PID=$!

# These steps are needed to ensure re-mounting the volume works properly
# Wait until server is up
echo "Waiting for Fuseki to finish starting up..."
until $(curl --output /dev/null --silent --head --fail http://localhost:3030); do
  sleep 1s
done

# Create the NCIT2 and CTRP datasets
curl -s -g -X POST -d "dbName=NCIT2&dbType=tdb2" "http://localhost:3030/\$/datasets"
curl -s -g -X POST -d "dbName=CTRP&dbType=tdb2" "http://localhost:3030/\$/datasets"

# Keep the container running by waiting on the fuseki process
echo "keep the container by waiting on the fuseki-server process"
wait $FUSEKI_PID
