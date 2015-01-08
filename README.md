A new implentation of Spark-Server.

## Features:

Supports multiple spark contexts.

The project spins new processes in order to run and manage multiple spark contexts. Inter-process comunication is achieved with the use of akka actors.

## Building Spark-job-rest (SJR)

The project has maven nature.

## Configure Spark-job-rest

In order to configure SJR the following file need to be edited: resources/application.conf

* Configure the default spark properties for context creation
``` 
#spark default configuration
spark.executor.memory=2g
spark.mesos.coarse=false
#Default Spark Driver JVM memory
driver.xmxMemory = 1g
```
* Configure settings like web server port and akka system ports
```
#application configuration
appConf{
#the port on which to deploy the apis
web.services.port=8097
........
}
```

## Run Spark-job-rest

After editing all the configuration files SJR can be run by executing the script : start-server.sh

## API

# Contexts

1. POST /context/{contextName}  -  Create Context
  * Body:  Raw entity with key-value pairs. 
  * Jars key is required and optional you can set any spark config or the config for the driver JVM memory:
  * jars="/home/ubuntu/example.jar:/home/ubuntu/spark-job-project.jar” - Multiple jars path separated with the ':'   character. These jars will be added at creation time to the class path of the context process.
  * spark.executor.memory=2g
  * driver.xmxMemory = 1g

2.  GET /context/{contextName}  -  Enquiry if context exists. 

3. DELETE /context/{contextName}  -  Delete Context

# Jobs

1. POST 10.0.2.110:8097/job?runningClass={runningClass}&context={contextName}  - Job Submission 
  * Body:  Raw entity with key-value pairs. Here you can set any configs that will be found in the config parameter received by the validate and run methods.

2. GET /job?jobId={uuid}&contextName={contextName} - Gets the result or status of a specific job
