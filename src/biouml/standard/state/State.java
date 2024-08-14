package biouml.standard.state;

import java.util.logging.Level;
import java.util.List;

import javax.annotation.Nonnull;
import javax.swing.undo.UndoableEdit;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.undo.DataCollectionAddUndo;
import ru.biosoft.access.core.undo.DataCollectionRemoveUndo;
import ru.biosoft.access.core.DataElementPath;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Diagram;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.standard.type.Concept;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.undo.Transaction;
import com.developmentontheedge.beans.undo.TransactionEvent;

@PropertyName("state")
public class State extends Concept
{
    protected static final Logger log = Logger.getLogger(State.class.getName());
    protected static final String DIAGRAM_REF = "diagram";
    
    public State()
    {
        this(null, null, "Empty state");
    }
    
    public State(Diagram diagram, String name)
    {
        this(null, diagram, name);
    }

    public State(DataCollection<?> parent, Diagram diagram, String name)
    {
        super( parent, name );
        try
        {
            getAttributes().add( new DynamicProperty( State.DIAGRAM_REF, DataElementPath.class, DataElementPath.create( diagram ) ) );
        }
        catch( Exception ex )
        {

        }
    }
    
    /**
     * Create state from edits (possibly performed on another diagram)
     * @throws Exception
     */
    public State(DataCollection<?> parent, Diagram diagram, String name, List<UndoableEdit> edits) throws Exception
    {
        this( parent, diagram, name );
        diagram.addState( this );
        diagram.setStateEditingMode( this );
        boolean isEnabled = diagram.isNotificationEnabled();
        diagram.setNotificationEnabled( true );
        boolean isModelNotify = true;
        EModel emodel = null;
        if (diagram.getRole() instanceof EModel)
        {
            emodel = diagram.getRole(EModel.class);
            isModelNotify = emodel.isNotificationEnabled();
            emodel.setNotificationEnabled( true );
        }
        
        cloneEdits( (Diagram)parent, edits );
        diagram.setNotificationEnabled( isEnabled );
        if (emodel != null)
            emodel.setNotificationEnabled( isModelNotify );
        diagram.restore();
    }

    /** The object textual description. Can be text/plain or text/html. */
    private String description;

    @Override
    public String getDescription()
    {
        return description;
    }
    @Override
    public void setDescription(String description)
    {
        this.description = description;
    }

    protected String version;
    public String getVersion()
    {
        return version;
    }
    public void setVersion(String version)
    {
        this.version = version;
    }

    protected StateUndoManager stateUndoManager;
    public @Nonnull StateUndoManager getStateUndoManager()
    {
        if( stateUndoManager == null )
        {
            stateUndoManager = new StateUndoManager();
        }
        return stateUndoManager;
    }

    public State clone(Diagram diagram, String name) throws Exception
    {
        return clone(null, diagram , name);
    }
    
    public State clone(DataCollection<?> parent, Diagram diagram, String name) throws Exception
    {
        State state = new State(parent, diagram, name);
        state.setComment(getComment());
        state.setDescription(getDescription());
        state.setDate(getDate());
        state.setDatabaseReferences(getDatabaseReferences());
        state.setLiteratureReferences(getLiteratureReferences());
        state.setSynonyms(getSynonyms());
        state.setTitle(getTitle());
        state.setType(getType());
        state.setVersion(getVersion());

        if( diagram != null )
        {
            diagram.addState(state);
            diagram.setStateEditingMode(state);

            List<UndoableEdit> edits = getStateUndoManager().getEdits();

            boolean isNotificationEnabled = diagram.isNotificationEnabled();
            diagram.setNotificationEnabled(true);
            boolean isPropagationEnabled = diagram.isPropagationEnabled();
            diagram.setPropagationEnabled(true);

            boolean isModelNotify = true;
            EModel emodel = null;
            Role role = diagram.getRole();
            if (role instanceof EModel)
            {
                emodel = ((EModel)role);
                isModelNotify = emodel.isNotificationEnabled();
                emodel.setNotificationEnabled( true );
            }
            
            try
            {
                state.cloneEdits(diagram, edits);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not clone edits of the state '" + getName() + "'");
            }

            diagram.setNotificationEnabled(isNotificationEnabled);
            diagram.setPropagationEnabled(isPropagationEnabled);

            if (emodel != null)
                emodel.setNotificationEnabled( isModelNotify );
            
            diagram.restore();
        }

        return state;
    }

    protected void cloneEdits(Diagram diagram, List<UndoableEdit> edits) throws Exception
    {
        for( UndoableEdit edit : edits )
        {
            if( edit instanceof Transaction )
            {
                startTransaction(new TransactionEvent(this, edit.getPresentationName()));
                setTransactionComment( ( (Transaction)edit ).getComment());
                cloneEdits(diagram, ( (Transaction)edit ).getEdits());
                completeTransaction();
            }
            else if( edit instanceof DataCollectionRemoveUndo )
            {
                DiagramStateUtility.redoRemoveEdit(diagram, (DataCollectionRemoveUndo)edit);
            }
            else if( edit instanceof DataCollectionAddUndo )
            {
                DiagramStateUtility.redoAddEdit(diagram, (DataCollectionAddUndo)edit);
            }
            else if( edit instanceof StatePropertyChangeUndo )
            {
                DiagramStateUtility.redoPropertyChangeEdit(diagram, (StatePropertyChangeUndo)edit);
            }
        }
    }

    /**
     * Start new transaction (group of edits)
     * @param te
     */
    public void startTransaction(TransactionEvent te)
    {
        getStateUndoManager().startTransaction(te);
    }

    /**
     * Complete transaction
     */
    public void completeTransaction()
    {
        getStateUndoManager().completeTransaction();
    }
    /**
     * @param comment
     */
    public void setTransactionComment(String comment)
    {
        getStateUndoManager().setTransactionComment(comment);
    }
}
