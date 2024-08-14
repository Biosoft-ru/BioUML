package ru.biosoft.access.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;

import org.eclipse.core.runtime.IConfigurationElement;

import com.developmentontheedge.beans.ActionsProvider;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.subaction.DynamicActionFactory;
import ru.biosoft.util.ObjectExtensionRegistry;

/**
 * This class is used to allow any plugin provide provide context specific actions
 * for repository pop up menu. For this purpose it loads all extensions for
 * <code>ru.biosoft.repository.actionsPorivider</code> extension point
 * and returns union of all possible actions for the given repository item (or bean).
 */
public class PluginActions implements ActionsProvider, RepositoryListener
{
    protected ObjectExtensionRegistry<ActionsProvider> actionProviders = new ObjectExtensionRegistry<>(
            "ru.biosoft.access.repositoryActionsProvider", ActionsProvider.class);
    protected ObjectExtensionRegistry<AbstractElementAction> elementActions = new ObjectExtensionRegistry<AbstractElementAction>(
            "ru.biosoft.access.elementAction", AbstractElementAction.class)
    {
        @Override
        protected AbstractElementAction loadElement(IConfigurationElement element, String className) throws Exception
        {
            AbstractElementAction action = super.loadElement(element, className);
            DynamicActionFactory.initAction(action, element, action.getClass());
            action.setPriority(getIntAttribute(element, "priority"));
            return action;
        }
    };

    @Override
    public Action[] getActions(Object bean)
    {
        List<Action> actionList = new ArrayList<>();
        Set<String> actionKeys = new HashSet<>();
        
        // New-style actions; always added to the top of the list
        if(bean instanceof DataElement)
        {
            List<Action> elementActionList = new ArrayList<>();
            final Map<Action, Integer> priorities = new IdentityHashMap<>();
            DataElement de = (DataElement)bean;
            for( AbstractElementAction action: elementActions )
            {
                int priority = action.getActionPriority(de);
                if(priority != AbstractElementAction.PRIORITY_NOT_SUPPORTED)
                {
                    elementActionList.add(action);
                    action.setDataElement(de);
                    priorities.put(action, priority);
                }
            }
            Collections.sort(elementActionList, Comparator.comparingInt( priorities::get ).reversed());
            for(Action action: elementActionList)
            {
                String key = action.getValue(Action.ACTION_COMMAND_KEY).toString();
                if(!actionKeys.contains(key))
                {
                    actionKeys.add(key);
                    actionList.add(action);
                }
            }
        }

        for( ActionsProvider provider: actionProviders )
        {
            Action[] actions = provider.getActions(bean);
            if( actions != null )
            {
                for( Action action : actions )
                {
                    String key = action.getValue(Action.ACTION_COMMAND_KEY).toString();
                    if(!actionKeys.contains(key))
                    {
                        actionList.add(action);
                        actionKeys.add(key);
                    }
                }
            }
        }

        return actionList.isEmpty()?null:actionList.toArray(new Action[actionList.size()]);
    }


    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void nodeClicked(DataElement de, int clickCount)
    {
        // Default double-click action: select best new-style action and execute
        if(clickCount > 1)
        {
            AbstractElementAction bestAction = null;
            int bestPriority = AbstractElementAction.PRIORITY_NOT_SUPPORTED;
            for( AbstractElementAction action: elementActions )
            {
                int priority = action.getActionPriority(de);
                if(priority > bestPriority)
                {
                    bestAction = action;
                    bestPriority = priority;
                }
            }
            if(bestAction != null && bestPriority > AbstractElementAction.PRIORITY_AUTO_ACTION)
            {
                bestAction.setDataElement(de);
                bestAction.actionPerformed(null);
            }
        }
    }

    @Override
    public void selectionChanged(DataElement node)
    {
    }
}
