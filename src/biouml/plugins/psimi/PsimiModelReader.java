package biouml.plugins.psimi;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

import one.util.streamex.StreamEx;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;
import biouml.plugins.psimi.access.IndexedDataCollection;
import biouml.plugins.psimi.model.Confidence;
import biouml.plugins.psimi.model.Experiment;
import biouml.plugins.psimi.model.Feature;
import biouml.plugins.psimi.model.Interaction;
import biouml.plugins.psimi.model.Interactor;
import biouml.plugins.psimi.model.Organism;
import biouml.plugins.psimi.model.Participant;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.Concept;
import biouml.standard.type.DatabaseReference;

import com.developmentontheedge.beans.DynamicProperty;

public class PsimiModelReader extends PsimiModelSupport
{
    protected static final Logger log = Logger.getLogger( PsimiModelReader.class.getName() );

    protected DataCollection parentDC;
    protected IndexedDataCollection<Concept> availabilitiesDC;
    protected IndexedDataCollection<Experiment> experimentsDC;
    protected IndexedDataCollection<Interactor> interactorsDC;
    protected IndexedDataCollection<Interaction> interactionsDC;
    protected IndexedDataCollection<Concept> sourcesDC;

    protected InputStream stream;
    protected String name;

    // //////////////////////////////////////////////////////////////////////////
    // Constructors and public methods
    //

    /**
     */
    public PsimiModelReader(String name, InputStream stream)
    {
        this.name = name;
        this.stream = stream;
    }

    public void read(DataCollection origin) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = null;

