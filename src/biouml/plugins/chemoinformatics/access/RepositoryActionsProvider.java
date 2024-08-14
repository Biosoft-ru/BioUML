package biouml.plugins.chemoinformatics.access;

import javax.swing.Action;

import biouml.plugins.chemoinformatics.MessageBundle;
import biouml.standard.type.Structure;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.ActionsProvider;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.RepositoryListener;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.gui.GUI;

/**
 * Repository actions provider for chemoinformatics plug-in
 */
public class RepositoryActionsProvider implements ActionsProvider, RepositoryListener
{

    private final ActionManager actionManager;
    public RepositoryActionsProvider()
    {
        actionManager = Application.getActionManager();
        Action action = new OpenStructuresAction();
        actionManager.addAction(OpenStructuresAction.KEY, action);
        new ActionInitializer(MessageBundle.class).initAction(action, OpenStructuresAction.KEY);
    }

    @Override
    public Action[] getActions(Object obj)
    {
        initListeners();
        if( ( obj instanceof DataCollection ) && Structure.class.isAssignableFrom( ( (DataCollection)obj ).getDataElementType()) )
        {
            Action openStructuresAction = actionManager.getAction(OpenStructuresAction.KEY);
            openStructuresAction.putValue(OpenStructuresAction.DATA_ELEMENT, obj);

            return new Action[] {openStructuresAction};
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
        if( clickCount > 1 && node instanceof Structure )
        {
            DocumentManager.getDocumentManager().openDocument(node);
        }
    }

    @Override
    public void selectionChanged(DataElement node)
    {
    }
}
