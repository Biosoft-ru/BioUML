
package biouml.plugins.simulation.plot;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.gui.DocumentManager;
import biouml.standard.simulation.plot.Plot;

import com.developmentontheedge.application.action.ApplicationAction;

/**
 * @author anna
 *
 */
public class OpenPlotAction extends AbstractAction
{
    public static final String KEY = "Open plot";
    public static final String KEY2 = "New plot";
    
    public OpenPlotAction()
    {
        super(KEY);
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        DataElementPath path = (DataElementPath)getValue(ApplicationAction.PARAMETER);
        DocumentManager.getDocumentManager().openDocument(Plot.class, path.optDataElement());
    }

}
