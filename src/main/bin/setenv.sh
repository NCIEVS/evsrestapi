
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK="WARN"
export LOGGING_LEVEL_GOV_NIH_NCI_EVS_API="INFO"
export LOGGING_LEVEL_SPRING_DATA_ES="INFO"

export EVS_SERVER_PORT="8080"
export NCI_EVS_API_PATH="/api/v1"

# true or false
export METRICS_ENABLED="EDIT_THIS"

export STARDOG_HOST="EDIT_THIS, e.g. without https://"
export STARDOG_PORT=5820
export STARDOG_DB="EDIT_THIS, e.g. NCIT2"
export STARDOG_USERNAME="REPLACE_WITH_USERNAME"
export STARDOG_PASSWORD="REPLACE_WITH_PASSWORD"
export STARDOG_READ_TIMEOUT="60"
export STARDOG_CONNECT_TIMEOUT="60"

export NCI_EVS_BULK_LOAD_DOWNLOAD_BATCH_SIZE=1000
export NCI_EVS_BULK_LOAD_INDEX_BATCH_SIZE=100
export ES_HOST="EDIT_THIS, e.g. without https://"
export ES_PORT="EDIT_THIS, e.g. 443"
export ES_SCHEME="EDIT_THIS, e.g. https"
export ES_CLEAN=false

export MAIL_HOST="EDIT_THIS, e.g. use your test smtp host"
export MAIL_USER="REPLACE_WITH_USERNAME"
export MAIL_PASSWORD="REPLACE_WITH_PASSWORD"

export DATA_DIR=/evs/data/ncim

# Obtaining data/config from evsrestapi-operations
# Replace "main" with "develop" for 
export CONFIG_BASE_URI=https://raw.githubusercontent.com/NCIEVS/evsrestapi-operations/main/config/metadata

