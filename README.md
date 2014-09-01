Project started from an ooyala spark-jobserver fork. (https://github.com/ooyala/spark-jobserver)

## Features:

Supports multiple spark contexts.

Increased limit for jar uploads(200MB)

The project uses a hierarchy of classloaders in order to achieve the isolation needed for running multiple spark contexts in the same JVM.

## Building Spark-job-rest (SJR)

You need to have sbt 0.13.x installed. To build SJR, follow the below steps:

cd spark-job-rest

sbt compile package assembly

## Configure Spark-job-rest

In order to configure SJR the following files need to be edited.

* config/application.conf: Contains all the application configurations.
	- master : Spark/mesos master
	- home : Spark home
	- jobserverjar : Full path to /job-server/target/spark-job-rest.jar
	- jobmanagerjar : Full path to /job-manager-helper/target/job-manager-helper-0.3.1.jar
	- akkajar  : Full path to /akka-app/target/akka.jar
	- mesoshelperjar : Full path to mesos-loader/target/mesos-loader-0.3.1.jar
	- mesosjar : Full path to mesos jar (mesos-0.16.0.jar)
* bin/start-server.sh : Used for starting the server.
	- Path to application.conf must be configured
	- Path to mesos native lib(libmesos.so) must be configured


## Run Spark-job-rest

After editing all the configuration files SJR can be run by executing the script : start-server.sh

For more info access https://github.com/ooyala/spark-jobserver
