package biouml.plugins.biopax;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.jar.Manifest;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Module;
import biouml.plugins.biopax.writer.BioPAXWriter;
import biouml.plugins.biopax.writer.BioPAXWriterFactory;
import biouml.workbench.BioUMLApplication;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.support.NameListToComboBoxModelAdapter;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;

@SuppressWarnings ( "serial" )
public class BioPAXModuleExportDialog extends OkCancelDialog implements JobControlListener
{
    private static final DataElementPath DATABASES_PATH = DataElementPath.create("databases");
    protected File selectedFile;
    protected Manifest manifest;
    protected Module module;
    protected JTextField pathTextField = new JTextField(20);
    protected JComboBox<String> formatComboBox = new JComboBox<>();
    protected JProgressBar progressBar = new JProgressBar();
    //protected BioPAXExporter be;
    private BioPAXWriter writer;
    protected FunctionJobControl jobControl;
    protected JButton browseButton;
    
    protected static final String PREFERENCES_IMPORT_DIRECTORY = "exportDialog.currentDirectory";
    private static final String BIOPAX_DIALOG_FORMAT_LEVEL = "Format:";
    protected String currentDirectory;

    public BioPAXModuleExportDialog(JDialog dialog, String title, Module module)
    {
        super(dialog, title);
        init(module);
    }

    public BioPAXModuleExportDialog(JFrame frame, String title, Module module)
    {
        super(frame, title);
        init(module);
    }

    private void updateModuleInfo()
    {

    }

    private void init(Module inModule)
    {
        this.module = inModule;

        JPanel content = new JPanel( new GridBagLayout() );
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        JComboBox<String> modulesComboBox = new JComboBox<>();
        modulesComboBox.setModel(new NameListToComboBoxModelAdapter(CollectionFactoryUtils.getDatabases()));
        modulesComboBox.addItemListener(e -> {
            String moduleName = (String)e.getItem();
            module = DATABASES_PATH.getChildPath(moduleName).getDataElement(Module.class);
            updateModuleInfo();
        });
        if( inModule != null )
        {
            modulesComboBox.setSelectedItem(inModule.getName());
        }

        browseButton = new JButton("...");
        
        formatComboBox.addItem(BioPAXSupport.BIOPAX_LEVEL_3);
        formatComboBox.addItem(BioPAXSupport.BIOPAX_LEVEL_2);

        pathTextField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                updateOKButton();
            }
        });
        
        progressBar.setMaximum(0);
        progressBar.setMaximum(100);

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(currentDirectory));
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileFilter(new BioPAXFileFilter());
            int res = chooser.showSaveDialog(Application.getApplicationFrame());
            if( res == JFileChooser.APPROVE_OPTION )
            {
                selectedFile = chooser.getSelectedFile();
                if( !selectedFile.getName().toLowerCase().endsWith(BioPAXFileFilter.OWL_EXTENTION) )
                {
                    selectedFile = new File(selectedFile.getPath() + BioPAXFileFilter.OWL_EXTENTION);
                }
                pathTextField.setText(selectedFile.getPath());
                currentDirectory = selectedFile.getPath();
                updateOKButton();
            }
        });

        updateOKButton();
        
        
        

        content.add(new JLabel("Selected module"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        content.add(new JLabel(module.getName()), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 0));

        content.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString("FILE_LOCATION")), new GridBagConstraints(0, 4, 1, 1,
                0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        content.add(pathTextField, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 0, 0), 0, 0));
        content.add(browseButton, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 0, 0), 0, 0));
        
        content.add(new JLabel(BIOPAX_DIALOG_FORMAT_LEVEL), new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        content.add(formatComboBox, new GridBagConstraints(1, 5, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
        
        content.add(progressBar, new GridBagConstraints(0, 6, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 0, 0), 0, 0));

        setContent(content);
        loadPreferences();
    }

    private void updateOKButton()
    {
        String filename = pathTextField.getText();
        boolean correctFileName = ( filename != null && filename.length() > 0 );
        okButton.setEnabled(correctFileName);
    }

    @Override
    protected void okPressed()
    {
        
        jobControl = new FunctionJobControl(null);
        jobControl.addListener(this);
        savePreferences();
        writer = BioPAXWriterFactory.getWriter((String)formatComboBox.getSelectedItem());
        (new Thread()
        {
            @Override
            public void run()
            {
                writer.write(module, new File(pathTextField.getText()), jobControl);
            }
        }).start();
    }
    
    @Override
    protected void cancelPressed()
    {
        if(jobControl != null && jobControl.getStatus()==FunctionJobControl.RUNNING)
        {
            jobControl.terminate();
        }
        super.cancelPressed();
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
        pathTextField.setEnabled(false);
        browseButton.setEnabled(false);
        okButton.setEnabled(false);
    }
    
    protected void loadPreferences()
    {
        String key = Preferences.DIALOGS + "/" + PREFERENCES_IMPORT_DIRECTORY;
        currentDirectory = Application.getPreferences().getStringValue(key, ".");
    }

    protected void savePreferences()
    {
        String key = Preferences.DIALOGS + "/" + PREFERENCES_IMPORT_DIRECTORY;
        if( Application.getPreferences().getProperty(key) != null )
            Application.getPreferences().setValue(key, currentDirectory);
        else
        {
            try
            {
                Preferences preferences = Application.getPreferences().getPreferencesValue(Preferences.DIALOGS);
                preferences.add(new DynamicProperty(PREFERENCES_IMPORT_DIRECTORY,String.class, currentDirectory));
            }
            catch(Exception e){}
        }
    }
}
