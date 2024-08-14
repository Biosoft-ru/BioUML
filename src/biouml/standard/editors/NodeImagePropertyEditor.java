package biouml.standard.editors;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JPanel;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.standard.type.ImageDescriptor;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

public class NodeImagePropertyEditor extends CustomEditorSupport
{
    private static Logger log = Logger.getLogger(NodeImagePropertyEditor.class.getName());

    protected JPanel panel;
    protected JButton editButton;
    protected Component parent;
    protected String title = "Image Editor";

    public NodeImagePropertyEditor()
    {
        panel = new JPanel(new BorderLayout(3, 0));
        panel.setOpaque(false);
        editButton = new JButton("Edit ...");
        panel.add(editButton, BorderLayout.EAST);

        editButton.addActionListener(e -> editButtonAction());
    }

    public void editButtonAction()
    {
        try
        {
            ImageDescriptor imageDescriptor = (ImageDescriptor)getBean();
            NodeImageChoiceDialog dialog;
            dialog = new NodeImageChoiceDialog(parent, title, imageDescriptor);

            if(dialog.doModal())
            {
                setValue(dialog.getImageSource());
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, t.getMessage(), t);
        }
    }

    @Override
    public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    {
        return panel;
    }

    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        this.parent = parent;
        return panel;
    }
}
