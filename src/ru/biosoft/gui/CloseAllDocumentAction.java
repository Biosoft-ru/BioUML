package ru.biosoft.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.gui.Document;

@SuppressWarnings ( "serial" )
public class CloseAllDocumentAction extends AbstractAction
{
    protected static final Logger log = Logger.getLogger(CloseAllDocumentAction.class.getName());

    public static final String KEY = "Close all diagrams";

    public CloseAllDocumentAction(boolean enabled)
    {
        super(KEY);
        setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        DocumentViewAccessProvider app = DocumentManager.getDocumentViewAccessProvider();
        try
        {
            for( Document doc : GUI.getManager().getDocuments() )
            {
                if( app.askSaveConfirmation(doc) )
                {
                    if( app.saveDocumentCurrentApplicationConfirmDialog(doc, doc.getDisplayName()) )
                    {
                        app.stateChanged(null);
                    }
                    else
                    {
                        continue;
                    }
                }
                GUI.getManager().removeDocument(doc);
            }
        }
        catch( Throwable t )
        {
            t.printStackTrace();
            log.log(Level.SEVERE, "Closing diagram error", t);
        }
        finally
        {
            app.enableDocumentActions(!GUI.getManager().getDocuments().isEmpty());
        }
    }
}
