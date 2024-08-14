set DEBUG_OPTS=-Xmx500m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006
"%JAVA_HOME%/bin/java" %DEBUG_OPTS% -classpath startup.jar org.eclipse.core.launcher.Main -application ru.biosoft.server.tomcat.runner -port 8080 -repo ./data_server
