package ru.biosoft.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class CloseDocumentAction extends AbstractAction
{
    public static final String KEY = "Close document";

    public CloseDocumentAction(boolean enabled)
    {
        super( KEY );
        setEnabled( enabled );
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        DocumentManager.getDocumentViewAccessProvider().closeCurrentDocument();
    }
}
