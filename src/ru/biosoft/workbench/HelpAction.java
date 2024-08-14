
package ru.biosoft.workbench;

import java.awt.event.ActionEvent;
import java.net.URI;

import javax.swing.AbstractAction;

public class HelpAction extends AbstractAction
{
    public static final String KEY = "Help";

    protected String helpId;

    public HelpAction(String helpId)
    {
        super(KEY);
        this.helpId = helpId;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        try
        {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

            if( desktop.isSupported( java.awt.Desktop.Action.BROWSE ) )
            {
                desktop.browse( new URI( "http://wiki.biouml.org" ) );
            }
        }
        catch( Exception ex )
        {
        }
    }
}
