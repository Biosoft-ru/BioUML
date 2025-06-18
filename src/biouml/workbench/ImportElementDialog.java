package biouml.workbench;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.DataElementImporterRegistry.ImporterInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.RepositoryTabs;
import ru.biosoft.gui.DocumentManager;
import ru.biosoft.gui.GUI;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.PropertiesDialog;
import ru.biosoft.util.TextUtil2;

@SuppressWarnings ( "serial" )
public class ImportElementDialog extends ProcessElementDialog implements JobControlListener
{
    protected JTextField tfPath = new JTextField(30);

    protected JLabel currentType = new JLabel();
    protected JComboBox<String> formatComboBox = new JComboBox<>();
    protected JProgressBar progressBar = new JProgressBar();
    private FunctionJobControl jobControl;

    protected DataCollection<?> parent;

    public static final String PREFERENCES_IMPORT_DIRECTORY = "importDialog.importDirectory";
    protected String importDirectory;

    ///////////////////////////////////////////////////////////////////
    // Constructor
    //

    public ImportElementDialog(DataCollection<?> parent)
    {
        super("IMPORT_ELEMENT_DIALOG_TITLE");
        
        setSize(500, 300);

        log = Logger.getLogger(ImportElementDialog.class.getName());

        this.parent = parent;

        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        //--- File name ---
        contentPane.add(new JLabel(messageBundle.getResourceString("DIALOG_FILE")), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
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

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(new File(importDirectory));
            chooser.setMultiSelectionEnabled(true);
            int res = chooser.showOpenDialog(Application.getApplicationFrame());
            if( res == JFileChooser.APPROVE_OPTION )
            {
                File[] files = chooser.getSelectedFiles();

                tfPath.setText(StreamEx.of(files).joining(";"));
                update();
                importDirectory = chooser.getCurrentDirectory().getAbsolutePath();
            }
        });

        //--- format ---
        contentPane.add(new JLabel(messageBundle.getResourceString("DIALOG_FORMAT")), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(formatComboBox, new GridBagConstraints(1, 1, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

        progressBar.setMaximum(100);
        contentPane.add(progressBar, new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 10, 0, 0), 0, 0));

        //--- logging settings ---
        initAppender("Element import log", messageBundle.getResourceString("IMPORT_ELEMENT_DIALOG_INFO"));

        contentPane.add(new JLabel(messageBundle.getResourceString("DIALOG_INFO")), new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));

