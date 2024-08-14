package biouml.workbench.diagram;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.workbench.BioUMLApplication;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

@SuppressWarnings ( "serial" )
public class NewDiagramDialog extends OkCancelDialog
{
    protected static final Logger log = Logger.getLogger(NewDiagramDialog.class.getName());

    protected JComboBox<ru.biosoft.access.core.DataElementPath> moduleType = new JComboBox<>();

    protected DataCollection<?> parent = null;

    protected JTextField diagramName = new JTextField();

    protected JComboBox<DiagramType> diagramType = new JComboBox<>();

    protected JEditorPane diagramTypeDescription = new JEditorPane();

    protected Diagram diagram = null;

    ///////////////////////////////////////////////////////////////////
    // Constructor
    //

    public NewDiagramDialog(JDialog dialog, String title, List<ru.biosoft.access.core.DataElementPath> dbNames, DataCollection<?> parent)
    {
        super(dialog, title);
        init(dbNames, parent);
    }

    public NewDiagramDialog(JFrame frame, String title, List<ru.biosoft.access.core.DataElementPath> dbNames, DataCollection<?> parent)
    {
        super(frame, title);
        init(dbNames, parent);
    }

    public static List<ru.biosoft.access.core.DataElementPath> getAvailableDatabases()
    {
        return StreamEx.of( CollectionFactoryUtils.getDatabases().stream() ).select( Module.class )
                .filter( module -> module.contains( Module.DIAGRAM ) )
                .filter( module -> module.getDiagrams().isMutable() )
                .append( CollectionFactoryUtils.getUserProjectsPath().getDataCollection().stream( Module.class ) )
                .map( Module::getCompletePath )
                .toList();
    }

    protected void init(List<ru.biosoft.access.core.DataElementPath> dbNames, DataCollection<?> parent)
    {
        this.parent = parent;

        JPanel content = new JPanel(new BorderLayout(5, 5));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel gridPanel = new JPanel(new GridBagLayout());
        content.add(gridPanel, BorderLayout.NORTH);

        JLabel moduleLabel = new JLabel(BioUMLApplication.getMessageBundle().getResourceString("DATABASE"));
        gridPanel.add(moduleLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        gridPanel.add(moduleType, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));

        //--- module issues ---
        for( DataElementPath db : dbNames )
        {
            moduleType.addItem(db);
        }

        DataElementPath modulePath = Module.optModulePath(parent);
        if( modulePath != null )
        {
            moduleType.setSelectedItem(modulePath);

            setTitle(getTitle() + " (module " + modulePath + ")");

            // hide module
            moduleLabel.setVisible(false);
            moduleType.setVisible(false);
        }
        else
        {
            if( moduleType.getItemCount() > 0 )
            {
                moduleType.setSelectedIndex(0);
            }
        }

        moduleType.addActionListener(e -> {
            int idx = moduleType.getSelectedIndex();
            if( idx >= 0 )
                setDiagramTypes(moduleType.getItemAt(idx));
            else
                setDiagramTypes(null);
            checkIfOkButtonShouldBeEnabled();
        });

        //--- diagram name issues ---
        gridPanel.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString("DIAGRAM_NAME")), new GridBagConstraints(0, 1, 1,
                1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        gridPanel.add(diagramName, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));

        diagramName.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                checkIfOkButtonShouldBeEnabled();
            }
        });

        //--- diagram type issues ---

        gridPanel.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString("DIAGRAM_TYPE")), new GridBagConstraints(0, 2, 1,
                1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 10, 0), 0, 0));

        //--- module diagram type issues ---
        gridPanel.add(diagramType, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));

        diagramType.addActionListener(evt -> updateDescription());

        int idx = moduleType.getSelectedIndex();
        if( idx >= 0 )
            setDiagramTypes(moduleType.getItemAt(idx));
        else
            setDiagramTypes(null);

        //--- diagram type description issues ---
        diagramTypeDescription.setEditable(false);
        diagramTypeDescription.setPreferredSize(new java.awt.Dimension(250, 150));

        JPanel descr = new JPanel(new BorderLayout(0, 5));

        descr.add(new JLabel(BioUMLApplication.getMessageBundle().getResourceString("NEW_DIAGRAM_TYPE_DESCRIPTION")), BorderLayout.NORTH);
        descr.add(new JScrollPane(diagramTypeDescription), BorderLayout.CENTER);

        content.add(descr, BorderLayout.CENTER);

        setContent(content);
        okButton.setEnabled(false);
    }

    protected void checkIfOkButtonShouldBeEnabled()
    {
        okButton.setEnabled(false);
        try
        {
            if( getModuleType().optDataElement() == null )
                okButton.setEnabled(false);
        }
        catch( Exception e )
        {
            okButton.setEnabled(false);
        }
        String name = diagramName.getText();
        okButton.setEnabled(name != null
                && name.length() > 0
                && 0 <= moduleType.getSelectedIndex()
                && 0 <= diagramType.getSelectedIndex());
    }

    ///////////////////////////////////////////////////////////////////
    // Properties
    //

    protected DataElementPath diagramCompletePath = null;
    public DataElementPath getDiagramCompletePath()
    {
        return diagramCompletePath;
    }

    protected String getDiagramName()
    {
        return diagramName.getText();
    }

    protected DataElementPath getModuleType()
    {
        return moduleType.getItemAt(moduleType.getSelectedIndex());
    }

    ///////////////////////////////////////////////////////////////////
    //
    //

    protected void setDiagramTypes(DataElementPath modulePath)
    {
        diagramType.removeAllItems();
        if(modulePath == null)
            return;
        try
        {
            modulePath.getDataElement( Module.class ).getType().getDiagramTypeObjects().forEach( diagramType::addItem );
            pack();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Setting diagram type error", e);
        }
    }

    @Override
    protected void okPressed()
    {
        // create new diagram here
        try
        {
            DataCollection<?> origin = parent;
            if( origin == null )
            {
                origin = ( (Module)getModuleType().getDataElement() ).getDiagrams();
            }

            int selectedIndex = diagramType.getSelectedIndex();
            if(selectedIndex == -1)
            {
                log.log(Level.SEVERE,  "Diagram type is not selected" );
                return;
            }
            DiagramType type = diagramType.getItemAt( selectedIndex );
            String diagramName = getDiagramName();


            if( checkElementNotExist( origin, diagramName ) )
                diagram = type.createDiagram( origin, diagramName, null );
            else
                return;
            diagramCompletePath = diagram.getCompletePath();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Creating new diagram ", e);
        }
        super.okPressed();
    }
    public Diagram getDiagram()
    {
        return diagram;
    }

    protected void updateDescription()
    {
        int selectedIndex = diagramType.getSelectedIndex();
        if(selectedIndex == -1)
            diagramTypeDescription.setText( "No diagram type is selected" );
        else
            diagramTypeDescription.setText(diagramType.getItemAt( selectedIndex ).getDescription());
    }

    private boolean checkElementNotExist(DataCollection origin, String name)
    {
        if( origin.contains( name ) )
        {
            String message = MessageFormat.format( BioUMLApplication.getMessageBundle().getResourceString( "NEW_DIAGRAM_ALREADY_EXIST" ),
                    new Object[] {name} );
            int status = JOptionPane.showConfirmDialog( Application.getApplicationFrame(), message, "", JOptionPane.YES_NO_OPTION );
            return ( status == JOptionPane.OK_OPTION );
        }
        return true;
    }
}