        StringBuffer buffer = new StringBuffer();
        byte[] b = new byte[65536];
        int length = stream.read( b );
        while( length > 0 )
        {
            buffer.append( new String( b, 0, length, StandardCharsets.UTF_8 ) );
            length = stream.read( b );
        }
        String xml = buffer.toString();
        xml = xml.replaceAll( "[\\x01\\x02\\x03\\x04\\x05\\x06\\x0b\\x0c\\x0f\\x12\\x14\\x16\\x92\\x1a\\x1c\\x1e\\xff]", "?" );
        stream = new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) );

        try
        {
            doc = builder.parse( stream );
        }
        catch( SAXException e )
        {
            log.log(Level.SEVERE,  "Parse \"" + name + "\" error: " + e.getMessage() );
            return;
        }
        catch( IOException e )
        {
            return;
        }

        read( origin, doc );
    }

    protected void read(DataCollection origin, Document doc) throws Exception
    {
        Element root = doc.getDocumentElement();

        if( root == null || !root.getNodeName().equals( ENTRY_SET ) )
        {
            error( "ERROR_ENTRYSET_ABSENT", new String[] {name} );
            return;
        }

        this.parentDC = origin;

        availabilitiesDC = (IndexedDataCollection<Concept>)parentDC.get( AVAILABILITIES );
        experimentsDC = (IndexedDataCollection<Experiment>)parentDC.get( EXPERIMENTS );
        interactorsDC = (IndexedDataCollection<Interactor>)parentDC.get( INTERACTORS );
        interactionsDC = (IndexedDataCollection<Interaction>)parentDC.get( INTERACTIONS );
        sourcesDC = (IndexedDataCollection<Concept>)parentDC.get( SOURCES );

        readEntrySet( root );
    }

    protected void readEntrySet(Element root)
    {
        for( Element child : XmlUtil.elements( root, ENTRY ) )
        {
            readEntry( child );
        }
    }

    protected void readEntry(Element root)
    {
        Element source = getElement( root, SOURCE );
        if( source != null )
        {
            readSource( source );
        }

        Element availabilityList = getElement( root, AVAILABILITY_LIST );
        if( availabilityList != null )
        {
            readAvailabilities( availabilityList );
        }

        Element experimentList = getElement( root, EXPERIMENT_LIST );
        if( experimentList != null )
        {
            readExperiments( experimentList );
        }

        Element interactorList = getElement( root, INTERACTOR_LIST );
        if( interactorList != null )
        {
            readInteractors( interactorList );
        }

        Element interactionList = getElement( root, INTERACTION_LIST );
        if( interactionList != null )
        {
            readInteractions( interactionList );
        }

        Element attributeList = getElement( root, ATTRIBUTE_LIST );
        if( attributeList != null )
        {
            BaseSupport attributes = new BaseSupport( parentDC, "Attributes", "" );
            readAttributeList( attributeList, attributes );
            try
            {
                parentDC.put( attributes );
            }
            catch( Throwable t )
            {
                error( "ERROR_ATTRIBUTES_ADD", new String[] {name} );
            }
        }
    }

    /*
     * process availability list
     */
    protected void readAvailabilities(Element root)
    {
        availabilitiesDC.setNotificationEnabled( false );
        for( Element child : XmlUtil.elements( root, AVAILABILITY ) )
        {
            readAvailability( child );
        }
        availabilitiesDC.setNotificationEnabled( true );
    }

    protected void readAvailability(Element root)
    {
        if( !root.hasAttribute( ID_ATTR ) )
        {
            return;
        }
        Concept availability = new Concept( availabilitiesDC, root.getAttribute( ID_ATTR ) + "(" + name + ")" );

        String description = getTextContent( root );
        if( description != null )
        {
            availability.setDescription( description.trim() );
        }

        try
        {
            availabilitiesDC.put( availability );
        }
        catch( Throwable t )
        {
            error( "ERROR_AVAILABILITY_ADD", new String[] {name, root.getAttribute( ID_ATTR )} );
        }
    }

    /*
     * process experiment list
     */
    protected void readExperiments(Element root)
    {
        experimentsDC.setNotificationEnabled( false );
        for( Element child : XmlUtil.elements( root, EXPERIMENT ) )
        {
            readExperiment( child );
        }
        experimentsDC.setNotificationEnabled( true );
    }

    protected void readExperiment(Element root)
    {
        if( !root.hasAttribute( ID_ATTR ) )
        {
            return;
        }
        Experiment experiment = new Experiment( experimentsDC, root.getAttribute( ID_ATTR ) + "(" + name + ")" );

        Element names = getElement( root, NAMES );
        if( names != null )
        {
            readNames( names, experiment );
        }
        Element bibref = getElement( root, BIBREF );
        if( bibref != null )
        {
            readBibRef( bibref, experiment );
        }
        Element xref = getElement( root, XREF );
        if( xref != null )
        {
            readXRef( xref, experiment );
        }
        Element attributes = getElement( root, ATTRIBUTE_LIST );
        if( attributes != null )
        {
            readAttributeList( attributes, experiment );
        }
        Element hoList = getElement( root, HOSTORGANISM_LIST );
        if( hoList != null )
        {
            experiment.setHostOrganismList( readOrganismList( hoList, experimentsDC ) );
        }
        Element idMethod = getElement( root, IDMETHOD );
        if( idMethod != null )
        {
            experiment.setInteractionDetectionMethod( readSimpleConcept( idMethod, experimentsDC ) );
        }
        Element piMethod = getElement( root, PIMETHOD );
        if( piMethod != null )
        {
            experiment.setParticipantIdentificationMethod( readSimpleConcept( piMethod, experimentsDC ) );
        }
        Element fdMethod = getElement( root, FDMETHOD );
        if( fdMethod != null )
        {
            experiment.setFeatureDetectionMethod( readSimpleConcept( fdMethod, experimentsDC ) );
        }
        Element coList = getElement( root, CONFIDENCE_LIST );
        if( coList != null )
        {
            experiment.setConfidenceList( readConfidenceList( coList, experimentsDC ) );
        }

        try
        {
            experimentsDC.put( experiment );
        }
        catch( Throwable t )
        {
            error( "ERROR_EXPERIMENT_ADD", new String[] {name, root.getAttribute( ID_ATTR )} );
        }
    }

    /*
     * process interactor list
     */
    protected void readInteractors(Element root)
    {
        interactorsDC.setNotificationEnabled( false );
        for( Element child : XmlUtil.elements( root, INTERACTOR ) )
        {
            readInteractor( child );
        }
        interactorsDC.setNotificationEnabled( true );
    }

    protected void readInteractor(Element root)
    {
        if( !root.hasAttribute( ID_ATTR ) )
        {
            return;
        }
        Interactor interactor = new Interactor( interactorsDC, root.getAttribute( ID_ATTR ) + "(" + name + ")" );

        Element names = getElement( root, NAMES );
        if( names != null )
        {
            readNames( names, interactor );
        }
        Element xref = getElement( root, XREF );
        if( xref != null )
        {
            readXRef( xref, interactor );
        }
        Element attributes = getElement( root, ATTRIBUTE_LIST );
        if( attributes != null )
        {
            readAttributeList( attributes, interactor );
        }
        Element interactorType = getElement( root, INTERACTORTYPE );
        if( interactorType != null )
        {
            interactor.setInteractorType( readInteractorType( interactorType, interactorsDC ) );
        }
        Element organism = getElement( root, ORGANISM );
        if( organism != null )
        {
            interactor.setOrganism( readOrganism( organism, interactorsDC ) );
        }
        Element sequence = getElement( root, SEQUENCE );
        if( sequence != null )
        {
            interactor.setSequence( getTextContent( sequence ) );
        }

        try
        {
            interactorsDC.put( interactor );
        }
        catch( Throwable t )
        {
            error( "ERROR_INTERACTOR_ADD", new String[] {name, root.getAttribute( ID_ATTR )} );
        }
    }

    protected Concept readInteractorType(Element root, DataCollection<?> origin)
    {
        String itName = "interactorType";
        // find first free index
        int ind = 0;
        while( origin.contains( itName + ind ) )
        {
            ind++;
        }
        Concept concept = new Concept( origin, itName + ind );

        Element names = getElement( root, NAMES );
        if( names != null )
        {
            readNames( names, concept );
        }
        Element xref = getElement( root, XREF );
        if( xref != null )
        {
            readXRef( xref, concept );
        }

        return concept;
    }

    /*
     * process interactions list
     */
    protected void readInteractions(Element root)
    {
        interactionsDC.setNotificationEnabled( false );
        for( Element child : XmlUtil.elements( root, INTERACTION ) )
        {
            readInteraction( child );
        }
        interactionsDC.setNotificationEnabled( true );
    }

    protected void readInteraction(Element root)
    {
        if( !root.hasAttribute( ID_ATTR ) )
        {
            return;
        }
        Interaction interaction = new Interaction( interactionsDC, root.getAttribute( ID_ATTR ) + "(" + name + ")" );

        Element names = getElement( root, NAMES );
        if( names != null )
        {
            readNames( names, interaction );
        }
        Element xref = getElement( root, XREF );
        if( xref != null )
        {
            readXRef( xref, interaction );
        }
        Element attributes = getElement( root, ATTRIBUTE_LIST );
        if( attributes != null )
        {
            readAttributeList( attributes, interaction );
        }
        Element aRef = getElement( root, AVAILABILITYREF );
        if( aRef != null )
        {
            String text = getTextContent( aRef );
            if( text != null )
            {
                interaction.setAvailabilityRef( Integer.parseInt( text ) );
            }
        }
        Element av = getElement( root, AVAILABILITY );
        if( av != null )
        {
            readAvailability( av );
            if( av.hasAttribute( ID_ATTR ) )
            {
                interaction.setAvailabilityRef( Integer.parseInt( av.getAttribute( ID_ATTR ).trim() ) );
            }
        }
        Element expList = getElement( root, EXPERIMENT_LIST );
        if( expList != null )
        {
            interaction.setExperimentList( readExperimentList( expList ) );
        }
        Concept[] intTypes = XmlStream.elements( root, INTTYPE ).map( intType -> readInteractionType( intType, interactionsDC ) )
                .toArray( Concept[]::new );
        interaction.setInteractionType( orNull(intTypes) );
        Element modelled = getElement( root, MODELLED );
        if( modelled != null )
        {
            String text = getTextContent( modelled );
            if( text != null )
            {
                interaction.setModelled( Boolean.parseBoolean( text ) );
            }
        }
        Element intralMolecular = getElement( root, INTRALMOLECULAR );
        if( intralMolecular != null )
        {
            String text = getTextContent( intralMolecular );
            if( text != null )
            {
                interaction.setIntraMolecular( Boolean.parseBoolean( text ) );
            }
        }
        Element negative = getElement( root, NEGATIVE );
        if( negative != null )
        {
            String text = getTextContent( negative );
            if( text != null )
            {
                interaction.setNegative( Boolean.parseBoolean( text ) );
            }
        }
        Element coList = getElement( root, CONFIDENCE_LIST );
        if( coList != null )
        {
            interaction.setConfidenceList( readConfidenceList( coList, interactionsDC ) );
        }
        Element paList = getElement( root, PARTICIPANT_LIST );
        if( paList != null )
        {
            interaction.setParticipantList( readParticipantList( paList, interactionsDC ) );
        }
        Element parameterList = getElement( root, PARAMETER_LIST );
        if( parameterList != null )
        {
            interaction.setParameterList( readParameterList( parameterList, interactionsDC ) );
        }
        Element iiList = getElement( root, II_LIST );
        if( iiList != null )
        {
            interaction.setInferredInteractionList( readInferredInteractorList( iiList, interactionsDC ) );
        }

        try
        {
            interactionsDC.put( interaction );
        }
        catch( Throwable t )
        {
            error( "ERROR_INTERACTION_ADD", new String[] {name, root.getAttribute( ID_ATTR )} );
        }
    }

    protected Concept[] readInferredInteractorList(Element root, DataCollection<?> origin)
    {
        Concept[] concepts = XmlStream.elements( root, II ).map( child -> readInferredInteractor( child, origin ) )
                .toArray( Concept[]::new );
        return orNull( concepts );
    }

    protected Concept readInferredInteractor(Element root, DataCollection<?> origin)
    {
        String parameterName = "inferredInteractor";
        // find first free index
        int ind = 0;
        while( origin.contains( parameterName + ind ) )
        {
            ind++;
        }
        Concept parameter = new Concept( origin, parameterName + ind );

        try
        {
            Integer[] pList = XmlStream.elements( root, PARTICIPANT )
                .cross( PARTICIPANTREF, PARTICIPANTFEATUREREF )
                .mapKeyValue( PsimiModelReader::getElement )
                .nonNull().map( element -> Integer.parseInt( getTextContent( element ) ) )
                .toArray( Integer[]::new );
            if( pList.length > 0 )
            {
                parameter.getAttributes().add( new DynamicProperty( PARTICIPANT_LIST, Integer[].class, pList ) );
            }
            Element eRef = getElement( root, EXPREF_LIST );
            if( eRef != null )
            {
                DynamicProperty ss = new DynamicProperty( EXPREF_LIST, Concept[].class, readExpRefList( eRef ) );
                parameter.getAttributes().add( ss );
            }
        }
        catch( Throwable t )
        {
            error( "ERROR_PARAMETER", new String[] {name, parameterName + ind} );
        }

        return parameter;
    }

    protected Integer[] readExperimentList(Element root)
    {
        List<Integer> eList = new ArrayList<>();
        for( Element child : XmlUtil.elements( root ) )
        {
            if( child.getNodeName().equals( EXPERIMENTREF ) )
            {
                eList.add( Integer.parseInt( getTextContent( child ) ) );
            }
            else if( child.getNodeName().equals( EXPERIMENT ) )
            {
                readExperiment( child );
                if( child.hasAttribute( ID_ATTR ) )
                {
                    eList.add( Integer.parseInt( child.getAttribute( ID_ATTR ).trim() ) );
                }
            }
        }

        if( eList.size() > 0 )
        {
            return eList.toArray( new Integer[eList.size()] );
        }
        return null;
    }

    protected Concept readInteractionType(Element root, DataCollection<?> origin)
    {
        String itName = "interactionType";
        // find first free index
        int ind = 0;
        while( origin.contains( itName + ind ) )
        {
            ind++;
        }
        Concept concept = new Concept( origin, itName + ind );

        Element names = getElement( root, NAMES );
        if( names != null )
        {
            readNames( names, concept );
        }
        Element xref = getElement( root, XREF );
        if( xref != null )
        {
            readXRef( xref, concept );
        }

        return concept;
    }

    protected Participant[] readParticipantList(Element root, DataCollection<?> origin)
    {
        Participant[] participants = XmlStream.elements( root, PARTICIPANT )
                .map( child -> readParticipant( child, origin ) )
                .toArray( Participant[]::new );
        return orNull( participants );
    }

    protected Participant readParticipant(Element root, DataCollection<?> origin)
    {
        if( !root.hasAttribute( ID_ATTR ) )
        {
            return null;
        }
        Participant participant = new Participant( origin, root.getAttribute( ID_ATTR ) );

        Element names = getElement( root, NAMES );
        if( names != null )
        {
            readNames( names, participant );
        }
        Element xref = getElement( root, XREF );
        if( xref != null )
        {
            readXRef( xref, participant );
        }
        Element attributes = getElement( root, ATTRIBUTE_LIST );
        if( attributes != null )
        {
            readAttributeList( attributes, participant );
        }
        Element interactorRef = getElement( root, INTERACTORREF );
        if( interactorRef != null )
        {
            participant.setInteractorRef( Integer.parseInt( getTextContent( interactorRef ) ) );
        }
        Element interactor = getElement( root, INTERACTOR );
        if( interactor != null )
        {
            readInteractor( interactor );
            if( interactor.hasAttribute( ID_ATTR ) )
            {
                participant.setInteractorRef( Integer.parseInt( interactor.getAttribute( ID_ATTR ).trim() ) );
            }
        }
        Element interactionRef = getElement( root, INTERACTIONREF );
        if( interactionRef != null )
        {
            participant.setInteractionRef( Integer.parseInt( getTextContent( interactionRef ) ) );
        }
        Element biologicalRole = getElement( root, BIOLOGICALROLE );
        if( biologicalRole != null )
        {
            participant.setBiologicalRole( readSimpleConcept( biologicalRole, interactionsDC ) );
        }
        Element experimentalRoleList = getElement( root, EXPERIMENTALROLE_LIST );
        if( experimentalRoleList != null )
        {
            participant.setExperimentalRoleList( readRoleList( experimentalRoleList, interactionsDC, EXPERIMENTALROLE ) );
        }
        Element experimentalPreparationList = getElement( root, EXPERIMENTALPREPARATION_LIST );
        if( experimentalPreparationList != null )
        {
            participant
                    .setExperimentalPreparationList( readRoleList( experimentalPreparationList, interactionsDC, EXPERIMENTALPREPARATION ) );
        }
        Element hostOrganismList = getElement( root, HOSTORGANISM_LIST );
        if( hostOrganismList != null )
        {
            participant.setHostOrganismList( readOrganismList( hostOrganismList, interactionsDC ) );
        }
        Element confidenceList = getElement( root, CONFIDENCE_LIST );
        if( confidenceList != null )
        {
            participant.setConfidenceList( readConfidenceList( confidenceList, interactionsDC ) );
        }
        Element featureList = getElement( root, FEATURE_LIST );
        if( featureList != null )
        {
            participant.setFeatureList( readFeatureList( featureList, interactionsDC ) );
        }
        Element parameterList = getElement( root, PARAMETER_LIST );
        if( parameterList != null )
        {
            participant.setParameterList( readParameterList( parameterList, interactionsDC ) );
        }
        Element participantIdentificationMethodList = getElement( root, PIMETHOD_LIST );
        if( participantIdentificationMethodList != null )
        {
            participant.setParticipantIdentificationMethodList( readMethodList( participantIdentificationMethodList, interactionsDC ) );
        }
        Element experimentalInteractorList = getElement( root, EI_LIST );
        if( experimentalInteractorList != null )
        {
            participant.setExperimentalInteractorList( readExperimentalInteractorList( experimentalInteractorList, interactionsDC ) );
        }

        return participant;
    }

    protected Concept[] readExperimentalInteractorList(Element root, DataCollection<?> origin)
    {
        Concept[] intList = XmlStream.elements( root, EI )
                .map( child -> readExperimentalInteractor( child, origin ) )
                .toArray( Concept[]::new );
        return orNull( intList );
    }

    protected Concept readExperimentalInteractor(Element root, DataCollection<?> origin)
    {
        String parameterName = "experimentalInteractor";
        // find first free index
        int ind = 0;
        while( origin.contains( parameterName + ind ) )
        {
            ind++;
        }
        Concept parameter = new Concept( origin, parameterName + ind );

        try
        {
            Element iRef = getElement( root, INTERACTORREF );
            if( iRef != null )
            {
                DynamicProperty ss = new DynamicProperty( INTERACTORREF, Integer.class, Integer.parseInt( getTextContent( iRef ) ) );
                parameter.getAttributes().add( ss );
            }
            Element interactor = getElement( root, INTERACTOR );
            if( interactor != null )
            {
                readInteractor( interactor );
                if( interactor.hasAttribute( ID_ATTR ) )
                {
                    DynamicProperty ss = new DynamicProperty( INTERACTORREF, Integer.class, Integer.parseInt( interactor.getAttribute(
                            ID_ATTR ).trim() ) );
                    parameter.getAttributes().add( ss );
                }
            }
            Element eRef = getElement( root, EXPREF_LIST );
            if( eRef != null )
            {
                DynamicProperty ss = new DynamicProperty( EXPREF_LIST, Concept[].class, readExpRefList( eRef ) );
                parameter.getAttributes().add( ss );
            }
        }
        catch( Throwable t )
        {
            error( "ERROR_PARAMETER", new String[] {name, parameterName + ind} );
        }

        return parameter;
    }

    protected Concept[] readMethodList(Element root, DataCollection<?> origin)
    {
        Concept[] methods = XmlStream.elements( root, PIMETHOD ).map( child -> readMethod( child, origin, EXREF_LIST, EXREF ) )
            .toArray( Concept[]::new );
        return orNull( methods );
    }

    protected Feature[] readFeatureList(Element root, DataCollection<?> origin)
    {
        Feature[] features = XmlStream.elements( root, FEATURE ).map( child -> readFeature( child, origin ) )
                .toArray( Feature[]::new );
        return orNull( features );
    }

    protected Feature readFeature(Element root, DataCollection<?> origin)
    {
        if( !root.hasAttribute( ID_ATTR ) )
        {
            return null;
        }
        Feature feature = new Feature( origin, root.getAttribute( ID_ATTR ) );

        Element names = getElement( root, NAMES );
        if( names != null )
        {
            readNames( names, feature );
        }
        Element xref = getElement( root, XREF );
        if( xref != null )
        {
            readXRef( xref, feature );
        }
        Element attributes = getElement( root, ATTRIBUTE_LIST );
        if( attributes != null )
        {
            readAttributeList( attributes, feature );
        }
        Element featureType = getElement( root, FEATURETYPE );
        if( featureType != null )
        {
            feature.setFeatureType( readSimpleConcept( featureType, interactionsDC ) );
        }
        Element featureDetectionMethod = getElement( root, FEATUREDETECTIONMETHOD );
        if( featureDetectionMethod != null )
        {
            feature.setFeatureDetectionMethod( readSimpleConcept( featureDetectionMethod, interactionsDC ) );
        }
        Element experimentRefList = getElement( root, EXPREF_LIST );
        if( experimentRefList != null )
        {
            feature.setExperimentRefList( readExpRefList( featureDetectionMethod ) );
        }
        Element featureRangeList = getElement( root, FEATURERANGE_LIST );
        if( featureRangeList != null )
        {
            feature.setFeatureRangeList( readFeatureRangeList( featureRangeList, interactionsDC ) );
        }

        return feature;
    }

    protected Concept[] readFeatureRangeList(Element root, DataCollection<?> origin)
    {
        Concept[] concepts = XmlStream.elements( root, FEATURERANGE ).map( child -> readFeatureRange( child, origin ) )
                .toArray( Concept[]::new );
        return orNull( concepts );
    }

    protected Concept readFeatureRange(Element root, DataCollection<?> origin)
    {
        String confidenceName = "reatureRange";
        // find first free index
        int ind = 0;
        while( origin.contains( confidenceName + ind ) )
        {
            ind++;
        }
        Concept featureRange = new Concept( origin, confidenceName + ind );

        try
        {
            Element startStatus = getElement( root, STARTSTATUS );
            if( startStatus != null )
            {
                DynamicProperty ss = new DynamicProperty( STARTSTATUS, Concept.class, readSimpleConcept( startStatus, origin ) );
                featureRange.getAttributes().add( ss );
            }
            Element endStatus = getElement( root, ENDSTATUS );
            if( endStatus != null )
            {
                DynamicProperty es = new DynamicProperty( ENDSTATUS, Concept.class, readSimpleConcept( endStatus, origin ) );
                featureRange.getAttributes().add( es );
            }
            Element begin = getElement( root, BEGIN );
            if( begin != null )
            {
                if( begin.hasAttribute( POSITION ) )
                {
                    DynamicProperty es = new DynamicProperty( BEGIN, Integer.class,
                            Integer.parseInt( begin.getAttribute( POSITION ).trim() ) );
                    featureRange.getAttributes().add( es );
                }
            }
            Element end = getElement( root, END );
            if( end != null )
            {
                if( end.hasAttribute( POSITION ) )
                {
                    DynamicProperty es = new DynamicProperty( END, Integer.class, Integer.parseInt( end.getAttribute( POSITION ).trim() ) );
                    featureRange.getAttributes().add( es );
                }
            }
            Element beginInterval = getElement( root, BEGININTERVAL );
            if( beginInterval != null )
            {
                if( beginInterval.hasAttribute( BEGIN ) && beginInterval.hasAttribute( END ) )
                {
                    Point p = new Point( Integer.parseInt( beginInterval.getAttribute( BEGIN ).trim() ), Integer.parseInt( beginInterval
                            .getAttribute( END ).trim() ) );
                    DynamicProperty es = new DynamicProperty( BEGININTERVAL, Point.class, p );
                    featureRange.getAttributes().add( es );
                }
            }
            Element endInterval = getElement( root, ENDINTERVAL );
            if( endInterval != null )
            {
                if( endInterval.hasAttribute( BEGIN ) && endInterval.hasAttribute( END ) )
                {
                    Point p = new Point( Integer.parseInt( endInterval.getAttribute( BEGIN ).trim() ), Integer.parseInt( endInterval
                            .getAttribute( END ).trim() ) );
                    DynamicProperty es = new DynamicProperty( ENDINTERVAL, Point.class, p );
                    featureRange.getAttributes().add( es );
                }
            }
            Element isLink = getElement( root, ISLINK );
            if( isLink != null )
            {
                DynamicProperty es = new DynamicProperty( ISLINK, Boolean.class, Boolean.valueOf( getTextContent( isLink ) ) );
                featureRange.getAttributes().add( es );
            }
        }
        catch( Throwable t )
        {
            error( "ERROR_FEATURE_RANGE", new String[] {name, confidenceName + ind} );
        }

        return featureRange;
    }

    /*
     * common functions
     */
    protected void readNames(Element root, Concept concept)
    {
        StringBuilder aliasesSB = null;
        for( Element child : XmlUtil.elements( root ) )
        {
            if( child.getNodeName().equals( ALIAS ) )
            {
                if( aliasesSB == null )
                    aliasesSB = new StringBuilder( getTextContent( child ) );
                else
                    aliasesSB.append( ", " ).append( getTextContent( child ) );
            }
            else if( child.getNodeName().equals( SHORT_LABEL ) )
            {
                concept.setTitle( getTextContent( child ) );
            }
            else if( child.getNodeName().equals( FULL_NAME ) )
            {
                concept.setCompleteName( getTextContent( child ) );
            }
        }
        if( aliasesSB != null )
            concept.setSynonyms( aliasesSB.toString() );
    }

    protected void readBibRef(Element root, Concept concept)
    {
        for( Element child : XmlUtil.elements( root ) )
        {
            if( child.getNodeName().equals( XREF ) )
            {
                readXRef( child, concept );
            }
            else if( child.getNodeName().equals( ATTRIBUTE_LIST ) )
            {
                readAttributeList( child, concept );
            }
        }
    }

    protected void readXRef(Element root, Concept concept)
    {
        List<DatabaseReference> dbRefs = new ArrayList<>();
        for( Element child : XmlUtil.elements( root ) )
        {
            if( child.getNodeName().equals( PRIMARY_REF ) || child.getNodeName().equals( SECONDARY_REF ) )
            {
                Element ref = child;
                DatabaseReference dbRef = new DatabaseReference();
                if( ref.hasAttribute( DB_ATTR ) && ref.hasAttribute( ID_ATTR ) )
                {
                    dbRef.setDatabaseName( ref.getAttribute( DB_ATTR ) );
                    dbRef.setId( ref.getAttribute( ID_ATTR ) );
                    if( ref.hasAttribute( AC_ATTR ) )
                    {
                        dbRef.setAc( ref.getAttribute( AC_ATTR ) );
                    }
                    if( ref.hasAttribute( REFTYPE_ATTR ) )
                    {
                        dbRef.setRelationshipType( ref.getAttribute( REFTYPE_ATTR ) );
                    }
                    dbRefs.add( dbRef );
                }
                else
                {
                    error( "ERROR_XREF_REQUIRED_ATTRIBUTES_ABSENT", new String[] {name} );
                    continue;
                }
            }
        }
        if( dbRefs.size() > 0 )
        {
            DatabaseReference[] oldReference = concept.getDatabaseReferences();
            if( oldReference == null || oldReference.length == 0 )
            {
                concept.setDatabaseReferences( dbRefs.toArray( new DatabaseReference[dbRefs.size()] ) );
            }
            else
            {
                //merge reference collections if oldReference is not empty
                concept.setDatabaseReferences( StreamEx.of(oldReference).append( dbRefs ).toArray( DatabaseReference[]::new ) );
            }
        }
    }

    protected void readAttributeList(Element root, BaseSupport dataElement)
    {
        for( Element attribute : XmlUtil.elements( root, ATTRIBUTE ) )
        {
            if( attribute.hasAttribute( NAME_ATTR ) )
            {
                try
                {
                    DynamicProperty dp = new DynamicProperty( attribute.getAttribute( NAME_ATTR ), String.class, getTextContent( attribute ) );
                    dataElement.getAttributes().add( dp );
                }
                catch( Throwable t )
                {
                    error( "ERROR_ATTRIBUTE_ADD", new String[] {name} );
                }
            }
            else
            {
                error( "ERROR_ATTRIBUTE_NAME_ABSENT", new String[] {name} );
                continue;
            }
        }
    }

    protected Concept readSimpleConcept(Element root, DataCollection<?> origin)
    {
        String methodName = "element";
        // find first free index
        int ind = 0;
        while( origin.contains( methodName + ind ) )
        {
            ind++;
        }
        Concept concept = new Concept( origin, methodName + ind );

        Element names = getElement( root, NAMES );
        if( names != null )
        {
            readNames( names, concept );
        }
        Element xref = getElement( root, XREF );
        if( xref != null )
        {
            readXRef( xref, concept );
        }

        return concept;
    }

    protected Concept readMethod(Element root, DataCollection<?> origin, String refListTagName, String refTagName)
    {
        Concept concept = readSimpleConcept( root, origin );

        Element refList = getElement( root, refListTagName );
        if( refList != null )
        {
            Integer[] resultList = XmlStream.elements( root, refTagName ).filter( child -> child.hasAttribute( ID_ATTR ) )
                .map( child -> Integer.parseInt( child.getAttribute( ID_ATTR ).trim() ) )
                .toArray( Integer[]::new );
            if( resultList.length > 0 )
            {
                concept.getAttributes().add( new DynamicProperty( refListTagName, Integer[].class, resultList ) );
            }
        }

        return concept;
    }
    protected Concept readDescription(Element root, DataCollection<?> origin)
    {
        String methodName = "description";
        // find first free index
        int ind = 0;
        while( origin.contains( methodName + ind ) )
        {
            ind++;
        }
        Concept concept = new Concept( origin, methodName + ind );

        Element names = getElement( root, NAMES );
        if( names != null )
        {
            readNames( names, concept );
        }
        Element xref = getElement( root, XREF );
        if( xref != null )
        {
            readXRef( xref, concept );
        }
        Element attributes = getElement( root, ATTRIBUTE_LIST );
        if( attributes != null )
        {
            readAttributeList( attributes, concept );
        }

        return concept;
    }

    protected Organism readOrganism(Element root, DataCollection<?> origin)
    {
        if( !root.hasAttribute( HOST_ORGANISM_ATTR ) )
        {
            return null;
        }
        Organism hostOrganism = new Organism( origin, root.getAttribute( HOST_ORGANISM_ATTR ) );

        Element names = getElement( root, NAMES );
        if( names != null )
        {
            readNames( names, hostOrganism );
        }

        Element cellType = getElement( root, CELLTYPE );
        if( cellType != null )
        {
            hostOrganism.setCelltype( readDescription( cellType, experimentsDC ) );
        }
        Element compartment = getElement( root, COMPARTMENT );
        if( compartment != null )
        {
            hostOrganism.setCompartment( readDescription( compartment, experimentsDC ) );
        }
        Element tissue = getElement( root, TISSUE );
        if( tissue != null )
        {
            hostOrganism.setTissue( readDescription( tissue, experimentsDC ) );
        }

        return hostOrganism;
    }

    protected Confidence[] readConfidenceList(Element root, DataCollection<?> origin)
    {
        Confidence[] confidences = XmlStream.elements( root, CONFIDENCE )
                .map( child -> readConfidence( child, origin ) )
                .toArray( Confidence[]::new );
        return orNull( confidences );
    }

    protected Confidence readConfidence(Element root, DataCollection<?> origin)
    {
        String confidenceName = "confidence";
        // find first free index
        int ind = 0;
        while( origin.contains( confidenceName + ind ) )
        {
            ind++;
        }
        Confidence confidence = new Confidence( origin, confidenceName + ind );

        Element unit = getElement( root, UNIT );
        if( unit != null )
        {
            confidence.setUnit( readDescription( unit, experimentsDC ) );
        }
        Element value = getElement( root, VALUE );
        if( value != null )
        {
            confidence.setValue( getTextContent( value ) );
        }
        Element expRefList = getElement( root, EXPREF_LIST );
        if( expRefList != null )
        {
            confidence.setExperimentRefList( readExpRefList( expRefList ) );
        }

        return confidence;
    }

    protected void readSource(Element root)
    {
        if( sourcesDC.containsInCache( name ) )
        {
            return;
        }
        Concept concept = new Concept( sourcesDC, name );

        Element names = getElement( root, NAMES );
        if( names != null )
        {
            readNames( names, concept );
        }
        Element bibRef = getElement( root, BIBREF );
        if( bibRef != null )
        {
            readBibRef( bibRef, concept );
        }
        Element xref = getElement( root, XREF );
        if( xref != null )
        {
            readXRef( xref, concept );
        }
        Element attributes = getElement( root, ATTRIBUTE_LIST );
        if( attributes != null )
        {
            readAttributeList( attributes, concept );
        }

        try
        {
            sourcesDC.put( concept );
        }
        catch( Throwable t )
        {
            error( "ERROR_SOURCE_ADD", new String[] {name} );
        }
    }

    protected static <T> T[] orNull(T[] array)
    {
        return array.length == 0 ? null : array;
    }

    protected Concept[] readRoleList(Element root, DataCollection<?> origin, String tagName)
    {
        Concept[] concepts = XmlStream.elements( root, tagName ).map( child -> readSimpleConcept( child, origin ) )
                .toArray( Concept[]::new );
        return orNull( concepts );
    }

    protected Organism[] readOrganismList(Element root, DataCollection<?> origin)
    {
        Organism[] organisms = XmlStream.elements( root, HOSTORGANISM ).map( child -> readOrganism( child, origin ) )
                .toArray( Organism[]::new );
        return orNull( organisms );
    }

    protected Integer[] readExpRefList(Element root)
    {
        Integer[] expRefs = XmlStream.elements( root, EXPREF ).map( this::getTextContent ).map( Integer::parseInt )
                .toArray( Integer[]::new );
        return orNull( expRefs );
    }

    protected Concept[] readParameterList(Element root, DataCollection<?> origin)
    {
        Concept[] concepts = XmlStream.elements( root, PARAMETER ).map( child -> readParameter( child, origin ) ).toArray( Concept[]::new );
        return orNull( concepts );
    }

    protected Concept readParameter(Element root, DataCollection<?> origin)
    {
        String parameterName = "parameter";
        // find first free index
        int ind = 0;
        while( origin.contains( parameterName + ind ) )
        {
            ind++;
        }
        Concept parameter = new Concept( origin, parameterName + ind );

        try
        {
            Element eRef = getElement( root, EXPERIMENTREF );
            if( eRef != null )
            {
                DynamicProperty ss = new DynamicProperty( EXPERIMENTREF, Integer.class, Integer.parseInt( getTextContent( eRef ) ) );
                parameter.getAttributes().add( ss );
            }
            if( root.hasAttribute( TERM ) )
            {
                DynamicProperty dp = new DynamicProperty( TERM, String.class, root.getAttribute( TERM ) );
                parameter.getAttributes().add( dp );
            }
            if( root.hasAttribute( TERMAC ) )
            {
                DynamicProperty dp = new DynamicProperty( TERMAC, String.class, root.getAttribute( TERMAC ) );
                parameter.getAttributes().add( dp );
            }
            if( root.hasAttribute( UNIT ) )
            {
                DynamicProperty dp = new DynamicProperty( UNIT, String.class, root.getAttribute( UNIT ) );
                parameter.getAttributes().add( dp );
            }
            if( root.hasAttribute( UNITAC ) )
            {
                DynamicProperty dp = new DynamicProperty( UNITAC, String.class, root.getAttribute( UNITAC ) );
                parameter.getAttributes().add( dp );
            }
            if( root.hasAttribute( BASE ) )
            {
                DynamicProperty dp = new DynamicProperty( BASE, Integer.class, Integer.parseInt( root.getAttribute( BASE ).trim() ) );
                parameter.getAttributes().add( dp );
            }
            if( root.hasAttribute( EXPONENT ) )
            {
                DynamicProperty dp = new DynamicProperty( EXPONENT, Integer.class, Integer.parseInt( root.getAttribute( EXPONENT ).trim() ) );
                parameter.getAttributes().add( dp );
            }
            if( root.hasAttribute( FACTOR ) )
            {
                DynamicProperty dp = new DynamicProperty( FACTOR, String.class, root.getAttribute( FACTOR ) );
                parameter.getAttributes().add( dp );
            }
            if( root.hasAttribute( UNCERTAINTY ) )
            {
                DynamicProperty dp = new DynamicProperty( UNCERTAINTY, String.class, root.getAttribute( UNCERTAINTY ) );
                parameter.getAttributes().add( dp );
            }
        }
        catch( Throwable t )
        {
            error( "ERROR_PARAMETER", new String[] {name, parameterName + ind} );
        }

        return parameter;
    }

    protected String getTextContent(Element root)
    {
        for( org.w3c.dom.Node child : XmlUtil.nodes( root ) )
        {
            if( child instanceof Text )
            {
                return ( (Text)child ).getData();
            }
        }
        return null;
    }
}
