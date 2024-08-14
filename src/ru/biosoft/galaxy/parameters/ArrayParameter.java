package ru.biosoft.galaxy.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import one.util.streamex.EntryStream;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import ru.biosoft.galaxy.ParametersContainer;

/**
 * Repeat parameter support
 */
public class ArrayParameter extends ParameterSupport
{
    protected Map<String, Parameter> childTypes = null;
    protected List<ParametersContainer> values = null;

    public ArrayParameter(boolean output)
    {
        super(output);
    }

    public Map<String, Parameter> getChildTypes()
    {
        if( childTypes == null )
        {
            childTypes = new HashMap<>();
        }
        return childTypes;
    }

    public List<ParametersContainer> getValues()
    {
        if( values == null )
        {
            values = new ArrayList<>();
        }
        return values;
    }

    @Override
    protected void doCloneParameter(ParameterSupport clone)
    {
        super.doCloneParameter(clone);
        ArrayParameter result = (ArrayParameter)clone;
        for( Map.Entry<String, Parameter> entry : childTypes.entrySet() )
        {
            result.getChildTypes().put(entry.getKey(), entry.getValue());
        }
    }
    
    public void setEntriesCount(int n)
    {
        getValues().clear();
        for(int i=0; i<n; i++)
        {
            ParametersContainer newEntry = new ParametersContainer();
            EntryStream.of( getChildTypes() ).mapValues( Parameter::cloneParameter ).forKeyValue( newEntry::put );
            getValues().add(newEntry);
        }
    }

    @Override
    public Parameter cloneParameter()
    {
        ArrayParameter result = new ArrayParameter(output);
        doCloneParameter(result);
        return result;
    }

    @Override
    public void setContainer(ParametersContainer container)
    {
        super.setContainer(container);
        for(Parameter parameter: getChildTypes().values())
            parameter.setContainer(container);
    }

    @Override
    public String toString()
    {
        JsonArray result = new JsonArray();
        for(Map<String, Parameter> group: getValues())
        {
            JsonObject element = new JsonObject();
            EntryStream.of( group ).mapValues( Parameter::toString ).forKeyValue( element::add );
            result.add(element);
        }
        return result.toString();
    }

    @Override
    public void setValue(String value)
    {
        try
        {
            JsonArray valueJSON = JsonArray.readFrom( value );
            getValues().clear();
            setEntriesCount(valueJSON.size());
            for(int i=0; i<valueJSON.size(); i++)
            {
                for(Entry<String, Parameter> entry: getValues().get(i).entrySet())
                {
                    JsonObject jsonObject = valueJSON.get(i).asObject();
                    if(jsonObject.get(entry.getKey()) != null)
                        entry.getValue().setValue(jsonObject.get(entry.getKey()).asString());
                }
            }
        }
        catch( ParseException | UnsupportedOperationException e )
        {
            // Ignore
        }
    }
}
