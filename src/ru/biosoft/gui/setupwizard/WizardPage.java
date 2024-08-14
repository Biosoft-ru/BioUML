package ru.biosoft.gui.setupwizard;

import javax.swing.JPanel;

/**
 * Interface for one page of setup wizard
 * 
 * @author tolstyh
 */
public interface WizardPage
{
    /**
     * get panel with settings
     */
    public JPanel getPanel();

    /**
     * indicates of page opening
     */
    public void fireOpenPage();
    
    /**
     * indicates of page closing
     */
    public void fireClosePage();
    
    /**
     * save settings
     */
    public void saveSettings() throws IncorrectDataException;
}
