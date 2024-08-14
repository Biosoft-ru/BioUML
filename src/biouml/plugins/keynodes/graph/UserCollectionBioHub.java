package biouml.plugins.keynodes.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.plugins.keynodes.graph.CollectionBioHub.HubElement;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.ExProperties;

public class UserCollectionBioHub extends KeyNodesHub<CollectionBioHub.HubElement>
{
    private static final ReferenceType DEFAULT_REFERENCE_TYPE = ReferenceTypeRegistry.getDefaultReferenceType();
    private CollectionBioHub innerHub;
    private DataElementPath customCollectionPath;
    boolean hasDiagramType = false;
    public static final String CUSTOM_REPOSITORY_HUB_NAME = "Custom repository hub";

    //to initialize from BioHubRegistry
    public UserCollectionBioHub(Properties properties)
    {
        super( properties );
    }
    public UserCollectionBioHub(Properties properties, DataElementPath collectionPath)
    {
        super( properties );
        if( collectionPath != null )
        {
            customCollectionPath = collectionPath;
            try
            {
                //TODO: rework
                DataCollection<?> dc = collectionPath.optDataCollection();
                Properties props;
                if( dc == null )
                {
                    props = new Properties( properties );
                }
                else
                {
                    Properties dcProperties = dc.getInfo().getProperties();
                    Map<String, Map<String, String>> subProperties = ExProperties.getSubProperties( dcProperties, "bioHub" );
                    Map<String, String> newStyleProps = subProperties.getOrDefault( "search", new HashMap<>() );
                    props = new Properties( dcProperties );
                    if( newStyleProps.get( "diagramType" ) != null )
                        hasDiagramType = true;
                    props.putAll( properties );
                    props.putAll( newStyleProps );
                }
                props.setProperty( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY, collectionPath.toString() );
                props.setProperty( "moduleName", collectionPath.toString() );
                innerHub = new CollectionBioHub( props );
            }
            catch( Exception e )
            {
                //do nothing
            }
        }
    }

    public DataElementPath getCustomCollectionPath()
    {
        return customCollectionPath;
    }

    //TODO: remove unnecessary methods
    //TODO: think how to process case of null innerHub
    @Override
    protected HubGraph<HubElement> getHub(TargetOptions dbOptions, String[] relationTypes)
    {
        return innerHub.getHub( dbOptions, relationTypes );
    }

    @Override
    protected ElementConverter<HubElement> getElementConverter()
    {
        return innerHub.getElementConverter();
    }

    @Override
    public String getElementTitle(Element element)
    {
        return innerHub.getElementTitle( element );
    }

    @Override
    public DataElementPath getCompleteElementPath(String acc)
    {
        return innerHub.getCompleteElementPath( acc );
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        if( innerHub == null )
            return super.getPriority( dbOptions );
        return innerHub.getPriority( dbOptions );
    }

    @Override
    public ReferenceType[] getSupportedInputTypes()
    {
        if( innerHub == null )
            return super.getSupportedInputTypes();
        return innerHub.getSupportedInputTypes();
    }

    @Override
    public DiagramType getVisualizationDiagramType()
    {
        if( innerHub == null )
            return super.getVisualizationDiagramType();
        if( hasDiagramType )
            return innerHub.getVisualizationDiagramType();
        return new PathwaySimulationDiagramType();
    }

    @Override
    public Element[] getRandomSample(int sampleSize, TargetOptions dbOptions, String[] relationTypes, IntUnaryOperator random)
    {
        if( innerHub == null )
            return super.getRandomSample( sampleSize, dbOptions, relationTypes, random );
        return innerHub.getRandomSample( sampleSize, dbOptions, relationTypes, random );
    }

    @Override
    public DataElementPath getCompleteElementPath(SpecieReference sr)
    {
        if( innerHub == null )
            return super.getCompleteElementPath( sr );
        return innerHub.getCompleteElementPath( sr );
    }

    @Override
    public List<Element> getReactionComponents(String reactionAcc, Species species, @Nonnull Map<String, Set<String>> preferredComponents)
    {
        if( innerHub == null )
            return super.getReactionComponents( reactionAcc, species, preferredComponents );
        return innerHub.getReactionComponents( reactionAcc, species, preferredComponents );
    }

    @Override
    public List<Element[]> getMinimalPaths(Element key, Element[] targets, TargetOptions dbOptions, String[] relationTypes, int maxLength,
            int direction)
    {
        if( innerHub == null )
            return super.getMinimalPaths( key, targets, dbOptions, relationTypes, maxLength, direction );
        return innerHub.getMinimalPaths( key, targets, dbOptions, relationTypes, maxLength, direction );
    }

