if [ -z $JAVA_HOME ]; then
	echo "WARNING: JAVA_HOME environment variable is not set.";
	java -jar "$(dirname $0)/plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar" -application biouml.plugins.junittest.testrunner $* 
else
	${JAVA_HOME}/bin/java -jar "$(dirname $0)/plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar" -application biouml.plugins.junittest.testrunner $*
fi
