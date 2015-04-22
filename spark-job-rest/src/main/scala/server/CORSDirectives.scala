package server

import spray.http._
import spray.routing._
/**
 * Created by emaorhian
 * Copy-pasted from https://github.com/Atigeo/jaws-spark-sql-rest/blob/01a1bd6579123ca4762ca00ac63cdb62e962125e/jaws-spark-sql-rest/src/main/scala/customs/CORSDirectives.scala
 */
trait CORSDirectives { this: HttpService =>
  private def respondWithCORSHeaders(origin: String, rh: Seq[HttpHeader]) = {
    val headers: List[HttpHeader] = List(
      HttpHeaders.`Access-Control-Allow-Origin`(SomeOrigins(List(origin))),
      HttpHeaders.`Access-Control-Allow-Credentials`(true),
      HttpHeaders.`Access-Control-Allow-Headers`("Origin", "X-Requested-With", "Content-Type", "Accept", "apiKey", "affiliationid")
    ) ++ rh.toList

    respondWithHeaders(headers)
  }
  private def respondWithCORSHeadersAllOrigins(rh: Seq[HttpHeader]) = {
    val headers: List[HttpHeader] = List(
      HttpHeaders.`Access-Control-Allow-Origin`(AllOrigins),
      HttpHeaders.`Access-Control-Allow-Credentials`(true),
      HttpHeaders.`Access-Control-Allow-Headers`("Origin", "X-Requested-With", "Content-Type", "Accept"),
      HttpHeaders.`Access-Control-Allow-Methods`(HttpMethods.DELETE, HttpMethods.GET, HttpMethods.POST  )
    ) ++ rh.toList

    respondWithHeaders(headers)
  }

  def corsFilter(origins: List[String], rh: HttpHeader*)(route: Route) =
    if (origins.contains("*"))
      respondWithCORSHeadersAllOrigins(rh)(route)
    else
      optionalHeaderValueByName("Origin") {
        case None =>
          route
        case Some(clientOrigin) => {
          if (origins.contains(clientOrigin))
            respondWithCORSHeaders(clientOrigin, rh)(route)
          else {
            // Maybe, a Rejection will fit better
            complete(StatusCodes.Forbidden, "Invalid origin")
          }
        }
      }
}
