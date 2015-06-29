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

# The following should be exported in order to be accessible in Config substitutions
export SPARK_HOME
export APP_DIR
export JAR_PATH
export CONTEXTS_BASE_DIR

# Context application settings
export SPARK_JOB_REST_CONTEXT_NAME="$contextName"
export SPARK_JOB_REST_CONTEXT_PORT="$port"

# job server jar needs to appear first so its deps take higher priority
# need to explicitly include app dir in classpath so logging configs can be found
#CLASSPATH="$parentdir/resources:$appdir:$parentdir/spark-job-rest.jar:$classpathParam:$EXTRA_CLASSPATH"
#echo "CLASSPATH = ${CLASSPATH}"

JARS=`echo "${classpathParam}:${EXTRA_CLASSPATH}" | sed -e 's/:/,/g'`
echo "JARS = ${JARS}"

# Create context process directory
mkdir -p "${processDir}"

cd "${processDir}"

# Start application using `spark-submit` which takes cake of computing classpaths
"${SPARK_HOME}/bin/spark-submit" \
  --class $MAIN \
  --driver-memory $xmxMemory \
  --conf "spark.executor.extraJavaOptions=${LOGGING_OPTS}" \
  --driver-java-options "${GC_OPTS} ${JAVA_OPTS} ${LOGGING_OPTS} ${CONFIG_OVERRIDES}" \
  --jars "${JARS}" $parentdir/spark-job-rest.jar \
  $conffile >> "${LOG_DIR}/${LOG_FILE}" 2>&1 &
echo $! > "${processDir}/context.pid"
