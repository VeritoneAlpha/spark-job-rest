package integration

import akka.actor.ActorSystem
import akka.util.Timeout
import client.SparkJobRestClient
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, BeforeAndAfter, FunSuite}
import org.scalatest.junit.JUnitRunner
import responses.Job
import server.Main

/**
* Created by raduchilom on 4/25/15.
*/
@RunWith(classOf[JUnitRunner])
class IntegrationTests extends FunSuite with BeforeAndAfter with ScalaFutures with Matchers {

  implicit val timeout = Timeout(10000)
  implicit val system = ActorSystem("localSystem")

//  Main.main(Array[String]())
  val client = new SparkJobRestClient("http://localhost:8097")

  val contextName = "testContext"
  val parameters = Map[String, String]("jars" -> "/Users/raduchilom/projects/spark-job-rest/examples/example-job/target/example-job.jar",
    "input" -> "100")

  before {
  }

  after {
  }

  test("Create Context & Delete Context") {
    val context = client.createContext(contextName, parameters)
    context.contextName should be(contextName)

    var contexts = client.getContexts()
    contexts.contexts.size should be(1)
    contexts.contexts.contains(context) should be(true)

    client.deleteContext(contextName) should be(true)
    contexts = client.getContexts()
    contexts.contexts.size should be(0)
  }

  test("Create Contexts & Delete Contexts") {

    for(i <- 0 to 4) {
      val context = client.createContext(contextName + i, parameters)
      context.contextName should be(contextName + i)
    }

    var contexts = client.getContexts()
    contexts.contexts.size should be(5)

    for(i <- 0 to 4) {
      client.deleteContext(contextName + i) should be(true)
      contexts = client.getContexts()
      contexts.contexts.size should be(4 - i)
    }
  }

  test("Create Context & Run Job") {
    val context = client.createContext(contextName, parameters)
    context.contextName should be(contextName)

    var contexts = client.getContexts()
    contexts.contexts.size should be(1)
    contexts.contexts.contains(context) should be(true)

    val job = client.runJob("com.job.SparkJobImplemented", contextName, parameters)
    job shouldBe a [Job]

    val jobResult = client.getJob(job.jobId, contextName)
    jobResult.result should be("100")

    client.deleteContext(contextName) should be(true)
    contexts = client.getContexts()
    contexts.contexts.size should be(0)
  }

}
