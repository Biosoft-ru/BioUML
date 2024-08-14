package biouml.plugins.test.tests;

import java.util.Arrays;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

import biouml.plugins.optimization.ExperimentalTableSupport;
import biouml.plugins.optimization.ExperimentalTableSupport.WeightMethod;

import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.test.Status;
import biouml.plugins.test.TestModel;
import biouml.standard.simulation.SimulationResult;

public class ExperimentValueTest extends Test
{
    private final ExperimentalTableSupport tableSupport;

    // similar with SED-ML
    private double maxDeviation;

    public ExperimentValueTest()
    {
        tableSupport = new ExperimentalTableSupport();
        tableSupport.setWeightMethod(WeightMethod.toString(WeightMethod.MEAN));

        resultVariable = new TestVariable(this);
    }

    private String experimentVariable;
    public String getExperimentVariable()
    {
        return experimentVariable;
    }
    public void setExperimentVariable(String experimentVariable)
    {
        String oldValue = this.experimentVariable;
        this.experimentVariable = experimentVariable;
        firePropertyChange("experimentVariable", oldValue, experimentVariable);
    }

    private TestVariable resultVariable;
    public TestVariable getResultVariable()
    {
        return resultVariable;
    }
    public void setResultVariable(TestVariable resultVariable)
    {
        this.resultVariable = resultVariable;
    }

    private int relativeTo = -1;
    public int getRelativeTo()
    {
        return relativeTo;
    }
    public void setRelativeTo(int relativeTo)
    {
        int oldValue = this.relativeTo;
        this.relativeTo = relativeTo;
        firePropertyChange("relativeTo", oldValue, relativeTo);
    }

    public DataElementPath getExperimentPath()
    {
        return tableSupport.getFilePath();
    }
    public void setExperimentPath(DataElementPath experimentPath)
    {
        tableSupport.setFilePath(experimentPath);
    }

    public String getWeightMethod()
    {
        return tableSupport.getWeightMethod();
    }
    public void setWeightMethod(String weightMethod)
    {
        tableSupport.setWeightMethod(weightMethod);
    }

    public TableDataCollection getTable()
    {
        return tableSupport.getTable();
    }

    public double getMaxDeviation()
    {
        return maxDeviation;
    }
    public void setMaxDeviation(double maxDeviation)
    {
        double oldValue = this.maxDeviation;
        this.maxDeviation = maxDeviation;
        firePropertyChange("maxDeviation", oldValue, maxDeviation);
    }

    protected SimulationEngine engine;

    @Override
    public String test(SimulationResult simulationResult, SimulationEngine engine)
    {
        this.engine = engine;

        if( getExperimentPath() == null )
        {
            status = Status.ERROR;
            return "The experiment path is empty.";
        }
        DataElement experiment = getExperimentPath().optDataElement();
        if( ! ( experiment instanceof TableDataCollection ) )
        {
            status = Status.ERROR;
            return "The selected experiment is not a table element.";
        }
        if( experimentVariable == null )
        {
            status = Status.ERROR;
            return "The experiment column is not specified.";
        }
        if( resultVariable == null )
        {
            status = Status.ERROR;
            return "The simulation result variable is not specified.";
        }

        try
        {
            TableDataCollection tdc = (TableDataCollection)experiment;
            double[] times = TableDataCollectionUtils.getColumn(tdc, "time");
            simulationResult.getTimes();//compactify call
            double[][] values = simulationResult.interpolateLinear(times);

            String newVarName = engine.getVariableCodeName(resultVariable.getSubDiagramName(), resultVariable.getName());
            int srIndex = simulationResult.getVariableMap().get(newVarName);

            double[] variableValues = new double[times.length];
            for( int i = 0; i < times.length; ++i )
            {
                variableValues[i] = values[i][srIndex];
            }

            tableSupport.calculateWeights(true, null);
            double distance = tableSupport.getDistance(variableValues, experimentVariable, relativeTo, null, variableValues.length);

            if( distance > maxDeviation )
            {
                status = Status.ERROR;
                return "The deviation between the table data and the simulation result is greater than the maximum allowable deviation.";
            }
        }
        catch( Exception e )
        {
            status = Status.ERROR;
            return e.getMessage();
        }

        status = Status.SUCCESS;
        return null;
    }

