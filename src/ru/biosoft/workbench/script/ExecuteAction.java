package ru.biosoft.workbench.script;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.task.RunnableTask;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.ViewPartRegistry;

public class ExecuteAction extends AbstractAction
{
    public static final String KEY = "ExecuteScript";

    public ExecuteAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Document document = Document.getCurrentDocument();
        if(document == null) return;
        document.save();
        Object model = document.getModel();
        if(!(model instanceof ScriptDataElement)) return;
        final ScriptDataElement script = (ScriptDataElement)model;
        OutputViewPart outputPane = (OutputViewPart)ViewPartRegistry.getViewPart( "script.output" );
        GUI.getManager().showViewPart( outputPane );
        final SwingScriptEnvironment environment = new SwingScriptEnvironment(outputPane.getTextPane());
        
        setEnabled(false);
        
        TaskPool.getInstance().submit(new RunnableTask("Script "+script.getName(), new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    String result = script.execute(script.getContent(), environment, false);
                    if(!environment.hasData())
                        environment.print(result);
                }
                finally
                {
                    setEnabled(true);
                }
            }
        }));
    }
}
