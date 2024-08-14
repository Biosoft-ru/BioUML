package ru.biosoft.access.support;

import java.util.Collections;

import javax.swing.ComboBoxModel;
import javax.swing.MutableComboBoxModel;

import ru.biosoft.access.core.DataCollection;

public class NameListToComboBoxModelAdapter extends NameListToListModelAdapter implements ComboBoxModel<String>, MutableComboBoxModel<String>
{
    public NameListToComboBoxModelAdapter(DataCollection<?> dataCollection)
    {
        super(dataCollection);
    }

    ////////////////////////////////////////////////////////////////////////////
    // implements ComboBoxModel interface
    //

    private Object selectedItem;
    @Override
    public  Object getSelectedItem()
    {
        return selectedItem;
    }
    @Override
    public void setSelectedItem(Object anItem)
    {
        selectedItem = anItem;
    }

    ////////////////////////////////////////////////////////////////////////////
    // implements MutableComboBoxModel interface
    //

    /** Adds new element according to sorting order. */
    @Override
    public void addElement(String name)
    {
        int index =  Collections.binarySearch(nameList, name);
        if( index < 0 )
        {
            index = -(index+1);
            nameList.add(index, name);
    
            fireIntervalAdded(this, index, index);
            //System.out.print("Added: " + index);
        }
    }

    @Override
    public void insertElementAt(String name, int index)
    {
        nameList.add(index, name);
        fireIntervalAdded(this, index, index);
    }

    @Override
    public void removeElement(Object obj)
    {
        String name = String.valueOf(obj);
        int index = Collections.binarySearch(nameList, name);
        removeElementAt(index);
    }

    @Override
    public void removeElementAt(int index)
    {
        if( index >= 0 )
        {
            Object obj = nameList.remove(index);
            if( obj != null && obj.equals(selectedItem) )
                selectedItem = null;

            fireIntervalRemoved(this, index, index);
        }
    }

}
