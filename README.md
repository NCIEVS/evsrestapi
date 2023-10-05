# EVSRESTAPI

Information on the build and deployment process for the EVSRESTAPI project

### Prerequisites

* Install Docker and ensure it is configured to allow (Docker -> Settings -> Resources)
* In your termianl, make sure that the command `git config core.autocrlf` returns `false`. Change it to `false` using `git config --global core.autocrlf false` if necessary
* Clone the project - [https://github.com/NCIEVS/evsrestapi](https://github.com/NCIEVS/evsrestapi)
* Choose a local data directory (e.g. c:/evsrestapi/dir). This will be where you save your projects and store their related data and act as a BASE_DIR.
  * `export set BASE_DIR=C:/Users/[usrName]/[your/local/path]`
* `mkdir -p $BASE_DIR/elasticsearch/data`
* Set a new variable $dir to the new directory we created. This will be referenced in upcoming steps. 
  * `export set dir=$BASE_DIR/data/`
* Download the "Unit Test Data" folder from <https://drive.google.com/drive/u/1/folders/11RcXLTsbOZ34_7ofKdVxLKHp_8aJGgTI>.  Unpack it to your $dir folder (so that $dir/UnitTestData exists)
  * cd into the UnitTestData and run `prep.sh`

### Steps for Loading NCI Thesaurus Data and Indexes Locally

* Launch Stardog and load NCI Thesaurus data - (see [Stardog Resources](STARDOG.md))
* Launch Elasticsearch docker container - (see [Elasticsearch Resources](ELASTICSEARCH.md))

* Make sure to set at least the following environment variables
    * export set ES_SCHEME=http
    * export set ES_HOST=localhost
    * export set ES_PORT=9301
    * export set STARDOG_HOST=localhost
    * export set STARDOG_PORT=5820
    * export set STARDOG_DB=NCIT2
    * export set STARDOG_USERNAME=admin
    * export set STARDOG_PASSWORD=admin

* Load the UnitTestData set

    cd evsrestapi
    make devreset
    tail -f log

### Steps for Building and Running EVSRESTAPI locally

* Launch Stardog and Elasticsearch (as described above)
  * If loaded properly, the loaded artifacts should be persistent and you can take down and restart the docker processes and the data will still be there.
  * NOTE: both services must be loaded and running for the application tests to run properly
* Configure application
  * see `src/main/resources/application-local.yml` file for local setup (these settings should be suitable for local deployment)
* Build the application (MUST DO BEFORE RUNNING if using "external tools configuration")
  * make clean build
  * Executable war file present in build/libs

* Run application in Eclipse
  * Click "Run" -> "Run Configurations"
  * Create a new "Java Application" configuration and name it "evsrestapi - local"
  * Set the "Project" to the `evsrestapi` project
  * Set the "Main Class" to `gov.nih.nci.evs.api.Application`
  * In the "Arguments" tab, add to "VM Arguments" the value `-Dspring.profiles.active=local`
  * Test that it's up by looking for swagger docs: [http://localhost:8082/swagger-ui.html#/](http://localhost:8082/swagger-ui.html#/)

* Run application from command line
  * Run with `java -Xmx4096 -Dspring.profiles.active=local -jar build/libs/evsrestapi*jar`
