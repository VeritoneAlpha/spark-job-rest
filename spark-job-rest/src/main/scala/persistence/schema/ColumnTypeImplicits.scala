package persistence.schema

import api.configRenderingOptions
import api.entities.ContextState._
import api.entities.JobState._
import api.entities.{ContextState, Jars, JobState}
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.slf4j.LoggerFactory
import persistence.slickWrapper.Driver.api._

import scala.util.{Failure, Success, Try}

object ColumnTypeImplicits {
  private val log = LoggerFactory.getLogger(getClass)

  /**
   * Custom column type conversion from [[com.typesafe.config.Config]] to [[String]]
   */
  implicit val configColumnType = MappedColumnType.base[Config, String](
  { config: Config => config.root().render(configRenderingOptions) },
  { configString: String => Try { ConfigFactory.parseString(configString) } match {
    case Success(config) => config
    case Failure(e: Throwable) =>
      log.error("Failed to parse config from string.", e)
      ConfigFactory.empty().withValue("error", ConfigValueFactory.fromAnyRef(e.getStackTrace))
  }
  }
  )

  /**
   * Custom column type conversion from [[Jars]] to [[String]]
   */
  implicit val jarsColumnType = MappedColumnType.base[Jars, String](
  { jars: Jars => jars.list.mkString(":") },
  { string: String => Jars.fromString(string) }
  )

  /**
   * Custom column type conversion from [[ContextState]] to [[String]]
   */
  implicit val contextStateColumnType = MappedColumnType.base[ContextState, String](
  { state: ContextState => state.toString },
  { string: String => ContextState.withName(string) }
  )

  /**
   * Custom column type conversion from [[JobState]] to [[String]]
   */
  implicit val jobStateColumnType = MappedColumnType.base[JobState, String](
  { state: JobState => state.toString },
  { string: String => JobState.withName(string) }
  )
}