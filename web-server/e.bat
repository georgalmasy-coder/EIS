call mvn clean package
copy target\web-server-1.0-SNAPSHOT.war \Apache\apache-tomcat-11.0.18\webapps\ROOT.war
rem copy target\EIS.war \Apache\apache-tomcat-11.0.18\webapps\EIS.*