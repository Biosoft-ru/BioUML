package ru.biosoft.math;

import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptJobControl;

public class MathScript extends ScriptDataElement
{
    public MathScript(DataCollection<?> origin, String name, String content)
    {
        super( name, origin, content );
    }
    
    public MathScript(String name, DataCollection<?> origin)
    {
        super( name, origin);
    }
    
    @Override
    protected ScriptJobControl createJobControl(String content, ScriptEnvironment env, Map<String, Object> scope,
            Map<String, Object> outVars, boolean sessionContext)
    {
        return new MathScriptJobControl( content, scope, outVars, env );
    }

}
