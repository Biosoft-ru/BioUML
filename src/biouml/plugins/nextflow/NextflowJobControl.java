package biouml.plugins.nextflow;

import java.io.File;

import org.json.JSONObject;

import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.tasks.DockerNextflowService;
import ru.biosoft.tasks.LocalNextflowService;
import ru.biosoft.tasks.NextflowService;

public class NextflowJobControl extends ScriptJobControl
{
    File scriptFile;
    ScriptEnvironment env;
   

    public NextflowJobControl(File file, ScriptEnvironment env)
    {
        this.scriptFile = file;
        this.env = env;
    }

    @Override
    public String getResult()
    {
        return null;
    }

    @Override
    protected void doRun() throws JobControlException
    {
        NextflowService service = new DockerNextflowService();//new LocalNextflowService();
        JSONObject resp;
        String id;
        try
        {
            resp = service.submit( scriptFile.getAbsolutePath(), new String[0] );
            System.out.println(resp);
            id = resp.getString( "runname" );
            System.out.println(id);
        }
        catch( Throwable e )
        {
            env.error( e.getMessage() );
            throw new JobControlException( e );
        }
        
        if(id == null)
        {
            env.error( resp.toString() );
            throw new JobControlException( JobControl.TERMINATED_BY_ERROR );
        }
        try
        {
            while( true )
            {
                resp = service.status( id );
                String status = resp.getString( "status" ).toUpperCase();
                
                if(resp.has( "stdout" ))
                    env.info( resp.getString( "stdout" ) );
                if(resp.has( "stderr" ))
                    env.info( resp.getString( "stderr" ) );
                
                //env.info( "status: " + status );
                if( status.equals( "COMPLETED" )  || status.equals( "FAILED" ))
                    break;
                Thread.currentThread().sleep( 1000 );
            }
        }
        catch( Exception e )
        {
            env.error( e.getMessage() );
            throw new JobControlException( e );
        }
    }

}
