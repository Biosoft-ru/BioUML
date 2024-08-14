package ru.biosoft.galaxy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

import one.util.streamex.EntryStream;

import ru.biosoft.galaxy.parameters.ArrayParameter;
import ru.biosoft.galaxy.parameters.ConditionalParameter;
import ru.biosoft.galaxy.parameters.Parameter;

/**
 * @author lan
 *
 */
public class ParametersContainer extends LinkedHashMap<String, Parameter>
{
    @Override
    public ParametersContainer clone()
    {
        ParametersContainer result = new ParametersContainer();
        for(Map.Entry<String, Parameter> entry : entrySet())
            result.put(entry.getKey(), entry.getValue().cloneParameter());
        return result;
    }

    @Override
    public Parameter put(String key, Parameter value)
    {
        Parameter result = super.put(key, value);
        value.setContainer(this);
        return result;
    }
    
    private static Parameter findParameter(String name, Map<String, Parameter> parameters)
    {
        if( parameters.containsKey(name) )
        {
            return parameters.get(name);
        }
        //array or conditional
        for( Parameter p : parameters.values() )
            if( p instanceof ArrayParameter )
            {
                ArrayParameter arrayParam = (ArrayParameter)p;
                if(arrayParam.getValues().isEmpty())
                {
                    ParametersContainer child = new ParametersContainer();
                    EntryStream.of(arrayParam.getChildTypes()).mapValues(Parameter::cloneParameter).forKeyValue(child::put);
                    Parameter target = findParameter(name, child);
                    if(target != null)
                    {
                        //Very strange line of code, hope that it is never executed
                        ((ArrayParameter)p).getValues().add(child);
                        return target;
                    }
                }
                else
                {
                    Map<String, Parameter> child = arrayParam.getValues().get(0);
                    Parameter target = findParameter(name, child);
                    if(target != null)
                        return target;
                }
            }
            else if(p instanceof ConditionalParameter)
            {
                ConditionalParameter conditionalParam = (ConditionalParameter)p;
                
                Map<String, Parameter> child = new HashMap<>();
                child.put(conditionalParam.getKeyParameterName(), conditionalParam.getKeyParameter());
                Parameter target = findParameter(name, child);
                if(target != null)
                    return target;
                
                child = conditionalParam.getWhenParameters(conditionalParam.getKeyParameter().toString());
                if(child == null)
                    continue;
                
                target = findParameter(name, child);
                if(target != null)
                    return target;
            }
        return null;
    }

    /**
     * Locates parameter by name looking inside of conditional and array parameters also
     * @param name
     * @return found parameter or null if nothing found
     */
    public Parameter getParameter(String name)
    {
        return findParameter(name, this);
    }

    private static Parameter findParameterByPath(LinkedList<String> path, Map<String, Parameter> container)
    {
        if( path.isEmpty() )
            return null;
        String name = path.removeFirst();
        if( path.isEmpty() )
            return container.get(name);
        ConditionalParameter condParam = (ConditionalParameter)container.get(name);
        String value = path.removeFirst();
        if( path.isEmpty() && value.equals(condParam.getKeyParameterName()) )
            return condParam.getKeyParameter();
        return findParameterByPath(path, condParam.getWhenParameters(value));
    }
    
    /**
     * Locates parameter by hierarchical name.
     * The hierarchical name is:
     *   <conditional1-name>|<conditional1-value>|...<conditinal_i-name>|<conditional_i-value>|<param-name>
     * for regular parameter, or
     *   <conditional1-name>|<conditional1-value>|...<conditinal_i-name>|<conditional_i-key-parameter-name>
     * for key parameter of conditional
     */
    public Parameter getParameterByPath(String name)
    {
        return getParameterByPath(name.split(Pattern.quote(GalaxyAnalysisParameters.NESTED_PARAMETER_DELIMETER)));
    }

    public Parameter getParameterByPath(String[] path)
    {
        return findParameterByPath(new LinkedList<>(Arrays.asList(path)), this);
    }
    
    /**
     * Locates parameter by simple path.
     * Unlike path in {@code getParameterByPath() } this path doesn't contain any values of conditional,
     * instead key parameter values are used.
     * The simple path is: 
     *    <conditional1-name>|<conditional2-name>|...<conditinal_i-name>|<param-name>
     * for regular parameter, or
     *    <conditional1-name>|<conditional2-name>|...<conditinal_i-name>|<conditional_i-key-parameter-name>
     * for key parameter of conditional
     * @param path     simple path to parameter
     * @return found parameter or null if nothing found
     */
    public Parameter getParameterBySimplePath(String[] path)
    {
        if( path.length == 0 )
            return null;

        Parameter p = getParameter( path[0] );
        if( path.length == 1 )
            return p;

        if( ! ( p instanceof ConditionalParameter ) )
            return null;
        ConditionalParameter cond = (ConditionalParameter)p;
        for( int i = 1; i < path.length - 1; i++ )
        {
            p = cond.getWhenParameters( cond.getKeyParameter().toString() ).get( path[i] );
            if( ! ( p instanceof ConditionalParameter ) )
                return null;
            cond = (ConditionalParameter)p;
        }

        String lastName = path[path.length - 1];
        if( lastName.equals( cond.getKeyParameterName() ) )
            return cond.getKeyParameter();
        return cond.getWhenParameters( cond.getKeyParameter().toString() ).get( lastName );
    }

    public Parameter getParameterBySimplePath(String path)
    {
        return getParameterBySimplePath( path.split( Pattern.quote( "." ) ) );
    }
}

