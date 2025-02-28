package biouml.plugins.physicell.document;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.gui.DocumentManager;

import com.developmentontheedge.application.action.ApplicationAction;

/**
 * @author axec
 *
 */
public class OpenPhysicellResultAction extends AbstractAction
{
    public static final String KEY = "Open physicell result";
    
    public OpenPhysicellResultAction()
    {
        super(KEY);
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        DataElementPath path = (DataElementPath)getValue(ApplicationAction.PARAMETER);
        DocumentManager.getDocumentManager().openDocument(PhysicellSimulationResult.class, path.optDataElement());
    }

}