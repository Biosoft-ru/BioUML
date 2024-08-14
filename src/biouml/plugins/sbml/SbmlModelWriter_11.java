package biouml.plugins.sbml;

import org.w3c.dom.Element;

public class SbmlModelWriter_11 extends SbmlModelWriter_L1
{
    @Override
    protected Element createSpeciesReferenceElement()
    {
        return document.createElement(SPECIE_REFERENCE_ELEMENT_11);
    }

    @Override
    protected void setSpeciesAttribute(Element speciesReferenceElement, String speciesName)
    {
        speciesReferenceElement.setAttribute(SPECIE_ATTR_11, speciesName);
    }
}
