import sbt._
import Keys._

object Dependencies {
  val excludeCglib = ExclusionRule(organization = "org.sonatype.sisu.inject")
  val excludeJackson = ExclusionRule(organization = "org.codehaus.jackson")
  val excludeNetty = ExclusionRule(organization = "org.jboss.netty")
  val excludeAsm = ExclusionRule(organization = "asm")

  val excludeHadoop = ExclusionRule(organization = "org.apache.hadoop")
  val excludeMesos = ExclusionRule(organization = "org.apache.mesos")
  val excludeSlf4j = ExclusionRule(organization = "org.slf4j")
  val excludeIoNetty = ExclusionRule(organization = "io.netty")
  val excludeCurator = ExclusionRule(organization = "org.apache.curator")
  val excludePowermock = ExclusionRule(organization = "org.powermock")
  val excludeEclipseJetty = ExclusionRule(organization = "org.eclipse.jetty")

  val HADOOP_HDFS_VERSION = "2.3.0-cdh5.1.0"
  val HADOOP_CLIENT_VERSION = "2.3.0-mr1-cdh5.1.0"
  val TACHYON_VERSION = "0.5.0"
  val MESOS_VERSION ="0.19.0"

  lazy val akkaDeps = Seq(
    // Akka is provided because Spark already includes it, and Spark's version is shaded so it's not safe
    // to use this one
    "com.typesafe.akka" %% "akka-slf4j" % "2.2.4" % "provided",
    "io.spray" %% "spray-json" % "1.2.5",
    "io.spray" % "spray-can" % "1.2.0",
    "io.spray" % "spray-routing" % "1.2.0"
  )

  lazy val sparkDeps = Seq(
    "org.apache.httpcomponents" % "httpclient" % "4.3.5",
    "org.apache.httpcomponents" % "httpcore" % "4.3.2",
    "org.apache.spark" % "spark-core_2.10" % "1.0.2" excludeAll(excludeIoNetty,excludeSlf4j,excludeMesos,excludeHadoop),
    "org.tachyonproject" % "tachyon" % TACHYON_VERSION excludeAll(excludeHadoop, excludeCurator, excludeEclipseJetty, excludePowermock),
    "org.apache.hadoop" % "hadoop-hdfs" % HADOOP_HDFS_VERSION,
    "org.apache.hadoop" % "hadoop-client" % HADOOP_CLIENT_VERSION,
    "org.apache.hadoop" % "hadoop-core" % HADOOP_CLIENT_VERSION,
    "org.apache.mesos" % "mesos" % MESOS_VERSION classifier("shaded-protobuf") exclude("com.google.protobuf", "protobuf-java"),
	
    // Force netty version.  This avoids some Spark netty dependency problem.
    "io.netty" % "netty" % "3.6.6.Final"
  )

  lazy val slickDeps = Seq(
    "com.typesafe.slick" %% "slick" % "2.0.2-RC1",
    "com.h2database" % "h2" % "1.3.170",
    "mysql" % "mysql-connector-java" % "5.1.31"
  )

  lazy val logbackDeps = Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.7"
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

  val repos = Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "spray repo" at "http://repo.spray.io",
    "cloudera-repo-releases" at "https://repository.cloudera.com/artifactory/repo/"
  )
}
