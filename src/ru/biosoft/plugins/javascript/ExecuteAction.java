package ru.biosoft.plugins.javascript;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.plugins.javascript.document.JSDocument;

@SuppressWarnings ( "serial" )
public class ExecuteAction extends AbstractAction
{
    protected Logger log = Logger.getLogger(ExecuteAction.class.getName());

    public static final String KEY = "execute javascript";
    public static final String DOCUMENT_ELEMENT = "document";

    public ExecuteAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JSDocument jsDocument = (JSDocument)getValue(DOCUMENT_ELEMENT);
        if( !jsDocument.executionStarted() )
        {
            jsDocument.getJSPanel().getDim().clearScope();
            try
            {
                jsDocument.startExecution(false);
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "JavaScript error: " + ex.getMessage());
            }
        }
    }
}
