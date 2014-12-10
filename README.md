A new implentation of Spark-Server.

## Features:

Supports multiple spark contexts.

The project spins new processes in order to run and manage multiple spark contexts. Inter-process comunication is done with akka actors.

## Building Spark-job-rest (SJR)

The project has maven nature.

## Configure Spark-job-rest

In order to configure SJR the following files need to be edited.

* resources/application.conf: Contains all the application configurations.

## Run Spark-job-rest

After editing all the configuration files SJR can be run by executing the script : start-server.sh
