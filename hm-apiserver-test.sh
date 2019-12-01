#!/bin/bash

# SOURCE_CODE_DIR="./hm-apiserver"
# cd $SOURCE_CODE_DIR || return 127

export SBT_OPTS="-XX:+CMSClassUnloadingEnabled -Xmx2G"
/usr/local/sbt/bin/sbt -Dsbt.log.noformat=true test
