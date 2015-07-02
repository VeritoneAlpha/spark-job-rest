package integration

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.util.Timeout
import api.responses.Job
import client.SparkJobRestClient
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

/**
* Created by raduchilom on 4/25/15.
*/
@RunWith(classOf[JUnitRunner])
class IntegrationTests extends FunSuite with ScalaFutures with Matchers {

  implicit val timeout = Timeout(10, TimeUnit.SECONDS)
  implicit val system = ActorSystem("localSystem")

  val client = new SparkJobRestClient("http://localhost:8097")
  val contextName = "testContext"
  val exampleJarPath = "/Users/raduchilom/projects/spark-job-rest/examples/example-job/target/example-job.jar"
  val parameters = Map[String, String]("jars" -> exampleJarPath,
    "input" -> "100")

  test("Create Context & Delete Context") {
    val context = client.createContext(contextName, parameters)
    context.contextName should be(contextName)

    var contexts = client.getContexts
    contexts.contexts.length shouldBe 1
    contexts.contexts.contains(context) shouldBe true

    client.deleteContext(contextName) shouldBe true
    contexts = client.getContexts
    contexts.contexts.length shouldBe 0
  }

  test("Create Contexts & Delete Contexts") {

    for(i <- 0 to 4) {
      val context = client.createContext(contextName + i, parameters)
      context.contextName should be (contextName + i)
    }

    var contexts = client.getContexts
    contexts.contexts.length shouldBe 5

    for(i <- 0 to 4) {
      client.deleteContext(contextName + i) shouldBe true
      contexts = client.getContexts
      contexts.contexts.length should be (4 - i)
    }
  }

  test("Create Context & Run Job") {
    val context = client.createContext(contextName, parameters)
    context.contextName should be(contextName)

    var contexts = client.getContexts
    contexts.contexts.length shouldBe 1
    contexts.contexts.contains(context) shouldBe true

    val job = client.runJob("com.job.SparkJobImplemented", contextName, parameters)
    job shouldBe a [Job]

    Thread.sleep(2000)

    val jobResult = client.getJob(job.jobId)
    jobResult.result should be("100")

    client.deleteContext(contextName) shouldBe true
    contexts = client.getContexts
    contexts.contexts.length shouldBe 0
  }

  test("Upload Jar") {
    val jarInfo = client.uploadJar("example-job.jar", exampleJarPath)
    jarInfo.name shouldBe "example-job.jar"
  }

}
