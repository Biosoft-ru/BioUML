package biouml.workbench.diagram;

import java.awt.event.ActionEvent;
import java.lang.reflect.Method;

import javax.swing.AbstractAction;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathDialog;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import ru.biosoft.table.TableDataCollection;
import biouml.model.Diagram;

public class SaveAsDocumentAction extends AbstractAction
{
    protected static final Logger log = Logger.getLogger("biouml.workbench.SaveAsDocumentAction");
    public static final String KEY = "Save document as";

    public SaveAsDocumentAction(boolean enabled)
    {
        super(KEY);
        setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        try
        {
            Document activeDocument = Document.getActiveDocument();
            if(activeDocument == null) return;
            DataElement element = (DataElement) activeDocument.getModel();
            Method cloneMethod = null;
            try
            {
                cloneMethod = element.getClass().getMethod("clone", DataCollection.class, String.class);
            }
            catch( NoSuchMethodException e1 )
            {
            }
            if( cloneMethod == null )
            {
                log.info("Document " + activeDocument.getDisplayName() + " does not support copying.");
                return;
            }

            DataElementPathDialog dialog = new DataElementPathDialog();

            //dirty hack
            if( element instanceof TableDataCollection )
                dialog.setElementClass(TableDataCollection.class);
            else if( element instanceof Diagram )
                dialog.setElementClass(Diagram.class);
            else
                dialog.setElementClass(element.getClass());
            
            //dirty hack - 2 : new elements can have null origin
            DataElementPath dePath  = null;
            try
            {
                dePath = DataElementPath.create(element);
            }
            catch(Exception pathException)
            {
            }
            dialog.setValue(dePath);
            dialog.setPromptOverwrite(true);
            if( dialog.doModal() )
            {
                DataCollection origin = dialog.getValue().getParentPath().getDataCollection();
                Object clonedElement = cloneMethod.invoke(element, origin, dialog.getValue().getName());
                origin.put((DataElement)clonedElement);
                if(clonedElement instanceof DataCollection)
                {
                    ((DataCollection)clonedElement).close();
                }
                origin.release(((DataElement)clonedElement).getName());
                GUI.getManager().getRepositoryTabs().selectElement( ( (DataElement)clonedElement ).getCompletePath() );
            }

        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Saving document error", t);
        }
    }
}
