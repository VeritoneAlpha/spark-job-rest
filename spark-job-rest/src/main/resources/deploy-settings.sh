#!/usr/bin/env bash

EXTRA_CLASSPATH="${EXTRA_CLASSPATH}:/opt/cloudera/parcels/CDH/jars/hadoop-aws-2.6.0-cdh5.4.2.jar:/opt/cloudera/parcels/CDH/jars/guava-12.0.1.jar"

JAVA_OPTS="${JAVA_OPTS}
           -Dspray.can.server.parsing.max-content-length=100m"