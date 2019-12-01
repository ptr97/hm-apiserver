#!/bin/bash

# SOURCE_CODE_DIR="./hm-apiserver"
# cd $SOURCE_CODE_DIR || return 127

JAR_NAME="hm-apiserver-0.0.1-SNAPSHOT.jar"
LOG_FILE="output.log"
JAR_DEST="./hm-apiserver/target/scala-2.12/${JAR_NAME}"
WORK_DIR="./run_dir/"

export SBT_OPTS="-XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=2G -Xmx2G"
/usr/local/sbt/bin/sbt -Dsbt.log.noformat=true assembly

cd ../
cp $JAR_DEST $WORK_DIR
cd $WORK_DIR || return 127

if test -f "$FILE"; then
    echo "$FILE exist"
fi

if ! [ -f $LOG_FILE ]; then
  touch $LOG_FILE
fi

java -jar $JAR_NAME &> $LOG_FILE &
