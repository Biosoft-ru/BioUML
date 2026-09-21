package biouml.plugins.wdl.analysis;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import biouml.plugins.wdl.analysis.WDLScriptLogs.LogConsumer;
import biouml.plugins.wdl.analysis.WDLScriptLogs.LogType;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptJobControl;
import ru.biosoft.jobcontrol.JobControlException;

public class WDLScriptJobControl extends ScriptJobControl
{
    String content;
    ScriptEnvironment env;
   

    public WDLScriptJobControl(String content, ScriptEnvironment env)
    {
        this.content = content;
        this.env = env;
    }

    @Override
    public String getResult()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void doRun() throws JobControlException
    {
        JSONObject wfDescr;
        try
        {
            wfDescr = CromwellAPI.INSTANCE.describeWorkflow( content, null, null );
        }
        catch( IOException e )
        {
            env.error( e.getMessage() );
            throw new JobControlException( e );
        }
        
        
        if(!wfDescr.getBoolean( "valid" ))
        {
            JSONArray errors = wfDescr.getJSONArray( "errors" );
            for(int i = 0; i < errors.length(); i++)
            {
                String error = errors.getString( i );    
                env.error( error );
            }
            return;
        }
        
        JSONObject resp;
        env.print( "WDL script is valid, running" );
        try
        {
            resp = CromwellAPI.INSTANCE.submitWorkflow( content, null, null, null );
        }
        catch( IOException e )
        {
            env.error( e.getMessage() );
            throw new JobControlException(e);
        }
        String taskId = resp.getString( "id" );
        
        WDLScriptLogs logs = new WDLScriptLogs( new LogConsumer()
        {
            @Override
            public void consume(String msg, String taskName, LogType type, int attempt, int shardIndex)
            {
                if(type == LogType.STDOUT)
                    env.info( taskName + ": " + msg );
                else if(type == LogType.STDERR)
                    env.error(taskName + ": " + msg );
            }
        } );
        
        while(true)
        {
            String state;
            try
            {
                Thread.sleep( 1000 );
                state = CromwellAPI.INSTANCE.getTaskStatus( taskId );
                if( state.equals( "Succeeded" ))
                    break;
                if(state.equals( "Failed" ))
                    throw new Exception("WDL script failed");//TODO: extract logs somehow
                if(!state.equals( "Not found" ))
                {
                    JSONObject logsResponse = CromwellAPI.INSTANCE.getLogs(taskId);
                    logs.updateLogs( logsResponse );
                }
            }
            catch( Exception e )
            {
                env.error( "ERROR:" + e.getMessage() );
                throw new JobControlException(e);
            }
        }
        
        try
        {
            JSONObject outputs = CromwellAPI.INSTANCE.getOutputs( taskId );
            env.print( outputs.toString() );
        }
        catch( IOException e )
        {
            env.error( e.getMessage() );
            throw new JobControlException( e );
        }
    }

}
