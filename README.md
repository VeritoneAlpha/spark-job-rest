[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Atigeo/spark-job-rest?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

## Features:

**Supports multiple spark contexts created from the same server**

The main problem this project solves is the inability to run multiple Spark contexts from the same JVM. This is a bug in Spark core that was also present in Ooyala's Spark Job Server, from which this project is inspired. The project launches a new process for each Spark context/application, with its own driver memory setting and its own driver log. Each driver JVM is created with its own Spark UI port, sent back to the api caller. Inter-process communication is achieved with akka actors, and each process is shut down when a Spark context/application is deleted.

## Version compatibility

SJR Version   | Spark Version
------------- | -------------
0.3.0         |  1.1.0 
0.3.1         |  1.3.1 

## Building Spark-Job-Rest (SJR)

The project is build with Maven3 and Java7.
```
make build
```
SJR can now be deployed from spark-job-rest/spark-job-rest/target/spark-job-rest.tar.gz

If your build fails with this error:
```
[ERROR] spark-job-rest/src/main/scala/server/domain/actors/ContextManagerActor.scala:171: error: value redirectOutput is not a member of ProcessBuilder
```
This happens because Maven uses Java6. You can run mvn -version in order to check the Java version that Maven uses.
```
$ mvn -version
Apache Maven 3.2.5
Java version: 1.6.0_65
```
If Maven uses Java6 you need to change it to Java7. This can be done by adding the JAVA_HOME export in your ~/.mavenrc file:
```
OSX:
export JAVA_HOME=/Library/Java/JavaVirtualMachines/{jdk-version}/Contents/Home
```
```
Ubuntu:
export JAVA_HOME=/usr/lib/jvm/{jdk-version}
```

If running from IDE fails with:
```
Exception in thread "main" java.lang.NoClassDefFoundError: akka/actor/Props
```
This happens because the spark dependency has the provided scope. In order to run from IDE you can remove the provided scope for the spark dependency(inside pom.xml) or you can add the spark assembly jar to the running classpath.

## Deploying Spark-Job-Rest

You can deploy Spark-Job-Rest by:
```
make deploy
```
which currently supports only local deploy.

Optionally you can specify deploy directory in `$SJR_DEPLOY_PATH` environment variable:
```
SJR_DEPLOY_PATH=/opt/spark-job-rest make deploy
```

In order to have a make proper installation you should set `$SPARK_HOME` to your Apache Spark distribution and `$SPARK_CONF_HOME` to directory which consists `spark-env.sh` (usually `$SPARK_HOME/conf` or `$SPARK_HOME/libexec/conf`).
You can do it in your bash profile (`~/.bash_profile` or `~/.bashrc`) by adding the following lines:
```
export SPARK_HOME=<Path to Apache Spark>
export SPARK_CONF_HOME=$SPARK_HOME/libexec/conf
```
After that either run in the new terminal session or source your bash profile.

To reinstall application or install it at remote host run `$SJR_DEPLOY_PATH/resources/install.sh` it will set proper directory paths.
 
## Starting Spark-Job-Rest

To start/stop SJR use
```
cd $SJR_DEPLOY_PATH
bin/start_server.sh
bin/stop_server.sh
```

or if it deployed to default destination just
```
make start
make stop
```

## Configure Spark-job-rest

In order to configure SJR the following file needs to be edited: resources/application.conf

* Configure the default spark properties for context creation
``` 
#spark default configuration
spark.executor.memory=2g
spark.master="local"
spark.path="/Users/user/spark-1.1.0"
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

Also, SPARK_HOME variable must be edited in the settings.sh file. It must be pointed to the local Spark deployment folder. The SJR can be run from outside the Spark cluster, but you need to at least copy the deployment folder from one of the slaves or master nodes.

For the UI to work, the file spark-job-rest/src/main/resources/webapp/js/behaviour.js must be edited in order to set the URL and the host of the machine where you are running the server. The UI can be accessed at serverAddress:serverPort/ .

## Run Spark-job-rest

After editing all the configuration files SJR can be run by executing the script : start-server.sh

## API

**Contexts**

- POST /contexts/{contextName}  -  Create Context

 * Body:  Raw entity with key-value pairs. 
 * jars key is required and it should be in the form of a comma separated list of jar paths. These jars will be added at Spark context creation time to the class path of the newly created context's JVM process. There are 3 types of jar paths supported:
    * Absolute path on the server side : /home/ubuntu/example.jar
    * Name of the jar that was uploaded to the server : example.jar
    * Hdfs path : hdfs://devbox.local:8020/user/test/example.jar
  
  ``` 
  Body example:
 jars="/home/ubuntu/example.jar,example.jar,hdfs://devbox.local:8020/user/test/example.jar”
 spark.executor.memory=2g
 driver.xmxMemory = 1g
  ```

- GET /contexts/{contextName}  -  returns Context JSON object | No such context.

- DELETE /contexts/{contextName}  -  Delete Context

**Jobs**

- POST /jobs?runningClass={runningClass}&context={contextName}  - Job Submission 

  * Body:  Raw entity with key-value pairs. Here you can set any configuration properties that will be passed to the config parameter of the validate and run methods of the provided jar (see the SparkJob definition below)

- GET /jobs/{jobId}?contextName={contextName} - Gets the result or state of a specific job

- GET /jobs - Gets the states/results of all jobs from all running contexts 

**Jars**

- POST /jars/{jarName}  - Upload jar
  * Body: Jar Bytes
  
- POST /jars  - Upload jar
  * Body: MultiPart Form

- GET /jars - Gets all the uploaded jars

- DELETE /jars/{jarName} - Delete jar

## HTTP Client

All the API methods can be called from Scala/Java with the help of an HTTP Client.

Maven Spark-Job-Rest-Client dependency:
```
<dependency>
    <groupId>com.xpatterns</groupId>
    <artifactId>spark-job-rest-client</artifactId>
    <version>0.3.1</version>
</dependency>
```

## Create Spark Job Project

Add maven Spark-Job-Rest-Api dependency:
```
<dependency>
    <groupId>com.xpatterns</groupId>
    <artifactId>spark-job-rest-api</artifactId>
    <version>0.3.1</version>
</dependency>
```

To create a job that can be submitted through the server, the class must implement the SparkJob trait.

```
import com.typesafe.config.Config
import org.apache.spark.SparkContext
import api.{SparkJobInvalid, SparkJobValid, SparkJobValidation, SparkJob}

class Example extends SparkJob {
    override def runJob(sc:SparkContext, jobConfig: Config): Any = { ... }
    override def validate(sc:SparkContext, config: Config): SparkJobValidation = { ... }
}
```

- runJob method contains the implementation of the Job. SparkContext and Config objects are provided through parameters.
- validate method allows for an initial validation. In order to run the job return SparkJobValid(), otherwise return SparkJobInvalid(message).

## Example

An example for this project can be found here: ```spark-job-rest/examples/example-job```. In order to package it, run 
``` mvn clean install ```

**Create a context**
```
$ curl -X POST -d "jars=/Users/raduc/projects/spark-job-rest/examples/example-job/target/example-job.jar" 'localhost:8097/contexts/test-context'

{
  "contextName": "test-context",
  "sparkUiPort": "16003"
}
```

**Check if context exists**

```
curl 'localhost:8097/contexts/test-context'

{
  "contextName": "test-context",
  "sparkUiPort": "16003"
}
```

**Run job** - The example job creates an RDD from a Range(0,input) and applies count on it.

```
$ curl -X POST -d "input=10000" 'localhost:8097/jobs?runningClass=com.job.SparkJobImplemented&contextName=test-context'

{
  "jobId": "2bd438a2-ac1e-401a-b767-5fa044b2bd69",
  "contextName": "test-context",
  "status": "Running",
  "result": "",
  "startTime": 1430287260144
}
```

```2bd438a2-ac1e-401a-b767-5fa044b2bd69``` represents the jobId. This id can be used to query for the job status/results.

**Query for results**

```
$ curl 'localhost:8097/jobs/2bd438a2-ac1e-401a-b767-5fa044b2bd69?contextName=test-context'

{
  "jobId": "2bd438a2-ac1e-401a-b767-5fa044b2bd69",
  "contextName": "test-context",
  "status": "Finished",
  "result": "10000",
  "startTime": 1430287261108
}
```

**Delete context**

```
curl -X DELETE 'localhost:8097/contexts/test-context'

{
  "message": "Context deleted."
}
```

**HTTP Client Example**

```
object Example extends App {
  implicit val system = ActorSystem()
  val contextName = "testContext"

  try {
    val sjrc = new SparkJobRestClient("http://localhost:8097")

    val context = sjrc.createContext(contextName, Map("jars" -> "/Users/raduchilom/projects/spark-job-rest/examples/example-job/target/example-job.jar"))
    println(context)

    val job = sjrc.runJob("com.job.SparkJobImplemented", contextName, Map("input" -> "10"))
    println(job)

    var jobFinal = sjrc.getJob(job.jobId, job.contextName)
    while (jobFinal.status.equals(JobStates.RUNNING.toString())) {
      Thread.sleep(1000)
      jobFinal = sjrc.getJob(job.jobId, job.contextName)
    }
    println(jobFinal)

    sjrc.deleteContext(contextName)
  } catch {
    case e:Exception => {
      e.printStackTrace()
    }
  }

  system.shutdown()
}
```
Running this would produce the output:

```
Context(testContext,16002)
Job(ab63c19f-bbb4-461e-8c6f-f0a35f73a943,testContext,Running,,1430291077689)
Job(ab63c19f-bbb4-461e-8c6f-f0a35f73a943,testContext,Finished,10,1430291078694)
```


## UI

The UI was added in a compiled and minified state. For sources and changes please refer to [spark-job-rest-ui](https://github.com/marianbanita82/spark-job-rest-ui) project.
