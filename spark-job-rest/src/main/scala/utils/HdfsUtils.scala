package utils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{Path, FileSystem}

/**
 * Created by raduchilom on 22/03/15.
 */
object HdfsUtils {

  def copyJarFromHdfs(hdfsPath: String, outputFolder: String) = {

    //    if(!config.hasPath("hdfs.namenode")){
    //      println("ERROR: HDFS NameNode is not set in application.conf!")
    //      throw new Exception("HDFS NameNode is not set in application.conf!")
    //    }

    val conf = new Configuration()
    //    conf.set("fs.defaultFS", getValueFromConfig(config, "hdfs.namenode", ""))
    conf.set("fs.defaultFS", hdfsPath)
    val hdfsFileSystem = FileSystem.get(conf)

    hdfsFileSystem.copyToLocalFile(new Path(hdfsPath), new Path(outputFolder))
  }

}
