package ru.biosoft.gui;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.undo.UndoableEdit;

import ru.biosoft.exception.ExceptionRegistry;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.application.action.ApplicationAction;

public class EditorsManager implements ViewPart
{
    protected PropertyInspector propertyInspector = new PropertyInspector();
    protected EditorsTabbedPane tabbedPane;

    public EditorsManager(EditorsTabbedPane tabbedPane)
    {
        action = new ApplicationAction(ru.biosoft.gui.resources.MessageBundle.class, EditorsTabbedPane.ACTION_NAME);

        this.tabbedPane = tabbedPane;

        editors = new DynamicPropertySetSupport();
        editors.addPropertyChangeListener(
            evt -> {
                EditorInfo info = editorsInfo.get(evt.getPropertyName());
                if( info != null )
                {
                    info.enabled = (Boolean)evt.getNewValue();
                    validateTab(info.viewPart);
                }
            });
    }

    ////////////////////////////////////////////////////////////////////////////
    //  DynamicPropertySet issues
    //

    protected HashMap<Object, EditorInfo> editorsInfo = new HashMap<>();
    protected DynamicPropertySetSupport editors;

    public DynamicPropertySet getEditors()
    {
        return editors;
    }
    public void setEditors(DynamicPropertySet dps)
    {
        // do nothing, this method essentila to allow edit dynamic properties.
    }

    protected void addDynamicProperty(int index, ViewPart viewPart, boolean isEnabled)
    {
        try
        {
            Action action = viewPart.getAction();
            String baseName = (String)action.getValue(Action.NAME);
            String name = baseName;
            int i=0;
            while(editors.getProperty( name ) != null)
                name = baseName+" ("+(++i)+")";
            DynamicProperty property = new DynamicProperty(name, Boolean.class, isEnabled);

            if( action.getValue(Action.SHORT_DESCRIPTION) instanceof String )
            {
                String descr = (String)action.getValue(Action.SHORT_DESCRIPTION);
                property.getDescriptor().setShortDescription(descr);
            }
            editors.add(index, property);

            EditorInfo info = new EditorInfo(name, viewPart, isEnabled);
            editorsInfo.put(name, info);
            editorsInfo.put(viewPart, info);
        }
        catch(Throwable t)
        {
            ExceptionRegistry.log(t);
        }
    }

    protected static class EditorInfo
    {
        String   name;
        ViewPart viewPart;
        Boolean  enabled;

        public EditorInfo(String name, ViewPart viewPart, Boolean enabled)
        {
            this.name     = name;
            this.viewPart = viewPart;
            this.enabled  = enabled;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // ViewPart issues
    //

    @Override
    public JComponent getView()
    {
        return propertyInspector;
    }

    protected Action action;
    @Override
    public Action getAction()
    {
        return action;
    }

    @Override
    public Object getModel()
    {
        return ViewPart.STATIC_VIEW;
    }

    @Override
    public Document getDocument()
    {
        return null;
    }

    /** pending: stub. */
    @Override
    public Action[] getActions()
    {
        return null;
    }

    @Override
    public boolean canExplore(Object model)
    {
        return true;
    }

    @Override
    public void explore(Object model, Document document)
    {
        if( propertyInspector.getBean() == null )
        {
            propertyInspector.explore(this);
            propertyInspector.expandAll(true);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Tabs managment issues
    //

    public boolean hideInappropriateTabs = true;
    public boolean getHideInappropriateTabs()
    {
        return hideInappropriateTabs;
    }
    public void setHideInappropriateTabs(boolean hideInappropriateTabs)
    {
        this.hideInappropriateTabs = hideInappropriateTabs;
        validateTabs(getModel());
    }

    protected ArrayList<ViewPart> views = new ArrayList<>();
    public void addViewPart(ViewPart viewPart, boolean enabled)
    {
        int index = 0;
        float priority = getPriority(viewPart);
        for(int i=0; i<views.size(); i++)
        {
            ViewPart view = views.get(i);
            if( priority <= getPriority(view) )
                break;

            index++;
        }

        addDynamicProperty(index, viewPart, enabled);
        views.add(index, viewPart);
        validateTab(viewPart);
    }

    public String getViewPartId(String name)
    {
        try
        {
            return (String)editorsInfo.get(name).viewPart.getAction().getValue("id");
        }
        catch( Exception e )
        {
            return "";
        }
    }

    public void validateTabs(Object model)
    {
        for( ViewPart view : views )
        {
            validateTab(view);
        }
    }

    public void validateTab(ViewPart viewPart)
    {
        boolean canExplore = viewPart.canExplore(tabbedPane.getModel());
        int index = tabbedPane.tabs.indexOf(viewPart);
        boolean enabled = true;
        EditorInfo info = editorsInfo.get(viewPart);
        if( info != null )
            enabled = info.enabled.booleanValue();

        // process enabled/disabled
        if( !enabled )
        {
            if( index != -1 )
                tabbedPane.removeTab(viewPart);

            return;
        }

        // process canExplore

        if( canExplore && index == -1 )
            insertView(viewPart);

        if( hideInappropriateTabs )
        {
            if( !canExplore && index != -1 )
                tabbedPane.removeTab(viewPart);
        }
        else
        {
            if( index == -1 )
            {
                insertView(viewPart);
                index = tabbedPane.tabs.indexOf(viewPart);
            }

            tabbedPane.tabPane.setEnabledAt(index, canExplore);
        }
    }

    protected void insertView(ViewPart viewPart)
    {
        int index = 0;
        float priority = getPriority(viewPart);
        for(int i=0; i<tabbedPane.tabs.size(); i++)
        {
            ViewPart view = tabbedPane.tabs.get(i);
            if( priority < getPriority(view) )
                break;

            index++;
        }

        tabbedPane.insertTab(index, viewPart);
    }

    public static float getPriority(ViewPart viewPart)
    {
        Action action = viewPart.getAction();
        try
        {
            Object priority = action.getValue(ViewPart.PRIORITY);
            if( priority instanceof Float )
                return ((Float)priority).floatValue();
        }
        catch(Throwable t)
        {
            System.out.println("Can not get priority for view '" + viewPart + ", error=" + t);
        }

        return ViewPart.DEFAULT_PRIORITY;
    }

    ////////////////////////////////////////////////////////////////////////////
    // TransactionListener - do nothing
    //

    @Override
    public void startTransaction(TransactionEvent te) { }
    @Override
    public boolean addEdit(UndoableEdit ue) { return false; }
    @Override
    public void completeTransaction() { }

    @Override
    public void modelChanged(Object model) { }

    @Override
    public void onClose() { }
}


