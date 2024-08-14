package ru.biosoft.galaxy.validators;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.Parameter;

/**
 * @author lan
 *
 */
public class LengthValidator extends ValidatorSupport
{
    private int min = -1;
    private int max = -1;
    
    @Override
    public void init(Element element, Parameter parameter)
    {
        super.init(element, parameter);
        if(element.hasAttribute("min"))
            min = Integer.parseInt(element.getAttribute("min"));
        if(element.hasAttribute("max"))
            max = Integer.parseInt(element.getAttribute("max"));
    }

    @Override
    public void validate() throws IllegalArgumentException
    {
        if(min != -1 && parameter.toString().length() < min)
            throw new IllegalArgumentException(message == null?"Must have length of at least "+min:message);
        if(max != -1 && parameter.toString().length() > max)
            throw new IllegalArgumentException(message == null?"Must have length no more than "+max:message);
    }

}
