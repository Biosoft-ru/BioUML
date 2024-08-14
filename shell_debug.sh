#IZPACK_JAVA_HOME#
DEBUG_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006" 

if [ -z $JAVA_HOME ]; then
	echo "WARNING: JAVA_HOME environment variable is not set.";
	java ${DEBUG_OPTS} -jar plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -application ru.biosoft.plugins.javascript.shell ./data ./data_resources $* 
else
	${JAVA_HOME}/bin/java ${DEBUG_OPTS} -jar plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -application ru.biosoft.plugins.javascript.shell ./data ./data_resources $* 
fi
