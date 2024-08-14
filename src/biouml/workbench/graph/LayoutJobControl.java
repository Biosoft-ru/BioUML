package biouml.workbench.graph;

import ru.biosoft.graph.Graph;
import ru.biosoft.graph.Layouter;

import biouml.model.Diagram;
import biouml.workbench.graph.DiagramToGraphTransformer;

import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControlException;

/**
 * Layouter job control
 */
public class LayoutJobControl extends AbstractJobControl
{
    protected Diagram diagram;
    protected Layouter layouter;
    protected Graph graph;

    public LayoutJobControl(Graph graph, Diagram diagram, Layouter layouter)
    {
        super(null);
        try
        {
        this.diagram = diagram.clone(diagram.getOrigin(), diagram.getName());
        }
        catch( Exception e )
        {
        }
        this.graph = graph;
        this.layouter = layouter;
    }

    public LayoutJobControl(Diagram diagram, Layouter layouter)
    {
        super(null);
        try
        {
            this.diagram = diagram.clone(diagram.getOrigin(), diagram.getName());
        }
        catch( Exception e )
        {
        }
        this.graph = DiagramToGraphTransformer.generateGraph(this.diagram, null);
        this.layouter = layouter;
    }

    @Override
    protected void doRun() throws JobControlException
    {
        fireJobStarted("Started");
        layouter.doLayout(graph, null);
        setCompleted();
        resultsAreReady();
    }

    public Graph getGraph()
    {
        return graph;
    }

    public Diagram getDiagram()
    {
        return diagram;
    }
}
