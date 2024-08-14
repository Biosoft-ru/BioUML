package ru.biosoft.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;


public class LocalNextflowService implements NextflowService
{

    private Map<String, Process> processes = new ConcurrentHashMap<>();
    
    @Override
    public JSONObject submit(String nextflowScriptPath, String[] params) throws Exception
    {
        File workDir = new File(nextflowScriptPath).getParentFile();
        ProcessBuilder pb = new ProcessBuilder( "nextflow", "run", nextflowScriptPath )
                .directory( workDir );
        
        Process proc = pb.start();
        String id = UUID.randomUUID().toString();
        processes.put( id, proc );
        JSONObject result = new JSONObject();
        result.put( "runname", id);
        result.put( "status", "STARTED");
        return result;
    }

    @Override
    public JSONObject status(String runname) throws Exception
    {
        JSONObject result = new JSONObject();
        result.put( "runname", runname );
    
        Process proc = processes.get( runname );
        if(proc == null)
        {
            result.put( "status", "NOT_FOUND");
            return result;
        }

        String errMsg = readStream(proc.getErrorStream());
        if(!errMsg.isEmpty())
            result.put( "stderr", errMsg );
        
        String outMsg = readStream(proc.getInputStream());
        if(!outMsg.isEmpty())
            result.put( "stdout", outMsg );
        
        if( proc.isAlive() )
            result.put( "status", "STARTED" );
        else
        {
            int rc = proc.exitValue();
            if( rc == 0 )
                result.put( "status", "COMPLETED" );
            else
            {
                result.put( "status", "FAILED" );
                result.put( "exitCode", rc );
            }
        }
        return result;
    }

    private String readStream(InputStream is) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        Reader reader = new InputStreamReader( is, StandardCharsets.UTF_8 );
        char[] cbuf = new char[100];
        while(reader.ready())
        {
            int n = reader.read( cbuf );
            sb.append( cbuf, 0, n );
        }
        return sb.toString();
    }

}
