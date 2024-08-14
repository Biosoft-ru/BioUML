package biouml.workbench.module.xml.editor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import ru.biosoft.access.SqlDataCollection;
import biouml.workbench.module.xml.XmlModule;
import biouml.workbench.module.xml.XmlModuleConstants;

@SuppressWarnings ( "serial" )
public class CommonTab extends JPanel
{
    protected MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    protected JTextField nameField;
    protected JTextField titleField;
    protected JTextArea description;
    protected JComboBox<String> typeField;
    protected JTextField dbTypeField;
    protected JTextField dbVersionField;
    protected JTextField dbNameField;
    protected JTextField jdbcDriverField;
    protected JTextField jdbcUrlField;
    protected JTextField jdbcUsernameField;
    protected JTextField jdbcPasswordField;

    protected XmlModule module;

    public CommonTab(XmlModule module)
    {
        this.module = module;

        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(new JLabel(messageBundle.getResourceString("COMMON_TAB_NAME_FIELD")), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        nameField = new JTextField(30);
        if( module != null )
        {
            nameField.setText(module.getName());
            nameField.setEnabled(false);
        }
        add(nameField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));

        add(new JLabel(messageBundle.getResourceString("COMMON_TAB_TITLE_FIELD")), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        titleField = new JTextField(30);
        if( module != null )
        {
            titleField.setText(module.getInfo().getDisplayName());
        }
        add(titleField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 0,
                0), 0, 0));

        add(new JLabel(messageBundle.getResourceString("COMMON_TAB_DESCRIPTION_FIELD")), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        description = new JTextArea(3, 30);
        description.setLineWrap(true);
        add(
                new JScrollPane(description, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 0), 0, 0));

        add(new JLabel(messageBundle.getResourceString("COMMON_TAB_TYPE_FIELD")), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        typeField = new JComboBox<>();
        typeField.addItem(XmlModuleConstants.TYPE_TEXT);
        typeField.addItem(XmlModuleConstants.TYPE_SQL);
        add(typeField, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));

        typeField.addItemListener(e -> changeType((String)e.getItem()));

