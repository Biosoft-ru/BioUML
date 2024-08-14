package biouml.plugins.simulation.document;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.gui.DocumentManager;

import com.developmentontheedge.application.action.ApplicationAction;

/**
 * @author axec
 *
 */
public class OpenInteractiveSimulationAction extends AbstractAction
{
    public static final String KEY = "Open interactive simulation";
    
    public OpenInteractiveSimulationAction()
    {
        super(KEY);
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        DataElementPath path = (DataElementPath)getValue(ApplicationAction.PARAMETER);
        DocumentManager.getDocumentManager().openDocument(InteractiveSimulation.class, path.optDataElement());
    }

}