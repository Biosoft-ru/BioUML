package biouml.plugins.server;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.security.Permission;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.ConnectionPool;
import ru.biosoft.server.Request;
import ru.biosoft.server.tomcat.TomcatConnection;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.document.TableDocument;
import ru.biosoft.tasks.TaskInfo;
import biouml.plugins.server.SqlEditorClient.SqlEditorException;
import biouml.plugins.server.access.AccessClient;
import biouml.plugins.server.access.SQLRegistry;
import biouml.plugins.server.access.ServerRegistry;
import biouml.plugins.server.access.SQLRegistry.SQLInfo;
import biouml.workbench.BioUMLApplication;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.dialog.OkCancelDialog;

/**
 * SQL query builder
 */
@SuppressWarnings ( "serial" )
public class SqlEditorViewPart extends ViewPartSupport
{
    protected static final Logger log = Logger.getLogger(SqlEditorViewPart.class.getName());

    public static final String CONNECT_ACTION = "sql-connect";
    public static final String EXECUTE_ACTION = "sql-execute";
    public static final String EXPLAIN_ACTION = "sql-explain";
    public static final String CLEAR_ACTION = "sql-clear";

    protected Action[] actions;
    protected Action executeAction = new ExecuteAction();
    protected Action explainAction = new ExplainAction();
    protected Action clearAction = new ClearAction();
    protected Action connectAction = new ConnectAction();

    protected SqlEditorConnectionProvider sqlClient = null;
    protected RequestPane requestPane = null;

    protected MessageBundle messageBundle = new MessageBundle();



    @Override
    public Action[] getActions()
    {
        if( actions == null )
        {
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(executeAction, EXECUTE_ACTION);
            initializer.initAction(explainAction, EXPLAIN_ACTION);
            initializer.initAction(clearAction, CLEAR_ACTION);
            initializer.initAction(connectAction, CONNECT_ACTION);

            actions = new Action[] {connectAction, executeAction, explainAction, clearAction};
        }

        return actions;
    }

    public SqlEditorViewPart()
    {
        setLayout(new BorderLayout());
        showRequestPane();
        enableQueryActions(false);
    }

    protected void enableQueryActions(boolean enabled)
    {
        executeAction.setEnabled(enabled);
        explainAction.setEnabled(enabled);
        clearAction.setEnabled(enabled);
    }

    /**
     * Show query builder pane
     */
    protected void showRequestPane()
    {
        requestPane = new RequestPane();
        add(requestPane, BorderLayout.CENTER);
        initRequestPane();
    }

    protected void initRequestPane()
    {
        requestPane.clear();
        requestPane.init();
        updateUI();
        enableQueryActions(sqlClient != null);
    }

    /**
     * Connect pane
     */
    private class ConnectDialog extends OkCancelDialog
    {
        public ConnectDialog(JFrame frame, String title)
        {
            super(frame, title, null, "Cancel", "Connect");
            init();
        }

        protected JComboBox<SQLInfo> connectionList;
        protected JComboBox<String> serverURL;
        protected JTextField username;
        protected JPasswordField password;
        protected JRadioButton directSQLRadioButton = null;
        protected JRadioButton serverSQLRadioButton = null;

        protected void init()
        {
            //biouml.workbench.resources.MessageBundle localResources = getMessageBundle();
            JPanel content = new JPanel(new BorderLayout());
            content.setBorder(new EmptyBorder(10, 10, 10, 10));

            JPanel fields = new JPanel(new GridBagLayout());
            content.add(fields, BorderLayout.CENTER);

            ButtonGroup radioGroup = new ButtonGroup();

            List<SQLInfo> sqlConnections = SQLRegistry.getSQLServersList();
            connectionList = new JComboBox<>(sqlConnections.toArray(new SQLInfo[sqlConnections.size()]));

            directSQLRadioButton = new JRadioButton(messageBundle.getResourceString("RB_DIRECT_CONNECTION"), true);
            fields.add(directSQLRadioButton, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL, new Insets(10, 0, 5, 0), 0, 0));
            fields.add(connectionList, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));

