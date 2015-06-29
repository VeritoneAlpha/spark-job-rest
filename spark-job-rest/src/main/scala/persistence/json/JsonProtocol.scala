package persistence.json

import java.util.UUID

import com.typesafe.config.{Config, ConfigFactory}
import persistence.schema.ContextState.ContextState
import persistence.schema._
import spray.json._

import scala.util.{Failure, Success, Try}

/**
 * Json serializers/deserializers
 */
object JsonProtocol extends DefaultJsonProtocol {
  implicit val jarsJsonFormat = jsonFormat1(Jars.apply)

  implicit object ConfigJsonFormat extends JsonFormat[Config] {
    def write(config: Config) = JsString(config.root().render(configRenderingOptions))

    def read(jsonConfig: JsValue) = jsonConfig match {
      case JsString(stringState) => Try { ConfigFactory.parseString(stringState) } match {
        case Success(config: Config) => config
        case Failure(e: Throwable) => deserializationError("Error while parsing config", e)
        case error => deserializationError(s"Error while parsing config: $error")
      }
      case _ => deserializationError("Expected string for config")
    }
  }

  implicit object ContextStateJsonFormat extends JsonFormat[ContextState] {
    def write(state: ContextState) = JsString(state.toString)

    def read(jsonState: JsValue) = jsonState match {
      case JsString(stringState) => Try { ContextState.withName(stringState) } match {
        case Success(state: ContextState) => state
        case Failure(e: Throwable) => deserializationError("Error while parsing context state", e)
        case error => deserializationError(s"Error while parsing context state: $error")
      }
      case _ => deserializationError("Expected string for context state")
    }
  }

  implicit object UUIDJsonFormat extends JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)

    def read(jsValue: JsValue) = jsValue match {
      case JsString(uuidString) => Try { UUID.fromString(uuidString) } match {
        case Success(uuid: UUID) => uuid
        case Failure(e: Throwable) => deserializationError("Error while parsing UUID", e)
        case error => deserializationError(s"Error while parsing UUID: $error")
      }
      case _ => deserializationError("Expected string for UUID")
    }
  }

  implicit val contextEntityJsonFormat = jsonFormat7(ContextEntity)
}
