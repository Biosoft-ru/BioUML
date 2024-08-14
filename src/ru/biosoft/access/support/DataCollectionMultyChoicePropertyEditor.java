package ru.biosoft.access.support;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;

import com.developmentontheedge.beans.editors.CustomEditorSupport;


abstract public class DataCollectionMultyChoicePropertyEditor extends CustomEditorSupport
{
    private static Logger log = Logger.getLogger(DataCollectionMultyChoicePropertyEditor.class.getName());

    protected JPanel panel;
    protected JButton editButton;
    protected Component parent;
    protected String title;
    
    public DataCollectionMultyChoicePropertyEditor()
    {
        panel = new JPanel(new BorderLayout(3, 0));
        panel.setOpaque(false);
        editButton = new JButton("Edit ...");
        panel.add(editButton, BorderLayout.EAST);
        initEditButtonListener();
    }

    protected void initEditButtonListener()
    {
        editButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    DataCollectionMultyChoiceDialog dialog;
                    DataCollection dc = getDataCollection();
                    if( dc != null )
                    {
                        dialog = new DataCollectionMultyChoiceDialog(parent, title, dc, (String[])getValue(), true);
                        if( dialog.doModal() )
                            setValue(dialog.getSelectedValues());
                    }
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, t.getMessage(), t);
                }
            }
        });
    }

    /** Should return the DataCollection containing possible values. */
    abstract public DataCollection getDataCollection() throws Exception;

    ////////////////////////////////////////////////////////////////////////////
    //
    //

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
