package com.developmentontheedge.application;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

@SuppressWarnings ( "serial" )
public class ApplicationToolBar extends JToolBar
{
    protected HashMap<Action, AbstractButton> buttons = new HashMap<>();

    public ApplicationToolBar()
    {
        setFloatable(true);
    }

    public AbstractButton addAction(String act)
    {
        return addAction(Application.getActionManager().getAction(act));
    }

    public AbstractButton addAction(Action action)
    {
        JButton b = createActionComponent(action);

        buttons.put(action, b);
        configureButton(b, action);
        add(b);
        return b;
    }

    public void addAction(Action action, int group)
    {
        int pos = addGroup(group);
        AbstractButton button = addActionAt(action, pos);
        configureButton(button, action);
    }

    public AbstractButton addActionAt(Action action, int pos)
    {
        JButton b = this.createActionComponent(action);
        buttons.put(action, b);
        configureButton(b, action);
        add(b, pos+1);
        return b;
    }

    public void removeAction(Action action)
    {
        AbstractButton button = buttons.get(action);
        if( button != null )
        {
            remove(button);
            buttons.remove(action);
        }
    }

    public void addSeparator(int type)
    {
        add(new MSeparator(type));
    }

    public AbstractButton addToggleButtonAt(Action action, int group, boolean isSelected)
    {
        int pos = addGroup(group);
        ImageIcon imageIcon = (ImageIcon)action.getValue(Action.SMALL_ICON);
        JToggleButton b = new JToggleButton(imageIcon, isSelected);
        buttons.put(action, b);
        add(b, pos+1);
        configureButton(b, action);
        return b;
    }

    MouseListener mouseListener = new LMouseListener();

    BevelBorder border;

    private void configureButton(AbstractButton button, Action action)
    {
        button.setAlignmentY(0.5f);
        button.setAction(action);
        button.setActionCommand((String)action.getValue(Action.ACTION_COMMAND_KEY));
        button.setText("");
        Dimension btnSize = new Dimension(25, 25);
        button.setSize(btnSize);
        button.setPreferredSize(btnSize);
        button.setMinimumSize(btnSize);
        button.setMaximumSize(btnSize);
        button.addMouseListener(mouseListener);
        button.setBorderPainted(false);
        if( border == null )
            border = new BevelBorder(BevelBorder.RAISED)
            {
                @Override
                protected void paintRaisedBevel(Component c, Graphics g, int x, int y, int width, int height)
                {
                    Color oldColor = g.getColor();
                    int h = height;
                    int w = width;
                    g.translate(x, y);
                    g.setColor(getHighlightOuterColor(c));
                    g.drawLine(0, 0, 0, h - 1);
                    g.drawLine(1, 0, w - 1, 0);
                    g.setColor(getHighlightInnerColor(c));
                    g.drawLine(1, 1, 1, h - 2);
                    g.drawLine(2, 1, w - 2, 1);
                    g.setColor(getShadowOuterColor(c));
                    g.drawLine(1, h - 1, w - 1, h - 1);
                    g.drawLine(w - 1, 1, w - 1, h - 2);
                    g.setColor(getShadowInnerColor(c));
                    g.drawLine(2, h - 2, w - 2, h - 2);
                    g.drawLine(w - 2, 2, w - 2, h - 3);
                    g.translate( -x, -y);
                    g.setColor(oldColor);
                }
            };
        button.setBorder(border);
    }

    public int addGroup(int group)
    {
        int pos = findSeparator(group);
        if( pos != -1 )
            return pos;
        int g = group;
        while( g > 0 )
        {
            g--;
            pos = findSeparator(g);
            if( pos != -1 )
                break;
        }
        add(new MSeparator(group), ++pos);
        return pos + 1;
    }

    public void cleanGroup(int group)
    {
        int pos = findSeparator(group);
        if( pos == -1 )
            return;

        int deleteCount = findNextSeparator(pos) - pos - 1;
        if( deleteCount > 0 )
        {
            for( int i = 0; i < deleteCount; i++ )
            {
                remove(pos + 1);
            }
        }
    }

    protected int findSeparator(int group)
    {
        for( int i = 0; i < getComponentCount(); i++ )
        {
            Component c = getComponent(i);
            if( c instanceof MSeparator && ( (MSeparator)c ).getGroup() == group )
                return i;
        }
        return -1;
    }

    protected int findNextSeparator(int pos)
    {
        for( int i = pos + 1; i < getComponentCount(); i++ )
        {
            Component c = getComponent(i);
            if( c instanceof MSeparator )
                return i;
        }
        return getComponentCount();
    }

    class MSeparator extends JToolBar.Separator
    {
        private final int group;

        public MSeparator(int group)
        {
            this.group = group;
            setOrientation(SwingConstants.VERTICAL);
        }

        int getGroup()
        {
            return group;
        }
    }

    static class LMouseListener extends MouseAdapter
    {
        @Override
        public void mouseEntered(MouseEvent e)
        {
            AbstractButton button = (AbstractButton)e.getSource();
            if( button.getAction().isEnabled() )
                button.setBorderPainted(true);
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            AbstractButton button = (AbstractButton)e.getSource();
            button.setBorderPainted(false);
        }
    }
}
