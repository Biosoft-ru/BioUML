package biouml.workbench.graph;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.LayoutJobControlImpl;
import ru.biosoft.graph.LayoutJobControl;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Diagram;
import biouml.standard.diagram.DiagramUtility;
import biouml.workbench.diagram.CompositeDiagramDocument;

/**
 * Apply layout action. By default apply layout for all
 * selected (and only selected) nodes
 */
@SuppressWarnings ( "serial" )
public class ApplyLayoutAction extends AbstractAction
{
    protected Logger log = Logger.getLogger( ApplyLayoutAction.class.getName() );

    public static final String KEY = "ReApply layout";

    public static final String DIAGRAM = "Diagram";
    public static final String VIEW_PANE = "View pane";
    public static final String LAYOUTER_VIEW_PART = "Layouter view part";

    public ApplyLayoutAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        Diagram diagram = (Diagram)getValue(ApplyLayoutAction.DIAGRAM);
        if( diagram == null )
        {
            log.log(Level.SEVERE, "diagram is undefined");
            return;
        }

        DataCollection<?> origin = diagram.getOrigin();
        if( origin instanceof Diagram )
        {
            DiagramUtility.setBaseOrigin(diagram);
            applyLayout(diagram);
            diagram.setOrigin(origin);
        }
        else
        {
            applyLayout(diagram);
        }

    }

    private void applyLayout(Diagram diagram)
    {
        LayouterViewPart layouterViewPart = (LayouterViewPart)getValue(PrepareLayoutAction.LAYOUTER_VIEW_PART);
        Layouter layouter = layouterViewPart.getCurrentLayouter();

        boolean notificationEnabled = diagram.isNotificationEnabled();
        diagram.setNotificationEnabled(false);
        diagram.setPathLayouter(layouter);
        diagram.setNotificationEnabled(notificationEnabled);

        Object viewPane = getValue(ApplyLayoutAction.VIEW_PANE);

        if( viewPane == null )
            return;

        if( viewPane instanceof CompositeDiagramDocument )
        {
            viewPane = ( (CompositeDiagramDocument)viewPane ).getDiagramViewPane();
        }
        else if( ! ( viewPane instanceof ViewEditorPane ) )
            return;

        try
        {
            Graph currentGraph = layouterViewPart.getCurrentGraph();
            Graph graph = DiagramToGraphTransformer.generateGraph(diagram, diagram.getType().getSemanticController().getFilter());

            if( currentGraph != null && graph != null )
            {
                boolean extraNodes = DiagramToGraphTransformer.reApplyLayout(currentGraph, graph);
                if( !extraNodes )
                {
                    ( (ViewEditorPane)viewPane ).startTransaction("Layout");
                    DiagramToGraphTransformer.applyLayout(graph, diagram);
                }
                else
                {
                    DiagramToGraphTransformer.applyLayout(graph, diagram);
                    PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
                    LayoutJobControl jobControl = new LayoutJobControlImpl(pathwayLayouter.estimate(graph, 0));
                    jobControl.addListener(layouterViewPart);
                    layouterViewPart.setCurrentGraph(graph);
                    layouterViewPart.setCurrentDiagram(diagram);
                    pathwayLayouter.doLayout(graph, jobControl);
                    ( (ViewEditorPane)viewPane ).startTransaction("Layout");
                    DiagramToGraphTransformer.applyLayout(graph, diagram);
                }
            }
            else
            {
                log.log(Level.SEVERE, "Graph is not ready");
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Error at applying layout", e);
        }
        ( (ViewEditorPane)viewPane ).completeTransaction();
        layouterViewPart.updateUI();
        ( (ViewEditorPane)viewPane ).updateUI();
    }
}
