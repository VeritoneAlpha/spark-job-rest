package persistence.schema

import com.typesafe.config.Config
import persistence.PersistentEnumeration
import persistence.slickWrapper.Driver.api._


/**
 * Enumeration for all context states.
 */
object ContextState extends PersistentEnumeration {
  type ContextState = Value
  val Requested = Value("requested")
  val Running = Value("running")
  val Stopped = Value("stopped")
  val Failed = Value("failed")
}

import persistence.schema.ContextState._

/**
 * Simple wrapper for JARs list persistence.
 * Check [[ColumnTypeImplicits]] for column types conversion.
 * @param list list of JARs
 */
case class Jars(list: List[String] = Nil)

/**
 * Companion object methods for [[Jars]]
 */
object Jars {
  /**
   * Loads [[Jars]] from string
   * @param repr JARSs as string
   * @return deserialized Jars
   */
  def fromString(repr: String): Jars = repr match {
    case "" => Jars()
    case jarsString: String => Jars(jarsString.split(":").toList)
  }
}

/**
 * Context entity
 * @param id context id
 * @param name context name
 * @param submittedConfig config submitted by client
 * @param finalConfig final config passed to context
 * @param jars list of JARs associated with the config
 * @param state context state
 * @param details detailed information about context state
 */
case class ContextEntity(name: String, submittedConfig: Config, finalConfig: Option[Config], jars: Jars, state: ContextState = Requested, details: String = "", id: ID = nextId)

/**
 * Contexts table is responsible for storing information about contexts
 * @param tag table tag name
 */
class Contexts(tag: Tag) extends Table[ContextEntity] (tag, contextsTable) {
  import ColumnTypeImplicits._

  def id = column[ID]("CONTEXT_ID", O.PrimaryKey)
  def name = column[String]("NAME")
  def submittedConfig = column[Config]("SUBMITTED_CONFIG", O.SqlType(configSQLType))
  def finalConfig = column[Option[Config]]("FINAL_CONFIG", O.SqlType(configSQLType))
  def jars = column[Jars]("JARS")
  def state = column[ContextState]("STATE")
  def details = column[String]("DETAILS", O.SqlType("VARCHAR(4096)"))

  def * = (name, submittedConfig, finalConfig, jars, state, details, id) <> (ContextEntity.tupled, ContextEntity.unapply)
}
