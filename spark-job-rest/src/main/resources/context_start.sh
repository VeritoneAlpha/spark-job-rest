#!/bin/bash
# Script to start the job server
set -e

get_abs_script_path() {
  pushd . >/dev/null
  cd $(dirname $0)
  appdir=$(pwd)
  popd  >/dev/null
}
get_abs_script_path

parentdir="$(dirname "$appdir")"

classpathParam=$1
contextName=$2
port=$3
xmxMemory=$4
processDir=$5

echo "classpathParam = $classpathParam"
echo "contextName = $contextName"
echo "port = $port"


GC_OPTS="-XX:+UseConcMarkSweepGC
         -verbose:gc -XX:+PrintGCTimeStamps -Xloggc:$appdir/gc.out
         -XX:MaxPermSize=512m
         -XX:+CMSClassUnloadingEnabled"

JAVA_OPTS="-Xmx$xmxMemory -XX:MaxDirectMemorySize=512M
           -XX:+HeapDumpOnOutOfMemoryError -Djava.net.preferIPv4Stack=true
           -Dcom.sun.management.jmxremote.authenticate=false
           -Dcom.sun.management.jmxremote.ssl=false"

MAIN="server.MainContext"

if [ -f "$appdir/settings.sh" ]; then
  . $appdir/settings.sh
else
  echo "Missing $appdir/settings.sh, exiting"
  exit 1
fi

if [ -z "$SPARK_HOME" ]; then
  echo "Please set SPARK_HOME or put it in $appdir/settings.sh first"
  exit 1
fi

# Pull in other env vars in spark config, such as MESOS_NATIVE_LIBRARY
. $SPARK_CONF_HOME/spark-env.sh

mkdir -p $LOG_DIR

LOG_FILE="$contextName.log"
LOGGING_OPTS="-Dlog4j.configuration=log4j.properties
              -DLOG_DIR=${LOG_DIR}
              -DLOG_FILE=${LOG_FILE}"

# For Mesos
CONFIG_OVERRIDES=""
if [ -n "$SPARK_EXECUTOR_URI" ]; then
  CONFIG_OVERRIDES="-Dspark.executor.uri=$SPARK_EXECUTOR_URI "
fi
# For Mesos/Marathon, use the passed-in port
if [ "$PORT" != "" ]; then
  CONFIG_OVERRIDES+="-Dspark.jobserver.port=$PORT "
fi

# Need to explicitly include app dir in classpath so logging configs can be found
CLASSPATH="${parentdir}/spark-job-rest.jar:$parentdir:$parentdir/resources:$appdir" >> "${LOG_DIR}/${LOG_FILE}"

# Replace ":" with commas in classpath
JARS=`echo "${classpathParam}" | sed -e 's/:/,/g'`

# Include extra classpath if not empty
if [ ! "${EXTRA_CLASSPATH}" = "" ]; then
    EXTRA_JARS=`echo "${EXTRA_CLASSPATH}" | sed -e 's/:/,/g'`
    JARS="${JARS},${EXTRA_JARS}"
fi

# Prepend with SQL extras if exists
SQL_EXTRAS="${parentdir}/spark-job-rest-sql.jar"
if [ -f "${SQL_EXTRAS}" ]; then
    CLASSPATH="${SQL_EXTRAS}:${CLASSPATH}"
    JARS="${SQL_EXTRAS},${JARS}"
fi

# Log classpath and jars
echo "CLASSPATH = ${CLASSPATH}" >> "${LOG_DIR}/${LOG_FILE}"
echo "JARS = ${JARS}" >> "${LOG_DIR}/${LOG_FILE}"

# The following should be exported in order to be accessible in Config substitutions
export SPARK_HOME
export APP_DIR
export JAR_PATH
export CONTEXTS_BASE_DIR

# Context application settings
export SPARK_JOB_REST_CONTEXT_NAME="$contextName"
export SPARK_JOB_REST_CONTEXT_PORT="$port"

# Create context process directory
mkdir -p "${processDir}"

cd "${processDir}"

# Start application using `spark-submit` which takes cake of computing classpaths
"${SPARK_HOME}/bin/spark-submit" \
  --verbose \
  --class $MAIN \
  --driver-memory $xmxMemory \
  --conf "spark.executor.extraJavaOptions=${LOGGING_OPTS}" \
  --conf "spark.driver.extraClassPath=${CLASSPATH}" \
  --driver-java-options "${GC_OPTS} ${JAVA_OPTS} ${LOGGING_OPTS} ${CONFIG_OVERRIDES}" \
  --jars "${JARS}" "${parentdir}/spark-job-rest.jar" \
  $conffile >> "${LOG_DIR}/${LOG_FILE}" 2>&1 &
echo $! > "${processDir}/context.pid"
