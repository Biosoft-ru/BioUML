package biouml.standard.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import one.util.streamex.EntryStream;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

/**
 * @author lan
 *
 */
public class ScriptDataGenerator extends SimulationDataGeneratorSupport
{
    private String name;
    private DataCollection<?> origin;
    private Script script;
    private Context context;
    private Scriptable scope;
    private String scriptSource;
    private Map<SimulationResult, List<ScriptVariable>> results = new HashMap<>();
    private int nPoints = 0;
    private Scriptable buck;
    private Map<String, Object> parameters = new HashMap<>();
    public static final String VARIABLES_PROPERTY = "variables";
    public static final String SCRIPT_PROPERTY = "script";
    public static final String PARAMETERS_PROPERTY = "parameters";
    
    public ScriptDataGenerator(DataCollection<?> origin, String name)
    {
        this(origin, name, null);
    }
    
    public ScriptDataGenerator(DataCollection<?> origin, String name, String script)
    {
        this.origin = origin;
        this.name = name;
        context = Context.enter();
        scope = new ImporterTopLevel();
        buck = new NativeObject();
        scope.put("$", scope, buck);
        setScript(script);
    }
    
    public ScriptDataGenerator(String jsonStr)
    {
        try
        {
            JSONObject json = new JSONObject(jsonStr);
            fromJSON(json);
        }
        catch( JSONException e )
        {
        }
    }
    
    public void setScript(String script)
    {
        this.scriptSource = script;
        if(script == null)
            this.script = null;
        else
            this.script = context.compileString(script, "", 1, null);
    }
    
    public String getScript()
    {
        return scriptSource;
    }

    /**
     * Add variable which will be available for DataGenerator during the calculation
     * To access it use $[name][point]
     * @param name name for variable to access from the formula
     * @param result SimulationResult for which the variable is defined
     * @param ref Reference to the variable in SimulationResult (null = time)
     * @param skipPoints how many simulation points to skip for this variable
     * @throws Exception if reference is invalid
     */
    public void addVariable(String name, SimulationResult result, String ref, int skipPoints) throws Exception
    {
        if(!results.containsKey(result))
        {
            results.put(result, new ArrayList<ScriptVariable>());
            new Listener(result);
        }
        Integer idx;
        if( ref == null )
            idx = -1;
        else if( result.getVariableMap() == null )
            idx = null;
        else
            idx = result.getVariableMap().get(ref);
        if(idx == null) throw new Exception("Unknown reference "+ref);
        ScriptVariable variableInfo = new ScriptVariable(name, idx, result, skipPoints);
        results.get(result).add(variableInfo);
        buck.put(name, buck, Context.toObject(variableInfo, buck));
        if(script == null) setScript("$['"+StringEscapeUtils.escapeJavaScript(name)+"'][point]");
        int count = result.getCount();
        for(int i=skipPoints; i<count; i++)
        {
            if(variableInfo.getIdx() == -1) variableInfo.addValue(result.getTime(i));
            else variableInfo.addValue(result.getValue(i)[variableInfo.getIdx()]);
        }
    }
    
    /**
     * Add parameter which will be available for DataGenerator during the calculation
     * To access it use $[name][any point]
     */
    public void addParameter(String name, Object value)
    {
        ScriptParameter parameter = new ScriptParameter(value);
        parameters.put(name, value);
        buck.put(name, buck, Context.toObject(parameter, buck));
    }
    
    private void add(double t, double[] y, SimulationResult result)
    {
        List<ScriptVariable> list = results.get(result);
        if(list == null) return;
        for(ScriptVariable var: list)
        {
            if(var.getSkipPoints() >= result.getCount()) continue;
            if(var.getIdx() == -1) var.addValue(t);
            else var.addValue(y[var.getIdx()]);
        }
        int oldNPoints = nPoints;
        nPoints = Integer.MAX_VALUE;
        for(List<ScriptVariable> list2 : results.values())
        {
            for(ScriptVariable var: list2)
            {
                nPoints = Math.min(nPoints, var.count());
            }
        }
        if(oldNPoints != nPoints)
        {
            notifyListeners();
        }
    }

    @Override
    public int getPointsCount()
    {
        return nPoints;
    }
    
    @Override
    public double getValue(int point) throws Exception
    {
        scope.put("point", scope, point);
        return (Double)script.exec(context, scope);
    }

    /**
     * @return the name
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * @return the origin
     */
    @Override
    public DataCollection<?> getOrigin()
    {
        return origin;
    }
    
    
    public Set<SimulationResult> getSimulationResults()
    {
        return results.keySet();
    }
    
    public List<ScriptVariable> getVariables(SimulationResult result)
    {
        return results.get(result);
    }
    
    private class Listener implements ResultListener
    {
        private SimulationResult result;

        public Listener(SimulationResult result)
        {
            this.result = result;
            result.addResultListener(this);
        }
        
        @Override
        public void start(Object model)
        {
        }

