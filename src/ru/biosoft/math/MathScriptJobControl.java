package ru.biosoft.math;

import java.util.ArrayList;
import java.util.Map;

import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptJobControl;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.model.Utils.ParseException;
import ru.biosoft.math.model.Utils.UnresolvedVariablesException;

import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

public class MathScriptJobControl extends ScriptJobControl
{
    private final String expression;
    private final Map<String, Object> scope;
    private final Map<String, Object> outVars;
    private final  ScriptEnvironment env;
    
    private Object result;

    public MathScriptJobControl(String expression, Map<String, Object> scope, Map<String, Object> outVars, ScriptEnvironment env)
    {
        super();
        this.expression = expression;
        this.scope = scope;
        this.outVars = outVars;
        this.env = env;
    }

    @Override
    public String getResult()
    {
        return String.valueOf( result );
    }

    @Override
    protected void doRun() throws JobControlException
    {
        try
        {
            result = Utils.evaluateExpression( expression, scope );
            env.print( getResult() );
        }
        catch( UnresolvedVariablesException | ParseException e )
        {
            throw new JobControlException( JobControl.TERMINATED_BY_ERROR, e.getMessage() );
        }
        
        for( String var : new ArrayList<>( outVars.keySet() ) )
            outVars.put( var, result );
    }
}
