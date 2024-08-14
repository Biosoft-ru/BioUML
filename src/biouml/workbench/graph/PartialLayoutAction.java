package biouml.workbench.graph;

import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.LayoutJobControl;
import ru.biosoft.graph.LayoutJobControlImpl;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.gui.Document;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;

import ru.biosoft.jobcontrol.JobProgressBar;

/**
 * Layout diagram and show preview.
 */
@SuppressWarnings ( "serial" )
public class PartialLayoutAction extends AbstractAction
{
    protected Logger log = Logger.getLogger(ApplyLayoutAction.class.getName());

    public static final String KEY = "Prepare partial layout";

    public static final String DIAGRAM = "Diagram";
    public static final String LAYOUTER_VIEW_PART = "Layouter view part";

    public PartialLayoutAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        Diagram diagram = (Diagram)getValue( PartialLayoutAction.DIAGRAM );
        Document document = Document.getActiveDocument();
        if(document == null) return;
        List<DataElement> selectedElements = document.getSelectedItems();
        if (selectedElements.isEmpty())
            return;
        List<DiagramElement> elementsToLayout = new ArrayList<>();
        if( diagram == null )
        {
            log.log(Level.SEVERE, "diagram is undefined");
            return;
        }
        
       
        for (DataElement de: selectedElements)
        {
            if ((de instanceof DiagramElement) && Diagram.getDiagram((DiagramElement)de).equals(diagram))
            {
                elementsToLayout.add( (DiagramElement )de);
                if (de instanceof Node)
                {
                    ((Node)de).edges().forEach( elementsToLayout::add );
                }
            }
        }
//        DataCollection origin = diagram.getOrigin();
//        if( origin instanceof Diagram )
//        {
//            DiagramUtility.setBaseOrigin(diagram);
            layoutDiagram(diagram, elementsToLayout);
//            diagram.setOrigin(origin);
//        }
//        else
//        {
//            layoutDiagram(diagram, elementsToLayout);
//        }
    }

    private void layoutDiagram(Diagram diagram, List<DiagramElement> elements)
    {
        LayouterViewPart layouterViewPart = (LayouterViewPart)getValue(PartialLayoutAction.LAYOUTER_VIEW_PART);
        JobProgressBar jpb = new JobProgressBar();

        try
        {

            setFixed(diagram, true);
            for (DiagramElement de: elements)
            {
                if (de instanceof Node)
                {
                    ((Node)de).setFixed( false );
                }
            }
            
            Diagram cloneDiagram = diagram.clone(diagram.getOrigin(), diagram.getName());

            setFixed(diagram, false);
            
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
    
    private void setFixed(Compartment compartment, boolean isFixed)
    {
        compartment.recursiveStream().select( Node.class ).forEach( n -> n.setFixed( isFixed ) );
    }
}
