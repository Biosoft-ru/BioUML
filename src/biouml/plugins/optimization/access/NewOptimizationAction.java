package biouml.plugins.optimization.access;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import ru.biosoft.access.core.DataCollection;

public class NewOptimizationAction extends AbstractAction
{
    public static final String KEY = "New optimization";
    public static final String DATA_COLLECTION = "Documents";

    public NewOptimizationAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        DataCollection dc = (DataCollection)getValue(DATA_COLLECTION);
        NewOptimizationDialog dialog = new NewOptimizationDialog(dc);
        dialog.doModal();
    }
}
