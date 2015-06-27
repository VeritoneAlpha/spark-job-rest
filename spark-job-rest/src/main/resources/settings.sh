#!/usr/bin/env bash

CDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
APP_DIR="${CDIR}/../"
DEPLOY_CONFIG="${CDIR}/deploy-settings.sh"

# Load optional deployment settings
if [ -f "${DEPLOY_CONFIG}" ]; then
    source "${DEPLOY_CONFIG}"
fi

if [ -z "${SPARK_HOME}" ]; then
    SPARK_HOME="/opt/spark"
fi

if [ -z "${SPARK_CONF_HOME}" ]; then
    SPARK_CONF_HOME=$SPARK_HOME/conf
fi

# Only needed for Mesos deploys
#SPARK_EXECUTOR_URI=/home/spark/spark-1.1.0.tar.gz

# Logging directory
LOG_DIR=${SJR_LOG_DIR-"${APP_DIR}/logs"}

# Extra classes:
EXTRA_CLASSPATH="${JSR_EXTRA_CLASSPATH}"

# Set proper jar path
JAR_PATH=${SJR_JAR_PATH-"${APP_DIR}/jars"}

# Set database root directory
DATABASE_ROOT_DIR=${SJR_DATABASE_ROOT_DIR-"${APP_DIR}/db"}

# Root location for contexts process directories
CONTEXTS_BASE_DIR=${SJR_CONTEXTS_BASE_DIR-"${APP_DIR}/contexts"}