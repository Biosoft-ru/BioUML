package biouml.model.xml;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Level;
import java.util.Iterator;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.standard.type.Stub;

public class XmlCreateDiagramElementDialog extends OkCancelDialog
{
    protected static final Logger log = Logger.getLogger(XmlCreateDiagramElementDialog.class.getName());

    protected Compartment parent;
    protected DiagramElement de;
    protected JTextField id;
    protected PropertyInspector propertyInspector;
    protected String typeStr;
    protected boolean isCompartment;
    protected DynamicPropertySet attributes;

    public XmlCreateDiagramElementDialog(String title, Compartment parent, String typeStr, boolean isCompartment,
            DynamicPropertySet attributes)
    {
        super(Application.getApplicationFrame(), title);
        this.parent = parent;
        this.typeStr = typeStr;
        this.isCompartment = isCompartment;
        this.attributes = attributes;
        init();
    }

    public DiagramElement getNewDiagramElement()
    {
        return de;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Protected methods
    //

    private void init()
    {
        JPanel content = new JPanel( new GridBagLayout() );
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContent(content);

        String idName = "Name";
        content.add(new JLabel(idName), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        id = new JTextField(15);
        content.add(id, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 0,
                0), 0, 0));

        id.addActionListener(e -> {
            String name = id.getText();
            if( parent.contains(name) )
            {
                JOptionPane.showMessageDialog(XmlCreateDiagramElementDialog.this, "Data element \"" + name + "\""
                        + " already exists.", "", JOptionPane.WARNING_MESSAGE);
            }
            else
            {
                createDiagramElement(name);

                propertyInspector.explore(de);
                id.setEnabled(false);
                okButton.setEnabled(true);
            }
        });

        okButton.setEnabled(false);

        propertyInspector = new PropertyInspector();
        propertyInspector.setPreferredSize(new Dimension(350, 250));
        propertyInspector.explore(de);

        content.add(propertyInspector, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));
    }

    protected void createDiagramElement(String name)
    {
        Stub stub = null;
        try
        {
            stub = new Stub(null, name, typeStr);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not create new diagram element, error: " + t, t);
        }

        if( !isCompartment )
        {
            de = new Node(parent, stub);
        }
        else
        {
            de = new Compartment(parent, stub);
            ( (Compartment)de ).setShapeSize(new Dimension(0, 0));
        }

        Iterator<String> iter = attributes.nameIterator();
        while( iter.hasNext() )
        {
            de.getAttributes().add( attributes.getProperty( iter.next() ) );
        }
    }
}
