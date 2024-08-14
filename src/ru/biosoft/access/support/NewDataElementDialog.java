package ru.biosoft.access.support;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.util.OkCancelDialog;

import com.developmentontheedge.beans.swing.PropertyInspectorEx;

/**
 * Creates new {@link ru.biosoft.access.core.DataElement} for the specified {@link ru.biosoft.access.core.DataCollection}.
 * It is strongly suggested that Class returned by ru.biosoft.access.core.DataCollection.getDataElementType
 * can be instantiated and has constructor with 2 parameters:
 * <code>DataCollection origin</code> and <code>String name</code> (they order is not important).
 *
 * Note. If data element with specified name is already exists,
 * then we get it from DataCollection and show in property inspector.

 * @pending use MessageBundle.
 */
@SuppressWarnings ( "serial" )
public class NewDataElementDialog extends OkCancelDialog
{
    protected static final Logger log = Logger.getLogger(NewDataElementDialog.class.getName());

    protected DataCollection dc;
    protected DataElement    de;
    protected JTextField     id;
    protected JPanel         content;
    protected PropertyInspectorEx propertyInspector;

    public NewDataElementDialog(Component parent, String title, DataCollection<?> dc)
    {
        super(parent, title);
        this.dc = dc;
        init();
    }

    public DataElement getNewDataElement()
    {
        return de;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Protected methods
    //

    private void init()
    {
        content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContent(content);

        String formatter = dc.getInfo().getProperty(DataCollectionConfigConstants.ID_FORMAT);
        if( formatter != null )
        {
            String name = IdGenerator.generateUniqueName(dc, new DecimalFormat(formatter));
            createDataElement(name);
        }
        else
        {
            String idName = "Name";
            content.add(new JLabel(idName),
                        new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                           GridBagConstraints.NONE,
                                           new Insets(0, 0, 0, 0), 0, 0));
            id = new JTextField(15);
            content.add(id,
                        new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                                           GridBagConstraints.HORIZONTAL,
                                           new Insets(5, 5, 0, 0), 0, 0));

            id.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    String name = id.getText();
                    if( dc.contains(name) )
                    {
                        JOptionPane.showMessageDialog(NewDataElementDialog.this,
                                                      "Data element \"" + name + "\"" + " is already exists.",
                                                      "", JOptionPane.WARNING_MESSAGE);
                    }
                    else
                    {
                        createDataElement(name);

                        propertyInspector.explore(de);
                        id.setEnabled(false);
                        okButton.setEnabled(true);
                    }
                }
            });

            okButton.setEnabled(false);
        }

        propertyInspector = new PropertyInspectorEx();
        propertyInspector.setPreferredSize(new Dimension(350, 250));
        propertyInspector.explore(de);

        content.add(propertyInspector,
                    new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.WEST,
                    GridBagConstraints.BOTH, new Insets(5, 5, 0, 0), 0, 0));
    }

    /** Creates data element with the specified name. */
    protected void createDataElement(String name)
    {
        try
        {
            de =  DataElementFactory.getInstance().create( dc, name );
        }
        catch(Throwable t)
        {
            String msg = "Creating data element error" + t;
            log.log(Level.SEVERE, msg, t);
            JOptionPane.showMessageDialog(this, msg, "", JOptionPane.ERROR_MESSAGE);
        }
    }
}
