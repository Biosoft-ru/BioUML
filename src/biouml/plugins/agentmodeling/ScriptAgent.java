package biouml.plugins.agentmodeling;

import java.util.HashMap;
import java.util.Map;

import one.util.streamex.StreamEx;

import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import biouml.plugins.simulation.Span;

/**
 * 
 * @author Ilya
 *
 * Agent performing some script
 */
public abstract class ScriptAgent extends SimulationAgent
{
    public final static String JAVA_SCRIPT_TYPE = "javascript";
    public final static String SCRIPT_AGENT = "ScriptAgent";
    public final static String SCRIPT_TYPE = "ScriptType";
    public static final String SCRIPT_INITIAL = "ScriptInitial";
    public static final String SCRIPT = "Script";
    public static final String SCRIPT_RESULT = "ScriptResult";
    
    private int spanIndex = 0;

    protected ScriptAgent(String name, Span span)
    {
        super(name, span);
        variables = new HashMap<>();
        variables.put("time", Double.valueOf(0));
        currentTime = span.getTime(spanIndex);
    }


    public static ScriptAgent createScriptAgent(String name, Span span, String script, String initialize, String scriptType, String scriptResult) throws Exception
    {
        switch( scriptType )
        {
            case JAVA_SCRIPT_TYPE:
            {
                return new JavaScriptAgent(name, span, script, initialize);
            }
            default:
                throw new Exception("Unknown script agent type: " + scriptType + " for agent " + name);
        }
    }

    @Override
    public void iterate()
    {
        spanIndex++;
        if( spanIndex >= span.getLength() )
            isAlive = false;
        else
        {
            currentTime = span.getTime(spanIndex);
            variables.put("time", currentTime);
        }
    }

    /**
     * Sets variable values to script context
     * @throws Exception
     */
    public abstract void defineVariableValues() throws Exception;

    /**
     * Gets variable values from script context
     * @throws Exception
     */
    public abstract void retrieveVariableValues() throws Exception;


    protected Map<String, Double> variables;

    @Override
    public void addVariable(String name)
    {
        addVariable(name, 0);
    }

    @Override
    public void addVariable(String name, double initialValue)
    {
        variables.put(name, initialValue);
    }


    @Override
    public boolean containsVariable(String name)
    {
        return variables.containsKey(name);
    }

    @Override
    public void setCurrentValue(String name, double value) throws Exception
    {
        variables.put(name, value);
    }

    @Override
    public double getPriority()
    {
        return DEFAULT_AGENT_PRIORITY;
    }

    @Override
    public double getCurrentValue(String name) throws Exception
    {
        return variables.get(name).doubleValue();
    }


    @Override
    public double[] getCurrentValues()
    {
        return StreamEx.ofValues( variables ).mapToDouble( x -> x ).toArray();
    }

    @Override
    public String[] getVariableNames()
    {
        return variables.keySet().toArray(new String[variables.size()]);
    }

    @Override
    public double[] getUpdatedValues() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void setUpdated()
    {
        // TODO Auto-generated method stub

    }

    public static class ScriptTypeEditor extends GenericComboBoxEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            return new String[] {JAVA_SCRIPT_TYPE};
        }
    }
}
