package ru.biosoft.analysis.optimization;

import static ru.biosoft.util.j2html.TagCreator.li;
import static ru.biosoft.util.j2html.TagCreator.ul;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.j2html.tags.ContainerTag;
import ru.biosoft.util.j2html.tags.Text;

@ClassIcon ( "resources/optimization-method.gif" )
abstract public class OptimizationMethod<T extends OptimizationMethodParameters> extends AnalysisMethodSupport<T>
{
    public static final String DEVIATION = "Calculation deviation";
    public static final String PENALTY = "Penalty function";
    public static final String EVALUATIONS = "Evaluations";
    public static final String VALUE = "Value";

    public OptimizationMethod(DataCollection<?> origin, String name, T parameters)
    {
        super(origin, name, parameters);
        log = Logger.getLogger( OptimizationMethod.class.getName() );
        info = new OptimizationMethodInfo();
        jobControl = new OptimizationMethodJobControl( );
    }
    
    @Override
    public OptimizationMethodJobControl getJobControl()
    {
        return (OptimizationMethodJobControl)super.getJobControl();
    }
    

    abstract public double[] getSolution() throws IOException, Exception;
    abstract public double[] getIntermediateSolution();

    abstract public double getDeviation();
    abstract public double getPenalty();

    private DataElementPath ownerPath;
    public void setOwnerPath(DataElementPath path)
    {
        this.ownerPath = path;
    }
    public DataElementPath getOwnerPath()
    {
        return this.ownerPath;
    }

    protected OptimizationProblem problem;
    protected List<Parameter> params;
    protected int n;

    protected double[] distances;
    protected double[] penalties;

    public void setOptimizationProblem(OptimizationProblem problem)
    {
        this.problem = problem;
        if( problem != null )
        {
            params = problem.getParameters();
            n = params.size();
            distances = new double[individualsNumber];
            penalties = new double[individualsNumber];
        }
    }
    public OptimizationProblem getOptimizationProblem()
    {
        return this.problem;
    }

