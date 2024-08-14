package ru.biosoft.plugins.javascript;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.plugins.javascript.document.Dim;
import ru.biosoft.plugins.javascript.document.JSDocument;

public class StepAction extends AbstractAction
{
    protected Logger log = Logger.getLogger(StepAction.class.getName());

    public static final String KEY = "execute step javascript";
    public static final String DOCUMENT_ELEMENT = "document";

    protected MessageBundle messageBundle = new MessageBundle();

    public StepAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JSDocument jsDocument = (JSDocument)getValue(DOCUMENT_ELEMENT);

        if( !jsDocument.getJSPanel().executionStarted() )
        {
            String text = jsDocument.getText(false);

            jsDocument.getJSPanel().getDim().setBreak();
            jsDocument.getJSPanel().getDim().clearScope();
            try
            {
                //jsDocument.startExecution();
                jsDocument.getJSPanel().getDim().evalScript(jsDocument.getJSElement().getName(), text, jsDocument.getEnvironment());
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "JavaScript error: " + ex.getMessage());
            }
        }
        else
        {
            jsDocument.getJSPanel().getDim().setReturnValue(Dim.STEP_OVER);
        }
    }
}
