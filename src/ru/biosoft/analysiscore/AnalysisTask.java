package ru.biosoft.analysiscore;

import ru.biosoft.exception.LoggedException;

import java.util.logging.Level;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.task.JobControlTask;
import ru.biosoft.jobcontrol.ClassJobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.util.TextUtil;

/**
 * @author lan
 *
 */
public class AnalysisTask extends JobControlTask
{
    private AnalysisMethod analysis;

    public AnalysisTask(AnalysisMethod analysis)
    {
        super("Analysis: " + analysis.getName() + " (user: " + SecurityManager.getSessionUser() + ")", analysis.getJobControl());
        this.analysis = analysis;
    }

    @Override
    public double estimateWeight()
    {
        return analysis.estimateWeight();
    }

    @Override
    public long estimateMemory()
    {
        return analysis.estimateMemory();
    }

    @Override
    public boolean canSubmit()
    {
        try
        {
            analysis.validateParameters();
            long estimatedMemory = estimateMemory();
            long allowedMemory = SecurityManager.getMaximumMemoryPerProcess();
            if( estimatedMemory > allowedMemory )
            {
                throw new IllegalArgumentException(
                        "Process requested too much memory to run. Please reconsider input data and parameters to use less memory.\n"
                                + "Memory requested: " + TextUtil.formatSize(estimatedMemory) + "\nMemory allowed for current user: "
                                + TextUtil.formatSize(allowedMemory));
            }
        }
        catch( LoggedException e )
        {
            e.log();
            analysis.getLogger().log( Level.SEVERE, e.getMessage() );
            emulateFailure( e );
            return false;
        }
        catch( IllegalArgumentException e )
        {
            analysis.getLogger().log( Level.SEVERE, e.getMessage() );
            emulateFailure( e );
            return false;
        }
        analysis.getLogger().info("Analysis '" + analysis.getName() + "' added to queue");
        return true;
    }

    @Override
    protected void doRun()
    {
        long estimatedMemory = estimateMemory();
        long availableMemory = Runtime.getRuntime().freeMemory() + Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
        if( estimatedMemory > availableMemory )
        {
            System.gc();
            availableMemory = Runtime.getRuntime().freeMemory() + Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
            if( estimatedMemory > availableMemory )
            {
                analysis.getLogger()
                        .log( Level.SEVERE,
                                "Process requested too much memory to run. Please reconsider input data and parameters to use less memory or wait till more memory will be available.\n"
                                + "Memory requested: "
                                + TextUtil.formatSize(estimatedMemory)
                                + "\nMemory available in the system: "
                                + TextUtil.formatSize(availableMemory));
                emulateFailure( null );
                return;
            }
        }
        super.doRun();
    }

    private void emulateFailure(Throwable ex)
    {
        ClassJobControl jc = analysis.getJobControl();
        JobControlException jex = new JobControlException( ex );
        jc.begin();
        jc.exceptionOccured( jex );
        jc.end( jex );
    }
}
