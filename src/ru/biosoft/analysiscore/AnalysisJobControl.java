package ru.biosoft.analysiscore;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.jobcontrol.StackProgressJobControl;

/**
 * @author lan
 */
public class AnalysisJobControl extends StackProgressJobControl
{
    protected static final Logger log = Logger.getLogger( AnalysisJobControl.class.getName() );

    protected AnalysisMethodSupport<?> method;

    public AnalysisJobControl(AnalysisMethodSupport<?> method)
    {
        super( log );
        this.method = method;
    }
    
    public AnalysisMethodSupport<?> getMethod()
    {
        return method;
    }

    @Override
    protected void doRun() throws JobControlException
    {
        try
        {
            if(pauseOnStart)
            {
                pause();
                checkStatus();
            }
            method.validateParameters();
            long startTime = System.currentTimeMillis();
            Object result;
            if(recoverMode)
            {
                method.getLogger().info("Analysis '"+method.getName()+"' restarted");
                result = method.recover();
            }
            else
            {
                method.getLogger().info("Analysis '"+method.getName()+"' started");
                result = method.justAnalyzeAndPut();
            }
            writeProperiesToResults( result );
            Object[] results = result == null ? method.getAnalysisResults() : result instanceof Object[] ? (Object[])result : new Object[] {result};
            resultsAreReady(results);
            long endTime = System.currentTimeMillis();
            method.getLogger().info("Analysis '"+method.getName()+"' finished ("+(endTime-startTime)/1000.0+" s)");
        }
        catch( Throwable e )
        {
            LoggedException buex = translateException( e );
            method.getLogger().log(Level.SEVERE, buex.log());
            throw new JobControlException(buex);
        }
    }

    private void writeProperiesToResults(Object returnedResult) throws Exception
    {
        //Write analysis parameters to returned results and results from parameters 
        Set<ru.biosoft.access.core.DataElementPath> resultsPathSet = new HashSet<>();
        if( returnedResult != null )
        {
            for( Object resultObj : returnedResult instanceof Object[] ? (Object[])returnedResult : new Object[] {returnedResult} )
            {
                if(resultObj instanceof DataCollection)
                {
                    method.writeProperties((DataElement)resultObj);
                    resultsPathSet.add( ( (DataElement)resultObj ).getCompletePath() );
                }
            }
        }
        Object[] analysisResuls = method.getAnalysisResults();
        if( analysisResuls != null )
        {
            for( Object resultObj : analysisResuls )
            {
                if( resultObj instanceof DataCollection && !resultsPathSet.contains( ( (DataElement)resultObj ).getCompletePath() ) )
                {
                    method.writeProperties( (DataElement)resultObj );
                }
            }
        }
    }

    private LoggedException translateException(Throwable e)
    {
        Properties params = new Properties();
        method.getParameters().write( params, "" );
        StringBuilder sb = new StringBuilder();
        sb.append( method.getName() + " failed with following parameters:\n" );
        for( Entry<Object, Object> entry : params.entrySet() )
            sb.append( entry.getKey() ).append( '=' ).append( entry.getValue() ).append( '\n' );
        log.severe( sb.toString() );

        while(e instanceof JobControlException && e.getCause() != null)
            e = e.getCause();
        while(e instanceof JobControlException && ((JobControlException)e).getError() != null)
            e = e.getCause();
        return ExceptionRegistry.translateException(e);
    }

    public final boolean isStopped()
    {
        return getStatus() == TERMINATED_BY_REQUEST;
    }

    private boolean recoverMode = false;
    public void setRecoverMode(boolean mode)
    {
        recoverMode = mode;
    }
    
    private boolean pauseOnStart = false;
    public void setPauseOnStart(boolean flag)
    {
        pauseOnStart = flag;
    }
}
