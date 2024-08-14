package ru.biosoft.table;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

public class StringSetViewer extends CustomEditorSupport
{
    @Override
    public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    {
        final String[] vals = getValue() instanceof StringSet?((StringSet)getValue()).toStringArray():new String[]{};
        JButton button = new JButton(getText(vals));
        button.setHorizontalAlignment(JButton.LEFT);
        button.addActionListener(evt -> {
            JFrame frame = new JFrame("Values");
            JList<String> list = new JList<>();
            if(getValue() instanceof StringSet)
                list.setListData(vals);
            JScrollPane scrollPane = new JScrollPane(list);
            frame.setSize(500, 500);
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.doLayout();
            frame.setVisible(true);
            frame.setAlwaysOnTop(true);
            frame.setResizable(true);
            frame.setLocationRelativeTo(null);
            frame.setEnabled(true);
            frame.setFocusable(true);
        });
        return button;
    }

    /**
     * Returns text describing current selection
     */
    protected String getText(Object[] vals)
    {
        StringBuffer result = new StringBuffer();
        if(vals.length == 0)
        {
            return "(no selection)";
        }
        if(vals.length>1)
        {
            result.append("["+(String.valueOf(vals.length))+"] ");
        }
        for(int i=0; i<vals.length; i++)
        {
            if(i>0) result.append(", ");
            result.append(vals[i].toString());
            if(result.length() > 100) break;
        }
        return result.toString();
    }
    
    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        return getCustomRenderer(parent, isSelected, false);
    }
}
