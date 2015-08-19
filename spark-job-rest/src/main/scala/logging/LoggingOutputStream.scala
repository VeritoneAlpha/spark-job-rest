package logging

import org.apache.log4j.Level
import org.apache.log4j.Logger
import java.io.{PrintStream, IOException, OutputStream}

/**
 * Created by raduchilom on 4/27/15.
 */
object LoggingOutputStream {
  /**
   * Default number of bytes in the buffer.
   */
  private val DEFAULT_BUFFER_LENGTH: Int = 256

  val log = Logger.getLogger(getClass)

  def redirectConsoleOutput = {
    System.setErr(new PrintStream(new LoggingOutputStream(log, Level.ERROR)));
    System.setOut(new PrintStream(new LoggingOutputStream(log, Level.INFO)));
  }
}

class LoggingOutputStream extends OutputStream {
  /**
   * Indicates stream state.
   */
  private var hasBeenClosed: Boolean = false
  /**
   * Internal buffer where data is stored.
   */
  private var buf: Array[Byte] = null
  /**
   * The number of valid bytes in the buffer.
   */
  private var count: Int = 0
  /**
   * Remembers the size of the buffer.
   */
  private var curBufLength: Int = 0
  /**
   * The logger to write to.
   */
  private var log: Logger = null
  /**
   * The log level.
   */
  private var level: Level = null

  /**
   * Creates the Logging instance to flush to the given logger.
   *
   * @param log         the Logger to write to
   * @param level       the log level
   * @throws IllegalArgumentException in case if one of arguments
   *                                  is  null.
   */
  @throws(classOf[IllegalArgumentException])
  def this(log: Logger, level: Level) {
    this()
    if (log == null || level == null) {
      throw new IllegalArgumentException("Logger or log level must be not null")
    }
    this.log = log
    this.level = level
    curBufLength = LoggingOutputStream.DEFAULT_BUFFER_LENGTH
    buf = new Array[Byte](curBufLength)
    count = 0
  }

  /**
   * Writes the specified byte to this output stream.
   *
   * @param b the byte to write
   * @throws IOException if an I/O error occurs.
   */
  @throws(classOf[IOException])
  def write(b: Int) {
    if (hasBeenClosed) {
      throw new IOException("The stream has been closed.")
    }
    if (b == 0) {
      return
    }
    if (count == curBufLength) {
      flush
    }
    buf(count) = b.toByte
    count += 1
  }

  /**
   * Flushes this output stream and forces any buffered output
   * bytes to be written out.
   */
  override def flush {
    if (count == 0) {
      return
    }
    val bytes: Array[Byte] = new Array[Byte](count)
    System.arraycopy(buf, 0, bytes, 0, count)
    val str: String = new String(bytes)
    log.log(level, str)
    count = 0
  }

  /**
   * Closes this output stream and releases any system resources
   * associated with this stream.
   */
  override def close {
    flush
    hasBeenClosed = true
  }
}