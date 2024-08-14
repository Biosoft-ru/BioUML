@echo off
::IZPACK_JAVA_HOME::
set JAVA_OPTS=-Xms1024m -Xmx2048m -server -XX:+DoEscapeAnalysis
set APP_OPTS=-application ru.biosoft.plugins.javascript.shell ./data ./data_resources ./users ./history %1 %2
set TOOL_APPS=dads
set JAR=plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar

if not "x%JAVA_HOME%" == "x" goto java_home_set
if "x%JAVA_HOME%" == "x" goto no_java_home 

:java_home_set
"%JAVA_HOME%/bin/java" %JAVA_OPTS% -jar %JAR% %APP_OPTS%
exit

:no_java_home
echo WARNING: JAVA_HOME environment variable is not set.
java %JAVA_OPTS% -jar %JAR% %APP_OPTS%
