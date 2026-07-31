# global service name
SERVICE                 := evsrestapi

#######################################################################
#                 OVERRIDE THIS TO MATCH YOUR PROJECT                 #
#######################################################################
APP_VERSION             := $(shell echo `grep "^version =" build.gradle | sed 's/version = //'`)
VERSION                 := $(shell echo `grep "^version =" build.gradle | sed 's/version = //; s/.RELEASE//'`)

# Builds should be repeatable, therefore we need a method to reference the git
# sha where a version came from.
GIT_VERSION          	?= $(shell echo `git describe --match=NeVeRmAtCh --always --dirty`)
GIT_COMMIT          	?= $(shell echo `git log | grep -m1 -oE '[^ ]+$'`)
GIT_COMMITTED_AT        ?= $(shell echo `git log -1 --format=%ct`)
GIT_BRANCH				?=
FULL_VERSION            := v$(APP_VERSION)-g$(GIT_VERSION)
DOCKER_TAG              := $(shell grep "^version =" build.gradle | sed 's/version = //; s/"//g; s/.RELEASE//')
DOCKER_IMAGE            ?= $(SERVICE):$(DOCKER_TAG)
ES_PORT                 ?= 9201
ES_SCHEME               ?= http
GRAPH_DB_PORT           ?= 3030
GRAPH_DB                ?= NCIT2
DOCKER_PORT             ?= 8082
DOCKER_ES_HOST          ?= host.docker.internal
DOCKER_GRAPH_DB_HOST    ?= host.docker.internal

GRADLEW                 ?= ./gradlew

ifeq ($(OS),Windows_NT)
DOCKER                  ?= docker.exe
else
DOCKER                  ?= docker
endif

.PHONY: build docker scandocker rundocker

# consider also "docker save..." and "docker load..." to avoid registry.
clean:
	$(GRADLEW) clean

# Build the library without tests
# On Windows use: git config core.eol lf
build:
	$(GRADLEW) clean spotlessApply build spotbugsMain spotbugsTest -x test -x zipFile

run: build
	java -Dspring.profiles.active=local -jar build/libs/evsrestapi*.war

# Build the application image from the executable Spring Boot JAR.
docker: build
	$(DOCKER) build --tag "$(DOCKER_IMAGE)" .

# Report all HIGH and CRITICAL image vulnerabilities with their installed and fixed versions.
# The complete HTML report is written to report-docker.html.
scandocker: docker
	trivy image "$(DOCKER_IMAGE)" --scanners vuln --severity HIGH,CRITICAL --format table
	trivy image "$(DOCKER_IMAGE)" --scanners vuln --format template -o report-docker.html --template "@config/trivy/html.tpl"

# Run against Jena/Fuseki and OpenSearch services exposed on the Docker host.
# Override DOCKER_ES_HOST, DOCKER_GRAPH_DB_HOST, ports, or any forwarded setting as needed.
rundocker: docker
	$(DOCKER) run --rm --name "$(SERVICE)" -p "$(DOCKER_PORT):8082" \
		-e SPRING_PROFILES_ACTIVE=local \
		-e EVS_SERVER_PORT=8082 \
		-e ES_HOST="$(DOCKER_ES_HOST)" \
		-e ES_PORT="$(ES_PORT)" \
		-e ES_SCHEME="$(ES_SCHEME)" \
		-e GRAPH_DB_HOST="$(DOCKER_GRAPH_DB_HOST)" \
		-e GRAPH_DB_PORT="$(GRAPH_DB_PORT)" \
		-e GRAPH_DB="$(GRAPH_DB)" \
		-e NCI_EVS_ADMIN_KEY \
		-e CONFIG_BASE_URI \
		-e MAIL_HOST \
		-e MAIL_PORT \
		-e MAIL_USER \
		-e MAIL_PASSWORD \
		-e MAIL_AUTH \
		-e MAIL_TLS \
		-e MAIL_RECIPIENT \
		-e RECAPTCHA_KEY \
		-e RECAPTCHA_SECRET \
		"$(DOCKER_IMAGE)"

test:
	$(GRADLEW) spotlessCheck -x test

releasetag:
	git tag -a "${VERSION}-RC-`/bin/date +%Y-%m-%d`" -m "Release ${VERSION}-RC-`/bin/date +%Y-%m-%d`"
	git push origin "${VERSION}-RC-`/bin/date +%Y-%m-%d`"

rmreleasetag:
	git tag -d "${VERSION}-RC-`/bin/date +%Y-%m-%d`"
	git push origin --delete "${VERSION}-RC-`/bin/date +%Y-%m-%d`"

tag:
	git tag -a "v`/bin/date +%Y-%m-%d`-${APP_VERSION}" -m "Release `/bin/date +%Y-%m-%d`"
	git push origin "v`/bin/date +%Y-%m-%d`-${APP_VERSION}"

rmtag:
	git tag -d "v`/bin/date +%Y-%m-%d`-${APP_VERSION}"
	git push origin --delete "v`/bin/date +%Y-%m-%d`-${APP_VERSION}"

version:
	@echo $(APP_VERSION)

devreset: build
	./src/main/bin/devreset.sh ../data/UnitTestData > log 2>&1 &

# Report all HIGH and CRITICAL dependency vulnerabilities with installed and fixed versions.
# The complete HTML report is written to report.html.
scan:
	$(GRADLEW) dependencies --write-locks
	trivy fs gradle.lockfile --scanners vuln --severity HIGH,CRITICAL --format table
	trivy fs gradle.lockfile --scanners vuln --format template -o report.html --template "@config/trivy/html.tpl"
	/bin/rm -rf gradle/dependency-locks
	/bin/rm gradle.lockfile
