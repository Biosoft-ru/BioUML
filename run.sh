#IZPACK_JAVA_HOME#
if [ -z $JAVA_HOME ]; then
	echo "WARNING: JAVA_HOME environment variable is not set.";
	java -jar plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -application biouml.workbench.launcher ./data ./data_resources ./users ./history ./analyses $* 
else
	${JAVA_HOME}/bin/java -jar plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar -application biouml.workbench.launcher ./data ./data_resources ./users ./history ./analyses $* 
fi

