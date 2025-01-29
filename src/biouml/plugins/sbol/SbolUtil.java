package biouml.plugins.sbol;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.sbolstandard.core2.AccessType;
import org.sbolstandard.core2.Annotation;
import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.DirectionType;
import org.sbolstandard.core2.FunctionalComponent;
import org.sbolstandard.core2.GenericTopLevel;
import org.sbolstandard.core2.Identified;
import org.sbolstandard.core2.Interaction;
import org.sbolstandard.core2.MapsTo;
import org.sbolstandard.core2.ModuleDefinition;
import org.sbolstandard.core2.Participation;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.SequenceAnnotation;
import org.sbolstandard.core2.SequenceConstraint;
import org.sbolstandard.core2.SequenceOntology;
import org.sbolstandard.core2.SystemsBiologyOntology;

import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.Type;
import ru.biosoft.util.DPSUtils;

public class SbolUtil
{
    private static final String JAVAX_XML_NAMESPACE_Q_NAME = "javax.xml.namespace.QName";
    private static Class qnameClass = null;
    public static final String SBOL_DOCUMENT_PROPERTY = "sbol_document";

    private static final Map<URI, String> dnaRegionToImage;
    private static final Map<String, URI> featurRoleToURI;
    private static final Map<String, URI> speciesToURI;
    private static final Map<URI, String> speciesURIToString;
    private static final Map<String, URI> interactionToURI;
    private static final Map<URI, String> interactionURIToString;
    private static final Map<String, URI> participationToURI;
    private static final Map<URI, String> participationURIToString;
    private static final Map<String, Integer> verticalShift;
    
    public static Set<URI> inputParticipantTypes;
    public static Set<URI> outputParticipantTypes;
    
    private static SequenceOntology so = new SequenceOntology();
    private static final SystemsBiologyOntology sbo = new SystemsBiologyOntology();
    public static URI TYPE_CIRCULAR = null;
    public static URI TYPE_LINEAR = null;
    public static URI TYPE_CHROMOSOMAL_LOCUS = null;

    static
    {
        try
        {
            qnameClass = Annotation.class.getClassLoader().loadClass( JAVAX_XML_NAMESPACE_Q_NAME );
        }
        catch( Exception ex )
        {

        }
    } 
    
    static
    {
        inputParticipantTypes = Set.of( 
                SystemsBiologyOntology.INHIBITOR, 
                SystemsBiologyOntology.COMPETITIVE_INHIBITOR,
                SystemsBiologyOntology.NON_COMPETITIVE_INHIBITOR, 
                sbo.getURIbyId( "SBO:0000536" ),  //partial_inhibitor
                sbo.getURIbyId( "SBO:0000537" ), //complete_inhibitor
                SystemsBiologyOntology.SILENCER, 
                sbo.getURIbyId( "SBO:0000639" ), //allosteric_inhibitor, see http://identifiers.org/SBO:0000639
                sbo.getURIbyId( "SBO:0000638" ),  //irreversible_inhibitor
                sbo.getURIbyId( "SBO:0000640" ), //uncompetitive_inhibitor
                SystemsBiologyOntology.PROMOTER,//According to documentation SystemsBiologyOntology.PROMOTER could be a participant of the STIMULATION reaction (not stated if it is in or out);
                SystemsBiologyOntology.STIMULATOR, 
                SystemsBiologyOntology.CATALYST, 
                SystemsBiologyOntology.ENZYMATIC_CATALYST,
                SystemsBiologyOntology.ESSENTIAL_ACTIVATOR, 
                SystemsBiologyOntology.BINDING_ACTIVATOR,
                SystemsBiologyOntology.CATALYTIC_ACTIVATOR, 
                SystemsBiologyOntology.SPECIFIC_ACTIVATOR,
                SystemsBiologyOntology.NON_ESSENTIAL_ACTIVATOR, 
                SystemsBiologyOntology.POTENTIATOR, 
                sbo.getURIbyId( "SBO:0000636" ), //allosteric activator
                sbo.getURIbyId( "SBO:0000637" ), //non-allosteric activator
                SystemsBiologyOntology.REACTANT, 
                SystemsBiologyOntology.MODIFIER, 
                SystemsBiologyOntology.INTERACTOR,
                SystemsBiologyOntology.TEMPLATE, 
                SystemsBiologyOntology.SUBSTRATE, 
                SystemsBiologyOntology.SIDE_SUBSTRATE );

        outputParticipantTypes = Set.of(
                SystemsBiologyOntology.INHIBITED, 
                SystemsBiologyOntology.STIMULATED,
                SystemsBiologyOntology.MODIFIED, 
                SystemsBiologyOntology.PRODUCT,
                SystemsBiologyOntology.SIDE_PRODUCT);
    }
    
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
        aMap.put( so.getURIbyId( "SO:0000288" ), "engineered-region" );
        aMap.put( so.getURIbyId( "SO:0000830" ), "chromosomal-locus" );
        aMap.put( so.getURIbyId( "SO:0000755" ), "circular-plasmid" );
        aMap.put( so.getURIbyId( "SO:0002223" ), "inert-dna-spacer" );
        aMap.put( so.getURIbyId( "SO:0000188" ), "intron" );
        aMap.put( so.getURIbyId( "SO:0000839" ), "polypeptide-region" );
        dnaRegionToImage = Collections.unmodifiableMap( aMap );
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
        bMap.put( "Inert DNA spacer", so.getURIbyId( "SO:0002223" ) );
        bMap.put( "Intron", so.getURIbyId( "SO:0000188" ) );
        bMap.put( "Polypeptide region", so.getURIbyId( "SO:0000839" ) );

