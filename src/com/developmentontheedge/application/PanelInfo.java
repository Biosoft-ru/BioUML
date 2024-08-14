package com.developmentontheedge.application;

import javax.swing.Action;
import javax.swing.JComponent;

public class PanelInfo
{
    public final static int LEFT   = 1;
    public final static int RIGHT  = 2;
    public final static int TOP    = 3;
    public final static int BOTTOM = 4;

    public PanelInfo(String name, JComponent panel, boolean isEnabled, Action action)
    {
        this.name = name;
        this.panel = panel;
        this.isEnabled = isEnabled;
        this.action = action;
    }

    protected String name;
    public String getName()
    {
        return name;
    }

    protected JComponent panel;
    public JComponent getPanel()
    {
        return panel;
    }

    protected boolean isEnabled;
    public boolean isEnabled()
    {
        return isEnabled;
    }
    public void setEnabled(boolean isEnabled)
    {
        this.isEnabled = isEnabled;
    }


    protected Action action;
    public Action getAction()
    {
        return action;
    }
}
