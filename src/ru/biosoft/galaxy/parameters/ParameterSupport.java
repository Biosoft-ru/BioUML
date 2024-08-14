package ru.biosoft.galaxy.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.galaxy.ParametersContainer;
import ru.biosoft.galaxy.validators.Validator;

/**
 * Abstract implementation of {@link Parameter}
 */
public abstract class ParameterSupport implements Parameter
{
    protected boolean output;
    protected ParametersContainer container;
    protected Map<String, MetaParameter> metadata;
    protected Map<String, Object> attributes;
    protected Map<String, Object> fields = new HashMap<>();
    protected List<Validator> validators = new ArrayList<>();

    public ParameterSupport(boolean output)
    {
        this.output = output;
    }
    
    @Override
    public boolean isOutput()
    {
        return output;
    }

    @Override
    public void setValue(String value)
    {
    }
    
    @Override
    public void setValueFromTest(String value)
    {
        this.setValue(value);
    }
    
    @Override
    public Map<String, MetaParameter> getMetadata()
    {
        if( metadata == null )
        {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    @Override
    public Map<String, Object> getAttributes()
    {
        if( attributes == null )
        {
            attributes = new HashMap<>();
        }
        return attributes;
    }
    
    @Override
    public Map<String, Object> getParameterFields()
    {
        return fields;
    }

    @Override
    public void addValidator(Validator validator)
    {
        validators.add(validator);
    }

    @Override
    public void validate() throws IllegalArgumentException
    {
        for(Validator validator: validators)
            validator.validate();
    }

    @Override
    public ParametersContainer getContainer()
    {
        return container;
    }

    @Override
    public void setContainer(ParametersContainer container)
    {
        this.container = container;
    }
    
    protected void doCloneParameter(ParameterSupport clone)
    {
        clone.fields = new HashMap<>(this.fields);
        if(this.attributes != null)
            clone.attributes = new HashMap<>(this.attributes);
        if(this.metadata != null)
            clone.metadata = new HashMap<>(this.metadata);
        for(Validator validator: validators)
            clone.validators.add(validator.clone(clone));
    }
}
