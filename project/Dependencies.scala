import sbt._
import Keys._

object Dependencies {
  val excludeCglib = ExclusionRule(organization = "org.sonatype.sisu.inject")
  val excludeJackson = ExclusionRule(organization = "org.codehaus.jackson")
  val excludeNetty = ExclusionRule(organization = "org.jboss.netty")
  val excludeAsm = ExclusionRule(organization = "asm")
  val excludeHadoop = ExclusionRule(organization = "org.apache.hadoop")
  val excludeMesos = ExclusionRule(organization = "org.apache.mesos")
  val excludeZookeper = ExclusionRule(organization = "org.apache.zookeeper")
  val excludeSlf4j = ExclusionRule(organization = "org.slf4j")
  val excludeIoNetty = ExclusionRule(organization = "io.netty")

  lazy val akkaDeps = Seq(
    // Akka is provided because Spark already includes it, and Spark's version is shaded so it's not safe
    // to use this one
    "io.netty" % "netty" % "3.6.6.Final",
    "com.typesafe.akka" %% "akka-slf4j" % "2.2.4" % "provided",
    "com.typesafe.akka" % "akka-remote_2.10" % "2.2.3",
    "io.spray" %% "spray-json" % "1.2.5",
    "io.spray" % "spray-can" % "1.2.0",
    "io.spray" % "spray-routing" % "1.2.0",
//    "org.apache.spark.atigeo" %% "spark-core" % "0.9.1" excludeAll(excludeIoNetty, excludeSlf4j, excludeZookeper, excludeMesos, excludeHadoop),
    "org.apache.spark.latch" %% "spark-core" % "0.9.1" excludeAll(excludeIoNetty, excludeSlf4j, excludeZookeper, excludeMesos, excludeHadoop),
    "org.apache.spark" %% "spark-core" % "0.9.1" excludeAll(excludeIoNetty, excludeSlf4j, excludeZookeper, excludeMesos, excludeHadoop),
    "org.slf4j" % "slf4j-api" % "1.7.2"

  )

  lazy val sparkDeps = Seq(
    // Force netty version.  This avoids some Spark netty dependency problem.
    "io.netty" % "netty" % "3.6.6.Final",
    "org.apache.hadoop" % "hadoop-hdfs" % "2.0.0-cdh4.6.0",
    "org.apache.hadoop" % "hadoop-client" % "2.0.0-mr1-cdh4.6.0",
    "org.apache.hadoop" % "hadoop-core" % "2.0.0-mr1-cdh4.6.0",
    "org.slf4j" % "slf4j-api" % "1.7.2",
    "org.slf4j" % "slf4j-log4j12" % "1.7.2",
//    "org.apache.spark.atigeo" %% "spark-core" % "0.9.1" excludeAll(excludeIoNetty, excludeSlf4j, excludeZookeper, excludeMesos, excludeHadoop),
    "org.apache.spark.latch" %% "spark-core" % "0.9.1" excludeAll(excludeIoNetty, excludeSlf4j, excludeZookeper, excludeMesos, excludeHadoop),
    "org.apache.spark" %% "spark-core" % "0.9.1" excludeAll(excludeIoNetty, excludeSlf4j, excludeZookeper, excludeMesos, excludeHadoop)
  )

  lazy val slickDeps = Seq(
    "com.typesafe.slick" %% "slick" % "2.0.2-RC1",
    "com.h2database" % "h2" % "1.3.170"
  )

  lazy val logbackDeps = Seq(
    "org.slf4j" % "slf4j-api" % "1.7.2",
    "org.slf4j" % "slf4j-log4j12" % "1.7.2",
    "log4j" % "log4j" % "1.2.16"
  )

  lazy val coreTestDeps = Seq(
    "org.scalatest" %% "scalatest" % "1.9.1" % "test",
    "com.typesafe.akka" %% "akka-testkit" % "2.2.4" % "test",
    "io.spray" % "spray-testkit" % "1.2.0" % "test"
  )

  lazy val commonDeps = Seq(
    "com.typesafe" % "config" % "1.0.0",
    "joda-time" % "joda-time" % "2.1",
    "org.joda" % "joda-convert" % "1.2",
    "com.yammer.metrics" % "metrics-core" % "2.2.0"
  )

  lazy val mesosDeps = Seq(
    "org.apache.mesos" % "mesos" % "0.16.0"
  )

  val repos = Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "spray repo" at "http://repo.spray.io",
    "cloudera-repo-releases" at "https://repository.cloudera.com/artifactory/repo/",
    "atigeo" at "http://nexus-2.life.atigeo.com:8080/nexus/content/repositories/atigeo/"
  )
}
