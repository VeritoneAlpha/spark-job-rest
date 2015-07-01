package com.job

import akka.actor.ActorSystem
import api.entities.JobState
import client.SparkJobRestClient

/**
 * Entry point for running [[S3DownloadJob]] on Spark-Job-REST
 */
object ExecuteDownload extends App {
  implicit val system = ActorSystem()
  val contextName = "downloadDataContext"

  try {
    val sjrc = new SparkJobRestClient("http://localhost:8097")

    val context = sjrc.createContext(contextName, Map("jars" -> "/home/ubuntu/s3-download-job.jar"))
    println(context)

    val bucketName="public-financial-transactions"
    val numPartitions=10
    val outputFolder="\"tachyon://localhost:19998/user/ubuntu/downloaded_data\""

    val job = sjrc.runJob("com.job.S3DownloadJob", contextName,
      Map("s3.bucket" -> bucketName,
        "num.partitions" -> String.valueOf(numPartitions),
        "fs.output" -> outputFolder
      ))
    println(job)

    var jobFinal = sjrc.getJob(job.jobId)
    while (jobFinal.status.equals(JobState.Running)) {
      Thread.sleep(1000)
      jobFinal = sjrc.getJob(job.jobId)
    }
    println(jobFinal)

    sjrc.deleteContext(contextName)
  } catch {
    case e:Exception =>
      e.printStackTrace()
  }

  system.shutdown()
}