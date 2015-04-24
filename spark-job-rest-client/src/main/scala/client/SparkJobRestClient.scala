//package client
//
//import java.util.concurrent.TimeUnit
//
//import akka.actor.{ActorSystem}
//import akka.util.Timeout
//import spray.http._
//import spray.client.pipelining._
//import client.JsonUtils._
//
//
//import scala.concurrent.Future
//
///**
// * Created by raduc on 23/04/15.
// */
//class SparkJobRestClient(serverAddress: String)(implicit system: ActorSystem) {
//
//  val getContextsAddress = "/contexts"
//  implicit val timeout = Timeout(1, TimeUnit.SECONDS)
//
//  def getContexts() : List[String] = {
//    import system.dispatcher
//
//    val pipeline: HttpRequest => Future[List[List[String]]] = sendReceive ~> unmarshal[List[List[String]]]
//
//    val response: Future[List[List[String]]] = pipeline(Get(serverAddress + getContexts()))
//    response.onSuccess {
//      case l: List[List[String]] => {
//        return l.map(_.head)
//      }
//    }
//
//    List()
//  }
//
//}
//
//object ab extends App {
//  implicit val system = ActorSystem()
//
//  val sjrc = new SparkJobRestClient("localhost:8097")
//  println(sjrc.getContexts())
//
//}
