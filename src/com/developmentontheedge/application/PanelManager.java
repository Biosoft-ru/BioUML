package com.developmentontheedge.application;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * @pending communication between toolbar, menu "View" and PanelManager
 */
@SuppressWarnings ( "serial" )
public class PanelManager extends JPanel implements PropertyChangeListener
{
    protected Map<String, PanelInfo> infos = new HashMap<>();
    private int defaultDividerSize = 5;

    public PanelManager()
    {
        setLayout(new BorderLayout());
    }

    ////////////////////////////////////////////////////////////////////////////
    // Methods
    //

    /**
     * Returns true if panel manager is empty (thre are no any panels) and false othervise.
     */
    public boolean isEmpty()
    {
        return infos.isEmpty();
    }

    /**
     * Returns true if panel manager contains panel with the specified name
     * and false otherwise.
     */
    public boolean contains(String name)
    {
        return infos.containsKey(name);
    }

    public JComponent getPanel(String name)
    {
        PanelInfo pi = getPanelInfo(name);
        return pi == null ? null : pi.getPanel();
    }

    public PanelInfo getPanelInfo( String name )
    {
        return infos.get(name);
    }

    public String[] getPanelNames()
    {
        return infos.keySet().toArray(new String[infos.size()]);
    }


    public void addPanel(PanelInfo info, String groupWith, int location)
    {
        addPanel(info, groupWith, location, 100);
    }

    public void addPanel(PanelInfo info, String groupWith, int location, int divPos)
    {
        JSplitPane splitPane = doAddPanel(info, groupWith, location);
        if( splitPane != null )
            splitPane.setDividerLocation(divPos);
    }

    public void addPanel(PanelInfo info, String groupWith, int location, double relativeDivPos)
    {
        JSplitPane splitPane = doAddPanel(info, groupWith, location);
        if( splitPane != null )
        {
            if( relativeDivPos < 0.05 || (relativeDivPos > 0.95 && relativeDivPos < 5) )
                relativeDivPos = 0.5;

            if( relativeDivPos < 5 )
                splitPane.setDividerLocation(relativeDivPos);
            else
                splitPane.setDividerLocation((int)relativeDivPos);
        }
    }

    protected JSplitPane doAddPanel(PanelInfo info, String groupWith, int location)
    {
/*      ApplicationAction action = (ApplicationAction)info.getAction();
        action.setEnabled(true);
        toolBar.addToggleButtonAt(action, GROUP_PANE, info.isEnabled());
*/
        JComponent panel = info.getPanel();
        panel.setVisible(info.isEnabled());

        JSplitPane splitPane = null;
        if (infos.isEmpty())
            add(panel, BorderLayout.CENTER);
        else
        {
            if(!infos.containsKey(groupWith))
                throw new RuntimeException("ApplicationFrame.addPanel(): can't group with non-existing panel");
            splitPane = group(infos.get(groupWith), info, location);
        }

        infos.put(info.getName(), info);
        return splitPane;
    }

    public void removePanel(String name)
    {
        doRemovePanel(name);
        infos.remove(name);
    }

    protected void doRemovePanel(String name)
    {
        PanelInfo pi = infos.get(name);
        if (pi == null)
            return;

/*        ApplicationAction action = (ApplicationAction)pi.getAction();
        if (action != null)
        {
            toolBar.removeAction(action);
        }
*/
        JComponent comp = pi.getPanel();
        Container parent = comp.getParent();

        if (parent instanceof JSplitPane)
        {
            JSplitPane parentSP = (JSplitPane)parent;
            Component comp1 = null; // should be stored
            if (parentSP.getTopComponent() == comp) // or left
                comp1 = parentSP.getBottomComponent();
            else // right or bottom
                comp1 = parentSP.getTopComponent(); // should be stored

            Container superParent = parentSP.getParent();
            if (superParent instanceof JSplitPane)
            {
                JSplitPane superParentSP = (JSplitPane)superParent;
                if (superParentSP.getTopComponent() == parent) // or left
                    superParentSP.setTopComponent(comp1);
                else // right or bottom
                    superParentSP.setBottomComponent(comp1);
            }
            else if (superParent == this)
            {
                superParent.remove(parent);
                superParent.add(comp1, BorderLayout.CENTER);
            }
        }
        else // parent = contentPane of the frame
        {
            parent.remove(comp);
        }
    }

