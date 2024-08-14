
package com.developmentontheedge.application;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

@SuppressWarnings ( "serial" )
public class ApplicationMenuBar extends JMenuBar
{
    public void addSeparator(JMenu menu, int type)
    {
        JPopupMenu popup = menu.getPopupMenu();
        popup.add(new MSeparator(type));
    }

    static class MSeparator extends JSeparator
    {
        private int group;

        public MSeparator(int group)
        {
            this.group = group;
        }

        int getGroup()
        {
            return group;
        }

        @Override
        public String toString()
        {
            return "separator " + group;
        }
    }
}
