package ru.biosoft.plugins.javascript;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextPane;

import ru.biosoft.workbench.script.SwingScriptEnvironment;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

public class JSCommandViewer extends CustomEditorSupport
{

    @Override
    public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    {
        if( ! ( getValue() instanceof JSCommand ) || ( (JSCommand)getValue() ).isEmpty() )
        {
            return new JLabel();
        }
        JButton button = new JButton("View");
        button.addActionListener(evt -> {
            JSCommand jsCommand = (JSCommand)getValue();
            JScriptContext.evaluateString(jsCommand.getCommand(), new SwingScriptEnvironment(new JTextPane()));
        });
        return button;
    }

    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        return getCustomRenderer(parent, isSelected, false);
    }
}
