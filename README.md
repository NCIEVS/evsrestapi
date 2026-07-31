# EVSRESTAPI

Information on the build and deployment process for the EVSRESTAPI project

## Prerequisites

* Install Docker and ensure it is configured to allow (Docker -> Settings -> Resources)
  * **NOTE**: For Macs, you may want to adjust the CPU & memory settings.
* Before cloning the repo, make sure that the command `git config core.autocrlf` returns `false`. Change it to `false` using `git config --global core.autocrlf false` if necessary
* Clone the project - [https://github.com/NCIEVS/evsrestapi](https://github.com/NCIEVS/evsrestapi)
* Create a local data directory and set a `$dir` variable in your terminal. This `$dir` variable will be referenced multiple times in upcoming steps.
  * Bash (Git Bash / WSL): `export dir="/c/Users/carlsenbr/eclipse-workspace/data"`
  * Windows CMD: `set dir=C:\Users\carlsenbr\eclipse-workspace\data`
* Execute `mkdir -p $dir/opensearch/data`
- Set a new variable `$ES_DIR` to the new directory just created. This `$ES_DIR` will be referenced in upcoming steps.
  * Bash: `export ES_DIR="$dir/opensearch/data"`
  * Windows CMD: `set ES_DIR=%dir%\\opensearch\\data`
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
  * RECAPTCHA_SECRET=<SECRET_KEY_FROM_RECAPTCHA>

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
  * e.g. `AUTH=true;MAIL_USER=<testUserEmail@domain.com>;TLS=true;MAIL_PASSWORD=#########;RECAPTCHA_KEY=########;RECAPTCHA_SECRET=########`
* Test that it's up by looking for swagger docs: [http://localhost:8082/swagger-ui/index.html#/](http://localhost:8082/swagger-ui/index.html#/)

### Run application in IntelliJ
* Click "Run" -> "Edit Configurations"
* Create a new "Spring Boot" configuration and name it "evsrestapi - local"
* Set the "Project" to the `evsrestapi-main` project
* Set the "Main Class" to `gov.nih.nci.evs.api.Application`
* Click on "Modify options" and select "Add VM options" & "Environment Variables"
* In the "VM options" text box, add the value `-Dspring.profiles.active=local`
* In the Environment variables add the email credentials and settings for testing:
  * e.g. `AUTH=true;MAIL_USER=<testUserEmail@domain.com>;TLS=true;MAIL_PASSWORD=<#########>;RECAPTCHA_KEY:########;RECAPTCHA_SECRET=########`
* Test that it's up by looking for swagger docs: [http://localhost:8082/swagger-ui/index.html#/](http://localhost:8082/swagger-ui/index.html#/)

### Run application from command line
* Run with `java -Xmx4096 -Dspring.profiles.active=local -jar build/libs/evsrestapi*.jar`

### Build, scan, and run the application image

* `make docker` builds the application and creates `evsrestapi:<version>`.
* The image starts the executable WAR, which runs the REST API entry point; the executable JAR is reserved for loader and reindex operations.
* `make scandocker` scans that image with Trivy and writes `report-docker.html`.
* `make rundocker` runs the image on port 8082 using the `local` Spring profile. It assumes Jena/Fuseki and OpenSearch are already running on the host, and uses `host.docker.internal` to reach them from the container.
* Before running it, create the ignored `.docker-secrets` directory. Each file is mounted read-only at `/run/secrets` and is imported by Spring Boot using its filename as the property name. Put credentials and secrets in `NCI_EVS_ADMIN_KEY`, `MAIL_USER`, `MAIL_PASSWORD`, and `RECAPTCHA_SECRET`; write each value without a trailing newline. Non-sensitive settings such as `MAIL_HOST`, `MAIL_PORT`, and `RECAPTCHA_KEY` continue to be forwarded from the host environment.
* The secret values are not passed as container environment variables, so they do not appear in `docker inspect`. Docker daemon administrators can still access a running container and must remain trusted.
* Override the service hosts or published port when necessary, for example:

  ```bash
  make rundocker DOCKER_ES_HOST=host.docker.internal DOCKER_GRAPH_DB_HOST=host.docker.internal DOCKER_PORT=8082
  ```

  The existing `ES_PORT`, `ES_SCHEME`, `GRAPH_DB_PORT`, and `GRAPH_DB` settings are passed through to the container. Email, reCAPTCHA, and other applicable local configuration environment variables are also forwarded.
