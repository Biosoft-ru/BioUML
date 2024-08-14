package ru.biosoft.galaxy.parameters;

import java.util.Map;

import ru.biosoft.galaxy.ParametersContainer;
import ru.biosoft.galaxy.validators.Validator;

/**
 * Base interface for Galaxy tool parameter (input/output)
 */
public interface Parameter
{
    @Override
    public String toString();
    public void setValue(String value);
    public void setValueFromTest(String value);
    public Parameter cloneParameter();
    public boolean isOutput();
    public Map<String, MetaParameter> getMetadata();
    public Map<String, Object> getAttributes();
    public Map<String, Object> getParameterFields();
    public void setContainer(ParametersContainer container);
    public ParametersContainer getContainer();
    public void addValidator(Validator validator);
    public void validate() throws IllegalArgumentException;
}
