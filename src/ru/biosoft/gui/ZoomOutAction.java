package ru.biosoft.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.graphics.editor.ViewPane;

public class ZoomOutAction extends AbstractAction
{
    public static final String KEY = "Zoom out";

    public ZoomOutAction(boolean enabled)
    {
        super(KEY);
        setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Document activeDocument = Document.getActiveDocument();
        if( activeDocument != null )
        {
            ViewPane viewPane = activeDocument.getViewPane();
            if( viewPane != null )
            {
                viewPane.scale(1 / 1.2, 1 / 1.2);
            }
        }
    }
}
