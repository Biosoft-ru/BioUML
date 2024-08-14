package biouml.workbench.diagram;

import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.gui.GUI;
import biouml.model.Module;
import biouml.workbench.BioUMLApplication;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;

@SuppressWarnings ( "serial" )
public class NewDiagramAction extends AbstractAction
{
    protected static final Logger log = Logger.getLogger(NewDiagramAction.class.getName());

    public static final String KEY = "New diagram";

    public static final String COLLECTION = "Collection";

    public NewDiagramAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        DataCollection parent = (DataCollection)getValue(COLLECTION);
        NewDiagramAction.newDiagram(parent);
        putValue(COLLECTION, null);
    }

    public static void newDiagram(DataCollection<?> parent)
    {
        ApplicationFrame frame = Application.getApplicationFrame();
        try
        {
            String dialogTilte = BioUMLApplication.getMessageBundle().getResourceString( "NEW_DIAGRAM_DIALOG_TITLE" );
            List<ru.biosoft.access.core.DataElementPath> databases = null;
            if( parent == null )
                databases = NewDiagramDialog.getAvailableDatabases();
            else
            {
                databases = new ArrayList<>();
                databases.add( Module.getModulePath( parent ) );
            }
    
            if( databases.size() == 0 )
            {
                JOptionPane.showMessageDialog(frame, BioUMLApplication.getMessageBundle().getResourceString("NEW_DIAGRAM_NO_DATABASES"),
                        dialogTilte, JOptionPane.OK_OPTION);
            }
            else
            {
                NewDiagramDialog dialog = new NewDiagramDialog(frame, dialogTilte, databases, parent);
                if( dialog.doModal() )
                {
                    dialog.getDiagramCompletePath().save(dialog.getDiagram());
                    DocumentManager.getDocumentManager().openDocument(dialog.getDiagram());
                    GUI.getManager().getRepositoryTabs().selectElement( dialog.getDiagram().getCompletePath() );
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Error during creating new diagram", e);
        }
    }
}
