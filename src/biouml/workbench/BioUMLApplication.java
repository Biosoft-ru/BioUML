package biouml.workbench;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyDescriptor;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.Platform;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;
import com.developmentontheedge.application.ApplicationMenu;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.PanelInfo;
import com.developmentontheedge.application.PanelManager;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.application.action.TogglePanelAction;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.print.PrintAction;
import com.developmentontheedge.print.PrintManager;
import com.developmentontheedge.print.PrintPreviewAction;
import com.developmentontheedge.print.PrintSetupAction;

import biouml.model.Module;
import biouml.workbench.diagram.DiagramDocument;
import biouml.workbench.diagram.NewDiagramAction;
import biouml.workbench.diagram.SaveAsDocumentAction;
import biouml.workbench.module.ExportModuleAction;
import biouml.workbench.module.ModuleSetupAction;
import biouml.workbench.module.NewModuleAction;
import biouml.workbench.module.RemoveModuleAction;
import biouml.workbench.module.xml.EditModuleAction;
import biouml.workbench.module.xml.NewCompositeModuleAction;
import biouml.workbench.perspective.Perspective;
import biouml.workbench.perspective.PerspectiveRegistry;
import biouml.workbench.perspective.PerspectiveUI;
import biouml.workbench.resources.MessageBundle;
import ru.biosoft.access.AccessCoreInit;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionListenerRegistry;
import ru.biosoft.access.QuerySystemRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.exception.BiosoftExceptionTranslator;
import ru.biosoft.access.file.GenericFileDataCollection;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.repository.PluginActions;
import ru.biosoft.access.repository.RepositoryListener;
import ru.biosoft.access.repository.RepositoryTabs;
import ru.biosoft.access.security.LoginAction;
import ru.biosoft.access.security.LoginDialog;
import ru.biosoft.access.security.LogoutAction;
import ru.biosoft.access.security.RegisterAction;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SingleSignOnSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.ExceptionTranslator;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.View.ModelResolver;
import ru.biosoft.graphics.access.DataElementModelResolver;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.graphics.editor.ViewPaneListener;
import ru.biosoft.gui.CloseAllDocumentAction;
import ru.biosoft.gui.CloseDocumentAction;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.Document.ActionType;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.gui.DocumentViewAccessProvider;
import ru.biosoft.gui.DocumentsPane;
import ru.biosoft.gui.ExplorerPane;
import ru.biosoft.gui.ExportDocumentAction;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.OpenPathAction;
import ru.biosoft.gui.PluggedEditorsTabbedPane;
import ru.biosoft.gui.SaveDocumentAction;
import ru.biosoft.gui.setupwizard.OpenSetupWizardAction;
import ru.biosoft.gui.setupwizard.SetupWizardDialog;
import ru.biosoft.gui.setupwizard.SetupWizardSupport;
import ru.biosoft.gui.setupwizard.WizardPageRegistry;
import ru.biosoft.journal.JournalProperties;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.plugins.Plugins;
import ru.biosoft.server.servlets.webservices.HttpServer;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.JULBeanLogger;
import ru.biosoft.util.NetworkConfigurator;
import ru.biosoft.workbench.AboutAction;
import ru.biosoft.workbench.Framework;
import ru.biosoft.workbench.HelpAction;
import ru.biosoft.workbench.LookAndFeelManager;

/**
 * General class for different BioUML applications.
 */
@SuppressWarnings ( "serial" )
public class BioUMLApplication extends ApplicationFrame implements ChangeListener, DocumentViewAccessProvider
{
    protected static final int GROUP_FILE = 1;
    protected static final int GROUP_EDIT = 2;
    protected static final int GROUP_DOCUMENT = 3;
    protected static final int GROUP_PANE = 4;
    protected static final int GROUP_SETUP = 5;
    protected static final int GROUP_HELP = 6;

    public static final String CONFIG_FILE = "preferences.xml";
    public static final String JAVA_LIBRARY_PATH = "JAVA_LIBRARY_PATH";

    public static final String SETUP_WIZARD_LOGO = "biouml/workbench/resources/logo.png";
    public static final String HOME_HOST = "http://biouml.org";

    public static final String LOCATION_PROPERTY_NAME = "Window location";

    protected static final Logger log = Logger.getLogger( BioUMLApplication.class.getName() );

    private final DocumentViewAccessProvider accessProvider = new BioUMLDocumentViewAccessProvider();

