package utils

import java.io.File

/**
 * Created by raduchilom on 22/03/15.
 */
object JarUtils {

  def validateJar(bytes: Array[Byte]): Boolean = {
    // For now just check the first few bytes are the ZIP signature: 0x04034b50 little endian
    if(bytes.size < 4 || bytes(0) != 0x50 || bytes(1) != 0x4b || bytes(2) != 0x03 || bytes(3) != 0x04){
      false
    } else {
      true
    }
  }

  def getPathForClasspath(path: String, jarFolder: String, contextName: String): String = {
    if(path.startsWith("/")){
      return path
    } else {
      var jarName = path
      if (path.startsWith("hdfs")) {
        FileUtils.createFolder(jarFolder + "tmp" + File.pathSeparator + contextName, true)
        HdfsUtils.copyJarFromHdfs(path, jarFolder)
        jarName = path.substring(path.lastIndexOf('\\'))
      }

      val file = new File(jarFolder + jarName)
      if (file.exists()) {
        return jarFolder + path
      }
    }

    throw new Exception(s"Jar $path  could not be resolved.")
  }


  def getJarPathForSpark(path: String, jarFolder: String): String = {
    if(path.startsWith("/")){
      return path
    } else if (path.startsWith("hdfs")) {
      return path
    } else {

      val file = new File(jarFolder + path)
      if (file.exists()) {
        return jarFolder + path
      }
    }

    throw new Exception(s"Jar $path  could not be resolved.")
  }

}
