package biouml.workbench.htmlgen;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.dialog.OkCancelDialog;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.util.DiagramXmlWriter;
import biouml.model.util.ImageGenerator;
import biouml.workbench.BioUMLApplication;
import biouml.workbench.diagram.DiagramDocument;
import biouml.workbench.module.StatusInfoDialog;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.gui.Document;
import ru.biosoft.jobcontrol.FunctionJobControl;

@SuppressWarnings ( "serial" )
public class GenerateHTMLDialog extends OkCancelDialog
{
    protected JPanel content;
    protected Module module;
    protected Diagram diagram;


    public static final String TEMPLATES_FOLDER_NAME = "templates";

    JComboBox<String> modulesComboBox = new JComboBox<>();
    JComboBox<String> diagramsComboBox = new JComboBox<>();
    JLabel diagramsLabal = new JLabel(BioUMLApplication.getMessageBundle().getResourceString( "DIAGRAM" ));

    protected JTextField outputFolderTextField = new JTextField( 20 );
    protected JTextField templateTextField = new JTextField( 20 );
    protected JComboBox<String> scopeComboBox = new JComboBox<>();

    protected final static Logger cat = Logger.getLogger( GenerateHTMLDialog.class.getName() );

    public GenerateHTMLDialog( JDialog dialog, String title, Module module, Diagram diagram )
    {
        super( dialog, title );
        init(module, diagram);
    }

    public GenerateHTMLDialog( JFrame frame, String title, Module module, Diagram diagram )
    {
        super( frame, title );
        init(module, diagram);
    }

    protected void updateOKButton()
    {
        okButton.setEnabled( outputFolderTextField.getText().length() > 0
                && templateTextField.getText().length() > 0 );
    }

