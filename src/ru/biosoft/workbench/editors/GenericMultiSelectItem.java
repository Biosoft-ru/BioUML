package ru.biosoft.workbench.editors;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GenericMultiSelectItem
{
    private String key = null;
    private Object[] availableValues;
    private Object[] values;
    private int[] indexes;
    
    protected GenericMultiSelectItem(String key, Object[] values, Object[] availableValues)
    {
        this.availableValues = availableValues;
        if(availableValues == null) return;
        this.key = key;
        if(values == null) return;

        selectValues( values );
    }
    
    public GenericMultiSelectItem(String key, Object[] values)
    {
        this(key, values, GenericEditorData.getAvailableValues(key));
    }
    
    public GenericMultiSelectItem(GenericMultiSelectItem oldValue, Object[] values)
    {
        this(oldValue.key, values);
    }
    
    protected String getKey()
    {
        return key;
    }
    
    protected int[] getIndexes()
    {
        return indexes;
    }
    
    public Object[] getAvailableValues()
    {
        return availableValues;
    }
    
    public void updateAvailableValues()
    {
        availableValues = GenericEditorData.getAvailableValues(key);
        Object[] values = getValues();
        selectValues( values );
    }
    
    public Object[] getValues()
    {
        if( availableValues == null )
            return new Object[] {};
        return values;
    }
    
    public String[] getStringValues()
    {
        if(availableValues == null || values == null) return new String[]{};
        String[] result = new String[values.length];
        for( int i = 0; i < values.length; i++ )
            result[i] = values[i].toString();
        return result;
    }
    
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o)
    {
        if(o == null || !(o instanceof GenericMultiSelectItem)) return false;
        GenericMultiSelectItem item = (GenericMultiSelectItem)o;
        if((key == null && item.key != null) || (key != null && !key.equals(item.key))) return false;
        if(!Arrays.equals( indexes, item.indexes )) return false;
        if(!Arrays.equals(availableValues, item.availableValues)) return false;
        return true;
    }
    
    private void selectValues(Object[] values)
    {
        if(availableValues == null) return;

        Map<String, Integer> valToIdx = new HashMap<>();
        for(int i = 0; i < availableValues.length; i++)
            valToIdx.put( availableValues[i].toString(), i );

        TIntList indexes = new TIntArrayList();
        for( int i = 0; i < values.length; i++)
        {
            Integer index = valToIdx.get( values[i].toString() );
            if(index != null)
                indexes.add(index);
        }
        this.indexes = indexes.toArray();
        
        this.values = (Object[])Array.newInstance( availableValues.getClass().getComponentType(), this.indexes.length );
        for(int i=0; i<this.indexes.length; i++)
        {
            this.values[i] = availableValues[this.indexes[i]];
        }
    }
}