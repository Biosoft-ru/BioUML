package biouml.plugins.test.access;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathField;
import ru.biosoft.gui.DocumentManager;
import biouml.model.Diagram;
import biouml.plugins.test.TestModel;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class NewTestDocumentDialog extends OkCancelDialog
{
    protected Logger log = Logger.getLogger(NewTestDocumentDialog.class.getName());

    protected DataCollection targetDC;
    protected DataElementPathField testPath = null;
    protected DataElementPathField diagramPath = null;

    protected MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    public NewTestDocumentDialog(DataCollection targetDC)
    {
        super(Application.getApplicationFrame(), "New test document");

        this.targetDC = targetDC;
        setResizable(false);

        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        contentPane.add(new JLabel(messageBundle.getString("NEW_TEST_NAME")), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        DataElementPath defaultPath = DataElementPath.create(targetDC, "new_test");
        testPath = new DataElementPathField("testPath", TestModel.class, messageBundle.getString("NEW_TEST_CLICK"), defaultPath, false);
        contentPane.add(testPath, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));

        contentPane.add(new JLabel(messageBundle.getString("NEW_MODEL_PATH")), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        defaultPath = DataElementPath.create("databases", "");
        diagramPath = new DataElementPathField("experimentPath", Diagram.class, messageBundle.getString("NEW_MODEL_CLICK"), defaultPath,
                true);
        contentPane.add(diagramPath, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));
        setContent(contentPane);
    }

    @Override
    protected void okPressed()
    {
        DataElementPath path = testPath.getPath();
        String name = path.getName();
        DataCollection dc = path.optParentCollection();
        if( !name.isEmpty() )
        {
            try
            {
                DataElementPath dPath = diagramPath.getPath();
                Diagram diagram = dPath.getDataElement(Diagram.class);
                if( diagram.getRole() == null )
                {
                    String message = MessageFormat.format(messageBundle.getString("WARN_INCORRECT_MODEL"), new Object[] {dPath.getName()});
                    JOptionPane.showMessageDialog(Application.getApplicationFrame(), message);
                    return;
                }
                TestModel testModel = new TestModel(dc, name, dPath);
                dc.put(testModel);

                DocumentManager.getDocumentManager().openDocument(testModel);
                super.okPressed();
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, messageBundle.getString("ERROR_TEST_CREATION"), t);
            }
        }
    }
}
