rem call mvn clean install

call mvn clean package -pl common,integration-server -am
call mvn clean package -pl web-server -am
copy /Y C:\udv\EIS\integration-server\target\eis-integration-server.jar C:\EIS\services\integration-server\eis-integration-server.jar

rem copy common\target\eis-common.jar C:\EIS\services\integration-server\*.*
rem copy integration-server\target\eis-integration-server.jar C:\EIS\services\integration-server\*.*
copy web-server\target\web-server-1.0-SNAPSHOT.war \Apache\apache-tomcat-11.0.18\webapps\ROOT.war

call mvn clean 


rem copy target\EIS.war \Apache\apache-tomcat-11.0.18\webapps\EIS.*