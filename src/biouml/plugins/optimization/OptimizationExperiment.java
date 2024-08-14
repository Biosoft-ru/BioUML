package biouml.plugins.optimization;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

import one.util.streamex.IntStreamEx;

import java.util.logging.Logger;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import biouml.model.Diagram;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.Type;

public class OptimizationExperiment extends BaseSupport
{
    protected static final Logger log = Logger.getLogger(OptimizationExperiment.class.getName());

    public static final String DIAGRAM_STATE_NAME = "diagramStateName";
    public static final String CELL_LINE = "cellLine";
    public static final String EXPERIMENT_TYPE = "experimentType";
    public static final String WEIGHT_METHOD = "weightMethod";

    private List<Integer> exactDataColumns;
    private final ExperimentalTableSupport tableSupport; //Support for the experimental table processing
    private Diagram diagram;
    private String diagramStateName = "";
    private List<ParameterConnection> parameterConnections;
    private String cellLine = "";
    private String experimentType = "";

    public OptimizationExperiment(String name)
    {
        super(null, name, Type.TYPE_EXPERIMENT);
        tableSupport = new ExperimentalTableSupport();
    }

    public OptimizationExperiment(String name, DataElementPath filePath)
    {
        super(filePath == null ? null : filePath.optParentCollection(), name, Type.TYPE_EXPERIMENT);
        tableSupport = new ExperimentalTableSupport(filePath);
        initExperimentType(tableSupport.getTable());
    }

    public OptimizationExperiment(String name, TableDataCollection tdc)
    {
        super(tdc.getOrigin(), name, Type.TYPE_EXPERIMENT);
        tableSupport = new ExperimentalTableSupport(tdc);
        initExperimentType(tableSupport.getTable());
    }

    public void initExperimentType(TableDataCollection tdc)
    {
        if(tdc.getColumnModel().hasColumn("time"))
        	setExperimentType(ExperimentType.toString(ExperimentType.TIME_COURSE));
        else
        	setExperimentType(ExperimentType.toString(ExperimentType.STEADY_STATE));
    }


    @PropertyName ( "File path" )
    @PropertyDescription ( "File path." )
    public DataElementPath getFilePath()
    {
        return tableSupport.getFilePath();
    }
    public void setFilePath(DataElementPath filePath)
    {
        tableSupport.setFilePath(filePath);
        parameterConnections = null;
    }

    public ExperimentalTableSupport getTableSupport()
    {
        return tableSupport;
    }

    @PropertyName ( "Weight method" )
    @PropertyDescription ( "Weight method." )
    public String getWeightMethod()
    {
        return tableSupport.getWeightMethod();
    }
    public void setWeightMethod(String weightMethod)
    {
        String oldvalue = tableSupport.getWeightMethod();
        tableSupport.setWeightMethod(weightMethod);
        firePropertyChange(WEIGHT_METHOD, oldvalue, weightMethod);
    }

    public void setDiagram(Diagram diagram)
    {
        this.diagram = diagram;
    }

    @PropertyName ( "Diagram state" )
    @PropertyDescription ( "Diagram state name." )
    public String getDiagramStateName()
    {
        return this.diagramStateName;
    }
    public void setDiagramStateName(String stName)
    {
        String oldvalue = this.diagramStateName;
        this.diagramStateName = stName;
        firePropertyChange(DIAGRAM_STATE_NAME, oldvalue, stName);
    }

    @PropertyName ( "Cell line" )
    @PropertyDescription ( "Cell line." )
    public String getCellLine()
    {
        return this.cellLine;
    }
    public void setCellLine(String cellLine)
    {
        String oldValue = this.cellLine;
        this.cellLine = cellLine;
        firePropertyChange(CELL_LINE, oldValue, cellLine);
    }

    @PropertyName ( "Experiment type" )
    @PropertyDescription ( "Experiment type." )
    public String getExperimentType()
    {
        return this.experimentType;
    }
    public void setExperimentType(String experimentType)
    {
        String oldvalue = this.experimentType;
        this.experimentType = experimentType;
        firePropertyChange(EXPERIMENT_TYPE, oldvalue, experimentType);
    }

    public static enum ExperimentType
    {
        TIME_COURSE, STEADY_STATE;

        public static String toString(ExperimentType experimentType)
        {
            switch( experimentType )
            {
                case TIME_COURSE:
                    return "Time course";

                case STEADY_STATE:
                    return "Steady state";

                default:
                    return "";
            }
        }

