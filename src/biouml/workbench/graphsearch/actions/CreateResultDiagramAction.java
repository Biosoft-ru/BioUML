package biouml.workbench.graphsearch.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;

import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.DiagramInfo;
import biouml.workbench.diagram.DiagramDocument;
import biouml.workbench.graphsearch.GraphSearchViewPart;
import biouml.workbench.graphsearch.MessageBundle;

/**
 * Create new diagram for graph search results
 */
@SuppressWarnings ( "serial" )
public class CreateResultDiagramAction extends AbstractAction
{
    protected static final Logger log = Logger.getLogger(CreateResultDiagramAction.class.getName());

    public static final String KEY = "Create diagram action";
    public static final String SEARCH_PANE = "SearchPane";

    protected MessageBundle messages = new MessageBundle();

    public CreateResultDiagramAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        GraphSearchViewPart searchPane = (GraphSearchViewPart)getValue(SEARCH_PANE);
        if( searchPane == null )
        {
            log.log(Level.SEVERE, "Search view part is undefined");
            return;
        }

        DiagramDocument document = createNewDiagramDocument();
        searchPane.addDocumentToTargetSet(document);
    }

    public static DiagramDocument createNewDiagramDocument()
    {
        DiagramType diagramType = new PathwaySimulationDiagramType();
        try
        {
            Diagram diagram = new Diagram(null, new DiagramInfo(null, getNewDiagramName(GUI.getManager().getDocuments())), diagramType);
            DiagramDocument diagramDocument = new DiagramDocument(diagram);
            GUI.getManager().addDocument(diagramDocument);
            return diagramDocument;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not create new diagram", e);
        }
        return null;
    }

    /**
     * Generate unique diagram name
     */
    protected static String getNewDiagramName(Collection<Document> documents)
    {
        String base = "New diagram ";
        int num = 0;
        String name = null;
        while( true )
        {
            num++;
            name = base + num;
            boolean exists = StreamEx.of( documents ).map( Document::getModel ).select( ru.biosoft.access.core.DataElement.class )
                    .map( ru.biosoft.access.core.DataElement::getName ).has( name );
            if( !exists )
            {
                break;
            }
        }
        return name;
    }
}
