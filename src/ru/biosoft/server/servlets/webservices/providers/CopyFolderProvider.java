package ru.biosoft.server.servlets.webservices.providers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.CopyFolderAnalysis;
import ru.biosoft.analysis.CopyFolderAnalysis.CopyFolderAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;

public class CopyFolderProvider extends WebJSONProviderSupport
{
    private static final Map<String, TaskInfo> jobToTask = new ConcurrentHashMap<>();

    @Override
    public void process(BiosoftWebRequest req, JSONResponse resp) throws Exception
    {
        String action = req.getAction();
        if( "copy".equals( action ) )
        {
            String fromPath = req.get( "path" );
            if( fromPath == null )
                throw new WebException( "EX_QUERY_PARAM_MISSING", "fromFolder" );
            String toPath = req.get( "newPath" );
            if( toPath == null )
                throw new WebException( "EX_QUERY_PARAM_MISSING", "toFolder" );
            String jobID = req.get( "jobID" );
            if( jobID == null )
                throw new WebException( "EX_QUERY_PARAM_MISSING", "jobID" );
            try
            {
                final CopyFolderAnalysis analysis = AnalysisMethodRegistry.getAnalysisMethod( CopyFolderAnalysis.class );
                CopyFolderAnalysisParameters parameters = analysis.getParameters();
                parameters.setWriteAnalysisInfo( false );
                parameters.setFromFolder( DataElementPath.create( fromPath ) );
                parameters.setToFolder( DataElementPath.create( toPath ) );

                TaskManager taskManager = TaskManager.getInstance();
                TaskInfo taskInfo = taskManager.addAnalysisTask( analysis, false );
                taskInfo.setTransient( "parameters", analysis.getParameters() );

                final WebJob webJob = WebJob.getWebJob( jobID );
                webJob.setTask( taskInfo );
                WebJob.attach( jobID, taskInfo );

                jobToTask.put( jobID, taskInfo );
                taskManager.runTask( taskInfo );
                resp.sendString( "ok" );
            }
            catch( Exception e )
            {
                resp.error( ExceptionRegistry.log( e ) );
            }
        }
        else if( "clearTask".equals( action ) )
        {
            String jobID = req.get( "jobID" );
            if( jobID == null )
                throw new WebException( "EX_QUERY_PARAM_MISSING", "jobID" );
            TaskInfo taskInfo = jobToTask.remove( jobID );
            if( taskInfo != null )
                TaskManager.getInstance().removeTask( taskInfo );
            resp.sendString( "ok" );
        }
        else
            throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION );
    }
}
