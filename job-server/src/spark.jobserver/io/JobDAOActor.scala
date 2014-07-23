package spark.jobserver.io

import akka.actor.Actor
import com.typesafe.config._
import java.io._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import scala.collection.mutable
import scala.Some

  case class SaveJar(appName: String, uploadTime: DateTime, jarBytes: Array[Byte])
  case class GetApps()
  case class RetrieveJarFile(appName: String, uploadTime: DateTime)
  case class SaveJobInfo(jobInfo: JobInfo)
  case class GetJobInfos()
  case class SaveJobConfig(jobId: String, jobConfig: Config)
  case class GetJobConfigs()
  case class GetLastUploadTime(appName: String)


  class JobDAOActor(config: Config) extends Actor with JobDAO{
  private val logger = LoggerFactory.getLogger(getClass)

  // appName to its set of upload times. Decreasing times in the seq.
  private val apps = mutable.HashMap.empty[String, Seq[DateTime]]
  // jobId to its JobInfo
  private val jobs = mutable.HashMap.empty[String, JobInfo]
  // jobId to its Config
  private val configs = mutable.HashMap.empty[String, Config]

  private val rootDir = getOrElse(config.getString("spark.jobserver.filedao.rootdir"),
    "/tmp/spark-jobserver/filedao/data")
  private val rootDirFile = new File(rootDir)
  logger.info("rootDir is " + rootDirFile.getAbsolutePath)

  private val jarsFile = new File(rootDirFile, "jars.data")
  private var jarsOutputStream: DataOutputStream = null
  private val jobsFile = new File(rootDirFile, "jobs.data")
  private var jobsOutputStream: DataOutputStream = null
  private val jobConfigsFile = new File(rootDirFile, "configs.data")
  private var jobConfigsOutputStream: DataOutputStream = null

  init()

  private def init() {
    // create the date directory if it doesn't exist
    if (!rootDirFile.exists()) {
      if (!rootDirFile.mkdirs()) {
        throw new RuntimeException("Could not create directory " + rootDir)
      }
    }

    // read back all apps info during startup
    if (jarsFile.exists()) {
      val in = new DataInputStream(new BufferedInputStream(new FileInputStream(jarsFile)))
      try {
        while (true) {
          val jarInfo = readJarInfo(in)
          addJar(jarInfo.appName, jarInfo.uploadTime)
        }
      } catch {
        case e: EOFException => // do nothing

      } finally {
        in.close()
      }
    }

    // read back all jobs info during startup
    if (jobsFile.exists()) {
      val in = new DataInputStream(new BufferedInputStream(new FileInputStream(jobsFile)))
      try {
        while (true) {
          val jobInfo = readJobInfo(in)
          jobs(jobInfo.jobId) = jobInfo
        }
      } catch {
        case eof: EOFException => // do nothing
        case e: Exception      => throw e

      } finally {
        in.close()
      }
    }

    // read back all job configs during startup
    if (jobConfigsFile.exists()) {
      val in = new DataInputStream(new BufferedInputStream(new FileInputStream(jobConfigsFile)))
      try {
        while (true) {
          val (jobId, jobConfig) = readJobConfig(in)
          configs(jobId) = jobConfig
        }
      } catch {
        case eof: EOFException => // do nothing
      } finally {
        in.close()
      }
    }

    // Don't buffer the stream. I want the apps meta data log directly into the file.
    // Otherwise, server crash will lose the buffer data.
    jarsOutputStream = new DataOutputStream(new FileOutputStream(jarsFile, true))
    jobsOutputStream = new DataOutputStream(new FileOutputStream(jobsFile, true))
    jobConfigsOutputStream = new DataOutputStream(new FileOutputStream(jobConfigsFile, true))
  }

  def saveJar(appName: String, uploadTime: DateTime, jarBytes: Array[Byte]) {
    // The order is important. Save the jar file first and then log it into jobsFile.
    val outFile = new File(rootDir, createJarName(appName, uploadTime) + ".jar")
    val bos = new BufferedOutputStream(new FileOutputStream(outFile))
    try {
      logger.debug("Writing {} bytes to file {}", jarBytes.size, outFile.getPath)
      bos.write(jarBytes)
      bos.flush()
    } finally {
      bos.close()
    }

    // log it into jobsFile
    writeJarInfo(jarsOutputStream, JarInfo(appName, uploadTime))

    // track the new jar in memory
    addJar(appName, uploadTime)
  }

    private def writeJarInfo(out: DataOutputStream, jarInfo: JarInfo) {
    out.writeUTF(jarInfo.appName)
    out.writeLong(jarInfo.uploadTime.getMillis)
  }

  private def readJarInfo(in: DataInputStream) = JarInfo(in.readUTF, new DateTime(in.readLong))

  private def addJar(appName: String, uploadTime: DateTime) {
    if (apps.contains(appName)) {
      apps(appName) = uploadTime +: apps(appName) // latest time comes first
    } else {
      apps(appName) = Seq(uploadTime)
    }
  }

  def getApps: Map[String, DateTime] = apps.map {
    case (appName, uploadTimes) =>
      appName -> uploadTimes.head
    }.toMap

  def retrieveJarFile(appName: String, uploadTime: DateTime): String =
    new File(rootDir, createJarName(appName, uploadTime) + ".jar").getAbsolutePath

  private def createJarName(appName: String, uploadTime: DateTime): String = appName + "-" + uploadTime

  def saveJobInfo(jobInfo: JobInfo) {
    writeJobInfo(jobsOutputStream, jobInfo)
    jobs(jobInfo.jobId) = jobInfo
  }

  private def writeJobInfo(out: DataOutputStream, jobInfo: JobInfo) {
    out.writeUTF(jobInfo.jobId)
    out.writeUTF(jobInfo.contextName)
    writeJarInfo(out, jobInfo.jarInfo)
    out.writeUTF(jobInfo.classPath)
    out.writeLong(jobInfo.startTime.getMillis)
    val time = if (jobInfo.endTime.isEmpty) jobInfo.startTime.getMillis else jobInfo.endTime.get.getMillis
    out.writeLong(time)
    val errorStr = if (jobInfo.error.isEmpty) "" else jobInfo.error.get.toString
    out.writeUTF(errorStr)
  }

  private def readError(in: DataInputStream) = {
    val error = in.readUTF()
    if (error == "") None else Some(new Throwable(error))
  }

  private def readJobInfo(in: DataInputStream) = JobInfo(
    in.readUTF,
    in.readUTF,
    readJarInfo(in),
    in.readUTF,
    new DateTime(in.readLong),
    Some(new DateTime(in.readLong)),
    readError(in))

  def getJobInfos: Map[String, JobInfo] = jobs.toMap

  def saveJobConfig(jobId: String, jobConfig: Config) {
    writeJobConfig(jobConfigsOutputStream, jobId, jobConfig)
    configs(jobId) = jobConfig
  }

  def getJobConfigs: Map[String, Config] = configs.toMap

  private def writeJobConfig(out: DataOutputStream, jobId: String, jobConfig: Config) {
    out.writeUTF(jobId)
    out.writeUTF(jobConfig.root().render(ConfigRenderOptions.concise()))
  }

  private def readJobConfig(in: DataInputStream): (String, Config) = (
    in.readUTF,
    ConfigFactory.parseString(in.readUTF)
  )

  def receive = {
    case param : SaveJar => {
      saveJar(param.appName, param.uploadTime, param.jarBytes)
    }
    case param : GetApps => {
      sender ! getApps
    }
    case param : RetrieveJarFile => {
      sender ! retrieveJarFile(param.appName, param.uploadTime)
    }
    case param : SaveJobInfo => {
      saveJobInfo(param.jobInfo)
    }
    case param : GetJobInfos => {
      sender ! getJobInfos
    }
    case param : SaveJobConfig => {
      saveJobConfig(param.jobId, param.jobConfig)
    }
    case param : GetJobConfigs => {
      sender ! getJobConfigs
    }
    case param : GetLastUploadTime => {
      sender ! getLastUploadTime(param.appName)
    }
    case _ => {
      println("Other TYPE !")
    }
  }

}
