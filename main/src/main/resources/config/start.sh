#!/usr/bin/env bash
/usr/bin/java -Dlog4j.configurationFile=./log4j2.xml -jar ./raspyTemp-1-jar-with-dependencies.jar ./app.properties ./ftp.properties
