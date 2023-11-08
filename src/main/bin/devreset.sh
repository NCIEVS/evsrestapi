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

if [ $help == 1 ] || [ ${#arr[@]} -ne 1 ]; then
  echo "Usage: src/main/bin/devreset.sh \"c:/data/UnitTestData\""
  echo "  e.g. src/main/bin/devreset.sh ../data/UnitTestData"
  exit 1
fi
dir=${arr[0]}

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

# Verify docker stardog is running
echo "    verify docker stardog is running"
ct=`docker ps | grep 'stardog/stardog' | wc -l`
if [[ $ct -ne 1 ]]; then
    echo "    ERROR: stardog docker is not running"
    exit 1
fi

# Verify docker stardog has a volume mounted that contains UnitTestData
echo "    verify docker stardog has /data/UnitTestData mounted"
pid=`docker ps | grep stardog/stardog | cut -f 1 -d\  `
datadir=`docker inspect -f '{{ .Mounts }}' $pid | perl -ne '/.*bind\s+([^\s]+)\s+\/data\s+.*/; print $1' | perl -pe 's/.*\/(host_mnt|mnt\/host)\/([cde])/$2:\//'`
if [[ -z "$datadir" ]]; then
    echo "ERROR: unable to determine volume mounted to /data in docker $pid"
    exit 1
fi
if [[ ! -e "$datadir/UnitTestData" ]]; then
    echo "ERROR: directory mounted as /data does not have a UnitTestData subdirectory = $datadir"
    exit 1
fi

# Verify docker elasticsearch is running
echo "    verify docker elasticsearch is running"
ct=`docker ps | grep 'elasticsearch/elasticsearch' | wc -l`
if [[ $ct -lt 1 ]]; then
    echo "    ERROR: elasticsearch docker is not running"
    exit 1
fi

# Verify docker elasticsearch can be reached
echo "    verify docker elasticsearch can be reached"
curl -s "$ES_SCHEME://$ES_HOST:$ES_PORT/_cat/indices" >> /dev/null
if [[ $? -ne 0 ]]; then
    echo "ERROR: problem connecting to docker elasticsearch"
    exit 1
fi

# Verfiy stardog container can run a script
echo "    verify docker stardog can run a script"
/bin/rm -f $dir/x.txt
cat > $dir/x.sh << EOF
#!/bin/bash
ls /data/UnitTestData > //data/UnitTestData/x.txt
EOF
chmod 755 $dir/x.sh
chmod ag+rwx $dir
pid=`docker ps | grep stardog/stardog | cut -f 1 -d\  `
# note: //data is required for gitbash
docker exec $pid //data/UnitTestData/x.sh
if [[ $? -ne 0 ]]; then
    echo "ERROR: problem connecting to docker elasticsearch"
    exit 1
fi
ct=`grep -c owl $dir/x.txt`
if [[ $ct -eq 0 ]]; then
    echo "ERROR: expecting owl files referenced in x.txt"
    exit 1
fi
/bin/rm -f $dir/x.txt



# Remove elasticsearch indexes
echo "  Remove elasticsearch indexes"
curl -s "$ES_SCHEME://$ES_HOST:$ES_PORT/_cat/indices" | cut -d\  -f 3 | egrep "metrics|concept|evs" | cat > /tmp/x.$$.txt
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

# Reindex ncim
echo "  Reindex ncim"
src/main/bin/ncim-part.sh --noconfig $dir/NCIM | sed 's/^/    /'
if [[ $? -ne 0 ]]; then
    echo "ERROR: problem running ncim-part.sh"
    exit 1
fi

# Reindex ncim - individual terminologies
for t in MDR ICD10CM ICD9CM LNC SNOMEDCT_US RADLEX; do

    # Keep the NCIM folder around while we run
    echo "Load $t (from downloaded data)"
    src/main/bin/ncim-part.sh --noconfig $dir/NCIM --keep --terminology $t | sed 's/^/    /'
    if [[ $? -ne 0 ]]; then
        echo "ERROR: loading $t"
        exit 1
    fi
done

# Clean and load stardog
echo "  Remove stardog databases and load monthly/weekly"
# TODO: if the following fails, there's nothing to catch it
# have the script check return values and write to /data/UnitTestData/x.txt if there is an error, or "success" if all is good
# and check that on the outside.
cat > $dir/x.sh << EOF
#!/bin/bash
echo "    drop databases"
/opt/stardog/bin/stardog-admin db drop CTRP | sed 's/^/      /'
/opt/stardog/bin/stardog-admin db drop NCIT2 | sed 's/^/      /'
echo "    create databases"
/opt/stardog/bin/stardog-admin db create -n CTRP | sed 's/^/      /'
/opt/stardog/bin/stardog-admin db create -n NCIT2 | sed 's/^/      /'
echo "    load data"
/opt/stardog/bin/stardog data add --named-graph http://NCI_T_weekly CTRP /data/UnitTestData/ThesaurusInferred_+1weekly.owl | sed 's/^/      /'
/opt/stardog/bin/stardog data add --named-graph http://NCI_T_monthly CTRP /data/UnitTestData/ThesaurusInferred_monthly.owl | sed 's/^/      /'
/opt/stardog/bin/stardog data add --named-graph http://NCI_T_monthly NCIT2 /data/UnitTestData/ThesaurusInferred_monthly.owl | sed 's/^/      /'
/opt/stardog/bin/stardog data add --named-graph http://GO_monthly NCIT2 /data/UnitTestData/GO/go.2022-07-01.owl | sed 's/^/      /'
/opt/stardog/bin/stardog data add --named-graph http://HGNC_monthly NCIT2 /data/UnitTestData/HGNC/HGNC_202209.owl | sed 's/^/      /'
/opt/stardog/bin/stardog data add --named-graph http://ChEBI_monthly NCIT2 /data/UnitTestData/ChEBI/chebi_213.owl | sed 's/^/      /'
/opt/stardog/bin/stardog data add --named-graph http://UmlsSemNet NCIT2 /data/UnitTestData/UmlsSemNet/umlssemnet.owl | sed 's/^/      /'
/opt/stardog/bin/stardog data add --named-graph http://MEDRT NCIT2 /data/UnitTestData/MED-RT/medrt.owl | sed 's/^/      /'
/opt/stardog/bin/stardog data add --named-graph http://Canmed NCIT2 /data/UnitTestData/Canmed/canmed.owl | sed 's/^/      /'
/opt/stardog/bin/stardog data add --named-graph http://Ctcae5 NCIT2 /data/UnitTestData/Ctcae5/ctcae5.owl | sed 's/^/      /'
echo "    optimize databases"
/opt/stardog/bin/stardog-admin db optimize -n CTRP | sed 's/^/      /'
/opt/stardog/bin/stardog-admin db optimize -n NCIT2 | sed 's/^/      /'
EOF
chmod 755 $dir/x.sh
chmod ag+rwx $dir
pid=`docker ps | grep stardog/stardog | cut -f 1 -d\  `
# note: //data is required for gitbash
docker exec $pid //data/UnitTestData/x.sh
if [[ $? -ne 0 ]]; then
    echo "ERROR: problem loading stardog"
    exit 1
fi
/bin/rm -f $dir/x.sh

# Hardcode the history file
historyFile=$dir/cumulative_history_21.06e.txt

# Reindex stardog terminologies
echo "  Reindex stardog terminologies"
src/main/bin/reindex.sh --noconfig --history $historyFile | sed 's/^/    /'
if [[ $? -ne 0 ]]; then
    echo "ERROR: problem running reindex.sh script"
    exit 1
fi

# Cleanup
/bin/rm -f /tmp/x.$$.txt $dir/x.{sh,txt}

echo ""
echo "--------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "--------------------------------------------------"
