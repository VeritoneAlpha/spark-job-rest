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
}

import persistence.schema.ContextState._

/**
 * Simple wrapper for JARs list persistence.
 * Check [[implicits]] for column types conversion.
 * @param list list of JARs
 */
case class Jars(list: List[String] = Nil)

/**
 * Context entity
 * @param id context id
 * @param name context name
 * @param state context state
 * @param config context config
 * @param jars list of JARs associated with the config
 */
case class Context(name: String, state: ContextState, config: Config, jars: Jars, id: ID = nextId)

/**
 * Contexts table is responsible for storing information about contexts
 * @param tag table tag name
 */
class Contexts(tag: Tag) extends Table[Context] (tag, contextsTable) {
  import implicits._

  def id = column[ID]("CONTEXT_ID", O.PrimaryKey)
  def name = column[String]("NAME")
  def state = column[ContextState]("STATE")
  def config = column[Config]("CONFIG", O.SqlType(configSQLType))
  def jars = column[Jars]("JARS")

  def * = (name, state, config, jars, id) <> (Context.tupled, Context.unapply)
}
