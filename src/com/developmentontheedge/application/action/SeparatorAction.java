package com.developmentontheedge.application.action;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * Special action that is used as separator to be inserted
 * between other actions.
 */
@SuppressWarnings ( "serial" )
public class SeparatorAction extends AbstractAction
{
    /** Key to get separator size. */
    public final static String DIMENSION = "dimension";

    public SeparatorAction()
    {
        this(new Dimension(15, 15));
    }

    public SeparatorAction(Dimension dimension)
    {
        putValue(Action.NAME, "separator");
        putValue(DIMENSION, dimension);
    }
   
    // do nothing
    @Override
    public void actionPerformed(ActionEvent e)
    {}
}