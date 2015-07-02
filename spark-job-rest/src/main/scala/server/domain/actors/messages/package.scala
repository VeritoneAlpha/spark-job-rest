package server.domain.actors

import persistence.slickWrapper.Driver.api._

/**
 * Namespace for common actor messages.
 * Actor specific messages should be defined in it's container object.
 * If at least 2 actors receive one message it SHOULD be moved here.
 */
package object messages {
  /**
   * Sent when actor is not initialized but requested for something
   */
  case object Uninitialized

  /**
   * This message is sent to actor to get it's initialization status
   */
  case object IsInitialized

  /**
   * When actor is initialized it returns this message in answer for [[IsInitialized]]
   */
  case object Initialized

  /**
   * This message should be sent to actor to initialize it.
   */
  case object Init

  /**
   * Requests database info from [[DatabaseServerActor]] and [[DatabaseConnectionActor]]
   */
  case object GetDatabaseInfo

  /**
   * Passes database info obtained by [[DatabaseServerActor]] and [[DatabaseConnectionActor]]
   * @param dbName database name
   * @param status server status
   * @param url server url
   * @param connectionString connection string
   */
  case class DatabaseInfo(dbName: String, status: String, url: String, connectionString: String)

  /**
   * Requests database connection from [[DatabaseServerActor]] and [[DatabaseConnectionActor]]
   */
  case object GetDatabaseConnection

  /**
   * Passes database connection obtained by [[DatabaseServerActor]] and [[DatabaseConnectionActor]]
   * @param db database connection
   */
  case class DatabaseConnection(@transient db: Database)

  /**
   * This message used for basic heartbeat negotiation and may be used both as request and response
   */
  case object IsAwake
}
