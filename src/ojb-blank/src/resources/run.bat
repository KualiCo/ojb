@echo off
java -cp @CLASSPATH_WINDOWS@;%CLASSPATH% -DOJB.bootLogLevel=WARN @MAIN_CLASS@

