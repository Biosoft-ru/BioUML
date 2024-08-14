package biouml.plugins.simulation.document;

import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.table.RowModel;

import ru.biosoft.access.support.DataCollectionRowModelAdapter;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.TabularPropertiesEditor;

/**
 * @author axec
 *
 */
@SuppressWarnings ( "serial" )
public class ParametersEditorPart extends TabularPropertiesEditor
{
    protected Object template = new InputParameter( "parameters", "parameters", "type", 0 );
    private static final Logger log = Logger.getLogger( ParametersEditorPart.class.getName() );
    private InteractiveSimulation simulation;

    //Actions
    protected IncreaseParameterAction increaseParameterAction;
    protected DecreaseParameterAction decreaseParameterAction;
    protected RunSimulationAction runSimulationAction;
    protected SaveParametersAction saveToDiagramAction;
    protected ResetParametersAction resetParametersAction;

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof InteractiveSimulation;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.simulation = (InteractiveSimulation)model;
        this.document = document;

        try
        {
            this.setDefaultNumberFormat( null );
            explore( getRowModel(), template, PropertyInspector.SHOW_USUAL );
            this.getTable().setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );
        }
        catch( Exception ex )
        {
            log.log( java.util.logging.Level.SEVERE, "Can not explore Simulation, error: " + ex.getMessage(), ex );
            explore( (Iterator)null );
        }
    }

    protected RowModel getRowModel()
    {
        return new DataCollectionRowModelAdapter( simulation.getInputParameters() );
    }

    @Override
    public Action[] getActions()
    {
        ActionManager actionManager = Application.getActionManager();
        if( increaseParameterAction == null )
        {
            increaseParameterAction = new IncreaseParameterAction();
            actionManager.addAction( IncreaseParameterAction.KEY, increaseParameterAction );
            new ActionInitializer( MessageBundle.class ).initAction( increaseParameterAction, IncreaseParameterAction.KEY );
            increaseParameterAction.setEnabled( true );
        }

        if( decreaseParameterAction == null )
        {
            decreaseParameterAction = new DecreaseParameterAction();
            actionManager.addAction( DecreaseParameterAction.KEY, decreaseParameterAction );
            new ActionInitializer( MessageBundle.class ).initAction( decreaseParameterAction, DecreaseParameterAction.KEY );
            decreaseParameterAction.setEnabled( true );
        }

        if( runSimulationAction == null )
        {
            runSimulationAction = new RunSimulationAction();
            actionManager.addAction( RunSimulationAction.KEY, runSimulationAction );
            new ActionInitializer( MessageBundle.class ).initAction( runSimulationAction, RunSimulationAction.KEY );
            runSimulationAction.setEnabled( true );
        }

        if( resetParametersAction == null )
        {
            resetParametersAction = new ResetParametersAction();
            actionManager.addAction( ResetParametersAction.KEY, resetParametersAction );
            new ActionInitializer( MessageBundle.class ).initAction( resetParametersAction, ResetParametersAction.KEY );
            resetParametersAction.setEnabled( true );
        }

        if( saveToDiagramAction == null )
        {
            saveToDiagramAction = new SaveParametersAction();
            actionManager.addAction( SaveParametersAction.KEY, saveToDiagramAction );
            new ActionInitializer( MessageBundle.class ).initAction( saveToDiagramAction, SaveParametersAction.KEY );
            saveToDiagramAction.setEnabled( true );
        }

        return new Action[] {resetParametersAction, increaseParameterAction, decreaseParameterAction, runSimulationAction,
                saveToDiagramAction};
    }

    public class IncreaseParameterAction extends AbstractAction
    {
        public static final String KEY = "Increase parameters value";

        public IncreaseParameterAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            int row = getSelectedRow();
            if (row == -1)
                return;
            InputParameter selected = getParameter( row );
            selected.setValue( selected.getValue() + selected.getValueStep() );
            updateRow( row );
            simulation.updateValue( selected );
            simulate();
        }
    }

    public class DecreaseParameterAction extends AbstractAction
    {
        public static final String KEY = "Decrease parameters value";

        public DecreaseParameterAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            int row = getSelectedRow();
            if (row == -1)
                return;
            InputParameter selected = getParameter( row );
            selected.setValue( selected.getValue() - selected.getValueStep() );
            updateRow( row );
            simulation.updateValue( selected );
            simulate();
        }
    }

    public class RunSimulationAction extends AbstractAction
    {
        public static final String KEY = "Run simulation";

        public RunSimulationAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            simulation.updateValues();
            simulate();
        }
    }

    public class ResetParametersAction extends AbstractAction
    {
        public static final String KEY = "Reset parameters";

        public ResetParametersAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            int row = getSelectedRow();
            if( row == -1 )
            {
                simulation.resetParameters();
                updateTable();
            }
            else
            {
                InputParameter selected = getParameter( row );
                updateRow( row );
                simulation.resetParameter( selected );
            }
            simulate();
        }
    }

    public class SaveParametersAction extends AbstractAction
    {
        public static final String KEY = "Save parameters";

        public SaveParametersAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            simulation.saveParametersToDiagram();
            updateTable();
        }
    }

    private void simulate()
    {
        setSimulationActions( false );
        simulation.doSimulation();
        setSimulationActions( true );
    }

    private void setSimulationActions(boolean enabled)
    {
        resetParametersAction.setEnabled( enabled );
        runSimulationAction.setEnabled( enabled );
        increaseParameterAction.setEnabled( enabled );
        decreaseParameterAction.setEnabled( enabled );
    }

    private int getSelectedRow()
    {
        JTable table = ParametersEditorPart.this.getTable();
        int[] rows = table.getSelectedRows();
        if( rows.length == 0 )
            return -1;
        return rows[0];
    }

    private InputParameter getParameter(int row)
    {
        return (InputParameter)getModelForRow( row );
    }

    private void updateRow(int row)
    {
        table.tableChanged( new TableModelEvent( tableModel, row ) );
    }

    private void updateTable()
    {
        table.tableChanged( new TableModelEvent( tableModel ) );
    }
}