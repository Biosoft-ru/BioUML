package ru.biosoft.plugins.javascript;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import ru.biosoft.plugins.javascript.document.Dim;
import ru.biosoft.plugins.javascript.document.JSDocument;

public class StopAction extends AbstractAction
{
    public static final String KEY = "stop javascript";
    public static final String DOCUMENT_ELEMENT = "document";

    public StopAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JSDocument jsDocument = (JSDocument)getValue(DOCUMENT_ELEMENT);
        jsDocument.getJSPanel().getDim().setReturnValue(Dim.EXIT);
        jsDocument.stopExecution();
    }
}
