#!/bin/bash

echo -e "********************************* Started cleaning up **************************************************"
rm -r nohup.out
#rm -rf /tmp/spark-job-rest/filedao/*
echo -e "********************************* Finished cleaning up ***************************************************"

echo -e "********************************* starting Spark-job-rest ********************************************************"

nohup java -Djava.library.path="/full_path_to_libmesos.so"  -XX:MaxPermSize=2048M -jar ../job-server/target/spark-job-rest.jar /full_path_to_project/config/application.conf &

echo -e "************************************** please wait *****************************************************************"

# The next part creates default-context. Remove if default-context creation is not needed.
sleep 8

echo -e "********************************* Registering context : default-context ********************************************************"

curl -d "" 'localhost:8090/contexts/default-context?memory-per-node=4g&spark.mesos.coarse=false&spark.ui.port=16000'

echo -e "********************************* Registered context : default-context ********************************************************"

echo -e "********************************************* Spark-job-rest started ***********************************************"

tail -f nohup.out
