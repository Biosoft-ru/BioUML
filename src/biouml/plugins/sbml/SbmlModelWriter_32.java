package biouml.plugins.sbml;

import org.w3c.dom.Element;

import biouml.standard.type.Reaction;

public class SbmlModelWriter_32 extends SbmlModelWriter_31
{
    @Override
    protected void setLevel(Element sbmlElement)
    {
        sbmlElement.setAttribute(SBML_LEVEL_ATTR, SBML_LEVEL_VALUE_3);
    }

    @Override
    protected void setVersion(Element sbmlElement)
    {
        sbmlElement.setAttribute(SBML_VERSION_ATTR, SBML_VERSION_VALUE_2);
    }

    @Override
    protected String getSbmlNamespace()
    {
        return SBML_LEVEL3_XMLNS_VALUE + "/version2/core";
    }
    
    @Override
    protected void writeReactionAttributes(Reaction reaction, Element element)
    {
        //attribute "fast" was removed from reaction
        element.setAttribute( REACTION_REVERSIBLE_ATTR, Boolean.toString(reaction.isReversible()) );
    }
}
