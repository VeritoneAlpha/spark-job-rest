package persistence

/**
 * Use this abstract class to create enumerations which may be persisted by Slick.
 */
abstract class PersistentEnumeration extends Enumeration {
  import slickWrapper.Driver._
  import slickWrapper.Driver.api._

  implicit val enumMapper = MappedJdbcType.base[Value, String](_.toString, this.withName)
}
