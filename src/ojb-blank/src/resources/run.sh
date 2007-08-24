#! /bin/sh
java -classpath @CLASSPATH_UNIX@:$CLASSPATH -DOJB.bootLogLevel=WARN @MAIN_CLASS@
