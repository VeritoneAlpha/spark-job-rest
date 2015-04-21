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
    val diskPath =
      if(path.startsWith("/")){
        path
      } else if (path.startsWith("hdfs")){
        val tempFolder = jarFolder + "tmp" + File.pathSeparator + contextName
        FileUtils.createFolder(tempFolder, true)
        HdfsUtils.copyJarFromHdfs(path, tempFolder)
        tempFolder + File.pathSeparator + getJarName(path)
      } else {
        jarFolder + getJarName(path)
      }

    val diskFile = new File(diskPath)
    if (diskFile.exists()) {
      return diskPath
    }

    throw new Exception(s"Jar $path not found.")
  }


  def getJarName(path: String): String = {
    if(path.contains('\\')) {
      path.substring(path.lastIndexOf('\\'))
    } else {
      path
    }
  }

  def getJarPathForSpark(path: String, jarFolder: String): String = {
    if(path.startsWith("hdfs")){
      //TODO: perform hdfs validation
      return path
    } else {
      val diskPath =
        if(path.startsWith("/")){
          path
        } else {
          jarFolder + getJarName(path)
        }
      val diskFile = new File(diskPath)
      if (diskFile.exists()) {
        return diskPath
      }
    }

    throw new Exception(s"Jar $path not found.")
  }

}
