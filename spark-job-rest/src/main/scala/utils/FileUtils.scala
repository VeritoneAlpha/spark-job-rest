package utils

import java.io.{File, FileOutputStream}
import org.apache.commons

/**
 * Created by raduchilom on 22/03/15.
 */
object FileUtils {

  def writeToFile(fileName: String, folderName: String, bytes: Array[Byte]): Unit = {
    val fos = new FileOutputStream(folderName + File.separator + fileName)
    fos.write(bytes)
    fos.close()
  }

  def createFolder(folder: String, overwrite: Boolean) = {
    val file = new File(folder)
    if(!file.exists()){
      file.mkdir()
    } else if (overwrite){
      commons.io.FileUtils.deleteDirectory(file)
      file.mkdir()
    }
  }

  def deleteFolder(folder: String): Unit = {
    val file = new File(folder)
    if(file.exists()){
      commons.io.FileUtils.deleteDirectory(file)
    }
  }
}
