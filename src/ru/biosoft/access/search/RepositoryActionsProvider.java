package ru.biosoft.access.search;

import javax.swing.Action;

import ru.biosoft.access.core.DataCollection;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.ActionsProvider;

public class RepositoryActionsProvider implements ActionsProvider
{
    @Override
    public Action[] getActions(Object obj)
    {
        ActionManager actionManager = Application.getActionManager();
        if( !initialized )
        {
            Action action = new DataSearchAction();
            actionManager.addAction(DataSearchAction.KEY, action);

            new ActionInitializer(MessageBundle.class).initAction(action, DataSearchAction.KEY);

            initialized = true;
        }

        if( obj instanceof DataCollection )
        {
            Action dataSearchAction = actionManager.getAction(DataSearchAction.KEY);
            dataSearchAction.putValue(DataSearchAction.DATA_COLLECTION, obj);
            return new Action[] {dataSearchAction};
        }

        return null;
    }

    private boolean initialized = false;
}
