#IZPACK_JAVA_HOME#

if [ -z $JAVA_HOME ]; then
	echo "WARNING: JAVA_HOME environment variable is not set.";
	java -jar plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -application ru.biosoft.plugins.javascript.shell ./data ./data_resources $* 
else
	${JAVA_HOME}/bin/java -jar plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -application ru.biosoft.plugins.javascript.shell ./data ./data_resources $* 
fi