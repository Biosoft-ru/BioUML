package com.developmentontheedge.print;

import javax.swing.AbstractAction;

@SuppressWarnings ( "serial" )
public abstract class PrintPreviewAction extends AbstractAction
{
    public static final String KEY = "Print preview";

    public PrintPreviewAction()
    {
        super(KEY);
    }
}
