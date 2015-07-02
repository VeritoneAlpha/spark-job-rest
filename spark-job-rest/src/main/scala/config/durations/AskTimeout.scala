package config.durations

/**
 * Injects default timeout
 */
trait AskTimeout {
  implicit val timeout = ask.timeout
}
