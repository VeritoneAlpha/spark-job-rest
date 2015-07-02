package persistence.schema

import api.entities.JobDetails
import api.entities.JobState._
import api.types._
import com.typesafe.config.Config
import persistence.slickWrapper.Driver.api._

/**
 * Jobs table stores information about jobs history
 * @param tag table tag name
 */
class Jobs(tag: Tag) extends Table[JobDetails] (tag, jobsTable) {
  import ColumnTypeImplicits._

  def id = column[ID]("JOB_ID", O.PrimaryKey)
  def runningClass = column[String]("RUNNING_CLASS")
  def submittedConfig = column[Config]("SUBMITTED_CONFIG", O.SqlType(configSQLType))
  def contextId = column[Option[ID]]("CONTEXT_ID")
  def startTime = column[Option[Long]]("START_TIME")
  def stopTime = column[Option[Long]]("STOP_TIME")
  def finalConfig = column[Option[Config]]("FINAL_CONFIG", O.SqlType(configSQLType))
  def status = column[JobState]("STATUS")
  def details = column[String]("VARCHAR(4096)")
  def submitTime = column[Long]("SUBMIT_TIME")
  def result = column[Option[String]]("RESULT", O.SqlType(resultSqlType))
  def contextName = column[Option[String]]("CONTEXT_NAME")

  def * = (runningClass, submittedConfig, contextId, startTime, stopTime, finalConfig, status, details, submitTime, result, contextName, id) <> (JobDetails.tupled, JobDetails.unapply)

  def context = foreignKey("CONTEXT_FK", contextId, contexts)(
    _.id,
    onUpdate = ForeignKeyAction.Restrict,
    onDelete = ForeignKeyAction.Cascade
  )
}
