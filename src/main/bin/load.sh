#!/bin/bash -f
#
# PREREQUISITE: This script requires a download of the "UnitTestData"
# (https://drive.google.com/drive/u/0/folders/1kXIr9J3jgO-8fN01LJwhNkOuZbAfaQBh)
# to a directory called "UnitTestData" that must live under whatever 
# directory is mounted as /data within the graph db container.  Thus, while in
# the graph db container the path /data/UnitTestData must be available.
#
# It resets the graph db and opensearch data sets locally to update to
# the latest dev testing data set at that google drive URL.
#
help=0
while [[ "$#" -gt 0 ]]; do case $1 in
  --help) help=1;;
  *) arr=( "${arr[@]}" "$1" );;
esac; shift; done

if [ $help == 1 ] || [ ${#arr[@]} -ne 1 ]; then
  echo "Usage: src/main/bin/load.sh \"c:/data/UnitTestData\""
  echo "  e.g. src/main/bin/load.sh ../data/UnitTestData"
  exit 1
fi
dir=${arr[0]}

# Hardcode the history file
historyFile=$dir/cumulative_history_21.06e.txt


databases=("NCIT2" "CTRP")
curl_cmd="curl -s -f -u ${GRAPH_DB_USERNAME}:${GRAPH_DB_PASSWORD}"

# Set up ability to format json
jq --help >> /dev/null 2>&1
if [[ $? -eq 0 ]]; then
    jq="jq ."
else
    jq="python -m json.tool"
fi

echo "--------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "--------------------------------------------------"
echo "dir = $dir"
echo ""
#set -e

# Check configuration
if [[ -z $GRAPH_DB_HOST ]]; then
    echo "ERROR: GRAPH_DB_HOST is not set"
    exit 1
elif [[ -z $GRAPH_DB_PORT ]]; then
    echo "ERROR: GRAPH_DB_PORT is not set"
    exit 1
elif [[ -z $GRAPH_DB_USERNAME ]]; then
    echo "ERROR: GRAPH_DB_USERNAME is not set"
    exit 1
elif [[ -z $GRAPH_DB_PASSWORD ]]; then
    echo "ERROR: GRAPH_DB_PASSWORD is not set"
    exit 1
fi

# Prerequisites - check the UnitTest
echo "  Check prerequisites"

# Check NCIt weekly
echo "    check NCIt weekly"
if [[ ! -e "$dir/ThesaurusInferred_+1weekly.owl" ]]; then
    echo "ERROR: unexpectedly ThesaurusInferred_+1weekly.owl file"
    exit 1
fi
# Check NCIt monthly
echo "    check NCIt monthly"
if [[ ! -e "$dir/ThesaurusInferred_monthly.owl" ]]; then
    echo "ERROR: unexpectedly ThesaurusInferred_monthly.owl file"
    exit 1
fi
# Check GO monthly
echo "    check GO monthly"
if [[ ! -e "$dir/GO/go.2022-07-01.owl" ]]; then
    echo "ERROR: unexpectedly missing GO/go.2022-07-01.owl file"
    exit 1
fi

# Check HGNC monthly
echo "    check HGNC monthly"
if [[ ! -e "$dir/HGNC/HGNC_202209.owl" ]]; then
    echo "ERROR: unexpectedly missing HGNC/HGNC_202209.owl file"
    exit 1
fi

# Check ChEBI monthly
echo "    check ChEBI monthly"
if [[ ! -e "$dir/ChEBI/chebi_213.owl" ]]; then
    echo "ERROR: unexpectedly missing ChEBI/chebi_213.owl file"
    exit 1
fi

# Check DUO
echo "    check DUO"
if [[ ! -e "$dir/DUO/duo_Feb21.owl" ]]; then
    echo "ERROR: unexpectedly missing DUO/duo_Feb21.owl file"
    exit 1
fi

# Check OBI
echo "    check OBI"
if [[ ! -e "$dir/OBI/obi_2022_07.owl" ]]; then
    echo "ERROR: unexpectedly missing OBI/obi_2022_07.owl file"
    exit 1
fi

# Check OBIB
echo "    check OBIB"
if [[ ! -e "$dir/OBIB/obib_2021-11.owl" ]]; then
    echo "ERROR: unexpectedly missing OBI/obib_2021-11.owl file"
    exit 1
fi

# Check NDFRT
echo "    check NDFRT"
if [[ ! -e "$dir/NDFRT/NDFRT_Public_2018.02.05_Inferred.owl" ]]; then
    echo "ERROR: unexpectedly missing NDFRT/NDFRT_Public_2018.02.05_Inferred.owl file"
    exit 1
fi

# Verify docker is running
echo "    verify jena/fuseki database is running"
$curl_cmd -s -f "http://${GRAPH_DB_HOST}:${GRAPH_DB_PORT}/$/ping" > /dev/null
if [[ $? -ne 0 ]]; then
    echo "Jena is not running"
    exit 1
fi


drop_databases(){
  for db in "${databases[@]}"
  do
    echo "    Dropping $db"
    $curl_cmd -X DELETE "http://${GRAPH_DB_HOST}:${GRAPH_DB_PORT}/$/datasets/${db}" > /dev/null
    if [[ $? -ne 0 ]]; then
        echo "Error occurred when dropping database ${db}. Response:$_"
        exit 1
    fi
  done
}

create_databases(){
  for db in "${databases[@]}"
  do
    echo "    Creating $db"
    $curl_cmd -X POST -d "dbName=${db}&dbType=tdb2" "http://${GRAPH_DB_HOST}:${GRAPH_DB_PORT}/$/datasets" > /dev/null
    if [[ $? -ne 0 ]]; then
        echo "Error occurred when creating database ${db}. Response:$_"
        exit 1
    fi
  done
}

load_terminology_data_in_transaction(){
  echo "    Loading $3 into $1"
  tx=$(curl -s -u "${GRAPH_DB_USERNAME}":"${GRAPH_DB_PASSWORD}" -X POST "http://localhost:5820/$1/transaction/begin")
  curl -s -u "${GRAPH_DB_USERNAME}":"${GRAPH_DB_PASSWORD}" -X POST "http://localhost:5820/$1/${tx}/add?graph-uri=$2" -H "Content-Type: application/rdf+xml" -T - < "$dir/$3"
  tx=$(curl -s -u "${GRAPH_DB_USERNAME}":"${GRAPH_DB_PASSWORD}" -X POST "http://localhost:5820/NCIT2/transaction/commit/${tx}")
  if [[ $? -ne 0 ]]; then
      echo "Error occurred when loading data into $1. Response:$_"
      exit 1
  fi
}

load_terminology_data(){
  echo "    Loading $3 into $1"
  $curl_cmd -X POST -H "Content-Type: application/rdf+xml" -T "$dir/$3" "http://${GRAPH_DB_HOST}:${GRAPH_DB_PORT}/$1/data?graph=$2" > /dev/null
  if [[ $? -ne 0 ]]; then
      echo "Error occurred when loading data into $1. Response:$_"
      exit 1
  fi
}

load_data(){
    load_terminology_data CTRP http://NCI_T_weekly ThesaurusInferred_+1weekly.owl
    load_terminology_data CTRP http://NCI_T_monthly ThesaurusInferred_monthly.owl
    load_terminology_data NCIT2 http://NCI_T_monthly ThesaurusInferred_monthly.owl
    load_terminology_data NCIT2 http://GO_monthly GO/go.2022-07-01.owl
    load_terminology_data NCIT2 http://HGNC_monthly HGNC/HGNC_202209.owl
    load_terminology_data NCIT2 http://ChEBI_monthly ChEBI/chebi_213.owl
    load_terminology_data NCIT2 http://UmlsSemNet UmlsSemNet/umlssemnet.owl
    load_terminology_data NCIT2 http://MEDRT MED-RT/medrt.owl
    load_terminology_data NCIT2 http://Canmed CanMed/canmed.owl
    load_terminology_data NCIT2 http://CTCAE CTCAE/ctcae5.owl
    load_terminology_data NCIT2 http://DUO_monthly DUO/duo_Feb21.owl
    load_terminology_data NCIT2 http://DUO_monthly DUO/iao_Dec20.owl
    load_terminology_data NCIT2 http://OBI_monthly OBI/obi_2022_07.owl
    load_terminology_data NCIT2 http://OBIB OBIB/obib_2021-11.owl
    load_terminology_data NCIT2 http://NDFRT2 NDFRT/NDFRT_Public_2018.02.05_Inferred.owl
    load_terminology_data NCIT2 http://MGED MGED/MGEDOntology.fix.owl
    load_terminology_data NCIT2 http://NPO NPO/npo-2011-12-08_inferred.owl
    load_terminology_data NCIT2 http://MA Mouse_Anatomy/ma_07_27_2016.owl
    load_terminology_data NCIT2 http://Zebrafish Zebrafish/zfa_2019_08_02.owl
}

# Clean and load
echo "  Remove databases and load monthly/weekly"
drop_databases
create_databases
load_data

# Cleanup
/bin/rm -f /tmp/x.$$.txt $dir/x.{sh,txt}

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"