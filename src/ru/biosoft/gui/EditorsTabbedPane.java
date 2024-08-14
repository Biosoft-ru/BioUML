package ru.biosoft.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;

import com.developmentontheedge.beans.ActionsProvider;
import com.developmentontheedge.beans.undo.Transactable;
import com.developmentontheedge.beans.undo.TransactionListener;
import com.developmentontheedge.application.action.SeparatorAction;

/**
 * Class to group several editors into one tabbed pane with common toolbar
 */
// TODO: reuse actions in update tab if they the same
public class EditorsTabbedPane extends EditorPartSupport
{
    public static final String ACTION_NAME = "Editors";
    public static final int TOOLBAR_BUTTON_SIZE = 20;

    protected JToolBar toolbar;
    protected JTabbedPane tabPane;
    private ViewPart currentTab;
    // //////////////////////////////////////////////////////////////////////////
    // Constructor
    //

    public EditorsTabbedPane()
    {
        this.setLayout(new BorderLayout());

        toolbar = new JToolBar();
        toolbar.setFloatable(false);

        tabPane = new JTabbedPane();
        tabPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabPane.addChangeListener(e -> updateTab());

        add(BorderLayout.NORTH, toolbar);
        add(BorderLayout.CENTER, tabPane);

        toolbar.addSeparator();

        tabActionsIndex = 0;
        modelActionsIndex = 1;

        editorsManager = new EditorsManager(this);
    }

    protected EditorsManager editorsManager;

    public ViewPart getEditorsManager()
    {
        return editorsManager;
    }

    public void addViewPart(ViewPart view, boolean enabled)
    {
        editorsManager.addViewPart(view, enabled);
    }

    public JToolBar getToolbar()
    {
        return this.toolbar;
    }

    // //////////////////////////////////////////////////////////////////////////
    // ViewPart interface implementation
    //

    @Override
    public Action[] getActions()
    {
        return null;
    }

    @Override
    public void explore(Object model, Document document)
    {
        if( this.model == model )
            return;

        this.model = model;
        this.document = document;

        editorsManager.validateTabs(model);
        //if( tabs != null && tabs.size() != 0 )
        //   selectTab(0);

        // set up model new properties
        if( modelActionProvider != null )
        {
            removeActions(modelActions, modelActionsIndex);
            removeNotActionComponents();

            modelActions = modelActionProvider.getActions(model);
            addActions(modelActions, modelActionsIndex);
        }

        int selectedIndex = tabPane.getSelectedIndex();
        ViewPart tab = null;
        for( int i = 0; i < tabPane.getTabCount(); i++ )
        {
            if( i == selectedIndex )
                continue;
            tab = tabs.get(i);
            if( tab.canExplore(model) && !ViewPart.STATIC_VIEW.equals(tab.getModel()) && tab.getModel() != model )
            {
                tab.modelChanged(model);
            }
        }

        // try to show the model value in current tab

        try
        {
            tab = tabs.get(tabPane.getSelectedIndex());
            if( tab.canExplore(model) )
            {
                if( tab.getModel() != this.model )//here this.model is used because during modelChanged execution current model may actually change (see AntimonyEditor for example)
                    updateTab();

                return;
            }
        }
        catch( IndexOutOfBoundsException e )
        {
        }

        // othervise find first tab that can show the model
        for( int i = 0; i < tabPane.getTabCount(); i++ )
        {
            tab = tabs.get(i);
            if( tab.canExplore(model) && !tab.getModel().equals(ViewPart.STATIC_VIEW) )
            {
                selectTab(i);
                break;
            }
        }
    }

//    /** Data (model) that is currently edited or explored. */
//    protected Object model;

    @Override
    public Object getModel()
    {
        return model;
    }

    @Override
    public void save()
    {
        for( int i = 0; i < tabPane.getTabCount(); i++ )
        {
            if( tabs.get(i) instanceof EditorPart )
                ( (EditorPart)tabs.get(i) ).save();
        }
    }

    // //////////////////////////////////////////////////////////////////////////
    // Transactable implementation
    //

    @Override
    public void addTransactionListener(TransactionListener tl)
    {
        super.addTransactionListener(tl);

        for( Object tab : tabs )
        {
            if( tab instanceof Transactable )
                ( (Transactable)tab ).addTransactionListener(tl);

        }
    }

    @Override
    public void removeTransactionListener(TransactionListener tl)
    {
        super.removeTransactionListener(tl);

        for( Object tab : tabs )
        {
            if( tab instanceof Transactable )
                ( (Transactable)tab ).removeTransactionListener(tl);

        }
    }

    // //////////////////////////////////////////////////////////////////////////
    // tab management issues
    //
    protected Vector<ViewPart> tabs = new Vector<>();

    protected Action[] tabActions;

    protected int tabActionsIndex;

    protected Action[] modelActions;

