CURRENT_DIR := $(shell pwd)

#
# Deployment configuration
#
# Default path to the tarball
PACKAGE_PATH := $(CURRENT_DIR)/spark-job-rest/target/spark-job-rest.tar.gz
# Override this to set deploy path
SJR_DEPLOY_PATH ?= $(CURRENT_DIR)/deploy

#
# We strongly suggest not to keep remote deployment configuration variables out of Git control!
#
# Overrides SJR_DEPLOY_PATH in remote deploy mode if not empty
SJR_REMOTE_DEPLOY_PATH ?=
# Set this the [user]@hostname of the machine you want to deploy to
SJR_DEPLOY_HOST ?=
# Optionally set path to SSH key here
SJR_DEPLOY_KEY ?=

#
# Remote deployment parameters
#
REMOTE_PARAMS := SJR_DEPLOY_PATH=$(SJR_DEPLOY_PATH) \
				 SJR_DEPLOY_HOST=$(SJR_DEPLOY_HOST) \
                 SJR_DEPLOY_KEY=$(SJR_DEPLOY_KEY) \
                 SJR_PACKAGE_PATH=$(PACKAGE_PATH) \
                 SJR_IS_REMOTE_DEPLOY="true" \
                 SJR_REMOTE_DEPLOY_PATH=$(SJR_REMOTE_DEPLOY_PATH)

all: build deploy

build:
	@mvn clean install

deploy:
	@SJR_DEPLOY_PATH=$(SJR_DEPLOY_PATH) \
	$(CURRENT_DIR)/spark-job-rest/src/main/scripts/deploy.sh deploy

remote-deploy:
	@$(REMOTE_PARAMS) $(CURRENT_DIR)/spark-job-rest/src/main/scripts/deploy.sh deploy

remote-start:
	@$(REMOTE_PARAMS) $(CURRENT_DIR)/spark-job-rest/src/main/scripts/deploy.sh start

remote-stop:
	@$(REMOTE_PARAMS) $(CURRENT_DIR)/spark-job-rest/src/main/scripts/deploy.sh stop

start:
	@echo "Starting spark-job-REST..."; \
	cd $(SJR_DEPLOY_PATH); \
	bin/start_server.sh

stop:
	@echo "Stopping spark-job-REST..."; \
	cd $(SJR_DEPLOY_PATH); \
	bin/stop_server.sh

.PHONY: all build deploy