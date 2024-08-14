package ru.biosoft.gui.setupwizard;

/**
 * Incorrect data exception
 * 
 * @author tolstyh
 */
public class IncorrectDataException extends Exception
{
    protected WizardPage page;

    public IncorrectDataException(WizardPage page, String message)
    {
        super(message);
        this.page = page;
    }

    public WizardPage getPage()
    {
        return page;
    }
}
