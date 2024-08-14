package ru.biosoft.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * @author lan
 *
 */
public class ListComboBoxModel<T> implements ComboBoxModel<T>
{
    private List<ListDataListener> listeners = new ArrayList<>();
    private List<T> list;
    private Object selection;
    
    public ListComboBoxModel()
    {
        this.list = Collections.emptyList();
    }
    
    public ListComboBoxModel(List<T> list)
    {
        this.list = list;
        if(list.size() > 0) selection = list.get(0);
        else selection = null;
    }
    
    public void updateList(List<T> newList)
    {
        if(newList == null) newList = Collections.emptyList();
        if(!newList.equals(list))
        {
            list = newList;
            int index = list.indexOf(selection);
            if(index>=0) selection = list.get(index);
            else if(list.size() > 0) selection = list.get(0);
            else selection = null;
            for(ListDataListener l: listeners)
            {
                l.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, list.size()));
            }
        }
    }

    @Override
    public int getSize()
    {
        return list.size();
    }

    @Override
    public T getElementAt(int index)
    {
        return list.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l)
    {
        listeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l)
    {
        listeners.remove(l);
    }

    @Override
    public void setSelectedItem(Object anItem)
    {
        this.selection = anItem;
    }

    @Override
    public Object getSelectedItem()
    {
        return this.selection;
    }
}
