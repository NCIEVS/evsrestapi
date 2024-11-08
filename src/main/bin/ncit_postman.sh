#!/bin/bash

# Bash script to run a Postman collection with a dynamically calculated API_URL

# TODO: Calculate or fetch the API_URL
# Replace this placeholder with the actual method to determine API_URL.
API_URL="http://localhost:8082/api/v1"  # Placeholder URL; update as needed.
# https://api-evsrest.nci.nih.gov - Prod API_URL
# https://api-test-evsrest.nci.nih.gov - Test API_URL
# https://api-qa-evsrest.nci.nih.gov - QA API_URL

# Ensure API_URL is set in the environment
if [ -z "$API_URL" ]; then
    echo "Error: API_URL is not set. Please export API_URL before running this script."
    exit 1
fi

# Define the Postman collection and environment file paths
COLLECTION_FILE="EVSRESTAPI_Postman_NCIt_Demo.postman_collection.json"

# Verify if newman is installed
if ! command -v newman &> /dev/null; then
    echo "Newman is not installed. Attempting to install it locally..."
    # Try installing Newman locally
    npm install newman
    if [ $? -ne 0 ]; then
        echo "Error: Newman installation failed. Please install it manually."
        exit 2
    fi
fi

# Run the Postman collection using newman and track the exit status
echo "Running the Postman collection with API_URL=$API_URL..."
newman run "$COLLECTION_FILE" --global-var "API_URL=$API_URL"
newman_status=$?

# Check if newman run succeeded
if [ $newman_status -eq 0 ]; then
    echo "Postman collection ran successfully."
    exit 0
else
    echo "Error: Postman collection failed with status code $newman_status."
    exit $newman_status
fi