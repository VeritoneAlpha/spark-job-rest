Project started from an ooyala spark-jobserver fork. (https://github.com/ooyala/spark-jobserver)

## Features:

Supports multiple spark contexts.
Increased limit for jar uploads(200MB)

The project uses a hierarchy of classloaders in order to achieve the isolation needed for running multiple spark contexts in the same JVM.

## Building SJS

You need to have sbt 0.13.x installed. To build SJS, follow the below steps:

cd spark-jobserver-master
sbt compile package assembly

## Configure SJS

In order configure Jaws the following files inside the "spark-jobserver-master/config" need to be edited.

* application.conf: Contains all the application configurations.
	- master : Spark/mesos master
	- home : Spark home
	- jobserverjar : Full path to spark-jobserver-master/job-server/target/spark-job-server.jar
	- jobmanagerjar : Full path to spark-jobserver-master/job-manager-helper/target/job-manager-helper-0.3.1.jar
	- akkajar  : Full path to spark-jobserver-master/akka-app/target/akka.jar
	- mesoshelperjar : Full path to mesos-loader/target/mesos-loader-4.0.0.jar
	- mesosjar : Full path to mesos jar (mesos-0.16.0.jar)
* start-server.sh : Used for starting the server.
	- Path to application.conf must be configured
	- Path to mesos native lib(libmesos.so) must be configured


## Run SJS

After editing all the configuration files SJS can be run in the following manner:

spark-jobserver-master/job-server/target/start-server.sh

For more info access https://github.com/ooyala/spark-jobserver
