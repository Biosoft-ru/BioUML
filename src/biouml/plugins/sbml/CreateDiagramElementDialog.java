package biouml.plugins.sbml;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Specie;
import com.developmentontheedge.application.dialog.OkCancelDialog;

/**
 * Creates SBML specie or compartment diagram element.
 *
 * @pending validate name to be valid SBML SName
 * @pending check whether the diagram already contains the element with the same name
 */
@SuppressWarnings ( "serial" )
public class CreateDiagramElementDialog extends OkCancelDialog
{
    protected static final Logger log = Logger.getLogger(CreateDiagramElementDialog.class.getName());

    protected Diagram diagram;
    protected Compartment parent;
    protected Class<?> type;
    protected MessageBundle resources;
    protected JTextField nameField;
    protected JComboBox<String> specieType;

    ////////////////////////////////////////////////////////////////////////////
    // Constructor and public properties
    //

    public CreateDiagramElementDialog(JFrame frame, Diagram diagram, Compartment parent, Class<?> type)
    {
        super(frame, "");

        this.diagram = diagram;
        this.parent = parent;
        this.type = type;

        resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());
        setTitle(resources.getResourceString("DIAGRAM_ELEMENT_TITLE"));

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContent(content);

        String nameLabel;
        if( isSpecie() )
        {
            nameLabel = resources.getResourceString("DIAGRAM_ELEMENT_SPECIE_NAME");
        }
        else
        {
            nameLabel = resources.getResourceString("DIAGRAM_ELEMENT_COMPARTMENT_NAME");

        }
        content.add(new JLabel(nameLabel), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        nameField = new JTextField(20);
        content.add(nameField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
                5, 0, 0), 0, 0));

        if( isSpecie() )
        {
            content.add(new JLabel(resources.getResourceString("DIAGRAM_ELEMENT_SPECIE_TYPE")), new GridBagConstraints(0, 1, 1, 1, 0.0,
                    0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            specieType = new JComboBox<>((String[])resources.getObject("SPECIE_TYPES"));
            content.add(specieType, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                    new Insets(5, 5, 0, 0), 0, 0));
        }

        okButton.setEnabled(false);
        nameField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                String name = nameField.getText();
                okButton.setEnabled(name != null && name.length() > 0);
            }
        });

    }

    protected Node node;
    public Node getNode()
    {
        return node;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    //

    protected boolean isSpecie()
    {
        return type.equals(Specie.class);
    }

    @Override
    protected void okPressed()
    {
        node = createDiagramElement();
        if( node != null )
        {
            super.okPressed();
        }
    }

    protected Node createDiagramElement()
    {
        Node node = null;
        String name = SbmlSemanticController.validateSName(nameField.getText());

        // check whethr the name is reserved SBML key word
        if( SbmlSemanticController.isReservedKeyWord(name) )
        {
            String title = resources.getString("DIAGRAM_ELEMENT_ERROR_TITLE");
            String message = resources.getString("DIAGRAM_ELEMENT_RESERVED");
            message = MessageFormat.format(message, new Object[] {name});
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            return null;
        }

        // check whether this unique name
        Object obj = SbmlSemanticController.resolveName(diagram, name);
        if( obj != null )
        {
            String title = resources.getString("DIAGRAM_ELEMENT_ERROR_TITLE");
            String message = resources.getString("DIAGRAM_ELEMENT_DUPLICATED");
            String type = obj.getClass().getName();
            type = type.substring(type.lastIndexOf(".") + 1);
            message = MessageFormat.format(message, new Object[] {name, type});
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            return null;
        }

        try
        {
            if( !isSpecie() )
            {
                node = new Compartment(parent, new biouml.standard.type.Compartment(null, name));
                VariableRole var = new VariableRole(node);
                node.setRole(var);
            }
            else
            {
                Specie specie = new Specie(null, name, (String)specieType.getSelectedItem());
                node = new Node(parent, specie);
                VariableRole var = new VariableRole(node, 0.0);
                node.setRole(var);
            }
        }
        catch( Throwable t )
        {
            String title = resources.getString("DIAGRAM_ELEMENT_ERROR_TITLE");
            String message = resources.getString("DIAGRAM_ELEMENT_ERROR");
            message = MessageFormat.format(message, new Object[] {name, t.getMessage()});
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
            log.log(Level.SEVERE, message, t);
        }

        return node;
    }
}
