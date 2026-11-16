# EVSRESTAPI

> [!WARNING]
> **RELEASE BLOCKER — HAPI FHIR SNAPSHOT: TESTING ONLY. DO NOT MERGE THIS BRANCH OR DEPLOY IT.**
> This branch uses HAPI FHIR `8.13.16-SNAPSHOT`, pinned to build `8.13.16-20261014.162006-5`,
> to test the Spring Boot 4 migration from upstream PR #8189.
> **We MUST upgrade to a full released HAPI FHIR version containing these changes and validate it
> before merging this branch or deploying.** A successful snapshot build/test does not clear this requirement.
> Once that full release is integrated and verified, remove this warning and the temporary snapshot
> repository, dependency-alignment rule, and build warning in `build.gradle`; update the HL7 core dependency
> to match the released HAPI version. Snapshot artifacts may expire from the upstream repository.

Information on the build and deployment process for the EVSRESTAPI project

## Data loading has moved to evsrestapi-operations

Indexing and other data-management scripts now live in
[evsrestapi-operations](https://github.com/NCIEVS/evsrestapi-operations). Its
`docs/switching-from-evsrestapi.md` covers setup and maps each old command to its
replacement. The old `src/main/bin` script paths keep working for one release, and
`make devreset` works as before.

Shared model and data-access code is in the `evsrestapi-core` subproject.
`make library` builds it as a plain JAR in `evsrestapi-core/build/libs`. The operations
loader compiles core from this checkout, so rebuild the loader after changing core.

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
  * `make devreset` builds the loader in the `../evsrestapi-operations` checkout (or `$EVS_OPS_HOME`)
    and runs its `bin/devreset.sh`

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
  * e.g. `MAIL_AUTH=true;MAIL_USER=<testUserEmail@domain.com>;MAIL_TLS=true;MAIL_PASSWORD=#########;RECAPTCHA_KEY=########;RECAPTCHA_SECRET=########`
* Test that it's up by looking for swagger docs: [http://localhost:8082/swagger-ui/index.html#/](http://localhost:8082/swagger-ui/index.html#/)

### Run application in IntelliJ
* Click "Run" -> "Edit Configurations"
* Create a new "Spring Boot" configuration and name it "evsrestapi - local"
* Set the "Project" to the `evsrestapi-main` project
* Set the "Main Class" to `gov.nih.nci.evs.api.Application`
* Click on "Modify options" and select "Add VM options" & "Environment Variables"
* In the "VM options" text box, add the value `-Dspring.profiles.active=local`
* In the Environment variables add the email credentials and settings for testing:
  * e.g. `MAIL_AUTH=true;MAIL_USER=<testUserEmail@domain.com>;MAIL_TLS=true;MAIL_PASSWORD=<#########>;RECAPTCHA_KEY=########;RECAPTCHA_SECRET=########`
* Test that it's up by looking for swagger docs: [http://localhost:8082/swagger-ui/index.html#/](http://localhost:8082/swagger-ui/index.html#/)

### Run application from command line
* Run with `java -Xmx4096m -Dspring.profiles.active=local -jar build/libs/evsrestapi*.war`

### Build, scan, and run the application image

- `make docker` builds the WAR inside a Linux/AMD64 Gradle build stage and creates
  `evsrestapi:<version>` without a local Gradle installation. The image runs the
  executable server WAR as the unprivileged `evsapi` user.
- `make scan` and `make scandocker` print HIGH/CRITICAL findings and write complete
  reports to `report.html` and `report-docker.html`, respectively.
- `make dockerpush DOCKER_IMAGE=registry/image:tag` pushes the already-built image.
- `make rundocker` runs on port 8082 with the `local` Spring profile. It connects to
  OpenSearch on host port 9201 and Fuseki on host port 3030 through
  `host.docker.internal`, with the host-gateway mapping needed on Linux.

For different ports, backend settings, or credentials, use a direct `docker run`
with the equivalent profile and service configuration. Non-sensitive values can
be supplied with `-e`.

#### Container credentials

Create the ignored `.docker-secrets` directory and store each secret in a file
named for its setting, such as `NCI_EVS_ADMIN_KEY`, `MAIL_USER`, `MAIL_PASSWORD`, or
`RECAPTCHA_SECRET`. Write values without a trailing newline and grant read access
to the container's `evsapi` user. Mount the directory read-only and import it as a
[Spring configuration tree](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.files.configtree).
For example, after creating the secret files and building image `evsrestapi:2.6.0`:

```bash
docker run --rm --name evsrestapi -p 8082:8082 \
  --add-host host.docker.internal:host-gateway \
  -e SPRING_PROFILES_ACTIVE=local \
  -e ES_HOST=host.docker.internal -e "ES_PORT=${ES_PORT:-9201}" \
  -e "ES_SCHEME=${ES_SCHEME:-http}" \
  -e GRAPH_DB_HOST=host.docker.internal \
  -e "GRAPH_DB_PORT=${GRAPH_DB_PORT:-3030}" -e "GRAPH_DB=${GRAPH_DB:-NCIT2}" \
  --mount "type=bind,src=$(pwd)/.docker-secrets,dst=/run/secrets,readonly" \
  -e 'SPRING_CONFIG_IMPORT=optional:configtree:/run/secrets/' \
  evsrestapi:2.6.0
```

Pass other applicable settings, such as `MAIL_HOST`, `MAIL_PORT`, and
`RECAPTCHA_KEY`, with additional `-e` options. Secret values supplied through files
are not included in the container environment shown by `docker inspect`;
Docker daemon administrators can still access the running container.
