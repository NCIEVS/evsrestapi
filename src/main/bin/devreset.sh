#!/bin/bash -f
#
# PREREQUISITE: This script requires a download of the "UnitTestData"
# (https://drive.google.com/drive/u/0/folders/1kXIr9J3jgO-8fN01LJwhNkOuZbAfaQBh)
# to a directory called "UnitTestData" that must live under whatever 
# directory is mounted as /data within the stardog container.  Thus, while in
# the stardog container the path /data/UnitTestData must be available.
#
# It resets the stardog and elasticsearch data sets locally to update to
# the latest dev testing data set at that google drive URL.
#
help=0
while [[ "$#" -gt 0 ]]; do case $1 in
  --help) help=1;;
  *) arr=( "${arr[@]}" "$1" );;
esac; shift; done

if [ $help == 1 ] || [ ${#arr[@]} -ne 2 ]; then
  echo "Usage: src/main/bin/devreset.sh \"c:/data/UnitTestData\" stardog"
  echo "  e.g. src/main/bin/devreset.sh ../data/UnitTestData stardog"
  exit 1
fi
dir=${arr[0]}
db_type=${arr[1]}
# Hardcode the history file
historyFile=$dir/cumulative_history_21.06e.txt


if [[ $db_type = "stardog" ]]; then
  databases=("NCIT2" "CTRP")
  curl_cmd="curl -s -f -u ${STARDOG_USERNAME}:${STARDOG_PASSWORD}"
elif [[ $db_type = "jena" ]]; then
  databases=("NCIT2" "CTRP")
  curl_cmd="curl -s -f"
fi

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
echo "db_type = $db_type"
echo ""
#set -e

# Check configuration
if [[ -z $STARDOG_HOST ]]; then
    echo "ERROR: STARDOG_HOST is not set"
    exit 1
elif [[ -z $STARDOG_PORT ]]; then
    echo "ERROR: STARDOG_PORT is not set"
    exit 1
elif [[ -z $STARDOG_USERNAME ]]; then
    echo "ERROR: STARDOG_USERNAME is not set"
    exit 1
elif [[ -z $STARDOG_PASSWORD ]]; then
    echo "ERROR: STARDOG_PASSWORD is not set"
    exit 1
elif [[ -z $ES_SCHEME ]]; then
    echo "ERROR: ES_SCHEME is not set"
    exit 1
elif [[ -z $ES_HOST ]]; then
    echo "ERROR: ES_HOST is not set"
    exit 1
elif [[ -z $ES_PORT ]]; then
    echo "ERROR: ES_PORT is not set"
    exit 1
fi

# Prerequisites - check the UnitTest
echo "  Check prerequisites"

# Check that reindex.sh is at src/main/bin
if [[ ! -e "src/main/bin/reindex.sh" ]]; then
    echo "ERROR: src/main/bin/reindex.sh does not exist, run from top-level evsrestapi directory"
    exit 1
fi

# Check NCIM
echo "    check NCIM"
ct=`ls $dir/NCIM | grep RRF | wc -l`
if [[ $ct -le 20 ]]; then
    echo "ERROR: unexpectedly small number of NCIM/*RRF files = $ct"
    exit 1
fi
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

# Verify docker stardog is running
echo "    verify $db_type database is running"
if [[ $db_type = "stardog" ]]; then
  $curl_cmd "http://${STARDOG_HOST}:${STARDOG_PORT}/admin/healthcheck" > /dev/null
elif [[ $db_type = "jena" ]]; then
  $curl_cmd -s -f "http://${STARDOG_HOST}:${STARDOG_PORT}/$/ping" > /dev/null
fi
if [[ $? -ne 0 ]]; then
    echo "$db_type is not running"
    exit 1
fi


# Verify elasticsearch can be reached
echo "    verify elasticsearch can be reached"
curl -s "$ES_SCHEME://$ES_HOST:$ES_PORT/_cat/indices" >> /dev/null
if [[ $? -ne 0 ]]; then
    echo "ERROR: problem connecting to elasticsearch"
    exit 1
fi

# Remove elasticsearch indexes
remove_elasticsearch_indexes(){
  echo "  Remove elasticsearch indexes"
  curl -s "$ES_SCHEME://$ES_HOST:$ES_PORT/_cat/indices" | cut -d\  -f 3 | egrep "metrics|concept|evs" | grep -v "snomed" | cat > /tmp/x.$$.txt
  if [[ $? -ne 0 ]]; then
      echo "ERROR: problem connecting to docker elasticsearch"
      exit 1
  fi
  for i in `cat /tmp/x.$$.txt`; do
      echo "    remove $i"
      curl -s -X DELETE "$ES_SCHEME://$ES_HOST:$ES_PORT/$i" >> /dev/null
      if [[ $? -ne 0 ]]; then
          echo "ERROR: problem removing elasticsearch index $i"
          exit 1
      fi
  done
}
# Reindex ncim - individual terminologies
reindex_ncim(){
  for t in MDR ICD10CM ICD9CM LNC SNOMEDCT_US RADLEX PDQ ICD10 HL7V3.0; do
      # Keep the NCIM folder around while we run
      echo "Load $t (from downloaded data)"
      src/main/bin/ncim-part.sh --noconfig $dir/NCIM --keep --terminology $t > /tmp/x.$$.txt 2>&1
      if [[ $? -ne 0 ]]; then
          cat /tmp/x.$$.txt | sed 's/^/    /'
          echo "ERROR: loading $t"
          exit 1
      fi
  done
  # Reindex ncim - must run after the prior section so that maps can connect to loaded terminologies
  echo "  Reindex ncim"
  src/main/bin/ncim-part.sh --noconfig $dir/NCIM > /tmp/x.$$.txt 2>&1
  if [[ $? -ne 0 ]]; then
      cat /tmp/x.$$.txt | sed 's/^/    /'
      echo "ERROR: problem running ncim-part.sh"
      exit 1
  fi
}

drop_databases(){
  for db in "${databases[@]}"
  do
    echo "    Dropping $db"
    if [[ $db_type = "stardog" ]]; then
      $curl_cmd -X DELETE "http://${STARDOG_HOST}:${STARDOG_PORT}/admin/databases/${db}" > /dev/null
    elif [[ $db_type = "jena" ]]; then
      $curl_cmd -X DELETE "http://${STARDOG_HOST}:${STARDOG_PORT}/$/datasets/${db}" > /dev/null
    fi
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
    if [[ $db_type = "stardog" ]]; then
      $curl_cmd -X POST -F root="{\"dbname\":\"${db}\"}"  "http://${STARDOG_HOST}:${STARDOG_PORT}/admin/databases" > /dev/null
    elif [[ $db_type = "jena" ]]; then
      $curl_cmd -X POST -d "dbName=${db}&dbType=tdb2" "http://${STARDOG_HOST}:${STARDOG_PORT}/$/datasets" > /dev/null
    fi
    if [[ $? -ne 0 ]]; then
        echo "Error occurred when creating database ${db}. Response:$_"
        exit 1
    fi
  done
}

load_terminology_data_in_transaction(){
  echo "    Loading $3 into $1"
  tx=$(curl -s -u "${STARDOG_USERNAME}":"${STARDOG_PASSWORD}" -X POST "http://localhost:5820/$1/transaction/begin")
  curl -s -u "${STARDOG_USERNAME}":"${STARDOG_PASSWORD}" -X POST "http://localhost:5820/$1/${tx}/add?graph-uri=$2" -H "Content-Type: application/rdf+xml" -T - < "$dir/$3"
  tx=$(curl -s -u "${STARDOG_USERNAME}":"${STARDOG_PASSWORD}" -X POST "http://localhost:5820/NCIT2/transaction/commit/${tx}")
  if [[ $? -ne 0 ]]; then
      echo "Error occurred when loading data into $1. Response:$_"
      exit 1
  fi
}

load_terminology_data(){
  echo "    Loading $3 into $1"
  $curl_cmd -X POST -H "Content-Type: application/rdf+xml" -T "$dir/$3" "http://${STARDOG_HOST}:${STARDOG_PORT}/$1/data?graph=$2" > /dev/null
  if [[ $? -ne 0 ]]; then
      echo "Error occurred when loading data into $1. Response:$_"
      exit 1
  fi
}

load_data(){
  if [[ $db_type = "stardog" ]]; then
    load_terminology_data_in_transaction CTRP http://NCI_T_weekly ThesaurusInferred_+1weekly.owl
    load_terminology_data_in_transaction CTRP http://NCI_T_monthly ThesaurusInferred_monthly.owl
    load_terminology_data_in_transaction NCIT2 http://NCI_T_monthly ThesaurusInferred_monthly.owl
    load_terminology_data_in_transaction NCIT2 http://GO_monthly GO/go.2022-07-01.owl
    load_terminology_data_in_transaction NCIT2 http://HGNC_monthly HGNC/HGNC_202209.owl
    load_terminology_data_in_transaction NCIT2 http://ChEBI_monthly ChEBI/chebi_213.owl
    load_terminology_data_in_transaction NCIT2 http://UmlsSemNet UmlsSemNet/umlssemnet.owl
    load_terminology_data_in_transaction NCIT2 http://MEDRT MED-RT/medrt.owl
    load_terminology_data_in_transaction NCIT2 http://Canmed CanMed/canmed.owl
    load_terminology_data_in_transaction NCIT2 http://CTCAE CTCAE/ctcae5.owl
    load_terminology_data_in_transaction NCIT2 http://DUO_monthly DUO/duo_Feb21.owl
    load_terminology_data_in_transaction NCIT2 http://DUO_monthly DUO/iao_Dec20.owl
    load_terminology_data_in_transaction NCIT2 http://OBI_monthly OBI/obi_2022_07.owl
    load_terminology_data_in_transaction NCIT2 http://OBIB OBIB/obib_2021-11.owl
    load_terminology_data_in_transaction NCIT2 http://NDFRT2 NDFRT/NDFRT_Public_2018.02.05_Inferred.owl
    load_terminology_data_in_transaction NCIT2 http://MGED MGED/MGEDOntology.owl
    load_terminology_data_in_transaction NCIT2 http://NPO NPO/npo-2011-12-08_inferred.owl
    load_terminology_data_in_transaction NCIT2 http://MA Mouse_Anatomy/ma_07_27_2016.owl
    load_terminology_data_in_transaction NCIT2 http://Zebrafish Zebrafish/zfa_2019_08_02.owl
  elif [[ $db_type = "jena" ]]; then
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
  fi
}


reindex(){
# Reindex stardog terminologies
echo "  Reindex stardog terminologies"
# After this point, the log is stored in the tmp folder unless an error is hit
src/main/bin/reindex.sh --noconfig --history "$historyFile" > /tmp/x.$$.txt 2>&1
if [[ $? -ne 0 ]]; then
    cat /tmp/x.$$.txt | sed 's/^/    /'
    echo "ERROR: problem running reindex.sh script"
    exit 1
fi
}

# Clean and load stardog
echo "  Remove stardog databases and load monthly/weekly"
drop_databases
create_databases
remove_elasticsearch_indexes
reindex_ncim
load_data
reindex

# Cleanup
/bin/rm -f /tmp/x.$$.txt $dir/x.{sh,txt}

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"