        TYPE_CHROMOSOMAL_LOCUS = so.getURIbyId( "SO:0000830" );
        //bMap.put( "Chromosomal locus", ROLE_CHROMOSOMAL_LOCUS );
        TYPE_CIRCULAR = so.getURIbyId( "SO:0000988" );
        TYPE_LINEAR = so.getURIbyId( "SO:0000987" );
        //bMap.put( "Circular plasmid",  ROLE_CIRCULAR);
        featurRoleToURI = Collections.unmodifiableMap( bMap );
    }

    static
    {
        Map<String, URI> sMap = new HashMap<>();
        sMap.put( SbolConstants.PROTEIN, ComponentDefinition.PROTEIN );
        sMap.put( SbolConstants.SIMPLE_CHEMICAL, ComponentDefinition.SMALL_MOLECULE );
        sMap.put( SbolConstants.COMPLEX, ComponentDefinition.COMPLEX );
        speciesToURI = Collections.unmodifiableMap( sMap );
        speciesURIToString = sMap.entrySet().stream().collect( Collectors.toMap( Map.Entry::getValue, Map.Entry::getKey ) );
    }

    static
    {
        TreeMap<String, URI> iMap = new TreeMap<>();
        iMap.put( "Inhibition", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000169" ) );
        iMap.put( "Stimulation", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000170" ) );
        iMap.put( "Biochemical Reaction", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000176" ) );
        iMap.put( "Association", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000177" ) );
        iMap.put( "Degradation", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000179" ) );
        iMap.put( "Genetic Production", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000589" ) );
        iMap.put( "Control", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000168" ) );
        iMap.put( "Dissociation", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000180" ) );
        iMap.put( "Process", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000375" ) );
        interactionToURI = Collections.unmodifiableMap( iMap );
        interactionURIToString = iMap.entrySet().stream().collect( Collectors.toMap( Map.Entry::getValue, Map.Entry::getKey ) );
    }

    static
    {
        TreeMap<String, URI> pMap = new TreeMap<>();
        pMap.put( "Inhibitor", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000020" ) );
        pMap.put( "Inhibited", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000642" ) );
        pMap.put( "Stimulator", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000459" ) );
        pMap.put( "Stimulated", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000643" ) );
        pMap.put( "Reactant", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000010" ) );
        pMap.put( "Product", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000011" ) );
        pMap.put( "Promoter", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000598" ) );
        pMap.put( "Modifier", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000019" ) );
        pMap.put( "Modified", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000644" ) );
        pMap.put( "Template", URI.create( "http://identifiers.org/biomodels.sbo/SBO:0000645" ) );
        participationToURI = Collections.unmodifiableMap( pMap );
        participationURIToString = pMap.entrySet().stream().collect( Collectors.toMap( Map.Entry::getValue, Map.Entry::getKey ) );
    }

    //    public String getParticipationType(String interactionType, boolean reactant)
    //    {
    //        switch (interactionType)
    //        {
    //            case SbolConstants.INHIBITION:
    //                return reactant? 
    //        }
    //    }

    static
    {
        Map<String, Integer> aMap = new HashMap<>();
        aMap.put( "promoter", 0 );
        aMap.put( "cds", 18 );
        aMap.put( "terminator", 5 );
        aMap.put( "insulator", 2 );
        aMap.put( "origin-of-replication", 18 );
        aMap.put( "primer-binding-site", 16 );
        aMap.put( "ribosome-entry-site", 8 );
        aMap.put( "nuclease-site", 0 );
        aMap.put( "engineered-region", 8 );
        aMap.put( "no-glyph-assigned", 8 );
        aMap.put( "ncrna", 16 );
        aMap.put( "blunt-restriction-site", 18 );
        aMap.put( "five-prime-sticky-restriction-site", 18 );
        aMap.put( "location-protein", 0 );
        aMap.put( "location-dna", 0 );
        aMap.put( "location-rna", 0 );
        aMap.put( "polyA", 8 );
        aMap.put( "protein-stability-element", 0 );
        aMap.put( "rna-stability-element", 0 );
        aMap.put( "assembly-scar", 18 );
        aMap.put( "operator", 18 );
        aMap.put( "aptamer", 0 );
        aMap.put( "transcription-end", 0 );
        aMap.put( "ribonuclease-site", 0 );
        aMap.put( "translation-end", 0 );
        aMap.put( "signature", 4 );
        aMap.put( "five-prime-overhang", 18 );
        aMap.put( "three-prime-sticky-restriction-site", 18 );
        aMap.put( "specific-recombination-site", 18 );
        aMap.put( "three-prime-overhang", 18 );
        aMap.put( "origin-of-transfer", 18 );
        aMap.put( "protease-site", 0 );
        aMap.put( "inert-dna-spacer", 18 );
        aMap.put( "intron", 18 );
        aMap.put( "polypeptide-region", 18 );
        verticalShift = Collections.unmodifiableMap( aMap );
    }

    public static URI getInteractionURIByType(String type)
    {
        return interactionToURI.get( type );
    }

    public static String getInteractionStringType(URI uri)
    {
        return interactionURIToString.get( uri );
    }

    public static URI getParticipationURIByType(String type)
    {
        return participationToURI.get( type );
    }

    public static String getParticipationStringType(URI uri)
    {
        return participationURIToString.get( uri );
    }

    public static URI getSpeciesURIByType(String type)
    {
        return speciesToURI.get( type );
    }

    public static String getSpeciesURIByType(URI uri)
    {
        return speciesURIToString.get( uri );
    }

    public static URI getURIByRole(String name)
    {
        return featurRoleToURI.get( name );
    }

    public static String[] getFeatureRoles()
    {
        return featurRoleToURI.keySet().stream().sorted().toArray( String[]::new );
    }

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
    
    
    public static URI getURIByTopology(String s)
    {
        switch (s)
        {
            case SbolConstants.TOPOLOGY_LINEAR:
                return SbolUtil.TYPE_LINEAR;
            case SbolConstants.TOPOLOGY_LOCUS:
                return  SbolUtil.TYPE_CHROMOSOMAL_LOCUS;
            case SbolConstants.TOPOLOGY_CIRCULAR:
                return SbolUtil.TYPE_CIRCULAR;
        }
        return null;
    }
    
    public static void setTopologyType(ComponentDefinition cd, String topology) throws SBOLValidationException
    {
        URI newURI = getURIByTopology(topology);
        for( URI uri : cd.getTypes() )
        {
            if( uri.equals( TYPE_LINEAR ) || uri.equals( TYPE_CIRCULAR ) || uri.equals( TYPE_CHROMOSOMAL_LOCUS ) )
                cd.removeType( uri );
        }
        cd.addType( newURI );
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
            else if( cd.containsType( ComponentDefinition.RNA_MOLECULE ) )
                return "ssNA";
            else if( cd.containsType( ComponentDefinition.DNA_MOLECULE ) )
                return "dsNA";

        }
        else if( sbolObject instanceof Interaction )
        {
            Interaction inter = (Interaction)sbolObject;
            if( inter.containsType( SystemsBiologyOntology.GENETIC_PRODUCTION ) || inter.containsType( SystemsBiologyOntology.PROCESS )
                    || inter.containsType( SystemsBiologyOntology.CONTROL ) || inter.containsType( SystemsBiologyOntology.INHIBITION )
                    || inter.containsType( SystemsBiologyOntology.DEGRADATION )
                    || inter.containsType( SystemsBiologyOntology.STIMULATION ) )
                return "process";
            else if( inter.containsType( SystemsBiologyOntology.DISSOCIATION ) )
                return "dissociation";
            else if( inter.containsType( SystemsBiologyOntology.BIOCHEMICAL_REACTION )
                    || inter.containsType( SystemsBiologyOntology.NON_COVALENT_BINDING ) )
                return "association";
        }
        return "unspecified-glyph";
    }

    /**
     * Returns path to image view for given node
     */
    public static String getSbolImage(Node node)
    {
        return node.getAttributes().getValueAsString( SbolConstants.NODE_IMAGE );
    }
    
    /**
     * Set icon path to given node
     */
    public static void setSbolImage(Node node, String icon)
    {
        node.getAttributes().add(DPSUtils.createHiddenReadOnly(SbolConstants.NODE_IMAGE, String.class, icon));
    }
    
    /**
     * Set icon path based on Identified to given node
     */
    public static void setSbolImage(Node node, Identified id)
    {
        node.getAttributes().add(DPSUtils.createHiddenReadOnly(SbolConstants.NODE_IMAGE, String.class, getSbolImagePath( id )));
    }
    
    public static Base getKernelByComponentDefinition(ComponentDefinition cd, boolean isTopLevel)
    {
        if( cd.containsType( ComponentDefinition.DNA_REGION ) )
        {
            if( isBackBone(cd) && isTopLevel )
                return new Backbone( cd );
            else
                return new SequenceFeature( cd );
        }
        else if( cd.containsType( ComponentDefinition.COMPLEX ) || cd.containsType( ComponentDefinition.PROTEIN )
                || cd.containsType( ComponentDefinition.SMALL_MOLECULE ) )
        {
            MolecularSpecies species = new MolecularSpecies( cd.getDisplayId() );
            species.setType(getSpeciesURIByType( cd.getTypes().iterator().next()));
            species.setSbolObject( cd );
            return species;
        }
        return new SbolBase( cd );
    }

    /**
     * Returns vertical shift of view for given image path
     */
    public static int getVerticalShift(String imgPath)
    {
        return verticalShift.getOrDefault( imgPath, 0 );
    }

    /**
     * Returns node connected to given edge which contains InteractionProperties (i.e. reaction)
     */
    public static Node findInteractionNode(Edge e)
    {
        if( e.getInput().getKernel() instanceof InteractionProperties )
            return e.getInput();
        else if( e.getOutput().getKernel() instanceof InteractionProperties )
            return e.getOutput();
        return null;
    }

    /**
     * Removes SBOL object referenced by given diagram element from SBOL Document handling all dependent elements
     */
    public static void removeSbolObjectFromDiagram(DiagramElement de) throws SBOLValidationException
    {
        SBOLDocument doc = SbolUtil.getDocument( Diagram.getDiagram( de ) );
        if( doc == null || ! ( isSbol(de) ) )
            return;

        if( de.getKernel() instanceof InteractionProperties )
        {
            removeInteraction( doc, getDisplayId( de ) );
        }
        else if( de instanceof Edge )
        {
            Node reactionNode = findInteractionNode( (Edge)de );
            Node otherNode = ( (Edge)de ).getOtherEnd( reactionNode );
            
            if( otherNode.getCompartment().getKernel() instanceof Backbone && getParticipations(otherNode).size() == 1)
            {
                ComponentDefinition participant = getSbolObject( otherNode, ComponentDefinition.class );
                FunctionalComponent parentComponent = findParent( doc, participant );
                if( parentComponent != null )
                {
                    for( MapsTo maps : parentComponent.getMapsTos() )
                    {
                        if( maps.getLocalDefinition().equals( participant ) )
                            parentComponent.removeMapsTo( maps );
                    }
                }
            }
            if( reactionNode != null && otherNode != null && isSbol(otherNode) )
                removeParticipation( getSbolObject( reactionNode, Interaction.class ), getPersistentIdentity( otherNode ) );
        }

Identified id = SbolUtil.getSbolObject( de);
        if( id instanceof ComponentDefinition )
            removeComponentDefinition( doc, (ComponentDefinition)id );
    }
    
    /**
     * Return list of participations referenced by edges connected to given node
     */
    public static List<Participation> getParticipations(Node node)
    {
        return node.edges().map( e -> e.getKernel() ).select( ParticipationProperties.class ).map( p -> p.getSbolObject() ).toList();
    }

    /**
     * Return components inside given component definition with given URI
     */
    public static List<Component> findComponents(ComponentDefinition cd, ComponentDefinition innerComp)
    {
        List<Component> result = new ArrayList<>();
        for( Component component : cd.getComponents() )
        {
            if( component.getDefinition().getPersistentIdentity().equals( innerComp.getPersistentIdentity() ) )
                result.add( component );
        }
        return result;
    }

    /**
     * Return functional components inside given module definition with given URI
     */
    public static List<FunctionalComponent> findFunctionalComponents(ModuleDefinition md, ComponentDefinition cd)
    {
        List<FunctionalComponent> result = new ArrayList<>();
        for( FunctionalComponent fc : md.getFunctionalComponents() )
        {
            if( fc.getDefinition().getPersistentIdentity().equals( cd.getPersistentIdentity() ) )
                result.add( fc );
        }
        return result;
    }

    /*
     * Removes Interaction given by URI from document
     */
    public static void removeInteraction(SBOLDocument doc, String displayID)
    {
        for( ModuleDefinition md : doc.getModuleDefinitions() )
        {
            Interaction interaction = md.getInteraction( displayID );
            if( interaction != null )
                md.removeInteraction( interaction );
        }
    }

    /*
     * Removes Participation given by URI  from interaction
     */
    public static void removeParticipation(Interaction interaction, URI participantURI)
    {
        for( Participation pt : interaction.getParticipations() )
        {
            if( pt.getParticipantDefinition().getPersistentIdentity().equals( participantURI ) )
                interaction.removeParticipation( pt );
        }
    }

    /*
     * Removes Component Definition given by URI from document by its URI
     */
    public static void removeComponentDefinition(SBOLDocument doc, ComponentDefinition cd) throws SBOLValidationException
    {
        removeComponents( doc, cd );
        removeFunctionalComponents( doc, cd );
        doc.removeComponentDefinition( cd );
    }

    /*
     * Removes Component from document by its URI
     */
    public static void removeComponents(SBOLDocument doc, ComponentDefinition cd) throws SBOLValidationException
    {
        for( ComponentDefinition compDef : doc.getComponentDefinitions() )
        {
            for( Component component : findComponents( compDef, cd ) )
            {
                for( SequenceAnnotation sa : compDef.getSequenceAnnotations() )
                {
                    if( sa.isSetComponent() && sa.getComponentURI().equals( component.getIdentity() ) )
                        compDef.removeSequenceAnnotation( sa );
                }
                for( SequenceConstraint sc : compDef.getSequenceConstraints() )
                {
                    if( sc.getSubjectURI().equals( component.getIdentity() ) )
                        compDef.removeSequenceConstraint( sc );
                    if( sc.getObjectURI().equals( component.getIdentity() ) )
                        compDef.removeSequenceConstraint( sc );
                }
                compDef.removeComponent( component );
            }
        }
    }

    /**
     * Removes functional component given by its URI from the document
     */
    public static void removeFunctionalComponents(SBOLDocument doc, ComponentDefinition cd) throws SBOLValidationException
    {
        for( ModuleDefinition md : doc.getModuleDefinitions() )
        {
            for( FunctionalComponent fc : findFunctionalComponents( md, cd ) )
            {
                for( MapsTo maps : fc.getMapsTos() )
                    fc.removeMapsTo( maps );
                md.removeFunctionalComponent( fc );
            }
        }
    }

    /**
     * Retrieves default module definition from SBOL document
     */
    public static ModuleDefinition getDefaultModuleDefinition(SBOLDocument doc) throws SBOLValidationException
    {
        if( doc.getRootModuleDefinitions().isEmpty() )
            return doc.createModuleDefinition( "Main_module", "1" );
        return doc.getModuleDefinitions().iterator().next();
    }

    /**
     * Retrives SBOL document from diagram attributes
     */
    public static SBOLDocument getDocument(Diagram diagram)
    {
        Object result = diagram.getAttributes().getValue( SbolConstants.SBOL_DOCUMENT_PROPERTY );
        if( result instanceof SBOLDocument )
            return (SBOLDocument)result;
        return null;
    }

    /**
     * Find functional component in the document that corresponds to given component definition display id
     */
    public static FunctionalComponent findFunctionalComponent(SBOLDocument doc, String componentDefinitionID)
    {
        for( ModuleDefinition md : doc.getModuleDefinitions() )
        {
            FunctionalComponent fc = findFunctionalComponent( md, componentDefinitionID );
            if( fc != null )
                return fc;
        }
        return null;
    }

    /**
     * Find functional component inside given module definition that corresponds to given component definition display id
     */
    public static FunctionalComponent findFunctionalComponent(ModuleDefinition md, String componentDefinitionID)
    {
        for( FunctionalComponent fc : md.getFunctionalComponents() )
        {
            if( fc.getDefinition().getDisplayId().equals( componentDefinitionID ) )
                return fc;
        }
        return null;
    }


    /**
     * Find parent (backbone) functional component containing component which corresponds to given component definition
     */
    public static FunctionalComponent findParent(SBOLDocument doc, ComponentDefinition cd)
    {
        for( ComponentDefinition componentDef : doc.getComponentDefinitions() )
        {
            for( Component innerComponent : componentDef.getComponents() )
            {
                if( innerComponent.getDefinition().getIdentity().equals( cd.getIdentity() ) )
                {
                    return findFunctionalComponent( doc, componentDef.getDisplayId() );
                }
            }
        }
        return null;
    }

    /**
     * Find component inside given component definition with given display id
     */
    public static Component findComponent(ComponentDefinition cd, String componentDefinitionID)
    {
        for( Component c : cd.getComponents() )
        {
            if( c.getDefinition().getDisplayId().equals( componentDefinitionID ) )
                return c;
        }
        return null;
    }

    /**
     * Creates functional component referencing given component definition and inside given module definition
     */
    public static FunctionalComponent createFunctionalComponent(ModuleDefinition moduleDefinition, ComponentDefinition componentDefinition)
            throws Exception
    {
        return moduleDefinition.createFunctionalComponent( componentDefinition.getDisplayId() + "_fc", AccessType.PUBLIC,
                componentDefinition.getIdentity(), DirectionType.INOUT );
    }

    /**
     * Returns true if document contains generic top level with BioUML layout annotation
     */
    public static boolean hasLayout( SBOLDocument doc) throws Exception
    {
        return doc.getGenericTopLevel( "Layout", "1" ) != null;
    }

    /**
     * Returns display id of SBOL object referenced by diagram element
     */
    public static String getDisplayId(DiagramElement de)
    {
        Identified identified = getSbolObject( de );
        return identified != null ? identified.getDisplayId() : null;
    }

    /**
     * Returns persistent identity of SBOL object referenced by diagram element
     */
    public static URI getPersistentIdentity(DiagramElement de)
    {
        Identified identified = getSbolObject( de );
        return identified != null ? identified.getPersistentIdentity() : null;
    }
    
    /**
     * Returns identity of SBOL object referenced by diagram element
     */
    public static URI getIdentity(DiagramElement de)
    {
        Identified identified = getSbolObject( de );
        return identified != null ? identified.getIdentity() : null;
    }
    
    /**
     * Returns true if node kernel contains SBOL object
     */
    public static boolean isSbol(DiagramElement de)
    {
        return de.getKernel() instanceof SbolBase;
    }

    /**
     * Returns SBOL object referenced by diagram element
     */
    public static Identified getSbolObject(DiagramElement de)
    {
        return ( (SbolBase)de.getKernel() ).getSbolObject();
    }

    /**
     * Returns SBOL object referenced by diagram element of given class
     */
    @SuppressWarnings ( "unchecked" )
    public static <T> T getSbolObject(DiagramElement de, Class<? extends Identified> T)
    {
        return (T) ( (SbolBase)de.getKernel() ).getSbolObject();
    }
    
    /**
     * Generate unique name without underscore delimiter i.e. Process1, Process2,....
     * We do not want to use underscore for names (like Process_1, Process_2) as SBOL utilize underscores for something
     */
    public static String generateUniqueName(Diagram diagram, String baseName)
    {
        return DefaultSemanticController.generateUniqueNodeName(diagram, baseName, true, "");
    }

    /**
     * Use reflection to call method getLocalPart from QName inside of Annotation
     * QName can not be referenced directly here because of LinkageError
     */
    @SuppressWarnings ( "unchecked" )
    public static String getName(Annotation annotation) throws Exception
    {
        Object qName = Annotation.class.getMethod( "getQName" ).invoke( annotation );
        return qnameClass.getMethod( "getLocalPart" ).invoke( qName ).toString();
    }

    /**
     * Use reflection to create new Top Level in SBOL Document 
     * QName can not be referenced directly here because of LinkageError
     */
    public static GenericTopLevel createTopLevel(SBOLDocument doc, String namespace, String name, String prefix) throws Exception
    {
        Object qname = createQName( namespace, name, prefix );
        return (GenericTopLevel)SBOLDocument.class.getMethod( "createGenericTopLevel", String.class, String.class, qnameClass ).invoke( doc,
                name, "1", qname );
    }

    /**
     * Use reflection to create Annotation object
     * QName can not be referenced directly here because of LinkageError
     */
    public static Annotation createAnnotation(String namespace, String name, String prefix, String val) throws Exception
    {
        Object qname = createQName( namespace, name, prefix );
        return Annotation.class.getConstructor( qnameClass, String.class ).newInstance( qname, val );
    }

    /**
     * Use reflection to create Annotation object with given list of nested annotations
     * QName can not be referenced directly here because of LinkageError
     */
    public static void createAnnotation(Identified object, String namespace, String name, String nameInner, String prefix,
            String nestedName, List<Annotation> nested) throws Exception
    {
        Object qname = createQName( namespace, name, prefix );
        Object qnameInner = createQName( namespace, nameInner, prefix );
        Method method = Identified.class.getMethod( "createAnnotation", qnameClass, qnameClass, String.class, List.class );
        method.invoke( object, qname, qnameInner, nestedName, nested );
    }

    /**
     * Use reflection to create QName object
     * QName can not be referenced directly here because of LinkageError
     */
    @SuppressWarnings ( "unchecked" )
    private static Object createQName(String namespace, String name, String prefix) throws Exception
    {
        return qnameClass.getConstructor( String.class, String.class, String.class ).newInstance( namespace, name, prefix );
    }

    static boolean isBackBone(ComponentDefinition cd)
    {
        return !cd.getComponents().isEmpty() || cd.getTypes().contains( TYPE_CIRCULAR )
                || cd.getTypes().contains( TYPE_CHROMOSOMAL_LOCUS ) || cd.getTypes().contains( TYPE_LINEAR );
    }
}