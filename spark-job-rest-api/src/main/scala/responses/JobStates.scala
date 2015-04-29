package responses

/**
 * Created by raduchilom on 4/24/15.
 */

object JobStates {

  sealed abstract class JobState(val name: String) {
    override def toString = name
  }

  case object RUNNING extends JobState("Running")

  case object ERROR extends JobState("Error")

  case object FINISHED extends JobState("Finished")

}
