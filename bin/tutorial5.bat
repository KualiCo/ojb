@echo off
cd target\test\ojb
if not errorlevel 1 goto ok

echo Please run this script from the project root directory as follows:
echo    bin\tutorial5.bat
goto end

:ok
set cp=
for %%i in (..\..\..\lib\*.jar) do call ..\..\..\bin\cp.bat %%i
set CP=..\..\..\target\classes;..\..\..\target\classestest;%CP%

java -cp %CP% org.apache.ojb.tutorial5.Application

cd..
cd..
cd..

:end
