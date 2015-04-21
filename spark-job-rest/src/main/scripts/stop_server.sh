#!/bin/bash

kill -9  $(ps aux | grep spark-job-rest.jar | awk '{print $2}')
