#!/bin/bash

# Start the fuseki-server
./fuseki start

# Wait for the server to start
sleep 10

# Create the NCIT2 dataset
curl -s -g -X POST -d "dbName=NCIT2&dbType=tdb2" "http://localhost:3030/\$/datasets"

# Create the CTRP dataset
curl -s -g -X POST -d "dbName=CTRP&dbType=tdb2" "http://localhost:3030/\$/datasets"

# Keep the container running
tail -f /dev/null