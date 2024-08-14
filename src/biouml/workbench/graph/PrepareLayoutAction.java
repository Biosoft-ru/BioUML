package biouml.workbench.graph;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.LayoutJobControl;
import ru.biosoft.graph.LayoutJobControlImpl;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.PathwayLayouter;
import biouml.model.Diagram;
import biouml.standard.diagram.DiagramUtility;
import ru.biosoft.jobcontrol.JobProgressBar;

/**
 * Layout diagram and show preview.
 */
@SuppressWarnings ( "serial" )
public class PrepareLayoutAction extends AbstractAction
{
    protected Logger log = Logger.getLogger(ApplyLayoutAction.class.getName());

    public static final String KEY = "Prepare layout";

    public static final String DIAGRAM = "Diagram";
    public static final String LAYOUTER_VIEW_PART = "Layouter view part";

    public PrepareLayoutAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        Diagram diagram = (Diagram)getValue(PrepareLayoutAction.DIAGRAM);

        if( diagram == null )
        {
            log.log(Level.SEVERE, "diagram is undefined");
            return;
        }
        DataCollection<?> origin = diagram.getOrigin();
        if( origin instanceof Diagram )
        {
            DiagramUtility.setBaseOrigin(diagram);
            layoutDiagram(diagram);
            diagram.setOrigin(origin);
        }
        else
        {
            layoutDiagram(diagram);
        }
    }

    private void layoutDiagram(Diagram diagram)
    {
        LayouterViewPart layouterViewPart = (LayouterViewPart)getValue(PrepareLayoutAction.LAYOUTER_VIEW_PART);
        JobProgressBar jpb = new JobProgressBar();

        try
        {
            Diagram cloneDiagram = diagram.clone(diagram.getOrigin(), diagram.getName());

            Layouter layouter = layouterViewPart.getCurrentLayouter();
            if( layouter == null )
            {
                layouter = diagram.getPathLayouter();
            }
            if( layouter == null )
            {
                log.log(Level.SEVERE, "layouter is undefined");
                return;
            }

            Graph graph = DiagramToGraphTransformer.generateGraph(cloneDiagram, diagram.getType().getSemanticController().getFilter());
            PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);

            LayoutJobControlImpl jobControl = new LayoutJobControlImpl(pathwayLayouter.estimate(graph, 0));
            jobControl.addListener(layouterViewPart);
            jobControl.addListener(jpb);

            layouterViewPart.setCurrentDiagram(cloneDiagram);
            layouterViewPart.setCurrentGraph(graph);
            layouterViewPart.setJobControl(jobControl);
            layouterViewPart.setProgresBar(jpb);

            LayoutThread t = new LayoutThread(pathwayLayouter, graph, jobControl);
            t.start();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Error at relayout", e);
        }
    }


    static class LayoutThread extends Thread
    {

        PathwayLayouter layouter;
        Graph graph;
        LayoutJobControl jobControl;

        LayoutThread(PathwayLayouter pathwayLayouter, Graph graph, LayoutJobControl jobControl)
        {
            this.layouter = pathwayLayouter;
            this.graph = graph;
            this.jobControl = jobControl;
        }
        @Override
        public void run()
        {
            layouter.doLayout(graph, jobControl);
        }
    }
}
