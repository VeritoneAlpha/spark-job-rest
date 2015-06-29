package persistence.services

import api.entities.ContextDetails
import api.entities.ContextState._
import persistence.schema._
import persistence.slickWrapper.Driver.api._
import server.domain.actors.durations._
import api.types._
import persistence.schema.ColumnTypeImplicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

/**
 * Collection of methods for persisting context entities
 */
object ContextPersistenceService {
  /**
   * Synchronously updates state for context with specified id.
   * Does not replace [[Error]] or [[Terminated]] states.
   *
   * @param contextId context's ID
   * @param newState context state to set
   * @param db database connection
   */
  def updateContextState(contextId: ID, newState: ContextState, db: Database, newDetails: String = ""): Unit = {
    val affectedContext = for { c <- contexts if c.id === contextId && c.state =!= Failed && c.state =!= Terminated } yield c
    val contextStateUpdate = affectedContext map (x => (x.state, x.details)) update (newState, newDetails)
    Await.ready(db.run(contextStateUpdate), defaultDbTimeout)
  }

  def contextById(contextId: ID, db: Database): Future[Option[ContextDetails]] = {
    db.run(contexts.filter(c => c.id === contextId).result).map {
      case Seq(context) => Some(context)
      case _ => None
    }
  }

  def allContexts(db: Database): Future[Array[ContextDetails]] = {
    db.run(contexts.result).map(_.toArray)
  }

  def allInactiveContexts(db: Database): Future[Array[ContextDetails]] = {
    db.run(contexts.filter(c => c.state =!= Running).result).map(_.toArray)
  }
}
