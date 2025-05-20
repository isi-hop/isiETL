@echo on
cd ..
del isiETL-0.1.jar
xcopy .\target\isiETL-0.1-jar-with-dependencies.jar /Y
rename isiETL-0.1-jar-with-dependencies.jar isiETL-0.1.jar
java -jar isiETL-0.1.jar %1 %2