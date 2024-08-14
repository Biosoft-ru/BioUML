package biouml.standard.editors;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.Node;
import biouml.standard.type.ImageDescriptor;
import biouml.standard.type.Substance;
import biouml.workbench.BioUMLApplication;
import biouml.workbench.graph.DiagramToGraphTransformer;
import biouml.workbench.resources.MessageBundle;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.util.OkCancelDialog;
import ru.biosoft.util.TextUtil;

@SuppressWarnings ( "serial" )
public class NodeImageChoiceDialog extends OkCancelDialog
{
    protected JPanel content;
    protected JComboBox<String> fileName = new JComboBox<>();
    protected JComboBox<String> structure = new JComboBox<>();
    protected JTextField location = new JTextField();
    protected JButton fileImportButton;
    protected JTextField titleField = new JTextField( 20 );
    protected JTextField shiftOffsetField = new JTextField( 20 );
    protected JLabel image = new JLabel();
    protected ViewPane viewPane;

    protected String value;
    protected ImageDescriptor imageDescriptor;
    protected Node nodeClone;
    protected Diagram diagram;

    protected final static Component component = new Component()
    {
    };

    protected final static MediaTracker tracker = new MediaTracker( component );

    public static final String DEFAULT = "<default>";

    private static final MessageBundle mb = BioUMLApplication.getMessageBundle();

