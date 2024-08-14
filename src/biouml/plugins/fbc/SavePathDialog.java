package biouml.plugins.fbc;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathField;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

@SuppressWarnings ( "serial" )
public class SavePathDialog extends OkCancelDialog
{
    protected DataElementPathField path = null;
    public DataElementPath resultPath;
    
    public SavePathDialog()
    {
        super(Application.getApplicationFrame(), "Save table");

        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 40, 10, 40));

        contentPane.add(new JLabel("Result path:"), new GridBagConstraints(0, 2, 1, 1, 0.0,
                0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        path = new DataElementPathField("path", TableDataCollection.class, "(click to select the result file)", null, false);
        contentPane.add(path, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));

        setContent(contentPane);
    }
    @Override
    protected void okPressed()
    {
        try
        {
            resultPath = path.getPath();
            super.okPressed();
        }
        catch( Throwable t )
        {
        }
    }
}
