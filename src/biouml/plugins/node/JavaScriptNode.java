package biouml.plugins.node;

import org.apache.commons.lang.StringEscapeUtils;
import org.mozilla.javascript.NativeObject;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.script.NullScriptEnvironment;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.plugins.javascript.Global;
import ru.biosoft.plugins.javascript.JSElement;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.tasks.process.MachineResources;
import ru.biosoft.util.RhinoUtils;
import biouml.plugins.node.NodeLauncher.ProcessMonitor;

/**
 * @author lan
 *
 */
public class JavaScriptNode extends JavaScriptHostObjectBase
{
    public void run(String path) throws Exception
    {
        run(path, null, false);
    }
    
    public ProcessMonitor run(String path, boolean background) throws Exception
    {
        return run(path, null, background);
    }
    
    public void run(String path, NativeObject inputVariables) throws Exception
    {
        run(path, inputVariables, false);
    }
    
    public ProcessMonitor run(String path, NativeObject inputVariables, boolean background) throws Exception
    {
        return run( path, inputVariables, background, null );
    }
    
    public ProcessMonitor run(String path, NativeObject inputVariables, boolean background, MachineResources resources) throws Exception
    {
        ScriptEnvironment environment = Global.getEnvironment();
        if(environment == null)
            environment = new NullScriptEnvironment();
        String script = DataElementPath.create(path).getDataElement(JSElement.class).getContent();
        if(inputVariables != null)
        {
            script = "Input = " + RhinoUtils.toJSONObject(inputVariables).toString(2) + ";" + script;
        }
        if(background)
            return NodeLauncher.runScriptBackground(script, environment, null, resources);
        NodeLauncher.runScript(script, environment, null, resources);
        return null;
    }
    
    public void analyze(String analysisName) throws Exception
    {
        analyze(analysisName, null, false);
    }
    
    public void analyze(String analysisName, NativeObject parameters) throws Exception
    {
        analyze(analysisName, parameters, false);
    }
    
    public ProcessMonitor analyze(String analysisName, boolean background) throws Exception
    {
        return analyze(analysisName, null, background);
    }
    
    public ProcessMonitor analyze(String analysisName, NativeObject parameters, boolean background) throws Exception
    {
        return analyze( analysisName, parameters, background, null );
    }
    
    public ProcessMonitor analyze(String analysisName, NativeObject parameters, boolean background, MachineResources resources) throws Exception
    {
        String script = "microarray.getAnalysis('" + StringEscapeUtils.escapeJavaScript(analysisName) + "')\n("
                + ( parameters == null ? "{}" : RhinoUtils.toJSONObject(parameters).toString(2) ) + ", true)";
        ScriptEnvironment environment = Global.getEnvironment();
        if(environment == null)
            environment = new NullScriptEnvironment();
        if(background)
            return NodeLauncher.runScriptBackground(script, environment, null, resources);
        NodeLauncher.runScript(script, environment, null, resources);
        return null;
    }
}
