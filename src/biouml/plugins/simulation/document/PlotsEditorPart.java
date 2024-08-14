package biouml.plugins.simulation.document;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.EditorPartSupport;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.swing.PropertyInspectorEx;

import biouml.model.Diagram;
import biouml.standard.diagram.DiagramUtility;

/**
 * 
 * @author axec
 *
 */
public class PlotsEditorPart extends EditorPartSupport
{
    public static final String PLOTS = "Variables to plot";
    private final PropertyInspectorEx inspector = new PropertyInspectorEx();
    private InteractiveSimulation simulation;
    private UpdatePlotAction updateAction;
    private SavePlotAction saveAction;
    private RunSimulationAction runSimulationAction; 
    
    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof InteractiveSimulation;
    }

    @Override
    public JComponent getView()
    {
        return inspector;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;   
        simulation = (InteractiveSimulation)model;
        inspector.explore(simulation.getPlots());
     }

    @Override
    public Action[] getActions()
    {
        ActionManager actionManager = Application.getActionManager();
        if( updateAction == null )
        {
            updateAction = new UpdatePlotAction();
            actionManager.addAction( UpdatePlotAction.KEY, updateAction );
            new ActionInitializer( MessageBundle.class ).initAction( updateAction, UpdatePlotAction.KEY );
            updateAction.setEnabled( true );
        }
        if( saveAction == null )
        {
            saveAction = new SavePlotAction();
            actionManager.addAction( SavePlotAction.KEY, saveAction );
            new ActionInitializer( MessageBundle.class ).initAction( saveAction, SavePlotAction.KEY );
            saveAction.setEnabled( true );
        }
        if( runSimulationAction == null )
        {
            runSimulationAction = new RunSimulationAction();
            actionManager.addAction( RunSimulationAction.KEY, runSimulationAction );
            new ActionInitializer( MessageBundle.class ).initAction( runSimulationAction, RunSimulationAction.KEY );
            runSimulationAction.setEnabled( true );
        }
        return new Action[] {updateAction, saveAction, runSimulationAction};
    }
    
    public class SavePlotAction extends AbstractAction
    {
        public static final String KEY = "Save plot";

        public SavePlotAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                Diagram diagram = simulation.getDiagram();
                DiagramUtility.setPlotsInfo( diagram, simulation.getPlots() );
                diagram.save();
            }
            catch( Exception ex )
            {
            }
        }
    }
    
    public class UpdatePlotAction extends AbstractAction
    {
        public static final String KEY = "Update plot";

        public UpdatePlotAction()
        {
            super( KEY );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            simulation.updatePlots();
        }
    }
    
    public class RunSimulationAction extends AbstractAction
    {
        public static final String KEY = "Run simulation";

        public RunSimulationAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            simulation.updateValues();     
            setEnabled( false );
            simulation.doSimulation();
            setEnabled( true );
        }
    }
}