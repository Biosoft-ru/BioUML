package biouml.plugins.keynodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.util.AddElementsUtils;
import biouml.model.xml.XmlDiagramType;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.plugins.keynodes.graph.UserHubEdge;
import biouml.standard.type.Base;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Species;
import biouml.workbench.graph.DiagramToGraphTransformer;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.RelationType;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graph.FastGridLayouter;
import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.OrthogonalPathLayouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * Cluster a list of molecules/genes by shortest path:
 * two genes are connected (belong to the same cluster), if there exists
 * a path between these genes in the selected hub within the given distance
 */

@ClassIcon ( "resources/shortest-path.gif" )
public class ShortestPathClustering extends AnalysisMethodSupport<ShortestPathClusteringParameters>
{
    public static final int MAX_CLUSTERS = 50;

    protected <T extends ShortestPathClusteringParameters> ShortestPathClustering(DataCollection<?> origin, String name, T parameters)
    {
        super( origin, name, parameters );
    }
    public ShortestPathClustering(DataCollection<?> origin, String name)
    {
        super(origin, name, new ShortestPathClusteringParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        try
        {
            super.validateParameters();

            ShortestPathClusteringParameters parameters = getParameters();

            checkRange("maxRadius", 1, 20);
            checkNotEmptyCollection("sourcePath");
            checkNotEmpty( "bioHub" );

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

            TableDataCollection input = parameters.getSourcePath().getDataElement(TableDataCollection.class);
            ReferenceType inputType = ReferenceTypeRegistry.optReferenceType(input.getReferenceType());
            ReferenceType[] types;
            if( inputType == null || ( types = bioHub.getSupportedMatching( inputType ) ) == null || types.length == 0 )
            {
                ReferenceType[] supportedTypes = bioHub.getSupportedInputTypes();
                String supportedStr = StreamEx.of(supportedTypes).map(ReferenceType::getDisplayName).joining("\n\t");
                supportedStr = "\t" + supportedStr;
                throw new IllegalArgumentException("Search collection " + bioHub.getName()
                        + " does not support objects of given type. \nAcceptable "
                        + ( supportedTypes.length > 1 ? "types are\n" : "type is\n" ) + supportedStr + "\nTry to convert table first.");
            }
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public DataElement[] justAnalyzeAndPut() throws Exception
    {
        List<String> names = parameters.getSourcePath().getDataElement(TableDataCollection.class).getNameList();
        log.info("Traversing the network...");
        jobControl.pushProgress(0, 50);
        Diagram result = createFullDiagram(names);
        if(jobControl.isStopped())
            return null;
        if( result == null )
        {
            log.info("Result was not created");
            return null;
        }
        result = processReactions( result );
        jobControl.popProgress();
        DataCollection<DataElement> outputCollection = DataCollectionUtils.createSubCollection( parameters.getOutputPath() );
        jobControl.pushProgress(50, 90);
        createClusters(outputCollection, new HashSet<>(names), result);
        jobControl.popProgress();
        DataElement table = outputCollection.get("Clusters");
        DataElement firstCluster = outputCollection.get("Cluster 1");
        if(table == null)
            return new ru.biosoft.access.core.DataElement[] {outputCollection};
        if(firstCluster == null)
            return new ru.biosoft.access.core.DataElement[] {outputCollection, table};
        return new ru.biosoft.access.core.DataElement[] {outputCollection, table, firstCluster};
    }

    protected Diagram processReactions(final Diagram diagram)
    {
        //HACK: Alexander wants to join reactions by enzymes for kegg_recon diagrams
        if( ! ( diagram.getType() instanceof XmlDiagramType )
                || !"kegg_recon.xml".equals( ( (XmlDiagramType)diagram.getType() ).getName() ) )
            return diagram;

        class JoinedReaction
        {
            List<Node> reactions = new ArrayList<>();
        }

        List<JoinedReaction> joinedReactions = new ArrayList<>();
        List<Node> reactions = diagram.stream( Node.class ).filter( n -> n.getKernel() instanceof Reaction ).toList();
        int reactionsSize = reactions.size();
        Set<Node> processedNodes = new HashSet<>();
        //TODO: modify joining condition
        for( int i = 0; i < reactionsSize - 1; i++ )
        {
            Node curReaction = reactions.get( i );
            if( processedNodes.contains( curReaction ) )
                continue;
            JoinedReaction jr = new JoinedReaction();
            jr.reactions.add( curReaction );
            Set<String> curModifiers = new HashSet<>();
            for( Edge edge : curReaction.getEdges() )
            {
                if( edge.getKernel() instanceof SpecieReference && ! ( (SpecieReference)edge.getKernel() ).isReactantOrProduct() )
                    curModifiers.add( edge.getOtherEnd( curReaction ).getName() );
            }
            processedNodes.add( curReaction );
            for( int j = i + 1; j < reactionsSize; j++ )
            {
                Node reactionToJoin = reactions.get( j );
                if( processedNodes.contains( reactionToJoin ) )
                    continue;
                Set<String> joinModifiers = new HashSet<>();
                for( Edge edge : reactionToJoin.getEdges() )
                {
                    if( edge.getKernel() instanceof SpecieReference && ! ( (SpecieReference)edge.getKernel() ).isReactantOrProduct() )
                        joinModifiers.add( edge.getOtherEnd( reactionToJoin ).getName() );
                }
                for( String joinModifier : joinModifiers )
                {
                    if( curModifiers.contains( joinModifier ) )
                    {
                        jr.reactions.add( reactionToJoin );
                        processedNodes.add( reactionToJoin );
                        break;
                    }
                }
            }
            joinedReactions.add( jr );
        }

        try
        {
            Diagram diagramWithJoinedReactions = diagram.getType().clone().createDiagram( null, "full joined diagram",
                    new DiagramInfo( "full joined diagram" ) );
            KeyNodeUtils.defineHighlightStyles( diagramWithJoinedReactions.getViewOptions() );
            diagram.stream( Node.class ).remove( n -> n.getKernel() instanceof Reaction )
                    .map( de -> de.clone( diagramWithJoinedReactions, de.getName() ) ).forEach( diagramWithJoinedReactions::put );
            for( JoinedReaction jr : joinedReactions )
            {
                if( jr.reactions.size() == 1 )
                {
                    Node rn = jr.reactions.get( 0 );
                    diagramWithJoinedReactions.put( rn.clone( diagramWithJoinedReactions, rn.getName() ) );
                    for( Edge edge : rn.getEdges() )
                        diagramWithJoinedReactions.put( edge.clone( diagramWithJoinedReactions, edge.getName() ) );
                }
                else
                {
                    class Link implements Comparable<Link>
                    {
                        final @Nonnull Node from;
                        final @Nonnull Node to;
                        final boolean isModifier;
                        final Base kernel;
                        final DynamicPropertySet attrs;
                        public Link(@Nonnull Node from, @Nonnull Node to, DynamicPropertySet attrs, Base oldKernel)
                        {
                            this.from = from;
                            this.to = to;
                            this.kernel = oldKernel;
                            this.attrs = attrs;
                            this.isModifier = oldKernel instanceof SpecieReference
                                    && ! ( (SpecieReference)oldKernel ).isReactantOrProduct();
                        }
                        @Override
                        public int compareTo(Link o)
                        {
                            if( isModifier && !o.isModifier )
                                return 1;
                            else if( o.isModifier && !isModifier )
                                return -1;
                            boolean isSR = kernel instanceof SpecieReference;
                            boolean isOtherSR = o.kernel instanceof SpecieReference;
                            if( isSR && ! isOtherSR )
                                return -1;
                            if( ! isSR && isOtherSR )
                                return 1;
                            else if( isSR && isOtherSR )
                                return ( (SpecieReference)kernel ).getSpecie().compareTo( ( (SpecieReference)o.kernel ).getSpecie() );
                            return 0;
                        }
                    }
                    String name = DefaultSemanticController.generateUniqueNodeName( diagramWithJoinedReactions,
                            UserHubEdge.USER_REACTION_NAME_PREFIX, true, "" );
                    Reaction joinedKernel = new Reaction( null, name );
                    Node joinedReaction = new Node( diagramWithJoinedReactions, joinedKernel );
                    StringBuilder title = new StringBuilder( "Joined:" );
                    List<Link> links = new ArrayList<>();
                    for( Node rn : jr.reactions )
                    {
                        title.append( " " ).append( rn.getName() );
                        for( Edge edge : rn.getEdges() )
                        {
                            @Nonnull Node from = edge.getInput();
                            if( from == rn )
                                from = joinedReaction;
                            else
                                from = CollectionFactory.getDataElement( from.getName(), diagramWithJoinedReactions, Node.class );
                            @Nonnull Node to = edge.getOutput();
                            if( to == rn )
                                to = joinedReaction;
                            else
                                to = CollectionFactory.getDataElement( to.getName(), diagramWithJoinedReactions, Node.class );
                            Base kernel = edge.getKernel();
                            if( kernel instanceof SpecieReference )
                                kernel = ( (SpecieReference)kernel ).clone( joinedKernel, kernel.getName() );
                            links.add( new Link( from, to, edge.getAttributes(), kernel ) );
                        }
                        DatabaseReference[] drs = ( (Reaction)rn.getKernel() ).getDatabaseReferences();
                        if( drs != null && drs.length > 0 )
                            joinedKernel.addDatabaseReferences( drs );
                    }
                    joinedReaction.setTitle( title.toString() );
                    joinedKernel.setTitle( title.toString() );
                    diagramWithJoinedReactions.put( joinedReaction );
                    Collections.sort( links );
                    for( Link link : links )
                    {
                        if( link.kernel instanceof SpecieReference )
                            joinedKernel.put( (SpecieReference)link.kernel );
                        Edge newEdge = new Edge( diagramWithJoinedReactions, link.kernel, link.from, link.to );
                        for( DynamicProperty dp : link.attrs )
                            newEdge.getAttributes().add( dp );
                        diagramWithJoinedReactions.put( newEdge );
                    }
                }
            }
            return diagramWithJoinedReactions;
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    private void createClusters(final @Nonnull DataCollection<?> outputCollection, final Set<String> names, final Diagram result)
    {
        Set<Node> nodes = result.stream( Node.class ).toSet();

        class ClusterInfo implements Comparable<ClusterInfo>
        {
            Set<DiagramElement> elements = new HashSet<>();
            int size;

            @Override
            public int compareTo(ClusterInfo o)
            {
                return size > o.size ? -1 : size < o.size ? 1 : 0;
            }
        }

        List<ClusterInfo> clusters = new ArrayList<>();
        log.info("Finding clusters...");

        while(!nodes.isEmpty())
        {
            ClusterInfo cluster = new ClusterInfo();
            Iterator<Node> iterator = nodes.iterator();
            Node node = iterator.next();
            iterator.remove();

            Set<Node> newNodes = new HashSet<>();
            newNodes.add(node);

            while(!newNodes.isEmpty())
            {
                Iterator<Node> newNodesIterator = newNodes.iterator();
                Node curNode = newNodesIterator.next();
                newNodesIterator.remove();
                cluster.elements.add(curNode);
                for(Edge edge : curNode.getEdges())
                {
                    cluster.elements.add(edge);
                    edge.nodes().remove( cluster.elements::contains ).forEach( newNodes::add );
                }
            }
            nodes.removeAll(cluster.elements);
            clusters.add(cluster);
        }
        log.info("Found clusters: "+clusters.size());
        if(clusters.size() > MAX_CLUSTERS)
        {
            log.warning( "Too many clusters: only "+MAX_CLUSTERS+" will be kept" );
            clusters = clusters.subList( 0, MAX_CLUSTERS );
        }
        jobControl.setPreparedness(18);
        for(ClusterInfo cluster: clusters)
        {
            cluster.size = (int)StreamEx.of(cluster.elements)
                    .filter( de -> de instanceof Node && names.contains( de.getName() ) )
                    .count();
        }
        Collections.sort(clusters);
        if(jobControl.isStopped())
            return;
        jobControl.setPreparedness(20);
        jobControl.pushProgress(20, 100);
        final TableDataCollection summary = TableDataCollectionUtils.createTableDataCollection(outputCollection, "Clusters");
        summary.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, String.valueOf(true));
        summary.getColumnModel().addColumn("Diagram", DataElementPath.class);
        summary.getColumnModel().addColumn("Size", Integer.class);
        summary.getColumnModel().addColumn("Hits", StringSet.class).setHidden(true);
        summary.getColumnModel().addColumn("Hit names", StringSet.class);

        final ShortestPathClustering analysis = this;
        final KeyNodesHub<?> bioHub = parameters.getKeyNodesHub();
        jobControl.forCollection(clusters, new Iteration<ClusterInfo>() {
            int clusterNum=0;

            @Override
            public boolean run(ClusterInfo cluster)
            {
                try
                {
                    clusterNum++;
                    String name = String.format("Cluster%2d", clusterNum);
                    log.info("Creating "+name);
                    Diagram diagram = result.getType().clone().createDiagram( outputCollection, name, new DiagramInfo( name ) );
                    KeyNodeUtils.defineHighlightStyles( diagram.getViewOptions() );
                    Set<String> hits = new TreeSet<>();
                    Set<String> hitNames = new TreeSet<>();
                    for(DiagramElement de : cluster.elements)
                    {
                        if(de instanceof Node)
                        {
                            diagram.put(de.clone(diagram, de.getName()));
                            if(names.contains(de.getName()))
                            {
                                hits.add(de.getName());
                                hitNames.add(de.getTitle());
                            }
                        }
                    }
                    StreamEx.of(cluster.elements).select( Edge.class )
                        .map( de -> de.clone(diagram, de.getName()) )
                        .forEach( diagram::put );

                    if(parameters.isUseFullPath())
                    {
                        for(Node de : diagram.stream( Node.class ))
                        {
                            AddReactantsAnalysis.addReactants( diagram, bioHub,
                                    parameters.getSpecies(), de, true );
                        }
                    }
                    Diagram resultDiagram = bioHub.convert( diagram );
                    layoutDiagram( resultDiagram );
                    writeProperties( resultDiagram );
                    AnalysisParametersFactory.writePersistent( resultDiagram, analysis );
                    //add information about species to diagram
                    resultDiagram.getAttributes().add(
                            new DynamicProperty( DataCollectionUtils.SPECIES_PROPERTY, String.class, parameters.getSpecies().getLatinName() ) );

                    CollectionFactoryUtils.save( resultDiagram );
                    TableDataCollectionUtils.addRow( summary, String.valueOf( clusterNum ), new Object[] {resultDiagram.getCompletePath(),
                            cluster.size, new StringSet( hits ), new StringSet( hitNames )}, true );
                    return true;
                }
                catch( Exception e )
                {
                    throw ExceptionRegistry.translateException(e);
                }
            }
        });
        summary.finalizeAddition();
        CollectionFactoryUtils.save(summary);
        jobControl.popProgress();
    }

    protected Diagram createFullDiagram(List<String> names)
    {
        Diagram result = null;

        TargetOptions dbOptions = KeyNodeAnalysis.getDBOptions();
        KeyNodesHub<?> bioHub = parameters.getKeyNodesHub();
        int maxRadius = parameters.getMaxRadius();
        int direction = parameters.getSearchDirection();

        DiagramType diagramType = bioHub.getVisualizationDiagramType();

        String[] relTypes = new String[] {Species.getDefaultSpecies( parameters.getSourcePath().optDataCollection() ).getLatinName()};
        jobControl.setPreparedness(3);
        try
        {
            result = diagramType.createDiagram( null, "full diagram", new DiagramInfo(null, "full diagram"));
        }
        catch( Exception e1 )
        {
            return null;
        }
        KeyNodeUtils.defineHighlightStyles( result.getViewOptions() );
        Diagram d = fillDiagram(result, names, bioHub, dbOptions, relTypes, maxRadius, direction, parameters.isUseFullPath());
        if( d != null && d.getSize() != 0 )
        {
            result = d;
        }

        jobControl.setPreparedness(100);
        return result;
    }

    protected Diagram fillDiagram(Diagram diagram, List<String> names, KeyNodesHub<?> bioHub, TargetOptions dbOptions, String[] relTypes,
            int maxRadius, int direction, boolean fullPath)
    {
        if(names.size() > parameters.getInputSizeLimit())
        {
            log.warning("Input table contains too many entries: first "+parameters.getInputSizeLimit()+" will be used");
            names = names.subList(0, parameters.getInputSizeLimit());
        }
        Set<String> input = new HashSet<>(names);

        int cntNodes = 0;
        Iterator<String> iter = names.iterator();
        int totalNodes = names.size();
        Map<String, Integer> added = new HashMap<>();
        while( iter.hasNext() && !jobControl.isStopped() )
        {
            String name = iter.next();

            input.remove(name);

            Element element1 = new Element("stub/%//" + name);
            Element[] elements2 = StreamEx.of( input ).map( n -> new Element( "stub/%//" + n ) ).toArray( Element[]::new );

            try
            {
                List<Element[]> paths = bioHub.getMinimalPaths(element1, elements2, dbOptions, relTypes, maxRadius, direction);
                for( Element[] path : paths )
                {
                    if( path == null || path.length == 0 )
                        continue;
                    int pathLength = path.length;
                    String pathName = path[0].getPath() + " " + path[pathLength - 1].getPath();
                    String pathReverseName = path[pathLength - 1].getPath() + " " + path[0].getPath();
                    if( added.keySet().contains(pathName) )
                    {
                        int prevLength = added.get(pathName);
                        added.put(pathName, Math.min(prevLength, pathLength));
                        added.put(pathReverseName, Math.min(prevLength, pathLength));
                        continue;
                    }
                    if( fullPath )
                    {
                        AddElementsUtils.addNodesToCompartment(path, diagram, null, null);
                        AddElementsUtils.addEdgesToCompartment(path, diagram, false, null);
                    }
                    else
                    {
                        if( pathLength <= 2 )
                        {
                            AddElementsUtils.addNodesToCompartment(path, diagram, null, null);
                            AddElementsUtils.addEdgesToCompartment(path, diagram, false, null);
                        }
                        else
                        {
                            Element[] clusterPath = new Element[2];
                            clusterPath[0] = path[0];
                            clusterPath[1] = path[path.length - 1];
                            clusterPath[0].setRelationType(RelationType.SEMANTIC);
                            clusterPath[1].setRelationType(RelationType.SEMANTIC);
                            if( clusterPath[0].getLinkedFromPath() != null )
                            {
                                clusterPath[0].setLinkedFromPath(clusterPath[1].getPath());
                                clusterPath[0].setLinkedLength(path.length);
                                clusterPath[1].setLinkedFromPath(null);
                            }
                            else
                            {
                                clusterPath[0].setLinkedFromPath(null);
                                clusterPath[1].setLinkedFromPath(clusterPath[0].getPath());
                                clusterPath[1].setLinkedLength(path.length);
                            }
                            AddElementsUtils.addNodesToCompartment(clusterPath, diagram, null, null);
                            AddElementsUtils.addEdgesToCompartment(clusterPath, diagram, false, null);
                        }
                    }
                    added.put(path[pathLength - 1].getPath() + " " + path[0].getPath(), pathLength);
                    added.put(path[0].getPath() + " " + path[pathLength - 1].getPath(), pathLength);
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Error while creating shortest path graph for " + element1.getAccession(), e);
            }

            jobControl.setPreparedness(3 + cntNodes * 70 / totalNodes);
            cntNodes++;
            if( direction != BioHub.DIRECTION_BOTH )
                input.add(name);
        }
        if(jobControl.isStopped()) return null;

        if( diagram.isEmpty() )
            return null;

        input = new HashSet<>(names);
        for( Node c : diagram.stream( Node.class ) )
        {
            if( input.contains( c.getName() ) )
            {
                c.setPredefinedStyle( KeyNodeConstants.HIT_HIGHLIGHT );
            }
        }
        //Add edge length marks properties if only connections are displayed
        if( !fullPath )
        {
            for( Edge e : diagram.stream( Edge.class ) )
            {
                try
                {
                    DataElementPath inputPath = DataElementPath.create(e.getInput().getKernel());
                    DataElementPath outputPath = DataElementPath.create(e.getOutput().getKernel());
                    String pathName = inputPath.toString() + " " + outputPath.toString();
                    if( added.keySet().contains(pathName) )
                    {
                        //molecule-reaction-molecule path have length 1
                        String pathLength = String.valueOf(( added.get(pathName) - 1 ) / 2);
                        DynamicProperty dp = new DynamicProperty( "edgeMark", String.class, pathLength );
                        e.setTitle( pathLength );
                        e.getAttributes().add(dp);
                    }
                }
                catch( Exception e1 )
                {
                }
            }
        }

        diagram.setView(null);
        jobControl.setPreparedness(80);
        if( jobControl.isStopped() )
            return null;
        return diagram;
    }

    private void layoutDiagram(Diagram diagram)
    {
        Layouter layouter;

        if( diagram.getSize() < 1100 && parameters.isUseFullPath())
        {
            layouter = new FastGridLayouter();
            OrthogonalPathLayouter pathLayouter = new OrthogonalPathLayouter();
            pathLayouter.setGridX(10);
            pathLayouter.setGridY(10);
            ( (FastGridLayouter)layouter ).getPathLayouterWrapper().setPathLayouter( pathLayouter );
        }
        else if( diagram.getSize() < 2000 )
            layouter = new HierarchicLayouter();
        else
            layouter = new ForceDirectedLayouter();

        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        new PathwayLayouter(layouter).doLayout(graph, null);
        DiagramToGraphTransformer.applyLayout(graph, diagram);
    }
}
