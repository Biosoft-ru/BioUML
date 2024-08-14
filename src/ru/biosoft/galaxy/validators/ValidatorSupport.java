package ru.biosoft.galaxy.validators;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.Parameter;

/**
 * @author lan
 *
 */
public abstract class ValidatorSupport implements Validator, Cloneable
{
    protected String message;
    protected Parameter parameter;

    @Override
    public void init(Element element, Parameter parameter)
    {
        if(element.hasAttribute("message"))
            message = element.getAttribute("message");
        this.parameter = parameter;
    }

    @Override
    public ValidatorSupport clone(Parameter parameter)
    {
        ValidatorSupport result = clone();
        result.parameter = parameter;
        return result;
    }
    
    @Override
    protected ValidatorSupport clone()
    {
        try
        {
            return (ValidatorSupport)super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            throw new RuntimeException(e);
        }
    }
}
