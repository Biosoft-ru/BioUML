package biouml.plugins.simulation;

import javax.swing.Action;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.RepositoryListener;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.gui.GUI;
import biouml.model.Diagram;
import biouml.plugins.simulation.document.OpenInteractiveSimulationAction;
import biouml.plugins.simulation.plot.OpenPlotAction;
import biouml.plugins.simulation.resources.MessageBundle;
import biouml.plugins.state.ApplyStateAction;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.state.State;
import biouml.workbench.RemoveDataElementAction;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.ActionsProvider;

public class RepositoryActionsProvider implements ActionsProvider, RepositoryListener
{
    private boolean initialized = false;

    @Override
    public Action[] getActions(Object obj)
    {
        ActionManager actionManager = Application.getActionManager();

        if( !initialized )
        {
            initialized = true;

            Action openPlotAction = new OpenPlotAction();
            actionManager.addAction(OpenPlotAction.KEY, openPlotAction);
            
            Action newPlotAction = new OpenPlotAction();
            actionManager.addAction(OpenPlotAction.KEY2, newPlotAction);
            
            Action newInteractiveSimulationAction = new OpenInteractiveSimulationAction();
            actionManager.addAction(OpenInteractiveSimulationAction.KEY, newInteractiveSimulationAction);

            Action applyStateActon = new ApplyStateAction();
            actionManager.addAction(ApplyStateAction.KEY, applyStateActon);
            
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(openPlotAction, OpenPlotAction.KEY);
            initializer.initAction(newPlotAction, OpenPlotAction.KEY2);
            initializer.initAction(applyStateActon, ApplyStateAction.KEY);
            initializer.initAction(newInteractiveSimulationAction, OpenInteractiveSimulationAction.KEY);
            
            initListeners();
        }

        // SimulationResult actions
        if( obj instanceof SimulationResult || obj instanceof Plot )
        {
            Action newPlotAction = actionManager.getAction(OpenPlotAction.KEY2);
            Action openPlotAction = actionManager.getAction(OpenPlotAction.KEY);

            Action removeDataElementAction = actionManager.getAction(RemoveDataElementAction.KEY);
            removeDataElementAction.putValue(RemoveDataElementAction.DATA_ELEMENT, obj);
            
            return obj instanceof SimulationResult ? new Action[] {newPlotAction, removeDataElementAction}
            : new Action[] {openPlotAction, removeDataElementAction} ;
        }
        else if ( obj instanceof State)
        {
            return new Action[]{ actionManager.getAction(ApplyStateAction.KEY)};
        }
        else if (obj instanceof Diagram)
        {
            return new Action[] {actionManager.getAction(OpenInteractiveSimulationAction.KEY)};
        }           
        return null;
    }
    
    protected void initListeners()
    {
        GUI.getManager().getRepositoryTabs().addListener(this);
    }
    
    @Override
    public void nodeClicked(DataElement node, int clickCount)
    {
        if( clickCount > 1 && node instanceof Plot )
        {
            DocumentManager.getDocumentManager().openDocument(node);
        }
    }

    @Override
    public void selectionChanged(DataElement node)
    {
    }

}
