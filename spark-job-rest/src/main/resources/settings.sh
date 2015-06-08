#!/usr/bin/env bash

CDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
APP_DIR="${CDIR}/../"

if [ -z "${SPARK_HOME}" ]; then
    SPARK_HOME="/opt/apache-spark"
fi

if [ -z "${SPARK_CONF_HOME}" ]; then
    SPARK_CONF_HOME=$SPARK_HOME/conf
fi

# Only needed for Mesos deploys
#SPARK_EXECUTOR_URI=/home/spark/spark-1.1.0.tar.gz

#Logging directory
LOG_DIR="${APP_DIR}/logs"
