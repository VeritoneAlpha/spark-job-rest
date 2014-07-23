package spark.jobserver;

import com.typesafe.config.Config;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import spark.jobserver.SparkJobValid$;


public class JavaExample implements SparkJob{
    @Override
    public Object runJob(SparkContext sc, Config jobConfig) {
        JavaSparkContext javaSC = new JavaSparkContext(sc);
        //Adds a JAR dependency for all tasks to be executed on this SparkContext in the future.
// The path passed can be either a local file, a file in HDFS (or other Hadoop-supported filesystems), or an HTTP, HTTPS or FTP URI.
//        javaSC.addJar();


        //do stuff
        System.out.println("Running job!");

        //return results
        return null;
    }

    @Override
    public SparkJobValidation validate(SparkContext sc, Config config) {
        boolean flag = true;
        System.out.println("Validating job");
        //do validation

        if(flag){
            return new SparkJobValid();
        } else {
            return new SparkJobInvalid("");
        }
    }
}

