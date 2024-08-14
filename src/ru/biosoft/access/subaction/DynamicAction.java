package ru.biosoft.access.subaction;

import java.awt.event.ActionEvent;
import java.beans.IntrospectionException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.access.exception.InvalidSelectionException;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.repository.DataElementPathDialog;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.gui.Document;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public abstract class DynamicAction extends AbstractAction implements Comparable<DynamicAction>
{
    //protected Logger log = Logger.getLogger(DynamicAction.class.getName());
    //TODO: jobcontrol logging check
    protected Logger log = Logger.getLogger( DynamicAction.class.getName() );
    public static final int SELECTED_ZERO = 0;
    public static final int SELECTED_ONE = 1;
    public static final int SELECTED_ANY = 2;
    public static final int SELECTED_ZERO_OR_ANY = 3;
    public static final int SELECTED_UNDEFINED = -1;

    public static final String ORDER_KEY = "Order";

    private Integer numSelected = SELECTED_UNDEFINED;
    private boolean acceptReadOnly = false;
    private String title;

    /**
     * Fast check whether current action is applicable for given model.
     * Will be used to check whether to show specified button or not.
     * You may omit some checks (if they are slow) moving them to isApplicableForRows.
     * In this case button will be shown, but not functional.
     * @param model - model to check against
     * @return true if action is applicable for model, false otherwise
     * @see isApplicableForRows
     */
    public boolean isApplicable(Object model)
    {
        return true;
    }

    /**
     * Slow check whether current action is applicable for given model and set of selected rows
     * Will be used after button is pressed.
     * @param model - model for which action is performed
     * @param selectedItems - iterator over selected rows
     * @throws LoggedException in case action is not applicable for parameters
     * @see isApplicable
     */
    public void validateParameters(Object model, List<DataElement> selectedItems) throws LoggedException
    {
        if(!isApplicable(model))
        {
            throw new ParameterNotAcceptableException("Document", String.valueOf(model));
        }
        checkNumSelected(selectedItems.size());
    }

    /**
     * Returns properties to show in dialog if it's necessary to show it after the action
     * Parameters may be used somehow to setup it. Note that it will be called only after success check isApplicableForRows
     * So you should not check here whether parameters are applicable
     * @param model - model for which action is performed
     * @param selectedItems - iterator over selected rows
     * @return bean containing properties or null if don't want the properties dialog to be shown
     * @see getTargetProperties
     */
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        return null;
    }

    /**
     * Perform actual action. This will be called after isApplicableForRows and after showing dialog if it's necessary.
     * You shouldn't use any swing UI code here as this action can be called for web also.
     * @param model - model for which action is performed
     * @param selectedItems - iterator over selected rows
     * @param properties - properties (returned by getProperties and optionally modified by user)
     */
    public abstract void performAction(Object model, List<DataElement> selectedItems, Object properties) throws Exception;

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        Document document = Document.getActiveDocument();

        if( document == null )
            return;
        final List<DataElement> selectedItems = document.getSelectedItems();
        document.applyEditorChanges();
        final Object model = document.getModel();
        try
        {
            validateParameters(model, selectedItems);
        }
        catch(Exception e)
        {
            ApplicationUtils.errorBox(e);
            //log.log(Level.SEVERE, e.getMessage(), e);
            log.log( Level.SEVERE, e.getMessage(), e );
            return;
        }
        final Object pd = getProperties(model, selectedItems);

        if(pd != null)
        {
            if(pd instanceof DynamicPropertySet && ((DynamicPropertySet)pd).size() == 1 && ((DynamicPropertySet)pd).getProperty("target") != null)
            {
                DataElementPathEditor editor = new DataElementPathEditor();
                DynamicProperty property = ((DynamicPropertySet)pd).getProperty("target");
                editor.setDescriptor(property.getDescriptor());
                editor.setValue(property.getValue());
                DataElementPathDialog dialog = editor.getDialog();
                if(dialog.doModal())
                {
                    property.setValue(dialog.getValue());
                } else
                    return;
            } else
            {
                CompositeProperty properties = ComponentFactory.getModel(pd);
                PropertyInspectorEx propertyInspector = new PropertyInspectorEx();
                propertyInspector.explore(properties);
                OkCancelDialog dialog = new OkCancelDialog(Application.getApplicationFrame(), "Properties");
                dialog.add(propertyInspector);
                if(!dialog.doModal()) return;
            }
        }
        else
        {
            String confirmation = getConfirmationMessage(model, selectedItems);
            if( confirmation != null )
            {
                int res = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), confirmation);
                if( res != JOptionPane.YES_OPTION )
                    return;
            }
        }
        try
        {
            performAction(model, selectedItems, pd);
        }
        catch( Exception e )
        {
            ApplicationUtils.errorBox(e);
            log.log( Level.SEVERE, e.getMessage(), e );
        }
    }

    /**
     * You may use this in your overridden getProperties to create properties containing only target selector
     * In this case only target selection dialog will be shown
     * @param targetClass - class of target ru.biosoft.access.core.DataElement
     * @param defValue - default path (usually the same as model)
     * @see getProperties
     */
    protected Object getTargetProperties(Class<? extends DataElement> targetClass, DataElementPath defValue)
    {
        try
        {
            DynamicPropertySet result = new DynamicPropertySetAsMap();
            PropertyDescriptorEx descriptor = DataElementPathEditor.registerOutput(new PropertyDescriptorEx("target", null, null), targetClass);
            descriptor.setDisplayName("Target path");
            descriptor.setShortDescription("Specify the path where to store the result");
            result.add(new DynamicProperty(descriptor, DataElementPath.class, defValue));
            return result;
        }
        catch(IntrospectionException e)
        {
            log.log( Level.SEVERE, e.getMessage(), e );
            return null;
        }
    }

    void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    protected void checkNumSelected ( int selected )
    {
        InvalidSelectionException.checkSelection(numSelected, selected);
    }

    public Integer getNumSelected()
    {
        return numSelected;
    }

    protected void setNumSelected(Integer numSelected)
    {
        if(numSelected > 2)
            this.numSelected = SELECTED_ZERO_OR_ANY;
        else if(numSelected < 0)
            this.numSelected = SELECTED_UNDEFINED;
        else
            this.numSelected = numSelected;
    }

    /**
     * Returns confirmation message
     * Method will be called if getProperies method returned null
     * @return confirmation string
     */
    public String getConfirmationMessage(Object model, List<DataElement> selectedItems)
    {
        return null;
    }

    /**
     * @return the acceptReadOnly (whether action is applicable for read-only object)
     */
    public boolean isAcceptReadOnly()
    {
        return acceptReadOnly;
    }

    /**
     * @param acceptReadOnly the acceptReadOnly to set (whether action is applicable for read-only object)
     */
    protected void setAcceptReadOnly(boolean acceptReadOnly)
    {
        Object oldValue = this.acceptReadOnly;
        this.acceptReadOnly = acceptReadOnly;
        firePropertyChange("acceptReadOnly", oldValue, acceptReadOnly);
    }

    /**
     * @param action
     * @return true if action belongs to the same group as this action
     */
    public boolean isSameGroup(DynamicAction action)
    {
        String key = getOrderKey();
        String otherKey = action.getOrderKey();
        int firstDash = key.indexOf( '-' );
        int secondDash = key.indexOf( '-', firstDash+1 );
        if(secondDash == -1)
            secondDash = firstDash;
        if(secondDash == -1)
            secondDash = 0;
        return otherKey.startsWith( key.substring( 0, secondDash ) );
    }

    public String getOrderKey()
    {
        return getValue( ORDER_KEY )+"-"+getValue( ACTION_COMMAND_KEY );
    }

    @Override
    public int compareTo(DynamicAction o)
    {
        return getOrderKey().compareTo( o.getOrderKey() );
    }
}