            radioGroup.add(directSQLRadioButton);

            username = new JTextField(30);
            password = new JPasswordField();

            String[] servers = ServerRegistry.getServerHosts(username.getText(), new String(password.getPassword()));
            serverURL = new JComboBox<>(servers);
            serverURL.setEditable(true);

            biouml.workbench.resources.MessageBundle resources = BioUMLApplication.getMessageBundle();

            serverSQLRadioButton = new JRadioButton(messageBundle.getResourceString("RB_SERVER_CONNECTION"), true);
            fields.add(serverSQLRadioButton, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL, new Insets(20, 0, 5, 0), 0, 0));
            radioGroup.add(serverSQLRadioButton);

            fields.add(new JLabel(resources.getResourceString("LOAD_DATABASE_USERNAME")), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
            fields.add(username, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                    new Insets(5, 10, 0, 0), 0, 0));

            fields.add(new JLabel(resources.getResourceString("LOAD_DATABASE_PASSWORD")), new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
            fields.add(password, new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                    new Insets(5, 10, 0, 0), 0, 0));

            fields.add(new JLabel(resources.getResourceString("LOAD_DATABASE_DIALOG_SERVER")), new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
            fields.add(serverURL, new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                    new Insets(5, 10, 0, 0), 0, 0));

            JButton updateServers = new JButton("Update servers");
            fields.add(updateServers, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), 0, 0));
            updateServers.addActionListener(e -> {
                String[] servers1;
                try
                {
                    servers1 = ServerRegistry.getServerHosts(username.getText(), new String(password.getPassword()));
                }
                catch( Exception ex )
                {
                    ApplicationUtils.errorBox(ex);
                    return;
                }
                serverURL.setModel(new DefaultComboBoxModel<>(servers1));
            });

            setContent(content);
        }

        protected void connect()
        {
            if( directSQLRadioButton.isSelected() )
            {
                Object sqlConnection = connectionList.getSelectedItem();
                if( sqlConnection instanceof SQLInfo )
                {
                    sqlClient = new DirectConnectionProvider((SQLInfo)sqlConnection, log);
                }

            }
            else if( serverSQLRadioButton.isSelected() )
            {
                String url = ServerRegistry.getServerURL((String)serverURL.getSelectedItem());
                String usernameStr = username.getText();
                String passwordStr = new String(password.getPassword());
                try
                {
                    ClientConnection conn = ConnectionPool.getConnection(TomcatConnection.class, url);
                    AccessClient connection = new AccessClient(new Request(conn, log), log);
                    Permission permission = connection.login(null, usernameStr, passwordStr);

                    ServerRegistry.setServerSession(url, permission.getSessionId());

                    if( sqlClient != null )
                    {
                        sqlClient.close();
                    }
                    sqlClient = new SqlEditorClient(url, new Request(conn, log), log, permission.getSessionId(), permission.getUserName());
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Create connection error", e);
                }
            }
        }

        @Override
        protected void okPressed()
        {
            connect();
            super.okPressed();
        }
    }

    /*
     * Request pane
     */
    private class RequestPane extends JPanel
    {
        protected JLabel connectionInfo;
        protected JTextArea queryPane;
        protected JList<String> tableList;
        protected JList<String> columnList;
        protected JList<String> historyList;
        protected JScrollPane selectorsScrollPane;
        protected Map<String, ColumnModel> tables;

        public RequestPane()
        {
            setLayout(new BorderLayout());

            JPanel upperPane = new JPanel();
            upperPane.setLayout(new GridBagLayout());

            String connInfoStr = sqlClient == null ? "none" : sqlClient.getInfo();
            connectionInfo = new JLabel(connInfoStr);
            upperPane.add(new JLabel(messageBundle.getResourceString("CURRENT_CONNECTION")), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 0), 0, 0));
            upperPane.add(connectionInfo, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 0), 0, 0));

            queryPane = new JTextArea();
            JScrollPane queryScroll = new JScrollPane(queryPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            upperPane.add(queryScroll, new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                    new Insets(10, 5, 0, 0), 0, 0));

            JPanel listsPane = new JPanel();
            listsPane.setLayout(new GridBagLayout());
            listsPane.add(new JLabel("Tables:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 0), 0, 0));

            tableList = new JList<>(new DefaultListModel<String>());
            tableList.setFixedCellWidth(180);
            JScrollPane tableListScroll = new JScrollPane(tableList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            listsPane.add(tableListScroll, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

            listsPane.add(new JLabel("Columns:"), new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 0), 0, 0));
            columnList = new JList<>(new DefaultListModel<String>());
            columnList.setFixedCellWidth(120);
            JScrollPane columnsListScroll = new JScrollPane(columnList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            listsPane.add(columnsListScroll, new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

            listsPane.add(new JLabel("History:"), new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL, new Insets(10, 5, 0, 0), 0, 0));
            historyList = new JList<>(new DefaultListModel<String>());
            JScrollPane historyListScroll = new JScrollPane(historyList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            listsPane.add(historyListScroll, new GridBagConstraints(2, 2, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

            selectorsScrollPane = new JScrollPane(listsPane);
            selectorsScrollPane.setBorder(null);
            selectorsScrollPane.addComponentListener(new ComponentListener()
            {
                @Override
                public void componentResized(ComponentEvent evt)
                {
                    int rowHeight = tableList.getFixedCellHeight();
                    if( rowHeight == -1 )
                    {
                        rowHeight = (int)Math.ceil(tableList.getPreferredScrollableViewportSize().getHeight()
                                / tableList.getVisibleRowCount());
                        tableList.setFixedCellHeight(rowHeight);
                    }
                    int numRows = selectorsScrollPane.getViewport().getHeight() / rowHeight - 2;
                    if( numRows < 5 )
                        numRows = 5;

                    tableList.setVisibleRowCount(numRows);
                    columnList.setVisibleRowCount(numRows);
                    historyList.setVisibleRowCount(numRows);
                    tableList.revalidate();
                    columnList.revalidate();
                    historyList.revalidate();
                }


                @Override
                public void componentMoved(ComponentEvent evt)
                {
                }
                @Override
                public void componentShown(ComponentEvent evt)
                {
                }
                @Override
                public void componentHidden(ComponentEvent evt)
                {
                }
            });

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, upperPane, selectorsScrollPane);
            splitPane.setDividerLocation(0.5);
            splitPane.setDividerSize(2);
            splitPane.setBorder(null);
            add(splitPane, BorderLayout.CENTER);
            initListeners();
        }

        public void init()
        {
            String connInfoStr = sqlClient == null ? "none" : sqlClient.getInfo();
            connectionInfo.setText(connInfoStr);
            if( sqlClient == null )
            {
                return;
            }
            try
            {
                DefaultListModel<String> tableListModel = (DefaultListModel<String>)tableList.getModel();
                tables = sqlClient.getTablesStructure();
                StreamEx.ofKeys(tables).sorted().forEach( tableListModel::addElement );
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not load table list", e);
            }
        }

        protected void initListeners()
        {
            tableList.addListSelectionListener(e -> {
                String tName = tableList.getSelectedValue();
                if( tName == null )
                    return;
                ColumnModel cm = tables.get(tName);
                DefaultListModel<String> columnListModel = (DefaultListModel<String>)columnList.getModel();
                columnListModel.removeAllElements();
                for( TableColumn column : cm )
                {
                    columnListModel.addElement(column.getName());
                }
                SqlEditorViewPart.this.updateUI();
            });
            tableList.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(final MouseEvent e)
                {
                    if( e.getClickCount() > 1 )
                    {
                        Object tName = tableList.getSelectedValue();
                        if( tName != null )
                        {
                            queryPane.setText("SELECT * FROM " + tName);
                        }
                    }
                }
            });
            columnList.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(final MouseEvent e)
                {
                    if( e.getClickCount() > 1 )
                    {
                        Object cName = columnList.getSelectedValue();
                        if( cName != null )
                        {
                            queryPane.insert(cName + " ", queryPane.getCaretPosition());
                        }
                    }
                }
            });
            historyList.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(final MouseEvent e)
                {
                    if( e.getClickCount() > 1 )
                    {
                        Object historyQuery = historyList.getSelectedValue();
                        if( historyQuery != null )
                        {
                            queryPane.setText(historyQuery.toString());
                        }
                    }
                }
            });
        }

        public void clear()
        {
            queryPane.setText("");
            ( (DefaultListModel<?>)tableList.getModel() ).removeAllElements();
            ( (DefaultListModel<?>)columnList.getModel() ).removeAllElements();
            ( (DefaultListModel<?>)historyList.getModel() ).removeAllElements();
        }

        /**
         * Execute query
         */
        public void executeQuery()
        {
            String query = queryPane.getText();
            processQuery(query);
        }

        /**
         * Explain query
         */
        public void executeExplain()
        {
            String query = queryPane.getText();
            processQuery("EXPLAIN " + query);
        }

        /**
         * Remove all from query field
         */
        public void clearQueryPane()
        {
            queryPane.setText("");
        }

        protected void processQuery(String query)
        {
            final String queryFinal = query;
            Runnable queryThread = () -> {
                try
                {
                    Journal journal = JournalRegistry.getCurrentJournal();
                    TaskInfo action = journal.getEmptyAction();
                    
                    Properties properties = new Properties();
                    properties.put(DataCollectionConfigConstants.NAME_PROPERTY, getResultTableName(GUI.getManager().getDocuments()));
                    StandardTableDataCollection tableDataCollection = new StandardTableDataCollection(null, properties);
                    sqlClient.fillResultTable(queryFinal, tableDataCollection);

                    TableDocument tableDocument = new TableDocument(tableDataCollection);
                    GUI.getManager().addDocument(tableDocument);
                    ( (DefaultListModel<String>)historyList.getModel() ).addElement(queryFinal);
                    
                    action.setType(TaskInfo.SQL);
                    action.setData(queryFinal);
                    action.setEndTime();
                    action.getAttributes().add(new DynamicProperty("host", String.class, sqlClient.getServerHost()));
                    journal.addAction(action);
                }
                catch( SqlEditorException ce )
                {
                    JOptionPane
                            .showMessageDialog(SqlEditorViewPart.this, ce.getMessage(), "SQL query error", JOptionPane.ERROR_MESSAGE);
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "SQL query exception", e);
                }
            };
            new Thread(queryThread).start();
        }

        protected String getResultTableName(Collection<Document> documents)
        {
            String base = "SQL result ";
            int num = 0;
            String name = null;
            do
            {
                num++;
                name = base + num;
            }
            while( StreamEx.of( documents ).map( Document::getModel ).select( ru.biosoft.access.core.DataElement.class ).map( ru.biosoft.access.core.DataElement::getName ).has( name ) );
            return name;
        }
    }

    //Actions
    private class ConnectAction extends AbstractAction
    {
        public ConnectAction()
        {
            super(CONNECT_ACTION);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            ConnectDialog dialog = new ConnectDialog(Application.getApplicationFrame(), "Connection properties");
            if( dialog.doModal() )
            {
                initRequestPane();
            }
        }
    }

    private class ExecuteAction extends AbstractAction
    {
        public ExecuteAction()
        {
            super(EXECUTE_ACTION);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( requestPane != null )
            {
                requestPane.executeQuery();
            }
        }
    }

    private class ExplainAction extends AbstractAction
    {
        public ExplainAction()
        {
            super(EXPLAIN_ACTION);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( requestPane != null )
            {
                requestPane.executeExplain();
            }
        }
    }

    private class ClearAction extends AbstractAction
    {
        public ClearAction()
        {
            super(CLEAR_ACTION);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( requestPane != null )
            {
                requestPane.clearQueryPane();
            }
        }
    }
}
