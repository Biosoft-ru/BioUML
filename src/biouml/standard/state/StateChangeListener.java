package biouml.standard.state;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.List;

import javax.swing.undo.UndoableEdit;

import java.util.logging.Logger;

import ru.biosoft.access.core.undo.DataCollectionAddUndo;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.undo.DataCollectionRemoveUndo;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.BeanUtil;
import biouml.model.DiagramElement;
import biouml.model.Edge;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.undo.TransactionListener;

public class StateChangeListener implements PropertyChangeListener, DataCollectionListener, TransactionListener
{
    protected State state;
    private StateUndoManager undoManager;

    protected boolean lock = false;

    protected static final Logger log = Logger.getLogger(StateChangeListener.class.getName());

    public StateChangeListener(State state)
    {
        super();
        this.undoManager = state.getStateUndoManager();
        this.state = state;
    }

    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        if( lock )
            return;
        lock = true;
        try
        {
            if( pce.getPropagationId() != pce.getSource() && pce.getPropagationId() instanceof Option && pce.getSource() instanceof Option )
            {
                Option parent = ((Option)pce.getSource()).getParent();
                if (parent == null)
                    parent = (Option)pce.getPropagationId();
//                Option parent = (Option)pce.getPropagationId();
                while(!(parent instanceof DiagramElement) && parent != null)
                {
                    parent = parent.getParent();
                }
                String propertyPath = BeanUtil.getPropertyPathFromRoot((Option)pce.getSource(), parent, pce.getPropertyName());
                if(propertyPath != null)
                    pce = new PropertyChangeEvent(parent, propertyPath, pce.getOldValue(), pce.getNewValue());
            }
            
            //During undo/redo operations all property changes are ignored
            if( pce.getSource() instanceof DataElement && !undoManager.isRedo() && !undoManager.isUndo() )
            {
                //Add all the edits except the change of the state
                if( !pce.getPropertyName().equals("currentStateName") )
                {
                    undoManager.addEdit(new StatePropertyChangeUndo(pce));
                }
                //The change of the state take into account only for subdiagrams inside composite diagram
                else if( ! ( (DataElement)pce.getSource() ).getCompletePath()
                        .equals( DataElementPath.create( state.getAttributes().getValueAsString( State.DIAGRAM_REF ) ) ) )
                {
                    undoManager.addEdit( new StatePropertyChangeUndo( pce ) );
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Exception while property change in state '" + state.getName() + "'", t);
        }
        lock = false;
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        if( lock )
            return;
        lock = true;
        DataElement element = e.getDataElement();
        if( element != null )
        {
            DataCollectionAddUndo undo = new DataCollectionAddUndo(element, e.getOwner());
            undoManager.addEdit(undo);
            //TODO: check this
            if(element instanceof Edge && ((DiagramElement)element).getRole() != null )
            {
                undoManager.addEdit(new StatePropertyChangeUndo(element, "role", null, ((DiagramElement)element).getRole()));
            }
        }
        lock = false;
    }

    protected DataElement elementToRemove = null;

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        elementToRemove = e.getDataElement();
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        if( lock )
            return;
        lock = true;
        // TODO: revive elementWasAdded to work with transactions
        if( elementToRemove != null /*&& !elementWasAdded(elementToRemove)*/ )
        {
            DataCollectionRemoveUndo undo = new DataCollectionRemoveUndo(elementToRemove, e.getOwner());
            undoManager.addEdit(undo);
        }
        lock = false;
    }

    //  elementChange event we should get as PropertyChangeEvent
    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        while( e.getPrimaryEvent() != null )
            e = e.getPrimaryEvent();

        if( e.getType() == DataCollectionEvent.ELEMENT_WILL_REMOVE )
            elementWillRemove(e);
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        while( e.getPrimaryEvent() != null )
            e = e.getPrimaryEvent();

        if( e.getType() == DataCollectionEvent.ELEMENT_ADDED )
            elementAdded(e);
        if( e.getType() == DataCollectionEvent.ELEMENT_REMOVED )
            elementRemoved(e);
    }

    protected boolean elementWasAdded(DataElement element)
    {
        lock = true;
        boolean result = false;
        List<UndoableEdit> edits = undoManager.getEditsFlat();
        for( int i = 0; i < edits.size(); i++ )
        {
            UndoableEdit ue = edits.get(i);
            if( ( ue instanceof DataCollectionAddUndo ) && ( (DataCollectionAddUndo)ue ).getDataElement() == element )
            {
                edits.remove(i);
                result = true;
                i--;
            }
            else if( result && ( ue instanceof StatePropertyChangeUndo ) && ( (StatePropertyChangeUndo)ue ).getSource() == element )
            {
                edits.remove(i);
                i--;
            }
        }
        lock = false;
        return result;
    }

    @Override
    public void startTransaction(TransactionEvent te)
    {
        state.startTransaction(te);
    }

    @Override
    public boolean addEdit(UndoableEdit ue)
    {
        return true;
    }

    @Override
    public void completeTransaction()
    {
        state.completeTransaction();
    }
}
