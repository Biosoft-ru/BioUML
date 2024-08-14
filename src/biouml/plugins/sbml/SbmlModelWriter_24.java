package biouml.plugins.sbml;

import org.w3c.dom.Element;

import biouml.model.dynamics.Event;

public class SbmlModelWriter_24 extends SbmlModelWriter_23
{
    @Override
    protected void setVersion(Element sbmlElement)
    {
        sbmlElement.setAttribute(SBML_VERSION_ATTR, SBML_VERSION_VALUE_4);
    }

    @Override
    protected String getSbmlNamespace()
    {
        return SBML_LEVEL2_XMLNS_VALUE + "/version4";
    }

    @Override
    public Element writeEvent(Event event, Element eventListElement)
    {
        Element element = super.writeEvent(event, eventListElement);
        //write even if default so we can read it in level 3 version 1
        element.setAttribute(EVENT_USE_VALUES_FROM_TRIGGER_TIME_ATTR, String.valueOf(event.isUseValuesFromTriggerTime()));
        return element;
    }
}
