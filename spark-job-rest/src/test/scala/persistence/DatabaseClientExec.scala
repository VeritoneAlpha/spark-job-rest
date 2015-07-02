package persistence

import persistence.slickWrapper.Driver.api._
import slick.jdbc.meta.MTable
import test.durations.dbTimeout

import scala.concurrent.Await
import scala.util.Try

/**
 * Entry point for test application that connects to database specified in app parameters and lists tables.
 */
object DatabaseClientExec {
  implicit val timeout = dbTimeout

  def main (args: Array[String]) {
    val connectionString = Try { args.head }.getOrElse("jdbc:h2:tcp://localhost:9092/./spark-job-rest-db")
    val db = Database.forURL(connectionString)
    val tables = Await.result(db.run(MTable.getTables), timeout.duration)
    print(s"Found ${tables.size} tables in database: $tables")
  }
}
