package persistence.services

import persistence.schema.ContextState._
import persistence.schema._
import persistence.slickWrapper.Driver.api._
import server.domain.actors.durations._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

/**
 * Collection of methods for persisting context entities
 */
object ContextPersistenceService {
  /**
   * Synchronously updates state for context with specified id.
   * Does not replace [[Failed]] or [[Stopped]] states.
   *
   * @param contextId context's ID
   * @param newState context state to set
   * @param db database connection
   */
  def updateContextState(contextId: ID, newState: ContextState, db: Database, newDetails: String = ""): Unit = {
    val affectedContext = for { c <- contexts if c.id === contextId && c.state =!= Failed && c.state =!= Stopped } yield c
    val contextStateUpdate = affectedContext map (x => (x.state, x.details)) update (newState, newDetails)
    Await.ready(db.run(contextStateUpdate), defaultDbTimeout)
  }

  def contextById(contextId: ID, db: Database): Future[Option[ContextEntity]] = {
    db.run(contexts.filter(c => c.id === contextId).result).map {
      case Seq(context) => Some(context)
      case _ => None
    }
  }

  def allContexts(db: Database): Future[Array[ContextEntity]] = {
    db.run(contexts.result).map(_.toArray)
  }

  def allInactiveContexts(db: Database): Future[Array[ContextEntity]] = {
    db.run(contexts.filter(c => c.state =!= Running).result).map(_.toArray)
  }
}