    public void removeAllPanels()
    {
        for(String key : infos.keySet())
        {
            doRemovePanel(key);
        }
        infos.clear();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utilities
    //

    protected JSplitPane group(PanelInfo pi1, PanelInfo pi2, int location)
    {
        JComponent comp1 = pi1.getPanel();

        JComponent comp2 = pi2.getPanel();
        Dimension sz2 = pi2.getPanel().getSize();

        JSplitPane splitPane = null;
        Container parent = comp1.getParent();

        switch (location)
        {
            case PanelInfo.LEFT   :
                splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, comp2, comp1);
                comp2.setPreferredSize ( new Dimension(sz2.width - location,sz2.height));
                break;

            case PanelInfo.RIGHT  :
                splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, comp1, comp2);
                comp2.setPreferredSize ( new Dimension(sz2.width - location,sz2.height));
                break;

            case PanelInfo.TOP    :
                splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,   comp2, comp1);
                comp2.setPreferredSize ( new Dimension(sz2.width ,sz2.height- location));
                break;

            case PanelInfo.BOTTOM :
            default     :
                splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,   comp1, comp2);
                comp2.setPreferredSize ( new Dimension(sz2.width ,sz2.height- location));
                break;
        }
        splitPane.setDividerSize(defaultDividerSize);

        if (!pi1.isEnabled() || !pi2.isEnabled())
        {
            splitPane.setDividerSize(0);
        }

        // add splitPane back to the components hierarchy
        if (parent instanceof JSplitPane)
        {
            JSplitPane parentSP = (JSplitPane)parent;
            if (parentSP.getLeftComponent() == null) // or top
            {
                parentSP.setLeftComponent(splitPane);
            } else // right or bottom
            {
                parentSP.setRightComponent(splitPane);
            }
            parentSP.setDividerSize( pi1.isEnabled() || pi2.isEnabled() ? defaultDividerSize : 0 );
        } else // parent = contentPane of the frame
        {
            parent.remove(comp1);
            parent.add(splitPane, BorderLayout.CENTER);
        }

        splitPane.addPropertyChangeListener(this);
        return splitPane;
    }


    public void togglePanel(String key)
    {
        PanelInfo pi = getPanelInfo(key);
        if (pi != null)
        {
            pi.setEnabled(!pi.isEnabled());

            JComponent comp = pi.getPanel();
            comp.setVisible(pi.isEnabled());
            JComponent parent = (JComponent)comp.getParent();

            if (parent instanceof JSplitPane)
            {
                update((JSplitPane)parent);
            }
            return;

        }
    }

    /**
     * This method gets called when a bound property is changed.
     * @param evt A PropertyChangeEvent object describing the event source
     *      and the property that has changed.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals("dividerLocation"))
        {
            JSplitPane sp = (JSplitPane)evt.getSource();
            Component top = sp.getTopComponent();
            Component bottom = sp.getBottomComponent();
            if (top != null    && top.isVisible() &&
                bottom != null && bottom.isVisible())
            {
                Integer integer = (Integer)evt.getNewValue();
                sizesDividerLocation.put(sp, integer);
            }
        }
    }

    protected Map<JSplitPane, Integer> sizesDividerLocation = new HashMap<>();

    private void update(JSplitPane sp)
    {
        Component comp1 = sp.getBottomComponent();
        Component comp2 = sp.getTopComponent();

        if (comp1.isVisible() && comp2.isVisible())
        {
            sp.setVisible(true);
            sp.setDividerSize(defaultDividerSize);

            Integer size = sizesDividerLocation.get(sp);
            if (size != null)
            {
                sp.setDividerLocation(size.intValue());
            }
        } else if (!comp1.isVisible() && !comp2.isVisible())
        {
            sp.setVisible(false);
        } else if (!comp1.isVisible() || !comp2.isVisible())
        {
            //if(sp.isVisible() && sp.getDividerLocation() != 1)
            if (sp.getDividerLocation() != 1)
            {
                sizesDividerLocation.put(sp, sp.getDividerLocation());
            }
            sp.setVisible(true);
            sp.setDividerSize(0);
        }

        Component parent = sp.getParent();
        if (parent instanceof JSplitPane)
        {
            update((JSplitPane)parent);
        }
    }
}
