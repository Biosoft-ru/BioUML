package biouml.plugins.sbol;


import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.SequenceOntology;

import biouml.standard.type.Type;

public class SbolUtil
{
    public static final String SBOL_DOCUMENT_PROPERTY = "sbol_document";

    //private static OWLOntology ontology = null;

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

    public static String getSbolImagePath(Identified sbolObject)
    {
        //TODO: support types
        if ( sbolObject instanceof ComponentDefinition )
        {
            ComponentDefinition cd = (ComponentDefinition) sbolObject;
            if ( cd.containsType(ComponentDefinition.DNA_REGION) )
            {
                if ( cd.containsRole(SequenceOntology.PROMOTER) )
                    return "promoter";
            }
            else if ( cd.containsType(ComponentDefinition.RNA_REGION) )
                return "rna-stability-element";
            else if ( cd.containsType(ComponentDefinition.PROTEIN) )
                return "protein";
            else if ( cd.containsType(ComponentDefinition.SMALL_MOLECULE) )
                return "simple-chemical-circle";
            else if ( cd.containsType(ComponentDefinition.COMPLEX) )
                return "complex-sbgn";
        }
        return "unspecified-glyph";
    }

    //    private static synchronized void initOntology()
    //    {
    //        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    //        try
    //        {
    //            ontology = manager.loadOntologyFromPhysicalURI(SbolUtil.class.getResource("resources/so.owl").toURI());
    //        }
    //        catch (OWLOntologyCreationException e)
    //        {
    //            // TODO Auto-generated catch block
    //            e.printStackTrace();
    //        }
    //        catch (URISyntaxException e)
    //        {
    //            // TODO Auto-generated catch block
    //            e.printStackTrace();
    //        }
    //    }
}
