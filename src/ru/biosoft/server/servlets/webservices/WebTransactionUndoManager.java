package ru.biosoft.server.servlets.webservices;

import javax.swing.undo.UndoableEdit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.security.SecurityManager;
import biouml.model.Diagram;

import com.developmentontheedge.beans.undo.Transaction;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.undo.TransactionUndoManager;

public class WebTransactionUndoManager extends TransactionUndoManager
{
    private static class UserTransactionEvent extends TransactionEvent
    {
        protected String user;
    
        public UserTransactionEvent(Object source, String name, String user)
        {
            super(source, name);
            this.user = user;
        }
    
        public String getUser()
        {
            return user == null ? "?" : user;
        }
    }
    private Diagram diagram;
    private static final long serialVersionUID = 1L;

    public WebTransactionUndoManager(Diagram diagram)
    {
        this.diagram = diagram;
    }

    @Override
    protected TransactionEvent createTransactionEvent(String name)
    {
        return new UserTransactionEvent(this, name, SecurityManager.getSessionUser());
    }
    
    @Override
    public synchronized void startTransaction(TransactionEvent te)
    {
        if(!(te instanceof UserTransactionEvent))
        {
            te = createTransactionEvent(te.getName());
        }
        super.startTransaction(te);
    }

    @Override
    public boolean addEdit(UndoableEdit anEdit)
    {
        synchronized( this )
        {
            if(undoInProgress) return false;
            if(currentTransaction != null)
                return currentTransaction.addEdit(anEdit);
            if(anEdit instanceof Transaction)
            {
                return super.addEdit(anEdit);
            }
            startTransaction(createTransactionEvent(anEdit.getPresentationName()));
            currentTransaction.addEdit(anEdit);
            super.completeTransaction();
            json = null;
        }
        synchronized(diagram)
        {
            diagram.notifyAll();
        }
        return true;
    }

    @Override
    public void completeTransaction()
    {
        synchronized(this)
        {
            super.completeTransaction();
            json = null;
        }
        synchronized(diagram)
        {
            diagram.notifyAll();
        }
    }
    
    private JSONArray json = null;
    public synchronized JSONArray toJSON()
    {
        if(json == null)
        {
            UndoableEdit nextEdit = editToBeRedone();
            json = new JSONArray();
            for( UndoableEdit edit : getEdits() )
            {
                TransactionEvent te = ( (Transaction)edit ).getTransactionEvent();
                JSONObject transaction = new JSONObject();
                try
                {
                    transaction.put("user", te instanceof UserTransactionEvent ? ( (UserTransactionEvent)te ).getUser() : "?");
                    transaction.put("name", te.getName());
                    if(edit == nextEdit) transaction.put("next", true);
                }
                catch( JSONException e )
                {
                }
                json.put(transaction);
            }
        }
        return json;
    }

    @Override
    public UndoableEdit editToBeRedone()
    {
        return super.editToBeRedone();
    }
}