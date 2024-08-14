package biouml.plugins.agentmodeling;

import java.util.logging.Level;
import java.util.Map.Entry;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import ru.biosoft.access.BiosoftSecurityManager;
import ru.biosoft.plugins.javascript.JScriptContext;
import biouml.plugins.simulation.Span;

/**
 * 
 * @author Ilya
 * Agent performs java script code
 */
public class JavaScriptAgent extends ScriptAgent
{  
    private Script prepared;
    private Script preparedInitial;
    ImporterTopLevel scope;
    
    public JavaScriptAgent(String name, Span span, String expression, String initialExcpression)
    {
        super(name, span);
        Context context = JScriptContext.getContext();
        prepared = context.compileString(expression, "", 1, null);
        preparedInitial = context.compileString(initialExcpression, "", 1, null);
    }
    
    @Override
    public void applyChanges()
    {
        try
        {
            Context context = JScriptContext.getContext();
            scope = JScriptContext.getScope();
            defineVariableValues();
            BiosoftSecurityManager.runInSandbox( () -> {
                preparedInitial.exec(context, scope);
            } );
            retrieveVariableValues();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Agent " + name + " failed during initialization: " + e.getMessage());
            isAlive = false;
        }
    }

    @Override
    public void iterate()
    {
        try
        {
            Context context = JScriptContext.getContext();
            scope = JScriptContext.getScope();
            defineVariableValues();
            BiosoftSecurityManager.runInSandbox( () -> {
                prepared.exec(context, scope);    
            } );
            retrieveVariableValues();
            
            super.iterate();
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Agent "+name+" failed at time "+ currentTime +": "+ex.getMessage());
            isAlive = false;
        }
    }
    
    @Override
    public void defineVariableValues()
    {
        Scriptable buck = new BuckScriptable();
        scope.put("$", scope, buck);
        for( Entry<String, Double> entry : variables.entrySet() )
        {
            String varName = entry.getKey();
            double val = Context.toNumber(entry.getValue());
            buck.put(varName, buck, val);
        }
    }

    @Override
    public void retrieveVariableValues() throws Exception
    {
        Scriptable buck = (Scriptable)scope.get("$", scope);
        for( Entry<String, Double> entry : variables.entrySet() )
        {
            Object obj = buck.get(entry.getKey(), buck);
            Double val;
            if( obj instanceof Integer )
                val = ( (Integer)obj ).doubleValue();
            else if( obj instanceof Double )
                val = ( (Double)obj );
            else
                throw new Exception("Incorrect result of agent: " + this.name + " for variable " + entry.getKey() + ", value: " + obj);
            entry.setValue(val);
        }
    }

    private final static class BuckScriptable extends ScriptableObject
    {
        private static final long serialVersionUID = 1L;
        @Override
        public String getClassName()
        {
            return "Variables";
        }
    }
}
