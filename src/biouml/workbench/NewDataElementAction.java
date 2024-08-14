package biouml.workbench;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.support.NewDataElementDialog;

import com.developmentontheedge.application.Application;

public class NewDataElementAction extends AbstractAction
{
    public static final String KEY = "New Data Element";
    public static final String DATA_COLLECTION = "Data Collection (parent)";

    public NewDataElementAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        DataCollection dc = (DataCollection)getValue(DATA_COLLECTION);

        NewDataElementDialog dlg = new NewDataElementDialog(Application.getApplicationFrame(),
                                                            "New data element: " + dc.getName(), dc);
        if( dlg.doModal() )
        {
            try
            {
                dc.put(dlg.getNewDataElement());
            }
            catch(Throwable t)
            {
                t.printStackTrace();
            }
        }
    }
}
