package ru.biosoft.analysis;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnGroup;

import ru.biosoft.jobcontrol.JobControlException;

/**
 * Abstract class for analysis. It should return table data collection.
 * provides job control, logger, methods for stopping analysis and calculating step count (override it if needed)
 * To create analysis you should  override method getAnalyzedData(), and getParameters()
 */
public abstract class MicroarrayAnalysis<T extends MicroarrayAnalysisParameters> extends AnalysisMethodSupport<T>
{
    public boolean go; //if false than analysis should stop
    public int step;
    private MicroarrayAnalysisJobControl jobControl = new MicroarrayAnalysisJobControl(this);
    public double progresByStep;

    protected MicroarrayAnalysis(DataCollection origin, String name, T parameters)
    {
        super(origin, name, parameters);
    }

    protected MicroarrayAnalysis(DataCollection origin, String name, Class<? extends JavaScriptHostObjectBase> jsClass, T parameters) throws Exception
    {
        super(origin, name, jsClass, parameters);
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        MicroarrayAnalysisParameters parameters = getParameters();

        ColumnGroup experimentData = parameters.getExperimentData();
        if( experimentData == null || experimentData.getTable() == null )
            throw new IllegalArgumentException("Please specify experiment table");

        if( experimentData.getColumns().length == 0 )
        {
            log.info("Experiment columns were not specified: all numeric columns will be used.");
            experimentData.setAllColumnsFromTable();
        }

//        throw new IllegalArgumentException("Please specify experiment columns");
     
        DataElementPath path = parameters.getOutputTablePath();
        if( path == null || path.optParentCollection() == null || path.getParentCollection().getName().equals( "" ) )
            throw new IllegalArgumentException("Please specify output collection.");

        if( path.getName().equals("") )
            throw new IllegalArgumentException("Please specify output name.");
        if( experimentData.getTablePath().equals(path) )
            throw new IllegalArgumentException("Output is the same as the input. Please specify different output name.");
    }

    /**
     * Init some properties for analysis running
     */
    public void init()
    {
        int stepCount = getStepCount();
        if( stepCount == 0 )
            throw new IllegalArgumentException("Experiment is empty or was loaded with errors.");
        progresByStep = 100d / stepCount;
        go = true;
        step = 0;
    }

    @Override
    public MicroarrayAnalysisJobControl getJobControl()
    {
        return jobControl;
    }

    public void incPreparedness(int step)
    {
        jobControl.setPreparedness((int) ( step * progresByStep ));
    }

    /**
     * Do analyze and the put resulted tabe data collection in data\microarray results
     */
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection result = justAnalyze();
        if( go && result != null )
        {
            MicroarrayAnalysisParameters parameters = getParameters();
            this.writeProperties(result);
            parameters.getOutputTablePath().save(result);
            log.info("Table " + result.getName() + " created.");
        }
        else
        {
            log.info("Analysis wasn't completed.");
        }
        return null;
    }

    /**
     * Do analyze
     */
    public TableDataCollection justAnalyze() throws Exception
    {
        init();
        return getAnalyzedData();
    }

    //Analyzing method
    protected abstract TableDataCollection getAnalyzedData() throws Exception;

    public static class MicroarrayAnalysisJobControl extends AnalysisJobControl
    {
        public MicroarrayAnalysisJobControl(MicroarrayAnalysis<?> method)
        {
            super(method);
        }

        @Override
        public void doRun() throws JobControlException
        {
            ((MicroarrayAnalysis<?>)method).go = true;
            super.doRun();
        }

        @Override
        protected void setTerminated(int status)
        {
            ((MicroarrayAnalysis<?>)method).go = false;
            super.setTerminated(status);
        }
    }

    //Override this method if input of your analysis differs from parameters.getExpriment()
    public int getStepCount()
    {
        MicroarrayAnalysisParameters parameters = getParameters();
        TableDataCollection table = parameters.getExperiment();
        int size = ( table != null ) ? table.getSize() : 0;
        if( getParameters().isFdr() )
            size *= 51;
        return size;
    }

}
