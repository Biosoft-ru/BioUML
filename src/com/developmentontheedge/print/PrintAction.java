package com.developmentontheedge.print;

import javax.swing.AbstractAction;

@SuppressWarnings ( "serial" )
public abstract class PrintAction extends AbstractAction
{
    public static final String KEY = "Print diagram";

    public PrintAction()
    {
        super(KEY);
    }
}