    private void init(Module module, Diagram diagram)
    {
        this.module = module;
        this.diagram = diagram;


        JButton templateBrowseButton = new JButton( "..." );
        JButton outputFolderBrowseButton = new JButton( "..." );

        content = new JPanel( new GridBagLayout() );
        content.setBorder( new EmptyBorder( 10, 10, 10, 10 ) );

        outputFolderBrowseButton.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                updateOKButton();
            }
        });

        outputFolderBrowseButton.addActionListener( e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled( false );
            chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
            chooser.setDialogTitle( BioUMLApplication.getMessageBundle().getResourceString( "SELECT_OUTPUT_FOLDER" ) );
            int res = chooser.showOpenDialog( Application.getApplicationFrame() );
            if ( res == JFileChooser.APPROVE_OPTION )
            {
                File selectedFile = chooser.getSelectedFile();
                outputFolderTextField.setText( selectedFile.getPath() );
                updateOKButton();
            }
        } );

        templateBrowseButton.addActionListener( e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(((LocalRepository)CollectionFactoryUtils.getDatabases()).getRootDirectory(), TEMPLATES_FOLDER_NAME));
            chooser.setFileFilter( new XSLFileFilter() );
            chooser.setMultiSelectionEnabled( false );
            chooser.setAcceptAllFileFilterUsed( false );
            chooser.setDialogTitle( BioUMLApplication.getMessageBundle().getResourceString( "SELECT_TEMPLATE" ) );
            int res = chooser.showOpenDialog( Application.getApplicationFrame() );
            if ( res == JFileChooser.APPROVE_OPTION )
            {
                File selectedFile = chooser.getSelectedFile();
                templateTextField.setText( selectedFile.getPath() );
                updateOKButton();
            }
        } );
        templateBrowseButton.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                updateOKButton();
            }
        });

        for(DataElement de: CollectionFactoryUtils.getDatabases())
        {
            if(de instanceof Module)
            {
                modulesComboBox.addItem(de.getName());
            }
        }

        scopeComboBox = new JComboBox<>();
        scopeComboBox.addItemListener(e -> {
            String item = (String)e.getItem();
            setDiagramsVisible(item.equals(BioUMLApplication.getMessageBundle().getResourceString( "DIAGRAM" )));
        });
        scopeComboBox.addItem( BioUMLApplication.getMessageBundle().getResourceString( "DATABASE" ) );
        scopeComboBox.addItem( BioUMLApplication.getMessageBundle().getResourceString( "DIAGRAM" ) );

        content.add( new JLabel( BioUMLApplication.getMessageBundle().getResourceString( "SCOPE" ) ),
                new GridBagConstraints( 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL,
                        new Insets( 0, 0, 0, 0 ), 0, 0 ) );
        content.add( scopeComboBox,
                new GridBagConstraints( 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.BOTH,
                        new Insets( 0, 10, 0, 0 ), 0, 0 ) );

        content.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString( "DATABASE" )),
                new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                       GridBagConstraints.HORIZONTAL,
                                       new Insets(10, 0, 0, 0), 0, 0));
        content.add(modulesComboBox,
                new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                       GridBagConstraints.BOTH,
                                       new Insets(10, 10, 0, 0), 0, 0));

        content.add(diagramsLabal,
                new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                       GridBagConstraints.HORIZONTAL,
                                       new Insets(10, 0, 0, 0), 0, 0));
        content.add(diagramsComboBox,
                new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                       GridBagConstraints.BOTH,
                                       new Insets(10, 10, 0, 0), 0, 0));

        content.add( new JLabel( BioUMLApplication.getMessageBundle().getResourceString( "OUTPUT_FOLDER" ) ),
                new GridBagConstraints( 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL,
                        new Insets( 10, 0, 0, 0 ), 0, 0 ) );
        content.add( outputFolderTextField,
                new GridBagConstraints( 1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.BOTH,
                        new Insets( 10, 10, 0, 0 ), 0, 0 ) );
        content.add( outputFolderBrowseButton,
                new GridBagConstraints( 2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL,
                        new Insets( 10, 10, 0, 0 ), 0, 0 ) );

        content.add( new JLabel( BioUMLApplication.getMessageBundle().getResourceString( "TEMPLATE" ) ),
                new GridBagConstraints( 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL,
                        new Insets( 10, 0, 0, 0 ), 0, 0 ) );
        content.add( templateTextField,
                new GridBagConstraints( 1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.BOTH,
                        new Insets( 10, 10, 0, 0 ), 0, 0 ) );
        content.add( templateBrowseButton,
                new GridBagConstraints( 2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL,
                        new Insets( 10, 10, 0, 0 ), 0, 0 ) );

        setContent( content );



        diagramsComboBox.addItemListener(e -> {
            if (GenerateHTMLDialog.this.module != null)
            {
                String diagramName = (String)e.getItem();
                try
                {
                    GenerateHTMLDialog.this.diagram = GenerateHTMLDialog.this.module.getDiagram(diagramName);
                    System.out.println( "levo: GenerateHTMLDialog.itemStateChanged: GenerateHTMLDialog.this.diagram = " + GenerateHTMLDialog.this.diagram );
                }
                catch ( Exception exc )
                {
                    cat.log(Level.SEVERE, "Error at getting "+diagramName+" diagram", exc);
                }
            }
        });

        modulesComboBox.addItemListener(e -> setModule((String)e.getItem()));
        System.out.println( "levo: GenerateHTMLDialog.init: module = " + module );
        System.out.println( "levo: GenerateHTMLDialog.init: diagram = " + diagram );

        if( module == null && diagram == null ) // when the action invoked via main menu
        {
            Document activeDocument = Document.getActiveDocument();
            if( activeDocument instanceof DiagramDocument )
            {
                this.diagram = ( (DiagramDocument)activeDocument ).getDiagram();
                this.module = Module.optModule(this.diagram);
            }
            else if( modulesComboBox.getItemCount() > 0 )
            {
                setModule(modulesComboBox.getItemAt(0));
            }
        }
        else
        {
            if( module != null )
            {
                modulesComboBox.setSelectedItem(module.getName());
                if( diagram != null )
                {
                    diagramsComboBox.setSelectedItem(diagram.getName());
                    scopeComboBox.setSelectedItem(BioUMLApplication.getMessageBundle().getResourceString("DIAGRAM"));
                }
            }
        }
    }

    private void setModule(String moduleName)
    {
        module = DataElementPath.create("databases").getChildPath(moduleName).getDataElement(Module.class);
        updateDiagrams( moduleName );
    }

    private void updateDiagrams( String moduleName )
    {
        System.out.println( "levo: GenerateHTMLDialog.updateDiagrams: moduleName = " + moduleName );
        try
        {
            DataCollection<Diagram> diagrams = this.module.getDiagrams();
            diagramsComboBox.removeAllItems();
            for(Diagram diagram: diagrams)
            {
                diagramsComboBox.addItem(diagram.getName());
            }
        }
        catch ( Exception exc )
        {
            cat.log(Level.SEVERE, "Error at getting module "+moduleName+" diagrams", exc);
        }
    }

    private void setDiagramsVisible(boolean diagramsVisible)
    {
        diagramsLabal.setVisible(diagramsVisible);
        diagramsComboBox.setVisible(diagramsVisible);
    }


    @Override
    protected void okPressed()
    {
        super.okPressed();

        final FunctionJobControl jobControl = new FunctionJobControl(null);
        final Logger cat = Logger.getLogger(GenerateHTMLDialog.class.getName());
        final String infoStr = BioUMLApplication.getMessageBundle().getResourceString("GENERATING_HTML");
        final StatusInfoDialog infoDialog = new StatusInfoDialog(Application.getApplicationFrame(), infoStr, cat, jobControl);

        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    infoDialog.setInfo(infoStr + "...");
                    genHTML( cat, jobControl, module, diagram);
                }
                catch(Throwable t)
                {
                    cat.log(Level.SEVERE, t.getMessage());
                    infoDialog.fails();
                    return;
                }
                infoDialog.success();
            }
        };
        infoDialog.startProcess(thread);
    }

    private void genHTML( Logger cat, FunctionJobControl jobControl, Module module, Diagram diagram ) throws Exception
    {
        if(jobControl != null)
            jobControl.functionStarted();
        try
        {
            String outputFolderPath = outputFolderTextField.getText();
            File outputFolder = new File( outputFolderPath );
            if( !outputFolder.exists() && !outputFolder.mkdir() )
            {
                throw new Exception("Failed to create output folder '" + outputFolderPath + "'.");
            }
            File templateFile= new File(templateTextField.getText());

            if (scopeComboBox.getSelectedItem().equals(BioUMLApplication.getMessageBundle().getResourceString("DIAGRAM")))
            {
                genHTML(diagram, outputFolder, templateFile, cat);
            }
            else if (scopeComboBox.getSelectedItem().equals(BioUMLApplication.getMessageBundle().getResourceString("DATABASE")))
            {
                String generatingHTMLForModuleMessage = MessageFormat.format(BioUMLApplication.getMessageBundle().getResourceString("GENERATING_HTML_FOR_DATABASE"),
                        new Object[]{module.getName()});
                cat.info(generatingHTMLForModuleMessage);

                int count = 0;
                DataCollection<Diagram> diagrams = module.getDiagrams();
                for(Diagram d: diagrams)
                {
                    genHTML(d, outputFolder, templateFile, cat);
                    count++;
                    if(jobControl != null)
                        jobControl.setPreparedness(100*count/diagrams.getSize());
                }
            }

            String message = BioUMLApplication.getMessageBundle().getResourceString("HTML_GENERATED_SUCCESSFULLY");
            cat.info(message);
            if(jobControl != null)
                jobControl.functionFinished(message);
        }
        catch ( Exception e )
        {
            if(jobControl != null)
               jobControl.functionTerminatedByError(e);
            throw e;
        }

    }


    public static void genHTML( Diagram diagram, File outputFolder, File templateFile, Logger cat ) throws Exception
    {
        String digramName = diagram.getName();
        File outputDiagramFile = new File( outputFolder, digramName + ".html" );
        File outputDiagramImageFile = new File( outputFolder, digramName + ".png" );

        String message = MessageFormat.format(BioUMLApplication.getMessageBundle().getResourceString("GENERATING_HTML_FOR_DIAGRAM"),
                new Object[]{digramName});
        cat.info(message);

        BufferedImage image = ImageGenerator.generateDiagramImage(diagram);
        try (FileOutputStream out = new FileOutputStream( outputDiagramImageFile ))
        {
            ImageGenerator.encodeImage( image, "PNG", out );
        }

        message = MessageFormat.format(BioUMLApplication.getMessageBundle().getResourceString("CREATING_IMAGE_SUCCESS"),
                new Object[]{outputDiagramImageFile.getName()});
        cat.info(message);

        Source streamSource = new StreamSource( templateFile );
        try (FileOutputStream fos = new FileOutputStream( outputDiagramFile ))
        {
            DiagramXmlWriter xmlWriter = new DiagramXmlWriter( fos );
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer(streamSource);
    
            Rectangle rect = diagram.getView().getBounds();
            transformer.setParameter("x_offset", Integer.toString((int)rect.getX()));
            transformer.setParameter("y_offset", Integer.toString((int)rect.getY()));
    
            //String outputPath = outputFolder.getAbsolutePath();
            String outputFolderPath = outputFolder.getCanonicalPath();
            if (!outputFolderPath.endsWith(File.separator))
            {
                outputFolderPath += File.separator;
            }
            transformer.setParameter("path", outputFolderPath);
    
            xmlWriter.write(diagram, transformer);
        }
        message = MessageFormat.format(BioUMLApplication.getMessageBundle().getResourceString("APPLAYING_TEMPLATE"),
                new Object[]{outputDiagramImageFile.getName()});
        cat.info(message);
    }

    /**
     * Displays modal dialog on screen.
     *
     * @return true if "Ok" button is pressed
     */
    @Override
    public boolean doModal()
    {
        setDiagramsVisible(true);
        pack();
        ApplicationUtils.moveToCenter ( this );
        setDiagramsVisible(scopeComboBox.getSelectedItem().equals(BioUMLApplication.getMessageBundle().getResourceString( "DIAGRAM" )));
        show();
        dispose();
        return result;
    }
}
