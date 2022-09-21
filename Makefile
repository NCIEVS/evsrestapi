# global service name
SERVICE                 := evsrestapi

#######################################################################
#                 OVERRIDE THIS TO MATCH YOUR PROJECT                 #
#######################################################################
APP_VERSION             := $(shell echo `grep "version =" build.gradle | sed 's/version = //'`)

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
build:
	./gradlew clean build -x test

version:
	@echo $(APP_VERSION)
