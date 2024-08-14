package biouml.plugins.optimization.diagram;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.research.research.ResearchDiagramType;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.DirectedConnection;
import biouml.standard.type.Type;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.HierarchicLayouter;

public class OptimizationDiagramManager
{
    protected Optimization optimization;
    protected HierarchicLayouter layouter;

    public static final String EXPERIMENT_ID = "experiment_";
    public static final String STATE_ID = "state_";
    public static final String PAR_ID = "par_";
    public static final String METHOD_ID = "method_";

    protected static final Logger log = Logger.getLogger(OptimizationDiagramManager.class.getName());

    public OptimizationDiagramManager(Optimization optimization)
    {
        this.optimization = optimization;
        layouter = new HierarchicLayouter();
        layouter.setVerticalOrientation(true);
        layouter.setLayerDeltaY(40);
    }

    private boolean isChanged;
    public boolean isDiagramChanged()
    {
        Diagram diagram = optimization.getOptimizationDiagram();
        isChanged = false;
        try
        {
            if( diagram == null )
            {
                initDiagram();
                return false;
            }
            String deName = optimization.getName();
            if( !diagram.contains("method_" + deName) )
            {
                //changeDiagram(diagram);
                return true;
            }
            if( checkExperiments(diagram) || checkModel(diagram) )
            {
                //changeDiagram(diagram);
                return true;
            }

        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not check diagram " + (diagram == null ? "" : diagram.getName()));
        }
        return false;
    }

    public void setChanged(boolean isChanged)
    {
        this.isChanged = isChanged;
    }

    protected boolean checkExperiments(Diagram diagram) throws Exception
    {
        List<String> experimentNodes = getDiagramNodes(diagram, EXPERIMENT_ID);

        List<OptimizationExperiment> experiments = optimization.getParameters().getOptimizationExperiments();
        if( experiments.size() != experimentNodes.size() )
        {
            //changeDiagram(diagram);
            return true;
        }
        for( OptimizationExperiment exp : experiments )
        {
            String deName = EXPERIMENT_ID + exp.getName();
            if( !experimentNodes.contains(deName) )
            {
                //changeDiagram(diagram);
                return true;
            }
        }
        return false;
    }

    protected boolean checkModel(Diagram diagram) throws Exception
    {
        List<String> stateNodes = getDiagramNodes(diagram, STATE_ID);
        List<String> parNodes = getDiagramNodes(diagram, PAR_ID);

        OptimizationParameters parameters = optimization.getParameters();

        String[] paths = parameters.getStatePaths();
        if( paths.length != stateNodes.size() )
        {
            //changeDiagram(diagram);
            return true;
        }
        int srNumber = 0;
        for( String path : paths )
        {
            DataElement state = CollectionFactory.getDataElement(path);
            if( state != null )
            {
                String deName = STATE_ID + state.getName();
                if( !stateNodes.contains(deName) )
                {
                    //changeDiagram(diagram);
                    return true;
                }

                String[] results = parameters.getResults(DataElementPath.create(state).toString());
                srNumber += results.length;
                for( String resultPath : results )
                {
                    DataElement parElement = CollectionFactory.getDataElement(resultPath);
                    if( parElement != null )
                    {
                        deName = PAR_ID + parElement.getName();
                        if( !parNodes.contains(deName) )
                        {
                            //changeDiagram(diagram);
                            return true;
                        }
                    }
                }
            }
        }
        if( srNumber != parNodes.size() )
        {
            //changeDiagram(diagram);
            return true;
        }
        return false;
    }

    public void changeDiagram(boolean isNewDiagram) throws Exception
    {
        if( isChanged )
            return;

        if( isNewDiagram )
        {
            DataCollection diagramsDataCollection = optimization.getOptimizationDiagramPath().getParentCollection();

            String oldName = optimization.getOptimizationDiagramPath().getName();
            oldName = oldName.replaceAll("_\\d+$", "");
            int ind = 0;
            while( diagramsDataCollection.contains(oldName + "_" + ind) )
            {
                ind++;
            }
            String newName = oldName + "_" + ind;
            optimization.setOptimizationDiagramPathQuiet(DataElementPath.create(diagramsDataCollection, newName));
            CollectionFactoryUtils.save(optimization);
        }

        initDiagram();
        isChanged = true;
    }