    @Override
    public List<Element> getAllReactions(Element key, Element[] targets, TargetOptions dbOptions, String[] relationTypes, int maxLength,
            int direction)
    {
        if( innerHub == null )
            return super.getAllReactions( key, targets, dbOptions, relationTypes, maxLength, direction );
        return innerHub.getAllReactions( key, targets, dbOptions, relationTypes, maxLength, direction );
    }

    @Override
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        if( innerHub == null )
            return super.getReference( startElement, dbOptions, relationTypes, maxLength, direction );
        return innerHub.getReference( startElement, dbOptions, relationTypes, maxLength, direction );
    }

    @Override
    public Element[] getMinimalPath(Element key, Element target, TargetOptions dbOptions, String[] relationTypes, int maxLength,
            int direction)
    {
        if( innerHub == null )
            return super.getMinimalPath( key, target, dbOptions, relationTypes, maxLength, direction );
        return innerHub.getMinimalPath( key, target, dbOptions, relationTypes, maxLength, direction );
    }

    @Override
    public String getParentIsoform(String basicForm)
    {
        if( innerHub == null )
            return super.getParentIsoform( basicForm );
        return innerHub.getParentIsoform( basicForm );
    }

    @Override
    public Diagram convert(Diagram diagram)
    {
        if( innerHub == null )
            return super.convert( diagram );
        if( hasDiagramType )
            return innerHub.convert( diagram );
        UserCollectionSBGNConverter converter = new UserCollectionSBGNConverter();
        try
        {
            diagram = converter.convert( diagram, null );
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( e );
        }
        return super.convert( diagram );
    }

    @Override
    public String getElementTypeName()
    {
        if( innerHub == null )
            return super.getElementTypeName();
        return innerHub.getElementTypeName();
    }

    @Override
    public Set<String> getNames(TargetOptions dbOptions, String[] relationTypes)
    {
        if( innerHub == null )
            return super.getNames( dbOptions, relationTypes );
        return innerHub.getNames( dbOptions, relationTypes );
    }

    @Override
    public ReferenceType[] getSupportedMatching(ReferenceType inputType)
    {
        ReferenceType ref = inputType == null ? DEFAULT_REFERENCE_TYPE : inputType;
        return new ReferenceType[] {ref};
    }

    @Override
    public boolean isRelationSignSupported()
    {
        if( innerHub == null )
            return super.isRelationSignSupported();
        return innerHub.isRelationSignSupported();
    }

    @Override
    public Map<Element, Element[]> getReferences(Element[] startElements, TargetOptions dbOptions, String[] relationTypes, int maxLength,
            int direction)
    {
        if( innerHub == null )
            return super.getReferences( startElements, dbOptions, relationTypes, maxLength, direction );
        return innerHub.getReferences( startElements, dbOptions, relationTypes, maxLength, direction );
    }

    @Override
    public String getName()
    {
        if( innerHub == null )
            return "Custom collection hub";
        return "Custom " + innerHub.getName();
    }

    @Override
    public String getShortName()
    {
        if( innerHub == null )
            return "Custom collection hub";
        return "Custom " + innerHub.getShortName();
    }

    @Override
    public DataElementPath getModulePath()
    {
        if( innerHub == null )
            return super.getModulePath();
        return innerHub.getModulePath();
    }

    @Override
    public String toString()
    {
        if( innerHub == null )
            return "Custom collection hub";
        return "Custom " + innerHub.toString();
    }

    @Override
    public String[] getSupportedSpecies()
    {
        if( innerHub == null )
            return super.getSupportedSpecies();
        return innerHub.getSupportedSpecies();
    }

    @Override
    public Properties[] getSupportedInputs()
    {
        if( innerHub == null )
            return super.getSupportedInputs();
        return innerHub.getSupportedInputs();
    }

    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        return super.getSupportedMatching( input );
    }

    @Override
    public double getMatchingQuality(ReferenceType inputType, ReferenceType outputType)
    {
        if( innerHub == null )
            return super.getMatchingQuality( inputType, outputType );
        return innerHub.getMatchingQuality( inputType, outputType );
    }

    @Override
    public double getMatchingQuality(Properties input, Properties output)
    {
        if( innerHub == null )
            return super.getMatchingQuality( input, output );
        return innerHub.getMatchingQuality( input, output );
    }

    @Override
    public Map<String, String[]> getReferences(String[] inputList, ReferenceType inputType, ReferenceType outputType, Properties properties,
            FunctionJobControl jobControl)
    {
        if( innerHub == null )
            return super.getReferences( inputList, inputType, outputType, properties, jobControl );
        return innerHub.getReferences( inputList, inputType, outputType, properties, jobControl );
    }

    @Override
    public Map<String, String[]> getReferences(String[] inputList, Properties input, Properties output, FunctionJobControl jobControl)
    {
        if( innerHub == null )
            return super.getReferences( inputList, input, output, jobControl );
        return innerHub.getReferences( inputList, input, output, jobControl );
    }

}
