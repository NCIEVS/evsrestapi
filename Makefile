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
DOCKER_SECRETS_DIR      ?= $(CURDIR)/.docker-secrets
DOCKER_IMAGE_STAMP      := build/.docker-image-$(subst :,_,$(subst /,_,$(DOCKER_IMAGE)))
DOCKER_BUILD_INPUTS     := Makefile Dockerfile .dockerignore build.gradle gradle.properties $(shell git ls-files --cached --others --exclude-standard src/main)

GRADLEW                 ?= ./gradlew

ifeq ($(OS),Windows_NT)
DOCKER                  ?= docker.exe
DOCKER_HOST_GATEWAY_ARG :=
else
DOCKER                  ?= docker
DOCKER_HOST_GATEWAY_ARG := --add-host host.docker.internal:host-gateway
endif

.PHONY: build docker dockerpush scandocker rundocker check-docker-secrets scan

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
docker: $(DOCKER_IMAGE_STAMP)
	@$(DOCKER) image inspect "$(DOCKER_IMAGE)" > /dev/null 2>&1 || { rm -f "$(DOCKER_IMAGE_STAMP)"; $(MAKE) --no-print-directory "$(DOCKER_IMAGE_STAMP)"; }
	@echo "Docker image $(DOCKER_IMAGE) is up to date."

$(DOCKER_IMAGE_STAMP): $(DOCKER_BUILD_INPUTS)
	$(DOCKER) build --platform linux/amd64 --tag "$(DOCKER_IMAGE)" .
	@mkdir -p "$(dir $@)"
	@touch "$@"

# Build and push a Linux/AMD64 image. Override DOCKER_IMAGE with a registry-qualified image name.
dockerpush:
	$(DOCKER) buildx build --platform linux/amd64 --tag "$(DOCKER_IMAGE)" --push .

# Report all HIGH and CRITICAL image vulnerabilities with their installed and fixed versions.
# The complete HTML report is written to report-docker.html.
scandocker: docker
	trivy image "$(DOCKER_IMAGE)" --scanners vuln --severity HIGH,CRITICAL --format table
	trivy image "$(DOCKER_IMAGE)" --scanners vuln --format template -o report-docker.html --template "@config/trivy/html.tpl"



# Require a local, ignored directory of Spring Boot config-tree secret files.
check-docker-secrets:
	@test -d "$(DOCKER_SECRETS_DIR)" || (echo "ERROR: Create $(DOCKER_SECRETS_DIR) and add secret files before running rundocker." && exit 1)

# Run against Jena/Fuseki and OpenSearch services exposed on the Docker host.
# Secrets are mounted read-only and imported from /run/secrets rather than passed as environment variables.
# Override DOCKER_ES_HOST, DOCKER_GRAPH_DB_HOST, ports, or the secrets directory as needed.
rundocker: docker check-docker-secrets
	$(DOCKER) run --rm --name "$(SERVICE)" -p "$(DOCKER_PORT):8082" $(DOCKER_HOST_GATEWAY_ARG) \
		--mount type=bind,src="$(DOCKER_SECRETS_DIR)",dst=/run/secrets,readonly \
		-e SPRING_CONFIG_IMPORT=optional:configtree:/run/secrets/ \
		-e SPRING_PROFILES_ACTIVE=local \
		-e EVS_SERVER_PORT=8082 \
		-e ES_HOST="$(DOCKER_ES_HOST)" \
		-e ES_PORT="$(ES_PORT)" \
		-e ES_SCHEME="$(ES_SCHEME)" \
		-e GRAPH_DB_HOST="$(DOCKER_GRAPH_DB_HOST)" \
		-e GRAPH_DB_PORT="$(GRAPH_DB_PORT)" \
		-e GRAPH_DB="$(GRAPH_DB)" \
		-e CONFIG_BASE_URI \
		-e MAIL_HOST \
		-e MAIL_PORT \
		-e MAIL_AUTH \
		-e MAIL_TLS \
		-e MAIL_RECIPIENT \
		-e RECAPTCHA_KEY \
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
