package biouml.plugins.server;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import biouml.workbench.module.AbstractLoadPane;
import ru.biosoft.gui.setupwizard.IncorrectDataException;
import ru.biosoft.gui.setupwizard.WizardPage;

/**
 * Wizard page for databases
 * 
 * @author tolstyh
 */
public class DatabasesWizardPage implements WizardPage
{
    protected AbstractLoadPane loadModulePane;

    @Override
    public JPanel getPanel()
    {
        JPanel topPanel = new JPanel(new BorderLayout());
        
        loadModulePane = new LoadDatabasesPane(false);

        topPanel.add(loadModulePane, BorderLayout.CENTER);
        
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
        loadModulePane.loadIfDatabasesChecked();
    }
}
