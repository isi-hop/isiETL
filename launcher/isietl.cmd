@echo off
cls
REM 0 parameters
REM => read isietl.properties if is set
REM instead use default values for -fip, -dp
REM 1 parameters
REM allowed alone [-dp defaut false, -fip, -h, -jt defalut false]
REM maximum 2 parameters in the same times
REM are allowed [-dp & -fip]

java -jar isiETL-0.2-jar-with-dependencies.jar %1 %2
