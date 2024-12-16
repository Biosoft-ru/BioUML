package biouml.plugins.sbol;


import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.FunctionalComponent;
import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.Interaction;
import org.sbolstandard.core2.ModuleDefinition;
import org.sbolstandard.core2.Participation;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.SequenceAnnotation;
import org.sbolstandard.core2.SequenceConstraint;
import org.sbolstandard.core2.SequenceOntology;
import org.sbolstandard.core2.SystemsBiologyOntology;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

public class SbolUtil
{


    public static final String SBOL_DOCUMENT_PROPERTY = "sbol_document";

    private static final Map<URI, String> dnaRegionToImage;
    private static final Map<String, URI> featurRoleToURI;
    private static final Map<String, URI> speciesToURI;
    private static final Map<String, Integer> verticalShift;

    public static final String TYPE_INHIBITION = "inhibition";
    public static final String TYPE_STIMULATION = "stimulation";
    public static final String TYPE_CONTROL = "control";
    public static final String TYPE_PROCESS = "process";
    public static final String TYPE_DEGRADATION = "degradation";
    public static final String TYPE_BIOCHEMICAL_REACTION = "degradation";
    public static final String TYPE_NON_COVALENT_BINDING = "non-covalent binding";
    public static final String TYPE_GENETIC_PRODUCTION = "genetic production";
    public static final String TYPE_CIRCULAR_END = "circular-plasmid end";
    public static final String TYPE_CIRCULAR_START = "circular-plasmid start";
    public static final String TYPE_DEGRADATION_PRODUCT = "degradation product";

    private static SequenceOntology so = new SequenceOntology();
    public static URI ROLE_CIRCULAR = null;
    public static URI ROLE_CHROMOSOMAL_LOCUS = null;

