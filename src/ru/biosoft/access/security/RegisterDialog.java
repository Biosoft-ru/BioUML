package ru.biosoft.access.security;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import ru.biosoft.util.OkCancelDialog;

/**
 * Dialog for input username and password
 */
public class RegisterDialog extends OkCancelDialog
{
    protected String message;
    protected JPanel content;
    protected JTextField username;
    protected JPasswordField password;
    protected JPasswordField password2;

    public RegisterDialog(Component parent, String title, String message)
    {
        super(parent, title);
        this.message = message;
        init();
    }

    public String getUsername()
    {
        return username.getText();
    }

    public String getPassword()
    {
        return new String(password.getPassword());
    }

    public String getSecondPassword()
    {
        return new String(password2.getPassword());
    }

    ////////////////////////////////////////////////////////////////////////////
    // Protected methods
    //

    private void init()
    {
        content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContent(content);

        content.add(new JLabel(message), new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        content.add(new JLabel("User name:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        username = new JTextField(15);
        content.add(username, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
                5, 0, 0), 0, 0));

        content.add(new JLabel("Password:"), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        password = new JPasswordField(15);
        content.add(password, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
                5, 0, 0), 0, 0));

        content.add(new JLabel("Confirm password:"), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        password2 = new JPasswordField(15);
        content.add(password2, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
                5, 0, 0), 0, 0));

        add(content, BorderLayout.CENTER);
    }
}
