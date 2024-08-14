package ru.biosoft.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.access.core.DataElement;

public class SaveDocumentAction
        extends AbstractAction
{
    public static final String KEY = "Save document";

    public SaveDocumentAction ( boolean enabled )
    {
        super ( KEY );
        setEnabled ( enabled );
    }

    @Override
    public void actionPerformed ( ActionEvent e )
    {
        DocumentManager.getDocumentViewAccessProvider().saveDocument ( );
        setEnabled ( false );
        Object model = Document.getCurrentDocument().getModel();
        if( model instanceof DataElement )
            GUI.getManager().getRepositoryTabs().selectElement( ( (DataElement)model ).getCompletePath() );
    }
}
