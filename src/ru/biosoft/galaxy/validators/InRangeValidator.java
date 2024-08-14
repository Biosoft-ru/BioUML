package ru.biosoft.galaxy.validators;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.Parameter;

/**
 * @author lan
 *
 */
public class InRangeValidator extends ValidatorSupport
{
    private Float min = Float.NEGATIVE_INFINITY;
    private Float max = Float.POSITIVE_INFINITY;
    
    @Override
    public void init(Element element, Parameter parameter)
    {
        super.init(element, parameter);
        if(element.hasAttribute("min") && !element.getAttribute("min").equals("-inf"))
            min = Float.parseFloat(element.getAttribute("min"));
        if(element.hasAttribute("max") && !element.getAttribute("max").equals("inf") && !element.getAttribute("max").equals("+inf"))
            max = Float.parseFloat(element.getAttribute("max"));
    }

    @Override
    public void validate() throws IllegalArgumentException
    {
        float value;
        try
        {
            value = Float.parseFloat(parameter.toString());
        }
        catch(NumberFormatException ex)
        {
            throw new IllegalArgumentException(message == null?"Must be a number":message);
        }
        if(value < min || value > max)
        {
            throw new IllegalArgumentException(message == null?"Value must be between "+min+" and "+max:message);
        }
    }
}
