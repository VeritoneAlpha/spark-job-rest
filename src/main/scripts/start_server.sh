#!/bin/bash
# Script to start the job server
set -e

parentdir="$(dirname "$dir")"

get_abs_script_path() {
  pushd . >/dev/null
  cd $(dirname $0)
  appdir=$(pwd)
  popd  >/dev/null
}

get_abs_script_path

parentdir="$(dirname "$appdir")"

GC_OPTS="-XX:+UseConcMarkSweepGC
         -verbose:gc -XX:+PrintGCTimeStamps -Xloggc:$appdir/gc.out
         -XX:MaxPermSize=512m
         -XX:+CMSClassUnloadingEnabled "

JAVA_OPTS="-Xmx1g -XX:MaxDirectMemorySize=512M
           -XX:+HeapDumpOnOutOfMemoryError -Djava.net.preferIPv4Stack=true
           -Dcom.sun.management.jmxremote.authenticate=false
           -Dcom.sun.management.jmxremote.ssl=false"

MAIN="server.Main"

conffile=$(ls -1 $parentdir/resources/*.conf | head -1)
if [ -z "$conffile" ]; then
  echo "No configuration file found"
  exit 1
fi

if [ -f "$parentdir/resources/settings.sh" ]; then
  . $parentdir/resources/settings.sh
else
  echo "Missing $parentdir/resources/settings.sh, exiting"
  exit 1
fi


LOGGING_OPTS="-Dlog4j.configuration=log4j-server.properties"

# For Mesos
#CONFIG_OVERRIDES="-Dspark.executor.uri=$SPARK_EXECUTOR_URI "
# For Mesos/Marathon, use the passed-in port
if [ "$PORT" != "" ]; then
  CONFIG_OVERRIDES+="-Dspark.jobserver.port=$PORT "
fi


# job server jar needs to appear first so its deps take higher priority
# need to explicitly include app dir in classpath so logging configs can be found
#CLASSPATH="$appdir:$appdir/spark-job-server.jar:$($SPARK_HOME/bin/compute-classpath.sh)"
CLASSPATH="$PRE_CLASSPATH:$parentdir/resources:$appdir:$parentdir/repo/*:$parentdir/spark-job-rest.jar"
echo "CLASSPATH = $CLASSPATH"

rm -rf logs
mkdir logs

exec java -cp $CLASSPATH $GC_OPTS $JAVA_OPTS $LOGGING_OPTS $CONFIG_OVERRIDES $MAIN $conffile > "logs/server.log" 2>&1 &
