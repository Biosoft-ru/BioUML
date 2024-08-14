package biouml.workbench.graphsearch.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.gui.Document;

import biouml.model.Diagram;
import biouml.model.util.AddElementsUtils;
import biouml.workbench.diagram.DiagramDocument;
import biouml.workbench.graphsearch.GraphSearchViewPart;
import biouml.workbench.graphsearch.MessageBundle;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

/**
 * Adds selected input and output elements to current active diagram document
 */
public class AddElementsToDiagramAction extends AbstractAction
{
    protected Logger log = Logger.getLogger( AddElementsToDiagramAction.class.getName() );

    public static final String KEY = "Add elements action";
    public static final String SEARCH_PANE = "SearchPane";

    protected MessageBundle messages = new MessageBundle();

    public AddElementsToDiagramAction()
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

        DiagramDocument document = null;

        Document currentDocument = Document.getActiveDocument();
        if( ! ( currentDocument instanceof DiagramDocument ) )
        {
            int status = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), messages
                    .getResourceString("CONFIRM_CREATE_DIAGRAM"), "", JOptionPane.YES_NO_OPTION);
            if( status == JOptionPane.OK_OPTION )
            {
                document = CreateResultDiagramAction.createNewDiagramDocument();
            }
        }
        else
        {
            if( searchPane.isDocumentInTargetSet((DiagramDocument)currentDocument) )
            {
                document = (DiagramDocument)currentDocument;
            }
            else
            {
                int messageType = JOptionPane.QUESTION_MESSAGE;
                String[] options = {"Use current", "New diagram", "Cancel"};
                int code = JOptionPane.showOptionDialog(Application.getApplicationFrame(), messages
                        .getResourceString("CONFIRM_USE_CURRENT_DIAGRAM"), "", 0, messageType, null, options, "New diagram");

                if( code == 0 )
                {
                    document = (DiagramDocument)currentDocument;
                    searchPane.addDocumentToTargetSet(document);
                }
                else if( code == 1 )
                {
                    document = CreateResultDiagramAction.createNewDiagramDocument();
                }
            }
        }

        if( document != null )
        {
            Diagram diagram = (Diagram)document.getModel();
            try
            {
                AddElementsUtils.addElements( diagram, searchPane.getAddElements(), null );
            }
            catch( Exception e )
            {
                String message = "Error adding elements: " + e.getMessage();
                log.log( Level.SEVERE, message );
                ApplicationUtils.errorBox( "Error", message );
            }
            document.update();
        }
    }
}
