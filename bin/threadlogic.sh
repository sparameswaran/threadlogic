#!/bin/sh

# Unix shell script for starting threadlogic. If you have big log files
# you might need to adjust Xmx setting.
BIN_DIR=$(dirname $0)
THREAD_LOGIC_JAR=$(ls $BIN_DIR/../dist/ThreadLogic*.jar)
java -Xmx1g -jar $THREAD_LOGIC_JAR
