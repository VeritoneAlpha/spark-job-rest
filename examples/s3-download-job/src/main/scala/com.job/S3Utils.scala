package com.job

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{S3ObjectInputStream, GetObjectRequest, ObjectListing, ListObjectsRequest}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FSDataOutputStream, Path, FileSystem}
import org.apache.hadoop.io.IOUtils
import org.apache.spark.SparkContext
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Try}

/**
 * Created by raduchilom on 4/18/15.
 */
object S3Utils {

  val log = LoggerFactory.getLogger(getClass)

  def getFiles(bucketName: String):List[(Int, String)] = {

    val s3Client: AmazonS3Client = getS3Client()

    val fileList = ListBuffer[(Int, String)]()

    try {
      log.info("Listing objects from S3")
      var counter = 0

      val listObjectsRequest = new ListObjectsRequest()
        .withBucketName(bucketName)
      var objectListing: ObjectListing = null

      do {
        import scala.collection.JavaConversions._
        objectListing = s3Client.listObjects(listObjectsRequest)
        objectListing.getObjectSummaries.foreach { objectSummary =>
          if(!objectSummary.getKey.endsWith(Path.SEPARATOR)) {
            fileList += Tuple2(counter, objectSummary.getKey)
            counter += 1
          }
        }
        listObjectsRequest.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated())

      log.info("Finished listing objects from S3")

    } catch {
      case e: Exception => {
        log.error("Failed listing files. ", e)
        throw e
      }
    }

    fileList.toList
  }

  def getFilesDistributed(bucketName: String, sc: SparkContext, numPartitions: Int) = {
    val s3Client: AmazonS3Client = getS3Client()

    val fileList = ListBuffer[String]()
    val folderList = ListBuffer[String]()

    val listObjectsRequest = new ListObjectsRequest()
      .withBucketName(bucketName)
      .withPrefix("")
      .withDelimiter("/")

    var objectListing: ObjectListing = null

    do {
      import scala.collection.JavaConversions._
      objectListing = s3Client.listObjects(listObjectsRequest)
      folderList ++= objectListing.getCommonPrefixes
      objectListing.getObjectSummaries.foreach { objectSummary =>
        if(!objectSummary.getKey.endsWith(Path.SEPARATOR)) {
          fileList += objectSummary.getKey
        }
      }
      listObjectsRequest.setMarker(objectListing.getNextMarker());
    } while (objectListing.isTruncated())

    val folderRdd = sc.parallelize(folderList.toList, folderList.size)
    val filesRdd1 = folderRdd.flatMap{ folder =>
      getFilesFromFolder(bucketName, folder)
    }
    filesRdd1.cache()
    filesRdd1.count()

    val filesRdd2 = sc.parallelize(fileList, numPartitions)
    val filesRdd = filesRdd1.union(filesRdd2)

    filesRdd.zipWithIndex().map {
      case (value, index) => (index, value)
    }
  }

  def getFilesFromFolder(bucketName: String, folderKey: String):List[String] = {

    val s3Client: AmazonS3Client = getS3Client()

    val fileList = ListBuffer[String]()

    try {
      log.info("Listing objects from S3")
      var counter = 0

      val listObjectsRequest = new ListObjectsRequest()
        .withBucketName(bucketName)
        .withPrefix(folderKey)
      var objectListing: ObjectListing = null

      do {
        import scala.collection.JavaConversions._
        objectListing = s3Client.listObjects(listObjectsRequest)
        objectListing.getObjectSummaries.foreach { objectSummary =>
          if(!objectSummary.getKey.endsWith(Path.SEPARATOR)) {
            fileList += objectSummary.getKey
            counter += 1
          }
        }
        listObjectsRequest.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated())

      log.info(s"Finished listing objects for folder $folderKey")

    } catch {
      case e: Exception => {
        log.error("Failed listing files. ", e)
        throw e
      }
    }

    fileList.toList
  }

  def getS3Client(): AmazonS3Client = {
//    val awsCreds = new ProfileCredentialsProvider()
//    val s3client = new AmazonS3Client(awsCreds)
val s3client = new AmazonS3Client()
//    s3client.setRegion(Region.getRegion(Regions.EU_WEST_1))
    s3client
  }

  def downloadFile(bucketName: String, key: String, outputFolder: String, s3Client: AmazonS3Client): (Try[Any], String) = {

    val downloadTry = Try {

      var inputStream: S3ObjectInputStream = null
      var outputStream: FSDataOutputStream = null

      try {

        log.info(s"Downloading file: $key")
        val s3object = s3Client.getObject(new GetObjectRequest(bucketName, key))
        inputStream = s3object.getObjectContent

        val outputPath = outputFolder + Path.SEPARATOR + key
        log.info(s"Writing file to: $outputPath")

        val conf = new Configuration();
        conf.set("fs.defaultFS", outputFolder)
        conf.set("fs.tachyon.impl","tachyon.hadoop.TFS")
        // new instance & set file in configuration
        val fs = FileSystem.get(conf);
        outputStream = fs.create(new Path(outputPath));

        IOUtils.copyBytes(inputStream, outputStream, 8192)

        fs.getFileStatus(new Path(outputPath)).getLen

      } finally {
        if (inputStream != null) {
          inputStream.close()
        }
        if (outputStream != null) {
          outputStream.close()
        }
      }
    }

    downloadTry match {
      case Failure(e:Throwable) => log.error("Error: ", e)
      case _ =>
    }

    (downloadTry, key)
  }


}
