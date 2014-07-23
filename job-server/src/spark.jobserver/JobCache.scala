package spark.jobserver

import akka.actor.ActorSelection
import akka.util.Timeout
import java.net.URL
import org.apache.spark.{SparkContext}
import org.joda.time.DateTime
import spark.jobserver.io.{RetrieveJarFile, GetApps}
import spark.jobserver.util.{ContextURLClassLoader, LRUCache}
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import akka.pattern.ask

case class JobJarInfo(constructor: () => SparkJob,
                      className: String,
                      jarFilePath: String)

/**
 * A cache for SparkJob classes.  A lot of times jobs are run repeatedly, and especially for low-latency
 * jobs, why retrieve the jar and load it every single time?
 */
class JobCache(maxEntries: Int, daoRef: ActorSelection, sparkContext: SparkContext, loader: ContextURLClassLoader) {
  private val cache = new LRUCache[(String, DateTime, String), JobJarInfo](maxEntries)
  implicit val timeout = Timeout(5 , TimeUnit.SECONDS)

  /**
   * Retrieves the given SparkJob class from the cache if it's there, otherwise use the DAO to retrieve it.
   * @param appName the appName under which the jar was uploaded
   * @param uploadTime the upload time for the version of the jar wanted
   * @param classPath the fully qualified name of the class/object to load
   */
  def getSparkJob(appName: String, uploadTime: DateTime, classPath: String): JobJarInfo = {
    cache.get((appName, uploadTime, classPath), {


      val future = daoRef ? RetrieveJarFile(appName, uploadTime)
      val result = Await.result(future, timeout.duration).asInstanceOf[String]

      val jarFilePath = new java.io.File(result).getAbsolutePath()
      sparkContext.addJar(jarFilePath)   // Adds jar for remote executors
      loader.addURL(new URL("file:" + jarFilePath))   // Now jar added for local loader
      val constructor = JarUtils.loadClassOrObject[SparkJob](classPath, loader)
      JobJarInfo(constructor, classPath, jarFilePath)
    })
  }
}
