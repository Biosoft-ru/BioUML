package ru.biosoft.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.beans.undo.Transaction;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.undo.TransactionListener;


public class DocumentTransactionUndoManager extends UndoManager implements TransactionListener
{
    private Transaction currentTransaction;
    private final List<ChangeListener> listeners = new ArrayList<>();

    protected Transaction createTransaction(TransactionEvent te)
    {
        return new Transaction(te);
    }

    public boolean hasTransaction()
    {
        return currentTransaction != null;
    }

    ////////////////////////////////////////////////////////////////////////////
    // TransactionListener interface implementation
    //

    @Override
    public void startTransaction(TransactionEvent te)
    {
        if( !isUndo && !isRedo )
        {
            // complete previous transaction if it is not completed
            if( currentTransaction != null && !currentTransaction.isEmpty() )
                completeTransaction();

            currentTransaction = createTransaction(te);
            fireChange();
        }
    }

    @Override
    public boolean addEdit(UndoableEdit ue)
    {
        if( !isUndo && !isRedo )
        {
            if( currentTransaction != null )
            {
                return currentTransaction.addEdit(ue);
            }
        }

        return false;
    }

    @Override
    public void completeTransaction()
    {
        if( !isUndo && !isRedo )
        {
            if( currentTransaction != null )
            {
                currentTransaction.end();
                super.addEdit(currentTransaction);
                currentTransaction = null;
                fireChange();
            }
        }
    }

    protected boolean isUndo = false;
    public boolean isUndo()
    {
        return isUndo;
    }
    
    @Override
    public synchronized void undo() throws CannotUndoException
    {
        isUndo = true;
        if( isInProgress() )
        {
            UndoableEdit edit = editToBeUndone();
            if( edit == null )
            {
                throw new CannotUndoException();
            }
            undoTo(edit);
        }
        else
        {
            super.undo();
        }
        fireChange();
        isUndo = false;
    }

    protected boolean isRedo = false;
    public boolean isRedo()
    {
        return isRedo;
    }
    
    @Override
    public synchronized void redo() throws CannotRedoException
    {
        isRedo = true;
        if( isInProgress() )
        {
            UndoableEdit edit = editToBeRedone();
            if( edit == null )
            {
                throw new CannotRedoException();
            }
            redoTo(edit);
        }
        else
        {
            super.redo();
        }
        fireChange();
        isRedo = false;
    }
    
    @Override
    public synchronized String getUndoPresentationName()
    {
        if (isInProgress()) {
            if (canUndo()) {
                String name = editToBeUndone().getPresentationName();
                if (!"".equals(name)) {
                    name = UIManager.getString("AbstractUndoableEdit.undoText") +
                        " " + name;
                } else {
                    name = UIManager.getString("AbstractUndoableEdit.undoText");
                }
                return name;
            } else {
                return UIManager.getString("AbstractUndoableEdit.undoText");
            }
        } else {
            return super.getUndoPresentationName();
        }
    }

    @Override
    public synchronized String getRedoPresentationName()
    {
        if (isInProgress()) {
            if (canRedo()) {
                String name = editToBeRedone().getPresentationName();
                if (!"".equals(name)) {
                    name = UIManager.getString("AbstractUndoableEdit.redoText") +
                        " " + name;
                } else {
                    name = UIManager.getString("AbstractUndoableEdit.redoText");
                }
                return name;
            } else {
                return UIManager.getString("AbstractUndoableEdit.redoText");
            }
        } else {
            return super.getRedoPresentationName();
        }
    }

    protected void fireChange()
    {
        ChangeEvent e = new ChangeEvent( this );
        for(ChangeListener listener : listeners.toArray( new ChangeListener[listeners.size()] ))
        {
            listener.stateChanged( e );
        }
    }

    public void addChangeListener(ChangeListener listener)
    {
        listeners.add( listener );
    }

    public void removeChangeListener(ChangeListener listener)
    {
        listeners.remove( listener );
    }
}