        public static List<String> getExperimentTypes()
        {
            List<String> list = new ArrayList<>();
            list.add(toString(TIME_COURSE));
            list.add(toString(STEADY_STATE));
            return list;
        }
    }

    public boolean isConstraintApplicable(OptimizationConstraint constraint)
    {
        for( String exp : constraint.getExperiments() )
        {
            if(exp.equals( OptimizationConstraint.ALL_EXPERIMENTS ))
                return true;
            if( getName().equals( exp ) )
                return true;
        }
        return false;
    }

    public boolean isTimeCourse()
    {
        String type = ExperimentType.toString(ExperimentType.TIME_COURSE);
        return experimentType.equals(type);
    }

    public boolean isSteadyState()
    {
        String type = ExperimentType.toString(ExperimentType.STEADY_STATE);
        return experimentType.equals(type);
    }

    public void initWeights()
    {
        try
        {
            tableSupport.calculateWeights(isTimeCourse(), fillExactDataColumns());
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, MessageBundle.getMessage("ERROR_EXPERIMENT_TABLE_GETTING"), e);
        }
    }

    public List<ParameterConnection> getParameterConnections()
    {
        if( parameterConnections == null )
            initParameterConnections();
        return parameterConnections;
    }
    public void setParameterConnections(List<ParameterConnection> parameterConnections)
    {
        this.parameterConnections = parameterConnections;
        fillExactDataColumns();
    }

    public String getVariableNameInDiagram(String name)
    {
        if( parameterConnections != null )
        {
            for( ParameterConnection connection : parameterConnections )
                if( connection.getNameInFile().equals(name) )
                    return connection.getVariableNameInDiagram();
        }
        return "";
    }

    public String getVariableNameInFile(String name)
    {
        if( parameterConnections != null )
        {
            for( ParameterConnection connection : parameterConnections )
                if( connection.getNameInDiagram().equals(name) )
                    return connection.getNameInFile();
        }
        return name;
    }

    public int getRelativePoint(String name)
    {
        if( isTimeCourse() && parameterConnections != null )
        {
            for( ParameterConnection connection : parameterConnections )
                if( connection.getNameInFile().equals(name) )
                    return connection.getRelativeTo();
        }
        return -1;
    }

    public void initParameterConnections()
    {
        parameterConnections = new ArrayList<>();
        TableDataCollection expTable = tableSupport.getTable();

        if( expTable != null )
        {
            for( TableColumn column : expTable.getColumnModel() )
            {
                ParameterConnection connection = new ParameterConnection(this, parameterConnections.size());
                connection.setDiagram(diagram);
                String columnName = column.getName();
                connection.setNameInFile(columnName);
                parameterConnections.add(connection);
            }
        }

        tableSupport.calculateWeights(isTimeCourse(), fillExactDataColumns());
    }

    public boolean containsRelativeData()
    {
        if( parameterConnections != null )
        {
            for( ParameterConnection connection : parameterConnections )
            {
                if( connection.getRelativeTo() != -1 )
                    return true;
            }
        }
        return false;
    }

    public List<Integer> fillExactDataColumns()
    {
        if( parameterConnections != null && containsRelativeData() )
        {
            exactDataColumns = IntStreamEx.ofIndices(parameterConnections, conn -> conn.getRelativeTo() == -1).boxed().toList();
            return exactDataColumns;
        }
        return null;
    }

    public List<Integer> getExactDataColumns()
    {
        return this.exactDataColumns;
    }

    @Override
    public OptimizationExperiment clone()
    {
        OptimizationExperiment clone = new OptimizationExperiment(getName(), getFilePath());
        clone.setCellLine(getCellLine());
        clone.setDiagram(diagram);
        clone.setDiagramStateName(getDiagramStateName());
        clone.setExperimentType(getExperimentType());
        clone.setTitle(getTitle());
        clone.setWeightMethod(getWeightMethod());

        List<ParameterConnection> connections = new ArrayList<>();
        for( ParameterConnection connection : getParameterConnections() )
            connections.add(connection.clone(clone, connections.size()));
        clone.setParameterConnections(connections);
        return clone;
    }

    @Override
    public String toString()
    {
        if( getFilePath() == null )
            return "";
        return getFilePath().toString() + "; " + diagramStateName;
    }
}
