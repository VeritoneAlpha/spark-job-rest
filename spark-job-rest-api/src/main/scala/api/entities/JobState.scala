package api.entities


/**
 * States of Spark jobs.
 */
object JobState extends Enumeration {
  type JobState = Value
  val Submitted = Value("Submitted")
  val Queued = Value("Queued")
  val Running = Value("Running")
  val Error = Value("Failed")
  val Finished = Value("Finished")
}