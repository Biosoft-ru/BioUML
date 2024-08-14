package biouml.workbench;

import java.awt.Dimension;
import java.text.MessageFormat;

import javax.swing.JFrame;

import java.util.logging.Level;

import java.util.logging.Logger;
import biouml.workbench.resources.MessageBundle;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.TextPaneAppender;


public class ProcessElementDialog extends OkCancelDialog
{
    // Logging issues
    private static final String[] CATEGORY_LIST = {"biouml.diagram", "biouml.workbench", "biouml.plugins"};
    protected Logger log;
    protected TextPaneAppender appender;

    protected MessageBundle messageBundle = BioUMLApplication.getMessageBundle();

    ///////////////////////////////////////////////////////////////////
    // Constructor
    //

    public ProcessElementDialog(String titleKey)
    {
        this(Application.getApplicationFrame(), titleKey);
    }

    public ProcessElementDialog(JFrame parent, String titleKey)
    {
        super(parent, "");
        setTitle(messageBundle.getResourceString(titleKey));
    }

    protected void initAppender(String title, String initialMessage)
    {
        //--- logging settings ---
        appender = new TextPaneAppender( new PatternFormatter( "[%4$-7s] :  %5$s%n" ), title );
        appender.setLevel( Level.INFO );
        appender.addToCategories(CATEGORY_LIST);
        appender.getLogTextPanel().setPreferredSize(new Dimension(350, 150));

        if( initialMessage != null )
            appender.getLogTextPanel().setText(initialMessage);
    }

    protected void info(String messageBundleKey, Object... params)
    {
        String message = messageBundle.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.info(message);
    }
    protected void warn(String messageBundleKey, Object... params)
    {
        String message = messageBundle.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.warning(message);
    }
    protected void error(String messageBundleKey, Object... params)
    {
        String message = messageBundle.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.log(Level.SEVERE, message);
    }
}
