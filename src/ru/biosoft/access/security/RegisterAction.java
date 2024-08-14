
package ru.biosoft.access.security;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.developmentontheedge.application.Application;

public class RegisterAction extends AbstractAction
{
    public static final String KEY = "Register";

    public RegisterAction(boolean enable)
    {
        super(KEY);
        setEnabled(enable);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        RegisterDialog registerDialog = new RegisterDialog(Application.getApplicationFrame(), "Register", "");
        if( registerDialog.doModal() )
        {
            if( registerDialog.getPassword().equals(registerDialog.getSecondPassword()) )
            {
                try
                {
                    SingleSignOnSupport.addUser(registerDialog.getUsername(), registerDialog.getPassword());
                    SingleSignOnSupport.login(registerDialog.getUsername(), registerDialog.getPassword());
                    Application.getActionManager().enableActions( false, LoginAction.KEY, RegisterAction.KEY );
                    Application.getActionManager().enableActions( true, LogoutAction.KEY );
                }
                catch( Exception ex )
                {
                    JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Can not create user: " + ex.getMessage(),
                            "Register failed", JOptionPane.ERROR_MESSAGE);
                }
            }
            else
            {
                JOptionPane.showMessageDialog(Application.getApplicationFrame(),
                        "Confirm password field must be the same as password field", "Register failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
