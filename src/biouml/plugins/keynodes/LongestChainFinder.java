package biouml.plugins.keynodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import biouml.plugins.keynodes.KeyNodeAnalysisParameters.GraphDecoratorEntry;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon("resources/FindLongestChain.gif")
public class LongestChainFinder extends AnalysisMethodSupport<LongestChainFinderParameters> implements PathGenerator
{
    public LongestChainFinder(DataCollection<?> origin, String name)
    {
        super( origin, name, new LongestChainFinderParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkNotEmpty( "bioHub" );
        checkRange( "maxRadius", 0, 30 );
        checkRange( "maxDijkstraDepth", 10, 200 );

        TableDataCollection input = parameters.getSource();
        int total = input.getSize();
        if( total == 0 )
            throw new IllegalArgumentException( "Molecule set is empty or was loaded with errors" );
        BioHubInfo bhi = parameters.getBioHub();
        if( bhi == null )
        {
            throw new IllegalArgumentException( "No biohub selected" );
        }
        KeyNodesHub<?> bioHub = parameters.getKeyNodesHub();
        if( bioHub == null )
        {
            throw new IllegalArgumentException( "Select custom biohub collection" );
        }
        ReferenceType inputType = ReferenceTypeRegistry.optReferenceType( input.getReferenceType() );
        ReferenceType[] types;
        if( inputType == null || ( types = bioHub.getSupportedMatching( inputType ) ) == null || types.length == 0 )
        {
            ReferenceType[] supportedTypes = bioHub.getSupportedInputTypes();
            String supportedStr = Stream.of( supportedTypes ).map( ReferenceType::getDisplayName ).map( n -> '\t' + n + '\n' )
                    .collect( Collectors.joining() );
            throw new IllegalArgumentException( "Search collection " + bioHub.getName()
                    + " does not support objects of given type. \nAcceptable " + ( supportedTypes.length > 1 ? "types are\n" : "type is\n" )
                    + supportedStr + "Try to convert table first." );
        }
        for( GraphDecoratorEntry decoratorParameters : parameters.getDecorators() )
        {
            if( !decoratorParameters.isAcceptable( bioHub ) )
            {
                throw new IllegalArgumentException( "Decorator " + decoratorParameters.getDecoratorName() + " is not acceptable for hub "
                        + bioHub.getName() );
            }
        }
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        // Update parameters just for the case
        for( GraphDecoratorEntry decoratorEntry : parameters.getDecorators() )
        {
            decoratorEntry.getParameters().setKeyNodeParameters( parameters );
        }

        TableDataCollection result = getLongestChains();
        if( jobControl != null )
            log.log(Level.FINE,  "Elapsed time " + jobControl.getElapsedTime() );
        if( result == null )
        {
            log.info( "Result was not created" );
            return null;
        }
        this.writeProperties( result );
        AnalysisParametersFactory.writePersistent( result, this );
        result.getInfo().setNodeImageLocation( getClass(), "resources/keynodes.gif" );
        result.getInfo().getProperties().setProperty( DataCollectionUtils.SPECIES_PROPERTY, parameters.getSpecies().getLatinName() );

        CollectionFactoryUtils.save( result );
        log.info( "DataCollection " + result.getName() + " created" );
        return result;
    }

    private TableDataCollection getLongestChains() throws Exception
    {
        final KeyNodesHub<?> bioHub = parameters.getKeyNodesHub();

        TableDataCollection input = parameters.getSource();

        final TargetOptions dbOptions = createTargetOptions( parameters );
        int size = input.getSize();
        if( parameters.isInputSizeLimited() && size > parameters.getInputSizeLimit() )
        {
            log.info( "Too many input " + bioHub.getElementTypeName() + "s. First " + parameters.getInputSizeLimit() + " "
                    + bioHub.getElementTypeName() + "s were selected for further analysis." );
            size = parameters.getInputSizeLimit();
        }

        jobControl.pushProgress( 0, 10 );
        final Set<String> targetNames = getInputList( input, dbOptions, size );
        if( targetNames.size() == 0 )
        {
            log.info( "No " + bioHub.getElementTypeName() + "s from input list can be taken for the analysis" );
            return null;
        }
        else
        {
            log.info( targetNames.size() + " " + parameters.getKeyNodesHub().getElementTypeName()
                    + "s from input list are taken for the analysis" );
        }
        if( jobControl.isStopped() )
            return null;
        jobControl.popProgress();

        jobControl.pushProgress( 10, 30 );
        log.info( "Chains detecting..." );
        //here we get possible paths from one node to others
        final Map<String, List<ChainLink>> res = getTargets( targetNames, dbOptions );
        jobControl.popProgress();
        if( jobControl.isStopped() )
            return null;

        jobControl.pushProgress( 30, 90 );
        List<Chain> chains = Collections.synchronizedList( new ArrayList<>() );
        //run Dijkstra's algorithm to find all possible chains from current element to all the others
        TaskPool.getInstance().iterate( targetNames, targetName -> {
            chains.addAll( runDijkstra( targetName, targetNames, res ) );
        }, jobControl );
        final int totalFound = chains.size();
        if( totalFound == 0 )
        {
            log.info( "No chain candidates found." );
            return null;
        }
        else
        {
            log.info( totalFound + " chain candidates found" );
        }
        jobControl.popProgress();
        if( jobControl.isStopped() )
            return null;

        jobControl.pushProgress( 90, 100 );
        log.info( "Filtering by score cutoff..." );
        filterChainScores( chains );
        log.info( chains.size() + " of " + totalFound + " chains passed the filter" );

        log.info( "Generating output..." );
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        ColumnModel columnModel = result.getColumnModel();
        columnModel.addColumn( "From input set", Integer.class );
        columnModel.addColumn( "Elements total", Integer.class );
        columnModel.addColumn( "Score", Double.class );
        columnModel.addColumn( "Hits", StringSet.class ).setHidden( true );
        columnModel.addColumn( "Hit names", StringSet.class );
        if( jobControl.isStopped() )
            return null;
        for( int i = 0; i < chains.size(); i++ )
        {
            Chain chain = chains.get( i );
            Object[] rowValues = new Object[] {chain.getTargetElementsNumber(), chain.getLength(), chain.score, chain.getHits(),
                    chain.getHitsTitles( bioHub )};
            TableDataCollectionUtils.addRow( result, chain.name, rowValues, true );
        }
        result.finalizeAddition();
        log.info( "Sorting by score..." );
        TableDataCollectionUtils.setSortOrder( result, "Score", false );
        jobControl.popProgress();
        return result;
    }

    private void filterChainScores(List<Chain> chains)
    {
        for( int i = chains.size() - 1; i >= 0; i-- )
        {
            if( chains.get( i ).score < parameters.getScoreCutoff() )
                chains.remove( i );
        }
    }

    private ConcurrentMap<String, List<ChainLink>> getTargets(Set<String> inputSet, final TargetOptions dbOptions) throws Exception
    {
        final ConcurrentMap<String, List<ChainLink>> targets = new ConcurrentHashMap<>();
        final Set<Element> allElements = StreamEx.of( inputSet ).map( name -> new Element( "stub/%//" + name ) ).toSet();
        TaskPool.getInstance().iterate( allElements, element -> {
            targets.put( element.getAccession(), getChainLinks( element, allElements, dbOptions ) );
        }, jobControl );
        return targets;
    }

    private List<ChainLink> getChainLinks(Element start, Set<Element> allElements, final TargetOptions dbOptions)
    {
        final String[] relTypes = getRelationTypes();
        Element[] elements = StreamEx.of( allElements ).remove( e -> e.getAccession().equals( start.getAccession() ) )
                .toArray( Element[]::new );
        List<Element[]> paths = parameters.getKeyNodesHub().getMinimalPaths( start, elements, dbOptions, relTypes,
                parameters.getMaxRadius(), parameters.getSearchDirection() );
        return StreamEx.of( paths ).nonNull().filter( p -> p.length != 0 ).map( p -> new ChainLink( p, start ) ).toList();
    }

    private Set<String> getInputList(DataCollection<?> input, TargetOptions dbOptions, int inputSizeLimit)
    {
        final String[] relTypes = getRelationTypes();
        Set<String> names = parameters.getKeyNodesHub().getNames( dbOptions, relTypes );
        return input.names().filter( names::contains ).limit( inputSizeLimit ).collect( Collectors.toSet() );
    }

    private List<Chain> runDijkstra(String startElementName, Set<String> inputs, Map<String, List<ChainLink>> elementToLinks)
    {
        List<ChainLink> links = elementToLinks.get( startElementName );
        if( links.isEmpty() )
            return Collections.emptyList();

        Map<String, GraphNode> nameToNode = StreamEx.of( inputs ).remove( startElementName::equals )
                .mapToEntry( input -> new GraphNode( input ) ).append( startElementName, new GraphNode( startElementName, 0 ) ).toMap();

        Set<String> ignore = new HashSet<>();
        List<String> currentElements = new ArrayList<>();
        currentElements.add( startElementName );
        int count = 0;
        while( !currentElements.isEmpty() && ++count < parameters.getMaxDijkstraDepth() )
            currentElements = runDijkstraIteration( currentElements, elementToLinks, nameToNode, ignore );

        return StreamEx.of( inputs ).remove( startElementName::equals )
                .map( endElementName -> createChain( startElementName, endElementName, nameToNode, elementToLinks ) ).nonNull().toList();
    }

    private List<String> runDijkstraIteration(List<String> currentElements, Map<String, List<ChainLink>> elementToLinks,
            Map<String, GraphNode> nameToNode, Set<String> ignore)
    {
        List<String> nextElements = new ArrayList<>();
        for( String currentElement : currentElements )
        {
            GraphNode currentNode = nameToNode.get( currentElement );
            List<ChainLink> links = elementToLinks.get( currentElement );
            for( ChainLink link : links )
            {
                String nextElement = link.getEndElement();
                if( ignore.contains( nextElement ) )
                    continue;

                GraphNode nextNode = nameToNode.get( nextElement );
                int newLength = currentNode.lengthFromStart + link.getLength();
                if( nextNode.lengthFromStart > newLength )
                    nextNode.setNewPath( currentElement, newLength );
                if( !currentElements.contains( nextElement ) )
                    nextElements.add( nextElement );
            }
            ignore.add( currentElement );
        }
        return nextElements;
    }

    private Chain createChain(String startElement, String endElement, Map<String, GraphNode> nameToNode,
            Map<String, List<ChainLink>> elementToLinks)
    {
        if( nameToNode.get( endElement ).previousElementName == null )
            return null;

        List<ChainLink> list = StreamEx
                .iterate( endElement, e -> startElement.equals( e ) ? null : nameToNode.get( e ).previousElementName )
                .takeWhile( Objects::nonNull )
                .pairMap(
                        (cur, prev) -> StreamEx.of( elementToLinks.get( prev ) ).findFirst( link -> link.getEndElement().equals( cur ) )
                                .orElse( null ) ).toList();

        if( list.stream().anyMatch( Objects::isNull ) )
            return null;

        Element[] path = StreamEx.of( list ).<Element>flatMap( link -> StreamEx.of( link.getPath() ) )
                .collapse( (e1, e2) -> e1.getAccession().equals( e2.getAccession() ) ).toArray( Element[]::new );

        return new Chain( startElement, endElement, path, nameToNode.keySet(), parameters.getScoreCoeff() );
    }

    protected static TargetOptions createTargetOptions(KeyNodeAnalysisParameters parameters)
    {
        return new TargetOptions( StreamEx.of( parameters.getDecorators() ).map( GraphDecoratorEntry::createCollectionRecord )
                .prepend( new CollectionRecord( KeyNodesHub.KEY_NODES_HUB, true ) ).toArray( CollectionRecord[]::new ) );
    }

    protected String[] getRelationTypes()
    {
        return new String[] {parameters.getSpecies().getLatinName()};
    }

    private static class GraphNode
    {
        private final String elementName;
        private int lengthFromStart = Integer.MAX_VALUE;
        private String previousElementName;

        public GraphNode(String elementName)
        {
            this.elementName = elementName;
        }
        public GraphNode(String elementName, int lengthFromStart)
        {
            this( elementName );
            this.lengthFromStart = lengthFromStart;
        }

        public void setNewPath(String previousElement, int lengthFromStart)
        {
            this.previousElementName = previousElement;
            this.lengthFromStart = lengthFromStart;
        }
    }

    private static class ChainLink
    {
        private final String startElement;
        private final String endElement;
        private final Element[] path;

        public ChainLink(Element[] path, Element startElement)
        {
            this.path = path;
            this.startElement = startElement.getAccession();
            this.endElement = startElement.equals( path[0] ) ? path[path.length - 1].getAccession() : path[0].getAccession();
        }
        public String getEndElement()
        {
            return endElement;
        }
        public Element[] getPath()
        {
            return path;
        }
        public int getLength()
        {
            return path.length;
        }
    }

    private static class Chain
    {
        private final Element[] chainElements;
        private final String name;
        private final double score;
        private final List<String> elementsFromInput;

        public Chain(String startElementName, String endElementName, Element[] elements, Set<String> targetElements, double scoreCoeff)
        {
            this.name = startElementName + " -> " + endElementName;
            this.chainElements = elements.clone();
            elementsFromInput = StreamEx.of( elements ).map( e -> e.getAccession() ).filter( targetElements::contains ).toList();
            score = getTargetElementsNumber() / ( getLength() + scoreCoeff );
        }
        public int getLength()
        {
            return ( chainElements.length + 1 ) / 2;
        }
        public int getTargetElementsNumber()
        {
            return elementsFromInput.size();
        }
        public StringSet getHits()
        {
            return new StringSet( elementsFromInput );
        }
        public StringSet getHitsTitles(KeyNodesHub<?> bioHub)
        {
            return elementsFromInput.stream().map( Element::new ).map( bioHub::getElementTitle )
                    .collect( Collectors.toCollection( StringSet::new ) );
        }
    }

    @Override
    public List<Element[]> generatePaths(String startElement, StringSet hits)
    {
        List<Element[]> paths = new ArrayList<>();
        Element[] chain = hits.stream().map( Element::new ).toArray( Element[]::new );
        for( int j = 0; j < chain.length - 1; j++ )
            paths.add( parameters.getKeyNodesHub().getMinimalPath( chain[j], chain[j + 1], createTargetOptions( parameters ),
                    getRelationTypes(), parameters.getMaxRadius(), parameters.getReverseDirection() ) );
        return paths;
    }

    @Override
    public StringSet getKeysFromName(String name)
    {
        return StreamEx.of( name.split( "->" ) ).map( e -> e.trim() ).toCollection( StringSet::new );
    }

    @Override
    public List<Element> getAllReactions(String startElement, StringSet hits)
    {
        List<Element> reactions = new ArrayList<>();
        Element[] chain = hits.stream().map( Element::new ).toArray( Element[]::new );
        for( int j = 0; j < chain.length - 1; j++ )
            reactions.addAll( parameters.getKeyNodesHub().getAllReactions( chain[j], new Element[] {chain[j + 1]},
                    createTargetOptions( parameters ), getRelationTypes(), parameters.getMaxRadius(), parameters.getReverseDirection() ) );
        return reactions;
    }

    @Override
    public KeyNodesHub<?> getKeyNodesHub()
    {
        return parameters.getKeyNodesHub();
    }
}
