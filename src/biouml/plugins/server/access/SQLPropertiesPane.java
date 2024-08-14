package biouml.plugins.server.access;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import java.util.logging.Logger;

import biouml.plugins.server.access.SQLRegistry.SQLInfo;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;

/**
 * SQL connection editor pane
 */
@SuppressWarnings ( "serial" )
public class SQLPropertiesPane extends JPanel
{
    protected static final Logger log = Logger.getLogger(SQLPropertiesPane.class.getName());

    private static final String[] SQL_TYPES = new String[] {"mysql", "oracle"};

    protected JComboBox<String> sqlType;
    protected JTextField sqlHost;
    protected JTextField sqlPort;
    protected JTextField sqlDatabase;
    protected JTextField sqlUsername;
    protected JPasswordField sqlPassword;

    protected JButton addButton;
    protected JButton removeButton;
    protected TabularPropertyInspector currentConnections;

    protected MessageBundle messages = new MessageBundle();

    public SQLPropertiesPane()
    {
        super();
        setLayout(new BorderLayout());
        List<SQLInfo> sqlConnections = SQLRegistry.getSQLServersList();

        JPanel basePanel = new JPanel(new GridBagLayout());
        add(basePanel, BorderLayout.CENTER);
        basePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        basePanel.add(new JLabel(messages.getString("SQL_DIALOG_TYPE")), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        basePanel.add(new JLabel(messages.getString("SQL_DIALOG_HOST")), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        basePanel.add(new JLabel(messages.getString("SQL_DIALOG_PORT")), new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        basePanel.add(new JLabel(messages.getString("SQL_DIALOG_DATABASE")), new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        sqlType = new JComboBox<>(SQL_TYPES);
        basePanel.add(sqlType, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 10, 0), 0, 0));

        sqlHost = new JTextField(messages.getString("SQL_DIALOG_DEFAULT_HOST"));
        basePanel.add(sqlHost, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 10, 0), 0, 0));

        sqlPort = new JTextField(messages.getString("SQL_DIALOG_DEFAULT_PORT"));
        basePanel.add(sqlPort, new GridBagConstraints(2, 1, 1, 1, 0.3, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 10, 0), 0, 0));

        sqlDatabase = new JTextField();
        basePanel.add(sqlDatabase, new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 10, 0), 0, 0));

        basePanel.add(new JLabel(messages.getString("SQL_DIALOG_USERNAME")), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        sqlUsername = new JTextField();
        basePanel.add(sqlUsername, new GridBagConstraints(1, 2, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        basePanel.add(new JLabel(messages.getString("SQL_DIALOG_PASSWORD")), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        sqlPassword = new JPasswordField();
        basePanel.add(sqlPassword, new GridBagConstraints(1, 3, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addButton = new JButton(messages.getString("SQL_DIALOG_ADD_BUTTON"));
        addButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                addSQLInfo();
            }
        });
        buttons.add(addButton);
        removeButton = new JButton(messages.getString("SQL_DIALOG_REMOVE_BUTTON"));
        removeButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                removeSQLInfo();
            }
        });
        buttons.add(removeButton);
        basePanel.add(buttons, new GridBagConstraints(0, 4, 4, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        currentConnections = new TabularPropertyInspector();
        currentConnections.setPreferredSize(new Dimension(450, 100));
        currentConnections.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane databasesScrollPane = new JScrollPane(currentConnections, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        currentConnections.explore(sqlConnections.iterator());

        basePanel.add(databasesScrollPane, new GridBagConstraints(0, 5, 4, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
    }

    protected void addSQLInfo()
    {
        String type = (String)sqlType.getSelectedItem();
        String host = sqlHost.getText();
        String port = sqlPort.getText();
        try
        {
            Integer.parseInt(port);
        }
        catch( NumberFormatException e )
        {
            log.log(Level.SEVERE, "Incorrect port: " + port);
            return;
        }
        String database = sqlDatabase.getText();
        String username = sqlUsername.getText();
        String password = sqlPassword.getText();
        if( host.length() > 0 )
        {
            SQLRegistry.addSQLServer(type, host, port, database, username, password);
        }
        currentConnections.explore(SQLRegistry.getSQLServersList().iterator());
    }

    protected void removeSQLInfo()
    {
        Object selectedObject = currentConnections.getModelOfSelectedRow();
        if( selectedObject instanceof SQLInfo )
        {
            SQLRegistry.removeSQLServer((SQLInfo)selectedObject);
            currentConnections.explore(SQLRegistry.getSQLServersList().iterator());
        }
    }
}
