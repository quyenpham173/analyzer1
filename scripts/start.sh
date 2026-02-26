BIN_PATH="$( cd "$(dirname "$0")" ; pwd -P )"
cd $BIN_PATH
echo "Current directory: $(pwd)"

# Run start script
PROC=analyzer-1

ENV_FILE=${BIN_PATH}/config/application.yml
LOG_CONFIG=${BIN_PATH}/config/logback.xml

PID=`ps -ef |grep -v grep |grep ${PROC}*.jar |awk '{print $2}'`
if [ "$PID" ]; then
    echo "Stop process ${PID}..."
    kill -9 $PID
    echo "Done."
fi

EXECJAR=$(ls "${BIN_PATH}"/${PROC}*.jar |sort -r |head -n1)
echo $EXECJAR
echo "Start Analyzer (1) Application ..."
java -Dspring.config.location=$ENV_FILE -Dspring.profiles.active=release -Dlogging.config=$LOG_CONFIG -jar $EXECJAR > /dev/null 2>&1 &
RUNNING_PID=`ps -ef |grep -v grep |grep ${PROC}*.jar |awk '{print $2}'`
echo "PID: ${RUNNING_PID}"
