# EVSRESTAPI

Information on the build and deployment process for the EVSRESTAPI project

### Prerequisites

* Install Docker and ensure it is configured to allow (Docker -> Settings -> Resources)
* Clone the project - [https://github.com/NCIEVS/evsrestapi](https://github.com/NCIEVS/evsrestapi)
    * Before cloning the repo, make sure that the command `git config core.autocrlf` returns `false`. Change it to `false` using `git config --global core.autocrlf false` if necessary

* Choose a local directory $dir (e.g. c:/evsrestapi)
* mkdir -p $dir/elasticsearch/data
* Download latest ThesaurusInf_*OWL.zip, unpack to $dir/ThesaurusInferred.owl (see [https://evs.nci.nih.gov/ftp1/NCI_Thesaurus/](https://evs.nci.nih.gov/ftp1/NCI_Thesaurus/))

### Steps for Loading NCI Thesaurus Data and Indexes Locally

* Launch Stardog and load NCI Thesaurus data - (see [Stardog Resources](STARDOG.md))
* Launch Elasticsearch docker container - (see [Elasticsearch Resources](ELASTICSEARCH.md))

* Load/Compute Indexes (for NCI Thesaurus)

    gradlew clean build -x test

    version=ncit_21.08d
    export EVS_SERVER_PORT=8083
    export NCI_EVS_BULK_LOAD_DOWNLOAD_BATCH_SIZE=500
    export NCI_EVS_BULK_LOAD_INDEX_BATCH_SIZE=100
    java -Dspring.profiles.active=local -jar build/libs/evsrestapi-*.jar --terminology $version --forceDeleteIndex > log 2>&1 &
    tail -f log


### Steps for Building and Running EVSRESTAPI locally

* Launch Stardog and Elasticsearch (as described above)
    * If loaded properly, the loaded artifacts should be persistent and you can take down and restart the docker processes and the data will still be there.
    * NOTE: both services must be loaded and running for the application tests to run properly
* Configure application
    * see `src/main/resources/application-local.yml` file for local setup (these settings should be suitable for local deployment)
* Build the application (MUST DO BEFORE RUNNING if using "external tools configuration")
    * ./gradlew clean build -x test
    * Executable war file present in build/libs

* Run application in Eclipse (SpringBoot)
    * Click "Run" -> "External Tools" -> "External Tools Configurations"
    * Create a new entry under "Program" and configure it as follows:
        * location = <path to java executable, e.g. 'C:/Program Files/Java/jdk1.8.0_191/bin/java.exe'>
        * working dir = <path to project, e.g. 'C:/Users/bcarl/Desktop/workspace/evsrestapi'>
        * Arguments = command line args
            * '-Xmx4096M' - ensure enough memory usage
            * '-Dspring.profiles.active=local' - make sure to use application-local.yml
            * '-jar *.war' - point to the war file

    * Test that it's up by looking for swagger docs: [http://localhost:8080/swagger-ui.html#/](http://localhost:8080/swagger-ui.html#/)

### Steps for Loading NCI Metathesaurus Indexes Locally

* Download the NCI Metathesaurus to a local directory
* Properly configure the environment variables needed by ncim.sh
* Run `ncim-part.sh`

        dir=c:/evsrestapi/NCIM_202202/META
        export NCI_EVS_BULK_LOAD_DOWNLOAD_BATCH_SIZE=1000
        export NCI_EVS_BULK_LOAD_INDEX_BATCH_SIZE=100
        src/main/bin/ncim-part.sh --noconfig $dir

### Steps for Loading MDR from NCI Metathesaurus Indexes Locally

* Download the NCI Metathesaurus to a local directory
* Properly configure the environment variables needed by ncim.sh
* Run `ncim-part.sh`

        dir=c:/evsrestapi/NCIM_202202/META
        export NCI_EVS_BULK_LOAD_DOWNLOAD_BATCH_SIZE=1000
        export NCI_EVS_BULK_LOAD_INDEX_BATCH_SIZE=100
        src/main/bin/ncim-part.sh --noconfig $dir --terminology MDR