    protected void calculateDistances(double[][] values)
    {
        try
        {
            double[][] result = problem.testGoodnessOfFit(values, jobControl);

            if( result != null && go)
            {
                for( int i = 0; i < individualsNumber; ++i )
                {
                    distances[i] = result[i][0];
                    penalties[i] = result[i][1];
                }
            }
            else if(go)
            {
                for( int i = 0; i < individualsNumber; ++i )
                {
                    distances[i] = Double.POSITIVE_INFINITY;
                    penalties[i] = Double.POSITIVE_INFINITY;
                }
            }
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Optimization method information
    //

    protected OptimizationMethodInfo info;
    public OptimizationMethodInfo getOptimizationMethodInfo()
    {
        return this.info;
    }

    private double[] lastDisplayedSolution = null;
    public double[] getLastDisplayedSolution()
    {
        return lastDisplayedSolution;
    }

    private double lastDisplayedDeviation = Double.NaN;
    public double getLastDisplayedDeviation()
    {
        return lastDisplayedDeviation;
    }

    private double lastDisplayedPanalty = Double.NaN;
    public double getLastDisplayedPenalty()
    {
        return lastDisplayedPanalty;
    }

    protected void displayInfo()
    {
        lastDisplayedSolution = getIntermediateSolution();
        lastDisplayedDeviation = getDeviation();
        lastDisplayedPanalty = getPenalty();
        info.setDeviation(Double.toString(lastDisplayedDeviation));
        info.setPenalty(Double.toString(lastDisplayedPanalty));
        info.setEvaluations(Integer.toString(problem.getEvaluationsNumber()));
    }

    protected void clearInfo()
    {
        lastDisplayedSolution = null;
        lastDisplayedDeviation = Double.NaN;
        lastDisplayedPanalty = Double.NaN;
        info.setDeviation("");
        info.setPenalty("");
        info.setEvaluations("");
    }

    private Object[] optResults;

    @Override
    public Object[] getAnalysisResults()
    {
        return this.optResults;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clone
    //
    public OptimizationMethod<T> clone(DataCollection<?> origin, String newName)
    {
        OptimizationMethod<T> cloneMethod = null;

        try
        {
            cloneMethod = getClass().getConstructor(DataCollection.class, String.class).newInstance(origin, newName);
            cloneMethod.setDescription(this.getDescription());
            cloneMethod.setOptimizationProblem(this.getOptimizationProblem());
            cloneProperties(cloneMethod);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not clone the optimization method " + getName() + ".");
        }
        return cloneMethod;
    }

    public void cloneProperties(OptimizationMethod<T> methodClone)
    {
        try
        {
            ComponentModel cModel = ComponentFactory.getModel(this.getParameters());
            ComponentModel cModelClone = ComponentFactory.getModel(methodClone.getParameters());

            for( int i = 0; i < cModel.getPropertyCount(); ++i )
            {
                Property prop = cModel.getPropertyAt(i);
                Property propClone = cModelClone.findProperty(prop.getName());
                propClone.setValue(prop.getValue());
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not clone properties of the optimization method " + getName() + ".");
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Job control
    //

    protected boolean go = true;

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        try
        {
            log.info("Start optimization.");

            incPreparedness(0);

            double[] resultValues = getSolution();

            DataElementPath resultPath = getParameters().getResultPath();
            optResults = saveResults(resultPath, resultValues, getDeviation(), getPenalty(), problem.getEvaluationsNumber(), false, true);

            problem.stop();

            incPreparedness(stepsNumber);
            log.info("Execution time: " + getWorkTime());
            clearInfo();
            
            return optResults;
        }
        catch( Throwable e )
        {
            go = false;
            while(e instanceof JobControlException && e.getCause() != null)
                e = e.getCause();
            while(e instanceof JobControlException && ((JobControlException)e).getError() != null)
                e = e.getCause();
            LoggedException buex = ExceptionRegistry.translateException( e );
            buex.log();
            throw buex;
        }
    }

    public class OptimizationMethodJobControl extends AnalysisJobControl
    {
        public OptimizationMethodJobControl()
        {
            super( OptimizationMethod.this );
        }

        @Override
        public void begin()
        {
            go = true;
            super.begin();
        }

        @Override
        protected void setTerminated(int status)
        {
            go = false;
            super.setTerminated(status);
            fireJobTerminated("");
        }
    }

    public Object[] saveResults(DataElementPath resultPath, double[] resultValues, double deviation, double penalty, int evaluations, boolean isIntermediate, boolean displayResults) throws Exception
    {
        DataCollection<DataElement> parent = DataCollectionUtils.createSubCollection(resultPath);
        parent.getInfo().setDescription(getOptimizationDescription(deviation, penalty, evaluations));
        CollectionFactoryUtils.save(parent);
        parent = resultPath.getDataCollection();

        TableDataCollection tdc = getInfoTable(resultPath.getDataCollection(), resultValues, deviation, penalty, evaluations);
        parent.put(tdc);
        if(displayResults)
            log.info(displayResultTable(tdc));

        if( isIntermediate )
            return null;

        Object[] results = problem.getResults(resultValues, resultPath.getDataCollection());

        for( Object result : results )
        {
            if( result instanceof AnalysisParameters )
            {
                try
                {
                    this.writeProperties(parent);
                }
                catch( Exception e )
                {
                    log.info("Can not write properties of the optimization method parameters.");
                }
            }
            else
            {
                parent.put((DataElement)result);
            }
        }
        return results;
    }

    protected int individualsNumber;

    protected static final int DEFAULT_STEPS_NUMBER = 100;
    protected int stepsNumber;

    protected void incPreparedness(int step)
    {
        if( stepsNumber == 0 )
        {
            incPreparedness(step % DEFAULT_STEPS_NUMBER, DEFAULT_STEPS_NUMBER);
        }
        else
        {
            incPreparedness(step, stepsNumber);
        }
    }

    private void incPreparedness(int step, int stepsNumber)
    {
        if( jobControl != null && go )
        {
            int p = 100 * step / stepsNumber;
            if( jobControl.getPreparedness() != p )
            {
                jobControl.setPreparedness(p);
                jobControl.fireValueChanged();
            }
        }
    }

    public String getWorkTime()
    {
        String result = "";

        long workTime = jobControl.getElapsedTime();
        long msec = workTime % 1000;
        workTime /= 1000;
        long sec = workTime % 60;
        workTime /= 60;
        long min = workTime % 60;
        workTime /= 60;
        result += Long.toString(workTime) + " h " + Long.toString(min) + " min " + Long.toString(sec) + " sec " + Long.toString(msec)
                + " msec.";

        return result;
    }

    public String displayResultTable(TableDataCollection tdc)
    {
        StringBuilder buf = new StringBuilder();
        buf.append("Optimization results:\n");

        Iterator<RowDataElement> it = tdc.iterator();
        while(it.hasNext())
        {
            RowDataElement row = it.next();
            Double value = (Double)row.getValue( VALUE );
            if( ( value == null || Double.isNaN( value ) ) && tdc.getColumnModel().getColumnCount() > 1 )
            {
                for(int k = 0; k < tdc.getColumnModel().getColumnCount(); ++k)
                {
                    TableColumn column = tdc.getColumnModel().getColumn( k );
                    if( !column.getName().equals( VALUE ) )
                    {
                        String str = row.getName() + " : " + column.getName() + " : " + row.getValue( column.getName() );
                        if( it.hasNext() )
                            str += "\n";
                        buf.append( str );
                    }
                }
            }
            else
            {
                String str = row.getName() + " : " + row.getValue(VALUE);
                if(it.hasNext())
                    str += "\n";
                buf.append(str);
            }
        }
        return buf.toString();
    }

    private TableDataCollection getInfoTable(DataCollection<DataElement> origin, double[] values, double deviation, double penalty,
            int evaluations) throws Exception
    {
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( origin, "optimizationInfo" );

        if( params != null )
        {
            List<String> columns = new ArrayList<>();

            //define columns
            for( Parameter param : params )
            {
                if( param.getScope() != null )
                    for( String scope : param.getScope() )
                        if( !columns.contains( scope ) )
                            columns.add( scope );
            }

            Collections.sort( columns );

            //add columns
            tdc.getColumnModel().addColumn( VALUE, DataType.Float );
            for( String column : columns )
                tdc.getColumnModel().addColumn( column, DataType.Float );

            //add rows
            for( Parameter param : params )
                if( !tdc.contains( param.getName() ) )
                {
                    Double[] row = new Double[columns.size() + 1];
                    for( int k = 0; k < row.length; ++k )
                        row[k] = Double.NaN;
                    TableDataCollectionUtils.addRow( tdc, param.getName(), row );
                }

            //fill the table with values
            for( int i = 0; i < params.size(); i++ )
            {
                Parameter param = params.get( i );
                RowDataElement rde = tdc.get( param.getName() );
                if( param.getScope() != null )
                    for( String scope : param.getScope() )
                        rde.setValue( scope, values[i] );
                else
                    rde.setValue( VALUE, values[i] );
                tdc.put( rde );
            }
        }

        TableDataCollectionUtils.addRow( tdc, DEVIATION, new Object[] {deviation} );
        TableDataCollectionUtils.addRow( tdc, PENALTY, new Object[] {penalty} );
        TableDataCollectionUtils.addRow( tdc, "Simulations", new Object[] {evaluations} );
        return tdc;
    }

    private String getOptimizationDescription(double deviation, double penalty, int evaluations)
    {
        StringBuilder buf = new StringBuilder();
        buf.append("\n");
        if(getOwnerPath() != null)
            buf.append("<b>Optimization document:</b> " + getOwnerPath().toString() + "\n");
        buf.append("<b>Optimization method:</b> " + getName() + "\n");

        ContainerTag tag = null;
        for(Property p : BeanUtil.properties(getParameters()))
        {
            ContainerTag li = li().with( new Text( p.getDisplayName() + " : " + p.getValue() ) )
                .condWith( p.getDescriptor().isExpert(), new Text(" (expert)") );
            tag = (tag == null ? ul() : tag).with( li );
        }
        if(tag != null)
        {
            buf.append("<b>Method parameters:</b> \n");
            buf.append(tag.toString());
        }
        buf.append("<b>" + DEVIATION + ":</b> " + deviation + "\n");
        buf.append("<b>" + PENALTY + ":</b> " + penalty + "\n");
        buf.append("<b>Simulations:</b> " + evaluations + "\n");
        buf.append("<b>Execution time:</b> " + getWorkTime());
        return buf.toString();
    }
}
