package ru.biosoft.workbench.editors;

import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import one.util.streamex.StreamEx;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.repository.JSONSerializable;
import ru.biosoft.util.TextUtil2;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

/**
 * ComboBox which can contain different sets of values
 * It's implementation is non-trivial due to difficulty of passing to BE Editor any information except class name
 * This problem was workarounded by passing additional information in the value
 * @see ru.biosoft.analysis.CELNormalizationParameters for usage example
 */
public class GenericComboBoxEditor extends CustomEditorSupport implements JSONSerializable
{
    private JComboBox<String> comboBox = new JComboBox<>();
    boolean refresh = false;

    private void setRefresh(Component parent)
    {
        // As getting list of items may be somewhat slow and it's called really often,
        // we will try to refresh only once per dialog
        refresh = true;
        if( parent instanceof JComponent )
        {
            String editorKey = this.getClass().getName() + "." + getDescriptor().getName();
            Object editor = ( (JComponent)parent ).getClientProperty(editorKey);
            if( editor != null )
                refresh = false;
            ( (JComponent)parent ).putClientProperty(editorKey, this);
        }
    }

    /**
     * Creates Editor or Renderer component
     * 
     * @param parent
     * @return Editor or Renderer component
     */
    private Component createComponent(Component parent)
    {
        setRefresh(parent);
        comboBox = new JComboBox<>();

        GenericComboBoxItem value = getValueItem();
        if( value == null )
            return comboBox;

        String[] vals = getTags();
        refresh = false;
        if( vals == null )
            return comboBox;
        for( String str : vals )
            comboBox.addItem(str);

        comboBox.setSelectedItem(value.toString());

        final String key = value.getKey();
        comboBox.addActionListener(ae -> doSet(key));

        return comboBox;
    }

    private GenericComboBoxItem getValueItem()
    {
        Object valueObj = getValue();
        if( valueObj == null )
            return null;
        GenericComboBoxItem value = null;
        if( valueObj instanceof GenericComboBoxItem )
        {
            value = (GenericComboBoxItem)valueObj;
        }
        else
        {
            Object valueStr = valueObj;
            String key = TextUtil2.nullToEmpty( (String)getDescriptor().getValue("key") );

            Object[] availVals = getAvailableValues();
            if( availVals == null )
                value = getDescriptor().getValue("tagList") == null ? new GenericComboBoxItem(key, valueStr) : new GenericComboBoxItem(key,
                        valueStr, (Object[])getDescriptor().getValue("tagList"));
            else
                value = new GenericComboBoxItem(key, valueStr, availVals);
        }
        return value;
    }

    @Override
    public void setAsText(String newValue) throws IllegalArgumentException
    {
        GenericComboBoxItem newValueItem = null;
        GenericComboBoxItem valueItem = getValueItem();
        Object[] availableValues = valueItem == null ? getAvailableValues() : valueItem.getAvailableValues();
        for(Object value: availableValues)
        {
            if(value.toString().equals(newValue))
                newValueItem = new GenericComboBoxItem(valueItem == null ? null : valueItem.getKey(), newValue, availableValues);
        }
        if(newValueItem != null)
        {
            if( getValue() instanceof GenericComboBoxItem )
                setValue(newValueItem);
            else
                setValue(newValueItem.getValue());
        }
    }

    @Override
    public String[] getTags()
    {
        Object[] vals = null;
        
        GenericComboBoxItem value = getValueItem();
        if( value != null )
        {
            if( refresh )
                value.updateAvailableValues();
            if( ! ( getValue() instanceof GenericComboBoxItem ) && value.getAvailableValues() != null )
                getDescriptor().setValue("tagList", value.getAvailableValues());
            vals = value.getAvailableValues();
        }else
            vals = getAvailableValues();
        
        if( vals == null )
            return null;
        
        return StreamEx.of( vals ).map( Object::toString ).toArray( String[]::new );
    }

    protected Object[] getAvailableValues()
    {
        return null;
    }

    private void doSet(String key)
    {
        if( getValue() instanceof GenericComboBoxItem )
            setValue(new GenericComboBoxItem(key, comboBox.getSelectedItem().toString(), getValueItem().getAvailableValues()));
        else
            setValue(new GenericComboBoxItem(key, comboBox.getSelectedItem().toString(), getValueItem().getAvailableValues()).getValue());
    }

    @Override
    public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    {
        setRefresh(parent);
        GenericComboBoxItem item = getValueItem();
        JLabel label = new JLabel(item == null ? "" : item.toString());
        return label;
    }

    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        return createComponent(parent);
    }

    @Override
    public void fromJSON(JSONObject input) throws JSONException
    {
        String newValue = input.optString("value");
        if( newValue != null && !newValue.equals("") )
        {
            setAsText(newValue);
        }
    }

    @Override
    public JSONObject toJSON() throws JSONException
    {
        JSONObject result = new JSONObject();
        Object value = getValue();
        if( value != null )
        {
            result.put("value", value.toString());
        }
        else
        {
            result.put("value", "");
        }
        JSONArray dictionary = new JSONArray();
        String[] tags = getTags();
        if( tags != null )
            for( String tag : tags )
                dictionary.put( tag );
        result.put("dictionary", dictionary);
        result.put("type", "code-string");
        return result;
    }
}