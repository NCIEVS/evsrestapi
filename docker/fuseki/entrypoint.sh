#!/bin/bash

# Start the fuseki-server
export JAVA_OPTIONS="-Xmx8000M"
export DEBUG=1
./fuseki start

# Wait for the server to start
echo "Wait 10 seconds to create databases"
sleep 10

# These steps are needed to ensure re-mounting the volume works properly
# Create the NCIT2 dataset
curl -s -g -X POST -d "dbName=NCIT2&dbType=tdb2" "http://localhost:3030/\$/datasets"
# Create the CTRP dataset
curl -s -g -X POST -d "dbName=CTRP&dbType=tdb2" "http://localhost:3030/\$/datasets"

# Keep the container running
echo "do a tail -f /dev/null to keep the container running"
tail -f /dev/null
