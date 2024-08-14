package com.developmentontheedge.application.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.developmentontheedge.application.PanelManager;

@SuppressWarnings ( "serial" )
public class TogglePanelAction extends AbstractAction
{
    private String key;
    private PanelManager panelManager;

    public TogglePanelAction(String key, PanelManager panelManager)
    {
        super(key);
        this.key = key;
        this.panelManager = panelManager;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        panelManager.togglePanel(key);
    }
}
