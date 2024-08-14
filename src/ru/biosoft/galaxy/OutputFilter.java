package ru.biosoft.galaxy;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.galaxy.parameters.ArrayParameter;
import ru.biosoft.galaxy.parameters.ConditionalParameter;
import ru.biosoft.galaxy.parameters.Parameter;
import ru.biosoft.util.StreamGobbler;
import ru.biosoft.util.TempFiles;

@CodePrivilege(CodePrivilegeType.LAUNCH)
public class OutputFilter
{

    protected static final Logger log = Logger.getLogger(OutputFilter.class.getName());

    private ParametersContainer parameters;

    private String currentNamespace;
    //Evaluation results for current namespace (map of expression to it's result of evaluation)
    private Map<String, Boolean> evaluationResults;

    public OutputFilter(ParametersContainer parameters)
    {
        this.parameters = parameters;
    }

    public synchronized boolean isHiddenByFilter(Parameter param)
    {
        if( !param.isOutput() )
            return false;
        List<String> filterExpressions = (List<String>)param.getAttributes().get(MethodInfoParser.FILTER_EXPRESSIONS_ATTR);
        if( filterExpressions.isEmpty() )
            return false;

        String namespace = getParametersAsJSON().toString();
        if( !namespace.equals(currentNamespace) )
        {
            currentNamespace = namespace;
            try
            {
                evaluateExpressions();
            }
            catch( Exception e )
            {
                log.log(Level.WARNING, "Can not evaluate filter expressions: ", e);
                currentNamespace = null;
            }
        }

        for( String expr : filterExpressions )
        {
            Boolean passFilter = evaluationResults.get(expr);
            if( passFilter == null )
                continue;
            if( !passFilter )
                return true;
        }

        return false;
    }

    private JSONObject getParametersAsJSON()
    {
        JSONObject jsonParam = new JSONObject();
        for( Map.Entry<String, Parameter> entry : parameters.entrySet() )
        {
            try
            {
                jsonParam.put(entry.getKey(), convertParameterToJSON(entry.getValue()));
            }
            catch( JSONException e )
            {
            }
        }
        return jsonParam;
    }

    private static Object convertParameterToJSON(Parameter p) throws JSONException
    {
        if( p instanceof ArrayParameter )
        {
            JSONArray jsonArray = new JSONArray();
            for( Map<String, Parameter> listElement : ( (ArrayParameter)p ).getValues() )
            {
                JSONObject innerElement = new JSONObject();
                for( Map.Entry<String, Parameter> entry : listElement.entrySet() )
                {
                    innerElement.put(entry.getKey(), convertParameterToJSON(entry.getValue()));
                }
                jsonArray.put(innerElement);
            }
            return jsonArray;
        }
        else if( p instanceof ConditionalParameter )
        {
            ConditionalParameter cp = (ConditionalParameter)p;
            JSONObject jsonObj = new JSONObject();
            jsonObj.put(cp.getKeyParameterName(), cp.getKeyParameter().toString());
            Map<String, Parameter> whenParameters = cp.getWhenParameters(cp.getKeyParameter().toString());
            for( Map.Entry<String, Parameter> entry : whenParameters.entrySet() )
            {
                jsonObj.put(entry.getKey(), convertParameterToJSON(entry.getValue()));
            }
            return jsonObj;
        }
        else
        {
            Object result = null;
            if( p.getParameterFields().containsKey("value") )
                result = p.getParameterFields().get("value");
            else
                result = p.toString();
            return result == null ? JSONObject.NULL : result;
        }
    }
    private void evaluateExpressions() throws Exception
    {

        Set<String> expressions = new HashSet<>();
        for( Parameter p : parameters.values() )
            if( p.isOutput() )
            {
                List<String> filterExpressions = (List<String>)p.getAttributes().get(MethodInfoParser.FILTER_EXPRESSIONS_ATTR);
                for( String expr : filterExpressions )
                    expressions.add(expr);
            }

        JSONArray expressionsJSON = new JSONArray(expressions);

        GalaxyFactory.initConstants();

        File namespaceFile = TempFiles.file("galaxynamespace.json", currentNamespace+"\n");
        File expressionsFile = TempFiles.file("galaxyfilter.txt", expressionsJSON+"\n");

        String[] command = new String[] {"python", new File(GalaxyFactory.getScriptPath(), GalaxyFactory.PYTHON_FILTER_FILE).getAbsolutePath(),
                expressionsFile.getAbsolutePath(), namespaceFile.getAbsolutePath()};

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        GalaxyFactory.setupPythonEnvironment( processBuilder.environment() );
        processBuilder.directory(GalaxyFactory.getScriptPath());
        Process proc = processBuilder.start();
        StreamGobbler inputReader = new StreamGobbler(proc.getInputStream(), true);
        StreamGobbler errorReader = new StreamGobbler(proc.getErrorStream(), true);
        proc.waitFor();
        namespaceFile.delete();
        expressionsFile.delete();


        evaluationResults = new HashMap<>();
        if( proc.exitValue() == 0 )
        {
            JSONArray evaluationResultsJSON = new JSONArray(inputReader.getData());
            for( int i = 0; i < evaluationResultsJSON.length(); i++ )
                evaluationResults.put(expressionsJSON.getString(i), evaluationResultsJSON.getBoolean(i));
        }
        else
            throw new Exception("Evaluate filter expressions: " + errorReader.getData());
    }

}