        contentPane.add(appender.getLogTextPanel(), new GridBagConstraints(0, 6, 3, 3, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), 0, 0));

        //--- dialog settings ---
        setContent(contentPane);

        okButton.setText(messageBundle.getResourceString("IMPORT_ELEMENT_DIALOG_IMPORT"));
        okButton.setPreferredSize(okButton.getMinimumSize());
        okButton.setEnabled(false);
        cancelButton.setText(messageBundle.getResourceString("DIALOG_CLOSE"));

        initFormats();
        loadPreferences();
    }

    protected void initFormats()
    {
        DataElementImporterRegistry.importers()
                .filter( info -> info.getImporter().accept( parent, null ) > DataElementImporter.ACCEPT_UNSUPPORTED )
                .map( ImporterInfo::getFormat )
                .prepend( DataElementImporterRegistry.AUTODETECT )
                .forEach( formatComboBox::addItem );
    }
    protected void loadPreferences()
    {
        importDirectory = Application.getPreferences().getStringValue(DataElementImporter.PREFERENCES_IMPORT_DIRECTORY, ".");
    }

    protected void savePreferences()
    {
        Application.getPreferences().addValue(DataElementImporter.PREFERENCES_IMPORT_DIRECTORY, importDirectory,
                DataElementImporter.PN_PREFERENCES_IMPORT_DIRECTORY, DataElementImporter.PD_PREFERENCES_IMPORT_DIRECTORY);
    }

    protected void update()
    {
        String fileName = tfPath.getText();
        okButton.setEnabled(fileName != null && fileName.length() > 0);
    }

    @Override
    protected void okPressed()
    {
        final String[] fileNames = TextUtil2.split( tfPath.getText(), ';' );
        savePreferences();

        try
        {
            ( new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        long totalStart = System.currentTimeMillis();
                        Integer totalOk = 0;
                        Integer totalFiles = 0;
                        for( JComponent c : new JComponent[] {tfPath, formatComboBox, okButton} )
                            c.setEnabled(false);
                        cancelButton.setText(messageBundle.getResourceString("DIALOG_CANCEL"));
                        for( String fileName : fileNames )
                        {
                            totalFiles++;
                            File file = new File(fileName);
                            if( !file.exists() )
                            {
                                error("INVALID_FILE_NAME", fileName);
                                continue;
                            }
                            info("IMPORT_ELEMENT_IMPORTING_FILE", fileName);

                            String format = (String)formatComboBox.getSelectedItem();
                            if( format.equals(DataElementImporterRegistry.AUTODETECT) )
                            {
                                DataElementImporterRegistry.ImporterInfo[] importerInfos = DataElementImporterRegistry
                                        .getAutoDetectImporter(file, parent, false);
                                if( importerInfos == null || importerInfos.length == 0 )
                                {
                                    error("NO_IMPORTERS_AVAILABLE", fileName);
                                    continue;
                                }

                                if( importerInfos.length > 1 )
                                {
                                    StringBuffer formats = new StringBuffer();
                                    for( ImporterInfo importerInfo : importerInfos )
                                    {
                                        formats.append("\n\t");
                                        formats.append(importerInfo.getFormat());
                                    }
                                    error("MORE_THAN_ONE_IMPORTER_AVAILABLE", fileName, formats.toString());
                                    continue;
                                }

                                format = importerInfos[0].getFormat();
                                info("IMPORTER_CHOSEN", fileName, format);
                            }

                            DataElementImporter importer = DataElementImporterRegistry.getImporter(file, format, parent);
                            if( importer == null )
                            {
                                error("NO_IMPORTERS_AVAILABLE", fileName, format);
                                continue;
                            }
                            //create dialog with properties if necessary
                            Object properties = importer.getProperties(parent, file, ApplicationUtils.getFileNameWithoutExtension(file.getName()));
                            if( properties instanceof Option )
                            {
                                PropertiesDialog dialog = createPropertiesDialog((Option)properties);
                                if( !dialog.doModal() )
                                {
                                    cancelPressed();
                                    return;
                                }
                            }

                            long start = System.currentTimeMillis();

                            info("IMPORT_ELEMENT_IMPORT_STARTED");
                            try
                            {
                                jobControl = new FunctionJobControl(null);
                                jobControl.addListener(ImportElementDialog.this);
                                final String finalFormat = format;
                                Journal journal = JournalRegistry.getCurrentJournal();
                                TaskInfo task = journal == null?null:journal.getEmptyAction();
                                DataElement importedDE = importer.doImport( parent, file,
                                        ApplicationUtils.getFileNameWithoutExtension( file.getName() ), jobControl, log );
                                if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                                {
                                    info("IMPORT_ELEMENT_IMPORT_CANCELLED");
                                    break;
                                }
                                if( jobControl.getStatus() == JobControl.COMPLETED )
                                {
                                    if(task != null)
                                    {
                                        task.setType(TaskInfo.IMPORT);
                                        task.setData(file.getAbsolutePath());
                                        task.getAttributes().add(
                                                new DynamicProperty(TaskInfo.IMPORT_OUTPUT_PROPERTY, String.class, DataElementPath.create(
                                                        parent, ApplicationUtils.getFileNameWithoutExtension(file.getName())).toString()));
                                        task.getAttributes().add(new DynamicProperty(TaskInfo.IMPORT_FORMAT_PROPERTY_DESCRIPTOR, String.class, format));
                                        if(properties != null)
                                        {
                                            DPSUtils.writeBeanToDPS(properties, task.getAttributes(), DPSUtils.PARAMETER_ANALYSIS_PARAMETER+".");
                                        }
                                        task.setEndTime();
                                        journal.addAction(task);
                                    }
                                    totalOk++;
                                    String time = String.valueOf(System.currentTimeMillis() - start);
                                    info("IMPORT_ELEMENT_DIALOG_SUCCESS", finalFormat, file.getAbsolutePath(), time);
                                    if( importedDE != null )
                                        GUI.getManager().getRepositoryTabs().selectElement( importedDE.getCompletePath() );
                                }
                                else
                                {
                                    log.info(jobControl.getTextStatus());
                                }
                            }
                            catch( Throwable t )
                            {
                                error("IMPORT_ELEMENT_FAILED", t.toString());
                            }
                        }
                        String time = String.valueOf(System.currentTimeMillis() - totalStart);
                        info("IMPORT_ELEMENT_COMPLETE", time, totalOk.toString(),
                                String.valueOf(totalFiles - totalOk));
                    }
                    catch( Exception ex )
                    {
                        error("ERROR_IMPORTING_ELEMENT", tfPath.getText(), formatComboBox.getSelectedItem(),
                                ex.getMessage());
                    }
                    for( JComponent c : new JComponent[] {tfPath, formatComboBox, okButton} )
                        c.setEnabled(true);
                    cancelButton.setText(messageBundle.getResourceString("DIALOG_CLOSE"));
                }
            } ).start();
        }
        catch( Exception ex )
        {
            error("ERROR_IMPORTING_ELEMENT", tfPath.getText(), formatComboBox.getSelectedItem(), ex.getMessage());
        }
    }

    @Override
    protected void cancelPressed()
    {
        if( jobControl != null && jobControl.getStatus() == FunctionJobControl.RUNNING )
        {
            jobControl.terminate();
        }
        else
        {
            super.cancelPressed();
        }
    }

    @Override
    public void jobPaused(JobControlEvent event)
    {
    }

    @Override
    public void jobResumed(JobControlEvent event)
    {
    }

    @Override
    public void jobStarted(JobControlEvent event)
    {
    }

    @Override
    public void jobTerminated(JobControlEvent event)
    {
        if( event.getJobControl().getStatus() == JobControl.TERMINATED_BY_ERROR )
            error("ERROR_IMPORTING_EXCEPTION", event.getMessage());
    }

    @Override
    public void resultsReady(JobControlEvent event)
    {
        DocumentManager documentManager = DocumentManager.getDocumentManager();
        RepositoryTabs repositoryTabs = GUI.getManager().getRepositoryTabs();
        for( Object result : event.getResults() )
            if( result instanceof DataElement )
            {
                DataElement de = (DataElement)result;
                documentManager.openDocument( de );
                repositoryTabs.selectElement( de.getCompletePath() );
            }
    }

    @Override
    public void valueChanged(JobControlEvent event)
    {
        progressBar.setValue(event.getPreparedness());
    }

    protected PropertiesDialog createPropertiesDialog(Option properties)
    {
        return new PropertiesDialog(this, "Import properties", properties);
    }
}
