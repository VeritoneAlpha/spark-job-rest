package api

import org.apache.spark.SparkContext

trait ContextLike {
  /**
   * Type of the context for representation
   */
  val contextClass: String

  override def toString: String = {
    super.toString + s"($contextClass)"
  }

  /**
   * Underlying Spark context
   * @return
   */
  def sparkContext: SparkContext

  /**
   * Validates whether job is valid for this context
   * @param job job to validate
   * @return
   */
  def validateJob(job: SparkJobBase): SparkJobValidation =
    if (isValidJob(job))
      SparkJobValid
    else
      SparkJobInvalid(s"Job ${job.toString} doesn't match context $this.")

  /**
   * Validates whether job is valid for this context
   * Should be implemented in concrete classes.
   * @param job job to validate
   * @return
   */
  def isValidJob(job: SparkJobBase): Boolean

  /**
   * This method should be called during cleanup
   */
  def stop()
}
