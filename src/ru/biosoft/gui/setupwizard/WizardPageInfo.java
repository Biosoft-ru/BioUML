package ru.biosoft.gui.setupwizard;

/**
 * Wizard page descriptor
 * 
 * @author tolstyh
 */
public class WizardPageInfo implements Comparable<WizardPageInfo>
{
    protected String name;
    protected String description;
    protected WizardPage page;
    protected int pos;

    public WizardPageInfo(String name, String description, int pos, WizardPage page)
    {
        this.name = name;
        this.description = description;
        this.pos = pos;
        this.page = page;
    }

    public String getName()
    {
        return name;
    }

    public WizardPage getPage()
    {
        return page;
    }

    public int getPos()
    {
        return pos;
    }

    public String getDescription()
    {
        return description;
    }

    @Override
    public int compareTo(WizardPageInfo o)
    {
        if(o.getPos() != getPos())
            return getPos() > o.getPos() ? 1 : -1;
        return getName().compareTo(o.getName());
    }
}
