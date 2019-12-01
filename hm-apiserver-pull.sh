#!/bin/bash

SOURCE_CODE_DIR="./hm-apiserver"

if [ -d $SOURCE_CODE_DIR ]; then
  echo "Clear old source code"
  rm -rf $SOURCE_CODE_DIR
  echo "Remove result error code: $?"
fi

git clone git@github.com:ptr97/hm-apiserver.git

cd $SOURCE_CODE_DIR || return 127

# export SBT_OPTS="-XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=2G -Xmx2G"
# /usr/local/sbt/bin/sbt -Dsbt.log.noformat=true compile
