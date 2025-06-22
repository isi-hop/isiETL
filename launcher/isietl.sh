#!/bin/bash
clear
#0 parameters
# => read isietl.properties if is set
# instead use default values for -fip, -dp
# 1 parameters
# allowed alone [-dp defaut false, -fip, -h, -jt defalut false]
#maximum 2 parameters in the same times
# are allowed [-dp & -fip]

java -jar isiETL-0.1-jar-with-dependencies.jar $1 $2
