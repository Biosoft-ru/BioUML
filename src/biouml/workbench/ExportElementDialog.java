package biouml.workbench;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.DataElementExporterRegistry.ExporterInfo;
import ru.biosoft.gui.Document;
import biouml.workbench.diagram.DiagramDocument;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

@SuppressWarnings ( "serial" )
public class ExportElementDialog extends ProcessElementDialog
{
    protected JTextField tfPath = new JTextField(30);

    protected DataElement dataElement;
    protected JLabel currentType = new JLabel();
    protected JComboBox<String> formatComboBox = new JComboBox<>();

    protected static final String PREFERENCES_EXPORT_DIRECTORY = "exportDialog.exportDirectory";
    protected String exportDirectory;

    ///////////////////////////////////////////////////////////////////
    // Constructor
    //

    public ExportElementDialog(JFrame parent, DataElement dataElement)
    {
        super(parent, "EXPORT_ELEMENT_DIALOG_TITLE");
        log = Logger.getLogger(ExportElementDialog.class.getName());
        this.dataElement = dataElement;

        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        //--- File name ---
        contentPane.add(new JLabel(messageBundle.getResourceString("DIALOG_FILE")), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(tfPath, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));

        JButton browseButton = new JButton("...");
        contentPane.add(browseButton, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
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
            JFileChooser chooser = new JFileChooser(new File(exportDirectory));
            int res = chooser.showOpenDialog(Application.getApplicationFrame());
            if( res == JFileChooser.APPROVE_OPTION )
            {
                File file = chooser.getSelectedFile();
                tfPath.setText(file.getPath());
                update();
                exportDirectory = chooser.getCurrentDirectory().getAbsolutePath();
            }
        });

        //--- format ---
        contentPane.add(new JLabel(messageBundle.getResourceString("DIALOG_FORMAT")), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        contentPane.add(formatComboBox, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
        initFormats();

        //--- logging settings ---
        initAppender("Element export log", "");

        contentPane.add(new JLabel(messageBundle.getResourceString("DIALOG_INFO")), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));

        contentPane.add(appender.getLogTextPanel(), new GridBagConstraints(0, 4, 3, 3, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), 0, 0));

        //--- dialog settings ---
        setContent(contentPane);

        okButton.setText(messageBundle.getResourceString("EXPORT_ELEMENT_DIALOG_EXPORT"));
        okButton.setPreferredSize(okButton.getMinimumSize());
        okButton.setEnabled(false);
        cancelButton.setText(messageBundle.getResourceString("DIALOG_CLOSE"));

        loadPreferences();
    }

    protected void initFormats()
    {
        List<String> formats = DataElementExporterRegistry.getExporterFormats(dataElement);
        if( formats.size() == 0 )
            info("EXPORT_ELEMENT_DIALOG_NO_EXPORTERS");
        for(String format: formats)
        {
            formatComboBox.addItem(format);
        }
    }

    protected void loadPreferences()
    {
        String key = Preferences.DIALOGS + "/" + PREFERENCES_EXPORT_DIRECTORY;
        exportDirectory = Application.getPreferences().getStringValue(key, ".");
    }

    protected void savePreferences()
    {
        String key = Preferences.DIALOGS + "/" + PREFERENCES_EXPORT_DIRECTORY;
        if( Application.getPreferences().getProperty(key) != null )
            Application.getPreferences().setValue(key, exportDirectory);
        else
        {
            try
            {
                Preferences preferences = Application.getPreferences().getPreferencesValue(Preferences.DIALOGS);
                preferences.add( new DynamicProperty( PREFERENCES_EXPORT_DIRECTORY,
                        messageBundle.getResourceString( "EXPORT_ELEMENT_DIALOG_PREFERENCES_DIR_PN" ),
                        messageBundle.getResourceString( "EXPORT_ELEMENT_DIALOG_PREFERENCES_DIR_PD" ), String.class, exportDirectory ) );
            }
            catch( Exception e )
            {
            }
        }
    }

    protected void update()
    {
        String fileName = tfPath.getText();
        okButton.setEnabled(fileName != null && fileName.length() > 0 && formatComboBox.getItemCount() > 0);
    }

    @Override
    protected void okPressed()
    {
        String format = (String)formatComboBox.getSelectedItem();
        String fileName = tfPath.getText();
        savePreferences();

        try
        {
            ExporterInfo[] exporterInfo = DataElementExporterRegistry.getExporterInfo(format, dataElement);

            // info should be mot empty because previously we have selected only suitable formats
            if( exporterInfo == null )
            {
                error("EXPORT_ELEMENT_DIALOG_NO_EXPORTER");
                return;
            }

            /**
             * @todo Implement suitable dialog for users ability to choose
             * one of feasible exporters
             */
            if( exporterInfo.length > 1 )
            {
                info("There are more than one exporters available for the given format:");
                for( int i = 0; i < exporterInfo.length; i++ )
                    info( ( i + 1 ) + ". " + exporterInfo[i].getExporter().getClass().getName());
                info("The first one " + exporterInfo[0].getExporter().getClass().getName() + " will be used.");
            }

            String suffix = exporterInfo[0].getSuffix();
            if( suffix.indexOf('.') == -1 )
                suffix = "." + suffix;

            if( fileName.endsWith(suffix) )
                suffix = "";

            File file = new File(fileName + suffix);

            DataElementExporter exporter = exporterInfo[0].cloneExporter();
            Object exportProperties = exporter.getProperties(dataElement, file);
            if( ( exportProperties != null ) )
            {
                Property scaleProperty = ComponentFactory.getModel(exportProperties).findProperty("scale");
                // Read current zoom level from opened document
                if(scaleProperty != null)
                {
                    Document document = Document.getCurrentDocument();
                    if(document instanceof DiagramDocument && ((DiagramDocument)document).getDiagram()==dataElement)
                    {
                        scaleProperty.setValue(((DiagramDocument)document).getViewPane().getScaleX());
                    }
                }
                OkCancelDialog dialog = createPropertiesDialog(exportProperties);
                if( !dialog.doModal() )
                {
                    cancelPressed();
                    return;
                }
            }
            long start = System.currentTimeMillis();
            exporter.doExport(dataElement, file);
            String time = "" + ( System.currentTimeMillis() - start );
            info("EXPORT_ELEMENT_DIALOG_SUCCESS", dataElement.getName(), format, file.getAbsolutePath(), time);
        }
        catch( Exception e )
        {
            error("EXPORT_ELEMENT_DIALOG_ERROR", dataElement.getName(), format, e.getMessage());
        }
    }

    protected OkCancelDialog createPropertiesDialog(Object properties)
    {
        PropertyInspector propertyInspector = new PropertyInspector();
        propertyInspector.explore(properties);
        OkCancelDialog dialog = new OkCancelDialog(this, "Export properties");
        dialog.add(propertyInspector);
        return dialog;
    }
}