    private static final ModelResolver viewModelResolver = new DataElementModelResolver();

    private static final ExceptionTranslator translator = new BiosoftExceptionTranslator();

    public BioUMLApplication(Image icon, String[] dataPath)
    {
        super(new ActionManager(), new DocumentManager(null), new PanelManager(), "");
        try
        {
            AccessCoreInit.init();
            QuerySystemRegistry.initQuerySystems();
            DataCollectionListenerRegistry.initDataCollectionListeners();
            JULBeanLogger.install();
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            ExceptionRegistry.setExceptionRegistry( translator );
            loadPreferences();
            NetworkConfigurator.initNetworkConfiguration();
            setTitle(Application.getGlobalValue("ApplicationTitle"));

            Rectangle rect = getWindowRect();
            setLocation(rect.x, rect.y);
            setSize(rect.width, rect.height);
            if( icon != null )
                setIconImage(icon);

            // Introspector has not access to CollectionClassLoader.
            // The only way is to setup it as thread contextClassLoader
            int threadCount = Thread.activeCount();
            Thread[] threads = new Thread[threadCount];
            Thread.enumerate(threads);
            for( int i = 0; i < threadCount; i++ )
            {
                if( threads[i] != null )
                {
                    try
                    {
                        threads[i].setContextClassLoader(Thread.currentThread().getContextClassLoader());
                        threads[i].setPriority(Thread.MIN_PRIORITY);
                    }
                    catch( Throwable t )
                    {
                        log.log(Level.SEVERE, "Could not assign thread priority", t);
                    }
                }
            }

            try
            {
                CollectionFactoryUtils.init();
            }
            catch( Throwable t )
            {
                log.log( Level.SEVERE, "Can not initialize: " + ExceptionRegistry.log( t ) );
            }

            try
            {
                Framework.initRepository(dataPath);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not initialize repository: "+ExceptionRegistry.log(t));
            }

            try
            {
                Plugins.getPlugins();
            }
            catch( Throwable t )
            {
                log.log( Level.SEVERE, "Failed to init plugins", t );
                System.out.println( "Failed to init plugins: " + t );
            }

            initActions();
            initPanels();
            initToolbar();
            initMenubar();
            initHTTPServer();

            addWindowListener(new WindowAdapter()
            {
                /**
                 * Invoked when a window has been closed.
                 */
                @Override
                public void windowClosing(WindowEvent e)
                {
                    if( closeApplication() )
                    {
                        setVisible(false);
                        dispose();

                        Preferences locPreferences = Application.getPreferences().getPreferencesValue(LOCATION_PROPERTY_NAME);
                        if( locPreferences != null )
                        {
                            Point location = getLocation();
                            locPreferences.getProperty("x").setValue(location.x);
                            locPreferences.getProperty("y").setValue(location.y);
                            Dimension dimension = getSize();
                            locPreferences.getProperty("width").setValue(dimension.width);
                            locPreferences.getProperty("height").setValue(dimension.height);
                        }

                        savePreferences();
                        System.exit(0);
                    }
                }
            });

            DocumentsPane.getDocumentsPane().addChangeListener(this);

            View.setModelResolver( viewModelResolver );

            // TODO: fix somehow race condition in toolbar.getPreferredSize()
            setVisible(true);

            PerspectiveUI.initPerspective();

            validate();

            login();

            showSetupWizard();

            ProxyTester.testServer(HOME_HOST, 10000, result -> {
                if( result == -1 )
                {
                    ApplicationUtils.errorBox( "Proxy error", getMessageBundle().getResourceString("PROXY_ERROR_MESSAGE"));
                }
            });

            //startupUpdateNotificator = new StartupUpdateNotificator();
            //startupUpdateNotificator.launchCheckInBackground();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
    //protected StartupUpdateNotificator startupUpdateNotificator;

    // /////////////////////////////////////////////////////////////////////////
    // initialisation issues
    //

    protected void loadPreferences()
    {
        String fileName = Platform.getInstallLocation().getURL().getPath() + CONFIG_FILE;
        Preferences preferences = new Preferences();

        try
        {
            ClassLoader cl = this.getClass().getClassLoader();
            preferences.load(fileName, cl);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Load preferences error", t);
        }

        Application.setPreferences(preferences);

        // ensure that dialogs section is available
        if( preferences.getValue(Preferences.DIALOGS) == null )
        {
            PropertyDescriptor descriptor = BeanUtil.createDescriptor(Preferences.DIALOGS);
            descriptor.setExpert(true);
            preferences.add(new DynamicProperty(descriptor, Preferences.class, new Preferences()));
        }

        if( preferences.getValue( JAVA_LIBRARY_PATH ) != null )
        {
            ClassLoading.addJavaLibraryPath( preferences.getStringValue( JAVA_LIBRARY_PATH, null ) );
        }

        // load look & feel
        LookAndFeelManager.loadLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }

    protected void savePreferences()
    {
        try
        {
            Application.getPreferences().save(CONFIG_FILE);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Save preferences error", t);
        }
    }

    protected Rectangle getWindowRect()
    {
        Preferences locPreferences = Application.getPreferences().getPreferencesValue(LOCATION_PROPERTY_NAME);
        int x, y, width, height;
        if( locPreferences == null )
        {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            DisplayMode mode = gd.getDisplayMode();

            width = 1200;
            height = 750;
            x = ( mode.getWidth() - width ) / 2;
            y = ( mode.getHeight() - height ) / 2;
            try
            {
                locPreferences = new Preferences();
                locPreferences.add(new DynamicProperty("x", Integer.class, x));
                locPreferences.add(new DynamicProperty("y", Integer.class, y));
                locPreferences.add(new DynamicProperty("width", Integer.class, width));
                locPreferences.add(new DynamicProperty("height", Integer.class, height));
                DynamicProperty locProperty = new DynamicProperty(LOCATION_PROPERTY_NAME, Preferences.class, locPreferences);
                locProperty.setHidden(true);
                Application.getPreferences().add(locProperty);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot create properties to save window location");
            }
        }
        else
        {
            x = (Integer)locPreferences.getValue("x");
            y = (Integer)locPreferences.getValue("y");
            width = (Integer)locPreferences.getValue("width");
            height = (Integer)locPreferences.getValue("height");
        }
        return new Rectangle(x, y, width, height);
    }

    //protected StartupUpdateNotificator startupUpdateNotificator;

    // /////////////////////////////////////////////////////////////////////////
    // initialization issues
    //

    protected void initHTTPServer()
    {
        Random random = new Random();
        int minPort = 20000;
        int maxPort = 25000;
        String sessionId = SecurityManager.generateSessionId();
        int port;
        int tryNum = 0;
        while(true)
        {
            try
            {
                port = random.nextInt(maxPort-minPort)+minPort;
                new HttpServer(port, sessionId).startServer();
                break;
            }
            catch( Exception e )
            {
                if(tryNum++ > 100)
                {
                    log.log(Level.SEVERE, "Unable to start HTTP server", e);
                    return;
                }
            }
        }
        Application.getPreferences().getPreferencesValue("Global").addValue("ServerPath", "http://localhost:"+port+"/biouml");
        Application.getPreferences().getPreferencesValue("Global").addValue("ServerSession", sessionId);
    }

    public static void initActions()
    {
        ActionManager actionManager = Application.getActionManager();
        Action action;

        // --- misc ----------------------------------------------------/
        actionManager.addAction(PreferencesAction.KEY, new PreferencesAction());
        actionManager.addAction(LoginAction.KEY, new LoginAction(true));
        actionManager.addAction(RegisterAction.KEY, new RegisterAction(true));
        actionManager.addAction(LogoutAction.KEY, new LogoutAction(false));
        actionManager.addAction(OpenSetupWizardAction.KEY, new OpenSetupWizardAction());

        // --- module actions ------------------------------------------/
        actionManager.addAction(NewModuleAction.KEY, new NewModuleAction());
        actionManager.addAction(NewCompositeModuleAction.KEY, new NewCompositeModuleAction());
        actionManager.addAction(ModuleSetupAction.KEY, new ModuleSetupAction());
        actionManager.addAction(RemoveModuleAction.KEY, new RemoveModuleAction());
        actionManager.addAction(ExportModuleAction.KEY, new ExportModuleAction());
        actionManager.addAction(EditModuleAction.KEY, new EditModuleAction());

        // --- diagram actions -----------------------------------------/
        actionManager.addAction(NewDiagramAction.KEY, new NewDiagramAction());
        actionManager.addAction(ImportElementAction.KEY, new ImportElementAction(false));

        // --- document actions ----------------------------------------/
        actionManager.addAction(OpenPathAction.KEY, new OpenPathAction(true));
        actionManager.addAction(SaveDocumentAction.KEY, new SaveDocumentAction(false));
        actionManager.addAction(SaveAsDocumentAction.KEY, new SaveAsDocumentAction(false));
        actionManager.addAction(CloseDocumentAction.KEY, new CloseDocumentAction(false));
        actionManager.addAction(CloseAllDocumentAction.KEY, new CloseAllDocumentAction(false));
        actionManager.addAction(ExportDocumentAction.KEY, new ExportDocumentAction());

        // --- toggle panel actions ------------------------------------/
        PanelManager panelManager = Application.getApplicationFrame().getPanelManager();
        actionManager.addAction(REPOSITORY_PANE_NAME, new TogglePanelAction(REPOSITORY_PANE_NAME, panelManager));
        actionManager.addAction(DOCUMENT_PANE_NAME, new TogglePanelAction(DOCUMENT_PANE_NAME, panelManager));
        actionManager.addAction(EXPLORER_PANE_NAME, new TogglePanelAction(EXPLORER_PANE_NAME, panelManager));
        actionManager.addAction(EDITOR_PANE_NAME, new TogglePanelAction(EDITOR_PANE_NAME, panelManager));

        // --- data related actions ------------------------------------/
        actionManager.addAction(NewDataElementAction.KEY, new NewDataElementAction());
        actionManager.addAction(RemoveDataElementAction.KEY, new RemoveDataElementAction());
        actionManager.addAction(ImportImageDataElementAction.KEY, new ImportImageDataElementAction());

        // --- Help & Updates & About -------------------------------------------/
        actionManager.addAction(AboutAction.KEY, new AboutAction());
        actionManager.addAction(HelpAction.KEY, new HelpAction("introduction"));
        //actionManager.addAction(ManageUpdatesAction.KEY, new ManageUpdatesAction());

        // --- print actions -------------------------------------------/
        action = new PrintAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Document activeDocument = Document.getActiveDocument();
                if( activeDocument != null )
                {
                    ViewPane viewPane = ( activeDocument instanceof DiagramDocument )
                            ? ( (DiagramDocument)activeDocument ).getDiagramViewPane() : activeDocument.getViewPane();
                    PrintManager.getPrintManager().print( viewPane.getContent() );
                }
            }
        };

        action.setEnabled(false);
        actionManager.addAction(PrintAction.KEY, action);

        action = new PrintPreviewAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Document activeDocument = Document.getActiveDocument();
                if( activeDocument != null )
                {
                    ViewPane viewPane = ( activeDocument instanceof DiagramDocument )
                            ? ( (DiagramDocument)activeDocument ).getDiagramViewPane() : activeDocument.getViewPane();
                    viewPane.resetScrollBars();
                    PrintManager.getPrintManager().preview(viewPane.getContent());
                }
            }
        };
        action.setEnabled(false);
        actionManager.addAction(PrintPreviewAction.KEY, action);

        actionManager.addAction(PrintSetupAction.KEY, new PrintSetupAction());

        // init all action properties from message bundle
        actionManager.initActions(biouml.workbench.resources.MessageBundle.class);
    }

    protected void initToolbar()
    {
        toolBar.addAction(NewDiagramAction.KEY);
        toolBar.addAction(SaveDocumentAction.KEY);
        toolBar.addAction(CloseDocumentAction.KEY);
        toolBar.addSeparator(GROUP_FILE);

        toolBar.addAction(PrintAction.KEY);
        toolBar.addAction(PrintPreviewAction.KEY);
        toolBar.addAction(PrintSetupAction.KEY);
        toolBar.addSeparator(GROUP_DOCUMENT);

        toolBar.addSeparator(GROUP_PANE);
        fillToolbarPaneActions();

        toolBar.addAction(PreferencesAction.KEY);
        toolBar.addSeparator(GROUP_SETUP);

        AboutAction.setStartupPluginName(Application.getGlobalValue("StartupPlugin", "biouml.workbench"));
        toolBar.addAction(AboutAction.KEY);
        toolBar.addSeparator(GROUP_HELP);

        fillToolbarJournalPane();
        fillToolbarPerspectivePane();
    }

    private void fillToolbarPerspectivePane()
    {
        if( PerspectiveRegistry.perspectives().count() <= 1 )
            return;
        JPanel perspectivePanel = new JPanel();
        final JComboBox<String> perspectivesBox = new JComboBox<>();
        PerspectiveRegistry.perspectives().map( Perspective::getTitle ).forEach( perspectivesBox::addItem );
        perspectivesBox.setSelectedItem(PerspectiveUI.getCurrentPerspective().toString());
        perspectivesBox.addActionListener(e -> Application.getPreferences().setValue(PerspectiveUI.PERSPECTIVE_PREFERENCE, perspectivesBox.getSelectedItem()));
        perspectivePanel.add(new JLabel(getMessageBundle().getResourceString("PERSPECTIVE_PROMPT")));
        perspectivePanel.add(perspectivesBox);
        toolBar.add(perspectivePanel);
    }

    protected void fillToolbarPaneActions()
    {
        ActionManager actionManager = Application.getActionManager();
        Action action = null;

        action = actionManager.getAction(REPOSITORY_PANE_NAME);
        toolBar.addToggleButtonAt(action, GROUP_PANE, true);

        action = actionManager.getAction(DOCUMENT_PANE_NAME);
        toolBar.addToggleButtonAt(action, GROUP_PANE, true);

        action = actionManager.getAction(EXPLORER_PANE_NAME);
        toolBar.addToggleButtonAt(action, GROUP_PANE, true);

        action = actionManager.getAction(EDITOR_PANE_NAME);
        toolBar.addToggleButtonAt(action, GROUP_PANE, true);
    }

    protected JComboBox<String> journalBox = null;

    protected void fillToolbarJournalPane()
    {
        JPanel rightPanel = new JPanel(new BorderLayout());
        String[] journalNames = JournalRegistry.getJournalNames();
        if( journalNames != null )
        {
            JPanel journalPanel = new JPanel();
            journalPanel.add(new JLabel(getMessageBundle().getResourceString("JOURNAL_USE")));
            final JCheckBox useJournalBox = new JCheckBox();
            useJournalBox.setSelected(true);
            useJournalBox.addChangeListener(e -> JournalRegistry.setJournalUse(useJournalBox.isSelected()));
            journalPanel.add(useJournalBox);
            journalPanel.add(new JLabel(getMessageBundle().getResourceString("JOURNAL_NAME")));
            journalBox = new JComboBox<>();
            updateJournalBox(journalNames);
            JournalRegistry.setCurrentJournal((String)journalBox.getSelectedItem());
            journalBox.addActionListener(e -> {
                if( "comboBoxChanged".equals(e.getActionCommand()) )
                {
                    String selectedJournal = (String)journalBox.getSelectedItem();
                    JournalRegistry.setCurrentJournal(selectedJournal);
                }
            });
            journalPanel.add(journalBox);

            rightPanel.add(journalPanel, BorderLayout.EAST);
        }
        toolBar.add(rightPanel);
    }
    public void updateJournalBox(String[] journalNames)
    {
        if( ( journalBox != null ) && ( journalNames != null ) )
        {
            journalBox.removeAllItems();
            for( String journalName : journalNames )
            {
                journalBox.addItem(journalName);
            }
            String currentJournal = JournalProperties.getCurrentJournal();
            if( currentJournal != null )
            {
                journalBox.setSelectedItem(currentJournal);
            }
        }
    }

    protected void initMenubar()
    {
        ActionManager actionManager = Application.getActionManager();
        Action action = null;

        try
        {
            OpenSetupWizardAction wizardAction = (OpenSetupWizardAction)actionManager.getAction(OpenSetupWizardAction.KEY);
            URL logoURL = this.getClass().getClassLoader().getResource(SETUP_WIZARD_LOGO);
            wizardAction.init(WizardPageRegistry.getWizardPages(), getMessageBundle().getResourceString("SETUP_WIZARD_TITLE"),
                    SetupWizardSupport.getSetupPreferences(), new ImageIcon(logoURL));
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not init setup wizard properties", e);
        }

        Set<String> topNames = new HashSet<>();

        // --- File menu -----------------------------------------------/
        ApplicationMenu fileMenu = new ApplicationMenu(getMessageBundle().getResourceString("MENU_FILE"));

        fileMenu.addAction(NewDiagramAction.KEY);
        fileMenu.addSeparator();
        fileMenu.addAction(CloseDocumentAction.KEY);
        fileMenu.addAction(CloseAllDocumentAction.KEY);
        fileMenu.addSeparator();
        fileMenu.addAction(OpenPathAction.KEY);
        fileMenu.addAction(SaveDocumentAction.KEY);
        fileMenu.addAction(SaveAsDocumentAction.KEY);
        fileMenu.addAction(ExportDocumentAction.KEY);
        fileMenu.addAction(ImportElementAction.KEY);
        fileMenu.addSeparator();
        fileMenu.addAction(LoginAction.KEY);
        fileMenu.addAction(RegisterAction.KEY);
        fileMenu.addAction(LogoutAction.KEY);
        fileMenu.addSeparator();
        fileMenu.addAction(OpenSetupWizardAction.KEY);
        fileMenu.addSeparator();
        fileMenu.addAction(PrintAction.KEY);
        fileMenu.addAction(PrintPreviewAction.KEY);
        fileMenu.addAction(PrintSetupAction.KEY);

        initExtendedMenuItems(fileMenu, actionManager, true);

        // enable/disable issues
        action = actionManager.getAction(ExportModuleAction.KEY);
        DataCollection<?> dc = null;
        try
        {
            dc = CollectionFactoryUtils.getDatabases();
        }
        catch( Exception e )
        {
        }
        action.setEnabled(dc != null && dc.getSize() != 0);

        menuBar.add(fileMenu);
        topNames.add(fileMenu.getText());

        //      --- Module menu -----------------------------------------------/
        DiagramDocument.getActionsByStaticWay( ActionType.MENU_ACTION ); // init diagram actions
        ApplicationMenu moduleMenu = createDatabasesMenu();
        menuBar.add(moduleMenu);
        topNames.add(moduleMenu.getText());

        for(MenuItemRegistry.ItemInfo info: MenuItemRegistry.getMenuItems())
        {
            String parent = info.getParent();
            if( !topNames.contains(parent) )
            {
                ApplicationMenu newMenu = new ApplicationMenu(parent);
                initExtendedMenuItems(newMenu, actionManager, false);
                menuBar.add(newMenu);
                topNames.add(newMenu.getText());
            }
        }

        // --- help menu -------------------------------------------/
        ApplicationMenu helpMenu = new ApplicationMenu(getMessageBundle().getResourceString("MENU_HELP"));

        helpMenu.addAction(actionManager.getAction(HelpAction.KEY));
        //helpMenu.addAction(actionManager.getAction(ManageUpdatesAction.KEY));
        helpMenu.addAction(actionManager.getAction(AboutAction.KEY));

        menuBar.add(helpMenu);
    }

    public static ApplicationMenu createDatabasesMenu()
    {
        ApplicationMenu moduleMenu = new ApplicationMenu(getMessageBundle().getResourceString("MENU_DATABASE"));

        moduleMenu.addAction(NewModuleAction.KEY);
        moduleMenu.addAction(NewCompositeModuleAction.KEY);

        moduleMenu.addSeparator();
        moduleMenu.addAction(ModuleSetupAction.KEY);
        moduleMenu.addAction(ExportModuleAction.KEY);

        initExtendedMenuItems(moduleMenu, Application.getActionManager(), true);
        return moduleMenu;
    }

    public static void initExtendedMenuItems(ApplicationMenu menu, ActionManager actionManager, boolean needSeparator)
    {
        boolean isFirst = needSeparator;
        for(MenuItemRegistry.ItemInfo info: MenuItemRegistry.getMenuItems())
        {
            try
            {
                if( info.getParent().equals(menu.getText()) )
                {
                    if( isFirst )
                    {
                        menu.addSeparator();
                        isFirst = false;
                    }
                    Action action = info.getAction();
                    try
                    {
                        Class<?> bundle = action.getClass().getClassLoader()
                                .loadClass( action.getClass().getPackage().getName() + ".MessageBundle" );
                        ActionInitializer initializer = new ActionInitializer( bundle );
                        initializer.initAction(action);
                    }
                    catch( ClassNotFoundException e )
                    {
                        log.info( "No message bundle defined for action "+action.getClass().getName() );
                    }
                    actionManager.addAction(info.getTitle(), action);
                    menu.addAction(info.getTitle());
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, ExceptionRegistry.log( e ));
                //just skip this menu
            }
        }
    }

    protected void initPanels()
    {
        try
        {
            RepositoryTabs repositoryTabs = new RepositoryTabs();
            RepositoryDocument repositoryDocument = new RepositoryDocument(this, repositoryTabs);
            RepositoryDocument.RepositoryUndoListener repositoryUndoListener = new RepositoryDocument.RepositoryUndoListener(
                    repositoryDocument);
            PluginActions actionsProvider = new PluginActions();
            repositoryTabs.setActionsProvider(actionsProvider);
            repositoryTabs.addListener(actionsProvider);

            CollectionFactoryUtils.getDatabases().addDataCollectionListener(repositoryUndoListener);

            repositoryTabs.addListener(repositoryDocument);
            repositoryTabs.addListener(new ModuleHandler());

            PanelInfo repositoryPanelInfo = new PanelInfo(REPOSITORY_PANE_NAME, repositoryTabs, true, null);
            panelManager.addPanel(repositoryPanelInfo, null, 0);

            // editors
            ExplorerPane explorerPane = new ExplorerPane();
            explorerPane.getPropertiesView().setPropertyShowMode(PropertyInspector.SHOW_USUAL);
            explorerPane.setModelActionProvider(actionsProvider);
            PanelInfo explorerPaneInfo = new PanelInfo(EXPLORER_PANE_NAME, explorerPane, true, null);
            panelManager.addPanel(explorerPaneInfo, REPOSITORY_PANE_NAME, PanelInfo.BOTTOM, 400);

            PluggedEditorsTabbedPane editorsPane = new PluggedEditorsTabbedPane();
            explorerPane.addViewPart(editorsPane.getEditorsManager(), true);
            PanelInfo editorsPaneInfo = new PanelInfo(EDITOR_PANE_NAME, editorsPane, true, null);
            panelManager.addPanel(editorsPaneInfo, EXPLORER_PANE_NAME, PanelInfo.RIGHT, 500);

            DocumentsPane documentsPane = new DocumentsPane();
            PanelInfo documentsPaneInfo = new PanelInfo(DOCUMENT_PANE_NAME, documentsPane, true, null);
            panelManager.addPanel(documentsPaneInfo, REPOSITORY_PANE_NAME, PanelInfo.RIGHT, 300);

            DocumentManager manager = (DocumentManager)getDocumentManager();
            manager.registerEditorPart(explorerPane);
            manager.registerEditorPart(editorsPane);
            manager.setActiveDocument(null);
        }
        catch( Exception t )
        {
            log.log(Level.SEVERE, "Error during init panels", t);
        }
    }


    // /////////////////////////////////////////////////////////////////

    @Override
    public void saveDocument()
    {
        Document document = Document.getCurrentDocument();
        if( document == null )
            return;
        saveDocument(document);
    }

    public static void saveDocument(Document document)
    {
        document.save();
        document.update();
    }

    /**
     * @return TRUE if application may be closed, FALSE to prevent application
     *         close
     */
    public boolean closeApplication()
    {
        try
        {
            Document document = null;
            while( ( document = GUI.getManager().getCurrentDocument() ) != null )
            {
                if( !askSaveConfirmation(document) )
                    GUI.getManager().removeDocument( document );
                else
                {
                    if( !saveDocumentConfirmDialog() )
                        return false;
                }
            }

            if( TaskManager.getInstance().hasIncompleteTasks() )
            {
                if( closeTasksConfirmDialog() )
                {
                    TaskManager.getInstance().stopAllTasks();
                }
                else
                {
                    return false;
                }
            }

            // close plugins
            Plugins.getPlugins().close();
            Framework.closeRepositories();
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, t.getMessage(), t);
        }
        return true;
    }

    protected static boolean saveDocumentConfirmDialog()
    {
        Document document = GUI.getManager().getCurrentDocument();

        if( !document.isChanged() || saveDocumentConfirmDialog(document, document.getDisplayName()) )
        {
            GUI.getManager().removeDocument(document);
            return true;
        }
        return false;
    }

    public static boolean saveDocumentConfirmDialog(Document document, String name)
    {
        String title = BioUMLApplication.getMessageBundle().getResourceString("CLOSE_CONFIRM_TITLE");
        if( document.isMutable() )
        {
            String message = MessageFormat.format(BioUMLApplication.getMessageBundle().getResourceString("CLOSE_CONFIRM_MESSAGE"),
                    new Object[] {name});
            int answer = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message, title, JOptionPane.YES_NO_CANCEL_OPTION);
            switch( answer )
            {
                case JOptionPane.YES_OPTION:
                    saveDocument(document);
                    break;
                case JOptionPane.NO_OPTION:
                    break;
                case JOptionPane.CANCEL_OPTION:
                    return false;
            }
        }
        else
        {
            String message = MessageFormat.format(BioUMLApplication.getMessageBundle().getResourceString("CLOSE_CONFIRM_MESSAGE_2"),
                    new Object[] {name});
            int answer = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message, title, JOptionPane.YES_NO_OPTION);
            switch( answer )
            {
                case JOptionPane.YES_OPTION:
                    break;
                case JOptionPane.NO_OPTION:
                    return false;
            }
        }
        return true;
    }

    public static boolean closeTasksConfirmDialog()
    {
        String title = BioUMLApplication.getMessageBundle().getResourceString("CLOSE_CONFIRM_TITLE");
        String message = BioUMLApplication.getMessageBundle().getResourceString("CLOSE_CONFIRM_TASKS");
        int answer = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message, title, JOptionPane.YES_NO_OPTION);
        if( answer == JOptionPane.YES_OPTION )
        {
            return true;
        }
        return false;
    }

    @Override
    public void closeCurrentDocument()
    {
        if( saveDocumentConfirmDialog() )
        {
            enableDocumentActions(!GUI.getManager().getDocuments().isEmpty());

            stateChanged(null);
        }
    }

    @Override
    public void enableDocumentActions(boolean flag)
    {
        Application.getActionManager().enableActions( flag, CloseDocumentAction.KEY, CloseAllDocumentAction.KEY,
                SaveAsDocumentAction.KEY, ExportDocumentAction.KEY );

        enableSaveDocumentActions(flag);
    }

    protected void enableSaveDocumentActions(boolean flag)
    {
        Application.getActionManager().enableActions( flag, SaveDocumentAction.KEY );
    }

    @Override
    public void updateSelection(ViewPane viewPane)
    {
        accessProvider.updateSelection( viewPane );
    }

    @Override
    public ViewPaneListener getDocumentViewListener()
    {
        return accessProvider.getDocumentViewListener();
    }

    private static MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());
    public static MessageBundle getMessageBundle()
    {
        return messageBundle;
    }

    private static class ModuleHandler implements RepositoryListener
    {
        @Override
        public void nodeClicked(DataElement node, int clickCount)
        {
            if( node != null )
            {
                Module module = Module.optModule(node);
                boolean enable = false;
                if( (node instanceof Module  && module != null) || node instanceof GenericFileDataCollection || node instanceof GenericDataCollection)
                    enable = true;
                Application.getActionManager().enableActions( enable, ImportElementAction.KEY );
            }
        }
        @Override
        public void selectionChanged(DataElement node)
        {
        }
    }

    // ////////////////////////////////////////////
    // ChangeListener implementation
    //

    @Override
    public void stateChanged(ChangeEvent e)
    {
        enableDocumentActions(!GUI.getManager().getDocuments().isEmpty());

        boolean diagramSaveActionsEnabled = false;
        Document document = GUI.getManager().getCurrentDocument();
        if( document != null && document.isMutable() )
            diagramSaveActionsEnabled = true;

        if( document != null && !document.getUndoManager().canUndo() )
            diagramSaveActionsEnabled = false;
        enableSaveDocumentActions(diagramSaveActionsEnabled);
    }

    @Override
    public boolean saveDocumentCurrentApplicationConfirmDialog(Document document, String displayName)
    {
        return saveDocumentConfirmDialog(document, displayName);
    }

    @Override
    public boolean askSaveConfirmation(Document doc)
    {
        return accessProvider.askSaveConfirmation( doc );
    }

    protected void login()
    {
        if( SingleSignOnSupport.isSSOUsed() )
        {
            LoginDialog loginDialog = new LoginDialog(this, "Login",
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
                    ApplicationUtils.errorBox( "Login failed", "Incorrect user name or password" );
                    login();
                }
            }
        }
    }

    public static void showSetupWizard()
    {
        try
        {
            Preferences setupPreferences = SetupWizardSupport.getSetupPreferences();
            if( SetupWizardSupport.isActive() )
            {
                URL logoURL = BioUMLApplication.class.getClassLoader().getResource(SETUP_WIZARD_LOGO);
                SetupWizardDialog wizard = new SetupWizardDialog(Application.getApplicationFrame(), WizardPageRegistry.getWizardPages(), getMessageBundle()
                        .getResourceString("SETUP_WIZARD_TITLE"), setupPreferences, new ImageIcon(logoURL));
                wizard.showWizard();
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not init setup wizard preferences", e);
        }
    }
}
