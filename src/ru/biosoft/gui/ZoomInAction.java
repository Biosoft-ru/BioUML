package ru.biosoft.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.graphics.editor.ViewPane;

public class ZoomInAction extends AbstractAction
{
    public static final String KEY = "Zoom in";

    public ZoomInAction(boolean enabled)
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
                viewPane.scale(1.2, 1.2);
            }
        }
    }
}
