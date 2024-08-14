package ru.biosoft.table;

import java.awt.Component;

import javax.swing.JLabel;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

/**
 * @author lan
 *
 */
public class DescribedStringViewer extends CustomEditorSupport
{
    @Override
    public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    {
        JLabel label = new JLabel(((DescribedString)getValue()).getTitle());
        // TODO: fix color
        label.setOpaque(false);
        setColor(label, isSelected, parent);
        return label;
    }

    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        return getCustomRenderer(parent, isSelected, false);
    }

}