    protected int modelActionsIndex;

    protected void addTab(ViewPart tab)
    {
        insertTab(tabs.size(), tab);
    }

    /**
     * TODO: use icon and tip if available
     */
    protected void insertTab(int index, ViewPart tab)
    {
        tabs.add(index, tab);
        tabPane.insertTab((String)tab.getAction().getValue(Action.NAME), null, tab.getView(), null, index);

        if( tab instanceof Transactable )
        {
            Object[] listeners = listenerList.getListenerList();
            for( int i = listeners.length - 2; i >= 0; i -= 2 )
            {
                if( listeners[i] == TransactionListener.class )
                    ( (Transactable)tab ).addTransactionListener((TransactionListener)listeners[i + 1]);
            }
        }
    }

    protected void removeTab(ViewPart tab)
    {
        int index = tabs.indexOf(tab);

        tabs.removeElementAt(index);
        tabPane.removeTabAt(index);

        if( tab instanceof Transactable )
        {
            Object[] listeners = listenerList.getListenerList();
            for( int i = listeners.length - 2; i >= 0; i -= 2 )
            {
                if( listeners[i] == TransactionListener.class )
                    ( (Transactable)tab ).removeTransactionListener((TransactionListener)listeners[i + 1]);
            }
        }
    }

    protected void selectTab(int i)
    {
        tabPane.setSelectedIndex(i);
        updateTab();
    }

    public void setSelectedIndex(Object model, ViewPart tab)
    {
        this.model = model;
        int index = tabs.indexOf(tab);
        tabPane.setSelectedIndex(index);
    }

    public void selectTab(ViewPart tab)
    {
        int index = tabs.indexOf(tab);
        currentTab.onClose();
        tabPane.setSelectedIndex(index);
        updateTab();
    }

    public ViewPart getSelectedTab()
    {
    	return currentTab;
    }
    
    protected void updateTab()
    {
        int index = tabPane.getSelectedIndex();

        if( index < 0 )
            return;

        ViewPart oldTab = currentTab;
      
        currentTab = tabs.get(index);

        if (oldTab != null && oldTab != currentTab)
            oldTab.onClose();
        
        if( tabPane.getSelectedIndex() >= 0 )
        {
            removeActions(tabActions, tabActionsIndex);
            removeNotActionComponents();
        }

        tabActions = currentTab.getActions();
        addActions(tabActions, tabActionsIndex);

        modelActionsIndex = tabActionsIndex + 1 + ( tabActions == null ? 0 : tabActions.length );

        if( ( model != null ) && currentTab.canExplore(model) )
        {
            currentTab.explore(model, document);
        }
    }
    
    public void updateActions()
    {
        if( tabPane.getSelectedIndex() >= 0 )
        {
            removeActions(tabActions, tabActionsIndex);
            removeNotActionComponents();
        }

        tabActions = currentTab.getActions();
        addActions(tabActions, tabActionsIndex);

        modelActionsIndex = tabActionsIndex + 1 + ( tabActions == null ? 0 : tabActions.length );
    }

    protected void configureButton(AbstractButton button, Action action)
    {
        button.setAlignmentY(0.5f);

        Dimension btnSize = new Dimension(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE);
        button.setSize(btnSize);
        button.setPreferredSize(btnSize);
        button.setMinimumSize(btnSize);
        button.setMaximumSize(btnSize);

        if( button.getIcon() != null )
            button.setText(null);
        else
            button.setText((String)action.getValue(Action.NAME));
    }

    protected void addActions(Action[] actions, int index)
    {
        if( actions != null )
        {
            for( int i = 0; i < actions.length; i++ )
            {
                Action action = actions[i];
                if( action instanceof SeparatorAction )
                {
                    JToolBar.Separator separator = new JToolBar.Separator((Dimension)action.getValue(SeparatorAction.DIMENSION));
                    toolbar.add(separator, index + i);
                }
                else
                {
                    JButton button = new JButton(action);
                    configureButton(button, action);
                    toolbar.add(button, index + i);
                }
            }
        }
    }

    protected void removeActions(Action[] actions, int index)
    {
        if( actions != null )
        {
            try
            {
                for( Action action2 : actions )
                    toolbar.remove(index);
            }
            catch( Throwable t )
            {
            }

            toolbar.repaint();
        }
    }

    protected void removeNotActionComponents()
    {
        for( Component component : toolbar.getComponents() )
        {
            if( ! ( component instanceof JButton ) && ! ( component instanceof JToolBar.Separator ) )
            {
                toolbar.remove(component);
            }
        }
    }

    protected ActionsProvider modelActionProvider;

    public ActionsProvider getModelActionProvider()
    {
        return modelActionProvider;
    }

    public void setModelActionProvider(ActionsProvider provider)
    {
        this.modelActionProvider = provider;
    }
}
