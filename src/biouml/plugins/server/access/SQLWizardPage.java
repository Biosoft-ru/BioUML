package biouml.plugins.server.access;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import ru.biosoft.gui.setupwizard.IncorrectDataException;
import ru.biosoft.gui.setupwizard.WizardPage;

/**
 * Wizard page for SQL preferences. Uses {@link SQLPropertiesPane}
 */
public class SQLWizardPage implements WizardPage
{
    @Override
    public JPanel getPanel()
    {
        JPanel topPanel = new JPanel(new BorderLayout());

        SQLPropertiesPane sqlPane = new SQLPropertiesPane();
        topPanel.add(sqlPane, BorderLayout.CENTER);

        return topPanel;
    }

    @Override
    public void saveSettings() throws IncorrectDataException
    {
        //nothing to do
    }
    
    @Override
    public void fireOpenPage()
    {
        //nothing to do
    }
    
    @Override
    public void fireClosePage()
    {
        //nothing to do
    }
}
