package biouml.workbench.module;

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
import java.util.jar.Attributes;
import java.util.jar.Manifest;
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

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

import biouml.model.Module;
import biouml.model.util.ModulePackager;
import biouml.workbench.BioUMLApplication;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class ModuleExportDialog extends OkCancelDialog
{
    protected JPanel content;
    protected File selectedFile;
    protected Manifest manifest;
    protected Module module;
    protected JTextField versionTextField = new JTextField();
    protected JTextField descTextField = new JTextField();
    protected JTextField pathTextField = new JTextField( 20 );
    protected boolean isProject = false;

    public ModuleExportDialog(JDialog dialog, String title, Module module)
    {
        super( dialog, title );
        init( module );
    }

    public ModuleExportDialog(JFrame frame, String title, Module module, boolean isProject)
    {
        super( frame, title );
        this.isProject = isProject;
        init( module );
    }

    public ModuleExportDialog(JFrame frame, String title, Module module)
    {
        super( frame, title );
        init( module );
    }

    private void updateModuleInfo()
    {
        if( module != null )
        {
            manifest = ModulePackager.getModuleManifest( module.getCompletePath() );
            String moduleVersion = "";
            String moduleDesc = "";
            if( manifest != null )
            {
                Attributes attributes = manifest.getMainAttributes();
                moduleVersion = attributes.getValue( ModulePackager.MF_DATABASE_VERSION );
                moduleDesc = attributes.getValue( ModulePackager.MF_DATABASE_DESCRIPTION );
            }
            versionTextField.setText( moduleVersion );
            descTextField.setText( moduleDesc );

        }
    }

    private void init(Module inModule)
    {
        this.module = inModule;

        content = new JPanel( new GridBagLayout() );
        content.setBorder( new EmptyBorder( 10, 10, 10, 10 ) );

        JComboBox<String> modulesComboBox = new JComboBox<>();
        if( inModule == null )
        {
            modulesComboBox.addItemListener( new ItemListener()
            {
                @Override
                public void itemStateChanged(ItemEvent e)
                {
                    try
                    {
                        String moduleName = (String)e.getItem();
                        DataCollection dc = CollectionFactoryUtils.getDatabases().get( moduleName );
                        module = (Module)dc;
                        updateModuleInfo();
                    }
                    catch( Exception e1 )
                    {
                        throw ExceptionRegistry.translateException( e1 );
                    }
                }
            } );
            for( DataCollection module : CollectionFactoryUtils.getDatabases() )
            {
                modulesComboBox.addItem( module.getName() );
            }
        }
        if( inModule != null )
        {
            modulesComboBox.addItem( inModule.getName() );
            modulesComboBox.setSelectedItem( inModule.getName() );
        }
        JButton browseButton = new JButton( "..." );

        pathTextField.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                updateOKButton();
            }
        } );

        browseButton.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser chooser = new JFileChooser();
                chooser.setAcceptAllFileFilterUsed( false );
                chooser.setMultiSelectionEnabled( false );
                chooser.setFileFilter( new BMDFileFilter() );
                int res = chooser.showSaveDialog( Application.getApplicationFrame() );
                if( res == JFileChooser.APPROVE_OPTION )
                {
                    selectedFile = chooser.getSelectedFile();
                    if( !selectedFile.getName().toLowerCase().endsWith( BMDFileFilter.BMD_EXTENTION ) )
                    {
                        selectedFile = new File( selectedFile.getPath() + BMDFileFilter.BMD_EXTENTION );
                    }
                    pathTextField.setText( selectedFile.getPath() );
                    updateOKButton();
                }
            }
        } );

        updateOKButton();

        content.add( new JLabel( BioUMLApplication.getMessageBundle().getResourceString( "SELECT_DATABASE" ) ), new GridBagConstraints( 0,
                0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
        content.add( modulesComboBox, new GridBagConstraints( 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets( 0, 10, 0, 0 ), 0, 0 ) );
        content.add( new JLabel( BioUMLApplication.getMessageBundle().getResourceString( "VERSION" ) ), new GridBagConstraints( 0, 2, 1, 1,
                0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 10, 0, 0, 0 ), 0, 0 ) );
        content.add( versionTextField, new GridBagConstraints( 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets( 10, 10, 0, 0 ), 0, 0 ) );
        content.add( new JLabel( BioUMLApplication.getMessageBundle().getResourceString( "DESCRIPTION" ) ), new GridBagConstraints( 0, 3, 1,
                1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 10, 0, 0, 0 ), 0, 0 ) );
        content.add( descTextField, new GridBagConstraints( 1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets( 10, 10, 0, 0 ), 0, 0 ) );
        content.add( new JLabel( BioUMLApplication.getMessageBundle().getResourceString( "FILE_LOCATION" ) ), new GridBagConstraints( 0, 4,
                1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 10, 0, 0, 0 ), 0, 0 ) );
        content.add( pathTextField, new GridBagConstraints( 1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets( 10, 10, 0, 0 ), 0, 0 ) );
        content.add( browseButton, new GridBagConstraints( 2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets( 10, 10, 0, 0 ), 0, 0 ) );
        setContent( content );
    }

    private void updateOKButton()
    {
        String filename = pathTextField.getText();
        boolean correctFileName = ( filename != null && filename.length() > 0 );
        okButton.setEnabled( correctFileName );
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();

        final FunctionJobControl jobControl = new FunctionJobControl( null );
        final Logger cat = Logger.getLogger( ModulePackager.class.getName() );

        final String exportingStr = BioUMLApplication.getMessageBundle().getResourceString( "EXPORTING" );
        final StatusInfoDialog infoDialog = new StatusInfoDialog( Application.getApplicationFrame(), exportingStr, cat, jobControl );

        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    infoDialog.setInfo( exportingStr + "..." );
                    ModulePackager.exportModule( module, module.getName(), versionTextField.getText(), descTextField.getText(),
                            pathTextField.getText(), jobControl, null );
                }
                catch( Throwable t )
                {
                    cat.log( Level.SEVERE, t.getMessage() );
                    infoDialog.fails();
                    return;
                }
                infoDialog.success();
            }
        };
        infoDialog.startProcess( thread );
    }
}