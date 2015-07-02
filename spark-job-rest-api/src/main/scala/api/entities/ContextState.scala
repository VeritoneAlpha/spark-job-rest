package api.entities


/**
 * States of Spark jobs.
 */
object ContextState extends Enumeration {
  type ContextState = Value
  val Requested = Value("Requested")
  val Queued = Value("Queued")
  val Running = Value("Running")
  val Terminated = Value("Terminated")
  val Failed = Value("Failed")
}
