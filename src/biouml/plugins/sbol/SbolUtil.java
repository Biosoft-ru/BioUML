package biouml.plugins.sbol;


import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.SequenceOntology;

import biouml.standard.type.Type;

public class SbolUtil
{
    public static final String SBOL_DOCUMENT_PROPERTY = "sbol_document";

    private static final Map<URI, String> dnaRegionToImage;

    static
    {
        Map<URI, String> aMap = new HashMap<>();
        aMap.put(SequenceOntology.PROMOTER, "promoter");
        //aMap.put(SequenceOntology.OPERATOR, "operator");
        aMap.put(SequenceOntology.CDS, "cds");
        //aMap.put(SequenceOntology.FIVE_PRIME_UTR, "five-prime-sticky-restriction-site");
        aMap.put(SequenceOntology.TERMINATOR, "terminator");
        aMap.put(SequenceOntology.INSULATOR, "insulator");
        aMap.put(SequenceOntology.ORIGIN_OF_REPLICATION, "origin-of-replication");
        aMap.put(SequenceOntology.PRIMER_BINDING_SITE, "primer-binding-site");
        aMap.put(SequenceOntology.RIBOSOME_ENTRY_SITE, "ribosome-entry-site");
        //aMap.put(SequenceOntology.GENE, "promoter");
        //aMap.put(SequenceOntology.MRNA, "promoter");
        aMap.put(SequenceOntology.RESTRICTION_ENZYME_RECOGNITION_SITE, "nuclease-site");
        //aMap.put(SequenceOntology.ENGINEERED_GENE, "promoter");
        aMap.put(SequenceOntology.ENGINEERED_REGION, "engineered-region");
        aMap.put(SequenceOntology.SEQUENCE_FEATURE, "no-glyph-assigned");
        //aMap.put(SequenceOntology.SGRNA, "promoter");
        //aMap.put(SequenceOntology.STRAND_ATTRIBUTE, "promoter");
        //aMap.put(SequenceOntology.SINGLE, "promoter");
        //aMap.put(SequenceOntology.DOUBLE, "promoter");
        //aMap.put(SequenceOntology.TOPOLOGY_ATTRIBUTE, "promoter");
        //aMap.put(SequenceOntology.LINEAR, "promoter");
        //aMap.put(SequenceOntology.CIRCULAR, "promoter");
        //aMap.put(SequenceOntology.DNA, "promoter");
        //aMap.put(SequenceOntology.RNA, "promoter");
        SequenceOntology so = new SequenceOntology();
        aMap.put(so.getURIbyId("SP:0001263"), "ncrna");
        aMap.put(so.getURIbyId("SP:0001691"), "blunt-restriction-site");
        aMap.put(so.getURIbyId("SP:0001975"), "five-prime-sticky-restriction-site");
        aMap.put(so.getURIbyId("SP:0001237"), "location-protein");
        aMap.put(so.getURIbyId("SP:0001236"), "location-dna");
        aMap.put(so.getURIbyId("SP:0000699"), "location-rna");
        aMap.put(so.getURIbyId("SP:0000553"), "polyA");
        aMap.put(so.getURIbyId("SP:0001955"), "protein-stability-element");
        aMap.put(so.getURIbyId("SP:0001979"), "rna-stability-element");
        aMap.put(so.getURIbyId("SP:0001953"), "assembly-scar");
        aMap.put(so.getURIbyId("SP:0000409"), "operator");
        aMap.put(so.getURIbyId("SP:0000031"), "aptamer");
        aMap.put(so.getURIbyId("SP:0000616"), "transcription-end");
        aMap.put(so.getURIbyId("SP:0001977"), "ribonuclease-site");
        aMap.put(so.getURIbyId("SP:0000327"), "translation-end");
        aMap.put(so.getURIbyId("SP:0001978"), "signature");
        aMap.put(so.getURIbyId("SP:0001932"), "five-prime-overhang");
        aMap.put(so.getURIbyId("SP:0001976"), "three-prime-sticky-restriction-site");
        aMap.put(so.getURIbyId("SP:0000299"), "specific-recombination-site");
        aMap.put(so.getURIbyId("SP:0001933"), "three-prime-overhang");
        aMap.put(so.getURIbyId("SP:0000724"), "origin-of-transfer");
        aMap.put(so.getURIbyId("SP:0001956"), "protease-site");

        dnaRegionToImage = Collections.unmodifiableMap(aMap);
    }

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
                for ( URI role : cd.getRoles() )
                {
                    if ( dnaRegionToImage.containsKey(role) )
                        return dnaRegionToImage.get(role);
                }
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
