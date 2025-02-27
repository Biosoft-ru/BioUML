package biouml.plugins.physicell.document;

import javax.swing.Action;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.RepositoryListener;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.gui.GUI;
import biouml.plugins.simulation.resources.MessageBundle;
import biouml.standard.simulation.plot.Plot;

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

            Action newPhysicellResultAction = new OpenPhysicellResultAction();
            actionManager.addAction( OpenPhysicellResultAction.KEY, newPhysicellResultAction );

            ActionInitializer initializer = new ActionInitializer( MessageBundle.class );
            initializer.initAction( newPhysicellResultAction, OpenPhysicellResultAction.KEY );

            initListeners();
        }

        return new Action[] {actionManager.getAction( OpenPhysicellResultAction.KEY )};
    }

    protected void initListeners()
    {
        GUI.getManager().getRepositoryTabs().addListener( this );
    }

    @Override
    public void nodeClicked(DataElement node, int clickCount)
    {
        if( clickCount > 1 && node instanceof Plot )
        {
            DocumentManager.getDocumentManager().openDocument( node );
        }
    }

    @Override
    public void selectionChanged(DataElement node)
    {
    }
}