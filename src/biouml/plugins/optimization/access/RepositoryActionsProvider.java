package biouml.plugins.optimization.access;

import javax.swing.Action;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import biouml.plugins.optimization.MessageBundle;
import biouml.plugins.optimization.Optimization;
import biouml.workbench.RemoveDataElementAction;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.ActionsProvider;

public class RepositoryActionsProvider implements ActionsProvider
{
    private final ActionManager actionManager;

    public RepositoryActionsProvider()
    {
        actionManager = Application.getActionManager();
        ActionInitializer initializer = new ActionInitializer(MessageBundle.class);

        Action action = new NewOptimizationAction();
        actionManager.addAction(NewOptimizationAction.KEY, action);
        initializer.initAction(action, NewOptimizationAction.KEY);

        action = new RemoveDataElementAction();
        actionManager.addAction(RemoveDataElementAction.KEY, action);
        initializer.initAction(action, RemoveDataElementAction.KEY);
    }

    @Override
    public Action[] getActions(Object bean)
    {
        if( bean instanceof DataCollection && DataCollectionUtils.isAcceptable((DataCollection)bean, Optimization.class) )
        {
            Action newOptimizationAction = actionManager.getAction(NewOptimizationAction.KEY);
            newOptimizationAction.putValue(NewOptimizationAction.DATA_COLLECTION, bean);

            return new Action[] {newOptimizationAction};
        }
        else if( bean instanceof Optimization )
        {
            Optimization opt = (Optimization)bean;
            Action removeOptimizationAction = actionManager.getAction(RemoveDataElementAction.KEY);
            removeOptimizationAction.putValue(RemoveDataElementAction.DATA_ELEMENT, opt);

            return new Action[] {removeOptimizationAction};
        }
        return null;
    }
}