    public NodeImageChoiceDialog(Component parent, String title, ImageDescriptor imageDescriptor)
    {
        super( parent, title );

        this.imageDescriptor = imageDescriptor;
        this.value = imageDescriptor.getSource();
        Node tmpNode = (Node)imageDescriptor.getParent();
        diagram = StreamEx.<Option> iterate( tmpNode, Option::getParent ).takeWhile( Objects::nonNull ).select( Diagram.class ).findFirst()
                .get();

        this.nodeClone = new Node( diagram, tmpNode.getKernel() );
        nodeClone.setTitleAngle( tmpNode.getTitleAngle() );
        nodeClone.setTitleOffset( tmpNode.getTitleOffset() );

        content = new JPanel( new GridBagLayout() );
        content.setBorder( new EmptyBorder( 10, 10, 10, 10 ) );

        fileName.addItem( DEFAULT );
        StreamEx.of( imageDescriptor.getImages() ).forEach( fileName::addItem );

        fileName.addItemListener( e -> {
            value = (String)fileName.getSelectedItem();
            if( value.equals( DEFAULT ) )
                value = null;
            nodeClone.getImage().setSource( value );
            refreshImage( nodeClone );
        } );

        fileImportButton = new JButton( "Import" );
        fileImportButton.addActionListener( event -> addImageAction() );

        String[] structures = imageDescriptor.getStructures( Module.optModule( ( (Node)imageDescriptor.getParent() ).getKernel() ) );
        if( structures != null )
        {
            StreamEx.of( structures ).forEach( structure::addItem );
        }

        structure.addItemListener( e -> {
            value = (String)structure.getSelectedItem();
            nodeClone.getImage().setSource( value );
            refreshImage( nodeClone );
        } );

        titleField.addActionListener( e -> {
            nodeClone.setTitle( titleField.getText() );
            nodeClone.getImage().setSource( value );
            refreshImage( nodeClone );
        } );

        location.setText( String.valueOf( nodeClone.getTitleAngle() ) );
        location.addActionListener( event -> {
            nodeClone.setTitleAngle( Double.parseDouble( location.getText() ) );
            nodeClone.setTitleOffset( getInt( shiftOffsetField.getText() ) );
            nodeClone.getImage().setSource( value );
            refreshImage( nodeClone );
        } );

        shiftOffsetField.setText( String.valueOf( nodeClone.getTitleOffset() ) );
        shiftOffsetField.addActionListener( e -> {
            nodeClone.setTitleAngle( Double.parseDouble( location.getText() ) );
            int intValue = getInt( shiftOffsetField.getText() );
            nodeClone.setTitleOffset( getInt( shiftOffsetField.getText() ) );
            shiftOffsetField.setText( String.valueOf( intValue ) );
            nodeClone.getImage().setSource( value );
            refreshImage( nodeClone );
        } );

        viewPane = new ViewPane();

        if( value == null )
        {
            fileName.setSelectedItem( DEFAULT );
        }
        else if( value.startsWith( "Data/structure/" ) )
        {
            structure.setSelectedItem( value );
        }
        else
        {
            fileName.setSelectedItem( value );
        }

        if( imageDescriptor.getParent() instanceof Node )
        {
            Node node = (Node)imageDescriptor.getParent();
            titleField.setText( node.getTitle() );
        }

        updateOKButton();

        boolean isMolecule = false;
        if( imageDescriptor.getParent() instanceof Node )
        {
            if( ( (Node)imageDescriptor.getParent() ).getKernel() instanceof Substance )
                isMolecule = true;
        }

        content.add( new JLabel( "Image:" ), new GridBagConstraints( 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets( 10, 0, 0, 0 ), 0, 0 ) );
        content.add( fileName, new GridBagConstraints( 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets( 10, 10, 0, 0 ), 0, 0 ) );
        content.add( fileImportButton, new GridBagConstraints( 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets( 10, 10, 0, 0 ), 0, 0 ) );

        content.add( viewPane, new GridBagConstraints( 3, 0, 1, 10, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                new Insets( 10, 10, 0, 0 ), 0, 0 ) );

        if( isMolecule )
        {
            content.add( new JLabel( "Chemical structure:" ), new GridBagConstraints( 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, new Insets( 10, 0, 0, 0 ), 0, 0 ) );
            content.add( structure, new GridBagConstraints( 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets( 10, 10, 0, 0 ), 0, 0 ) );
        }
        content.add( new JLabel( "Title:" ), new GridBagConstraints( 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets( 10, 0, 0, 0 ), 0, 0 ) );
        content.add( titleField, new GridBagConstraints( 1, 2, 1, 2, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets( 10, 10, 0, 0 ), 0, 0 ) );

        content.add( new JLabel( "Location:" ), new GridBagConstraints( 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets( 10, 0, 0, 0 ), 0, 0 ) );
        content.add( location, new GridBagConstraints( 1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets( 10, 10, 0, 0 ), 0, 0 ) );

        content.add( new JLabel( "Shift offset:" ), new GridBagConstraints( 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets( 10, 0, 0, 0 ), 0, 0 ) );
        content.add( shiftOffsetField, new GridBagConstraints( 1, 5, 1, 2, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets( 10, 10, 0, 0 ), 0, 0 ) );

        refreshImage( (Node)imageDescriptor.getParent() );

        addComponentListener( new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent event)
            {
                refreshImage( nodeClone );
            }
        } );

        setContent( content );
    }
    private void updateOKButton()
    {
        boolean correctFileName = ( ( (String)fileName.getSelectedItem() != null && ( (String)fileName.getSelectedItem() ).length() > 0 ) || ( (String)structure
                .getSelectedItem() != null && ( (String)structure.getSelectedItem() ).length() > 0 ) );
        okButton.setEnabled( correctFileName );
    }

    @Override
    protected void okPressed()
    {
        if( imageDescriptor.getParent() instanceof Node )
        {
            Node node = (Node)imageDescriptor.getParent();
            node.setTitle( titleField.getText() );
            node.setTitleOffset( getInt( shiftOffsetField.getText() ) );
            node.setTitleAngle( Double.parseDouble( location.getText() ) );
            DiagramToGraphTransformer.layoutSingleNodeEdges( node, diagram.getPathLayouter(), null );
        }
        super.okPressed();
    }

    protected void refreshImage(Node node)
    {
        if( diagram != null && node != null )
        {
            CompositeView view = diagram.getType().getDiagramViewBuilder()
                    .createNodeView( node, diagram.getViewOptions(), this.getParent().getGraphics() );
            Dimension paneSize = viewPane.getSize();
            Dimension viewSize = view.getBounds().getSize();
            viewPane.setView( view, new Point( ( paneSize.width - viewSize.width ) / 2, ( paneSize.height - viewSize.height ) / 2 ) );
        }
    }

    public String getImageSource()
    {
        return value;
    }

    private int getInt(String string)
    {
        try
        {
            return Integer.parseInt( string );
        }
        catch( Exception e )
        {
        }
        return 0;
    }

    protected void addImageAction()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter( new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                if( file.isDirectory() )
                    return true;
                if( file.isFile() && null != file.getName() )
                {
                    String fileName = file.getName().toLowerCase();
                    String extensions[] = TextUtil.split( mb.getResourceString( "PIC_EXTENSIONS" ), ' ' );
                    return StreamEx.of( extensions ).anyMatch( fileName::endsWith );
                }
                return false;
            }

            @Override
            public String getDescription()
            {
                return mb.getResourceString( "PIC_EXTENSIONS_TITLE" );
            }
        } );
        chooser.setMultiSelectionEnabled( true );

        int res = chooser.showOpenDialog( Application.getApplicationFrame() );
        if( res == JFileChooser.APPROVE_OPTION )
        {
            File files[] = chooser.getSelectedFiles();
            for( File file : files )
            {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                try
                {
                    BufferedImage image = getBufferedImageFromImage( toolkit.getImage( file.getPath() ) );
                    imageDescriptor.addImage( image, file.getName() );
                    fileName.addItem( file.getName() );
                }
                catch( Exception e )
                {
                    ApplicationUtils.errorBox( ExceptionRegistry.log( e ) );
                    continue;
                }
            }
        }
    }

    private BufferedImage getBufferedImageFromImage(Image image) throws Exception
    {
        synchronized( tracker )
        {
            tracker.addImage( image, 0 );
            try
            {
                tracker.waitForID( 0, 0 );
            }
            catch( InterruptedException e )
            {
                System.out.println( "INTERRUPTED while loading Image" );
            }
            boolean error = tracker.isErrorID( 0 );
            Object[] errors = tracker.getErrorsID( 0 );
            tracker.removeImage( image, 0 );
            if( error )
            {
                throw new Exception( "Error during get buffered image occurred: " + StreamEx.of( errors ).joining( "\n" ) );
            }
        }
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try
        {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage( image.getWidth( null ), image.getHeight( null ), transparency );
        }
        catch( HeadlessException e )
        {
            // The system does not have a screen
        }

        if( bimage == null )
        {
            int type = BufferedImage.TYPE_INT_RGB;
            bimage = new BufferedImage( image.getWidth( null ), image.getHeight( null ), type );
        }

        Graphics g = bimage.createGraphics();
        g.drawImage( image, 0, 0, null );
        g.dispose();
        return bimage;
    }
}
