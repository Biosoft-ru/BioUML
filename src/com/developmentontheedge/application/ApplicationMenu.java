
package com.developmentontheedge.application;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

@SuppressWarnings ( "serial" )
public class ApplicationMenu
        extends JMenu
{
    public ApplicationMenu ( String name )
    {
        super ( name );
    }

    public JMenuItem addAction ( Action a )
    {
        return add ( a );
    }

    public JMenuItem addAction ( String key )
    {
        return add ( Application.getActionManager ( ).getAction ( key ) );
    }

    /**
     * Overrides method in JMenu
     */
    @Override
    public JMenuItem add ( Action a )
    {
        JMenuItem menuItem = super.add ( a );
        menuItem.setActionCommand ( ( String ) a.getValue ( Action.ACTION_COMMAND_KEY ) );
        return menuItem;
    }

    public void addSeparator ( JMenu menu, int type )
    {
        JPopupMenu popup = menu.getPopupMenu ( );
        popup.add ( new MSeparator ( type ) );
    }

    static class MSeparator
            extends JSeparator
    {
        private int group;

        public MSeparator ( int group )
        {
            this.group = group;
        }

        int getGroup ( )
        {
            return group;
        }

        @Override
        public String toString ( )
        {
            return "separator " + group;
        }
    }
}
