package biouml.workbench;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;


import ru.biosoft.access.core.DataCollection;

public class ImportElementAction
        extends AbstractAction
{
    public static final String KEY = "Import document";

    public static final String DATABASE = "Database";

    public ImportElementAction ( )
    {
        this ( true );
    }

    public ImportElementAction ( boolean enabled )
    {
        super ( KEY );
        setEnabled ( enabled );
    }

    @Override
    public void actionPerformed ( ActionEvent evt )
    {
        ImportElementDialog dialog = new ImportElementDialog ( ( DataCollection ) getValue ( DATABASE ) );
        dialog.doModal ( );
    }
}
