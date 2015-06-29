package api

import java.util.UUID

/**
 * Common types used in all over Spark Job REST project
 */
package object types {
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
  def nextIdentifier: ID = UUID.randomUUID()
}