        add(new JLabel(messageBundle.getResourceString("COMMON_TAB_DB_NAME_FIELD")), new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        dbNameField = new JTextField(30);
        dbNameField.setEnabled(false);
        add(dbNameField, new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 0,
                0), 0, 0));

        add(new JLabel(messageBundle.getResourceString("COMMON_TAB_DB_TYPE_FIELD")), new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        dbTypeField = new JTextField(30);
        dbTypeField.setEnabled(false);
        add(dbTypeField, new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 0,
                0), 0, 0));

        add(new JLabel(messageBundle.getResourceString("COMMON_TAB_DB_VERSION_FIELD")), new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        dbVersionField = new JTextField(30);
        dbVersionField.setEnabled(false);
        add(dbVersionField, new GridBagConstraints(1, 6, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5,
                0, 0), 0, 0));

        add(new JLabel(messageBundle.getResourceString("COMMON_TAB_JDBC_DRIVER_FIELD")), new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        jdbcDriverField = new JTextField(30);
        jdbcDriverField.setEnabled(false);
        jdbcDriverField.setText(messageBundle.getResourceString("COMMON_TAB_JDBC_DEFAULT_DRIVER"));
        add(jdbcDriverField, new GridBagConstraints(1, 7, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
                5, 0, 0), 0, 0));

        add(new JLabel(messageBundle.getResourceString("COMMON_TAB_JDBC_URL_FIELD")), new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        jdbcUrlField = new JTextField(30);
        jdbcUrlField.setEnabled(false);
        jdbcUrlField.setText(messageBundle.getResourceString("COMMON_TAB_JDBC_DEFAULT_URL"));
        add(jdbcUrlField, new GridBagConstraints(1, 8, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5,
                0, 0), 0, 0));

        add(new JLabel(messageBundle.getResourceString("COMMON_TAB_JDBC_USERNAME_FIELD")), new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        jdbcUsernameField = new JTextField(30);
        jdbcUsernameField.setEnabled(false);
        add(jdbcUsernameField, new GridBagConstraints(1, 9, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
                5, 0, 0), 0, 0));

        add(new JLabel(messageBundle.getResourceString("COMMON_TAB_JDBC_PASSWORD_FIELD")), new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        jdbcPasswordField = new JTextField(30);
        jdbcPasswordField.setEnabled(false);
        add(jdbcPasswordField, new GridBagConstraints(1, 10, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(
                5, 5, 0, 0), 0, 0));

        setInitValues();
    }

    protected void changeType(String type)
    {
        if( type.equals(XmlModuleConstants.TYPE_SQL) )
        {
            dbNameField.setEnabled(true);
            dbTypeField.setEnabled(true);
            dbVersionField.setEnabled(true);
            jdbcDriverField.setEnabled(true);
            jdbcUrlField.setEnabled(true);
            jdbcUsernameField.setEnabled(true);
            jdbcPasswordField.setEnabled(true);
        }
        else
        {
            dbNameField.setEnabled(false);
            dbTypeField.setEnabled(false);
            dbVersionField.setEnabled(false);
            jdbcDriverField.setEnabled(false);
            jdbcUrlField.setEnabled(false);
            jdbcUsernameField.setEnabled(false);
            jdbcPasswordField.setEnabled(false);
        }
    }

    protected void setInitValues()
    {
        if( module != null )
        {
            description.setText(module.getDescription());

            Properties dbProperties = module.getDatabaseProperties();
            if( dbProperties != null )
            {
                if( dbProperties.containsKey(XmlModuleConstants.DATABASE_TYPE_ATTR) )
                {
                    dbTypeField.setText(dbProperties.getProperty(XmlModuleConstants.DATABASE_TYPE_ATTR));
                }
                if( dbProperties.containsKey(XmlModuleConstants.DATABASE_NAME_ATTR) )
                {
                    dbNameField.setText(dbProperties.getProperty(XmlModuleConstants.DATABASE_NAME_ATTR));
                }
                if( dbProperties.containsKey(XmlModuleConstants.DATABASE_VERSION_ATTR) )
                {
                    dbVersionField.setText(dbProperties.getProperty(XmlModuleConstants.DATABASE_VERSION_ATTR));
                }

                if( dbProperties.containsKey(SqlDataCollection.JDBC_DRIVER_PROPERTY) )
                {
                    jdbcDriverField.setText(dbProperties.getProperty(SqlDataCollection.JDBC_DRIVER_PROPERTY));
                }
                if( dbProperties.containsKey(SqlDataCollection.JDBC_URL_PROPERTY) )
                {
                    jdbcUrlField.setText(dbProperties.getProperty(SqlDataCollection.JDBC_URL_PROPERTY));
                }
                if( dbProperties.containsKey(SqlDataCollection.JDBC_USER_PROPERTY) )
                {
                    jdbcUsernameField.setText(dbProperties.getProperty(SqlDataCollection.JDBC_USER_PROPERTY));
                }
                if( dbProperties.containsKey(SqlDataCollection.JDBC_PASSWORD_PROPERTY) )
                {
                    jdbcPasswordField.setText(dbProperties.getProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY));
                }
            }
            typeField.setSelectedItem(module.getModuleType());
        }
    }

    public String validateForm()
    {
        String result = "<html>";
        if( titleField.getText().trim().length() == 0 || nameField.getText().trim().length() == 0 )
        {
            result += messageBundle.getResourceString("COMMON_TAB_TITLE_ERROR") + "<br>";
        }
        if( typeField.getSelectedItem().equals(XmlModuleConstants.TYPE_SQL) )
        {
            if( jdbcDriverField.getText().trim().length() == 0 || jdbcUrlField.getText().trim().length() == 0 )
            {
                result += messageBundle.getResourceString("COMMON_TAB_JDBC_ERROR") + "<br>";
            }
        }
        if( result.length() > 6 )
        {
            return result;
        }
        return null;
    }

    public void applyChanges(XmlModule module)
    {
        module.getInfo().setDisplayName(titleField.getText());
        module.getInfo().setDescription(description.getText());
        module.setModuleType((String)typeField.getSelectedItem());

        if( typeField.getSelectedItem().equals(XmlModuleConstants.TYPE_SQL) )
        {
            Properties dbProperties = module.getDatabaseProperties();

            dbProperties.put(XmlModuleConstants.DATABASE_TYPE_ATTR, dbTypeField.getText());
            dbProperties.put(XmlModuleConstants.DATABASE_NAME_ATTR, dbNameField.getText());
            dbProperties.put(XmlModuleConstants.DATABASE_VERSION_ATTR, dbVersionField.getText());

            dbProperties.put(SqlDataCollection.JDBC_DRIVER_PROPERTY, jdbcDriverField.getText());
            dbProperties.put(SqlDataCollection.JDBC_URL_PROPERTY, jdbcUrlField.getText());
            dbProperties.put(SqlDataCollection.JDBC_USER_PROPERTY, jdbcUsernameField.getText());
            dbProperties.put(SqlDataCollection.JDBC_PASSWORD_PROPERTY, jdbcPasswordField.getText());
        }
    }

    public String getModuleName()
    {
        return nameField.getText();
    }
}