    static
    {
        Map<URI, String> aMap = new HashMap<>();
        aMap.put( SequenceOntology.PROMOTER, "promoter" );
        //aMap.put(SequenceOntology.OPERATOR, "operator");
        aMap.put( SequenceOntology.CDS, "cds" );
        //aMap.put(SequenceOntology.FIVE_PRIME_UTR, "five-prime-sticky-restriction-site");
        aMap.put( SequenceOntology.TERMINATOR, "terminator" );
        aMap.put( SequenceOntology.INSULATOR, "insulator" );
        aMap.put( SequenceOntology.ORIGIN_OF_REPLICATION, "origin-of-replication" );
        aMap.put( SequenceOntology.PRIMER_BINDING_SITE, "primer-binding-site" );
        aMap.put( SequenceOntology.RIBOSOME_ENTRY_SITE, "ribosome-entry-site" );
        //aMap.put(SequenceOntology.GENE, "promoter");
        //aMap.put(SequenceOntology.MRNA, "promoter");
        aMap.put( SequenceOntology.RESTRICTION_ENZYME_RECOGNITION_SITE, "nuclease-site" );
        //aMap.put(SequenceOntology.ENGINEERED_GENE, "promoter");
        aMap.put( SequenceOntology.ENGINEERED_REGION, "engineered-region" );
        aMap.put( SequenceOntology.SEQUENCE_FEATURE, "no-glyph-assigned" );
        //aMap.put(SequenceOntology.SGRNA, "promoter");
        //aMap.put(SequenceOntology.STRAND_ATTRIBUTE, "promoter");
        //aMap.put(SequenceOntology.SINGLE, "promoter");
        //aMap.put(SequenceOntology.DOUBLE, "promoter");
        //aMap.put(SequenceOntology.TOPOLOGY_ATTRIBUTE, "promoter");
        //aMap.put(SequenceOntology.LINEAR, "promoter");
        //aMap.put(SequenceOntology.CIRCULAR, "promoter");
        //aMap.put(SequenceOntology.DNA, "promoter");
        //aMap.put(SequenceOntology.RNA, "promoter");
        aMap.put( so.getURIbyId( "SO:0001263" ), "ncrna" );
        aMap.put( so.getURIbyId( "SO:0001691" ), "blunt-restriction-site" );
        aMap.put( so.getURIbyId( "SO:0001975" ), "five-prime-sticky-restriction-site" );
        aMap.put( so.getURIbyId( "SO:0001237" ), "location-protein" );
        aMap.put( so.getURIbyId( "SO:0001236" ), "location-dna" );
        aMap.put( so.getURIbyId( "SO:0000699" ), "location-rna" );
        aMap.put( so.getURIbyId( "SO:0000553" ), "polyA" );
        aMap.put( so.getURIbyId( "SO:0001955" ), "protein-stability-element" );
        aMap.put( so.getURIbyId( "SO:0001979" ), "rna-stability-element" );
        aMap.put( so.getURIbyId( "SO:0001953" ), "assembly-scar" );
        aMap.put( so.getURIbyId( "SO:0000409" ), "operator" );
        aMap.put( so.getURIbyId( "SO:0000031" ), "aptamer" );
        aMap.put( so.getURIbyId( "SO:0000616" ), "transcription-end" );
        aMap.put( so.getURIbyId( "SO:0001977" ), "ribonuclease-site" );
        aMap.put( so.getURIbyId( "SO:0000327" ), "translation-end" );
        aMap.put( so.getURIbyId( "SO:0001978" ), "signature" );
        aMap.put( so.getURIbyId( "SO:0001932" ), "five-prime-overhang" );
        aMap.put( so.getURIbyId( "SO:0001976" ), "three-prime-sticky-restriction-site" );
        aMap.put( so.getURIbyId( "SO:0000299" ), "specific-recombination-site" );
        aMap.put( so.getURIbyId( "SO:0001933" ), "three-prime-overhang" );
        aMap.put( so.getURIbyId( "SO:0000724" ), "origin-of-transfer" );
        aMap.put( so.getURIbyId( "SO:0001956" ), "protease-site" );
        aMap.put( so.getURIbyId( "SO:0000288" ), "engineered-region");
        aMap.put( so.getURIbyId( "SO:0000830" ), "chromosomal-locus");
        aMap.put( so.getURIbyId( "SO:0000755" ), "circular-plasmid");
        

        dnaRegionToImage = Collections.unmodifiableMap(aMap);
    }


