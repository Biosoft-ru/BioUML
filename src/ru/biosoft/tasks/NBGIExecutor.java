package ru.biosoft.tasks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.FileCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.tasks.NextflowJobWatcher.Handler;

public class NBGIExecutor implements TaskExecutor
{
    private static final Logger log = Logger.getLogger( NBGIExecutor.class.getName() );
    
    
    private NextflowService nextflow;
    private NextflowJobWatcher jobWatcher;
    
    @Override
    public void init(DynamicPropertySet config)
    {
        String apiURL = (String)config.getValueAsString( "apiURL" );
        String user = (String)config.getValueAsString( "user" );
        String execType = (String)config.getValueAsString( "execType" );
        nextflow = new NBGINextflowService( apiURL, execType, user );
        jobWatcher = new NextflowJobWatcher( nextflow );
    }
    
    @Override
    public int canExecute(TaskInfo taskInfo)
    {
        if(taskInfo.getType().equals( TaskInfo.ANALYSIS ))
        {
            String source = taskInfo.getSource().toString();
            if(source.startsWith( "analyses/Galaxy/" ) || source.startsWith( "analyses/Galaxy2/" ))
                return EXECUTE_NO;
            return EXECUTE_REMOTE;
        }
        return EXECUTE_NO;
    }

    @Override
    public void submit(TaskInfo taskInfo)
    {
        AnalysisJobControl jc = (AnalysisJobControl)taskInfo.getJobControl();
        
        //Make calls to job control in the similar way as in AnalysisJobControl.run()
        jc.begin();

        
        try
        {
            //Generate javascript
            AnalysisMethodSupport<?> method = jc.getMethod();
            
            method.getLogger().log( Level.INFO, "Submitting job to nextflow service" );
            
            String js = method.generateJavaScript( method.getParameters() );

            //Write javascript to file
            String jsUUID = UUID.randomUUID().toString();
            String jsLocalFilePath = "/nf/js/" + jsUUID + ".js";
            String jsRemoteFilePath = "/var/nfwork/nsk/js/" + jsUUID + ".js";
            File jsFile = new File( jsLocalFilePath );
            Files.write( jsFile.toPath(), js.getBytes( StandardCharsets.UTF_8 ) );
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString( "rw-r--r--" );
            Files.setPosixFilePermissions( jsFile.toPath(), perms );


            //Ask nextflow to start javascript
            //resp = nextflow.submit( "/var/nfwork/nsk/biouml-shell.nf", new String[] {"--script", js} );
            JSONObject resp = nextflow.submit( "/var/nfwork/nsk/biouml-script.nf", new String[] {"--script_file", jsRemoteFilePath} );
            method.getLogger().log( Level.INFO, "Job submitted with id: " + resp.getString( "runname" ) );
            method.getLogger().log( Level.INFO, "Waiting for job " + resp.getString( "runname" ) );
            log.log( Level.INFO, resp.toString() );
            
            String jobId = resp.getString( "runname" );
            jobWatcher.register( jobId, new Handler() {
                @Override
                public void statusUpdate(JSONObject params)
                {
                    JSONObject header = params.getJSONObject( "header" );
                    if(header.optBoolean( "success" ))
                    {
                        //Successfully finished
                        jobWatcher.unregister( jobId, this );
                        reportMessages(method.getLogger(), params);
                        reInitFiles();
                        jc.resultsAreReady();
                        jc.end();
                        jsFile.delete();
                    }else
                    {
                        String event = header.optString( "event", null );
                        if("started".equals(event))
                        {
                            //still running
                        }else if(event==null && params.getJSONArray( "workflowProcessList" ).length() > 0)
                        {
                            //failed
                            jobWatcher.unregister( jobId, this );
                            
                            reportMessages(method.getLogger(), params);
                            JobControlException jcex = new JobControlException( JobControl.TERMINATED_BY_ERROR, "");
                            jc.exceptionOccured( jcex );
                            jc.end(jcex);
                            
                            //do not delete jsFile of failed process for easy debugging
                            //jsFile.delete();
                        }else
                        {
                            log.warning( "Unhandled jobStatus: " + params );
                        }
                    }

                }

                });
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Can not submit task " + taskInfo.getName(), e );

            JobControlException jcex = new JobControlException( e );
            jc.exceptionOccured( jcex );
            jc.end( jcex );
        }


    }
    
    private void reportMessages(Logger log, JSONObject params)
    {
        JSONArray jsonArray = params.getJSONArray( "workflowProcessList" );
        for(int i = 0; i < jsonArray.length(); i++)
        {
            JSONObject processInfo = jsonArray.getJSONObject( i );
            String stdout = processInfo.getString( "stdOut" );
            String stderr = processInfo.getString( "stdErr" );
            log.info( stdout );
            log.severe( stderr );
        }
    }
    
    private void reInitFiles()
    {
        DataElementPath.create( "data/Files" ).getDataElement( FileCollection.class ).reinit();
    }

}
