package biouml.plugins.test.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import one.util.streamex.StreamEx;

import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.test.Status;
import biouml.plugins.test.TestModel;
import biouml.standard.simulation.SimulationResult;

public class IntervalTest extends Test
{
    private TestVariable[] variables;
    private double valueFrom;
    private double valueTo;

    private long from;
    private long to;

    public TestVariable[] getVariables()
    {
        return variables;
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

    public double getValueFrom()
    {
        return valueFrom;
    }

    public void setValueFrom(double valueFrom)
    {
        double oldValue = this.valueFrom;
        this.valueFrom = valueFrom;
        firePropertyChange("valueFrom", oldValue, valueFrom);
    }

    public double getValueTo()
    {
        return valueTo;
    }

    public void setValueTo(double valueTo)
    {
        double oldValue = this.valueTo;
        this.valueTo = valueTo;
        firePropertyChange("valueTo", oldValue, valueTo);
    }

    public long getFrom()
    {
        return from;
    }

    public long getTo()
    {
        return to;
    }
    
    private SimulationEngine engine;

    @Override
    public String test(SimulationResult result, SimulationEngine engine)
    {
        if( ( to < from ) || ( result.getInitialTime() > from ) || ( result.getCompletionTime() < to ) )
        {
            status = Status.ERROR;
            return "Incorrect time interval";
        }
        this.engine = engine;
        
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
        double[] times = result.getTimes();
        for( int i = 0; i < times.length; i++ )
        {
            if( times[i] >= from && times[i] <= to )
            {
                double[] values = result.getValue(i);
                for( Integer varInd : varInds )
                {
                    int varIndInt = varInd.intValue();
                    if( values[varIndInt] < valueFrom || values[varIndInt] > valueTo )
                    {
                        status = Status.ERROR;
                        return "Incorrect variable value: time=" + times[i];
                    }
                }
            }
        }
        status = Status.SUCCESS;
        return null;
    }
    @Override
    public String getInfo()
    {
        StringBuilder result = new StringBuilder("Interval: from=").append(from).append(", to=").append(to);
        result.append(", interval=[").append(valueFrom).append(",").append(valueTo).append("]");
        if( variables != null )
        {
            result.append( StreamEx.of( variables ).joining( ", ", ", variables=[", "]" ) );
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

        String[] variableNames = SteadyStateTest.getVariableNames(variables, engine);
        
        double[] times = result.getTimes();
        double[][] values = result.getValues(variableNames);
        
      
        
        StringBuffer code = new StringBuffer("plot('Time','Value',");
        code.append(Arrays.toString(times));
        for( int i = 0; i < values.length; i++ )
        {
            addValuesArrayStr(code, "line", variableNames[i], Arrays.toString(values[i]));
        }

        //add valueFrom valueTo lines
        addValuesArrayStr(code, "constant", "From value", Double.toString(valueFrom));
        addValuesArrayStr(code, "constant", "To value", Double.toString(valueTo));

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
        if( tokens.length >= 4 )
        {
            from = Integer.parseInt(tokens[0]);
            to = Integer.parseInt(tokens[1]);
            valueFrom = Double.parseDouble(tokens[2]);
            valueTo = Double.parseDouble(tokens[3]);
            List<TestVariable> vars = new ArrayList<>();
            for( int i = 4; i < tokens.length; i++ )
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
        result.append(valueFrom);
        result.append( DELIMITER );
        result.append( valueTo );
        result.append( DELIMITER );
        if( variables != null )
        {
            for( TestVariable var : variables )
            {
                result.append( var.toString() );
                result.append( DELIMITER );
            }
        }
        return result.toString();
    }

}
