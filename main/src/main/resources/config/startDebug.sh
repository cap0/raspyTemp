#!/usr/bin/env bash
/usr/bin/java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=1098 -Dlog4j.configurationFile=./log4j2.xml -jar ./raspyTemp-1-jar-with-dependencies.jar ./app.properties ./ftp.properties
