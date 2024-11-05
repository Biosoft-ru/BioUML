package biouml.plugins.sbol;


import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Identified;

import biouml.standard.type.Type;

public class SbolUtil
{
    public static final String SBOL_DOCUMENT_PROPERTY = "sbol_document";
    public static String getType(Identified sbolObject)
    {
        if ( sbolObject instanceof ComponentDefinition )
        {
            ComponentDefinition cd = (ComponentDefinition) sbolObject;
            if ( cd.containsType(ComponentDefinition.DNA_REGION) )
                return Type.TYPE_GENE;
            else if ( cd.containsType(ComponentDefinition.RNA_REGION) )
                return Type.TYPE_RNA;
            else if ( cd.containsType(ComponentDefinition.PROTEIN) )
                return Type.TYPE_PROTEIN;
            else if ( cd.containsType(ComponentDefinition.SMALL_MOLECULE) )
                return Type.TYPE_SUBSTANCE;
            else if ( cd.containsType(ComponentDefinition.COMPLEX) )
                return Type.TYPE_MOLECULE;
        }
        return Type.TYPE_UNKNOWN;
    }
}
