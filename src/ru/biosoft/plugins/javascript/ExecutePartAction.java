package ru.biosoft.plugins.javascript;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.plugins.javascript.document.JSDocument;

@SuppressWarnings ( "serial" )
public class ExecutePartAction extends AbstractAction
{
    protected Logger log = Logger.getLogger(ExecuteAction.class.getName());

    public static final String KEY = "execute javascript part";
    public static final String DOCUMENT_ELEMENT = "document";

    public ExecutePartAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JSDocument jsDocument = (JSDocument)getValue(DOCUMENT_ELEMENT);
        if( !jsDocument.executionStarted() )
        {
            try
            {
                jsDocument.startExecution(true);
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "JavaScript error: " + ex.getMessage());
            }
        }
    }
}
