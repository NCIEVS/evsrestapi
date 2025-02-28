# EVSRESTAPI

Information on the build and deployment process for the EVSRESTAPI project

## Prerequisites

* Install Docker and ensure it is configured to allow (Docker -> Settings -> Resources)
  * **NOTE**: For Macs, you may want to adjust the CPU & memory settings.
* Before cloning the repo, make sure that the command `git config core.autocrlf` returns `false`. Change it to `false` using `git config --global core.autocrlf false` if necessary
* Clone the project - [https://github.com/NCIEVS/evsrestapi](https://github.com/NCIEVS/evsrestapi)
* Create a local data directory and set a `$dir` variable in your terminal. This `$dir` variable will be referenced multiple times in upcoming steps.
  * `export set dir=C:/Users/carlsenbr/eclipse-workspace/data`
* Execute `mkdir -p $dir/opensearch/data`
* * Set a new variable `$ES_DIR` to the new directory just created. This `$ES_DIR` will be referenced in upcoming steps.
* `export set ES_DIR=$dir/opensearch/data`
* Download the "Unit Test Data" folder from <https://drive.google.com/drive/u/1/folders/11RcXLTsbOZ34_7ofKdVxLKHp_8aJGgTI>.  Unpack it to your `$dir` folder (so that `$dir/UnitTestData` exists)
  * run `prep.sh`

## Steps for Loading NCI Thesaurus Data and Indexes Locally

* Launch Graph DB and load NCI Thesaurus data - (see [Jena/Fuseki Resources](JENA.md))
* Launch Opensearch docker container - (see [Opensearch Resources](OPENSEARCH.md))

* Make sure to set at least the following environment variables
  * ES_SCHEME=http
  * ES_HOST=localhost
  * ES_PORT=9201
  * GRAPH_DB_HOST=localhost
  * GRAPH_DB_PORT=5820
  * GRAPH_DB=NCIT2
  * GRAPH_DB_USERNAME=admin
  * GRAPH_DB_PASSWORD=admin
  * CONFIG_BASE_URI=https://raw.githubusercontent.com/NCIEVS/evsrestapi-operations/develop/config/metadata
  * MAIL_USERNAME=<YOUR_WORK_EMAIL>
  * MAIL_PASSWORD=<YOUR_GMAIL_APP_PASSWORD>
  * MAIL_AUTH=true
  * MAIL_TLS=true
  * RECAPTCHA_KEY=<SITE_KEY_FROM_RECAPTCHA>
  * {RECAPTCHA_SECRET=<SECRET_KEY_FROM_RECAPTCHA>

* Load the UnitTestData set by running `prep.sh`
  * Make sure that you can run all programs in `bash`, especially if on Mac which defaults to `zsh`

      ```
      cd evsrestapi
      make devreset
      tail -f log
    ```

## Steps for Building and Running EVSRESTAPI locally

* Launch Jena/fuseki and Opensearch (as described above)
  * If loaded properly, the loaded artifacts should be persistent and you can take down and restart the docker processes and the data will still be there.
  * NOTE: both services must be loaded and running for the application tests to run properly
* Configure application
  * see `src/main/resources/application-local.yml` file for local setup (these settings should be suitable for local deployment)
* Build the application (MUST DO BEFORE RUNNING if using "external tools configuration")
  * make clean build
  * Executable war file present in build/libs

### Run application in Eclipse
* Click "Run" -> "Run Configurations"
* Create a new "Java Application" configuration and name it "evsrestapi - local"
* Set the "Project" to the `evsrestapi` project
* Set the "Main Class" to `gov.nih.nci.evs.api.Application`
* In the "Arguments" tab, add to "VM Arguments" the value `-Dspring.profiles.active=local`
* In the Environment variables add the email credentials and settings for testing:
  * e.g. `AUTH=true;MAIL_USER=<testUserEmail@domain.com;TLS=true;MAIL_PASSWORD=#########;RECAPTCHA_KEY:########;RECAPTCHA_SECRET=#######`
* Test that it's up by looking for swagger docs: [http://localhost:8082/swagger-ui.html#/](http://localhost:8082/swagger-ui.html#/)

### Run application in IntelliJ
* Click "Run" -> "Edit Configurations"
* Create a new "Spring Boot" configuration and name it "evsrestapi - local"
* Set the "Project" to the `evsrestapi-main` project
* Set the "Main Class" to `gov.nih.nci.evs.api.Application`
* Click on "Modify options" and select "Add VM options" & "Environment Variables"
* In the "VM options" text box, add the value `-Dspring.profiles.active=local`
* In the Environment variables add the email credentials and settings for testing:
  * e.g. `AUTH=true;MAIL_USER=<testUserEmail@domain.com>;TLS=true;MAIL_PASSWORD=<#########>;RECAPTCHA_KEY:########;RECAPTCHA_SECRET=########`
* Test that it's up by looking for swagger docs: [http://localhost:8082/swagger-ui.html#/](http://localhost:8082/swagger-ui.html#/)

### Run application from command line
* Run with `java -Xmx4096 -Dspring.profiles.active=local -jar build/libs/evsrestapi*jar`
