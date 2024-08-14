package biouml.workbench.diagram;

import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.AbstractAction;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.undo.Transaction;

import biouml.model.Diagram;
import biouml.model.dynamics.Variable;
import biouml.standard.state.DiagramStateUtility;
import biouml.standard.state.State;
import biouml.standard.state.StatePropertyChangeUndo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathDialog;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.datatype.DataType;

abstract public class SetInitialValuesAction extends AbstractAction
{
    public static final String KEY = "set initial values";
    public static final String VALUE_COLUMN = "Value";

    abstract protected void setValue(DataElement de, double value);
    abstract protected @Nonnull Iterator<DataElement> getElementsIterator();

    protected Logger log = Logger.getLogger(SetInitialValuesAction.class.getName());

    public SetInitialValuesAction()
    {
        super(KEY);
    }

    public SetInitialValuesAction(Logger log)
    {
        super(KEY);
        this.log = log;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        try
        {
            DataElementPathDialog dialog = new DataElementPathDialog( "Select a table with initial values or a diagram state" );
            dialog.setElementMustExist( true );
            dialog.setMultiSelect( false );
            dialog.setElementClass( TableDataCollection.class, State.class );
            dialog.setValue( (DataElementPath)null );
            if( dialog.doModal() )
            {
                if( dialog.getValue().getDataElement() instanceof TableDataCollection )
                {
                    setValues( (TableDataCollection)dialog.getValue().getDataElement() );
                }
                else if( dialog.getValue().getDataElement() instanceof State )
                {
                    setValues( (State)dialog.getValue().getDataElement() );
                }
            }
        }
        catch( Exception exc )
        {
            log.log( Level.SEVERE, "SetInitialValuesAction: can not set initial values.", exc );
        }
    }

    public void setValues(TableDataCollection tdc) throws Exception
    {
        if( tdc.getColumnModel().hasColumn( VALUE_COLUMN ) )
        {
            if( tdc.getColumnModel().getColumn( VALUE_COLUMN ).getType().equals( DataType.Float ) )
            {
                Iterator<DataElement> it = getElementsIterator();
                while( it.hasNext() )
                {
                    DataElement de = it.next();
                    if( tdc.contains( de.getName() ) )
                    {
                        setValue( de, (double)tdc.get( de.getName() ).getValue( VALUE_COLUMN ) );
                    }
                    else
                    {
                        log.log( Level.INFO, "The table '" + tdc.getName() + "' does not contain a row with id '" + de.getName()
                                + "'. Thus, the value for '" + de.getName() + "' remains the same." );
                    }
                }
            }
            else
            {
                log.log( Level.WARNING, "The column '" + VALUE_COLUMN + "' in the table '" + tdc.getName()
                        + "' must be of the type 'Float'." );
            }
        }
        else
        {
            log.log( Level.WARNING, "The table '" + tdc.getName() + "' must contain the column '" + VALUE_COLUMN
                    + "' including new initial values to be set." );
        }
    }

    private void setValues(State st)
    {
        Iterator<DataElement> it = getElementsIterator();
        while( it.hasNext() )
        {
            DataElement de = it.next();
            Double newValue = findValue(st, de.getName());
            if( newValue != null )
            {
                setValue( de, newValue );
            }
            else
            {
                log.log( Level.INFO, "The state '" + st.getName() + "' does not change a value for '" + de.getName()
                        + "'. Thus, the value for '" + de.getName() + "' remains the same." );
            }
        }
    }

    private Double findValue(State st, String paramName)
    {
        return findValue( st.getStateUndoManager().getEdits(), DiagramStateUtility.getNativeDiagram(st), paramName );
    }

    private Double findValue(List<UndoableEdit> edits, Diagram diagram, String paramName)
    {
        for( UndoableEdit edit : edits )
        {
            if(edit instanceof Transaction)
            {
                Double value = findValue( ((Transaction)edit).getEdits(), diagram, paramName );
                if(value != null)
                    return value;
            }
            else
            {
                if(edit instanceof StatePropertyChangeUndo)
                {
                    Property property = ComponentFactory.getModel( diagram ).findProperty( ( (StatePropertyChangeUndo)edit ).getPropertyName() );
                    if( property.getOwner() instanceof Variable )
                    {
                        Variable var = (Variable)property.getOwner();
                        if( var.getName().equals( paramName ) )
                            return (Double) ( (StatePropertyChangeUndo)edit ).getNewValue();
                    }
                }
            }
        }
        return null;
    }
}