    static
    {
        Map<String, URI> bMap = new HashMap<>();
        bMap.put( "Promoter", SequenceOntology.PROMOTER );
        bMap.put( "CDS", SequenceOntology.CDS );
        bMap.put( "Terminator", SequenceOntology.TERMINATOR );
        bMap.put( "Insulator", SequenceOntology.INSULATOR );
        bMap.put( "Origin of replication", SequenceOntology.ORIGIN_OF_REPLICATION );
        bMap.put( "Primer binding site", SequenceOntology.PRIMER_BINDING_SITE );
        bMap.put( "Ribosome entry site", SequenceOntology.RIBOSOME_ENTRY_SITE );
        bMap.put( "Nuclease site", SequenceOntology.RESTRICTION_ENZYME_RECOGNITION_SITE );
        bMap.put( "Engineered region", SequenceOntology.ENGINEERED_REGION );
        bMap.put( "Unknown", SequenceOntology.SEQUENCE_FEATURE );

        bMap.put( "NCRNA", so.getURIbyId( "SO:0001263" ) );
        bMap.put( "Blunt restriction site", so.getURIbyId( "SO:0001691" ) );
        bMap.put( "Five prime sticky resitriction site", so.getURIbyId( "SO:0001975" ) );
        bMap.put( "Location protein", so.getURIbyId( "SO:0001237" ) );
        bMap.put( "Location DNA", so.getURIbyId( "SO:0001236" ) );
        bMap.put( "Location RNA", so.getURIbyId( "SO:0000699" ) );
        bMap.put( "Poly A", so.getURIbyId( "SO:0000553" ) );
        bMap.put( "Protein stability element", so.getURIbyId( "SO:0001955" ) );
        bMap.put( "RNA stability element", so.getURIbyId( "SO:0001979" ) );
        bMap.put( "Assembly scar", so.getURIbyId( "SO:0001953" ) );
        bMap.put( "Operator", so.getURIbyId( "SO:0000409" ) );
        bMap.put( "Aptamer", so.getURIbyId( "SO:0000031" ) );
        bMap.put( "transcription end", so.getURIbyId( "SO:0000616" ) );
        bMap.put( "Riboneclease site", so.getURIbyId( "SO:0001977" ) );
        bMap.put( "Translation end", so.getURIbyId( "SO:0000327" ) );
        bMap.put( "Signature", so.getURIbyId( "SO:0001978" ) );
        bMap.put( "Five prime overhang", so.getURIbyId( "SO:0001932" ) );
        bMap.put( "Three prime sticky restriction site", so.getURIbyId( "SO:0001976" ) );
        bMap.put( "Specific recombination site", so.getURIbyId( "SO:0000299" ) );
        bMap.put( "Three prime overhang", so.getURIbyId( "SO:0001933" ) );
        bMap.put( "Origin of transfer", so.getURIbyId( "SO:0000724" ) );
        bMap.put( "Protease site", so.getURIbyId( "SO:0001956" ) );
        
        ROLE_CHROMOSOMAL_LOCUS = so.getURIbyId( "SO:0000830" );
        //bMap.put( "Chromosomal locus", ROLE_CHROMOSOMAL_LOCUS );
        ROLE_CIRCULAR = so.getURIbyId( "SO:0000755" );
        //bMap.put( "Circular plasmid",  ROLE_CIRCULAR);
        

        featurRoleToURI = Collections.unmodifiableMap( bMap );
    }

    static
    {
        Map<String, URI> sMap = new HashMap<>();
        sMap.put( SbolConstants.PROTEIN, ComponentDefinition.PROTEIN );
        sMap.put( SbolConstants.SIMPLE_CHEMICAL,ComponentDefinition.SMALL_MOLECULE );
        sMap.put( SbolConstants.COMPLEX, ComponentDefinition.COMPLEX );
        speciesToURI = Collections.unmodifiableMap( sMap );
    }

    static
    {
        Map<String, Integer> aMap = new HashMap<>();
        aMap.put("promoter", 0);
        aMap.put("cds", 18);
        aMap.put("terminator", 5);
        aMap.put("insulator", 2);
        aMap.put("origin-of-replication", 18);
        aMap.put("primer-binding-site", 16);
        aMap.put("ribosome-entry-site", 8);
        aMap.put("nuclease-site", 0);
        aMap.put("engineered-region", 8);
        aMap.put("no-glyph-assigned", 8);
        aMap.put("ncrna", 16);
        aMap.put("blunt-restriction-site", 18);
        aMap.put("five-prime-sticky-restriction-site", 18);
        aMap.put("location-protein", 0);
        aMap.put("location-dna", 0);
        aMap.put("location-rna", 0);
        aMap.put("polyA", 8);
        aMap.put("protein-stability-element", 0);
        aMap.put("rna-stability-element", 0);
        aMap.put("assembly-scar", 18);
        aMap.put("operator", 18);
        aMap.put("aptamer", 0);
        aMap.put("transcription-end", 0);
        aMap.put("ribonuclease-site", 0);
        aMap.put("translation-end", 0);
        aMap.put("signature", 4);
        aMap.put("five-prime-overhang", 18);
        aMap.put("three-prime-sticky-restriction-site", 18);
        aMap.put("specific-recombination-site", 18);
        aMap.put("three-prime-overhang", 18);
        aMap.put("origin-of-transfer", 18);
        aMap.put("protease-site", 0);

        verticalShift = Collections.unmodifiableMap(aMap);
    }


