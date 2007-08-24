@echo off
REM Please let JAVA_HOME point to your JDK base directory
if "%JAVA_HOME%" == "" set JAVA_HOME=C:\j2sdk1.4.0

set JAVA=%JAVA_HOME%\bin\java
set cp=
for %%i in (lib\*.jar) do call bin\cp.bat %%i
set CP=..\target\classes;..\target\classestest;%JAVA_HOME%\lib\tools.jar;%CP%
"%JAVA%" -classpath "%CP%" -Dant.home=lib org.apache.tools.ant.Main %1 %2 %3 %4 %5 %6 -buildfile build.xml
