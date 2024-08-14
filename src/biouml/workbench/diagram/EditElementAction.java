package biouml.workbench.diagram;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import ru.biosoft.util.PropertiesDialog;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.DiagramElement;


@SuppressWarnings ( "serial" )
public class EditElementAction extends AbstractAction
{
    public static final String KEY = "Edit";
    public static final String DIAGRAM_ELEMENT = "diagram element";
    public static final String VIEWPANE = "viewPane";

    public EditElementAction()
    {
        this( true );
    }

    public EditElementAction(boolean enabled)
    {
        super( KEY );
        setEnabled( enabled );
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        DiagramElement de = (DiagramElement)getValue( DIAGRAM_ELEMENT );
        PropertiesDialog dialog = new PropertiesDialog(Application.getApplicationFrame(), de.getName(), de);
        dialog.setModal( false );
        dialog.setAlwaysOnTop( false );
        dialog.pack();
        ApplicationUtils.moveToCenter(dialog);
        dialog.setVisible( true );
    }
}