    public static URI getSpeciesURIByType(String type)
    {
        return speciesToURI.get( type );
    }
    
    public static URI getURIByRole(String name)
    {
        return featurRoleToURI.get( name );
    }

    public static String[] getFeatureRoles()
    {
        return featurRoleToURI.keySet().stream().sorted().toArray( String[]::new );
    }

    //private static OWLOntology ontology = null;


    public static String getType(Identified sbolObject)
    {
        if( sbolObject instanceof ComponentDefinition )
        {
            ComponentDefinition cd = (ComponentDefinition)sbolObject;
            if( cd.containsType( ComponentDefinition.DNA_REGION ) )
                return Type.TYPE_GENE;
            else if( cd.containsType( ComponentDefinition.RNA_REGION ) )
                return Type.TYPE_RNA;
            else if( cd.containsType( ComponentDefinition.PROTEIN ) )
                return Type.TYPE_PROTEIN;
            else if( cd.containsType( ComponentDefinition.SMALL_MOLECULE ) )
                return Type.TYPE_SUBSTANCE;
            else if( cd.containsType( ComponentDefinition.COMPLEX ) )
                return Type.TYPE_MOLECULE;
        }
        return Type.TYPE_UNKNOWN;
    }

    public static String getSbolImagePath(Identified sbolObject)
    {
        //TODO: support types
        if( sbolObject instanceof ComponentDefinition )
        {
            ComponentDefinition cd = (ComponentDefinition)sbolObject;
            if( cd.containsType( ComponentDefinition.DNA_REGION ) )
            {
                for( URI role : cd.getRoles() )
                {
                    if( dnaRegionToImage.containsKey( role ) )
                        return dnaRegionToImage.get( role );
                }
            }
            else if( cd.containsType( ComponentDefinition.RNA_REGION ) )
                return "rna-stability-element";
            else if( cd.containsType( ComponentDefinition.PROTEIN ) )
                return "protein";
            else if( cd.containsType( ComponentDefinition.SMALL_MOLECULE ) )
                return "simple-chemical-circle";
            else if( cd.containsType( ComponentDefinition.COMPLEX ) )
                return "complex-sbgn";
            else if ( cd.containsType(ComponentDefinition.RNA_MOLECULE) )
                return "ssNA";
            else if ( cd.containsType(ComponentDefinition.DNA_MOLECULE) )
                return "dsNA";

        }
        else if ( sbolObject instanceof Interaction )
        {
            Interaction inter = (Interaction) sbolObject;
            if ( inter.containsType(SystemsBiologyOntology.GENETIC_PRODUCTION) )
                return "process";
            else if ( inter.containsType(SystemsBiologyOntology.DISSOCIATION) )
                return "dissociation";
            else if ( inter.containsType(SystemsBiologyOntology.BIOCHEMICAL_REACTION) || inter.containsType(SystemsBiologyOntology.NON_COVALENT_BINDING) )
                return "association";
        }
        return "unspecified-glyph";
    }

    public static Base getKernelByComponentDefinition(ComponentDefinition cd, boolean isTopLevel)
    {
        if ( cd.containsType(ComponentDefinition.DNA_REGION) )
        {
            if ( cd.getComponents().size() > 0 && isTopLevel )
                return new Backbone(cd);
            else
                return new SequenceFeature(cd);
        }
        return new SbolBase(cd);
    }

    public static int getVerticalShift(String imgPath)
    {
        return verticalShift.getOrDefault(imgPath, 0);
    }

