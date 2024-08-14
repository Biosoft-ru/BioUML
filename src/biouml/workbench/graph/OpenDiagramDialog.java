package biouml.workbench.graph;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.support.NameListToComboBoxModelAdapter;
import ru.biosoft.gui.ExplorerPane;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.standard.type.Base;
import biouml.workbench.BioUMLApplication;
import biouml.workbench.diagram.NewDiagramDialog;

import com.developmentontheedge.application.dialog.OkCancelDialog;

@SuppressWarnings ( "serial" )
public class OpenDiagramDialog extends OkCancelDialog
{
    protected static final Logger log = Logger.getLogger( OpenDiagramDialog.class.getName() );

    protected NameListToComboBoxModelAdapter diagrams = null;

    protected ExplorerPane explorerPane = null;

    protected Module module;

    protected Diagram diagram;
    protected String diagramName;

    protected JComboBox<String> diagramsBox;


    ///////////////////////////////////////////////////////////////////
    // Constructor
    //

    public OpenDiagramDialog ( JFrame frame, String title, Module module ) throws Exception
    {
        super ( frame, title );

        this.module = module;

        JPanel content = new JPanel ( new BorderLayout ( 5, 5 ) );
        content.setBorder ( new EmptyBorder ( 10, 10, 10, 10 ) );

        explorerPane = new ExplorerPane ( );
        explorerPane.setPreferredSize ( new Dimension ( 400, 300 ) );
        content.add ( explorerPane, BorderLayout.CENTER );

        JPanel gridPanel = new JPanel ( new GridBagLayout ( ) );
        content.add ( gridPanel, BorderLayout.NORTH );

        diagrams = new NameListToComboBoxModelAdapter ( module.getDiagrams ( ) );

        // --- diagrams issues ---
        gridPanel.add ( new JLabel ( BioUMLApplication.getMessageBundle ( )
                .getResourceString ( "DIAGRAM" ) ),
                new GridBagConstraints ( 0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets ( 0, 0, 0, 0 ), 0, 0 ) );
        diagramsBox = new JComboBox<> ( diagrams );
        diagramsBox.addActionListener ( new ActionListener ( )
            {
                @Override
                public void actionPerformed ( ActionEvent e )
                {
                    if ( diagramsBox.getModel ( ) == null )
                        return;

                    diagramName = ( String ) diagramsBox.getSelectedItem ( );

                    Base selectedKernel = null;
                    try
                    {
                        diagram = OpenDiagramDialog.this.module.getDiagram ( diagramName );
                        if ( diagram == null )
                            return;
                        selectedKernel = diagram.getKernel ( );
                    }
                    catch ( Exception e1 )
                    {
                    log.log( Level.SEVERE, "Cannot open diagram " + diagramName, e1 );
                    }
                    if ( selectedKernel == null )
                        return;

                    explorerPane.explore ( selectedKernel, null );
                }
            } );

        gridPanel.add ( diagramsBox, new GridBagConstraints ( 1, 1, 1, 1, 0.0,
                0.0, GridBagConstraints.EAST, GridBagConstraints.REMAINDER,
                new Insets ( 5, 5, 0, 0 ), 0, 0 ) );

        diagramsBox.setSelectedIndex ( 0 );

        JButton newButton = new JButton ( BioUMLApplication.getMessageBundle ( ).getResourceString ( "BUTTON_NEW" ) );
        newButton.addActionListener ( new ActionListener ( )
            {
                @Override
                public void actionPerformed ( ActionEvent e )
                {
                    String dialogTilte = BioUMLApplication.getMessageBundle ( ).getResourceString ( "NEW_DIAGRAM_DIALOG_TITLE" );
                    NewDiagramDialog dialog = new NewDiagramDialog ( OpenDiagramDialog.this, dialogTilte, NewDiagramDialog.getAvailableDatabases(), OpenDiagramDialog.this.module );
                    if ( dialog.doModal ( ) )
                    {
                        try
                        {
                            OpenDiagramDialog.this.module.putDiagram ( dialog.getDiagram ( ) );
                        }
                        catch ( Exception e1 )
                        {
                        log.log( Level.SEVERE, "Cannot create new diagram", e1 );
                        }
                    }
                    OpenDiagramDialog.this.repaint ( );
                }
            } );
        buttonPanel.add ( newButton );

        setContent ( content );

        pack ( );
        setResizable ( false );
    }

    protected Diagram getDiagram ( )
    {
        return diagram;
    }

    @Override
    protected void cancelPressed ( )
    {
        diagrams.close ( );
        diagram = null;
        super.cancelPressed ( );
    }

    @Override
    protected void okPressed ( )
    {
        diagrams.close ( );
        super.okPressed ( );
    }

}
