package biouml.plugins.biopax.imports;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.apache.commons.lang.StringUtils;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.biopax.BioPAXFileFilter;
import biouml.plugins.biopax.BioPAXModuleType;
import biouml.plugins.biopax.BioPAXSQLModuleType;
import biouml.plugins.biopax.BioPAXTextModuleType;
import biouml.plugins.biopax.reader.BioPAXReaderFactory;
import biouml.plugins.server.access.SQLRegistry;
import biouml.plugins.server.access.SQLRegistry.SQLInfo;
import biouml.workbench.BioUMLApplication;
import biouml.workbench.ImportElementDialog;
import biouml.workbench.ProcessElementDialog;
import biouml.workbench.resources.MessageBundle;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.Repository;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.util.TextUtil;

@SuppressWarnings ( "serial" )
public class ImportBioPAXDialog extends ProcessElementDialog implements JobControlListener
{
    protected JTextField tfModuleName = new JTextField(30);
    protected JTextField tfPath = new JTextField(30);
    protected JComboBox<String> formatComboBox = new JComboBox<>();
    protected JComboBox<SQLInfo> sqlList = new JComboBox<>();
    protected JLabel sqlListLabel  = new JLabel(BIOPAX_DIALOG_SQL);

    protected Diagram diagram;
    protected JLabel currentType = new JLabel();

    protected JButton browseButton;
    protected FunctionJobControl jobControl;

    protected static final String PREFERENCES_IMPORT_DIRECTORY = "importDialog.importDirectory";
    protected String importDirectory;
    protected MessageBundle resources;

    protected JProgressBar progressBar = new JProgressBar();

    private final static String BIOPAX_DIALOG_FILE = "OWL file:";
    private final static String BIOPAX_DIALOG_DATABASE = "Database name:";

    private final static String BIOPAX_DIALOG_LOG_INFO = "To import BioPAX module please specify file name and database name";
    private final static String BIOPAX_DIALOG_LOG_INFO_2 = "To import BioPAX file please specify file name";

    private final static String BIOPAX_DIALOG_DATABASE_TYPE = "Database type:";

    private final static String BIOPAX_DATABASE_TYPE_OWL = "owl file";
    private final static String BIOPAX_DATABASE_TYPE_TEXT = "text";
    private final static String BIOPAX_DATABASE_TYPE_SQL = "sql";

    private final static String BIOPAX_DIALOG_SQL = "SQL connection:";

    private final static String IMPORT_BIOPAX_DIALOG_TITLE = "Import BioPAX";
    private final static String DIAGRAM_DIALOG_CANCEL = "Cancel";

    protected boolean isNewModule;
    protected Module module;
    ///////////////////////////////////////////////////////////////////
    // Constructor
    //
    public ImportBioPAXDialog()
    {
        this(true, null);
        this.isNewModule = true;
    }

    public ImportBioPAXDialog(final boolean isNewModule, Module module)
    {
        super(IMPORT_BIOPAX_DIALOG_TITLE);

        this.isNewModule = isNewModule;
        this.module = module;

        log = Logger.getLogger(ImportElementDialog.class.getName());
        resources = BioUMLApplication.getMessageBundle();

        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        //--- File name ---
        contentPane.add(new JLabel(BIOPAX_DIALOG_FILE), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(tfPath, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));

        browseButton = new JButton("...");
        contentPane.add(browseButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(5, 5, 0, 0), 0, 0));