    public static boolean removeSbolObjectFromDiagram(DiagramElement de)
    {
        Diagram diagram = Diagram.getDiagram(de);
        Object doc = diagram.getAttributes().getValue(SbolUtil.SBOL_DOCUMENT_PROPERTY);
        if ( doc == null || !(doc instanceof SBOLDocument) )
            return false;
        if ( (de instanceof Edge || de.getKernel() instanceof Reaction) && de.getAttributes().hasProperty("interactionURI") )
        {
            String interactionURIString = de.getAttributes().getValueAsString("interactionURI");
            URI uri = URI.create(interactionURIString);
            for ( ModuleDefinition md : ((SBOLDocument) doc).getModuleDefinitions() )
            {
                Interaction interaction = md.getInteraction(uri);
                if ( interaction != null )
                {
                    md.removeInteraction(interaction);
                    return true;
                }
            }
            return false;
        }
        else if(de instanceof Edge )
        {
            Edge e = (Edge)de;
            Node reactionNode = null;
            Node otherNode = null;
            
            if(e.getInput().getKernel() instanceof Reaction)
            {
                reactionNode = e.getInput();
                otherNode = e.getOutput();
            }
            else if ( e.getOutput().getKernel() instanceof Reaction )
            {
                reactionNode = e.getOutput();
                otherNode = e.getInput();
            }
            if ( reactionNode != null && reactionNode.getAttributes().hasProperty("interactionURI") && otherNode != null && otherNode.getKernel() instanceof SbolBase )
            {
                String interactionURIString = reactionNode.getAttributes().getValueAsString("interactionURI");
                URI uri = URI.create(interactionURIString);
                for ( ModuleDefinition md : ((SBOLDocument) doc).getModuleDefinitions() )
                {
                    Interaction interaction = md.getInteraction(uri);
                    if ( interaction != null )
                    {
                        URI participantURI = ((SbolBase) otherNode.getKernel()).getSbolObject().getPersistentIdentity();
                        for ( Participation pt : interaction.getParticipations() )
                        {
                            if ( pt.getParticipantDefinition().getPersistentIdentity().equals(participantURI) )
                            {
                                interaction.removeParticipation(pt);
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
        if ( !(de.getKernel() instanceof SbolBase) )
            return false;
        return removeSbolObjectFromDocument((SBOLDocument) doc, ((SbolBase) de.getKernel()).getSbolObject().getIdentity());
    }

    public static boolean removeSbolObjectFromDocument(SBOLDocument doc, URI uri)
    {
        for ( ComponentDefinition cd : doc.getComponentDefinitions() )
        {
            Component compToRemove = null;
            for ( Component component : cd.getComponents() )
            {
                if ( component.getDefinitionURI().equals(uri) )
                {
                    compToRemove = component;
                    break;
                }
            }
            if ( compToRemove == null )
                continue;
            for ( SequenceAnnotation sa : cd.getSequenceAnnotations() )
            {
                if ( sa.isSetComponent() && sa.getComponentURI().equals(compToRemove.getIdentity()) )
                {
                    cd.removeSequenceAnnotation(sa);
                }
            }
            for ( SequenceConstraint sc : cd.getSequenceConstraints() )
            {
                if ( sc.getSubjectURI().equals(compToRemove.getIdentity()) )
                {
                    cd.removeSequenceConstraint(sc);
                }
                if ( sc.getObjectURI().equals(compToRemove.getIdentity()) )
                {
                    cd.removeSequenceConstraint(sc);
                }
            }
            try
            {
                cd.removeComponent(compToRemove);
            }
            catch (SBOLValidationException e)
            {
                // TODO Auto-generated catch block
            }
        }

        for ( ModuleDefinition md : doc.getModuleDefinitions() )
        {
            for ( FunctionalComponent c : md.getFunctionalComponents() )
            {
                if ( c.getDefinitionURI().equals(uri) )
                {
                    try
                    {
                        md.removeFunctionalComponent(c);
                    }
                    catch (SBOLValidationException e)
                    {
                        // TODO Auto-generated catch block
                    }
                }
            }
        }

        ComponentDefinition cd = doc.getComponentDefinition(uri);
        if ( cd == null )
            return false;
        try
        {
            doc.removeComponentDefinition(cd);
        }
        catch (SBOLValidationException e)
        {
            // TODO Auto-generated catch block
            return false;
        }
        return true;
    }

}
