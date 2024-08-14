package biouml.workbench.diagram;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.gui.Document;
import biouml.model.Diagram;

public class ViewOptionsAction extends AbstractAction
{
    public static final String KEY = "View options";

    public ViewOptionsAction ( )
    {
        this ( true );
    }

    public ViewOptionsAction ( boolean enabled )
    {
        super ( KEY );
        setEnabled ( enabled );
    }

    @Override
    public void actionPerformed ( ActionEvent evt )
    {
        DataElement model = Document.getActiveModel();
        if(model instanceof Diagram)
        {
            new ViewOptionsDialog ( ( Diagram ) model ).doModal ( );
        }
    }
}