    @Override
    public String getInfo()
    {
        StringBuffer result = new StringBuffer("Experiment: ");
        if( getExperimentPath() != null )
        {
            result.append(getExperimentPath().getName());
            result.append(".");
            result.append(experimentVariable);
            result.append(" = ");
            result.append(resultVariable);
        }
        result.append(" (");

        String dataType;
        if( relativeTo == -1 )
            dataType = "Exact values";
        else
        {
            dataType = "Relative values";
        }

        result.append(dataType);
        result.append(",");
        result.append(getWeightMethod());
        result.append(")");
        return result.toString();
    }

    @Override
    public String generateJavaScript(TestModel model)
    {
        SimulationResult result = model.getSimulationResult(this);
        if( result == null )
            return null;

        double[] times = result.getTimes();

        String var = resultVariable.getName();
        if( this.engine != null )
            var = engine.getVariableCodeName(resultVariable.getSubDiagramName(), var);

        double[] values = result.getValues(new String[] {var})[0];

        StringBuffer code = new StringBuffer("plot('Time','Value',");
        code.append(Arrays.toString(times));

        code.append(",{name:'");
        code.append(resultVariable);
        code.append("', type:'line'");
        code.append(", values:");
        code.append(Arrays.toString(values));
        code.append("}");

        DataElement experiment = getExperimentPath().optDataElement();
        if( experiment instanceof TableDataCollection )
        {
            try
            {
                TableDataCollection tdc = (TableDataCollection)experiment;
                double[] xValues = TableDataCollectionUtils.getColumn(tdc, "time");
                double[] yValues = TableDataCollectionUtils.getColumn(tdc, experimentVariable);

                if( relativeTo != -1 )
                {
                    double[][] intMatrix = result.interpolateLinear(xValues);
                    double baseValue = intMatrix[relativeTo][result.getVariableMap().get(var)];

                    for( int i = 0; i < yValues.length; ++i )
                    {
                        yValues[i] *= baseValue / 100;
                    }
                }

                code.append(",{name:'");
                code.append(experimentVariable);
                code.append("', type:'experiment'");
                code.append(", values:{x:");
                code.append(Arrays.toString(xValues));
                code.append(",y:");
                code.append(Arrays.toString(yValues));
                code.append("}}");
            }
            catch( Exception e )
            {
            }
        }

        code.append(")");
        return code.toString();
    }

    //
    //Serialization
    //

    @Override
    public void loadFromString(String string)
    {
        String[] tokens = string.split(DELIMITER);
        if( tokens.length >= 6 )
        {
            String token = tokens[0];
            if( token.length() > 0 )
                setExperimentPath(DataElementPath.create(token));
            experimentVariable = tokens[1];

            String str = tokens[2];
            if( str.trim().length() > 0 )
                resultVariable.setContent(str);

            relativeTo = Integer.parseInt(tokens[3]);
            setWeightMethod(tokens[4]);
            maxDeviation = Double.parseDouble(tokens[5]);
        }
    }

    @Override
    public String toString()
    {
        StringBuffer result = new StringBuffer();
        result.append( ( getExperimentPath() == null ) ? "" : getExperimentPath().toString());
        result.append(DELIMITER);
        result.append(experimentVariable);
        result.append(DELIMITER);
        result.append(resultVariable.toString());
        result.append(DELIMITER);
        result.append(relativeTo);
        result.append(DELIMITER);
        result.append(getWeightMethod());
        result.append(DELIMITER);
        result.append(maxDeviation);
        return result.toString();
    }
}
