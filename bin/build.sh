#! /bin/sh

# $Id: build.sh,v 1.1 2007-08-24 22:17:39 ewestfal Exp $

if [ -z "$JAVA_HOME" ] ; then
  JAVA=`which java`
  if [ -z "$JAVA" ] ; then
    echo "Cannot find JAVA. Please set your PATH."
    exit 1
  fi
  JAVA_BIN=`dirname $JAVA`
  JAVA_HOME=$JAVA_BIN/..
fi

#JAVA_HOME=/usr/java/jdk1.3.0_02
#JAVA_HOME=/home/tom/incoming/jdk1.2.2

JAVA=$JAVA_HOME/bin/java

CLASSPATH=`echo lib/*.jar | tr ' ' ':'`:$CLASSPATH
CLASSPATH=build/classes/:$CLASSPATH:$JAVA_HOME/lib/tools.jar


$JAVA -classpath $CLASSPATH -Dant.home=lib org.apache.tools.ant.Main "$@" -buildfile build.xml


