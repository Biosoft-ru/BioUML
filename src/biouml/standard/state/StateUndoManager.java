package biouml.standard.state;

import java.util.List;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.undo.DataCollectionRemoveUndo;

import com.developmentontheedge.beans.undo.TransactionUndoManager;

@SuppressWarnings ( "serial" )
public class StateUndoManager extends TransactionUndoManager
{
    private static final Logger log = Logger.getLogger(StateUndoManager.class.getName());
    protected boolean canUndo;
    protected boolean canRedo;

    protected boolean isUndo;
    protected boolean isRedo;
    
    public StateUndoManager()
    {
        canUndo = false;
        canRedo = false;
        isUndo = false;
        isRedo = false;
        setLimit(Integer.MAX_VALUE);
    }

    @Override
    public synchronized boolean addEdit(UndoableEdit anEdit)
    {
        canUndo = true;
        if( currentTransaction != null && currentTransaction.getEdits().size() > 0 )
        {
            UndoableEdit lastEdit = currentTransaction.getEdits().get(currentTransaction.getEdits().size() - 1);
            if( lastEdit instanceof StatePropertyChangeUndo && anEdit instanceof StatePropertyChangeUndo )
            {
                StatePropertyChangeUndo first = (StatePropertyChangeUndo)anEdit;
                StatePropertyChangeUndo second = (StatePropertyChangeUndo)lastEdit;
                if( first.getSource().equals(second.getSource()) && first.getPropertyName().equals(second.getPropertyName())
                        && first.getNewValue().equals( second.getNewValue() )
                        && ( ( first.getOldValue() == null && second.getOldValue() == null )
                                || first.getOldValue().equals( second.getOldValue() ) ) )
                    return false;
            }
        }
        return super.addEdit(anEdit);
    }

    @Override
    public synchronized void undo() throws CannotUndoException
    {
        completeTransaction();
        isUndo = true;
        for( int i = edits.size() - 1; i >= 0; i-- )
        {
            UndoableEdit edit = edits.elementAt(i);
            if( edit == null )
            {
                log.warning("Undoing state: edit#"+i+" is absent. Result might be incorrect.");
            }
            else if( edit.canUndo() )
            {
                try
                {
                    edit.undo();
                }
                catch( CannotUndoException e )
                {
                    log.warning("Undoing state: edit#"+i+" ("+edit.getPresentationName()+") was failed to undo. Edit was deleted.");
                }
            }
        }
        if( edits.size() > 0 )
        {
            canRedo = true;
        }
        canUndo = false;
        isUndo = false;
    }

    @Override
    public synchronized void redo() throws CannotRedoException
    {
        completeTransaction();
        isRedo = true;
        for( int i = 0; i < edits.size(); i++ )
        {
            UndoableEdit edit = edits.elementAt(i);
            if( edit == null )
            {
                log.warning("Redoing state: edit#"+i+" is absent. Result might be incorrect.");
            }
            else if( edit.canRedo() )
            {
                try
                {
                    edit.redo();
                }
                catch( CannotRedoException e )
                {
                    log.warning("Redoing state: edit#"+i+" ("+edit.getPresentationName()+") was failed to redo. Edit was deleted.");
                    edits.remove(i);
                    i--;
                }
            }
            else
            {
                edits.remove(i);
                i--;
            }
        }
        if( edits.size() > 0 )
        {
            canUndo = true;
        }
        canRedo = false;
        isRedo = false;
    }
    
    public synchronized void undoDeleted() throws CannotUndoException
    {
        List<UndoableEdit> edits = getEditsFlat();
        for( int i = edits.size() - 1; i >= 0; i-- )
        {
            try
            {
                UndoableEdit edit = edits.get(i);
                if( edit == null ) continue;
                if( edit.getClass().getName().equals(DataCollectionRemoveUndo.class.getName()) )
                {
                    edit.undo();
                }
            }
            catch( Exception e )
            {
            }
        }
    }

    public synchronized void redoDeleted() throws CannotRedoException
    {
        List<UndoableEdit> edits = getEditsFlat();
        for( int i = 0; i < edits.size(); i++ )
        {
            try
            {
                UndoableEdit edit = edits.get(i);
                if( edit == null ) continue;
                if( edit.getClass().getName().equals(DataCollectionRemoveUndo.class.getName()) )
                {
                    edit.redo();
                }
            }
            catch( Exception e )
            {
            }
        }
    }

    @Override
    public synchronized boolean canRedo()
    {
        return canRedo;
    }

    @Override
    public synchronized boolean canUndo()
    {
        return canUndo;
    }

    public synchronized boolean isRedo()
    {
        return isRedo;
    }

    public synchronized boolean isUndo()
    {
        return isUndo;
    }
    
    public List<UndoableEdit> getEditsFlat()
    {
        StreamEx<UndoableEdit> edits = StreamEx.of( getEdits() ).flatMap( TransactionUtils::editsFlat );
        if(currentTransaction != null)
            edits = edits.append( TransactionUtils.editsFlat( currentTransaction ) );
        return edits.toList();
    }

    /**
     * Remove specific edit
     * @param i - number of edit (zero-based)
     */
    public void removeEdit(int i)
    {
        edits.remove(i);
    }
}
