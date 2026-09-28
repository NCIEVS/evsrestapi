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
DOCKER_IMG              := $(shell docker images | grep $(SERVICE) | grep " $(DOCKER_TAG) " | perl -pe 's/ +/ /g;' | cut -d\  -f 3 )

GRADLEW                 ?= ./gradlew

.PHONY: build docker dockerpush scandocker rundocker scan

# consider also "docker save..." and "docker load..." to avoid registry.
clean:
	$(GRADLEW) clean

# Build the library without tests
# On Windows use: git config core.eol lf
build:
	$(GRADLEW) clean spotlessApply build spotbugsMain spotbugsTest -x test -x zipFile

run: build
	java -Dspring.profiles.active=local -jar build/libs/evsrestapi*.war

# Build the application and image in an isolated Linux/AMD64 Docker build environment.
docker:

# Remove prior docker image if it is built
ifdef DOCKER_IMG
	docker rmi -f $(DOCKER_IMG)
else
	@echo No docker image to remove
endif
 
	@echo x $(DOCKER_IMG)

	docker build --platform linux/amd64 --no-cache-filter=gradle-build --tag "$(DOCKER_IMAGE)" .

# Build and push a Linux/AMD64 image. Override DOCKER_IMAGE with a registry-qualified image name.
dockerpush:
	docker push --platform linux/amd64 "$(DOCKER_IMAGE)"

# Report all HIGH and CRITICAL image vulnerabilities with their installed and fixed versions.
# The complete HTML report is written to report-docker.html.
scandocker:
	docker save -o scan.tar $(DOCKER_IMAGE)
	trivy image --input scan.tar $(DOCKER_IMAGE) --format template -o report.html --template "@config/trivy/html.tpl"
	egrep "CRITICAL|HIGH" report.html
	/bin/rm -f scan.tar

# Run against Jena/Fuseki and OpenSearch services exposed on the Docker host.
# Some env is not represented here which is intended to run locally (e.g. MAIL_*, RECAPTCHA_KEY, CONFIG_BASE_URI
# This runs in the foreground, add -d to run in the background
rundocker:
	docker run --rm --name "$(SERVICE)" -p "8082:8082" \
		-e EVS_SERVER_PORT=8082 \
		-e ES_HOST="host.docker.internal" \
		-e ES_PORT="9201" \
		-e ES_SCHEME="http" \
		-e GRAPH_DB_HOST="host.docker.internal" \
		-e GRAPH_DB_PORT="3030" \
		-e GRAPH_DB="NCIT2" \
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
# The complete HTML report is written to report.html. Generated dependency locks are always removed.
scan:
	@set -e; \
	trap 'rm -rf gradle/dependency-locks gradle.lockfile' EXIT; \
	$(GRADLEW) dependencies --write-locks; \
	trivy fs gradle.lockfile --scanners vuln --severity HIGH,CRITICAL --format table; \
	trivy fs gradle.lockfile --scanners vuln --format template -o report.html --template "@config/trivy/html.tpl"
