import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object Assembly {
  lazy val settings = assemblySettings ++ Seq(
    test in assembly := {},
    jarName in assembly := "spark-job-rest.jar",
    excludedJars in assembly <<= (fullClasspath in assembly) map { _ filter { cp =>
      List("servlet-api", "guice-all", "junit", "uuid", "jsp-api-2.0", "antlr", "avro",
        "scala-actors", "stax-api", "mockito").exists(cp.data.getName.startsWith(_))
    } },
    assembleArtifact in packageScala := true,   // We don't need the Scala library, Spark already includes it
    mergeStrategy in assembly := {
      case m if m.toLowerCase.equalsIgnoreCase("META-INF/services/org.apache.hadoop.fs.FileSystem") => MergeStrategy.concat
      case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
      case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
//      case "reference.conf" => MergeStrategy.concat
      case _ => MergeStrategy.first
    }
  )

  lazy val settingsManagerHelper = assemblySettings ++ Seq(
    jarName in assembly := "job-manager-helper.jar",
    excludedJars in assembly <<= (fullClasspath in assembly) map { _ filter { cp =>
      List("servlet-api", "guice-all", "junit", "uuid",
        "jetty", "jsp-api-2.0", "antlr", "avro",
        "scala-actors", "spark", "commons-cli", "stax-api", "mockito", "hdfs", "hadoop").exists(cp.data.getName.startsWith(_))
    } },
    assembleArtifact in packageScala := false,   // We don't need the Scala library, Spark already includes it
    mergeStrategy in assembly := {
      case m if m.toLowerCase.equalsIgnoreCase("META-INF/services/org.apache.hadoop.fs.FileSystem") => MergeStrategy.concat
      case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
      case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
      case "reference.conf" => MergeStrategy.concat
      case _ => MergeStrategy.first
    }
  )

  lazy val settingsAkka = assemblySettings ++ Seq(
    jarName in assembly := "akka.jar",
    excludedJars in assembly <<= (fullClasspath in assembly) map { _ filter { cp =>
      List("servlet-api", "guice-all", "junit", "uuid",
        "jetty", "jsp-api-2.0", "antlr", "avro",
         "spark", "commons-cli", "stax-api", "mockito", "hdfs", "hadoop", "mesos").exists(cp.data.getName.startsWith(_))
    } },
    assembleArtifact in packageScala := true,   // We don't need the Scala library, Spark already includes it
    mergeStrategy in assembly := {
      case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
      case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
      case "reference.conf" => MergeStrategy.concat
      case _ => MergeStrategy.first
    }
  )
}
