package biouml.workbench.graph;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graph.Graph;
import biouml.model.Diagram;
import biouml.standard.diagram.DiagramUtility;

/**
 * Layout diagram and show preview.
 */
@SuppressWarnings ( "serial" )
public class SaveLayoutAction extends AbstractAction
{
    protected Logger log = Logger.getLogger(ApplyLayoutAction.class.getName());

    public static final String KEY = "Save layout";

    public static final String DIAGRAM = "Diagram";
    public static final String LAYOUTER_VIEW_PART = "Layouter view part";

    public SaveLayoutAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        Diagram diagram = (Diagram)getValue(SaveLayoutAction.DIAGRAM);
        if( diagram == null )
        {
            log.log(Level.SEVERE, "diagram is undefined");
            return;
        }
        
        DataCollection<?> origin = diagram.getOrigin();
        if( origin instanceof Diagram )
        {
            DiagramUtility.setBaseOrigin(diagram);
            saveLayout(diagram);
            diagram.setOrigin(origin);
        }
        else
        {
            saveLayout(diagram);
        }
       
    }
    
    private void saveLayout(Diagram diagram)
    {
        LayouterViewPart layouterViewPart = (LayouterViewPart)getValue(SaveLayoutAction.LAYOUTER_VIEW_PART);
        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, diagram.getType().getSemanticController().getFilter());
       
        layouterViewPart.setCurrentDiagram(diagram);
        layouterViewPart.setCurrentGraph(graph);
        layouterViewPart.resultsReady(null);
        layouterViewPart.updateUI();
    }
}
