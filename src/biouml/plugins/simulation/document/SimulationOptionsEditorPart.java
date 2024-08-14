package biouml.plugins.simulation.document;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;

import ru.biosoft.exception.ExceptionRegistry;
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
public class SimulationOptionsEditorPart extends EditorPartSupport
{  
    private final PropertyInspectorEx inspector = new PropertyInspectorEx();   
    private InteractiveSimulation simulation;
    private RecompileAction recompileAction;
    private SaveEngineAction saveAction;
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
        this.simulation = ( (InteractiveSimulation)model );        
        inspector.explore(simulation.getEngine());
     }

    @Override
    public Action[] getActions()
    {
        ActionManager actionManager = Application.getActionManager();
        if( recompileAction == null )
        {
            recompileAction = new RecompileAction();
            actionManager.addAction( RecompileAction.KEY, recompileAction );
            new ActionInitializer( MessageBundle.class ).initAction( recompileAction, RecompileAction.KEY );
            recompileAction.setEnabled( true );
        }
        if( saveAction == null )
        {
            saveAction = new SaveEngineAction();
            actionManager.addAction( SaveEngineAction.KEY, saveAction );
            new ActionInitializer( MessageBundle.class ).initAction( saveAction, SaveEngineAction.KEY );
            saveAction.setEnabled( true );
        }
        if( runSimulationAction == null )
        {
            runSimulationAction = new RunSimulationAction();
            actionManager.addAction( RunSimulationAction.KEY, runSimulationAction );
            new ActionInitializer( MessageBundle.class ).initAction( runSimulationAction, RunSimulationAction.KEY );
            runSimulationAction.setEnabled( true );
        }
        return new Action[] {recompileAction, runSimulationAction, saveAction};
    }
    
    public class RecompileAction extends AbstractAction
    {
        public static final String KEY = "Recompile model";

        public RecompileAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                simulation.recompile();
            }
            catch( Exception ex )
            {
                throw ExceptionRegistry.translateException( ex );
            }
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
    
    public class SaveEngineAction extends AbstractAction
    {
        public static final String KEY = "Save engine";

        public SaveEngineAction()
        {
            super( KEY );
            setEnabled( true );
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                Diagram diagram = simulation.getDiagram();
                DiagramUtility.setPreferredEngine( diagram, simulation.getEngine());
                diagram.save();
            }
            catch( Exception ex )
            {
            }
        }
    }
}