package biouml.plugins.test.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.test.Status;
import biouml.plugins.test.TestModel;
import biouml.standard.simulation.SimulationResult;

public class SteadyStateTest extends Test
{
    // variables that should be at steady state
    private TestVariable[] variables;

    private long from;
    private long to;

    private double rTol = 1E-10;

    public TestVariable[] getVariables()
    {
        return variables;
    }

    public long getFrom()
    {
        return from;
    }

    public double getRTol()
    {
        return rTol;
    }

    public void setRTol(double tolerance)
    {
        double oldValue = this.rTol;
        this.rTol = tolerance;
        firePropertyChange("rTol", oldValue, rTol);
    }

    public void setVariables(TestVariable[] variables)
    {
        TestVariable[] oldValue = this.variables;
        this.variables = variables;
        firePropertyChange("variables", oldValue, variables);
    }

    public void setFrom(long from)
    {
        long oldValue = this.from;
        this.from = from;
        firePropertyChange("from", oldValue, from);
    }

    public void setTo(long to)
    {
        long oldValue = this.to;
        this.to = to;
        firePropertyChange("to", oldValue, to);
    }

    public long getTo()
    {
        return to;
    }

    protected SimulationEngine engine;

    @Override
    public String test(SimulationResult result, SimulationEngine engine)
    {
        this.engine = engine;

        if( ( to < from ) || ( result.getInitialTime() > from ) || ( result.getCompletionTime() < to ) )
        {
            status = Status.ERROR;
            return "Incorrect time interval";
        }
        List<Integer> varInds = new ArrayList<>();
        for( TestVariable var : variables )
        {
            String varName = var.getName();
            String newVarName = engine.getVariableCodeName(var.getSubDiagramName(), var.getName());
            Integer ind = result.getVariableMap().get(newVarName);
            if( ind == null )
            {
                status = Status.ERROR;
                return "Cannot find variable: " + varName;
            }
            varInds.add(ind);
        }
        Map<Integer, Double> min = new HashMap<>();
        Map<Integer, Double> max = new HashMap<>();
        double[] times = result.getTimes();
        for( int i = 0; i < times.length; i++ )
        {
            if( times[i] >= from && times[i] <= to )
            {
                double[] values = result.getValue(i);
                for( Integer varInd : varInds )
                {
                    int varIndInt = varInd.intValue();
                    if( !min.containsKey(varInd) )
                    {
                        min.put(varInd, values[varIndInt]);
                    }
                    else
                    {
                        double oldMin = min.get(varInd);
                        if( values[varIndInt] < oldMin )
                            min.put(varInd, values[varIndInt]);
                    }
                    if( !max.containsKey(varInd) )
                    {
                        max.put(varInd, values[varIndInt]);
                    }
                    else
                    {
                        double oldMax = max.get(varInd);
                        if( values[varIndInt] > oldMax )
                            max.put(varInd, values[varIndInt]);
                    }
                }
            }
        }
        for( Integer varInd : varInds )
        {
            if( min.containsKey(varInd) && max.containsKey(varInd) )
            {
                double varMin = min.get(varInd);
                double varMax = max.get(varInd);
                if( Math.abs(varMax - varMin) > rTol * Math.abs(varMax + varMin) )
                {
                    status = Status.ERROR;
                    return "Variable is not in steady state";
                }
            }
        }
        status = Status.SUCCESS;
        return null;
    }

    public static String[] getVariableNames(TestVariable[] vars, SimulationEngine engine)
    {
        String[] result = new String[vars.length];
        for( int i = 0; i < vars.length; i++ )
        {
            if( engine != null )
                result[i] = engine.getVariableCodeName(vars[i].getSubDiagramName(), vars[i].getName());
           else
            result[i] = vars[i].getName();
        }
        return result;
    }

    @Override
    public String getInfo()
    {
        StringBuffer result = new StringBuffer("Steady state: from=");
        result.append(from);
        result.append(", to=");
        result.append(to);
        if( variables != null )
        {
            result.append(", variables=[");
            for( int i = 0; i < variables.length; i++ )
            {
                if( i > 0 )
                    result.append(", ");
                result.append(variables[i]);
            }
            result.append("]");
        }
        return result.toString();
    }

    @Override
    public String generateJavaScript(TestModel model)
    {
        SimulationResult result = model.getSimulationResult(this);
        if( result == null )
            return null;

        if( variables == null || variables.length == 0 )
            return null;

        String[] variableNames = getVariableNames(variables, engine);

        double[] times = result.getTimes();
        double[][] values = result.getValues(variableNames);

        int timePoint = 0;
        for( int i = 0; i < times.length; i++ )
        {
            if( times[i] >= from )
            {
                timePoint = i;
                break;
            }
        }

        StringBuffer code = new StringBuffer("plot('Time','Value',");
        code.append(Arrays.toString(times));
        for( int i = 0; i < values.length; i++ )
        {
            if( values[i] != null )
            {
                addValuesArrayStr(code, "constant", variableNames[i] + "_base line", Double.toString(values[i][timePoint]));
                addValuesArrayStr(code, "line", variableNames[i], Arrays.toString(values[i]));
            }
        }

        code.append(")");
        return code.toString();
    }

    protected void addValuesArrayStr(StringBuffer code, String type, String title, String values)
    {
        code.append(",{name:'");
        code.append(title);
        code.append("', type:'");
        code.append(type);
        code.append("', values:");
        code.append(values);
        code.append("}");
    }

    //
    //Serialization
    //

    @Override
    public void loadFromString(String string)
    {
        String[] tokens = string.split(DELIMITER);
        if( tokens.length >= 2 )
        {
            from = Integer.parseInt(tokens[0]);
            to = Integer.parseInt(tokens[1]);
            List<TestVariable> vars = new ArrayList<>();
            for( int i = 2; i < tokens.length; i++ )
            {
                String str = tokens[i];
                if( str.trim().length() > 0 )
                {
                    try
                    {
                        vars.add(new TestVariable(str));
                    }
                    catch( Exception ex )
                    {

                    }
                }
            }
            variables = vars.toArray(new TestVariable[vars.size()]);
        }
    }

    @Override
    public String toString()
    {
        StringBuffer result = new StringBuffer();
        result.append(from);
        result.append(DELIMITER);
        result.append(to);
        result.append(DELIMITER);
        if( variables != null )
        {
            for( TestVariable var : variables )
            {
                result.append(var.toString());
                result.append(DELIMITER);
            }
        }
        return result.toString();
    }
}
