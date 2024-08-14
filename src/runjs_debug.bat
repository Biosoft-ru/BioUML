cd ..

set DEBUG_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006 

java %DEBUG_OPTS% -classpath startup.jar org.eclipse.core.launcher.Main -application ru.biosoft.plugins.javascript.shell ./data ./data_resources