#!/bin/bash

appdir="$(dirname "$0")"

if [ -f "$appdir/server.pid" ]; then
    pid="$(cat "$appdir/server.pid")"
    proc="$(ps axu | grep "$pid" | grep spark-job-rest.jar | awk '{print $2}')"
    if [ -n "$proc" ]; then
        echo "Killing pid $proc"
        kill -9 $proc
    else
        echo "Pid $pid does not exist or it's not for spark-job-rest."
    fi
else
echo "Pid file $appdir/server.pid was not found"
fi

