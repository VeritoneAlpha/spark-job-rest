#!/bin/bash

echo "****************** stopping spark-job-rest *********************"

pid=`ps -eo pid,args | grep "spark-job-rest.jar" | \

grep -v grep | cut -c1-6`

#do what I need with the pi

echo $pid

kill -9 $pid

echo "****************** stopped spark-job-rest *********************"

