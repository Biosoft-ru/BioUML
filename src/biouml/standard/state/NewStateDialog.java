package biouml.standard.state;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

import biouml.model.Diagram;

public class NewStateDialog extends OkCancelDialog
{
    protected State state;
    protected Diagram parent;
    protected JTextField id;
    protected JPanel content;
    protected PropertyInspector propertyInspector;

    public NewStateDialog(String title, Diagram parent)
    {
        super(Application.getApplicationFrame(), title);
        this.parent = parent;
        init();
    }

    public State getNewDiagramElement()
    {
        return state;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Protected methods
    //

    private void init()
    {
        content = new JPanel(new GridBagLayout());
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
            if( parent.getState( name ) != null )
            {
                JOptionPane.showMessageDialog(NewStateDialog.this, "State \"" + name + "\"" + " is already exists.", "",
                        JOptionPane.WARNING_MESSAGE);
            }
            else
            {
                state = new State(parent, name);

                propertyInspector.explore(state);
                id.setEnabled(false);
                okButton.setEnabled(true);
            }
        });

        okButton.setEnabled(false);

        propertyInspector = new PropertyInspector();
        propertyInspector.setPreferredSize(new Dimension(350, 250));
        propertyInspector.explore(state);

        content.add(propertyInspector, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));
    }
}
