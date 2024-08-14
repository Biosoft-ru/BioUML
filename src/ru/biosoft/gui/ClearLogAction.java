package ru.biosoft.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class ClearLogAction extends AbstractAction
{
    
    public static final String KEY = "clear log";
    
    private final ClearablePane pane;
    
    public ClearLogAction(ClearablePane pane)
    {
        this.pane = pane;
    }

    @Override
    public void actionPerformed ( ActionEvent arg0 )
    {
        pane.clear ( );
    }
    
}