    protected Diagram initDiagram() throws Exception
    {
        Diagram diagram = optimization.getOptimizationDiagram();
        try
        {
            DataElementPath optimizationDiagramPath = optimization.getOptimizationDiagramPath();
            DataCollection diagramsDataCollection = optimizationDiagramPath.getParentCollection();

            if( diagram == null )
            {
                diagram = new Diagram(diagramsDataCollection, new DiagramInfo(optimizationDiagramPath.getName()), new ResearchDiagramType());
            }
            else
            {
                diagram.clear();
            }

            Node optimizationNode = new Node(diagram, METHOD_ID + optimization.getName(), optimization);
            optimizationNode.setTitle(optimization.getTitle());
            diagram.put(optimizationNode);

            Diagram d = optimization.getDiagram();
            Node diagramNode = new Node(diagram, d.getName(), new Stub(null, d.getName(), Type.DIAGRAM_INFO));
            diagramNode.getAttributes().add(new DynamicProperty(DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY, String.class, d.getCompletePath().toString()));
            diagram.put(diagramNode);

            Edge edge = new Edge(diagram, new DirectedConnection(diagram, diagramNode.getName() + " to " + optimizationNode.getName()),
                    diagramNode, optimizationNode);
            optimizationNode.addEdge(edge);
            diagramNode.addEdge(edge);
            diagram.put(edge);
            refreshNodes(diagram);
            diagramsDataCollection.put(diagram);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not initiate diagram for the optimization " + optimization.getName());
        }
        return diagram;
    }

    public void refreshNodes(Diagram diagram) throws Exception
    {
        fixNodes(diagram);
        refreshExperiments(diagram);
        refreshMethod(diagram);
        layout(diagram);
    }

    protected void refreshExperiments(Diagram diagram) throws Exception
    {
        List<String> experimentNodes = getDiagramNodes(diagram, EXPERIMENT_ID);

        for( OptimizationExperiment experiment : optimization.getParameters().getOptimizationExperiments() )
        {
            String deName = EXPERIMENT_ID + experiment.getName();
            if( experimentNodes.contains(deName) )
            {
                experimentNodes.remove(deName);
            }
            else
            {
                //add experiment node
                addExperiment(experiment, experiment.getTitle(), diagram);
            }
        }
        for( String name : experimentNodes )
        {
            removeNode(name, diagram);
        }
    }

    protected void addExperiment(OptimizationExperiment experiment, String title, Diagram diagram) throws Exception
    {
        String name = EXPERIMENT_ID + experiment.getName();
        Node expNode = new Node(diagram, name, new Stub(null, experiment.getName(), Type.TYPE_EXPERIMENT));

        if( experiment.getTableSupport().getTable() != null )
        {
            expNode.getAttributes().add(
                    new DynamicProperty(DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY, String.class, experiment.getTableSupport().getTable()
                            .getCompletePath().toString()));
            expNode.setTitle(title);
            diagram.put(expNode);

            Node optimizationNode = getOptimizationNode(diagram);
            if( optimizationNode != null )
            {
                Edge edge = new Edge(diagram, new DirectedConnection(diagram, expNode.getName() + " to " + optimizationNode.getName()),
                        expNode, optimizationNode);
                optimizationNode.addEdge(edge);
                expNode.addEdge(edge);
                diagram.put(edge);
            }
        }
    }

