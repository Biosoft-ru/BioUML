package com.developmentontheedge.application.action;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.Action;

public class ActionManager
{
    private final Hashtable<String, Action> actions = new Hashtable<>();

    public Action getAction ( String actionName )
    {
        return actions.get ( actionName );
    }

    public void addAction ( String actionName, Action act )
    {
        actions.put ( actionName, act );
        act.putValue ( Action.ACTION_COMMAND_KEY, actionName );
    }
    
    public Hashtable<String, Action> getActions()
    {
        return actions;
    }

    public void initActions ( Class<?>... resourceClasses )
    {
        ActionInitializer initializer = new ActionInitializer( resourceClasses );
        Enumeration<String> keys = actions.keys ( );
        while ( keys.hasMoreElements ( ) )
        {
            String key = keys.nextElement ( );
            Action action = actions.get ( key );
            initializer.initAction ( action, key );
        }
    }

    public void enableActions ( boolean value, String... actionNames )
    {
        for ( String actionName : actionNames )
        {
            Action action = getAction ( actionName );
            if ( action != null )
                action.setEnabled ( value );
        }
    }

}
