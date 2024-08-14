package ru.biosoft.workbench.script;

import java.awt.BorderLayout;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;

import com.developmentontheedge.application.action.ActionInitializer;

import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.gui.ClearLogAction;
import ru.biosoft.gui.ClearablePane;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.gui.resources.MessageBundle;

@SuppressWarnings ( "serial" )
public class OutputViewPart extends ViewPartSupport implements ClearablePane
{
    protected JTextPane textArea;
    protected AbstractAction clearLogAction;

    public OutputViewPart()
    {
        textArea = new JTextPane(new DefaultStyledDocument());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        clearLogAction = new ClearLogAction(this);
        new ActionInitializer(MessageBundle.class).initAction(clearLogAction, ClearLogAction.KEY);
        clearLogAction.setEnabled(true);

        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    public JTextPane getTextPane()
    {
        return textArea;
    }

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof ScriptDataElement;
    }

    @Override
    public Action[] getActions()
    {
        return new Action[] {clearLogAction};
    }
    
    @Override
    public void clear()
    {
        Document doc = textArea.getDocument();
        try
        {
            doc.remove( 0, doc.getLength() );
        }
        catch( BadLocationException e1 )
        {
            // ignore
        }
    }
}
