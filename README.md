# EVSRESTAPI

Information on the build and deployment process for the EVSRESTAPI project

### Prerequisites

* Install Docker and ensure it is configured to allow (Docker -> Settings -> Resources)
    * Memory = 7G
    * Swap = 1G
* Clone the project - [https://github.com/NCIEVS/evsrestapi](https://github.com/NCIEVS/evsrestapi)
    * Before cloning the repo, make sure that the command `git config core.autocrlf` returns `false`. Change it to `false` using `git config --global core.autocrlf false` if necessary

* Choose a local directory $dir (e.g. c:/evsrestapi)
* mkdir -p $dir/elasticsearch/data
* Download latest ThesaurusInf_*OWL.zip, unpack to $dir/ThesaurusInferred.owl (see [https://evs.nci.nih.gov/ftp1/NCI_Thesaurus/](https://evs.nci.nih.gov/ftp1/NCI_Thesaurus/))

### Steps for Loading Data and Indexes Locally

* Launch Stardog and load NCI Thesaurus data - (see [Stardog Resources](STARDOG.md))
* Launch Elasticsearch docker container 
In a terminal/Cygwin window, run the following to have an elasticsearch instance running. Keep this window open to keep the server running.

      docker pull docker.elastic.co/elasticsearch/elasticsearch:6.7.0
      # Choose a directory for your elasticsearch data to live
      dir=c:/evsrestapi/elasticsearch/data
      docker run -d --name=es_evs --rm -p 9200:9200 -v "$dir":/usr/share/elasticsearch/data  -e "discovery.type=single-node" -e ES_JAVA_OPTS="-Xms1g -Xmx3g"  docker.elastic.co/elasticsearch/elasticsearch:6.7.0


* Load/Compute Indexes - Run from the "elasticsearch/scripts" folder of the cloned https://github.com/NCIEVS/evsrestapi repo.

      # Check properties in application-local.yml (local profile) or application.yml (otherwise)
      # and make sure the following properties are properly configured 
      ? nci.evs.stardog.host (the stardog host; default is localhost) 
      ? nci.evs.elasticsearch.server.host (the elasticsearch host; default is localhost)
      ? nci.evs.bulkload.conceptsDir (the directory used to store downloaded concept files; default is /tmp/; ignored for real-time index load)
      ? nci.evs.bulkload.downloadBatchSize (the batch size for download from stardog; default is 1000)
      ? nci.evs.bulkload.indexBatchSize (the batch size for upload to Elasticsearch; default is 1000)

      # Build the project - From the root of cloned https://github.com/NCIEVS/evsrestapi
      gradlew clean build -x test
      
      ** Usage
      
        usage: java -jar $DIR/evsrestapi-*.jar
        -d,--downloadOnly        Download concepts and skip elasticsearch load.
        -f,--forceDeleteIndex    Force delete index if index already exists.
        -h,--help                Show this help information and exit.
        -l,--location <arg>      The folder location (ex: /tmp/) to use for
                                  download. Overrides the configuration in
                                  application.yml file. Will be used only if
                                  download is required.
        -r,--realTime            Load elasticsearch in real-time by fetching
                                  concepts from stardog. Skips downloading to
                                  folder. Ignores --location (-l), --downloadOnly
                                  (-d), --skipDownload (-s) options.
        -s,--skipDownload        Load elasticsearch from folder without download.
        -t,--terminology <arg>   The terminology (ex: ncit_20.02d) to load.

        # To print help information
        java -jar <path/to/spring-boot-fat-jar> --help 
        
        example: java -jar build/libs/evsrestapi-*.jar --help
      
      *** To load indexes by downloading concepts to local disk:
      
        # In this mode, index loading happens internally in two steps
        # step 1: download concepts to a local folder in batches of size 1000. The default folder is /tmp/.
        # step 2: upload concepts from the local folder to Elasticsearch in batches of size 1000
        # Single command will trigger both the steps internally, in a sequential manner
        
        # To initiate index load process
        java -jar <path/to/spring-boot-fat-jar> --terminology <terminology_version> --forceDeleteIndex
        
        example: java -jar build/libs/evsrestapi-*.jar --terminology ncit_20.02d --forceDeleteIndex
      
      *** To skip download step and load indexes to Elasticsearch from Stardog directly: 
      
        # In this mode, index loading happens in real-time (meaning, in a single step)
        java -jar <path/to/spring-boot-fat-jar> --terminology <terminology_version> --realTime --forceDeleteIndex
        
        example: java -jar build/libs/evsrestapi-*.jar --terminology ncit_20.04d --realTime --forceDeleteIndex

      *** To run and build indexes against a docker stardog/elasticsearch:

        version=ncit_20.08e
        export NCI_EVS_BULK_LOAD_DOWNLOAD_BATCH_SIZE=500
        export NCI_EVS_BULK_LOAD_INDEX_BATCH_SIZE=100
        java -Dspring.profiles.active=local -jar build/libs/evsrestapi-*.jar --terminology $version --realTime --forceDeleteIndex


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

