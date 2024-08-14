
package ru.biosoft.analysis.diagram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramTypeConverter;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.util.AddElementsUtils;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import biouml.workbench.graph.DiagramToGraphTransformer;
import biouml.workbench.graphsearch.GraphSearchOptions;
import biouml.workbench.graphsearch.QueryEngine;
import biouml.workbench.graphsearch.QueryEngineRegistry;
import biouml.workbench.graphsearch.QueryOptions;
import biouml.workbench.graphsearch.SearchElement;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graph.ForceDirectedLayouter;

/**
 * @author anna
 */
@ClassIcon ( "resources/extend-network.gif" )
public class DiagramExtensionAnalysis extends AnalysisMethodSupport<DiagramExtensionAnalysisParameters>
{
    private static final int MAX_LINKED_REACTIONS = 100;
    private static final Set<String> stopList = new HashSet<>();

    private final Map<ru.biosoft.access.core.DataElementPath, QueryEngine> queryEngines = new HashMap<>();

    public DiagramExtensionAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new DiagramExtensionAnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        DiagramExtensionAnalysisParameters params = getParameters();
        checkPaths();
        DataElementPath inputPath = params.getInputDiagramPath();
        DataElement de = inputPath.optDataElement();

        if( ! ( de instanceof Diagram ) )
            throw new IllegalArgumentException("Element " + inputPath.toString() + " is not a diagram");
    }

    /**
     * main analysis method
     * @return
     * @throws Exception
     */
    @Override
    public Diagram justAnalyzeAndPut() throws Exception
    {
        log.info("Diagram extension analysis started");
        Diagram diagram = enrichDiagram();
        CollectionFactoryUtils.save(diagram);
        log.info("Diagram " + diagram.getName() + " was successfully created.");
        return diagram;
    }

    private @Nonnull Diagram enrichDiagram() throws Exception
    {
        validateParameters();
        DiagramExtensionAnalysisParameters parameters = getParameters();
        DataElementPath diagramPath = parameters.getOutputDiagramPath();
        DataElementPath inputPath = parameters.getInputDiagramPath();
        Diagram inputDiagram = inputPath.getDataElement(Diagram.class);
        int stepNumber = parameters.getStepNumber();

        Diagram diagram = inputDiagram.clone(diagramPath.getParentCollection(), diagramPath.getName());

        if( stepNumber == 0 )
            return diagram;

        AddElementsUtils.fixCurrentNodes(diagram, true);
        log.info("Adding nodes to diagram...");
        DiagramTypeConverter[] converters = AddElementsUtils.getAvailableConverters(diagram);
        enrichCompartment(diagram, stepNumber, diagram, converters);
        log.info("Layouting diagram...");
        DiagramToGraphTransformer.layout(diagram, new ForceDirectedLayouter());
        AddElementsUtils.fixCurrentNodes(diagram, false);
        diagram.setView(null);
        return diagram;
    }

    /**
     * Add reaction and participants to compartment if it already contains at least two reaction components. Performs numSteps iterations.
     */
    private void enrichCompartment(Compartment compartment, int numSteps, Diagram diagram, DiagramTypeConverter[] converters)
            throws Exception
    {
        //TODO: optimize search etc. to increase speed
        for( DiagramElement de : compartment )
        {
            if( de instanceof Compartment )
            {
                Base kernel = de.getKernel();
                if( kernel == null || kernel.getType().equals(Type.TYPE_COMPARTMENT) )
                    enrichCompartment((Compartment)de, numSteps, diagram, converters);
            }
        }
        jobControl.setPreparedness(5);
        boolean reactionsOnly = parameters.isReactionsOnly();
        int stepPercent = 90 / numSteps;
        for( int i = 0; i < numSteps; i++ )
        {
            Reaction[] toAdd = findReactions(compartment);
            int numToAdd = toAdd.length;
            for( int j = 0; j < numToAdd; j++ )
            {
                Reaction reaction = toAdd[j];
                Node reactionNode = null;
                boolean newReaction = false;
                if( !compartment.contains(reaction.getName()) )
                {
                    AddElementsUtils.addNode(compartment, reaction, converters, true, null);
                    newReaction = true;
                }
                reactionNode = compartment.findNode(reaction.getName());

                for( SpecieReference sr : reaction.getSpecieReferences() )
                {
                    String specieName = sr.getSpecieName();
                    DataElement de = getSpecieDataElement(sr);
                    if( de != null )
                    {
                        Node node = null;
                        boolean newNode = false;
                        if( !compartment.contains(specieName) )
                        {
                            if( reactionsOnly )
                                continue;

                            AddElementsUtils.addNode(compartment, (Base)de, converters, true, null);
                            newNode = true;
                        }
                        node = compartment.findNode(specieName);
                        if( node != null )
                        {

                            boolean hasEdge = false;
                            if( !newReaction && !newNode )
                            {
                                Edge[] edges = reactionNode.getEdges();

                                for( Edge e : edges )
                                {
                                    Node in = e.getInput();
                                    Node out = e.getOutput();
                                    if( ( in.getName().equals(reaction.getName()) && out.getName().equals(specieName) )
                                            || ( out.getName().equals(reaction.getName()) && in.getName().equals(specieName) ) )
                                    {
                                        hasEdge = true;
                                        break;
                                    }
                                }
                            }
                            if( !hasEdge )
                            {
                                if( sr.getRole().equals(SpecieReference.PRODUCT) )
                                    AddElementsUtils.addEdge(reactionNode, node, sr.getRole(), diagram, converters);
                                else
                                    AddElementsUtils.addEdge(node, reactionNode, sr.getRole(), diagram, converters);
                            }
                        }
                    }
                }
                jobControl.setPreparedness(5 + i * stepPercent + j * stepPercent / numToAdd);
            }
        }
        jobControl.setPreparedness(95);
    }

    private List<Reaction> searchLinked(@Nonnull Base base)
    {
        if( stopList.contains(DataElementPath.create(base).toString()) )
            return null;

        DataElementPath path = Module.optModulePath(base);
        if(path == null)
        {
            log.warning( "Cannot find module for "+base.getCompletePath() );
            return null;
        }
        QueryEngine queryEngine = getQueryEngine( path );

        TargetOptions targetOptions = new TargetOptions(new CollectionRecord(path, true));
        SearchElement[] start = {new SearchElement(base)};
        QueryOptions queryOptions = new QueryOptions(1, BioHub.DIRECTION_BOTH);
        List<Reaction> reactions = new ArrayList<>();
        try
        {
            SearchElement[] result = queryEngine.searchLinked(start, queryOptions, targetOptions, null);
            for( SearchElement se : result )
            {
                if( se.getBase() instanceof Reaction )
                {
                    reactions.add((Reaction)se.getBase());
                    //TODO: process stop lists
                    if( reactions.size() > MAX_LINKED_REACTIONS )
                    {
                        stopList.add(DataElementPath.create(base).toString());
                        return null;
                    }
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Error extending "+base.getCompletePath()+": "+ExceptionRegistry.log( e ));
            return null;
        }
        return reactions;
    }

    private QueryEngine getQueryEngine(DataElementPath path)
    {
        if(queryEngines.containsKey( path ))
        {
            return queryEngines.get( path );
        }
        CollectionRecord cr = new CollectionRecord(path, true);
        TargetOptions targetOptions = new TargetOptions(cr);
        Map<String, QueryEngine> engines = QueryEngineRegistry.lookForQueryEngines(cr, GraphSearchOptions.TYPE_NEIGHBOURS);
        QueryEngine queryEngine = null;
        if( engines != null && !engines.isEmpty() )
        {
            queryEngine = QueryEngineRegistry.lookForQueryEngine(targetOptions, GraphSearchOptions.TYPE_NEIGHBOURS);
        }
        if( queryEngine == null )
        {
            log.log(Level.SEVERE, "Cannot find QueryEngine for module " + path);
            return null;
        }
        // Cannot use computeIfAbsent as null values are possible here
        queryEngines.put( path, queryEngine );
        return queryEngine;
    }

    private Reaction[] findReactions(Compartment compartment) throws Exception
    {
        Set<Reaction> reactions = new HashSet<>();
        for( DiagramElement de : compartment )
        {
            Base kernel = de.getKernel();
            if( de instanceof Node && kernel != null && ! ( kernel instanceof Reaction ) && !kernel.getType().equals(Type.TYPE_COMPARTMENT)
                    && !kernel.getType().equals(Type.TYPE_REACTION) )
            {
                if(kernel.getOrigin() == null) {
                    Object path = kernel.getAttributes().getValue( Util.ORIGINAL_PATH );
                    if(path instanceof String) {
                        Base origKernel = DataElementPath.create((String)path).optDataElement( Base.class );
                        if(origKernel != null) {
                            kernel = origKernel;
                        }
                    }
                }
                List<Reaction> reactionElements = searchLinked(kernel);
                if( reactionElements == null )
                    continue;
                Iterator<Reaction> iter = reactionElements.iterator();
                while( iter.hasNext() )
                {
                    Reaction rde = iter.next();
                    String reactionName = rde.getName();
                    if( !compartment.contains(reactionName) ) //reaction was not present on diagram
                    {
                        SpecieReference[] srefs = ( rde ).getSpecieReferences();
                        if( StreamEx.of( srefs ).map( SpecieReference::getSpecieName ).without( kernel.getName() )
                                .anyMatch( compartment::contains ) )
                            reactions.add( rde );
                    }
                }
            }
            else if( kernel instanceof Reaction )
            {
                reactions.add((Reaction)kernel);
            }
            else if( kernel instanceof Stub && kernel.getType().equals(Type.TYPE_REACTION) )
            {
                Object reactionNameObj = de.getAttributes().getValue(DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY);
                if( reactionNameObj != null )
                {
                    DataElement reactionDe = CollectionFactory.getDataElement(reactionNameObj.toString());
                    if( reactionDe instanceof Reaction )
                    {
                        reactions.add((Reaction)reactionDe);
                    }
                }
            }
        }
        return reactions.toArray(new Reaction[reactions.size()]);
    }

    private DataElement getSpecieDataElement(SpecieReference sr)
    {
        DataElement de = null;
        try
        {
            String relativeName = sr.getSpecie(); //this is relative to module name
            DataCollection<?> origin = sr.getOrigin(); //reaction data element
            Module module = Module.getModule(origin);
            de = CollectionFactory.getDataElement(relativeName, module);
        }
        catch( Exception e )
        {
        }
        return de;
    }
}
