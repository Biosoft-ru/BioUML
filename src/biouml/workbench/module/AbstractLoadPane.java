package biouml.workbench.module;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import java.util.logging.Level;
import java.util.logging.Logger;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.TextUtil;
import biouml.workbench.BioUMLApplication;
import biouml.workbench.resources.MessageBundle;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.beans.swing.table.DefaultRowModel;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.dialog.OkCancelDialog;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;

import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;

@SuppressWarnings ( "serial" )
public abstract class AbstractLoadPane extends JPanel
{
    public static final String ROOT_DC_NAME = "databases";
    protected JComboBox<String> serverURL;
    protected JTextField username;
    protected JPasswordField password;
    protected boolean useAuth = false;
    protected TabularPropertyInspector databasesPane;
    protected JButton findModulesButton;
    protected JButton getInfoButton;
    protected JButton loadButton;
    protected DatabaseLink selectedLink;
    protected TextPaneAppender appender;
    protected JProgressBar progressBar = new JProgressBar();
    protected List<DatabaseLink> databaseLinks = new ArrayList<>();
    protected String helpTopic;
    protected final LocalRepository databases = (LocalRepository)CollectionFactoryUtils.getDatabases();

    protected static final MessageBundle resources = BioUMLApplication.getMessageBundle();
    protected static final Logger log = Logger.getLogger(AbstractLoadPane.class.getName());
    private static final String[] CATEGORY_LIST = {"biouml.diagram", "biouml.workbench", "biouml.plugins"};

    /**
     * Should return list of predefined server urls. User may specify his own url also.
     */
    protected abstract List<String> getServers();

    /**
     * Should load specified module with specified name
     * @param sModuleName - module name on the server
     * @param cModuleName - module name on the client
     * @param jc
     */
    protected abstract int loadModule(String sModuleName, String cModuleName, FunctionJobControl jc);

    /**
     * Shows window with additional DB info
     */
    protected abstract String getDBInfo(DatabaseLink link);

    /**
     * Should return list of available modules for given url/username/password
     */
    protected abstract List<DatabaseLink> getDatabaseLinks(String url, String username, String password) throws Exception;

    /**
     * Add server into registry
     */
    protected abstract void registerServer(String url);

    protected AbstractLoadPane(String helpTopic, boolean useAuth, String initMessage)
    {
        super();
        this.useAuth = useAuth;
        this.helpTopic = helpTopic;

        setLayout(new BorderLayout());

        username = new JTextField();
        password = new JPasswordField();

        List<String> list;
        try
        {
            list = getServers();
            if(list.isEmpty())
                list = Collections.singletonList("(No servers available)");
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage(), ExceptionRegistry.log(e));
            list = Collections.singletonList("(Servers cannot be loaded; check application log)");
        }
        serverURL = new JComboBox<>(new Vector<>(list));
        serverURL.setEditable(true);

        databasesPane = new TabularPropertyInspector();
        databasesPane.setPreferredSize(new Dimension(450, 50));
        databasesPane.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        databasesPane.addListSelectionListener(event -> {
            if( event.getFirstIndex() != -1 )
            {
                selectedLink = (DatabaseLink)databasesPane.getModelOfSelectedRow();
                getInfoButton.setEnabled(true);
            }
            else
            {
                selectedLink = null;
                getInfoButton.setEnabled(false);
            }
        });

