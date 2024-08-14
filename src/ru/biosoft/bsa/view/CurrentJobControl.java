package ru.biosoft.bsa.view;

import ru.biosoft.jobcontrol.AbstractJobControl;

public class CurrentJobControl
{
    private static CurrentJobControl instance = null;
    private AbstractJobControl value;

    protected CurrentJobControl()
    {
    }

    public static CurrentJobControl getInstance()
    {
        if( instance == null )
        {
            instance = new CurrentJobControl();
        }
        return instance;
    }

    public AbstractJobControl getValue()
    {
        return value;
    }

    public void setValue(AbstractJobControl value)
    {
        this.value = value;
    }

}
