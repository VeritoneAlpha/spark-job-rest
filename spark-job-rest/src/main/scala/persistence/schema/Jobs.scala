package persistence.schema

import api.entities.JobDetails
import com.typesafe.config.Config
import persistence.slickWrapper.Driver.api._
import api.types._
import api.entities.JobState._

/**
 * Jobs table stores information about jobs history
 * @param tag table tag name
 */
class Jobs(tag: Tag) extends Table[JobDetails] (tag, jobsTable) {
  import ColumnTypeImplicits._

  def id = column[ID]("JOB_ID", O.PrimaryKey)
  def contextId = column[ID]("CONTEXT_ID")
  def startTime = column[Option[Long]]("START_TIME")
  def stopTime = column[Option[Long]]("STOP_TIME")
  def runningClass = column[String]("RUNNING_CLASS")
  def submittedConfig = column[Config]("SUBMITTED_CONFIG", O.SqlType(configSQLType))
  def finalConfig = column[Option[Config]]("FINAL_CONFIG", O.SqlType(configSQLType))
  def status = column[JobState]("STATUS")
  def submitTime = column[Long]("SUBMIT_TIME")

  def * = (contextId.?, startTime, stopTime, runningClass, submittedConfig, finalConfig, status, submitTime, id) <> (JobDetails.tupled, JobDetails.unapply)

  def context = foreignKey("CONTEXT_FK", contextId, contexts)(
    _.id,
    onUpdate = ForeignKeyAction.Restrict,
    onDelete = ForeignKeyAction.Cascade
  )
}
