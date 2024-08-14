
package ru.biosoft.gui;

import java.awt.BorderLayout;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JTabbedPane;


import ru.biosoft.gui.resources.MessageBundle;

import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ApplicationAction;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;

@SuppressWarnings ( "serial" )
public class LogPane extends ViewPartSupport implements ClearablePane
{
    static final String[] CATEGORY_LIST = {"ru.biosoft", "com.developmentontheedge", "biouml"};
    private TextPaneAppender appender;

    protected Action clearLogAction;
    private JTabbedPane tabbedPane;
    private TextPaneAppender errorAppender;

    @Override
    public Action[] getActions()
    {
        return new Action[] {clearLogAction};
    }

    public LogPane()
    {
        clearLogAction = new ClearLogAction(this);
        new ActionInitializer(MessageBundle.class).initAction(clearLogAction, ClearLogAction.KEY);
        clearLogAction.setEnabled(true);
        init();
    }

    protected void init()
    {
        //appender = new TextPaneAppender(new PatternLayout("%-5p :  %m%n"), "Application Log");
        //appender.setThreshold(Level.DEBUG);
        appender = new TextPaneAppender( new PatternFormatter( "[%4$-7s] :  %5$s%n" ), "Application Log" );
        appender.setLevel( Level.FINE );
        appender.addToCategories(CATEGORY_LIST);
        setLayout(new BorderLayout());

        errorAppender = new TextPaneAppender( new PatternFormatter( "[%4$-7s] :  %5$s%n" ), "Error Log" );//new PatternLayout("%-5p :  %m%n"), "Error Log");
        //errorAppender.setThreshold(Level.DEBUG);
        errorAppender.setLevel( Level.FINE );
        errorAppender.addToCategories(new String[] {"error.log"});
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane(JTabbedPane.RIGHT);
        tabbedPane.add("Application log", appender.getLogTextPanel());
        tabbedPane.add("Error log", errorAppender.getLogTextPanel());

        add(tabbedPane);

        if( action == null )
            action = new ApplicationAction("Application log", "Application log console");
    }

    @Override
    public Object getModel()
    {
        return STATIC_VIEW;
    }

    @Override
    public void clear()
    {
        appender.close();
        errorAppender.close();
        remove(tabbedPane);
        init();
        updateUI();
    }

}
