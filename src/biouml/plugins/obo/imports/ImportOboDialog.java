package biouml.plugins.obo.imports;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.plugins.obo.OboModuleType;
import biouml.workbench.BioUMLApplication;
import biouml.workbench.ImportElementDialog;
import biouml.workbench.ProcessElementDialog;
import biouml.workbench.resources.MessageBundle;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.Repository;

@SuppressWarnings ( "serial" )
public class ImportOboDialog extends ProcessElementDialog
{
    protected JTextField tfModuleName = new JTextField(30);
    protected JTextField tfPath = new JTextField(30);

    protected Diagram diagram;
    protected JLabel currentType = new JLabel();

    protected static final String PREFERENCES_IMPORT_DIRECTORY = "importDialog.importDirectory";
    protected String importDirectory;
    protected MessageBundle resources;

    private final static String OBO_DIALOG_FILE = "OBO file:";
    private final static String OBO_DIALOG_DATABASE = "Database name:";
    private final static String OBO_DIALOG_LOG_INFO = "To import OBO database please specify file name and database name\n";

    private final static String IMPORT_OBO_DIALOG_TITLE = "Import OBO";
    
    ///////////////////////////////////////////////////////////////////
    // Constructor
    //

    public ImportOboDialog()
    {
        super(IMPORT_OBO_DIALOG_TITLE);

        log = Logger.getLogger(ImportElementDialog.class.getName());
        resources = BioUMLApplication.getMessageBundle();

        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        //--- File name ---
        contentPane.add(new JLabel(OBO_DIALOG_FILE), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(tfPath, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));

        JButton browseButton = new JButton("...");
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

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(new File(importDirectory));
            int res = chooser.showOpenDialog(Application.getApplicationFrame());
            if( res == JFileChooser.APPROVE_OPTION )
            {
                File file = chooser.getSelectedFile();
                tfPath.setText(file.getPath());
                update();
                importDirectory = chooser.getCurrentDirectory().getAbsolutePath();
            }
        });

        //--- module name ---
        contentPane.add(new JLabel(OBO_DIALOG_DATABASE), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(tfModuleName, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));

        //--- logging settings ---
        initAppender("BioPAX import log", OBO_DIALOG_LOG_INFO);

        contentPane.add(new JLabel(messageBundle.getResourceString("DIAGRAM_DIALOG_INFO")), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));

        contentPane.add(appender.getLogTextPanel(), new GridBagConstraints(0, 4, 3, 3, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), 0, 0));

        //--- dialog settings ---
        setContent(contentPane);

        okButton.setText( "Import" );
        okButton.setPreferredSize(okButton.getMinimumSize());
        okButton.setEnabled(false);
        cancelButton.setText(messageBundle.getResourceString("DIAGRAM_DIALOG_CLOSE"));

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
        okButton.setEnabled(fileName != null && fileName.length() > 0 && moduleName != null && !moduleName.equals(""));
    }

    @Override
    protected void okPressed()
    {
        String filename = tfPath.getText();
        String name = tfModuleName.getText();
        String type = null;
        savePreferences();
        try
        {
            OboModuleType moduleType = new OboModuleType();
            moduleType.setFileName(filename);
            moduleType.createModule((Repository)CollectionFactoryUtils.getDatabases(), name);
            log.info( "Import done." );
        }
        catch( Throwable t )
        {
            String title = resources.getResourceString("NEW_DATABASE_ERROR_TITLE");
            String message = message("NEW_DATABASE_ERROR", new String[] {name, type, t.getMessage()});
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            log.log(Level.SEVERE, message, t);
        }
    }

    protected String message(String key, String[] params)
    {
        String message = resources.getString(key);
        return MessageFormat.format(message, (Object[])params);
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
                preferences.add(new DynamicProperty(PREFERENCES_IMPORT_DIRECTORY,String.class, importDirectory));
            }
            catch(Exception e){}
        }
    }
}
