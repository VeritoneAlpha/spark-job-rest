package persistence.services

import api.entities.JobDetails
import api.entities.JobState._
import api.types._
import com.typesafe.config.Config
import config.durations
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.LoggerFactory
import persistence.schema.ColumnTypeImplicits._
import persistence.schema._
import persistence.slickWrapper.Driver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

/**
 * Collection of methods for persisting job entities
 */
object JobPersistenceService {
  private val log = LoggerFactory.getLogger(getClass)

  private val dbTimeout = durations.db.timeout

  /**
   * Inserts new job to database
   * @param job job entity to persist
   * @param db database connection
   * @return future of affected columns
   */
  def insertJob(job: JobDetails, db: Database): Future[Int] = {
    log.info(s"Inserting job ${job.id}.")
    db.run(jobs += job)
  }

  /**
   * Synchronously updates status for job with specified id.
   * Does not replace [[Error]] or [[Finished]] statuses.
   *
   * @param id job's ID
   * @param newStatus job status to set
   * @param db database connection
   */
  def updateJobStatus(id: ID, newStatus: JobState, db: Database, newDetails: String = ""): Unit = {
    log.info(s"Updating job $id state to $newStatus with details: $newDetails")
    val affectedJob = for { j <- jobs if j.id === id && j.status =!= Error && j.status =!= Finished } yield j
    val jobUpdate = affectedJob map (x => (x.status, x.details)) update (newStatus, newDetails)
    Await.ready(db.run(jobUpdate), dbTimeout.duration)
  }

  /**
   * Synchronously persists to database that job has been started.
   * @param jobId job ID
   * @param contextName assigned context name
   * @param contextId context ID to assign to job
   * @param finalConfig final job config
   * @param db database connection
   */
  def persistJobStart(jobId: ID, contextName: String, contextId: ID, finalConfig: Config, db: Database): Unit = {
    log.info(s"Persisting job $jobId start on context $contextId.")
    val affectedJob = for { j <- jobs if j.id === jobId} yield j
    val newValues = (Some(contextName), Some(contextId), Some(finalConfig), Some(new DateTime(DateTimeZone.UTC).getMillis), Running)
    val jobUpdate = affectedJob map (j => (j.contextName, j.contextId, j.finalConfig, j.startTime, j.status)) update newValues
    Await.result(db.run(jobUpdate), dbTimeout.duration)
  }

  /**
   * Synchronously persists to database job result
   * @param id job ID
   * @param result job result
   * @param db database connection
   */
  def persistJobResult(id: ID, result: String, db: Database): Unit = {
    log.info(s"Persisting job $id finish.")
    val affectedJob = for { j <- jobs if j.id === id && j.status =!= Finished && j.status =!= Error} yield j
    val newValues = (Some(result), Finished, Some(new DateTime(DateTimeZone.UTC).getMillis))
    val jobUpdate = affectedJob map (j => (j.result, j.status, j.stopTime)) update newValues
    Await.ready(db.run(jobUpdate), dbTimeout.duration)
  }

  /**
   * Synchronously persists to database that job was failed
   * @param id job ID
   * @param reason failure reason
   * @param db database connection
   */
  def persistJobFailure(id: ID, reason: String, db: Database): Unit = {
    log.info(s"Persisting job $id failure.")
    val affectedJob = for { j <- jobs if j.id === id} yield j
    val newValues = (Some(reason), Error, Some(new DateTime(DateTimeZone.UTC).getMillis))
    val jobUpdate = affectedJob map (j => (j.result, j.status, j.stopTime)) update newValues
    Await.ready(db.run(jobUpdate), dbTimeout.duration)
  }

  /**
   * Asynchronously finds job by ID
   * @param jobId job ID
   * @param db database connection
   * @return found job future
   */
  def jobById(jobId: ID, db: Database): Future[Option[JobDetails]] = {
    db.run(jobs.filter(j => j.id === jobId).result).map {
      case Seq(job) => Some(job)
      case _ => None
    }
  }

  /**
   * Asynchronously returns all jobs
   * @param db database connection
   * @return future of all jobs
   */
  def allJobs(db: Database): Future[Array[JobDetails]] = {
    db.run(jobs.result).map(_.toArray)
  }
}
