package biouml.plugins.biopax.reader;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.semanticweb.owl.model.OWLConstant;
import org.semanticweb.owl.model.OWLDataPropertyExpression;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLOntology;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.HtmlUtil;
import ru.biosoft.util.TextUtil2;
import uk.ac.manchester.cs.owl.OWLDataPropertyImpl;
import uk.ac.manchester.cs.owl.OWLObjectPropertyImpl;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.biopax.access.BioPaxOwlDataCollection.BioPAXCollectionJobControl;
import biouml.plugins.biopax.model.BioSource;
import biouml.plugins.biopax.model.Confidence;
import biouml.plugins.biopax.model.EntityFeature;
import biouml.plugins.biopax.model.Evidence;
import biouml.plugins.biopax.model.OpenControlledVocabulary;
import biouml.plugins.biopax.model.SequenceInterval;
import biouml.plugins.biopax.model.SequenceSite;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnDiagramViewOptions;
import biouml.plugins.sbgn.Type;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Base;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.Complex;
import biouml.standard.type.Concept;
import biouml.standard.type.DNA;
import biouml.standard.type.DatabaseInfo;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Protein;
import biouml.standard.type.Publication;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Structure;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class BioPAXReader_level3 extends BioPAXReader
{
    protected String namePrefix = "";

    public BioPAXReader_level3(OWLOntology ontology)
    {
        super( ontology );
    }

    public BioPAXReader_level3(OWLOntology ontology, DataCollection<DataCollection> data, DataCollection<Diagram> diagrams,
            DataCollection<DataCollection> dictionaries)
    {
        super( ontology );
        this.data = data;
        this.diagrams = diagrams;
        this.dictionaries = dictionaries;
        setInternalCollections();
    }


    public boolean read() throws Exception
    {
        return read( "", null );
    }

    @Override
    public boolean read(String prefix, BioPAXCollectionJobControl jobControl) throws Exception
    {
        namePrefix = prefix;
        if( jobControl != null )
            jobControl.functionStarted();

        Set<OWLIndividual> individuals = ontology.getReferencedIndividuals();
        int totalCount = individuals.size();
        int currentCount = 0;
        int currentPercent = 0;

        List<OWLIndividual> pendingDiagrams = new ArrayList<>();

        for( OWLIndividual individual : individuals )
        {
            String type = getType( individual );

            if( isType( type, PHYSICAL_ENTITY_TYPES ) )
            {
                parsePhysicalEntity( individual );
            }
            else if( isType( type, CONTROL_TYPES ) )
            {
                parseControl( individual, type );
            }
            else if( isType( type, CONVERSION_TYPES ) || "MolecularInteraction".equals( type ) )
            {
                parseConversion( individual, type );
            }
            else if( isType( type, ENTITY_REFERENCE_TYPES ) )
            {
                parseEntityReference( individual );
            }
            else if( "GeneticInteraction".equals( type ) )
            {
                parseGeneticInteraction( individual );
            }
            else if( "TemplateReaction".equals( type ) )
            {
                parseTemplateReaction( individual );
            }
            else if( "Gene".equals( type ) )
            {
                parseGene( individual, null );
            }
            else if( "Pathway".equals( type ) )
            {
                pendingDiagrams.add( individual ); //read diagram after everything else, so we have all controls added to reactions
            }

            currentCount++;
            if( ( currentCount * 100 ) / totalCount > currentPercent )
            {
                currentPercent = currentCount * 100 / totalCount;
                if( jobControl != null )
                {
                    jobControl.setPreparedness( currentPercent );
                    if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                        return false;
                }
            }
        }

        for( OWLIndividual ind : pendingDiagrams )
        {
            diagrams.put( readDiagram( null, diagrams, ind ) );
        }


        if( jobControl != null )
            jobControl.functionFinished( "complete" );
        return true;
    }

    public Diagram readDiagram(String name, DataCollection<Diagram> origin, OWLIndividual pathwayIndividual) throws Exception
    {
        String diagramName = name == null ? namePrefix + getName( pathwayIndividual ) : name;
        Diagram diagram;
        if( origin != null && origin.contains( diagramName ) )
            return get( origin, diagramName );

        DiagramInfo diagramInfo = new DiagramInfo( origin, diagramName );
        diagramInfo.setTitle( getTitleName( pathwayIndividual ) );
        diagramInfo.setComment( getComments( pathwayIndividual ) );
        diagramInfo.setDatabaseReferences( getDatabaseReference( pathwayIndividual, "xref" ) );
        diagramInfo.setLiteratureReferences( getPublications( pathwayIndividual, "xref" ) );

        DynamicPropertySet dps = diagramInfo.getAttributes();
        writeToDPS( dps, "Availability", String.class, getStringProperty( "availability", pathwayIndividual ) );
        writeToDPS( dps, "Organism", String.class, getOrganism( pathwayIndividual ) );
        writeToDPS( dps, "Synonyms", String.class, getSynonyms( pathwayIndividual ) );
        writeToDPS( dps, "Evidence", Evidence.class, getEvidence( pathwayIndividual ) );
        writeToDPS( dps, "DataSource", String[].class, getDataSource( pathwayIndividual ) );

        DiagramType diagramType = getDiagramType();

        diagram = diagramType.createDiagram( origin, diagramName, diagramInfo );

        SbgnDiagramViewOptions options = (SbgnDiagramViewOptions)diagram.getViewOptions();
        options.setNodeTitleLimit( 100 );
        options.setNodeTitleFont( new ColorFont( "Arial", 0, 12 ) );
        options.setCustomTitleFont( new ColorFont( "Arial", 0, 12 ) );

        diagram.setNotificationEnabled( false );

        List<String> components = parsePathwayComponents( pathwayIndividual );
        drawPathway( components, diagram );
        arrangeDiagram( diagram );
        writeToDPS( dps, "Components", String[].class, components.toArray( new String[components.size()] ) );

        diagram.setNotificationEnabled( true );

        return diagram;
    }

    private List<String> parsePathwayComponents(OWLIndividual individual) throws Exception
    {
        List<String> components = new ArrayList<>();
        for( OWLIndividual ind : getProperties( individual, "pathwayComponent" ) )
        {
            String type = getType( ind );
            if( type.equals( "Pathway" ) )
            {
                Diagram diagram = readDiagram( null, diagrams, ind );
                diagrams.put( diagram );
                components.add( Module.DIAGRAM + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( diagram.getName() ) );

            }
            else if( isType( type, CONTROL_TYPES ) )
            {
                components.add( Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( controls.getName() )
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( parseControl( ind, type ) ) );
            }
            else if( isType( type, CONVERSION_TYPES ) || type.equals( "MolecularInteraction" ) )
            {
                components.add( Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( conversions.getName() )
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( parseConversion( ind, type ).getName() ) );
            }
            else if( type.equals( "GeneticInteraction" ) )
            {
                components.add( Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( conversions.getName() )
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( parseGeneticInteraction( ind ).getName() ) );
            }
            else if( type.equals( "TemplateReaction" ) )
            {
                components.add( Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( controls.getName() )
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( parseTemplateReaction( ind ) ) );
            }
        }
        return components;
    }

    /**
     * Simplest case of control, we add SpecieReference to reaction
     */
    private void parsePhysicalEntityControl(OWLIndividual control, OWLIndividual controller, OWLIndividual controlled, String controlType)
            throws Exception
    {
        String name = namePrefix + control.toString();
        if( participants.contains( name ) )
            return;

        Reaction reaction = null;

        String type = getType( controlled );

        if( isType( type, CONVERSION_TYPES ) || type.equals( "MolecularInteraction" ) )
        {
            reaction = parseConversion( controlled, type );
        }
        else if( type.equals( "GeneticInteraction" ) )
        {
            reaction = parseGeneticInteraction( controlled );
        }
        else
        {
            return;
        }

        Base base = parsePhysicalEntity( controller );
        SpecieReference reference = new SpecieReference( reaction, reaction.getName(), base.getName(), SpecieReference.MODIFIER );
        reference.setSpecie( getRelativeToModuleName( base ) );

        String str = getStringProperty( "controlType", control );

        if( controlType.equals( "Catalysis" ) )
            reference.setModifierAction( "catalysis" );
        else if( controlType.equals( "Modulation" ) )
            reference.setModifierAction( "modulation" );
        else if( str != null )
            reference.setModifierAction( str.contains( "INHIBITION" ) ? "inhibition" : "stimulation" );
        else
            reference.setModifierAction( "catalysis" ); //default

        reaction.put( reference );
        reaction.getOrigin().put( reaction );
        participants.put( reference );
    }

    private boolean isPhysicalEntityControl(OWLIndividual controller, OWLIndividual controlled)
    {
        String controllerType = getType( controller );
        String controlledType = getType( controlled );
        return isType( controllerType, PHYSICAL_ENTITY_TYPES ) && isType( controlledType, CONVERSION_TYPES )
                || controlledType.equals( "GeneticInteraction" );
    }

    private String parseControl(OWLIndividual individual, String controlType) throws Exception
    {
        String name = namePrefix + individual.toString();
        if( controls.contains( name ) )
            return name;
        name = DiagramUtility.validateName( name );
        SemanticRelation control = new SemanticRelation( controls, name );

        control.setTitle( getTitleName( individual ) );
        control.setRelationType( getStringProperty( "controlType", individual ) );
        control.setComment( getComments( individual ) );

        String outputName = null;

        OWLIndividual controller = getProperty( individual, "controller" );
        if( controller != null )
        {
            String type = getType( controller );
            if( isType( type, PHYSICAL_ENTITY_TYPES ) )
            {
                Base specie = parsePhysicalEntity( controller );
                //                SpecieReference specie = parsePhysicalEntityParticipant(controller, control, SpecieReference.MODIFIER);
                String specieName = Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                        + ru.biosoft.access.core.DataElementPath.escapeName( specie.getOrigin().getName() ) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                        + ru.biosoft.access.core.DataElementPath.escapeName( specie.getName() );
                control.setInputElementName( specieName );
            }
            else if( type.equals( "Pathway" ) )
            {
                Diagram diagram = readDiagram( null, diagrams, controller );
                diagrams.put( diagram );
                control.setInputElementName( Module.DIAGRAM + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + diagram.getName() );
            }
        }

        OWLIndividual controlled = getProperty( individual, "controlled" );
        if( controlled != null )
        {
            String type = getType( controlled );
            if( type.equals( "Pathway" ) )
            {
                Diagram diagram = readDiagram( null, diagrams, controlled );
                diagrams.put( diagram );
                control.setOutputElementName( Module.DIAGRAM + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + diagram.getName() );
            }
            else if( isType( type, CONTROL_TYPES ) )
            {
                outputName = parseControl( controlled, type );
                control.setOutputElementName(
                        Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( controls.getName() )
                                + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( outputName ) );
            }
            else if( isType( type, CONVERSION_TYPES ) || type.equals( "MolecularInteraction" ) )
            {
                outputName = parseConversion( controlled, type ).getName();
                control.setOutputElementName(
                        Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( conversions.getName() )
                                + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( outputName ) );
            }
            else if( type.equals( "GeneticInteraction" ) )
            {
                outputName = parseGeneticInteraction( controlled ).getName();
                control.setOutputElementName(
                        Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( conversions.getName() )
                                + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( outputName ) );

            }
            else if( type.equals( "TemplateReaction" ) )
            {
                outputName = parseTemplateReaction( controlled );
                control.setOutputElementName(
                        Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( controls.getName() )
                                + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( outputName ) );
            }
        }

        control.setDatabaseReferences( getDatabaseReference( individual, "xref" ) );
        control.setLiteratureReferences( getPublications( individual, "xref" ) );
        //TODO: participant property
        DynamicPropertySet dps = control.getAttributes();
        writeToDPS( dps, "Type", String.class, controlType );
        writeToDPS( dps, "Availability", String.class, getStringProperty( "availability", individual ) );
        writeToDPS( dps, "Synonyms", String.class, getSynonyms( individual ) );
        writeToDPS( dps, "DataSource", String[].class, getDataSource( individual ) );
        writeToDPS( dps, "Evidence", Evidence.class, getEvidence( individual ) );
        OpenControlledVocabulary ocv = getOCV( individual, "interactionType" );
        if( ocv != null )
        {
            writeToDPS( dps, "InteractionType", String.class,
                    Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( vocabulary.getName() )
                            + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( ocv.getName() ) );
        }

        if( controlType.equals( "Catalysis" ) )
        {
            String[] cofactors = getPropertiesByTypes( individual, "coFactor", PHYSICAL_ENTITY_TYPES )
                    .map( entity -> parsePhysicalEntityParticipant( entity, control, null ) )
                    .map( specie -> Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName( specie.getOrigin().getName() ) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName( specie.getName() ) )
                    .toArray( String[]::new );
            if( cofactors.length > 0 )
                writeToDPS( dps, "Cofactor", String[].class, cofactors );
            writeToDPS( dps, "Direction", String.class, getStringProperty( "catalysisDirection", individual ) );
        }
        controls.put( control );

        if( controlled != null && controller != null && isPhysicalEntityControl( controller, controlled ) )
            this.parsePhysicalEntityControl( individual, controller, controlled, controlType );
        addRelationLinks( control );
        return control.getName();
    }


    private Reaction parseConversion(OWLIndividual individual, String conversionType)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Reaction conversion = get( conversions, name );
        if( conversion == null )
        {
            conversion = new Reaction( conversions, name );
            conversion.setTitle( getTitleName( individual ) );
            conversion.setComment( getComments( individual ) );

            Set<SpecieReference> specieReferences = new HashSet<>();
            Map<String, String> st = parseStoichiometry( individual, "participantStoichiometry" );
            //TODO: if reaction direction is RIGHT_TO_LEFT than left is product and right is reactant
            if( !conversionType.equals( "MolecularInteraction" ) )
            {
                for( OWLIndividual ind : getPropertiesByTypes( individual, "left", PHYSICAL_ENTITY_TYPES ).toList() )
                {
                    SpecieReference specie = parsePhysicalEntityParticipant( ind, conversion, SpecieReference.REACTANT );
                    String indID = ind.toString();
                    if( st.containsKey( indID ) )
                        specie.setStoichiometry( st.get( indID ) );
                    specieReferences.add( specie );
                }

                for( OWLIndividual ind : getPropertiesByTypes( individual, "right", PHYSICAL_ENTITY_TYPES ).toList() )
                {
                    SpecieReference specie = parsePhysicalEntityParticipant( ind, conversion, SpecieReference.PRODUCT );
                    String indID = ind.toString();
                    if( st.containsKey( indID ) )
                        specie.setStoichiometry( st.get( indID ) );
                    specieReferences.add( specie );
                }
            }
            for( OWLIndividual ind : getPropertiesByTypes( individual, "participant", PHYSICAL_ENTITY_TYPES ).toList() )
            {
                SpecieReference specie = parsePhysicalEntityParticipant( ind, conversion, SpecieReference.OTHER );
                String indID = ind.toString();
                if( st.containsKey( indID ) )
                    specie.setStoichiometry( st.get( indID ) );
                specieReferences.add( specie );
            }

            conversion.setSpecieReferences( specieReferences.toArray( new SpecieReference[specieReferences.size()] ) );
            conversion.setDatabaseReferences( getDatabaseReference( individual, "xref" ) );
            conversion.setLiteratureReferences( getPublications( individual, "xref" ) );
            String conversionDirection = getStringProperty( "conversionDirection", individual );
            if( conversionDirection != null && conversionDirection.equals( "REVERSIBLE" ) )
            {
                conversion.setReversible( true );
            }

            DynamicPropertySet dps = conversion.getAttributes();
            writeToDPS( dps, "Type", String.class, conversionType );
            writeToDPS( dps, "Availability", String.class, getStringProperty( "availability", individual ) );
            writeToDPS( dps, "Synonyms", String.class, getSynonyms( individual ) );
            writeToDPS( dps, "DataSource", String[].class, getDataSource( individual ) );
            writeToDPS( dps, "Evidence", Evidence.class, getEvidence( individual ) );
            if( !conversionType.equals( "MolecularInteraction" ) )
            {
                writeToDPS( dps, "Spontaneous", String.class, getStringProperty( "spontaneous", individual ) );
            }
            OpenControlledVocabulary ocv = getOCV( individual, "interactionType" );
            if( ocv != null )
            {
                writeToDPS( dps, "InteractionType", String.class,
                        Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( vocabulary.getName() )
                                + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( ocv.getName() ) );
            }
            if( conversionType.equals( "BiochemicalReaction " ) || conversionType.equals( "TransportWithBiochemicalReaction" ) )
            {
                writeToDPS( dps, "DeltaG", String.class, getStringProperty( "deltaG", individual ) );
                writeToDPS( dps, "DeltaH", String.class, getStringProperty( "deltaH", individual ) );
                writeToDPS( dps, "DeltaS", String.class, getStringProperty( "deltaS", individual ) );
                writeToDPS( dps, "EcNumber", String[].class, getStringListProperty( "eCNumber", individual ) );
                writeToDPS( dps, "Keq", String.class, getStringProperty( "kEQ", individual ) );
            }
            conversions.put( conversion );
            addReactionLinks( conversion );
        }
        return conversion;
    }

    private Reaction parseGeneticInteraction(OWLIndividual individual) throws Exception
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        if( conversions.contains( name ) )
            return conversions.get( name );

        Reaction gi = new Reaction( conversions, name );

        gi.setTitle( getTitleName( individual ) );
        gi.setComment( getComments( individual ) );
        gi.setDatabaseReferences( getDatabaseReference( individual, "xref" ) );
        gi.setLiteratureReferences( getPublications( individual, "xref" ) );

        SpecieReference[] specieReferences = getPropertiesByTypes( individual, "participant", "Gene" ).map( ind -> parseGene( ind, gi ) )
                .toArray( SpecieReference[]::new );
        gi.setSpecieReferences( specieReferences );

        DynamicPropertySet dps = gi.getAttributes();
        writeToDPS( dps, "Type", String.class, "GeneticInteraction" );
        writeToDPS( dps, "Availability", String.class, getStringProperty( "availability", individual ) );
        writeToDPS( dps, "Synonyms", String.class, getSynonyms( individual ) );
        writeToDPS( dps, "DataSource", String[].class, getDataSource( individual ) );
        writeToDPS( dps, "Evidence", Evidence.class, getEvidence( individual ) );

        Confidence[] intScores = getPropertiesByTypes( individual, "interactionScore", "Score" ).map( this::parseScore )
                .toArray( Confidence[]::new );
        if( intScores.length > 0 )
        {
            writeToDPS( dps, "InteractionScore", Confidence[].class, intScores );
        }
        OpenControlledVocabulary ocv = getOCV( individual, "interactionType" );
        if( ocv != null )
        {
            writeToDPS( dps, "InteractionType", String.class,
                    Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( vocabulary.getName() )
                            + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( ocv.getName() ) );
        }
        ocv = getOCV( individual, "phenotype" );
        if( ocv != null )
        {
            writeToDPS( dps, "Phenotype", String.class,
                    Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( vocabulary.getName() )
                            + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( ocv.getName() ) );
        }
        conversions.put( gi );
        addReactionLinks( gi );
        return gi;
    }

    private String parseTemplateReaction(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        if( controls.contains( name ) )
            return name;
        SemanticRelation control = new SemanticRelation( controls, name );

        control.setTitle( getTitleName( individual ) );
        control.setRelationType( "TemplateReaction" );
        control.setComment( getComments( individual ) );

        getPropertiesByTypes( individual, "product", PHYSICAL_ENTITY_TYPES ).findAny()
                .map( ind -> parsePhysicalEntityParticipant( ind, control, null ) )
                .map( specie -> Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( specie.getOrigin().getName() )
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( specie.getName() ) )
                .ifPresent( control::setOutputElementName );

        getPropertiesByTypes( individual, "template", PHYSICAL_ENTITY_TYPES ).findAny()
                .map( ind -> parsePhysicalEntityParticipant( ind, control, null ) )
                .map( specie -> Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( specie.getOrigin().getName() )
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( specie.getName() ) )
                .ifPresent( control::setInputElementName );

        control.setDatabaseReferences( getDatabaseReference( individual, "xref" ) );
        control.setLiteratureReferences( getPublications( individual, "xref" ) );
        DynamicPropertySet dps = control.getAttributes();
        writeToDPS( dps, "Type", String.class, "TemplateReaction" );
        writeToDPS( dps, "Availability", String.class, getStringProperty( "availability", individual ) );
        writeToDPS( dps, "Synonyms", String.class, getSynonyms( individual ) );
        writeToDPS( dps, "DataSource", String[].class, getDataSource( individual ) );
        writeToDPS( dps, "Evidence", Evidence.class, getEvidence( individual ) );
        writeToDPS( dps, "TemplateDirection", String.class, getStringProperty( "templateDirection", individual ) );
        OpenControlledVocabulary ocv = getOCV( individual, "interactionType" );
        if( ocv != null )
        {
            writeToDPS( dps, "InteractionType", String.class,
                    Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( vocabulary.getName() )
                            + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( ocv.getName() ) );
        }
        controls.put( control );
        addRelationLinks( control );

        return control.getName();
    }

    private Map<String, String> parseStoichiometry(OWLIndividual individual, String tag)
    {
        // physicalEntity, comment
        Map<String, String> stoich = new HashMap<>();
        for( OWLIndividual ind : getPropertiesByTypes( individual, tag, "Stoichiometry" ).toList() )
        {
            OWLObjectPropertyImpl ope = new OWLObjectPropertyImpl( factory, createURI( "physicalEntity" ) );
            Set<OWLIndividual> entities = ind.getObjectPropertyValues( ontology ).get( ope );
            String[] coef = getStringListProperty( "stoichiometricCoefficient", ind );
            if( entities != null )
            {
                boolean each = ( coef.length == entities.size() );
                int i = 0;
                for( OWLIndividual ind2 : entities )
                {
                    String name = ind2.toString();
                    String c = each ? coef[i] : coef[0];
                    stoich.put( name, c );
                }
            }
        }
        return stoich;
    }

    ////////////////////////////////////
    // Reaction and pathway participants
    //
    private SpecieReference parsePhysicalEntityParticipant(OWLIndividual individual, BaseSupport parent, String role)
    {
        String name = namePrefix + individual.toString() + "_as_" + ( ( role != null ) ? role : SpecieReference.OTHER );
        name = DiagramUtility.validateName( name );
        SpecieReference sr = get( participants, name );
        if( sr == null )
        {
            sr = new SpecieReference( participants, name );
            sr.setParent( parent );
            sr.setRole( role == null ? SpecieReference.OTHER : role );
            Base base = parsePhysicalEntity( individual );
            if( base != null )
            {
                sr.setSpecie( getRelativeToModuleName( base ) );
                sr.setTitle( base.getTitle() );
                //                addMatchingLink(base.getName(), dbRefs, true);
            }
            else
                sr.setSpecie( "" );
            participants.put( sr );
        }
        return sr;
    }

    /**
     * Creates object of PhysicalEntity. This is an abstract object.
     */
    private Concept parseAbstractPhysicalEntity(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Concept concept = get( physicalEntities, name );
        if( concept == null )
        {
            concept = new Concept( physicalEntities, name );
            parsePhysicalEntity( concept, individual );
            physicalEntities.put( concept );
        }
        return concept;
    }

    /**
     * Creates Protein object. Subclass of PhysicalEntity.
     * Here we read only specific for Protein attributes.
     */
    private Protein parseProtein(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Protein protein = get( proteins, name );
        if( protein == null )
        {
            protein = new Protein( proteins, name );
            parsePhysicalEntity( protein, individual );
            setEntityReferenceAttr( protein, individual );
            proteins.put( protein );
        }
        return protein;
    }

    /**
     * Creates SmallMolecule object. Subclass of PhysicalEntity.
     * Here we read only specific for SmallMolecule attributes.
     */
    private Substance parseSmallMolecule(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Substance smallMolecule = get( smallMolecules, name );
        if( smallMolecule == null )
        {
            smallMolecule = new Substance( smallMolecules, name );
            parsePhysicalEntity( smallMolecule, individual );
            setEntityReferenceAttr( smallMolecule, individual );
            smallMolecules.put( smallMolecule );
        }
        return smallMolecule;
    }


    private Base parseDNA(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        DNA dna = get( dnas, name );
        if( dna == null )
        {
            dna = new DNA( dnas, name );
            parsePhysicalEntity( dna, individual );
            setEntityReferenceAttr( dna, individual );
            dnas.put( dna );
        }
        return dna;
    }

    private Base parseRNA(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        RNA rna = get( rnas, name );
        if( rna == null )
        {
            rna = new RNA( rnas, name );
            parsePhysicalEntity( rna, individual );
            setEntityReferenceAttr( rna, individual );
            rnas.put( rna );
        }
        return rna;
    }

    private Base parseDNARegion(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Concept entity = get( physicalEntities, name );
        if( entity == null )
        {
            entity = new Concept( physicalEntities, name );
            parsePhysicalEntity( entity, individual );
            setEntityReferenceAttr( entity, individual );
            physicalEntities.put( entity );
        }
        return entity;
    }

    private Base parseRNARegion(OWLIndividual individual)
    {
        return parseDNARegion( individual );
    }

    private void setEntityReferenceAttr(Base base, OWLIndividual individual)
    {
        OWLIndividual entityReference = getProperty( individual, "entityReference" );
        if( entityReference != null )
        {
            Base reference = parseEntityReference( entityReference );
            writeToDPS( base.getAttributes(), "EntityReference", DataElementPath.class, reference.getCompletePath());
        }
    }

    /**
     * Creates Complex object. Subclass of PhysicalEntity.
     * Here we read only specific for Complex attributes.
     */
    private Complex parseComplex(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Complex complex = get( complexes, name );
        if( complex == null )
        {
            complex = new Complex( complexes, name );
            parsePhysicalEntity( complex, individual );
            Map<String, String> stoichiometry = parseStoichiometry( individual, "componentStoichiometry" );
            Set<String> components = new HashSet<>();
            for( OWLIndividual ind : getPropertiesByTypes( individual, "component", PHYSICAL_ENTITY_TYPES ).toList() )
            {
                Base substance = parsePhysicalEntity( ind );
                String id = ind.toString();
                if( stoichiometry.containsKey( id ) )
                {
                    //in BioPAX 3 when entity is in complex it is considered as a new entity in new state (bound) thus we may assign it its stiochiometry
                    writeToDPS( substance.getAttributes(), "componentStoichiometry", String.class, stoichiometry.get( id ) );
                }
                components.add( Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( substance.getOrigin().getName() )
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( substance.getName() ) );
            }
            DynamicPropertySet dps = complex.getAttributes();
            writeToDPS( dps, "Components", String[].class, components.toArray( new String[components.size()] ) );
            complexes.put( complex );
        }
        return complex;
    }

    private SpecieReference parseGene(OWLIndividual individual, BaseSupport parent)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        SpecieReference sr = get( participants, name );
        if( sr == null )
        {
            sr = new SpecieReference( participants, name );
            sr.setParent( parent );
            sr.setRole( SpecieReference.OTHER );
            //            parsePhysicalEntity(sr, individual);

            DynamicPropertySet dps = sr.getAttributes();
            writeToDPS( dps, "Type", String.class, "Gene" );
            writeToDPS( dps, "Organism", String.class, getOrganism( individual ) );
            writeToDPS( dps, "StandardName", String.class, getStandardName( individual ) );
            writeToDPS( dps, "DatabaseReference", DatabaseReference[].class, getDatabaseReference( individual, "xref" ) );
            writeToDPS( dps, "LiteratureReference", String[].class, getPublications( individual, "xref" ) );

            participants.put( sr );
        }
        return sr;
    }

    private Base parseEntityReference(OWLIndividual individual)
    {
        String type = getType( individual );
        if( type == null )
            return parseAbstractEntityReference( individual );

        switch( type )
        {
            case "ProteinReference":
                return parseProteinReference( individual );
            case "SmallMoleculeReference":
                return parseSmallMoleculeReference( individual );
            case "DNAReference":
                return parseDnaReference( individual );
            case "RNAReference":
                return parseRnaReference( individual );
            case "DNARegionReference":
                return parseDnaRegionReference( individual );
            case "RNARegionReference":
                return parseRnaRegionReference( individual );
            default:
                return parseAbstractEntityReference( individual );
        }
    }

    private Base parseAbstractEntityReference(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Concept entity = get( physicalEntities, name );
        if( entity == null )
        {
            entity = new Concept( physicalEntities, name );
            parseEntityReference( entity, individual );
            physicalEntities.put( entity );
        }
        return entity;
    }

    private Base parsePhysicalEntity(OWLIndividual individual)
    {
        String type = getType( individual );
        if( type == null )
            return parseAbstractPhysicalEntity( individual );

        switch( type )
        {
            case "Protein":
                return parseProtein( individual );
            case "SmallMolecule":
                return parseSmallMolecule( individual );
            case "Complex":
                return parseComplex( individual );
            case "DNA":
                return parseDNA( individual );
            case "RNA":
                return parseRNA( individual );
            case "DnaRegion":
                return parseDNARegion( individual );
            case "RnaRegion":
                return parseRNARegion( individual );
            default:
                return parseAbstractPhysicalEntity( individual );
        }
    }

    /**
     * Reads entity properties (root class for all biological classes in BioPAX ontology)
     */
    private void parseEntity(Concept entity, OWLIndividual individual)
    {
        String title = getDisplayName( individual );
        String[] names = getNames( individual );
        String standardName = getStandardName( individual );

        if( title == null )
            if( standardName != null )
                title = standardName;
            else if( names.length > 0 )
                title = names[0];
        if( title != null )
            entity.setTitle( title );

        if( names.length > 0 )
            entity.setSynonyms( String.join( ", ", names ) );

        entity.setDatabaseReferences( getDatabaseReference( individual, "xref" ) );
        entity.setLiteratureReferences( getPublications( individual, "xref" ) );

        entity.setComment( getComments( individual ) );
        DynamicPropertySet dps = entity.getAttributes();
        writeToDPS( dps, "Availability", String.class, getStringProperty( "availability", individual ) );
        writeToDPS( dps, "DataSource", String[].class, getDataSource( individual ) );
        writeToDPS( dps, "StandardName", String.class, standardName );
        writeToDPS( dps, "Evidence", Evidence.class, getEvidence( individual ) );
    }

    /**
     * Reads PhysicalEntity properties. Subclass of Entity. Defined as a pool of entities, where each enity has a physical structure.
     * Subclasses are: Protein, SmallMolecule, DNA, RNA, Complex
     * TODO: use cellular location to define compartment of entity
     */
    private void parsePhysicalEntity(Concept entity, OWLIndividual individual)
    {
        parseEntity( entity, individual );
        DynamicPropertySet dps = entity.getAttributes();
        OpenControlledVocabulary ocv = getOCV( individual, "cellularLocation" );
        if( ocv != null )
        {
            writeToDPS( dps, "CellularLocation", String.class,
                    Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( vocabulary.getName() )
                            + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( ocv.getName() ) );
        }
        ru.biosoft.access.core.DataElementPath[] features = getFeaturePaths( individual, "feature" );
        if( features.length > 0 )
            writeToDPS( dps, "FeatureList", DataElementPath[].class, features );
        ru.biosoft.access.core.DataElementPath[] notFeatures = getFeaturePaths( individual, "notFeature" );
        if( notFeatures.length > 0 )
            writeToDPS( dps, "NotFeatureList", DataElementPath[].class, notFeatures);
        //TODO: memberPhysicalEntity property support
    }


    /**
     * Parse EntityReference class
     * Subclasses: DnaReference, ProteinReference, RnaReference, SmallMoleculeReference
     *
     * An entity reference is a grouping of several physical entities across different contexts and molecular states, that share
     * common physical properties and often named and treated as a single entity with multiple states by biologists.
     */
    private void parseEntityReference(Concept entity, OWLIndividual individual)
    {
        String title = getDisplayName( individual );
        String[] names = getNames( individual );
        String standardName = getStandardName( individual );

        if( title == null )
            if( standardName != null )
                title = standardName;
            else if( names.length > 0 )
                title = names[0];
        if( title != null )
            entity.setTitle( title );

        if( names.length > 0 )
            entity.setSynonyms( String.join( ", ", names ) );

        entity.setDatabaseReferences( getDatabaseReference( individual, "xref" ) );
        entity.setLiteratureReferences( getPublications( individual, "xref" ) );

        DynamicPropertySet dps = entity.getAttributes();
        writeToDPS( dps, "StandardName", String.class, standardName );
        writeToDPS( dps, "Evidence", Evidence.class, getEvidence( individual ) );
        ru.biosoft.access.core.DataElementPath[] features = getFeaturePaths( individual, "entityFeature" );
        if( features.length > 0 )
            writeToDPS( dps, "FeatureList", DataElementPath[].class, features );
        OpenControlledVocabulary ocv = getOCV( individual, "entityReferenceType" );
        if( ocv != null )
        {
            writeToDPS( dps, "EntityReferenceType", String.class,
                    Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( vocabulary.getName() )
                            + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( ocv.getName() ) );
        }
        //TODO: memberEntityReference property support
    }

    private Base parseDnaReference(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        DNA dna = get( dnas, name );
        if( dna == null )
        {
            dna = new DNA( dnas, name );
            dna.setComment( getComments( individual ) );
            parseEntityReference( dna, individual );
            DynamicPropertySet dps = dna.getAttributes();
            writeToDPS( dps, "Organism", String.class, getOrganism( individual ) );
            writeToDPS( dps, "Sequence", String.class, getStringProperty( "sequence", individual ) );
            dnas.put( dna );
        }
        return dna;
    }

    private Base parseDnaRegionReference(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Concept entity = get( physicalEntities, name );
        if( entity == null )
        {
            entity = new Concept( physicalEntities, name );
            parseEntityReference( entity, individual );
            physicalEntities.put( entity );
        }
        //TODO containerEntityReference
        DynamicPropertySet dps = entity.getAttributes();
        writeToDPS( dps, "Evidence", Evidence.class, getEvidence( individual ) );
        writeToDPS( dps, "Sequence", String.class, getStringProperty( "sequence", individual ) );
        writeToDPS( dps, "AbsoluteRegion", Concept[].class, getFeatureLocations( individual, "absoluteRegion" ) );

        Set<String> subRegion = getPropertiesByTypes( individual, "subRegion", ENTITY_REFERENCE_TYPES ).map( this::parseEntityReference )
                .map( DataElementPath::create ).map( Object::toString ).toSet();
        if( !subRegion.isEmpty() )
            writeToDPS( dps, "SubRegion", String[].class, subRegion.toArray( new String[subRegion.size()] ) );
        OpenControlledVocabulary ocv = getOCV( individual, "regionType" );
        if( ocv != null )
        {
            writeToDPS( dps, "RegionType", String.class,
                    Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( vocabulary.getName() )
                            + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( ocv.getName() ) );
        }
        return entity;
    }

    private Base parseProteinReference(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Protein proteinReference = get( proteins, name );
        if( proteinReference == null )
        {
            proteinReference = new Protein( proteins, name );
            proteinReference.setComment( getComments( individual ) );
            parseEntityReference( proteinReference, individual );
            DynamicPropertySet dps = proteinReference.getAttributes();
            writeToDPS( dps, "Organism", String.class, getOrganism( individual ) );
            writeToDPS( dps, "Sequence", String.class, getStringProperty( "sequence", individual ) );
            proteins.put( proteinReference );
        }
        return proteinReference;
    }
    private Base parseRnaReference(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        RNA rna = get( rnas, name );
        if( rna == null )
        {
            rna = new RNA( rnas, name );
            rna.setComment( getComments( individual ) );
            parseEntityReference( rna, individual );
            DynamicPropertySet dps = rna.getAttributes();
            writeToDPS( dps, "Organism", String.class, getOrganism( individual ) );
            writeToDPS( dps, "Sequence", String.class, getStringProperty( "sequence", individual ) );
            rnas.put( rna );
        }
        return rna;
    }

    private Base parseRnaRegionReference(OWLIndividual individual)
    {
        return parseDnaRegionReference( individual );
    }

    private Base parseSmallMoleculeReference(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Substance smallMoleculeReference = get( smallMolecules, name );
        if( smallMoleculeReference == null )
        {
            smallMoleculeReference = new Substance( smallMolecules, name );
            smallMoleculeReference.setComment( getComments( individual ) );
            parseEntityReference( smallMoleculeReference, individual );
            DynamicPropertySet dps = smallMoleculeReference.getAttributes();
            writeToDPS( dps, "Availability", String.class, getStringProperty( "availability", individual ) );
            writeToDPS( dps, "DataSource", String[].class, getDataSource( individual ) );
            writeToDPS( dps, "ChemicalFormula", String.class, getStringProperty( "chemicalFormula", individual ) );
            writeToDPS( dps, "MolecularWeight", String.class, getStringProperty( "molecularWeight", individual ) );

            getPropertiesByTypes( individual, "structure", "ChemicalStructure" ).map( this::parseChemicalStructure )
                    .forEach( structure -> writeToDPS( dps, "ChemicalStructure", Structure.class, structure ) );
            smallMolecules.put( smallMoleculeReference );
        }
        return smallMoleculeReference;
    }

    private Structure parseChemicalStructure(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Structure cs = new Structure( null, name );
        cs.setData( getStringProperty( "structureData", individual ) );
        cs.setFormat( getStringProperty( "structureFormat", individual ) );
        cs.setComment( getComments( individual ) );
        return cs;
    }

    private DatabaseReference parseDatabaseReference(OWLIndividual individual, String type)
    {
        DatabaseReference dr = new DatabaseReference();
        dr.setComment( getComments( individual ) );
        dr.setDatabaseVersion( getStringProperty( "dbVersion", individual ) );
        dr.setDatabaseName( getStringProperty( "db", individual ) );
        dr.setIdVersion( getStringProperty( "idVersion", individual ) );
        dr.setId( getStringProperty( "id", individual ) );
        if( type.equals( "UnificationXref" ) )
        {
            dr.setRelationshipType( type );
        }
        else
        {
            String rt = getStringProperty( "relationshipType", individual );
            //TODO: relationshipType is an array of properties
            if( rt != null )
            {
                dr.setRelationshipType( rt );
            }
            else
            {
                dr.setRelationshipType( type );
            }
        }
        return dr;
    }

    private Publication parsePublication(OWLIndividual individual)
    {
        String name = namePrefix + individual.toString();
        if( publications.contains( name ) )
        {
            return get( publications, name );
        }
        Publication pb = new Publication( publications, name );
        String title = TextUtil2.nullToEmpty( getStringProperty( "title", individual ) );

        String authors = getAuthors( individual );
        if( authors.isEmpty() )
        {
            int pos = title.lastIndexOf( ". ", title.length() - 2 );
            if( pos > 0 )
            {
                // Work-around possibly buggy cases in HumanCyc BioPAX files where title is merged with authors like:
                // Unified nomenclature for Eph family receptors and their ligands, the ephrins. Eph Nomenclature Committee.
                authors = title.substring( pos + 2, title.length() - 1 );
                title = title.substring( 0, pos + 1 );
            }
            else
            {
                authors = "";
            }
        }
        pb.setAuthors( authors );

        pb.setComment( getComments( individual ) );
        String year = getStringProperty( "year", individual );
        if( year != null )
        {
            pb.setYear( year );
        }
        pb.setTitle( title );
        pb.setFullTextURL( getStringProperty( "url", individual ) );
        String db = getStringProperty( "db", individual );
        String id = getStringProperty( "id", individual );
        if( db != null && db.equalsIgnoreCase( "PubMed" ) )
            pb.setPubMedId( id );
        pb.setDb( db );
        pb.setDbVersion( getStringProperty( "dbVersion", individual ) );
        pb.setIdName( id );
        pb.setIdVersion( getStringProperty( "idVersion", individual ) );
        pb.setSimpleSource( getStringProperty( "source", individual ) );
        publications.put( pb );

        return pb;
    }
    private BioSource parseBioSource(OWLIndividual individual)
    {
        String name = namePrefix + individual.toString();
        BioSource bs = get( organisms, name );
        if( bs == null )
        {
            bs = new BioSource( null, name );
            bs.setCompleteName( getName( individual ) );
            bs.setTitle( getTitleName( individual ) );
            bs.setComment( getComments( individual ) );
            //TODO: tissue and cellType are OCV properties
            bs.setTissue( getStringProperty( "tissue", individual ) );
            bs.setDatabaseReferences( getDatabaseReference( individual, "xref" ) );
            bs.setCelltype( getStringProperty( "cellType", individual ) );

            organisms.put( bs );
        }
        return bs;
    }

    private DatabaseInfo parseProvenance(OWLIndividual individual)
    {
        String name = namePrefix + getName( individual );
        DatabaseInfo di = get( dataSources, name );
        if( di == null )
        {
            di = new DatabaseInfo( null, name );
            di.setComment( getComments( individual ) );
            di.setLiteratureReferences( getPublications( individual, "xref" ) );
            di.setDatabaseReferences( getDatabaseReference( individual, "xref" ) );
            dataSources.put( di );
        }
        return di;
    }

    private OpenControlledVocabulary parseOpenControlledVocabulary(OWLIndividual individual)
    {
        String name = namePrefix + individual.toString();
        OpenControlledVocabulary sp = get( vocabulary, name );
        if( sp == null )
        {
            //there was null as origin
            sp = new OpenControlledVocabulary( vocabulary, name );
            sp.setComment( getComments( individual ) );
            sp.setTerm( getStringProperty( "term", individual ) );
            sp.setDatabaseReferences( getDatabaseReference( individual, "xref" ) );
            sp.setLiteratureReferences( getPublications( individual, "xref" ) );
            sp.setVocabularyType( getType( individual ) );

            vocabulary.put( sp );
        }
        return sp;
    }

    private Evidence parseEvidence(OWLIndividual individual)
    {
        String name = namePrefix + individual.toString();
        Evidence ev = new Evidence( null, name );
        ev.setComment( getComments( individual ) );
        ev.setConfidence( getConfidence( individual ) );
        OpenControlledVocabulary ocv = getOCV( individual, "evidenceCode" );
        if( ocv != null )
        {
            ev.setEvidenceCode( Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( vocabulary.getName() )
                    + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( ocv.getName() ) );
        }
        ev.setDatabaseReferences( getDatabaseReference( individual, "xref" ) );
        ev.setLiteratureReferences( getPublications( individual, "xref" ) );

        return ev;
    }

    private Confidence parseScore(OWLIndividual individual)
    {
        String name = namePrefix + individual.toString();
        Confidence ev = new Confidence( null, name );
        ev.setComment( getComments( individual ) );
        ev.setConfidenceValue( getStringProperty( "value", individual ) );
        DatabaseInfo ds = parseProvenance( individual );
        ev.setDatabaseReferences( ds.getDatabaseReferences() );
        ev.setLiteratureReferences( ds.getLiteratureReferences() );
        //ev.setDatabaseReferences(getDatabaseReference(individual, "xref"));
        //ev.setLiteratureReferences(getPublications(individual, "xref"));

        return ev;
    }

    private EntityFeature parseEntityFeature(OWLIndividual individual, String type)
    {
        String name = namePrefix + individual.toString();
        EntityFeature ef = get( entityFeature, name );
        if( ef != null )
            return ef;

        ef = new EntityFeature( null, namePrefix + individual.toString() );

        ef.setComment( getComments( individual ) );
        OpenControlledVocabulary ocv = getOCV( individual, "featureLocationType" );
        if( ocv != null )
        {
            ef.setFeatureType( DataElementPath.create( ocv ).toString() );
        }
        ef.setDatabaseReferences( getDatabaseReference( individual, "xref" ) );
        ef.setLiteratureReferences( getPublications( individual, "xref" ) );
        ef.setFeatureLocation( getFeatureLocations( individual, "featureLocation" ) );
        ef.setMemberFeature( getMemberFeatures( individual ) );
        DynamicPropertySet dps = ef.getAttributes();
        writeToDPS( dps, "Evidence", Evidence.class, getEvidence( individual ) );
        writeToDPS( dps, "Type", String.class, type );
        entityFeature.put( ef );
        return ef;
    }

    //TODO: store features in database and provide links
    private EntityFeature parseBindingFeature(OWLIndividual individual, String type)
    {
        String name = namePrefix + individual.toString();
        EntityFeature ef = get( entityFeature, name );
        if( ef != null )
            return ef;

        ef = parseEntityFeature( individual, type );
        DynamicPropertySet dps = ef.getAttributes();
        writeToDPS( dps, "Intramolecular", Boolean.class, Boolean.parseBoolean( getStringProperty( "intramolecular", individual ) ) );
        ru.biosoft.access.core.DataElementPath[] bindsTo = getFeaturePaths( individual, "bindsTo" );
        if( bindsTo.length > 0 )
            writeToDPS( dps, "bindsTo", DataElementPath[].class, bindsTo );

        entityFeature.put( ef );
        return ef;
    }

    private EntityFeature parseFragmentFeature(OWLIndividual individual, String type)
    {
        return parseEntityFeature( individual, type );
    }

    private EntityFeature parseModificationFeature(OWLIndividual individual, String type)
    {
        String name = namePrefix + individual.toString();
        EntityFeature ef = get( entityFeature, name );
        if( ef != null )
            return ef;

        ef = parseEntityFeature( individual, type );
        OpenControlledVocabulary ocv = getOCV( individual, "modificationType" );
        if( ocv != null )
        {
            String mt = Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( vocabulary.getName() )
                    + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( ocv.getName() );
            DynamicPropertySet dps = ef.getAttributes();
            writeToDPS( dps, "ModificationType", String.class, mt );
        }
        entityFeature.put( ef );
        return ef;
    }

    private SequenceInterval parseSequenceInterval(OWLIndividual individual)
    {
        SequenceInterval si = new SequenceInterval( null, namePrefix + getName( individual ) );
        si.setComment( getComments( individual ) );
        si.setBegin( getSequenceSite( individual, "sequenceIntervalBegin" ) );
        si.setEnd( getSequenceSite( individual, "sequenceIntervalEnd" ) );
        return si;
    }

    private SequenceSite parseSequenceSite(OWLIndividual individual)
    {
        SequenceSite ss = new SequenceSite( null, namePrefix + getName( individual ) );
        ss.setComment( getComments( individual ) );
        ss.setPositionStatus( getStringProperty( "positionStatus", individual ) );
        ss.setSequencePosition( getStringProperty( "sequencePosition", individual ) );

        return ss;
    }

    ////////////////////////////////////////////////////////////////////////////
    // get properties from individuals
    /**
     * displayName values are short names suitable for display in a graphic
     * standardName is name that follow a standard nomenclature, like systematic yeast ORF names (e.g. YJL034W) (can be null)
     * name is generic name property, can be null, can be multiple
     */
    private String getName(OWLIndividual individual)
    {
        String name = null;
        String[] names = getNames( individual );
        if( names.length > 0 )
            name = names[0];
        else
        {
            name = getStandardName( individual );
            if( name == null )
                name = getDisplayName( individual );
            if( name == null )
                name = individual.toString();
        }
        return HtmlUtil.stripHtml( name ).replaceAll( "[/:]", "_" );
    }

    private String getTitleName(OWLIndividual individual)
    {
        String title = getDisplayName( individual );
        if( title != null )
            return title;

        title = getStandardName( individual );
        if( title != null )
            return title;

        String[] names = getNames( individual );
        if( names.length > 0 )
            return names[0];

        return individual.toString();
    }

    private String getDisplayName(OWLIndividual individual)
    {
        return getStringProperty( "displayName", individual );
    }

    private String getStandardName(OWLIndividual individual)
    {
        return getStringProperty( "standardName", individual );
    }

    private String getStringProperty(String propertyName, OWLIndividual individual)
    {
        OWLDataPropertyExpression ode = new OWLDataPropertyImpl( factory, createURI( propertyName ) );
        Set<OWLConstant> param = individual.getDataPropertyValues( ontology ).get( ode );
        if( param == null )
            return null;
        return param.iterator().next().getLiteral();
    }

    private String[] getNames(OWLIndividual individual)
    {
        return getStringListProperty( "name", individual );
    }

    private String getOrganism(OWLIndividual individual)
    {
        return getPropertiesByTypes( individual, "organism", "BioSource" ).findAny().map( this::parseBioSource )
                .map( bs -> Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( organisms.getName() )
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( bs.getName() ) )
                .orElse( null );
    }

    private String[] getStringListProperty(String propertyName, OWLIndividual individual)
    {
        OWLDataPropertyExpression ode = new OWLDataPropertyImpl( factory, createURI( propertyName ) );
        Set<OWLConstant> param = individual.getDataPropertyValues( ontology ).get( ode );
        if( param != null )
        {
            return param.stream().map( OWLConstant::getLiteral ).sorted().distinct().toArray( String[]::new );
        }
        return new String[0];
    }

    private String getComments(OWLIndividual individual)
    {
        return join( Stream.of( getStringListProperty( "comment", individual ) )
                .map( s -> s.replaceAll( "[^a-zA-Z0-9!@#$%^&*()\\?;=+',-:_<>.\"/\\\\]", " " ).trim() ).filter( TextUtil2::nonEmpty )
                .toArray( String[]::new ) ).trim();
    }

    private @Nonnull String getAuthors(OWLIndividual individual)
    {
        return String.join( "\n", getStringListProperty( "author", individual ) );
    }

    private String getSynonyms(OWLIndividual individual)
    {
        String result = String.join( "; ", getStringListProperty( "synonyms", individual ) );
        return result.isEmpty() ? null : result;
    }

    private DatabaseReference[] getDatabaseReference(OWLIndividual individual, String tag)
    {
        List<DatabaseReference> xrefs = new ArrayList<>();
        for( OWLIndividual ind : getProperties( individual, tag ) )
        {
            String type = getType( ind );
            if( type.equals( "xref" ) || type.equals( "UnificationXref" ) || type.equals( "RelationshipXref" ) )
            {
                //                if( getStringProperty( "db", individual ) != null )
                xrefs.add( parseDatabaseReference( ind, type ) );
            }
            if( type.equals( "PublicationXref" ) )
            {
                Publication pb = parsePublication( ind );
                if( pb.getPubMedId() != null )
                    xrefs.add( getPubMedReference( pb ) );
            }
        }
        return xrefs.size() == 0 ? null : xrefs.toArray( new DatabaseReference[xrefs.size()] );
    }

    /**
     * @param pb
     * @param pubMedId
     * @return DatabaseReference to this pubmed ID
     */
    private DatabaseReference getPubMedReference(Publication pb)
    {
        String pubMedId = pb.getPubMedId();
        DatabaseReference result = new DatabaseReference( "PubMed", pubMedId );
        result.setRelationshipType( "PublicationXref" );
        result.setComment( pb.getReference() );
        return result;
    }

    private String[] getPublications(OWLIndividual individual, String tag)
    {
        String[] xrefs = getPropertiesByTypes( individual, tag, "PublicationXref" ).map( this::parsePublication )
                .map( Publication::getReference ).toArray( String[]::new );
        return xrefs.length == 0 ? null : xrefs;
    }

    private String[] getDataSource(OWLIndividual individual)
    {
        return getPropertiesByTypes( individual, "dataSource", "Provenance" ).map( this::parseProvenance )
                .map( ds -> Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( dataSources.getName() )
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( ds.getName() ) )
                .sorted().distinct().toArray( String[]::new );
    }

    private DataElementPath[] getFeaturePaths(OWLIndividual individual, String term)
    {
        return StreamEx.of( getEntityFeatures( individual, term ) ).map( f -> f.getCompletePath() ).toArray( DataElementPath[]::new );
    }

    private EntityFeature[] getEntityFeatures(OWLIndividual individual, String term)
    {
        Set<EntityFeature> dis = new HashSet<>();
        for( OWLIndividual ind : getProperties( individual, term ) )
        {
            String type = getType( ind );
            if( type.equals( "EntityFeature" ) )
            {
                dis.add( parseEntityFeature( ind, type ) );
            }
            else if( type.equals( "BindingFeature" ) || type.equals( "CovalentBindingFeature" ) )
            {
                dis.add( parseBindingFeature( ind, type ) );
            }
            else if( type.equals( "FragmentFeature" ) )
            {
                dis.add( parseFragmentFeature( ind, type ) );
            }
            else if( type.equals( "ModificationFeature" ) )
            {
                dis.add( parseModificationFeature( ind, type ) );
            }
        }
        return dis.toArray( new EntityFeature[dis.size()] );
    }

    /**
     * @param ind
     * @return
     */


    private OpenControlledVocabulary getOCV(OWLIndividual individual, String tag)
    {
        return getPropertiesByTypes( individual, tag, VOCABULARY_TYPES ).findAny().map( this::parseOpenControlledVocabulary )
                .orElse( null );
    }

    private Evidence getEvidence(OWLIndividual individual)
    {
        return getPropertiesByTypes( individual, "Evidence", "Evidence" ).findAny().map( this::parseEvidence ).orElse( null );
    }

    private Confidence getConfidence(OWLIndividual individual)
    {
        return getPropertiesByTypes( individual, "confidence", "Score" ).findAny().map( this::parseScore ).orElse( null );
    }

    /*private SequenceInterval getFeatureLocation(OWLIndividual individual) throws Exception
    {
        for(OWLIndividual ind: getProperties(individual, "featureLocation") )
        {
            String type = getType(ind);
            if( type.equals("SequenceInterval") )
            {
                return parseSequenceInterval(ind);
            }
            else if( type.equals("SequenceSite") )
            {

            }
            else if( type.equals("SequenceLocation") )
            {

            }
        }
        return null;
    }*/

    private Concept[] getFeatureLocations(OWLIndividual individual, String tag)
    {
        return getProperties( individual, tag ).stream().map( ind -> {
            switch( getType( ind ) )
            {
                case "SequenceInterval":
                    return parseSequenceInterval( ind );
                case "SequenceSite":
                    return parseSequenceSite( ind );
                case "SequenceLocation":
                    Concept cpt = new Concept( null, namePrefix + getName( ind ) );
                    cpt.setComment( getComments( ind ) );
                    return cpt;
                default:
                    return null;
            }
        } ).filter( Objects::nonNull ).toArray( Concept[]::new );
    }

    private EntityFeature[] getMemberFeatures(OWLIndividual individual)
    {
        return getPropertiesByTypes( individual, "memberFeature", "EntityFeature" ).map( ind -> parseEntityFeature( ind, "EntityFeature" ) )
                .toArray( EntityFeature[]::new );
    }

    private SequenceSite getSequenceSite(OWLIndividual individual, String ssType)
    {
        return getPropertiesByTypes( individual, ssType, "sequenceSite" ).findAny().map( this::parseSequenceSite ).orElse( null );
    }

    ////////////////////////////////////////////////////////////////////////////
    // draw diargam
    //

    protected void drawPathway(List<String> components, Diagram diagram)
    {
        for( String component : components )
        {
            if( component.startsWith( Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( conversions.getName() ) ) )
            {
                drawConversion( component, diagram );
            }
            else if( component
                    .startsWith( Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( controls.getName() ) ) )
            {
                drawControl( component, diagram );
            }
            else if( component.startsWith( Module.DIAGRAM ) )
            {
                Diagram d = (Diagram)getDataElementByName( component );
                DiagramInfo diagramInfo = (DiagramInfo)d.getKernel();
                String[] comp = (String[])diagramInfo.getAttributes().getValue( "Components" );
                drawPathway( Arrays.asList( comp ), diagram );
            }
        }
    }

    protected Node drawConversion(String fullName, Diagram diagram)
    {
        Node node = (Node)diagram.get( DataElementPath.create( fullName ).getName() );
        if( node != null )
            return node;
        Reaction conversion = (Reaction)getDataElementByName( fullName );
        String title = conversion.getTitle();
        if( title == null )
            title = conversion.getName();
        node = new Node( diagram, conversion );
        node.setTitle( title );

        String type = (String)conversion.getAttributes().getValue( "Type" );
        String sbgnType = "ComplexAssembly".equals( type ) ? "association" : "Degradation".equals( type ) ? "dissociation" : "process";
        node.getAttributes().add( new DynamicProperty( SBGNPropertyConstants.SBGN_REACTION_TYPE_PD, String.class, sbgnType ) );
        diagram.put( node );

        for( SpecieReference sr : conversion.getSpecieReferences() ) //TODO: use semantic controller
        {
            Edge edge = null;
            Node targetNode = null;
            String path = sr.getSpecie();

            DataElement kernel = getDataElementByName( path );

            if( kernel instanceof Complex )
                targetNode = drawComplexNode( (Complex)kernel, diagram );
            else if( kernel instanceof Base )
                targetNode = drawNode( (Base)kernel, diagram );
            if( targetNode != null )
            {
                if( sr.getRole().equals( SpecieReference.PRODUCT ) )
                {
                    edge = new Edge( diagram, sr, node, targetNode );
                }
                else
                {
                    edge = new Edge( diagram, sr, targetNode, node );
                }
                diagram.put( edge );
            }
        }
        return node;
    }

    protected DiagramElement drawControl(String fullName, Diagram diagram)
    {
        DiagramElement de = diagram.get( DataElementPath.create( fullName ).getName() );
        if( de != null )
            return de;
        SemanticRelation control = (SemanticRelation)getDataElementByName( fullName );

        String inputName = control.getInputElementName();
        Node inputNode = null;
        if( inputName != null )
        {
            if( inputName
                    .startsWith( Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( participants.getName() ) ) )
            {
                SpecieReference sr = (SpecieReference)getDataElementByName( inputName );
                DataElement kernel = getDataElementByName( sr.getSpecie() );
                if( kernel instanceof Complex )
                    inputNode = drawComplexNode( (Complex)kernel, diagram );
                else
                    inputNode = drawNode( (Base)kernel, diagram );
            }
            else if( inputName.startsWith( Module.DIAGRAM ) )
            {
                inputNode = drawSubDiagram( inputName, diagram );
            }
        }
        String outputName = control.getOutputElementName();
        Node outputNode = null;
        if( outputName != null )
        {
            if( outputName
                    .startsWith( Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( conversions.getName() ) ) )
            {
                outputNode = drawConversion( outputName, diagram );
            }
            else if( outputName
                    .startsWith( Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( controls.getName() ) ) )
            {
                //TODO: outputNode = drawControl(outputName, diagram);
            }
            else if( inputName.startsWith( Module.DIAGRAM ) )
            {
                outputNode = drawSubDiagram( outputName, diagram );
            }
        }
        if( inputNode != null && outputNode != null )
        {
            if( inputNode.getKernel() instanceof Specie && outputNode.getKernel() instanceof Reaction )
            {
                Reaction reaction = (Reaction)outputNode.getKernel();
                Specie specie = (Specie)inputNode.getKernel();
                SpecieReference reference = new SpecieReference( reaction, reaction.getName(), specie.getName(), SpecieReference.MODIFIER );
                reference.setSpecie( getRelativeToModuleName( specie ) );
                reaction.put( reference );
                Edge edge = new Edge( diagram, reference, inputNode, outputNode );
                String type = control.getAttributes().getValueAsString( "Type" );
                String modifierType = type.contains( "INHIBITION" ) ? Type.TYPE_INHIBITION : Type.TYPE_CATALYSIS;
                edge.getAttributes().add( new DynamicProperty( SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, modifierType ) );
                diagram.put( edge );
            }
            else
            {
                Edge edge = new Edge( diagram, control, inputNode, outputNode );
                diagram.put( edge );
                return edge;
            }
        }
        return null;
    }

    protected @Nonnull Node drawNode(Base base, Compartment compartment)
    {
        Compartment node = (Compartment)compartment.get( base.getName() );
        if( node != null )
            return node;

        String name = DiagramUtility.validateName( base.getName() );
        String entityType = base.getOrigin().getName().equals( smallMolecules.getName() ) ? "simple chemical" : "macromolecule";
        Specie newKernel = new Specie( null, name, entityType );

        base.getAttributes().forEach( attr -> newKernel.getAttributes().add( attr ) );

        if( base instanceof Referrer )
        {
            Referrer ref = (Referrer)base;
            if( ref.getDatabaseReferences() != null )
            {
                for( DatabaseReference reference : ref.getDatabaseReferences() )
                    newKernel.addDatabaseReferences( reference );
            }
            newKernel.setComment( ref.getComment() );
        }
        node = new Compartment( compartment, newKernel );

        drawFeature( node );

        VariableRole role = new VariableRole( node );
        Diagram.getDiagram( compartment ).getRole( EModel.class ).put( role );
        node.setRole( role );
        String title = base.getTitle();
        node.setTitle( title );
        node.setShapeSize( new Dimension( 0, 0 ) );
        compartment.put( node );
        return node;
    }

    /**
     *
     * TODO: parse notFeature somehow
     */
    private void drawFeature(Compartment compartment)
    {
        Base specie = compartment.getKernel();

        DynamicProperty dp = specie.getAttributes().getProperty( "FeatureList" );
        if( dp != null && dp.getValue() instanceof DataElementPath[] )
        {
            for( DataElementPath featurePath : (DataElementPath[])dp.getValue() )
            {
                EntityFeature feature = get( entityFeature, featurePath.getName() );
                String type = feature.getAttributes().getValueAsString( "Type" );
                if( "EntityFeature".equals(type) || "ModificationFeature".equals(type) )
                {
                    String id = feature.getName();
                    String comment = feature.getComment();
                    Node variableNode = new Node( compartment, new Stub( null, id, Type.TYPE_VARIABLE ) );
                    variableNode.getAttributes().add( new DynamicProperty( "Feature", DataElementPath.class, featurePath ) );
                    variableNode.setTitle( comment );
                    compartment.put( variableNode );
                }
            }
        }
    }

    protected @Nonnull Node drawComplexNode(Complex complex, Compartment diagram)
    {
        String complexName = complex.getName();
        Compartment complexNode = (Compartment)diagram.get( DataElementPath.create( complexName ).getName() );
        if( complexNode != null )
            return complexNode;

        Base base = new Specie( null, DiagramUtility.validateName( complexName ), "complex" );
        complexNode = new Compartment( diagram, base );
        complexNode.setTitle( complex.getTitle() );
        complexNode.setShapeSize( new Dimension( 0, 0 ) );
        diagram.put( complexNode );

        String[] components = (String[])complex.getAttributes().getValue( "Components" );
        if( components != null )
            for( String component : components )
            {
                DataElement de = getDataElementByName( component );
                Node compNode = null;
                if( de instanceof Complex )
                    compNode = drawComplexNode( (Complex)de, complexNode );
                else if( de instanceof Base )
                    compNode = drawNode( (Base)de, complexNode );
                if( compNode != null )
                    complexNode.put( compNode );
            }
        return complexNode;
    }

    protected Node drawSubDiagram(String relativeName, Diagram diagram)
    {
        Diagram subDiagram = DataElementPath.create( diagrams.getOrigin() ).getRelativePath( relativeName ).getDataElement( Diagram.class );
        DiagramInfo info = (DiagramInfo)subDiagram.getKernel();
        Diagram innerDiagram = new Diagram( null, info, diagram.getType() );
        try
        {
            return new SubDiagram( innerDiagram, diagram, diagram.getName() );
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    private static String[] CONTROL_TYPES = {"Control", "Catalysis", "Modulation", "TemplateReactionRegulation"};
    private static String[] CONVERSION_TYPES = {"Conversion", "BiochemicalReaction", "ComplexAssembly", "Degradation", "Transport",
            "TransportWithBiochemicalReaction"};
    private static String[] PHYSICAL_ENTITY_TYPES = {"PhysicalEntity", "Complex", "DNA", "DNARegion", "Protein", "RNA", "RNARegion",
            "SmallMolecule"};
    private static String[] ENTITY_REFERENCE_TYPES = {"EntityReference", "DnaReference", "ProteinReference", "RnaReference",
            "SmallMoleculeReference"};
    private static String[] VOCABULARY_TYPES = {"ControlledVocabulary", "CellularLocationVocabulary", "CellVocabulary",
            "EntityReferenceTypeVocabulary", "EvidenceCodeVocabulary", "ExperimentalFormVocabulary", "InteractionVocabulary",
            "PhenotypeVocabulary", "RelationshipTypeVocabulary", "SequenceModificationVocabulary", "SequenceRegionVocabulary",
            "TissueVocabulary"};
}
