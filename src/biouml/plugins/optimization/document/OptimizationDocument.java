package biouml.plugins.optimization.document;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.EventListenerList;
import javax.swing.undo.UndoableEdit;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.beans.undo.PropertyChangeUndo;
import com.developmentontheedge.beans.undo.Transactable;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.undo.TransactionListener;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.optimization.MessageBundle;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationConstraint;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.ParameterConnection;
import biouml.plugins.optimization.SimulationTaskRegistry;
import biouml.plugins.optimization.diagram.OptimizationChangeListener;
import biouml.plugins.simulation.SimulationTaskParameters;
import biouml.workbench.diagram.DiagramDocument;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.RedoAction;
import ru.biosoft.gui.UndoAction;

@ClassIcon ( "resources/optimizationDocument.gif" )
public class OptimizationDocument extends Document implements PropertyChangeListener, Transactable
{
    private final TabularPropertyInspector tpi;

    public OptimizationDocument(Optimization optimization)
    {
        super(optimization);

        tpi = new TabularPropertyInspector();
        viewPane = new ViewPane();
        if( optimization != null )
        {
            update();
            viewPane.add(tpi);
            initListeners();
        }
    }

    private void initListeners()
    {
        List<Parameter> fParams = getOptimizationParameters().getFittingParameters();
        for( Parameter param : fParams )
            param.addPropertyChangeListener(this);

        List<OptimizationExperiment> experiments = getOptimizationParameters().getOptimizationExperiments();
        for( OptimizationExperiment exp : experiments )
        {
            exp.addPropertyChangeListener(this);
            for( ParameterConnection connection : exp.getParameterConnections() )
                connection.addPropertyChangeListener(this);
        }

        List<OptimizationConstraint> constraints = getOptimizationParameters().getOptimizationConstraints();
        for( OptimizationConstraint constr : constraints )
            constr.addPropertyChangeListener(this);

        getOptimization().addPropertyChangeListener(this);
        addTransactionListener(undoManager);

        Option parameters = getOptimization().getOptimizationMethod().getParameters();
        parameters.addPropertyChangeListener(this);

        OptimizationChangeListener changeListener = new OptimizationChangeListener(getOptimization(), this);
        if( changeListener.isDiagramChanged() )
        {
            String message = MessageBundle.getMessage("CHANGE_OPTIMIZATION_DIAGRAM");
            int res = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message);
            try
            {
                changeListener.changeDiagram(res != JOptionPane.YES_OPTION);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not change diagram for optimization document " + getOptimization().getName());
            }
        }
        getOptimization().addPropertyChangeListener(changeListener);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //
    public Optimization getOptimization()
    {
        applyEditorChanges();
        return (Optimization)getModel();
    }

    protected OptimizationParameters getOptimizationParameters()
    {
        return getOptimization().getParameters();
    }

    @Override
    public String getDisplayName()
    {
        Optimization optimization = getOptimization();
        Module module = Module.optModule(optimization);
        if( module != null )
        {
            return module.getName() + " : " + optimization.getName();
        }
        return optimization.getName();
    }

    private static boolean actionInitialized = false;

    @Override
    public Action[] getActions(ActionType actionType)
    {
        ActionManager actionManager = Application.getActionManager();
        if( !actionInitialized )
        {
            actionInitialized = true;

            ActionInitializer initializer = new ActionInitializer(biouml.workbench.resources.MessageBundle.class);

            //toolbar actions
            Action action = new UndoAction(false);
            actionManager.addAction(UndoAction.KEY, action);
            initializer.initAction(action, UndoAction.KEY);

            action = new RedoAction(false);
            actionManager.addAction(RedoAction.KEY, action);
            initializer.initAction(action, RedoAction.KEY);
        }
        if( actionType == ActionType.TOOLBAR_ACTION )
        {
            Action undoAction = actionManager.getAction(UndoAction.KEY);
            Action redoAction = actionManager.getAction(RedoAction.KEY);
            return new Action[] {undoAction, redoAction};
        }
        return null;
    }

    public TabularPropertyInspector getTabularPropertyInspector()
    {
        return this.tpi;
    }

