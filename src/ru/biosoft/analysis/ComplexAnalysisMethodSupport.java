package ru.biosoft.analysis;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.jobcontrol.JobControlListener;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.exception.AnalysisFailException;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;

/**
 * @author lan
 *
 */
public abstract class ComplexAnalysisMethodSupport<T extends AnalysisParameters> extends AnalysisMethodSupport<T>
{
    public ComplexAnalysisMethodSupport(DataCollection origin, String name, Class<? extends JavaScriptHostObjectBase> jsClass, T parameters)
    {
        super(origin, name, jsClass, parameters);
    }

    public ComplexAnalysisMethodSupport(DataCollection origin, String name, T parameters)
    {
        super(origin, name, parameters);
    }

    private static class SubJobInfo
    {
        JobControl job;
        double fromPercent, toPercent;
        String name;

        public SubJobInfo(JobControl job, double fromPercent, double toPercent, String name)
        {
            this.job = job;
            this.fromPercent = fromPercent;
            this.toPercent = toPercent;
            this.name = name == null?"":name;
        }
    }
    
    private class ComplexJobControlListener implements JobControlListener
    {
        private final SubJobInfo info;

        public ComplexJobControlListener(SubJobInfo info)
        {
            this.info = info;
        }
        
        @Override
        public void valueChanged(JobControlEvent event)
        {
            getJobControl().setPreparedness((int)(((double)event.getPreparedness())/100*(info.toPercent-info.fromPercent)+info.fromPercent));
        }
        
        @Override
        public void resultsReady(JobControlEvent event)
        {
            getJobControl().setPreparedness((int)info.toPercent);
        }
        
        @Override
        public void jobTerminated(JobControlEvent event)
        {
            if(event.getStatus() == JobControl.TERMINATED_BY_ERROR)
            {
                JobControlException exception = event.getException();
                error = new AnalysisFailException(exception.getError() == null ? exception : exception.getError(), info.name);
            }
            if(event.getStatus() == JobControl.TERMINATED_BY_REQUEST)
            {
                getJobControl().terminate();
            }
        }
        
        @Override
        public void jobStarted(JobControlEvent event)
        {
        }
        
        @Override
        public void jobResumed(JobControlEvent event)
        {
        }
        
        @Override
        public void jobPaused(JobControlEvent event)
        {
        }
    }
    private final List<SubJobInfo> jobs = new ArrayList<>();
    private LoggedException error = null;

    /**
     * Add analysis to analyzes queue. Analyzes will be executed in the same sequence as they are added using this function.
     * @param method analysis to add
     * @param endPercent percent on progress bar which should be displayed when analysis is finished
     * @param name text string to identify analysis (will prepend error messages if any)
     * analysis's own progress will be mapped to shorter range between previous analysis endPercent and current analysis endPercent.
     * Note that last added analysis should have endPercent = 100
     */
    protected void addAnalysis(AnalysisMethod method, double endPercent, String name)
    {
        SubJobInfo info = new SubJobInfo(method.getJobControl(), jobs.size() == 0?0:jobs.get(jobs.size()-1).toPercent, endPercent, name);
        jobs.add(info);
        method.getJobControl().addListener(new ComplexJobControlListener(info));
    }

    /**
     * Called before launching all jobs
     * You may override this to register jobs using addJob
     * @throws Exception
     */
    protected void beforeRun() throws Exception
    {
    }
    
    /**
     * Called after launching all jobs
     * @throws Exception
     */
    protected void afterRun() throws Exception
    {
    }

    /**
     * Called before specific job
     * @param jobName - name of the job specified in addJob call
     * @throws Exception
     */
    protected void beforeJob(String jobName) throws Exception
    {
    }

    /**
     * Called after running specific job
     * @param jobName - name of the job specified in addJob call
     * @throws Exception
     */
    protected void afterJob(String jobName) throws Exception
    {
    }

    @Override
    final public Object justAnalyzeAndPut() throws Exception
    {
        beforeRun();
        for(SubJobInfo subJob : jobs)
        {
            beforeJob(subJob.name);
            subJob.job.run();
            afterJob(subJob.name);
            if(jobControl.isStopped())
            {
                return null;
            }
            if(error != null)
            {
                throw error;
            }
        }
        try
        {
            afterRun();
        }
        catch( Exception e )
        {
            throw new JobControlException(e);
        }
        return null;
    }
}
