CURRENT_DIR := $(shell pwd)
PACKAGE_PATH := $(CURRENT_DIR)/spark-job-rest/target/spark-job-rest.tar.gz
SJR_DEPLOY_PATH ?= $(CURRENT_DIR)/deploy

all: build deploy

build:
	@mvn clean install

deploy:
	@echo "Deploying to '$(SJR_DEPLOY_PATH)'..."; \
	rm -rf $(SJR_DEPLOY_PATH) ; \
	mkdir -p $(SJR_DEPLOY_PATH) ; \
	tar zxf $(PACKAGE_PATH) -C $(SJR_DEPLOY_PATH) --strip-components=1; \
	sh $(SJR_DEPLOY_PATH)/resources/install.sh

start:
	@echo "Starting spark-job-REST..."; \
	cd $(SJR_DEPLOY_PATH); \
	bin/start_server.sh

stop:
	@echo "Stopping spark-job-REST..."; \
	cd $(SJR_DEPLOY_PATH); \
	bin/stop_server.sh

.PHONY: all build deploy