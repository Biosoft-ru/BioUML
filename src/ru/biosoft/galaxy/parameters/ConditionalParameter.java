package ru.biosoft.galaxy.parameters;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ru.biosoft.galaxy.ParametersContainer;

/**
 * 'conditional' Galaxy tool parameter support
 */
public class ConditionalParameter extends ParameterSupport
{
    protected String keyParameterName;
    protected Parameter keyParameter;
    protected Map<String, Map<String, Parameter>> whenParameters;

    public ConditionalParameter(boolean output)
    {
        super(output);
        whenParameters = new LinkedHashMap<>();
    }

    public String getKeyParameterName()
    {
        return keyParameterName;
    }

    public void setKeyParameterName(String keyParameterName)
    {
        this.keyParameterName = keyParameterName;
    }

    public Parameter getKeyParameter()
    {
        return keyParameter;
    }

    public void setKeyParameter(Parameter keyParameter)
    {
        this.keyParameter = keyParameter;
    }

    public void addKeyValue(String key)
    {
        whenParameters.put(key, new LinkedHashMap<String, Parameter>());
    }

    public void addParameter(String key, String name, Parameter value)
    {
        whenParameters.get(key).put(name, value);
    }

    public Set<String> getWhenSet()
    {
        return whenParameters.keySet();
    }

    public Map<String, Parameter> getWhenParameters(String key)
    {
        return whenParameters.get(key);
    }

    public boolean containsProperty(String name)
    {
        if( keyParameterName.equals(name) )
            return true;

        for( Map<String, Parameter> map : whenParameters.values() )
        {
            if( map.containsKey(name) )
                return true;
        }

        return false;
    }

    public void setParameterValue(String name, String value)
    {
        if( keyParameterName.equals(name) )
        {
            keyParameter.setValue(value);
            return;
        }

        for( Map<String, Parameter> map : whenParameters.values() )
        {
            if( map.containsKey(name) )
            {
                map.get(name).setValue(value);
            }
        }
    }

    @Override
    protected void doCloneParameter(ParameterSupport clone)
    {
        super.doCloneParameter(clone);
        ConditionalParameter result = (ConditionalParameter)clone;
        result.setKeyParameterName(keyParameterName);
        result.setKeyParameter(keyParameter.cloneParameter());
        for( Map.Entry<String, Map<String, Parameter>> whenParamEntry : whenParameters.entrySet() )
        {
            String key = whenParamEntry.getKey();
            result.addKeyValue(key);
            for( Map.Entry<String, Parameter> entry : whenParamEntry.getValue().entrySet() )
            {
                result.addParameter(key, entry.getKey(), entry.getValue().cloneParameter());
            }
        }
    }

    @Override
    public Parameter cloneParameter()
    {
        ConditionalParameter result = new ConditionalParameter(output);
        doCloneParameter(result);
        return result;
    }

    @Override
    public void setContainer(ParametersContainer container)
    {
        super.setContainer(container);
        if(keyParameter != null)
            keyParameter.setContainer(container);
        for(Map<String,Parameter> whenGroup: whenParameters.values())
        {
            for(Parameter parameter: whenGroup.values())
                parameter.setContainer(container);
        }
    }
}
