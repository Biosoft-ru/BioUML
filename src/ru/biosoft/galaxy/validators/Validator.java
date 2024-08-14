package ru.biosoft.galaxy.validators;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.Parameter;

/**
 * @author lan
 */
public interface Validator
{
    /**
     * Init from DOM-element
     * @param element
     * @param parameter Galaxy parameter to bind to
     */
    public void init(Element element, Parameter parameter);
    
    public void validate() throws IllegalArgumentException;
    
    /**
     * Clone and bind to new Parameter object
     */
    public Validator clone(Parameter parameter);
}
