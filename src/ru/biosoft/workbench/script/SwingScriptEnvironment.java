package ru.biosoft.workbench.script;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLDocument;

import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;
import ru.biosoft.jobcontrol.JobControl;

public class SwingScriptEnvironment implements ScriptEnvironment
{
    protected JTextPane textPane;
    private boolean hasData = false;
    
    private static final SimpleAttributeSet ERROR_STYLE, WARN_STYLE, PRINT_STYLE;
    
    static
    {
        ERROR_STYLE = new SimpleAttributeSet();
        StyleConstants.setForeground(ERROR_STYLE, Color.RED);
        StyleConstants.setBold(ERROR_STYLE, true);
        WARN_STYLE = new SimpleAttributeSet();
        StyleConstants.setForeground(WARN_STYLE, Color.ORANGE);
        PRINT_STYLE = new SimpleAttributeSet();
        StyleConstants.setForeground(PRINT_STYLE, Color.BLACK);
    }

    public SwingScriptEnvironment(JTextPane textArea)
    {
        this.textPane = textArea;
    }

    @Override
    public void error(String msg)
    {
        if( msg.length() > 0 )
        {
            appendText(msg + "\n", ERROR_STYLE);
        }
    }

    @Override
    public void warn(String msg)
    {
        if( msg.length() > 0 )
        {
            appendText(msg + "\n", WARN_STYLE);
        }
    }

    @Override
    public void print(String msg)
    {
        if( msg.length() > 0 )
        {
            appendText(msg + "\n", PRINT_STYLE);
        }
    }

    @Override
    public void info(String msg)
    {
        print( msg );
    }

    @Override
    public void showGraphics(BufferedImage image)
    {
        showGraphics(new ImageDataElement("", null, image));
    }

    @Override
    public void showGraphics(final ImageElement element) 
    {
        hasData = true;
        JComponent imageBuffer = new JComponent()
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                g.drawImage(element.getImage(getBounds().getSize()), 0, 0, null);
            }
        };
        OkCancelDialog dialog = new OkCancelDialog(Application.getApplicationFrame(), "Graphics", imageBuffer, null, "Close");
        dialog.addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                if(element instanceof Closeable)
                {
                    try
                    {
                        ( (Closeable)element ).close();
                    }
                    catch( IOException e1 )
                    {
                        // Ignore
                    }
                }
            }
        } );
        dialog.setSize(550, 450);
        dialog.setModal(false);
        dialog.setVisible( true );
        dialog.requestFocus();
    }
    
    @Override
    public void showTable(TableDataCollection dataCollection)
    {
        TabularPropertyInspector tpi = new TabularPropertyInspector();
        OkCancelDialog dialog = new OkCancelDialog(Application.getApplicationFrame(), "Table", tpi, null, "Close");
        dialog.setSize(550, 450);
        tpi.explore(dataCollection.iterator());
        dialog.setModal(false);
        dialog.setVisible( true );
        dialog.requestFocus();
    }

    protected void appendText(String str, AttributeSet style)
    {
        try
        {
            hasData = true;
            Document doc = textPane.getDocument();
            doc.insertString(doc.getLength(), str, style);
        }
        catch( BadLocationException e )
        {
        }
    }
    @Override
    public void showHtml(String html)
    {
        hasData = true;
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        HTMLDocument doc = new HTMLDocument();

        textPane.setDocument(doc);

        int idx = html.indexOf("<body>");
        if( idx > 0 )
            html = "<html>" + html.substring(idx);
        textPane.setText(html);

        JScrollPane scrollPane = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        OkCancelDialog dialog = new OkCancelDialog(Application.getApplicationFrame(), "Help", scrollPane, null, "Close");
        dialog.setSize(550, 450);
        dialog.show();
    }

    public boolean hasData()
    {
        return hasData;
    }
    
    private JobControl jobControl;
    public void setJobControl(JobControl jobControl)
    {
        this.jobControl = jobControl;
    }

    @Override
    public boolean isStopped()
    {
         return jobControl != null && jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST;
    }

    @Override
    public String addImage(BufferedImage image)
    {
        return null;
    }
}
