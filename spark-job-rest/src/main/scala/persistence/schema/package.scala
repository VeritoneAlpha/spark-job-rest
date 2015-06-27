package persistence

import java.util.UUID

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import persistence.slickWrapper.Driver.api._

package object schema {
  /**
   * This is a type alias for entity ID
   */
  type ID = UUID

  /**
   * Type alias for optional link to foreign entity
   */
  type WEAK_LINK = Option[ID]

  /**
   * Returns next unique identifier. We use it to simplify switching to different identifiers backend.
   * @return next ID
   */
  def nextId: ID = UUID.randomUUID()

  /**
   * This render potions used to render configs to database.
   */
  val configRenderingOptions = ConfigRenderOptions.concise()

  /**
   * This SQL type should be used as a backend for [[com.typesafe.config.Config]].
   */
  val configSQLType = "CLOB"

  /**
   * Table tags
   */
  val jobsTable = "JOBS"
  val contextsTable = "CONTEXTS"

  /**
   * Tables: entry points for table schema
   */
  val jobs = TableQuery[Jobs]
  val contexts = TableQuery[Contexts]

  /**
   * Add here tables which should be created during server start.
   */
  val autoGeneratedTables = List(
    (contextsTable, contexts.schema),
    (jobsTable, jobs.schema)
  )

  object implicits {
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
    {
      case "" => Jars()
      case jarsString: String => Jars(jarsString.split(":").toList)
    }
    )
  }
}
