package biouml.plugins.keynodes;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.util.AddElementsUtils;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.plugins.keynodes.graph.UserHubEdge;
import biouml.plugins.sbgn.Type;
import biouml.standard.diagram.Util;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Species;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.exception.TableNoColumnException;

/**
 * @author anna
 *
 */
@ClassIcon ( "resources/visualize.gif" )
public class KeyNodeVisualization extends AnalysisMethodSupport<KeyNodeVisualizationParameters>
{
    private static final int MAX_DIAGRAM_ELEMENTS = 3000;
    public KeyNodeVisualization(DataCollection<?> origin, String name)
    {
        super(origin, name, new KeyNodeVisualizationParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        KeyNodeVisualizationParameters params = getParameters();
        DataElementPath knResultPath = params.getKnResultPath();

        DataElement knResult = knResultPath.getDataElement();
        AnalysisMethod method = AnalysisParametersFactory.readAnalysis( knResult );
        if( ! ( method instanceof PathGenerator ) || ! ( method.getParameters() instanceof BasicKeyNodeAnalysisParameters ) )
            method = AnalysisParametersFactory.readAnalysisPersistent( knResult );
        if( ! ( method instanceof PathGenerator ) || ! ( method.getParameters() instanceof BasicKeyNodeAnalysisParameters ) )
            throw new InvalidParameterException( "This table is not a result of master regulator node analysis or longest chain finder" );
        if( ( (BasicKeyNodeAnalysisParameters)method.getParameters() ).getBioHub() == null
                || ( (PathGenerator)method ).getKeyNodesHub() == null )
            throw new InvalidParameterException( "Result of analysis is incorrect (Unable to read biohub parameter)" );
        if( ! ( knResult instanceof TableDataCollection ) )
            throw new InvalidParameterException("This element is not a table");
        if( params.getSelectedItems() == null && "(none)".equals( params.getRankColumn() ) )
            throw new InvalidParameterException( "Rank column must be specified" );
    }

    boolean emptyInput = false;
    @Override
    public Diagram[] justAnalyzeAndPut() throws Exception
    {
        validateParameters();
        List<Diagram> result = new ArrayList<>();
        KeyNodeVisualizationParameters params = getParameters();
        TableDataCollection knResult = params.getKnResultPath().getDataElement(TableDataCollection.class);
        DataElementPath destination = params.getOutputPath();
        List<DataElement> selectedItems = params.getSelectedItems();

        AnalysisMethod method = AnalysisParametersFactory.readAnalysis( knResult );
        if( ! ( method instanceof PathGenerator ) || ! ( method.getParameters() instanceof BasicKeyNodeAnalysisParameters ) )
            method = AnalysisParametersFactory.readAnalysisPersistent( knResult );
        Species species = ( (BasicKeyNodeAnalysisParameters)method.getParameters() ).getSpecies();
        PathGenerator analysis = (PathGenerator)method;

        ColumnModel columnModel = knResult.getColumnModel();
        int hitsColumn = columnModel.optColumnIndex( "Hits" );
        if( hitsColumn < 0 || columnModel.getColumn( hitsColumn ).getValueClass() != StringSet.class )
        {
            hitsColumn = knResult.columns()
                    .filter( column -> column.getValueClass() == StringSet.class )
                    .mapToInt( column -> columnModel.optColumnIndex(column.getName()) )
                    .findFirst().orElseThrow( () -> new TableNoColumnException(knResult, "Hits") );
        }

        List<? extends DataElement> rdes = null;
        int totalElements = 0;
        if( selectedItems != null )
        {
            rdes = selectedItems;
            totalElements = selectedItems.size();
            emptyInput = totalElements == 0;
        }
        else
        {
            int scoreColumn = columnModel.getColumnIndex( parameters.getRankColumn() );
            knResult.sortTable( scoreColumn, parameters.isLowestRanking() );
            totalElements = Math.min( parameters.getNumTopRanking(), knResult.getSize() );
            rdes = knResult.stream().limit( totalElements ).collect( Collectors.toList() );
            emptyInput = knResult.getSize() == 0;
        }
        if( emptyInput )
            log.warning( "Input is empty (table or selection)" );

        DataCollection<?> origin = destination.getParentCollection();
        DiagramType diagramType = analysis.getKeyNodesHub().getVisualizationDiagramType();
        jobControl.setPreparedness(3);
        if( params.isSeparateResults() && !emptyInput )
        {
            int nameColumn = columnModel.optColumnIndex("Master molecule name");
            double oneNodeProgress = 95.0 / totalElements;

            for( int i = 0; i < totalElements && !jobControl.isStopped(); i++ )
            {
                jobControl.pushProgress( (int) ( i * oneNodeProgress ), (int) ( ( i + 1 ) * oneNodeProgress ) );
                RowDataElement rde = (RowDataElement)rdes.get( i );
                Diagram diagram = null;
                try
                {
                    String keyName = (nameColumn != -1 ? rde.getValues()[nameColumn].toString() : rde.getName()).replace("/", "");
                    if( keyName.contains( ":" ) )
                    {
                        log.warning( "Keynode name contains illegal symbol ':' ('" + keyName
                                + "'). It will be replaced by '_' in result file name." );
                        keyName = keyName.replaceAll( ":", "_" );
                    }
                    DataElementPath path = DataElementPath.create( origin, destination.getName() + ", " + keyName ).uniq();
                    log.info("Creating "+path.getName());
                    diagram = new Diagram(origin, new DiagramInfo(origin, path.getName()), diagramType);
                    Diagram d = fillDiagram( diagram, Arrays.asList( rde ), hitsColumn, 1, analysis, species );
                    if( d == null || d.isEmpty() )
                    {
                        log.log(Level.SEVERE,  "Diagram is empty; skipping" );
                    }
                    else
                    {
                        CollectionFactoryUtils.save( d );
                        result.add( d );
                    }
                }
                catch( Exception e1 )
                {
                    log.warning(ExceptionRegistry.log(e1));
                }
                jobControl.popProgress();
            }
        }
        else
        {
            Diagram diagram = new Diagram(origin, new DiagramInfo(origin, destination.getName()), diagramType);
            Diagram d = fillDiagram( diagram, rdes, hitsColumn, totalElements, analysis, species );
            if( d == null || ( d.getSize() == 0 && !emptyInput ) )
                throw new Exception( "Empty result: diagram is not created" );

            CollectionFactoryUtils.save( d );
            result.add( d );
        }
        if( result.size() == 0 )
            return null;
        jobControl.setPreparedness(100);
        return result.toArray(new Diagram[result.size()]);
    }

    private Diagram fillDiagram(Diagram diagram, List<? extends DataElement> des, int hitsColumn, int totalChains, PathGenerator analysis,
            Species species)
    {
        boolean visualizeAllPaths = parameters.isVisualizeAllPaths();
        Set<String> allhits = new HashSet<>();
        Set<String> allkeys = new HashSet<>();
        int limit = des.size() > totalChains ? totalChains : des.size();
        Set<Element> elements = new HashSet<>();
        for( int i = 0; i < limit && !jobControl.isStopped(); i++ )
        {
            DataElement de = des.get( i );
            if( ! ( de instanceof RowDataElement ) )
            {
                log.warning( "Element '" + de.getName() + "' of unexpected type '" + de.getClass().getName() + "' selected. Was expected "
                        + RowDataElement.class.getName() );
                continue;
            }
            RowDataElement rde = (RowDataElement)de;
            allkeys.addAll( analysis.getKeysFromName( rde.getName() ) );

            Object[] values = rde.getValues();
            Object valueHits = values[hitsColumn];
            StringSet hits = null;
            if( valueHits instanceof StringSet )
            {
                hits = (StringSet)valueHits;
                allhits.addAll( hits );
            }
            else
            {
                log.warning( "Unexpected value " + valueHits + " of type " + ( valueHits == null ? null : valueHits.getClass() ) + " in "
                        + rde.getCompletePath() );
            }
            if( hits != null )
            {
                try
                {
                    if( visualizeAllPaths )
                    {
                        List<Element> reactions = analysis.getAllReactions( rde.getName(), hits );
                        AddElementsUtils.addNodesToCompartment( reactions.stream().toArray( Element[]::new ), diagram, null, null );
                        jobControl.setPreparedness( 3 + i * 67 / limit );
                    }
                    else
                    {
                        List<Element[]> paths = analysis.generatePaths( rde.getName(), hits );
                        for( int j = 0; j < paths.size(); j++ )
                        {
                            Element[] path = paths.get( j );
                            if( path == null || path.length == 0 )
                                continue;
                            elements.addAll( Arrays.asList( path ) );
                            AddElementsUtils.addNodesToCompartment( path, diagram, null, null );
                            AddElementsUtils.addEdgesToCompartment( path, diagram, false, null );
                            addEdgeLabels( path, diagram );
                            jobControl.setPreparedness( 3 + i * 67 / limit + j * 67 / ( limit * paths.size() ) );
                        }
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE,  "Error while creating visualization for '" + rde.getName() + "'", e );
                }
            }

            if( diagram.getSize() > MAX_DIAGRAM_ELEMENTS )
            {
                String message = "Result becomes too big: it exceeds maximum number of allowed diagram elements - molecules and connections ("
                        + MAX_DIAGRAM_ELEMENTS
                        + ")."
                        + ( i > 0 ? " Please select less elements."
                                : " Please try to select other elements or run analysis with smaller radius." );
                log.log(Level.SEVERE,  message );
                throw new IllegalArgumentException( message );
            }
        }
        if( ( diagram.isEmpty() && !emptyInput ) || jobControl.isStopped() )
            return null;

        jobControl.setPreparedness( 70 );
        KeyNodesHub<?> bioHub = analysis.getKeyNodesHub();

        if( parameters.isAddParticipants() )
        {
            for( Node de : diagram.stream(Node.class) )
                AddReactantsAnalysis.addReactants(diagram, bioHub, species, de, !visualizeAllPaths);
        }

        Diagram d = bioHub.convert( diagram );
        colorDiagram( d, allhits, allkeys );
        d.getInfo().setNodeImageLocation( getClass(), "resources/visualization.gif" );
        //add information about species to diagram
        d.getAttributes().add( new DynamicProperty( DataCollectionUtils.SPECIES_PROPERTY, String.class, species.getLatinName() ) );

        if( emptyInput )
            return d;

        d.setView( null );
        jobControl.setPreparedness( 75 );
        if( jobControl.isStopped() )
            return null;
        jobControl.pushProgress( 75, 95 );

        if( parameters.isLayoutDiagram() )
            KeyNodeUtils.layoutDiagram( d, parameters.getLayoutXDist(), parameters.getLayoutYDist(), jobControl );

        jobControl.popProgress();
        return d;
    }

    private void addNoteLabels(Element[] elements, Diagram diagram)
    {
        for( Element element : elements )
        {
            if( element.getValue( KeyNodeConstants.REACTION_LABEL ) != null )
            {
                DataElement kernel = AddElementsUtils.optKernel( element );
                if( kernel == null )
                    continue;
                Node node = diagram.findNode( kernel.getName() );
                if( node == null )
                    continue;
                StringSet label;
                Object oldLabel = node.getKernel().getAttributes().getValue( Util.REACTION_LABEL );
                if( oldLabel != null && oldLabel instanceof StringSet )
                    label = (StringSet)oldLabel;
                else
                    label = new StringSet();
                label.add( (String)element.getValue( KeyNodeConstants.REACTION_LABEL ) );
                node.getKernel().getAttributes()
                        .add( new DynamicProperty( Util.REACTION_LABEL, StringSet.class, label ) );
            }
        }
    }

    public static void addEdgeLabels(Element[] elements, Diagram diagram)
    {
        Map<String, String> elementToLabel = new HashMap<>();
        for( Element element : elements )
        {
            if( element.getValue( KeyNodeConstants.REACTION_LABEL ) != null )
            {
                elementToLabel.put( element.getAccession(), (String)element.getValue( KeyNodeConstants.REACTION_LABEL ) );
            }
        }

        for( Element element : elements )
        {
            if( element.getLinkedFromPath() != null && !element.getLinkedFromPath().isEmpty() )
            {
                DataElement kernelR = AddElementsUtils.optKernel( element );
                if( kernelR == null )
                    continue;
                Node nodeTo = diagram.findNode( kernelR.getName() );
                if( nodeTo == null )
                    continue;

                String fromNodeName = DataElementPath.create( element.getLinkedFromPath() ).getName();

                if( elementToLabel.containsKey( fromNodeName ) )
                {
                    Node nodeFrom = diagram.findNode( fromNodeName );
                    if( nodeFrom == null )
                        continue;
                    Edge edge = nodeFrom.edges().findFirst( e -> e.getOutput().equals( nodeTo ) ).orElse( null );
                    if( edge != null )
                    {
                        edge.getAttributes().add( new DynamicProperty( Util.REACTION_LABEL, String.class,
                                elementToLabel.get( fromNodeName ) ) );
                    }
                }
            }
        }
    }

    public static void colorDiagram(Diagram diagram, Set<String> hits, Set<String> keys)
    {
        KeyNodeUtils.defineHighlightStyles( diagram.getViewOptions() );

        diagram.recursiveStream().select( Node.class ).forEach( n -> {
            String nodename = n.getName();
            String highlight = null;
            if( hits.contains( nodename ) )
            {
                if( keys.contains( nodename ) )
                    highlight = KeyNodeConstants.HIT_KEY_HIGHLIGHT;
                else
                    highlight = KeyNodeConstants.HIT_HIGHLIGHT;
            }
            else if( keys.contains( nodename ) )
            {
                highlight = KeyNodeConstants.KEY_HIGHLIGHT;
            }
            if( highlight != null )
            {
                if( n instanceof Compartment && n.getKernel() != null && n.getKernel().getType().equals( Type.TYPE_COMPLEX ) )
                {
                    final String hl = highlight;
                    ( (Compartment)n ).stream( Node.class ).forEach( subnode -> subnode.setPredefinedStyle( hl ) );
                }
                else
                    n.setPredefinedStyle( highlight );
            }

        } );

        diagram.stream( Edge.class ).filter( edge -> edge.getAttributes().getProperty( Util.REACTION_LABEL ) != null )
                .forEach( edge -> {
                    String label = edge.getAttributes().getValueAsString( Util.REACTION_LABEL );
                    if( label.contains( "+" ) )
                        edge.setPredefinedStyle( KeyNodeConstants.ACTIVATION_STYLE );
                    else if( label.contains( "-" ) )
                        edge.setPredefinedStyle( KeyNodeConstants.INHIBITION_STYLE );
                } );

        diagram.stream( Node.class ).filter( UserHubEdge::isUserReaction ).flatMap( Node::edges ).distinct().forEach( edge -> {
            edge.setPredefinedStyle( KeyNodeConstants.USER_REACTION_STYLE );
        } );
    }
}
