package api.entities

import api.entities.JobState._
import api.types._
import com.typesafe.config.Config
import org.joda.time._

/**
 * Job entity
 * @param contextId link to context
 * @param status job status
 * @param startTime job start timestamp
 * @param stopTime job stop timestamp
 * @param runningClass classpath to class where job should be submitted
 * @param submittedConfig job config submitted to job server
 * @param finalConfig config finally passed to job
 * @param details detailed information about job state
 * @param submitTime timestamp when jab was submitted
 * @param id job ID
 */
case class JobDetails(runningClass: String,
                      submittedConfig: Config,
                      contextId: Option[ID] = None,
                      startTime: Option[Long] = None,
                      stopTime: Option[Long] = None,
                      finalConfig: Option[Config] = None,
                      status: JobState = Submitted,
                      details: String = "",
                      submitTime: Long = new DateTime(DateTimeZone.UTC).getMillis,
                      result: Option[String] = None,
                      contextName: Option[String] = None,
                      id: ID = nextIdentifier)
