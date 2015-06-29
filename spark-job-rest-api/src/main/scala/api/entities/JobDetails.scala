package api.entities

import api.types._
import com.typesafe.config.Config
import org.joda.time._
import JobState._

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
case class JobDetails(contextId: WEAK_LINK,
               startTime: Option[Long],
               stopTime: Option[Long],
               runningClass: String,
               submittedConfig: Config,
               finalConfig: Option[Config],
               status: JobState = Submitted,
               submitTime: Long = new DateTime(DateTimeZone.UTC).getMillis,
               id: ID = nextIdentifier)
