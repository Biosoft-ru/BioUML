package biouml.plugins.wdl.texteditor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class TextPaneAppender extends Handler
{
    StringWriter sw;
    LogPanel logTextPanel;
    LogPublishingThread logPublisher;
    String name;
    PrintWriter pw;

    static final String COLOR_OPTION_FATAL = "Color.Fatal";
    static final String COLOR_OPTION_ERROR = "Color.Error";
    static final String COLOR_OPTION_WARN = "Color.Warn";
    static final String COLOR_OPTION_INFO = "Color.Info";
    static final String COLOR_OPTION_DEBUG = "Color.Debug";
    static final String COLOR_OPTION_BACKGROUND = "Color.Background";
    static final String FONT_NAME_OPTION = "Font.Name";
    static final String FONT_SIZE_OPTION = "Font.Size";
    static final String EVENT_BUFFER_SIZE_OPTION = "EventBuffer.Size";

    private String[] categoryNames;

    public TextPaneAppender(Formatter formatter, String name)
    {
        this.setFormatter(formatter);
        this.name = name;
        this.sw = new StringWriter();
        pw = new PrintWriter(sw);
        setLogTextPanel(new LogPanel());
        logPublisher = new LogPublishingThread(name, logTextPanel, Level.FINE, 500);
    }

    @Override
    public void close() throws SecurityException
    {
        if( categoryNames != null )
            removeFromCategories(categoryNames);
        categoryNames = null;
        logPublisher.close();
    }

    @Override
    public void flush()
    {
    }

    @Override
    public void publish(LogRecord record)
    {
        String text = this.getFormatter().format(record);

        // Print Stacktrace
        if( record.getThrown() != null )
        {
            record.getThrown().printStackTrace(pw);

            for( int i = 0; i < sw.getBuffer().length() && i < 200; i++ )
            {
                if( sw.getBuffer().charAt(i) == '\t' )
                    sw.getBuffer().replace(i, i + 1, "        ");
            }
            text += sw.toString();
            sw.getBuffer().delete(0, sw.getBuffer().length());
        }
        else if( !text.endsWith("\n") )
            text += "\n";
        logPublisher.publishEvent(record.getLevel(), text);
    }

    public LogPanel getLogTextPanel()
    {
        return logTextPanel;
    }

    public String[] getOptionStrings()
    {
        return new String[] {COLOR_OPTION_FATAL, COLOR_OPTION_ERROR, COLOR_OPTION_WARN, COLOR_OPTION_INFO, COLOR_OPTION_DEBUG,
                COLOR_OPTION_BACKGROUND, FONT_NAME_OPTION, FONT_SIZE_OPTION};
    }


    //@Override
    public void setName(String name)
    {
        this.name = name;
    }

    protected void setLogTextPanel(LogPanel logTextPanel)
    {
        this.logTextPanel = logTextPanel;
        //logTextPanel.setTextBackground(Color.white);
    }

    static class LogPublishingThread extends Thread
    {
        LogPanel logTextPanel;
        List<EventBufferElement> evts;
        Level triggerPrio;
        long pubInterval;
        volatile boolean finish = false;

        public LogPublishingThread(String name, LogPanel logTextPanel, Level triggerPrio, long pubInterval)
        {
            super("Logging thread: " + name);
            this.logTextPanel = logTextPanel;
            this.evts = new ArrayList<>(1000);
            this.triggerPrio = triggerPrio;
            this.pubInterval = pubInterval;
            //this.setPriority(Thread.NORM_PRIORITY - 1);
            this.start();
        }

        public void close()
        {
            finish = true;
            interrupt();
        }

        @Override
        public void run()
        {
            while( true )
            {
                synchronized( evts )
                {
                    try
                    {
                        evts.wait(pubInterval);
                    }
                    catch( InterruptedException e )
                    {
                    }
                    if( finish )
                    {
                        break;
                    }

                    logTextPanel.newEvents(evts.toArray(new EventBufferElement[evts.size()]));

                    evts.clear();
                }
            }

        }

        public void publishEvent(Level prio, String text)
        {
            synchronized( evts )
            {
                evts.add(new EventBufferElement(prio, text));
                if( triggerPrio != null && prio.intValue() >= triggerPrio.intValue() )
                {
                    evts.notify();
                }
            }
        }
    }

    public void addToCategories(String[] categoryNames)
    {
        this.categoryNames = categoryNames;
        for( int i = 0; i < categoryNames.length; i++ )
        {
            Logger cat = Logger.getLogger(categoryNames[i]);
            cat.addHandler(this);
        }
    }

    public void removeFromCategories(String[] categoryNames)
    {
        for( int i = 0; i < categoryNames.length; i++ )
        {
            Logger cat = Logger.getLogger(categoryNames[i]);
            cat.removeHandler(this);
        }
    }

    static class EventBufferElement
    {
        public String text;
        public Level prio;
        public int numLines;

        EventBufferElement(Level prio, String text)
        {
            this.prio = prio;
            this.text = text;
            numLines = 1;
            int pos = text.indexOf('\n', 0);
            int len = text.length() - 1;

            while( ( pos > 0 ) && ( pos < len ) )
            {
                numLines++;
                pos = text.indexOf('\n', pos + 1);
            }
        }
    }


}