        tfPath.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                update();
            }
        });

        tfModuleName.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                update();
            }
        });

        browseButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser chooser = new JFileChooser(new File(importDirectory));
                chooser.setFileFilter(new BioPAXFileFilter());
                chooser.setMultiSelectionEnabled(true);
                int res = chooser.showOpenDialog(Application.getApplicationFrame());
                if( res == JFileChooser.APPROVE_OPTION )
                {
                    File files[] = chooser.getSelectedFiles();
                    StringBuilder fileslist = new StringBuilder(files[0].getPath());
                    for( int i = 1; i < files.length; i++ )
                    {
                        fileslist.append( ";" ).append( files[i].getPath() );
                    }
                    tfPath.setText(fileslist.toString());
                    importDirectory = chooser.getCurrentDirectory().getAbsolutePath();
                    if(isNewModule)
                    {
                        String moduleName = files[0].getName();
                        Matcher m = Pattern.compile("(.+)(\\.\\w+)").matcher(moduleName);
                        if( m.matches() )
                        {
                            moduleName = m.group(1);
                        }
                        tfModuleName.setText(StringUtils.capitalize(moduleName));
                    }
                    update();
                }
            }
        });

        formatComboBox.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                String format = (String)formatComboBox.getSelectedItem();
                if(format.equals(BIOPAX_DATABASE_TYPE_SQL))
                {
                    sqlList.setVisible(true);
                    sqlListLabel.setVisible(true);
                }
                else
                {
                    sqlList.setVisible(false);
                    sqlListLabel.setVisible(false);
                }
            }});

        progressBar.setMaximum(0);
        progressBar.setMaximum(100);

        if( isNewModule )
        {
            //--- format ---
            formatComboBox.addItem(BIOPAX_DATABASE_TYPE_TEXT);
            formatComboBox.addItem(BIOPAX_DATABASE_TYPE_SQL);
            formatComboBox.addItem(BIOPAX_DATABASE_TYPE_OWL);

            contentPane.add(new JLabel(BIOPAX_DIALOG_DATABASE_TYPE), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
            contentPane.add(formatComboBox, new GridBagConstraints(1, 1, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

            //--- SQL connection list ---
            List<SQLInfo> sqlConnections = SQLRegistry.getSQLServersList();
            sqlList = new JComboBox<>(sqlConnections.toArray(new SQLInfo[sqlConnections.size()]));
            contentPane.add(sqlListLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
            contentPane.add(sqlList, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
            sqlList.setVisible(false);
            sqlListLabel.setVisible(false);

            //--- module name ---
            contentPane.add(new JLabel(BIOPAX_DIALOG_DATABASE), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
            contentPane.add(tfModuleName, new GridBagConstraints(1, 3, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
        }
        //--- progress bar

        contentPane.add(progressBar, new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 0, 0), 0, 0));

        //--- logging settings ---
        if( isNewModule )
        {
            initAppender("BioPAX import log", BIOPAX_DIALOG_LOG_INFO);
        }
        else
        {
            initAppender("BioPAX import log", BIOPAX_DIALOG_LOG_INFO_2);
        }

        contentPane.add(new JLabel(messageBundle.getResourceString("DIAGRAM_DIALOG_INFO")), new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));

        contentPane.add(appender.getLogTextPanel(), new GridBagConstraints(0, 6, 3, 3, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), 0, 0));

        //--- dialog settings ---
        setContent(contentPane);

        okButton.setText(messageBundle.getResourceString("IMPORT_ELEMENT_DIALOG_IMPORT"));
        okButton.setPreferredSize(okButton.getMinimumSize());
        okButton.setEnabled(false);
        cancelButton.setText(DIAGRAM_DIALOG_CANCEL);

        loadPreferences();
    }

    protected void loadPreferences()
    {
        String key = Preferences.DIALOGS + "/" + PREFERENCES_IMPORT_DIRECTORY;
        importDirectory = Application.getPreferences().getStringValue(key, ".");
    }

    protected void update()
    {
        String fileName = tfPath.getText();
        String moduleName = tfModuleName.getText();
        if( !isNewModule )
        {
            okButton.setEnabled(fileName != null && fileName.length() > 0);
            return;
        }
        okButton.setEnabled(fileName != null && fileName.length() > 0 && moduleName != null && !moduleName.equals(""));
    }

    @Override
    protected void okPressed()
    {
        String format = (String)formatComboBox.getSelectedItem();
        String filename = tfPath.getText();
        final String name = tfModuleName.getText();
        String type = null;
        savePreferences();
        try
        {
            final String[] filenames = TextUtil.split( filename, ';' );
            for(String file: filenames)
            {
                BioPAXReaderFactory.checkBioPAXFile(new File(file));
            }
            if( isNewModule )
            {
                if( format.equals(BIOPAX_DATABASE_TYPE_OWL) )
                {
                    BioPAXModuleType moduleType = new BioPAXModuleType();
                    moduleType.setFileNames(filenames);
                    moduleType.createModule((Repository)CollectionFactoryUtils.getDatabases(), name);
                    super.okPressed();
                }
                else if( format.equals(BIOPAX_DATABASE_TYPE_TEXT) )
                {
                    final String addFiles[] = new String[filenames.length - 1];
                    for( int i = 1; i < filenames.length; i++ )
                    {
                        addFiles[i - 1] = filenames[i];
                    }
                    jobControl = new FunctionJobControl(null);
                    jobControl.addListener(this);
                    ( new Thread()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                BioPAXTextModuleType moduleType = new BioPAXTextModuleType();
                                moduleType.setFileNames(filenames);
                                moduleType.setJobControl(jobControl);
                                module = moduleType.createModule((Repository)CollectionFactoryUtils.getDatabases(), name);

                                moduleType.addPathways(addFiles, module.getPrimaryCollection());
                            }
                            catch( Throwable t )
                            {
                                t.printStackTrace();
                            }
                        }
                    } ).start();
                }
                else if( format.equals(BIOPAX_DATABASE_TYPE_SQL))
                {
                    final String addFiles[] = new String[filenames.length - 1];
                    for( int i = 1; i < filenames.length; i++ )
                    {
                        addFiles[i - 1] = filenames[i];
                    }
                    jobControl = new FunctionJobControl(null);
                    jobControl.addListener(this);
                    ( new Thread()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                BioPAXSQLModuleType moduleType = new BioPAXSQLModuleType();
                                moduleType.setFileNames(filenames);
                                moduleType.setJobControl(jobControl);
                                moduleType.setDatabaseProperties((SQLInfo)sqlList.getSelectedItem());
                                module = moduleType.createModule((Repository)CollectionFactoryUtils.getDatabases(), name);
                                moduleType.addPathways(addFiles, module);
                            }
                            catch( Throwable t )
                            {
                                ApplicationUtils.errorBox(ExceptionRegistry.log(t));
                            }
                        }
                    } ).start();
                }
            }
            else
            {
                if( module != null && (module.getType() instanceof BioPAXTextModuleType || module.getType() instanceof BioPAXSQLModuleType) )
                {
                    jobControl = new FunctionJobControl(null);
                    jobControl.addListener(this);
                    ( new Thread()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                if( module.getType() instanceof BioPAXTextModuleType )
                                {
                                    BioPAXTextModuleType btmt = (BioPAXTextModuleType)module.getType();
                                    btmt.setJobControl(jobControl);
                                    btmt.addPathways(filenames, module.getPrimaryCollection());
                                }
                                else if( module.getType() instanceof BioPAXSQLModuleType )
                                {
                                    BioPAXSQLModuleType bsmt = (BioPAXSQLModuleType)module.getType();
                                    bsmt.setJobControl(jobControl);
                                    bsmt.addPathways(filenames, module);
                                }
                            }
                            catch( Throwable t )
                            {
                                ApplicationUtils.errorBox(ExceptionRegistry.log(t));
                            }
                        }
                    } ).start();
                }
            }
        }
        catch( Throwable t )
        {
            String title = resources.getResourceString("NEW_DATABASE_ERROR_TITLE");
            String message = message("NEW_DATABASE_ERROR", new String[] {name, type, ExceptionRegistry.log(t)});
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected void cancelPressed()
    {
        if( jobControl != null && jobControl.getStatus() == FunctionJobControl.RUNNING )
        {
            jobControl.terminate();
        }
        super.cancelPressed();
    }

    protected String message(String key, String[] params)
    {
        String message = resources.getString(key);
        return MessageFormat.format(message, (Object[])params);
    }

    @Override
    public void valueChanged(JobControlEvent event)
    {
        progressBar.setValue(event.getPreparedness());
    }

    @Override
    public void jobResumed(JobControlEvent event)
    {

    }

    @Override
    public void jobPaused(JobControlEvent event)
    {

    }

    @Override
    public void resultsReady(JobControlEvent event)
    {

    }

    @Override
    public void jobTerminated(JobControlEvent event)
    {
        super.okPressed();
    }

    @Override
    public void jobStarted(JobControlEvent event)
    {
        tfModuleName.setEnabled(false);
        tfPath.setEnabled(false);
        formatComboBox.setEnabled(false);
        appender.getLogTextPanel().setEnabled(false);
        okButton.setEnabled(false);
        browseButton.setEnabled(false);
    }

    protected void savePreferences()
    {
        String key = Preferences.DIALOGS + "/" + PREFERENCES_IMPORT_DIRECTORY;
        if( Application.getPreferences().getProperty(key) != null )
            Application.getPreferences().setValue(key, importDirectory);
        else
        {
            try
            {
                Preferences preferences = Application.getPreferences().getPreferencesValue(Preferences.DIALOGS);
                preferences.add(new DynamicProperty(PREFERENCES_IMPORT_DIRECTORY, String.class, importDirectory));
            }
            catch( Exception e )
            {
            }
        }
    }
}