package ru.biosoft.gui;

import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.undo.TransactionListener;

public class EditorPartSupport extends ViewPartSupport implements EditorPart
{
    @Override
    public void save()
    {}

    ////////////////////////////////////////////////////////////////////////////
    // Transactable interface implementation
    //

    @Override
    public void addTransactionListener(TransactionListener listener)
    {
        listenerList.add(TransactionListener.class, listener);
    }

    @Override
    public void removeTransactionListener(TransactionListener listener)
    {
        listenerList.remove(TransactionListener.class, listener);
    }

    protected void fireStartTransaction(TransactionEvent evt)
    {
        Object[] listeners = listenerList.getListenerList();
        for ( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if ( listeners[ i ] == TransactionListener.class )
                ( (TransactionListener) listeners[ i + 1 ] ).startTransaction(evt);
        }
    }

    protected void fireAddEdit(UndoableEdit ue)
    {
        Object[] listeners = listenerList.getListenerList();
        for ( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if ( listeners[ i ] == TransactionListener.class )
                ( (TransactionListener) listeners[ i + 1 ] ).addEdit(ue);
        }
    }

    protected void fireCompleteTransaction()
    {
        Object[] listeners = listenerList.getListenerList();
        for ( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if ( listeners[ i ] == TransactionListener.class )
                ( (TransactionListener) listeners[ i + 1 ] ).completeTransaction();
        }
    }

}