    protected void refreshMethod(Diagram diagram) throws Exception
    {
        List<String> stateNodes = getDiagramNodes(diagram, STATE_ID);

        OptimizationParameters parameters = optimization.getParameters();
        for( String path : parameters.getStatePaths() )
        {
            DataElement state = CollectionFactory.getDataElement(path);
            if( state != null )
            {
                String deName = STATE_ID + state.getName();
                Node stateNode = null;
                if( stateNodes.contains(deName) )
                {
                    stateNode = (Node)diagram.get(deName);
                    stateNodes.remove(deName);
                }
                else
                {
                    //add state node
                    stateNode = addStateNode(diagram, deName, state, parameters);
                }

                if( stateNode != null )
                    refreshResultNodes(diagram, state, stateNode, parameters);
            }
        }
        for( String name : stateNodes )
        {
            removeNode(name, diagram);
        }
    }

    protected void refreshResultNodes(Diagram diagram, DataElement state, Node stateNode, OptimizationParameters parameters)
            throws Exception
    {
        List<String> resultNodes = getDiagramNodes(diagram, PAR_ID);

        String[] results = parameters.getResults(DataElementPath.create(state).toString());
        if( results != null )
        {
            for( String resultPath : results )
            {
                DataElement parElement = CollectionFactory.getDataElement(resultPath);
                if( parElement != null && !resultNodes.contains(PAR_ID + parElement.getName()) )
                {
                    Node parNode = new Node(diagram, new Stub(diagram, PAR_ID + parElement.getName(), Type.TYPE_SIMULATION_RESULT));
                    parNode.getAttributes().add(new DynamicProperty(DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY, String.class, resultPath));
                    parNode.setTitle(parElement.getName());
                    diagram.put(parNode);

                    Edge edge = new Edge(diagram, new DirectedConnection(diagram, stateNode.getName() + " to " + parNode.getName()),
                            stateNode, parNode);
                    parNode.addEdge(edge);
                    stateNode.addEdge(edge);
                    diagram.put(edge);
                }
            }
        }
    }

    protected Node addStateNode(Diagram diagram, String deName, DataElement state, OptimizationParameters parameters) throws Exception
    {
        Node stateNode = new Node(diagram, new Stub(diagram, deName, Type.ANALYSIS_TABLE));

        stateNode.getAttributes().add(
                new DynamicProperty(DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY, String.class, DataElementPath.create(state).toString()));
        stateNode.setTitle(state.getName());
        diagram.put(stateNode);

        Node optimizationNode = getOptimizationNode(diagram);
        if( optimizationNode != null )
        {
            Edge edge = new Edge(diagram, new DirectedConnection(diagram, optimizationNode.getName() + " to " + stateNode.getName()),
                    optimizationNode, stateNode);
            optimizationNode.addEdge(edge);
            stateNode.addEdge(edge);
            diagram.put(edge);
        }

        return stateNode;
    }

    protected @Nonnull List<String> getDiagramNodes(Diagram diagram, String id) throws Exception
    {
        return diagram.stream( Node.class ).map( Node::getName ).filter( name -> name.startsWith( id ) ).toList();
    }

    protected Node getOptimizationNode(Diagram diagram) throws Exception
    {
        return diagram.stream( Node.class ).findFirst( node -> node.getName().startsWith( METHOD_ID ) ).orElse( null );
    }

    protected void removeNode(String experimentName, Diagram diagram) throws Exception
    {
        Edge[] edges = ( (Node)diagram.get(experimentName) ).getEdges();
        for( Edge edge : edges )
            diagram.remove(edge.getName());
        diagram.remove(experimentName);
    }

    protected void fixNodes(Diagram diagram) throws Exception
    {
        diagram.stream( Node.class ).forEach( node -> node.setFixed( true ) );
    }

    protected void layout(Diagram diagram)
    {
        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        layouter.doLayout(graph, null);

        Iterator<ru.biosoft.graph.Node> it = graph.nodeIterator();
        while( it.hasNext() )
        {
            it.next().fixed = false;
        }
        graph.move(40, 40);

        DiagramToGraphTransformer.applyLayout(graph, diagram);
    }
}
