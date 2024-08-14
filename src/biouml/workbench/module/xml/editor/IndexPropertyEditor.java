package biouml.workbench.module.xml.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.workbench.module.xml.XmlModule.InternalType;
import biouml.workbench.module.xml.XmlModule.InternalType.IndexDescription;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

public class IndexPropertyEditor extends CustomEditorSupport
{
    private static Logger log = Logger.getLogger(IndexPropertyEditor.class.getName());

    protected JPanel panel;
    protected JButton editButton;
    protected Component parent;

    public IndexPropertyEditor()
    {
        panel = new JPanel(new BorderLayout(3, 0));
        panel.setOpaque(false);
        editButton = new JButton("Edit ...");
        panel.add(editButton, BorderLayout.EAST);

        editButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                editButtonAction();
            }
        });
    }

    public void editButtonAction()
    {
        try
        {
            List<IndexDescription> indexes = ( (InternalType)getBean() ).getIndexes();
            IndexEditorDialog dialog = new IndexEditorDialog(parent, "Indexes editor", indexes);

            if( dialog.doModal() )
            {
                ( (InternalType)getBean() ).setIndexes(dialog.getIndexes());
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