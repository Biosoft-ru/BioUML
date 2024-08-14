package biouml.plugins.sbml;

import org.w3c.dom.Element;

public class SbmlModelWriter_25 extends SbmlModelWriter_23
{
    @Override
    protected void setVersion(Element sbmlElement)
    {
        sbmlElement.setAttribute(SBML_VERSION_ATTR, SBML_VERSION_VALUE_5);
    }

    @Override
    protected String getSbmlNamespace()
    {
        return SBML_LEVEL2_XMLNS_VALUE + "/version5";
    }
}
