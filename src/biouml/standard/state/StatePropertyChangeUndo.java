package biouml.standard.state;


import java.beans.PropertyChangeEvent;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import ru.biosoft.util.BeanUtil;

/**
 * State property change edit.
 * When state edit is undone we would like roll back to the value which was just before state was set rather then value which was at the moment of state creation<br>
 * E.g. user has created state in which initial value of variable x was changed from "0" (default) to "5"<br>
 * Then user restores diagram, works with it and sets x initial value to "6". Then he applies state created previously and restores again. Initial value is back at "0".<br>
 * To avoid this situation, when state is applied (state edit is redone), we store old value which is "6". After state is undone, this old value is restored. 
 * @author Ilya
 */
@SuppressWarnings ( "serial" )
public class StatePropertyChangeUndo extends AbstractUndoableEdit
{
    private Object oldValue;
    private final PropertyChangeEvent pce;

    public StatePropertyChangeUndo(PropertyChangeEvent pce)
    {
        this.pce = pce;
        this.oldValue = pce.getOldValue();
    }
    public StatePropertyChangeUndo(Object source, String propertyName, Object oldValue, Object newValue)
    {
        this( new PropertyChangeEvent( source, propertyName, oldValue, newValue ) );
    }

    @Override
    public void undo() throws CannotUndoException
    {
        try
        {
            super.undo();
            BeanUtil.setBeanPropertyValue( pce.getSource(), pce.getPropertyName(), oldValue );
        }
        catch( Exception e )
        {
            throw new CannotUndoException();
        }
    }

    @Override
    public void redo() throws CannotRedoException
    {
        try
        {
            oldValue = BeanUtil.getBeanPropertyValue( pce.getSource(), pce.getPropertyName() );
            super.redo();
            BeanUtil.setBeanPropertyValue( pce.getSource(), pce.getPropertyName(), pce.getNewValue() );
        }
        catch(Exception e)
        {
            //throw new CannotRedoException();
        }
    }

    @Override
    public String getPresentationName()
    {
        return "Change property '" + pce.getPropertyName()  + "' of '" + pce.getSource()+"'";
    }

    public String getPropertyName()
    {
        return pce.getPropertyName();
    }
    
    public Object getNewValue()
    {
        return pce.getNewValue();
    }
    
    public Object getOldValue()
    {
        return pce.getOldValue();
    }
    
    public Object getSource()
    {
        return pce.getSource();
    }
}
