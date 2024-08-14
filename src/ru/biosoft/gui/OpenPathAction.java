package ru.biosoft.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathDialog;

public class OpenPathAction extends AbstractAction
{

    public static final String KEY = "Open path";

    public OpenPathAction(boolean enabled)
    {
        super( KEY );
        setEnabled( enabled );
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        DataElementPathDialog dialog = new DataElementPathDialog( "Open data element" );
        dialog.setMultiSelect( false );
        dialog.setElementMustExist( true );
        dialog.setValue( (DataElementPath)null );
        if( dialog.doModal() )
        {
            DataElementPath path = dialog.getValue();
            if( path.exists() )
            {
                DocumentManager.getDocumentManager().openDocument( path.getDataElement() );
                GUI.getManager().getRepositoryTabs().selectElement( path );
            }
        }
    }

}
