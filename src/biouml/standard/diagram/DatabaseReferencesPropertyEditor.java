package biouml.standard.diagram;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JPanel;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.standard.type.Referrer;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

public class DatabaseReferencesPropertyEditor extends CustomEditorSupport
{
    private static Logger log = Logger.getLogger(DatabaseReferencesPropertyEditor.class.getName());

    protected JPanel panel;
    protected JButton editButton;
    protected Component parent;

    public DatabaseReferencesPropertyEditor()
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
            Referrer referrer = (Referrer)getBean();
            DatabaseReferencesEditDialog dialog;
            dialog = new DatabaseReferencesEditDialog(parent, referrer);

            if( dialog.isEnabled() && dialog.doModal() )
            {
                setValue(dialog.getValue());
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