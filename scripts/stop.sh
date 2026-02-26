#!/bin/bash
PROC=analyzer-1*.jar

PID=`ps -ef |grep -v grep |grep $PROC |awk '{print $2}'`
if [ "$PID" ]; then
    echo "Stop process ${PID}..."
    kill -9 $PID
    echo "Done."
fi