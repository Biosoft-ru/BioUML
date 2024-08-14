package ru.biosoft.table.access;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import ru.biosoft.table.MessageBundle;

import com.developmentontheedge.application.Application;

@SuppressWarnings ( "serial" )
public class RemoveTableDocumentAction extends AbstractAction
{
    protected Logger log = Logger.getLogger(RemoveTableDocumentAction.class.getName());
    protected MessageBundle messageBandle = new MessageBundle();

    public static final String KEY = "Remove table data collection";
    public static final String DATA_ELEMENT = "Table data collection";

    public RemoveTableDocumentAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        DataElement de = (DataElement)getValue(DATA_ELEMENT);
        DataCollection<?> dc = de.getOrigin();

        String message = messageBandle.getResourceString("CONFIRM_REMOVE_ELEMENT");
        message = MessageFormat.format(message, new Object[] {de.getName(), dc.getCompletePath()});
        int res = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message);
        if( res != JOptionPane.YES_OPTION )
            return;
        try
        {
            dc.remove(de.getName());

            Document document = Document.getActiveDocument();

            if( document == null )
                return;

            document.getUndoManager().discardAllEdits();
            for( Document doc : GUI.getManager().getDocuments() )
            {
                if( doc.getModel().equals(de) )
                {
                    GUI.getManager().removeDocument( doc );
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Remove table document error", t);
        }
    }
}
