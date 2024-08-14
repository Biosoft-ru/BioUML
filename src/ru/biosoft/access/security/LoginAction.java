
package ru.biosoft.access.security;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.developmentontheedge.application.Application;

public class LoginAction extends AbstractAction
{
    public static final String KEY = "Login";

    public LoginAction(boolean enable)
    {
        super(KEY);
        setEnabled(enable);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        LoginDialog loginDialog = new LoginDialog(Application.getApplicationFrame(), "Login",
                "<html>Enter username and password to<br> login or click Cancel otherwise");
        if( loginDialog.doModal() )
        {
            if( SingleSignOnSupport.login(loginDialog.getUsername(), loginDialog.getPassword()) )
            {
                Application.getActionManager().enableActions( false, LoginAction.KEY, RegisterAction.KEY );
                Application.getActionManager().enableActions( true, LogoutAction.KEY );
            }
            else
            {
                JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Incorrect user name or password", "Login failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
