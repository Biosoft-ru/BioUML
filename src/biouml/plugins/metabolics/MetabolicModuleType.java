package biouml.plugins.metabolics;

import biouml.model.DiagramType;
import biouml.standard.StandardModuleType;

public class MetabolicModuleType extends StandardModuleType
{
    public static final String NOTATION_NAME = "kegg_recon.xml";
    
    @Override
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        return new Class[0];
    }

    @Override
    public String[] getXmlDiagramTypes()
    {
        return new String[] {NOTATION_NAME};
    }

}
