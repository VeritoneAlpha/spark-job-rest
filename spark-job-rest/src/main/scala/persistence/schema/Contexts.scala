package persistence.schema

import api.entities.{ContextDetails, Jars}
import api.entities.ContextState._
import com.typesafe.config.Config
import persistence.slickWrapper.Driver.api._
import api.types._

/**
 * Contexts table is responsible for storing information about contexts
 * @param tag table tag name
 */
class Contexts(tag: Tag) extends Table[ContextDetails] (tag, contextsTable) {
  import ColumnTypeImplicits._

  def id = column[ID]("CONTEXT_ID", O.PrimaryKey)
  def name = column[String]("NAME")
  def submittedConfig = column[Config]("SUBMITTED_CONFIG", O.SqlType(configSQLType))
  def finalConfig = column[Option[Config]]("FINAL_CONFIG", O.SqlType(configSQLType))
  def jars = column[Jars]("JARS")
  def state = column[ContextState]("STATE")
  def details = column[String]("DETAILS", O.SqlType("VARCHAR(4096)"))
  def sparkUiPort = column[Option[String]]("SPARK_UI_PORT")

  def * = (name, submittedConfig, finalConfig, jars, state, details, sparkUiPort, id) <> (ContextDetails.tupled, ContextDetails.unapply)
}
