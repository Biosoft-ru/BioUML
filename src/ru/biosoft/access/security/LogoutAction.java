package ru.biosoft.access.security;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import com.developmentontheedge.application.Application;

public class LogoutAction extends AbstractAction
{
    public static final String KEY = "Logout";

    public LogoutAction(boolean enable)
    {
        super(KEY);
        setEnabled(enable);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SingleSignOnSupport.logout();
        Application.getActionManager().enableActions( true, LoginAction.KEY, RegisterAction.KEY );
        Application.getActionManager().enableActions( false, LogoutAction.KEY );
    }
}