    public DiagramDocument getDiagramDocument(Diagram diagram)
    {
        for( Document document : GUI.getManager().getDocuments() )
        {
            Object model = document.getModel();
            if( diagram.equals(model) )
                return (DiagramDocument)document;
        }
        return new DiagramDocument(diagram);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Update issues
    //
    @Override
    protected void doUpdate()
    {
        tpi.explore(getOptimizationParameters().getFittingParameters().iterator());
        tpi.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    @Override
    public boolean isMutable()
    {
        return getOptimization().getOrigin().isMutable();
    }

    @Override
    public void save()
    {
        Optimization optimization = getOptimization();
        Diagram diagram = getOptimization().getOptimizationDiagram();
        try
        {
            CollectionFactoryUtils.save(optimization);
            CollectionFactoryUtils.save(diagram);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, MessageBundle.getMessage("ERROR_OPTIMIZATION_SAVING"), e);
        }

        super.save();
    }

    @Override
    public void close()
    {
        Optimization optimization = getOptimization();
        removeTransactionListener(undoManager);

        Option mParams = optimization.getOptimizationMethod().getParameters();
        mParams.removePropertyChangeListener(this);

        optimization.getOrigin().release(optimization.getName());

        super.close();
    }

    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        if( log.isLoggable( Level.FINE ) )
        {
            String message = MessageFormat.format(MessageBundle.getMessage("INFO_PROPERTY_CHANGING"), new Object[] {pce.getPropertyName()});
            log.log(Level.FINE, message);
        }

        if( !undoManager.isUndo() && !undoManager.isRedo() )
        {
            fireStartTransaction(new TransactionEvent(getModel(), "Change"));
            fireAddEdit(new PropertyChangeUndo(pce));
            fireCompleteTransaction();

            if( pce.getPropertyName().equals(OptimizationExperiment.EXPERIMENT_TYPE) )
            {
                List<OptimizationExperiment> experiments = getOptimization().getParameters().getOptimizationExperiments();
                OptimizationExperiment changedExp = (OptimizationExperiment)pce.getSource();
                Map<String, SimulationTaskParameters> stp = getOptimization().getParameters().getSimulationTaskParameters();
                stp.remove( changedExp.getName() );
                stp = SimulationTaskRegistry.getSimulationTaskParameters( experiments, stp, getOptimization().getDiagram() );
                getOptimization().getParameters().setSimulationTaskParameters(stp);
            }
        }
        else
        {
            if( pce.getPropertyName().equals(OptimizationParameters.FITTING_PARAMETERS) )
            {
                @SuppressWarnings ( "unchecked" )
                List<Parameter> newValue = (List<Parameter>)pce.getNewValue();
                @SuppressWarnings ( "unchecked" )
                List<Parameter> oldValue = (List<Parameter>)pce.getOldValue();
                if( newValue.size() == oldValue.size() )
                    getOptimization().initDiagramParameters();
            }

        }


    }
    protected EventListenerList listenerList = new EventListenerList();

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

    ////////////////////////////////////////////////////////////////////////////
    // Transaction issues
    //
    protected void fireStartTransaction(TransactionEvent evt)
    {
        Object[] listeners = listenerList.getListenerList();
        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == TransactionListener.class )
                ( (TransactionListener)listeners[i + 1] ).startTransaction(evt);
        }
    }

    protected void fireAddEdit(UndoableEdit ue)
    {
        Object[] listeners = listenerList.getListenerList();
        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == TransactionListener.class )
                ( (TransactionListener)listeners[i + 1] ).addEdit(ue);
        }
    }

    protected void fireCompleteTransaction()
    {
        Object[] listeners = listenerList.getListenerList();
        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == TransactionListener.class )
                ( (TransactionListener)listeners[i + 1] ).completeTransaction();
        }
    }

    ////////////////////////////////////////
    // OptimizationDocumentListener issues
    //
    /**
     * Add a OptimizationDocumentListenerr to the listener list.
     * @param l      the ExperimentAdditionListener to be added
     */
    public void addOptimizationDocumentListener(OptimizationDocumentListener l)
    {
        listenerList.add(OptimizationDocumentListener.class, l);
    }

    /**
     * Remove a OptimizationDocumentListener from the listener list.
     * @param l      the ExperimentAdditionListener to be removed
     */
    public void removeOptimizationDocumentListener(OptimizationDocumentListener l)
    {
        listenerList.remove(OptimizationDocumentListener.class, l);
    }

    public void fireValueChanged(EventObject e)
    {
        Object[] listeners = listenerList.getListenerList();

        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == OptimizationDocumentListener.class )
            {
                ( (OptimizationDocumentListener)listeners[i + 1] ).valueChanged(e);
            }
        }
    }
}
