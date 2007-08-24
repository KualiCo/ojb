#! /bin/sh

cd target/test/ojb

CLASSPATH=`echo ../../../lib/*.jar | tr ' ' ':'`:$CLASSPATH
CLASSPATH=../../../target/classes:../../../target/classestest:$CLASSPATH

java -classpath $CLASSPATH -DOJB.bootLogLevel=INFO org.apache.ojb.tutorial1.Application

cd ..
cd ..
cd ..

