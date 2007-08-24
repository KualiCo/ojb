#! /bin/sh

cd target/test/ojb

CLASSPATH=`echo ../../../lib/*.jar | tr ' ' ':'`:$CLASSPATH
CLASSPATH=../../../target/classes:../../../target/classestest:$CLASSPATH

java -classpath $CLASSPATH org.apache.ojb.tutorial2.Application

cd ..
cd ..
cd ..
