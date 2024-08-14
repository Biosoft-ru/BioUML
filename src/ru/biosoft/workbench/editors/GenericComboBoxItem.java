package ru.biosoft.workbench.editors;

import ru.biosoft.access.support.SerializableAsText;

/**
 * Item in GenericComboBoxEditor
 */
public class GenericComboBoxItem implements SerializableAsText
{
    private String key = null;
    private int index = 0;
    private Object value = "";
    private Object[] availableValues;
    
    protected GenericComboBoxItem(String key, Object value, Object[] availableValues)
    {
        this.availableValues = availableValues;
        this.key = key;
        if(availableValues == null || availableValues.length == 0) return;
        if(value != null)
        {
            for(int i=0; i<availableValues.length; i++)
            {
                if(availableValues[i].toString().equals(value.toString()))
                {
                    this.value = availableValues[i];
                    this.index = i;
                    return;
                }
            }
        }
        this.value = availableValues[0];
    }
    
    public GenericComboBoxItem(String key, Object value)
    {
        this(key, value, GenericEditorData.getAvailableValues(key));
    }
    
    public GenericComboBoxItem(String data)
    {
        this(data.substring(0, data.indexOf(":")), data.substring(data.indexOf(":")+1));
    }
    
    public GenericComboBoxItem(GenericComboBoxItem oldValue, String value)
    {
        this(oldValue.key, value);
    }
    
    public GenericComboBoxItem()
    {
    }
    
    protected String getKey()
    {
        return key;
    }
    
    protected int getIndex()
    {
        return index;
    }
    
    public Object[] getAvailableValues()
    {
        return availableValues;
    }
    
    public void updateAvailableValues()
    {
        availableValues = GenericEditorData.getAvailableValues(key);
        Object value = getValue();
        index = 0;
        this.value = availableValues != null && availableValues.length>0?availableValues[0]:"";
        if(availableValues == null || availableValues.length == 0) return;
        for(int i=0; i<availableValues.length; i++)
        {
            if(availableValues[i].toString().equals(value.toString()))
            {
                this.index = i;
                this.value = availableValues[i];
            }
        }
    }
    
    public Object getValue()
    {
        return value;
    }
    
    @Override
    public String toString()
    {
        return getValue().toString();
    }
    
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o)
    {
        if(o == null || !(o instanceof GenericComboBoxItem)) return false;
        GenericComboBoxItem item = (GenericComboBoxItem)o;
        if(item.index != index) return false;
        if((value == null && item.value != null) || (value != null && !value.equals(item.value))) return false;
        if((key == null && item.key != null) || (key != null && !key.equals(item.key))) return false;
        return true;
    }

    @Override
    public String getAsText()
    {
        return getKey()+":"+getValue();
    }
}