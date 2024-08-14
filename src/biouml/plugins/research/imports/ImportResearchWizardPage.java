package biouml.plugins.research.imports;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import ru.biosoft.gui.setupwizard.IncorrectDataException;
import ru.biosoft.gui.setupwizard.WizardPage;
import biouml.workbench.module.AbstractLoadPane;

public class ImportResearchWizardPage implements WizardPage
{
    protected AbstractLoadPane loadModulePane;
    
    @Override
    public void fireOpenPage()
    {
        // nothing to do

    }

    @Override
    public JPanel getPanel()
    {
        JPanel topPanel = new JPanel(new BorderLayout());
        
        loadModulePane = new ImportResearchPane(false);
        topPanel.add(loadModulePane, BorderLayout.CENTER);
        
        return topPanel;
    }

    @Override
    public void saveSettings() throws IncorrectDataException
    {
        // nothing to do

    }
    
    @Override
    public void fireClosePage()
    {
        loadModulePane.loadIfDatabasesChecked();
    }
}
