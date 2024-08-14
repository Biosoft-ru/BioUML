package biouml.plugins.riboseq.ingolia.svmlight;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.tasks.process.LauncherFactory;
import ru.biosoft.tasks.process.ProcessLauncher;
import biouml.plugins.riboseq.ingolia.Observation;
import biouml.plugins.riboseq.ingolia.ObservationList;

public class SVMLight
{
    protected void runCommand(String program, List<String> arguments, Logger log) throws Exception
    {
        ProcessLauncher launcher = LauncherFactory.getDefaultLauncher();
        launcher.setCommand( makeCommand( program, arguments ) );
        launcher.setEnvironment( new LogScriptEnvironment( log ) );
        int result = launcher.executeAndWait();
        if(result != 0)
            throw new Exception("program exited with " + result);
    }
    
    private String makeCommand(String program, List<String> arguments)
    {
        StringBuilder builder = new StringBuilder();
        builder.append( quote( program ) );
        for(String arg : arguments)
            builder.append( ' ' ).append( quote(arg) );
        return builder.toString();
    }
    
    private String quote(String s)
    {
        validate(s);
        return "'" + s + "'";
    }
    private void validate(String s)
    {
        if(s.contains( "'" ) || s.contains( "\\" ))
            throw new IllegalArgumentException();
    }
    
    
    protected void writeObservations(ObservationList observations, File observationsFile) throws IOException
    {
        try( BufferedWriter writer = ApplicationUtils.utfWriter( observationsFile ) )
        {
            for(Observation o : observations.getObservations())
            {
                StringBuilder line = new StringBuilder();
                int response;
                switch(o.getType())
                {
                    case YES: response = 1; break;
                    case NO: response = -1; break;
                    case UNKNOWN: response = 0; break;
                    default:
                       throw new AssertionError();
                }
                line.append( response );
                double[] predictors = o.getPredictors();
                for(int i = 0; i < predictors.length; i++)
                    line
                        .append( ' ' )
                        .append( i + 1 )
                        .append( ':' )
                        .append( predictors[i] );
                if(o.getDescription() != null)
                    line
                        .append( " # " )
                        .append( o.getDescription() );
                line.append( '\n' );
                writer.write( line.toString() );
            }
        }
    }

}
