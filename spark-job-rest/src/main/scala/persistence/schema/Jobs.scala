package persistence.schema

import com.typesafe.config.Config
import org.joda.time._
import persistence.PersistentEnumeration
import persistence.slickWrapper.Driver.api._

object JobStatus extends PersistentEnumeration {
  type JobStatus = Value
  val Submitted = Value("submitted")
  val Queued = Value("queued")
  val Running = Value("running")
  val Finished = Value("finished")
  val Failed = Value("failed")
}

import persistence.schema.JobStatus._

/**
 * Job entity
 * @param contextId link to context
 * @param status job status
 * @param startTime job start timestamp
 * @param stopTime job stop timestamp
 * @param runningClass classpath to class where job should be submitted
 * @param submittedConfig job config submitted to job server
 * @param finalConfig config finally passed to job
 * @param submitTime timestamp when jab was submitted
 * @param id job ID
 */
case class Job(contextId: WEAK_LINK,
               startTime: Option[Long],
               stopTime: Option[Long],
               runningClass: String,
               submittedConfig: Config,
               finalConfig: Config,
               status: JobStatus = Submitted,
               submitTime: Long = new DateTime(DateTimeZone.UTC).getMillis,
               id: ID = nextId)

/**
 * Jobs table stores information about jobs history
 * @param tag table tag name
 */
class Jobs(tag: Tag) extends Table[Job] (tag, jobsTable) {
  import implicits._

  def id = column[ID]("JOB_ID", O.PrimaryKey)
  def contextId = column[ID]("CONTEXT_ID")
  def startTime = column[Option[Long]]("START_TIME")
  def stopTime = column[Option[Long]]("STOP_TIME")
  def runningClass = column[String]("RUNNING_CLASS")
  def submittedConfig = column[Config]("SUBMITTED_CONFIG", O.SqlType(configSQLType))
  def finalConfig = column[Config]("FINAL_CONFIG", O.SqlType(configSQLType))
  def status = column[JobStatus]("STATUS")
  def submitTime = column[Long]("SUBMIT_TIME")

  def * = (contextId.?, startTime, stopTime, runningClass, submittedConfig, finalConfig, status, submitTime, id) <> (Job.tupled, Job.unapply)

  def context = foreignKey("CONTEXT_FK", contextId, contexts)(
    _.id,
    onUpdate = ForeignKeyAction.Restrict,
    onDelete = ForeignKeyAction.Cascade
  )
}
