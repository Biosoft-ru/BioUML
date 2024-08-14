package ru.biosoft.access.security;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.gui.setupwizard.IncorrectDataException;
import ru.biosoft.gui.setupwizard.WizardPage;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

/**
 * Wizard page for proxy settings
 * 
 * @author tolstyh
 */
public class SingleSignOnWizardPage implements WizardPage
{
    protected static final Logger log = Logger.getLogger(SingleSignOnWizardPage.class.getName());

    protected MessageBundle messageBundle = new MessageBundle();
    protected JCheckBox useSSO;
    protected JTextField username;
    protected JPasswordField password;
    protected JPasswordField password2;

    @Override
    public JPanel getPanel()
    {
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());

        useSSO = new JCheckBox();
        useSSO.setSelected(SingleSignOnSupport.isSSOUsed());
        useSSO.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                statusChanged();
            }
        });

        panel.add(new JLabel(messageBundle.getString("USE_SSO_TEXT")), new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 10), 0, 0));
        panel.add(useSSO, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 10, 10), 0, 0));

        panel.add(new JLabel(messageBundle.getString("CREATE_USER_TEXT")), new GridBagConstraints(0, 1, 2, 1, 0.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 0), 0, 0));

        username = new JTextField();
        password = new JPasswordField();
        password2 = new JPasswordField();

        panel.add(new JLabel(messageBundle.getString("USERNAME_TEXT")), new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 0), 0, 0));
        panel.add(username, new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 0, 0), 0, 0));

        panel.add(new JLabel(messageBundle.getString("PASSWORD_TEXT")), new GridBagConstraints(0, 3, 1, 1, 0.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 0), 0, 0));
        panel.add(password, new GridBagConstraints(1, 3, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 0, 0), 0, 0));

        panel.add(new JLabel(messageBundle.getString("CONFIRM_PASSWORD_TEXT")), new GridBagConstraints(0, 4, 1, 1, 0.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 0), 0, 0));
        panel.add(password2, new GridBagConstraints(1, 4, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 0, 0), 0, 0));

        topPanel.add(panel, BorderLayout.NORTH);

        statusChanged();

        return topPanel;
    }

    protected void statusChanged()
    {
        if( useSSO.isSelected() )
        {
            username.setEnabled(true);
            password.setEnabled(true);
            password2.setEnabled(true);
        }
        else
        {
            username.setEnabled(false);
            password.setEnabled(false);
            password2.setEnabled(false);
        }
    }

    @Override
    public void saveSettings() throws IncorrectDataException
    {
        try
        {
            Preferences savedSsoPreferences = SingleSignOnSupport.getSSOPreferences();
            savedSsoPreferences.add(new DynamicProperty(SingleSignOnSupport.PREFERENCES_USE_SSO, SingleSignOnSupport.PREFERENCES_USE_SSO,
                    "Using of Singl-Sign-On", Boolean.class, useSSO.isSelected()));
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not save SSO preferences", e);
        }

        if( password.getText().length() > 0 && !password.getText().equals(password2.getText()) )
        {
            throw new IncorrectDataException(this, messageBundle.getString("PASSWORD_ERROR"));
        }

        if( useSSO.isSelected() && username.getText().length() > 0 )
        {
            try
            {
                SingleSignOnSupport.addUser(username.getText(), password.getText());
            }
            catch( Exception e )
            {
                throw new IncorrectDataException(this, messageBundle.getString("USERNAME_ERROR"));
            }
        }
    }
    
    @Override
    public void fireOpenPage()
    {
        //nothing to do
    }
    
    @Override
    public void fireClosePage()
    {
        //nothing to do
    }
}