        @Override
        public void add(double t, double[] y) throws Exception
        {
            ScriptDataGenerator.this.add(t, y, result);
        }
    }
    
    public Map<String, Object> getParameters()
    {
        return parameters;
    }
    
    @Override
    public String toString()
    {
        try
        {
            JSONObject jsonObj = toJSON();
            return jsonObj.toString();
        }
        catch( JSONException e )
        {
        }
        return "";
    }
    
    public void fromJSON(JSONObject json) throws JSONException
    {
        name = json.getString("name");
        if(json.has("origin"))
        {
            origin = CollectionFactory.getDataCollection(json.getString("origin"));
        }
        
        context = Context.enter();
        scope = new ImporterTopLevel();
        buck = new NativeObject();
        scope.put("$", scope, buck);
        
        setScript(json.getString("script"));
        
        if( json.has("parameters") )
        {
            addParametersFromJSONArray(json.getJSONArray("parameters"));
        }
        if(json.has("variables"))
        {
            addVariablesFromJSONArray(json.getJSONArray("variables"));
        }
    }
    
    public JSONObject toJSON() throws JSONException
    {
        JSONObject json = new JSONObject();
        json.put("name", name);
        if(origin != null)
            json.put("origin", origin.getCompletePath().toString());
        json.put("script", getScript());
        
        if( parameters != null )
        {
            JSONArray params = getParametersJSONArray();
            json.put("parameters", params);
        }
        
        if(results != null)
        {
            JSONArray varArray = getVariablesJSONArray();
            json.put("variables", varArray);
        }
        return json;
    }
    
    

    private void addVariablesFromJSONArray(JSONArray array) throws JSONException
    {
        addVariablesFromJSONArray(array, null);
    }
    
    /**
     * Add variables for specified simulation result only
     * @param array - JSONArray of variables [[name, SimulationResult path, name in result, skipPoints]]
     * @param result - SimulationResultObject
     * @throws JSONException
     */
    private void addVariablesFromJSONArray(JSONArray array, SimulationResult result) throws JSONException
    {

        for( int i = 0; i < array.length(); i++ )
        {
            JSONArray v = array.getJSONArray(i);
            try
            {
                DataElementPath resultPath = DataElementPath.create(v.getString(1));
                //TODO: correct result check, not by name
                if( result != null && ! ( result.getName().equals(resultPath.getName()) ) )
                    continue;
                else if( result == null )
                {
                    DataElement de = resultPath.optDataElement();
                    if( de instanceof SimulationResult )
                    {
                        result = (SimulationResult)de;
                    }
                }
                if( result != null )
                {
                    String ref = v.getString(2);
                    if( ref.equals("null") )
                        ref = null;
                    addVariable(v.getString(0), result, ref, v.getInt(3));
                }

            }
            catch( Exception e )
            {
            }
        }
    }
    
    public void addVariablesFromString(String arrayStr) throws JSONException
    {
        JSONArray array = new JSONArray(arrayStr);
        addVariablesFromJSONArray(array);
    }
    
    public void addVariablesFromString( String arrayStr, SimulationResult result) throws JSONException
    {
        JSONArray array = new JSONArray(arrayStr);
        addVariablesFromJSONArray(array, result);
    }
    
    private JSONArray getVariablesJSONArray()
    {
        JSONArray varArray = new JSONArray();
        Set<SimulationResult> results = getSimulationResults();

        for( SimulationResult res : results )
        {
            String resultName =  DataElementPath.create(res).toString();
            
            Map<String, Integer> varMap = res.getVariableMap();
            Map<Integer, String> revVarMap = EntryStream.of(varMap).invert().toMap();
            for( ScriptVariable var : getVariables(res) )
            {
                JSONArray v = new JSONArray();
                v.put(var.getName());
                String nameInResult = revVarMap.get(var.getIdx());
                v.put(resultName);
                v.put(nameInResult);
                v.put(var.getSkipPoints());
                varArray.put(v);
            }
        }
        return varArray;
    }
    
    public String getVariablesAsString()
    {
        return getVariablesJSONArray().toString();
    }
    
    private JSONArray getParametersJSONArray()
    {
        JSONArray params = new JSONArray();
        for( Map.Entry<String, Object> entry : parameters.entrySet() )
        {
            JSONArray p = new JSONArray();
            p.put(entry.getKey());
            p.put(entry.getValue());
            params.put(p);
        }
        return params;
    }
    
    public String getParametersString()
    {
        return getParametersJSONArray().toString();
    }
    
    private void addParametersFromJSONArray(JSONArray params) throws JSONException
    {
        for(int i = 0; i < params.length(); i++)
        {
            JSONArray p = params.getJSONArray(i);
            addParameter(p.getString(0), p.get(1));
        }
    }
    
    public void addParametersFromString( String paramsStr) throws JSONException
    {
        addParametersFromJSONArray(new JSONArray(paramsStr));
    }
}
