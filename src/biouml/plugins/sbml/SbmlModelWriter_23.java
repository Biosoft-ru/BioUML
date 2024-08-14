package biouml.plugins.sbml;

import org.w3c.dom.Element;

import biouml.model.Node;
import biouml.model.dynamics.Event;

public class SbmlModelWriter_23 extends SbmlModelWriter_22
{
    @Override
    protected void setVersion(Element sbmlElement)
    {
        sbmlElement.setAttribute(SBML_VERSION_ATTR, SBML_VERSION_VALUE_3);
    }

    @Override //timeUnits attribute was removed in SBML 23
    public Element writeEvent(Event event, Element eventListElement)
    {
        Element eventElement = super.writeEvent( event, eventListElement );
        if (eventElement.hasAttribute( TIME_UNITS_ATTR ))
            eventElement.removeAttribute( TIME_UNITS_ATTR );
        return eventElement;
    }

    @Override
    public Element writeSpecie(Element speciesListElement, Node species)
    {
        Element element = super.writeSpecie(speciesListElement, species);
        if(element != null)
            writeSBOTerm(element, species.getAttributes());
        return element;
    }
}
