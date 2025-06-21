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

.PHONY: build

# consider also "docker save..." and "docker load..." to avoid registry.
clean:
	./gradlew clean

# Build the library without tests
# On Windows use: git config core.eol lf
build:
	./gradlew clean spotlessApply build -x test -x zipFile

test:
	./gradlew spotlessCheck -x test 

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

scan:
			./gradlew dependencies --write-locks
			trivy fs gradle.lockfile --format template -o report.html --template "@config/trivy/html.tpl"
			grep CRITICAL report.html
			/bin/rm -rf gradle/dependency-locks
			/bin/rm gradle.lockfile
