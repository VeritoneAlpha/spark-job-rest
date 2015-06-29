package persistence.schema

import com.typesafe.config.{Config, ConfigFactory}
import persistence.slickWrapper.Driver.api._
import api.entities.{ContextState, JobState, Jars}
import api.entities.ContextState._
import api.entities.JobState._
import api.configRenderingOptions

object ColumnTypeImplicits {
  /**
   * Custom column type conversion from [[com.typesafe.config.Config]] to [[String]]
   */
  implicit val configColumnType = MappedColumnType.base[Config, String](
  { config: Config => config.root().render(configRenderingOptions) },
  { configString: String =>
    ConfigFactory.parseString(configString)
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