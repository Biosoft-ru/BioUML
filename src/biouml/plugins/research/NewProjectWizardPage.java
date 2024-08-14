package biouml.plugins.research;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import biouml.plugins.research.action.NewProjectPane;

import ru.biosoft.gui.setupwizard.IncorrectDataException;
import ru.biosoft.gui.setupwizard.WizardPage;

/**
 * Wizard page for project creation
 */
public class NewProjectWizardPage implements WizardPage
{
    private MessageBundle messages = new MessageBundle();

    protected NewProjectPane researchPane;

    @Override
    public JPanel getPanel()
    {
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel bottonsPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createButton = new JButton(messages.getString("WIZARD_CREATE_TEXT"));
        createButton.setEnabled(false);
        bottonsPane.add(createButton);
        topPanel.add(bottonsPane, BorderLayout.SOUTH);

        researchPane = new NewProjectPane(createButton);
        topPanel.add(researchPane, BorderLayout.CENTER);

        createButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                researchPane.createNewResearch();
            }
        });

        return topPanel;
    }

    @Override
    public void saveSettings() throws IncorrectDataException
    {
        // nothing to save
    }

    @Override
    public void fireOpenPage()
    {
        researchPane.updateSQLInfo();
    }
    
    @Override
    public void fireClosePage()
    {
        //nothing to do
    }
}
