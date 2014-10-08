# Environment and deploy file
# For use with bin/server_deploy, bin/server_package etc.
DEPLOY_HOSTS="hostname1.net
              hostname2.net"

APP_USER=spark
APP_GROUP=spark
INSTALL_DIR=/home/ec2-user/latest-mssh/spark-job-server
LOG_DIR=/home/ec2-user/latest-mssh/spark-job-server/logs
SPARK_HOME=/home/ec2-user/latest-mssh/spark-1.1.0
SPARK_CONF_HOME=$SPARK_HOME/conf
# Only needed for Mesos deploys
SPARK_EXECUTOR_URI=/home/ec2-user/latest-mssh/spark-1.1.0/lib/spark-assembly-1.1.0-hadoop2.3.0-mr1-cdh5.1.0.jar