        JScrollPane databasesScrollPane = new JScrollPane(databasesPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        findModulesButton = new JButton(resources.getResourceString("LOAD_DATABASE_DIALOG_FIND"));
        findModulesButton.addActionListener(e -> fillModuleNames());
        getInfoButton = new JButton(resources.getResourceString("LOAD_DATABASE_DIALOG_DBINFO"));
        getInfoButton.setEnabled(false);
        getInfoButton.addActionListener(e -> {
            String dbInfo = getDBInfo(selectedLink);
            DBInfoDialog infoDialog = new DBInfoDialog(Application.getApplicationFrame(), dbInfo);
            infoDialog.doModal();
        });
        loadButton = new JButton(resources.getResourceString("LOAD_DATABASE_DIALOG_LOAD"));
        loadButton.setEnabled(false);
        loadButton.addActionListener(e -> okPressed());

        JPanel fields = new JPanel(new GridBagLayout());
        add(fields, BorderLayout.CENTER);
        fields.setBorder(new EmptyBorder(10, 10, 10, 10));

        if( useAuth )
        {
            fields.add(new JLabel(resources.getResourceString("LOAD_DATABASE_USERNAME")), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            fields.add(username, new GridBagConstraints(1, 0, 1, 1, 3.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(
                    5, 5, 0, 0), 0, 0));

            fields.add(new JLabel(resources.getResourceString("LOAD_DATABASE_PASSWORD")), new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            fields.add(password, new GridBagConstraints(3, 0, 1, 1, 3.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(
                    5, 5, 0, 0), 0, 0));
            JButton updateServers = new JButton("Update servers");
            fields.add(updateServers, new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), 0, 0));
            updateServers.addActionListener(e -> {
                try
                {
                    serverURL.setModel(new DefaultComboBoxModel<>(new Vector<>(getServers())));
                }
                catch( Exception ex )
                {
                    ApplicationUtils.errorBox(ex);
                    return;
                }
            });
        }

        fields.add(new JLabel(resources.getResourceString("LOAD_DATABASE_DIALOG_SERVER")), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        fields.add(serverURL, new GridBagConstraints(1, 1, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
                5, 0, 0), 0, 0));

        fields.add(new JLabel(resources.getResourceString("LOAD_DATABASES_TABLE_TITLE")), new GridBagConstraints(0, 2, 5, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));

        fields.add(databasesScrollPane, new GridBagConstraints(0, 3, 5, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(findModulesButton);
        buttons.add(getInfoButton);
        buttons.add(loadButton);
        fields.add(buttons, new GridBagConstraints(0, 4, 5, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5,
                0, 0), 0, 0));

        progressBar.setMaximum(100);
        fields.add(progressBar, new GridBagConstraints(0, 5, 5, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 0, 0), 0, 0));

        fields.add(new JLabel(resources.getResourceString("LOAD_DATABASE_DIALOG_INFO")), new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        initAppender("LOAD_DATABASE_DIALOG_INFO", initMessage);
        fields.add(appender.getLogTextPanel(), new GridBagConstraints(0, 7, 5, 2, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), 0, 0));

        serverURL.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                validateValues();
            }
        });

        serverURL.setSelectedIndex(0);
    }

    protected boolean canLoad()
    {
        if( TextUtil.isEmpty( getSelectedUrl() ) )
        {
            return false;
        }

        return databaseLinks.stream().anyMatch( DatabaseLink::isShouldBeInstalled );
    }

    protected void validateValues()
    {
        loadButton.setEnabled(canLoad());
        String url = getSelectedUrl();
        findModulesButton.setEnabled(url != null && !url.isEmpty());
    }

    public static class InstallDBJobControl extends FunctionJobControl
    {
        private int current;
        private final int total;

        public InstallDBJobControl(Logger cat, JobControlListener listener, int total)
        {
            super(cat, listener);
            this.total = total;
        }

        @Override
        public void setPreparedness(int percent)
        {
            super.setPreparedness( ( percent + current * 100 ) / total);
        }

        public void nextSubprocess()
        {
            current++;
            setPreparedness(0);
        }
    }

    /**
     * Install selected databases with confirm request
     */
    public void loadIfDatabasesChecked()
    {
        if( databaseLinks.stream().anyMatch( DatabaseLink::isShouldBeInstalled ) )
        {
            String title = resources.getResourceString("LOAD_DATABASES_TABLE_INSTALL_SELECTED_TITLE");
            String message = resources.getResourceString("LOAD_DATABASES_TABLE_INSTALL_SELECTED_MESSAGE");
            int answer = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message, title, JOptionPane.YES_NO_OPTION);
            if( answer == JOptionPane.YES_OPTION )
            {
                okPressed();
            }
        }
    }

    public void okPressed()
    {
        loadButton.setEnabled(false);
        new Thread()
        {
            @Override
            public void run()
            {
                int installedCount = 0;
                int toInstall = 0;
                for( DatabaseLink dLink : databaseLinks )
                {
                    if( dLink.isShouldBeInstalled() )
                        toInstall++;
                }
                InstallDBJobControl jc = new InstallDBJobControl(log, new JobControlListenerAdapter()
                {
                    @Override
                    public void valueChanged(JobControlEvent event)
                    {
                        progressBar.setValue(event.getPreparedness());
                    }
                }, toInstall);
                for( DatabaseLink dLink : databaseLinks )
                {
                    if( dLink.isShouldBeInstalled() )
                    {
                        String serverName = dLink.getServerName();
                        String clientName = dLink.getClientName();
                        if( serverName != null && serverName.length() > 0 && clientName != null && clientName.length() > 0 )
                        {
                            installedCount += loadModule(serverName, clientName, jc);
                        }
                        dLink.setShouldBeInstalled(false);
                        jc.nextSubprocess();
                    }
                }

                String address = getSelectedUrl();
                registerServer(address);

                if( findServerAddressComboItem(address) < 0 )
                {
                    serverURL.addItem(address);
                    int idx = findServerAddressComboItem(address);
                    serverURL.setSelectedIndex(idx);
                }
                log.info("" + installedCount + " databases were installed");
                loadButton.setEnabled(true);
            }
        }.start();
    }

    protected void fillModuleNames()
    {
        findModulesButton.setEnabled(false);

        final PropertyChangeListener listener = evt -> validateValues();

        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                List<DatabaseLink> newLinks = null;
                try
                {
                    newLinks = getDatabaseLinks(getSelectedUrl(), username.getText(), new String(password.getPassword()));
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Unable to fetch database links: "+ExceptionRegistry.log(t));
                }

                if( newLinks != null )
                {
                    databaseLinks = newLinks;
                    Collections.sort(databaseLinks);
                    for( DatabaseLink link : databaseLinks )
                    {
                        link.addPropertyChangeListener(listener);
                    }
                }
                else
                {
                    databaseLinks.clear();
                }

                if( databaseLinks.size() > 0 )
                {
                    databasesPane.explore(databaseLinks.toArray(new DatabaseLink[databaseLinks.size()]));
                    log.info("Databases are loaded successfully.");
                    log.info("Please check install checkboxes on databases you want to install.");
                    log.info("Press '" + resources.getResourceString("LOAD_DATABASE_DIALOG_LOAD") + "' button when finished.");
                }
                else
                {
                    databasesPane.explore(new DefaultRowModel(), new DatabaseLink(""), PropertyInspector.SHOW_USUAL);
                    log.warning("No databases found. Please check the URL.");
                }
                findModulesButton.setEnabled(true);
            }
        };
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    protected void initAppender(String title, String initialMessage)
    {
        //--- logging settings ---
        appender = new TextPaneAppender( new PatternFormatter( "[%4$-7s] :  %5$s%n" ), title );
        appender.setLevel( Level.INFO );
        appender.addToCategories(CATEGORY_LIST);
        appender.getLogTextPanel().setPreferredSize(new Dimension(450, 200));

        if( initialMessage != null )
            appender.getLogTextPanel().setText(initialMessage);
    }

    protected String message(String key, Object[] params)
    {
        String message = resources.getString(key);
        return MessageFormat.format(message, params);
    }

    protected int findServerAddressComboItem(String address)
    {
        for( int i = 0; i < serverURL.getItemCount(); i++ )
        {
            if( address.equalsIgnoreCase(serverURL.getItemAt(i)) )
            {
                return i;
            }
        }

        return -1;
    }

    protected String getSelectedUrl()
    {
        Object obj = serverURL.getSelectedItem();
        if( obj == null )
            return null;
        String result = obj.toString().trim();
        if( !result.endsWith("/") )
            result += "/";
        return result;
    }

    //classes
    public static class DatabaseLink extends Option implements Comparable<DatabaseLink>
    {
        private String serverName;
        private String clientName;
        private boolean shouldBeInstalled;
        private String availability;
        private String accessType;
        private Properties properties;

        public DatabaseLink(String serverName)
        {
            this.serverName = serverName;
            this.clientName = this.serverName;
            this.shouldBeInstalled = false;
        }

        public Properties getProperties()
        {
            if( properties == null )
                properties = new Properties();
            return properties;
        }

        public String getServerName()
        {
            return serverName;
        }

        public void setServerName(String serverName)
        {
            this.serverName = serverName;
        }

        public String getClientName()
        {
            return clientName;
        }

        public void setClientName(String clientName)
        {
            String oldValue = this.clientName;
            this.clientName = clientName;
            firePropertyChange("clientName", oldValue, clientName);
        }

        public boolean isShouldBeInstalled()
        {
            return shouldBeInstalled;
        }

        public void setShouldBeInstalled(boolean shouldBeInstalled)
        {
            boolean oldValue = this.shouldBeInstalled;
            this.shouldBeInstalled = shouldBeInstalled;
            firePropertyChange("shoudBeInstalled", oldValue, shouldBeInstalled);
        }

        public String getAvailability()
        {
            return availability;
        }

        public void setAvailability(String availability)
        {
            this.availability = availability;
        }

        public String getAccessType()
        {
            return accessType;
        }

        public void setAccessType(String accessType)
        {
            this.accessType = accessType;
        }

        @Override
        public int compareTo(DatabaseLink obj)
        {
            return serverName.compareTo(obj.serverName);
        }
    }

    public static class DatabaseLinkBeanInfo extends BeanInfoEx
    {
        public DatabaseLinkBeanInfo()
        {
            this(DatabaseLink.class, AbstractLoadPane.resources.getClass().getName());
        }

        public DatabaseLinkBeanInfo(Class<? extends DatabaseLink> type, String name)
        {
            super(type, name);
            beanDescriptor.setDisplayName("Database link");
            beanDescriptor.setShortDescription("Database link");
        }

        @Override
        public void initProperties() throws Exception
        {
            PropertyDescriptorEx pde;

            pde = new PropertyDescriptorEx("serverName", beanClass, "getServerName", null);
            add(pde, AbstractLoadPane.resources.getResourceString("LOAD_DATABASE_DIALOG_SERVER_NAME"), AbstractLoadPane.resources
                    .getResourceString("LOAD_DATABASE_DIALOG_SERVER_NAME"));

            pde = new PropertyDescriptorEx("clientName", beanClass, "getClientName", "setClientName");
            add(pde, AbstractLoadPane.resources.getResourceString("LOAD_DATABASE_DIALOG_CLIENT_NAME"), AbstractLoadPane.resources
                    .getResourceString("LOAD_DATABASE_DIALOG_CLIENT_NAME"));

            pde = new PropertyDescriptorEx("availability", beanClass, "getAvailability", null);
            add(pde, AbstractLoadPane.resources.getResourceString("LOAD_DATABASE_DIALOG_AVAILABILITY"), AbstractLoadPane.resources
                    .getResourceString("LOAD_DATABASE_DIALOG_AVAILABILITY"));

            pde = new PropertyDescriptorEx("accessType", beanClass, "getAccessType", null);
            add(pde, AbstractLoadPane.resources.getResourceString("LOAD_DATABASE_DIALOG_ACCESSTYPE"), AbstractLoadPane.resources
                    .getResourceString("LOAD_DATABASE_DIALOG_ACCESSTYPE"));

            pde = new PropertyDescriptorEx("shouldBeInstalled", beanClass, "isShouldBeInstalled", "setShouldBeInstalled");
            add(pde, AbstractLoadPane.resources.getResourceString("LOAD_DATABASE_DIALOG_INSTALL"), AbstractLoadPane.resources
                    .getResourceString("LOAD_DATABASE_DIALOG_INSTALL"));
        }

    }

    public static class DBInfoDialog extends OkCancelDialog
    {
        public DBInfoDialog(JFrame parent, String text)
        {
            super(parent, "Database info");
            init(text);
        }

        protected void init(String text)
        {
            cancelButton.setVisible(false);
            setPreferredSize(new Dimension(250, 600));

            JTextPane content = new JTextPane();
            content.setContentType("text/html");
            content.setText(getHTMLText(text));
            content.setEditable(false);

            setContent(content);
        }

        private String getHTMLText(String text)
        {
            String result = "<b>" + text;

            result = result.replaceAll("\n", "<br><b>");
            result = result.replaceAll(":", ":</b>");
            result = result.replaceAll("\t", "&nbsp;&nbsp;");

            return result;
        }
    }

}
