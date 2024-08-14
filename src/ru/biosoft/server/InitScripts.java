package ru.biosoft.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.security.SecurityManager;

//run js scripts on startup from $SERVER_PATH/conf/init/ folder
public class InitScripts
{
    private static final Logger log = Logger.getLogger( InitScripts.class.getName() );
    public static void run() throws IOException
    {
        String serverPath = System.getProperty("biouml.server.path");
        Path dir = Paths.get( serverPath, "conf/init" );
        if(!Files.isDirectory( dir ))
            return;
        Path[] scripts = Files.list( dir )
                .filter( p->p.getFileName().toString().endsWith( ".js" ) )
                .sorted()
                .toArray( Path[]::new );
        ScriptEnvironment env = new LogScriptEnvironment( log, true );
        for(Path script : scripts)
        {
            try
            {
                String content = new String(Files.readAllBytes( script ));
                SecurityManager.runPrivileged( () -> ScriptTypeRegistry.execute("js", content, env, false) );
            }
            catch(Exception e)
            {
                log.log( Level.WARNING, "Executing " + script, e );
            }
        }
    }
    
    public static void runSafe()
    {
        try {
            run();
        } catch(Throwable t)
        {
            log.log( Level.SEVERE, "", t );
        }
    }

}
