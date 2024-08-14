package biouml.plugins.test.editors;

import java.awt.BorderLayout;
import java.util.ResourceBundle;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathField;
import biouml.plugins.test.AcceptanceTestSuite;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class NewTestSuiteDialog extends OkCancelDialog
{
    protected DataElementPathField testPath = null;

    protected MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    public NewTestSuiteDialog(DataCollection targetDC)
    {
        super(Application.getApplicationFrame(), "New test suite");

        setResizable(false);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        String elementNameBase = "test_suite";
        String elementName = elementNameBase;
        int ind = 2;
        while( targetDC.contains(elementName) )
        {
            elementName = elementNameBase + "_" + ind;
            ind++;
        }
        DataElementPath defaultPath = DataElementPath.create(targetDC, elementName);
        testPath = new DataElementPathField("testPath", AcceptanceTestSuite.class, messageBundle.getString("NEW_SUITE_CLICK"), defaultPath,
                false);
        contentPane.add(testPath, BorderLayout.CENTER);

        setContent(contentPane);
    }

    public DataElementPath getPath()
    {
        return testPath.getPath();
    }
}
