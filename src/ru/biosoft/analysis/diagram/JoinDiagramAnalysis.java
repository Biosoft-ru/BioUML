package ru.biosoft.analysis.diagram;

import java.util.Iterator;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.LayoutJobControl;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.plugins.graph.GraphPlugin;
import ru.biosoft.plugins.graph.LayouterDescriptor;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.workbench.graph.DiagramToGraphTransformer;

/**
 * Join diagrams of the same type
 * @author anna
 */
@ClassIcon ( "resources/join-diagrams.gif" )
public class JoinDiagramAnalysis extends AnalysisMethodSupport<JoinDiagramParameters>
{
    public JoinDiagramAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new JoinDiagramParameters());
    }

    @Override
    protected AnalysisJobControl createJobControl()
    {
        return new JoinDiagramJobControl();
    }

    @Override
    public JoinDiagramJobControl getJobControl()
    {
        return (JoinDiagramJobControl)jobControl;
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        JoinDiagramParameters parameters = getParameters();
        checkPaths();
        DataElementPathSet inputs = parameters.getInputDiagrams();
        Iterator<ru.biosoft.access.core.DataElementPath> iter = inputs.iterator();
        String prevType = null;
        while( iter.hasNext() )
        {
            DataElementPath path = iter.next();
            Diagram de = path.getDataElement(Diagram.class);
            DiagramType type = de.getType();
            if( prevType == null )
                prevType = type.getClass().getName();
            else if( !prevType.equals(type.getClass().getName()) )
                throw new IllegalArgumentException("Can not join diagrams of different types");
        }
    }

    /**
     * main analysis method
     * @throws Exception
     */
    @Override
    public Diagram justAnalyzeAndPut() throws Exception
    {
        log.info("Join analysis started");
        validateParameters();
        jobControl.setPreparedness(1);
        JoinDiagramParameters parameters = getParameters();
        DataElementPath outputPath = parameters.getOutputDiagramPath();
        DataElementPathSet inputs = parameters.getInputDiagrams();
        int total = inputs.size();
        Iterator<ru.biosoft.access.core.DataElementPath> iter = inputs.iterator();
        Diagram first = iter.next().getDataElement(Diagram.class);
        Diagram diagram = first.clone(outputPath.optParentCollection(), outputPath.getName());
        jobControl.setPreparedness(1 + 74 / total);

        SemanticController sc = diagram.getType().getSemanticController();

        int cnt = 1;
        while( iter.hasNext() )
        {
            DataElementPath path = iter.next();
            Diagram next = path.getDataElement(Diagram.class);
            for(Node node : next.stream( Node.class ))
            {
                if( !diagram.contains(node.getName()) )
                {
                    DiagramElement newDe = node.clone(diagram, node.getName());
                    diagram.put(newDe);
                }
            }
            for(Edge edge : next.stream( Edge.class ))
            {
                Edge newDe = edge.clone( diagram, edge.getName() );
                if( sc.findEdge( newDe.getInput(), newDe.getOutput(), edge.getKernel() ) == null )
                {
                    if( newDe.getKernel() instanceof SpecieReference )
                    {
                        if( newDe.getInput().getKernel() instanceof Reaction )
                            ( (Reaction)newDe.getInput().getKernel() ).put( (SpecieReference)newDe.getKernel() );
                        else if( newDe.getOutput().getKernel() instanceof Reaction )
                            ( (Reaction)newDe.getOutput().getKernel() ).put( (SpecieReference)newDe.getKernel() );
                    }
                    diagram.put(newDe);
                }

            }
            jobControl.setPreparedness(1 + 74 * ( ++cnt ) / total);
        }
        if(jobControl.isStopped())
            return null;

        String name = parameters.getLayouterName();
        if( !name.equals(JoinDiagramParameters.NONE_LAYOUTER) )
        {
            log.info("Layouting diagram");
            try
            {
                Layouter layouter = null;
                LayouterDescriptor ld = GraphPlugin.getLayouter(name);
                if( ld != null )
                    layouter = ld.createLayouter();
                if(layouter == null)
                {
                    if(diagram.getSize() < 1000)
                        layouter = new HierarchicLayouter();
                    else
                        layouter = new ForceDirectedLayouter();
                }
                Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
                PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
                int numberOfEstimatedOperations = pathwayLayouter.estimate(graph, 0);
                jobControl.pushProgress(75, 95);
                getJobControl().setLayout(true);
                getJobControl().setNumberOfEstimatedOperations(numberOfEstimatedOperations);
                pathwayLayouter.doLayout(graph, getJobControl());
                getJobControl().setLayout(false);
                jobControl.popProgress();
                DiagramToGraphTransformer.applyLayout(graph, diagram);
            }
            catch( Exception e )
            {
                log.warning("Error while layouting: "+ExceptionRegistry.log(e));
            }
        }

        outputPath.save(diagram);
        jobControl.setPreparedness(100);
        log.info("Diagram " + diagram.getName() + " was successfully created.");
        return diagram;
    }
    

    public class JoinDiagramJobControl extends AnalysisJobControl implements LayoutJobControl
    {
        private int numberOfEstimatedOperations;
        private boolean isLayout = false;

        public JoinDiagramJobControl()
        {
            super(JoinDiagramAnalysis.this);
        }

        @Override
        public void done(int operationsDone)
        {
            double progressStep = 100d / numberOfEstimatedOperations;
            if( progressStep * operationsDone < 100 )
            {
                this.setPreparedness((int) ( progressStep * operationsDone + 0.5 ));
            }
            else
            {
                this.setPreparedness(100);
            }
        }

        @Override
        public int getNumberOfEstimatedOperations()
        {
            return numberOfEstimatedOperations;
        }

        @Override
        public void setNumberOfEstimatedOperations(int estimatedOperations)
        {
            numberOfEstimatedOperations = estimatedOperations;
        }

        public void setLayout(boolean layout)
        {
            isLayout = layout;
        }

        @Override
        public void terminate()
        {
            if( !isLayout )
            {
                super.terminate();
            }
        }
    }
}
